package dexter.cluster;

import java.util.*;

import dexter.cluster.tune.MonitoredReuseNJTreeBuilder;


//
// Collects up to targetSize disjoint node pairs from nodes, and returns them as a collection of edges.
// The edges don't necessarily represent edges in a graph; the Edge class is subverted because it
// conveniently encapsulates 2 nodes and the distance between them.
//
// A greedy algorithm won't work. A node in the returned collection may only be represented once. Suppose
// the algorithm has collected n1-n2. Then it finds shorter edge n2-n3. It collects n2-n3, discarding
// n1-n2 because of the overlap of n2. Then it finds even shorter edge n3-n4, which is collected at the
// expense of n2-n3. Now there is no longer any overlap with n1-n2, which perhaps ought to be part of the
// returned collection but has been permanently discarded.
//
// Rather than using the greedy approach, this algorithm collects more edges than it needs and then retains
// a disjoint set.
//

public class ShortEdgeSelector<P>
{
	private final static float				DFLT_OVERSAMPLING_FACTOR			= 3f;
	
	private int								targetSize;
	private Collection<Node<P>>				nodes;
	private DistanceMatrix<Node<P>> 		distances;	
	private Map<Node<P>, Float>				nodeToR;
	private float 							oversamplingFactor = DFLT_OVERSAMPLING_FACTOR;
	private TreeSet<Edge<P>>				bestEdges;
	
	
	// For testing, have this object compute the node-to-r map.
	private ShortEdgeSelector(int targetSize, 
					  			 Collection<Node<P>> nodes, 
					  			 DistanceMatrix<Node<P>> distances)
	{
		this(targetSize, nodes, distances, null);
	}
	

	// For deployment, caller has already computed node-to-r map.
	public ShortEdgeSelector(int targetSize, 
					  		 Collection<Node<P>> nodes, 
					  		 DistanceMatrix<Node<P>> distances,
					  		 Map<Node<P>, 
					  		 Float> nodeToR)
	{
		this.targetSize = targetSize;
		this.nodes = nodes;
		this.distances = distances;
		
		if (nodeToR == null)
			this.nodeToR = computeRForAllNodes(nodes);
		else
			this.nodeToR = nodeToR;
	}
	
	
	public void setOversamplingFactor(float oversamplingFactor)
	{
		this.oversamplingFactor = oversamplingFactor;
	}
	
	
	public Vector<Edge<P>> selectShortestEdges()
	{
		int oversampleSize = (int)(targetSize * oversamplingFactor);
		sop(oversampleSize + " = oversampleSize");
		
		// Oversample.
		TreeSet<Edge<P>> oversample = new TreeSet<Edge<P>>();
		for (Node<P> n1: nodes)
		{
			for (Node<P> n2: nodes)
			{
				if (n1 == n2)
					continue;
				Edge<P> edge = new Edge<P>(n1, n2, bigD(n1, n2));
				oversample.add(edge);
				if (oversample.size() > oversampleSize)
				{
					oversample.pollLast();
				}
			}
		}
		
		// Reduce sample, greedily enforcing the rule that a node may only appear once.
		Set<Node<P>> returnNodes = new HashSet<Node<P>>();
		Vector<Edge<P>> ret = new Vector<Edge<P>>(targetSize);
		for (Edge<P> edge: oversample)
		{
			if (returnNodes.contains(edge.node1)  ||  returnNodes.contains(edge.node2))
				continue;
			ret.add(edge);
			if (ret.size() == targetSize)
				return ret;
			returnNodes.add(edge.node1);
			returnNodes.add(edge.node2);
		}
		sop("SHORT EDGE SEL COLLECTED " + ret.size());
		return ret;
	}
	
	
	//
	// Defined as D-sub-ij on p. 170 of Durbin Eddy & al. Note capital D, versus d-sub-ij, which is the 
	// distance provided in the table. The NJ distance is the distance in the table, minus the average 
	// distances ("r") from nodes i and j to all other nodes. This metric can be negative; it is only used 
	// for picking nearest neighbors.
	//
	private final float bigD(Node<P> node1, Node<P> node2)
	{
		float r1 = nodeToR.get(node1);
		float r2 = nodeToR.get(node2);
		return distances.getDistance(node1, node2) - r1 - r2;
	}		
	
	
	// Defined as r-sub-i at the top of p. 171. Only needed for testing.
	private float r(Node<P> node, Collection<Node<P>> allNodes)
	{
		assert allNodes.size() > 2;
		
		float r = 0f;
		for (Node<P> nodeK: allNodes)
			if (nodeK != node)
				r += distances.getDistance(node, nodeK);
		r /= (nodes.size() - 2f);
		return r;
	}
	
	
	private Map<Node<P>, Float> computeRForAllNodes(Collection<Node<P>> allNodes)
	{
		HashMap<Node<P>, Float> ret = new HashMap<Node<P>, Float>();
		for (Node<P> node: allNodes)
			ret.put(node, r(node, allNodes));		
		return ret;
	}
	
	
	static void sop(Object x)			{ System.out.println(x); }
	
	
	public static void main(String[] args)
	{
		try
		{
			sop("START");	
			
			// Create nodes;
			int nNodes = 5000;
			Vector<Node<String>> nodes = TreeBuilder.createStringPayloadNodes("Leaf_", nNodes, 0);
			
			// Create matrix with pseudo-random distances. Capacity needs to be 2x the number of original nodes.
			HalfArrayDistanceMatrix<Node<String>> distances = new HalfArrayDistanceMatrix<Node<String>>(nNodes*2);
			distances.randomize(nodes, 10, 100);
			
			// Create uut.
			int targetSize = 15;
			ShortEdgeSelector<String> that = new ShortEdgeSelector<String>(targetSize, nodes, distances);
			that.setOversamplingFactor(5500);
			
			// Run.
			Vector<Edge<String>> bestEdges = that.selectShortestEdges();
			for (Edge<String> e: bestEdges)
				sop(e);
			sop("At oversample=" + that.oversamplingFactor + "x, found " + bestEdges.size());
		}
		catch (Exception x)
		{
			sop("Stress: " + x.getMessage());
			x.printStackTrace();
		}
		sop("DONE");
	}
}

/*

nNodes = 5000:


SHORT EDGE SEL COLLECTED 14 of !5
Leaf_0896 __ -102.34326 __ Leaf_2367
Leaf_0111 __ -101.87314 __ Leaf_1569
Leaf_3536 __ -101.831085 __ Leaf_4897
Leaf_2776 __ -101.79896 __ Leaf_2987
Leaf_3842 __ -101.783966 __ Leaf_4333
Leaf_0126 __ -101.77786 __ Leaf_1207
Leaf_3128 __ -101.77275 __ Leaf_1383
Leaf_1699 __ -101.76517 __ Leaf_1851
Leaf_2477 __ -101.71163 __ Leaf_4347
Leaf_0492 __ -101.70303 __ Leaf_4233
Leaf_0057 __ -101.6801 __ Leaf_0659
Leaf_2641 __ -101.67635 __ Leaf_4564
Leaf_2727 __ -101.674545 __ Leaf_4039
Leaf_1968 __ -101.63979 __ Leaf_4482

At oversample=3.0x, found 14
At oversample=10.0x, found 15
At oversample=20.0x, found 15
At oversample=100.0x, found 15
At oversample=1000.0x, found 15
At oversample=5500.0x, found 15



*/
