package dexter.cluster;

import java.util.*;
import java.io.*;

import dexter.cluster.tune.*;



/*****************
      The Neighbor-Joining algorithm doesn't build a tree, it builds an undirected graph, placing n-2
intermediate nodes between n leaf nodes. This builder creates a tree by placing a root node in the
middle of the last pair of leaf nodes to be joined. This is reasonable, since those nodes are the
most mutually distant pair as measured by the NJ metric.

See Durbin, Eddy, Krogh, and Mitchison p. 171.

COMPLEXITY:
      The edge-building part of the brute-force algorithm is O(n^4) due to finding nearest neighbors:
      while (have untreated nodes)
           for each node
                for each node
                     Are these nearest remaining neighbors? bigD() calls r = for each node
                     
      To eliminate the innermost O(n), initially compute r for all nodes, and compute on the fly
      for intermediate nodes as they are generated. Can't cache a bigD distance table - I'm already
      concerned about distance table size (O(n^2)). Might be ok if I upgrade to a 32G machine but 
      that might exclude certain users.

*******************/


public class NeighborJoiningTreeBuilder<P> extends TreeBuilder<P> 
{
	// Structures for the NJ algorithm.
	private Set<Node<P>>				setL;		// book calls this "L"
	private Set<Node<P>>				setT;		// book calls this "T"
	private Vector<Edge>				edges;		// for now, to preserve order for debugging. TODO: convert to array
	private Map<Node<P>, Float>			nodeTo_r;	// my guess: "r" stands for radius
	
	// Structures for converting edges generated by the algorithm to a tree.
	private Edge						lastEdge;
	private Map<Node<P>, Vector<Edge>> 	internalNodeToEdges;
	private int							nEdgesIncorporatedIntoTree;
	
	
	private class Edge
	{
		Vector<Node<P>>					nodes;
		float							length;
		
		Edge(Node<P> node1, Node<P> node2, float length)
		{
			nodes = new Vector<Node<P>>(2);
			nodes.add(node1);
			nodes.add(node2);
			this.length = length;
		}
		
		Node<P> otherNode(Node<P> n1)
		{
			assert nodes.contains(n1);
			for (Node<P> n2: nodes)
				if (n2 != n1)
					return n2;
			return null;
		}
		
		public String toString()
		{
			return nodes.get(0) + " __ " + length + " __ " + nodes.get(1);
		}
	}  // End of inner class Edge
	
	
	public NeighborJoiningTreeBuilder(DistanceMatrix<Node<P>> distanceMatrix) 
	{
		super(distanceMatrix, ClusterAlgorithm.NJ);		
		
		nextIntermediateNodeSN = distanceMatrix.nKeys();
		edges = new Vector<Edge>();
	}

	
	public Node<P> buildTree() 
	{
		// Inform listeners that clustering is starting.
		fireClusteringStarted();
		
		// Initialize.
		setL = new HashSet<Node<P>>(distanceMatrix.keys());			// unassigned nodes
		setT = new HashSet<Node<P>>(distanceMatrix.keys());			// product

		// If tracking progress: 1st phase is to apply the NJ algorithm to generate edges.
		int totalEdgeGeneratingSteps = setL.size() - 2;
		int nEdgesGenerated = 0;
		startNewPhase(totalEdgeGeneratingSteps);
		
		// Iterate until only 2 nodes remain in L.
		Node<P> nodeI = null;
		Node<P> nodeJ = null;
		float adjustedDij = 0f;
		float dn1n2 = 0f;
		while (setL.size() > 2)										// O(n)
		{
			// Abort if requested.
			if (requestAbort)
				return null;
			
			// Use verbose with care.
			if (verbose)
				sop(new Date() + ": " + setL.size() + " in setL");
			
			// Precompute r for all nodes in L.
			if (verbose)
				sop("  " + new Date() + ": will compute r for all nodes");
			nodeTo_r = compute_rForAllNodes();
			
			// "Pick a pair i,j, in L for which Dij is minimal."
			if (verbose)
				sop("  " + new Date() + ": will pick a pair i,j, in L for which Dij is minimal");
			nodeI = nodeJ = null;
			adjustedDij = Float.MAX_VALUE;
			for (Node<P> n1: setL)									// O(n^2)
			{
				for (Node<P> n2: setL)								// O(n^3)
				{
					if (n1 == n2)
						continue;
					dn1n2 = bigD(n1, n2);
					if (dn1n2 < adjustedDij)
					{
						adjustedDij = dn1n2;
						nodeI = n1;
						nodeJ = n2;
					}
				}
			}

			// "Define a new node k and set d(km) for all m in L (except i and j)." 
			if (verbose)
				sop("  " + new Date() + ": will define a new node k and set d(km) for all m in L");
			Node<P> nodeK = new Node<P>();
			nodeK.setName("N" + nextIntermediateNodeSN++);				// k is the new node - note no payload
			float dij = distanceMatrix.getDistance(nodeI, nodeJ);
			for (Node<P> nodeM: setL)
			{
				if (nodeM == nodeI  ||  nodeM == nodeJ)
					continue;
				float dim = distanceMatrix.getDistance(nodeI, nodeM);
				float djm = distanceMatrix.getDistance(nodeJ, nodeM);
				float dkm = (dim + djm - dij) / 2f;
				distanceMatrix.setDistance(nodeK, nodeM, dkm);
			}
			
			// "Add k to T with edges..."
			if (verbose)
				sop("  " + new Date() + ": will add k to T");
			float dik = (dij + r(nodeI) - r(nodeJ)) / 2f;
			distanceMatrix.setDistance(nodeI, nodeK, dik);
			Edge eik = new Edge(nodeI, nodeK, dik);
			edges.add(eik);
			float djk = dij - dik;
			distanceMatrix.setDistance(nodeJ, nodeK, djk);
			edges.add(new Edge(nodeJ, nodeK, djk));
			setT.add(nodeK);
			
			// "Remove i and j from L and add k."
			setL.remove(nodeI);
			setL.remove(nodeJ);
			setL.add(nodeK);
			
			// Report progress.
			reportPhaseProgress(++nEdgesGenerated);	
		}  // end of while-loop
		
		// Terminate main NJ loop. "When L consists of two leaves i and j
		// add the remaining edge between i and j, with length dij."
		// But it's more convenient for the tree-building step if we
		// cache the last edge without adding it to the collection.
		Iterator<Node<P>> iter = setL.iterator();
		nodeI = iter.next();
		nodeJ = iter.next();
		assert !iter.hasNext();
		lastEdge = new Edge(nodeI, nodeJ, distanceMatrix.getDistance(nodeI, nodeJ)); 
		firePhaseFinished();
		
		// Build a tree from the collection of edges.
		Node<P> rootedTree = convertEdgesToRootedTree();
		fireClusteringFinished();
		return rootedTree;
	}  // End of buildTree()
	
	
	//
	// Defined as D-sub-ij on p. 170. Note capital D, versus d-sub-ij, which is the distance provided
	// in the table. The NJ distance is the distance in the table, minus the average distances ("r")
	// from nodes i and j to all other nodes. This metric can be negative; it is only used for picking
	// nearest neighbors.
	//
	// This method should only be called when the node-to-r map is valid. The map must be revised every 
	// time nodes are added to or removed from L.
	//
	// Called at a level with complexity = O(n^3)
	//
	private float bigD(Node<P> node1, Node<P> node2)
	{
		float r1 = nodeTo_r.get(node1);
		float r2 = nodeTo_r.get(node2);
		return distanceMatrix.getDistance(node1, node2) - r1 - r2;
	}
	
	
	// Defined as r-sub-i at the top of p. 171.
	private float r(Node<P> nodeI)
	{
		assert setL.size() > 2;
		
		float r = 0f;
		for (Node<P> nodeK: setL)
			if (nodeK != nodeI)
				r += distanceMatrix.getDistance(nodeI, nodeK);
		r /= (setL.size() - 2f);
		return r;
	}
	
	
	private Map<Node<P>, Float> compute_rForAllNodes()
	{
		HashMap<Node<P>, Float> ret = new HashMap<Node<P>, Float>();
		for (Node<P> node: setL)
			ret.put(node, r(node));		
		return ret;
	}
	
	
	
	
					
	
	
				
					//////////////////////////////////////////////////////
					//                                                  //
					//                    CONVERSION                    //
					//                                                  //
					//////////////////////////////////////////////////////
				
					
	
	//
	// The algorithm produces a set of edges, which need to be converted to a rooted tree. The
	// last edge added is split in half and a root node is inserted.
	//
	private Node<P> convertEdgesToRootedTree()
	{
		// Insert root node in center of last edge added.
		Node<P> root = new Node<P>();
		root.setName("NEIGHBOR-JOINING ROOT");
		float halfLen = lastEdge.length / 2f;
		for (Node<P> node: lastEdge.nodes)
			edges.add(new Edge(root, node, halfLen));
		
		// Report start of this phase.
		startNewPhase(edges.size());
		int nEdgesConnected = 0;
				
		// Map internal nodes to edges. Internal nodes are connected to 3 edges, except for the 
		// root, which is connected to 2 edges.
		internalNodeToEdges = new HashMap<Node<P>, Vector<Edge>>();
		for (Edge edge: edges)
		{
			for (Node<P> node: edge.nodes)
			{
				if (node.getPayload() == null)
				{
					// Internal node, connected to 3 edges.
					Vector<Edge> vec = internalNodeToEdges.get(node);
					if (vec == null)
					{
						vec = new Vector<Edge>(3);
						internalNodeToEdges.put(node, vec);
					}
					vec.add(edge);
					assert vec.size() <= 3;
				}
			}
			reportPhaseProgress(++nEdgesConnected);
		}
		firePhaseFinished();
		
		// Recursively build down from the nodes that were connected to the last edge added.
		startNewPhase(edges.size());
		recurseBuildTree(null, root);		// null grandparent because root has no parent
		firePhaseFinished();
		return root;
	}
	
	
	private void recurseBuildTree(Node<P> grandparent, Node<P> parent)
	{

		Vector<Edge> edgesFromParent = internalNodeToEdges.get(parent);
		for (Edge edge: edgesFromParent)
		{
			// There are 3 edges from the parent. The edge from the grandparent has already
			// been processed and should be ignored.
			Node<P> kid = edge.otherNode(parent);
			if (kid == grandparent)
				continue;
			
			// Connect parent to kid and recurse if kid is internal node.
			parent.addKid(kid, edge.length);
			reportPhaseProgress(++nEdgesIncorporatedIntoTree);
			if (kid.getPayload() == null)
				recurseBuildTree(parent, kid);
		}
	}

	
	
	
	
	
	
	
	
						///////////////////////////////////////////////////
						//                                               //
						//                    TESTING                    //
						//                                               //
						///////////////////////////////////////////////////
	
	
	//
	// TEST_DISTANCES_SJSU was verified by converting the distance matrix to PHYLIP format and 
	// pasting into http://www.trex.uqam.ca/index.php?action=trex. The result agreed with mine,
	// but disagreed with my homework from SJSU. 
	//
	// The other 2 examples come from online examples with published solutions that agree with
	// my results.
	//	
	
	
	

	// From Fall 2008 CS223 HW4P1.
	public final static float[][] TEST_DISTANCES_SJSU =
	{
		{  0, 22, 39, 39, 41 },
		{ 22,  0, 41, 41, 43 },
		{ 39, 41,  0, 18, 20 },
		{ 39, 41, 18,  0, 10 },
		{ 41, 43, 20, 10,  0 }
	};
	
	
	// http://en.wikipedia.org/wiki/Neighbor_joining#Example
	public final static float[][] TEST_DISTANCES_WIKIPEDIA =
	{
		{  0,  7, 11, 14 },
		{  7,  0,  6,  9 },
		{ 11,  6,  0,  7 },
		{ 14,  9,  7,  0 }
	};
	
	
	// http://evolution-textbook.org/content/free/tables/Ch_27/T11_EVOW_Ch27.pdf
	public final static float[][] TEST_DISTANCES_EVO_ONLINE_TEXT =
	{
		{ 0,  5,  4,  7,  6,  8 },
		{ 5,  0,  7, 10,  9, 11 },
		{ 4,  7,  0,  7,  6,  8 },
		{ 7, 10,  7,  0,  5,  9 },
		{ 6,  9,  6,  5,  0,  8 },
		{ 8, 11,  8,  9,  8,  0 }
	};
	
	
	public static DistanceMatrix<Node<String>> buildTestMatrix(float[][] distances)
	{
		int nNodes = distances.length;
		for (float[] row: distances)
			assert row.length == nNodes;
		
		DistanceMatrix<Node<String>> matrix = new DualKeyDistanceMatrix<Node<String>>();
		Vector<Node<String>> nodes = new Vector<Node<String>>();
		for (int i=0; i<nNodes; i++)
		{
			Node<String> node = new Node<String>("" + (char)('A'+i));
			nodes.add(node);
		}
		
		for (int i=0; i<nNodes; i++)
			for (int j=0; j<nNodes; j++)
				matrix.setDistance(nodes.get(i), nodes.get(j), distances[i][j]);
		
		return matrix;
	}


	
	
	
	
	
	
						/////////////////////////////////////////////////////////
						//                                                     //
						//                    MISC AND MAIN                    //
						//                                                     //
						/////////////////////////////////////////////////////////

	

	
	
	public static int[] getNStepsPerPhase(int nTaxa)
	{
		return new int[] { Math.max(1, nTaxa-2), Math.max(1, 2*nTaxa-2), Math.max(1, 2*nTaxa-2) };
	}
	
	
	public static void main(String[] args)
	{
		try
		{
			sop("START");	
			
			// Create nodes;
			int nNodes = 100;
			Vector<Node<String>> nodes = TreeBuilder.createStringPayloadNodes("Leaf", nNodes, 0);
			
			// Create matrix with pseudo-random distances. Capacity needs to be 2x the
			// number of original nodes.
			HalfArrayDistanceMatrix<Node<String>> distances = new HalfArrayDistanceMatrix<Node<String>>(nNodes*2);
			distances.randomize(nodes, 10, 100);
			NeighborJoiningTreeBuilder<String> treeBuilder = new NeighborJoiningTreeBuilder<String>(distances);
			Node<String> tree = treeBuilder.buildTree();
			sop(tree.toStringWithIndent());
		}
		catch (Exception x)
		{
			sop("Stress: " + x.getMessage());
			x.printStackTrace();
		}
		sop("DONE");
	}
}
