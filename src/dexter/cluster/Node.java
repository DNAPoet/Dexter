package dexter.cluster;

import java.io.File;
import java.util.*;

import dexter.util.*;


//
// The name field supports input from Newick format where node names are provided. For tree building,
// interior nodes created by the tree algorithm are given sequential names for debugging, while the
// important node information (e.g. dataset/organism/gene for an expression measurement) is available
// in the payload.
//


public class Node<P>
{
	P					payload;
	String				name;
	float				distToParent;
	Node<P>				parent;
	Vector<Node<P>>		kids = new Vector<Node<P>>();
	
	
	public Node()		{ }
	
	
	public Node(P payload)
	{ 
		this(payload, null, 0f);
	}
	
	
	public Node(String name, float distToParent)
	{
		this(null, name, distToParent);
	}
	
	
	public Node(P payload, String name, float distToParent)
	{
		this.payload = payload;
		this.name = name;
		this.distToParent = distToParent;
	}
	
	
	public void addKid(Node<P> kid)
	{
		kids.add(kid);
		kid.parent = this;
	}
	
	
	public void addKid(Node<P> kid, float distance)
	{
		kid.distToParent = distance;
		kids.add(kid);
	}
	
	
	public Vector<Node<P>> collectLeafNodes()
	{
		Vector<Node<P>> vec = new Vector<Node<P>>();
		collectLeafNodesRecurse(vec);
		return vec;
	}
	
	
	// Populates the vector.
	private void collectLeafNodesRecurse(Vector<Node<P>> vec)
	{
		if (isLeaf())
			vec.add(this);
		else
			for (Node<P> kid: kids)
				kid.collectLeafNodesRecurse(vec);
	}
	
	
	public Set<Node<P>> collectDescendants()
	{
		Set<Node<P>> ret = new HashSet<Node<P>>();
		for (Node<P> kid: kids)
		{
			ret.add(kid);
			ret.addAll(kid.collectDescendants());
		}
		return ret;
	}
	
	
	public Node<P> getFirstLeafChild()
	{
		Node<P> n = this;
		while (!n.isLeaf())
			n = n.kids.firstElement();
		return n;
	}
	
	
	public Node<P> getLastLeafChild()
	{
		Node<P> n = this;
		while (!n.isLeaf())
			n = n.kids.lastElement();
		return n;
	}
	
	
	public Vector<P> collectLeafPayloads()
	{
		Vector<Node<P>> nodes = collectLeafNodes();
		Vector<P> payloads = new Vector<P>(nodes.size());
		for (Node<P> node: nodes)
			payloads.add(node.getPayload());
		return payloads;
	}
	
	
	// This node doesn't have to be the root. It can be intermediate, if you want to print the subtree.
	public String toNewickStringForRoot()
	{
		String s = this.toNewickString();
		int n = s.lastIndexOf(':');
		s = s.substring(0, n);
		s += ";";
		return s;
	}
	
	
	public String toNewickString()
	{
		if (isLeaf())
		{
			String newickName = name;
			if (payload != null  &&  payload instanceof NewickNodeNameProvider)
				newickName = ((NewickNodeNameProvider)payload).getNewickName();
			return newickName + ":" + distToParent;
		}
		
		String s = "(";
		for (Node<P> kid: kids)
			s += kid.toNewickString() + ",";
		s = s.substring(0, s.length()-1);
		s += "):" + distToParent;
		return s;
	}
	
	
	public String toStringWithIndent()
	{
		String s = "ROOT: " + toString();
		for (Node<P> kid: kids)
			s += '\n' + kid.toStringWithIndent("  ");
		return s;
	}
	
	
	private String toStringWithIndent(String indent)
	{
		String s = indent + toString() + " @ " + distToParent;
		for (Node<P> kid: kids)
			s += '\n' + kid.toStringWithIndent(indent + "  ");
		return s;
	}
	

	public String toString()					
	{ 
		if (name != null)
			return name;
		else if (payload != null)
			return payload.toString();
		else
			return "NO NAME";
	}
	
	
	
	
	
	
				
				
	
				////////////////////////////////////////////////////////////////
				//                                                            //
				//                        REROOTING                           //
				//                                                            //
				////////////////////////////////////////////////////////////////
				
	
	
	
	// Supports rerooting at old root (identity operation) and at a leaf node. For deployment,
	// gui should ensure that new root is always intermediate.
	public void reroot(Node<P> newRoot)
	{
		assert this.isRoot();
		
		if (newRoot == this)
			return;
		
		// Compute step distances of all nodes from the new root.
		Map<Node<P>, Integer> stepDistancesFromNewRoot = computeAllStepDistancesFromNewRoot(newRoot);
		
		// Determine new parent-child relationships.
		Map<Node<P>, Node<P>> nodeToNewParent = new HashMap<Node<P>, Node<P>>();
		Map<Node<P>, Float> nodeToBranchLenToParent = new HashMap<Node<P>, Float>();
		Map<Node<P>, Vector<Node<P>>> nodeToNewKids = new HashMap<Node<P>, Vector<Node<P>>>();
		for (Node<P> node: stepDistancesFromNewRoot.keySet())
		{
			int distOfThisNode = stepDistancesFromNewRoot.get(node);
			// Collect neighbors (parent and kids).
			Vector<Node<P>> neighbors = new Vector<Node<P>>();
			if (node.parent != null)
				neighbors.add(node.parent);
			neighbors.addAll(node.kids);
			int nParents = 0;
			// Determine new parent (it's 1 step closer to the new root).
			Node<P> newParent = null;
			for (Node<P> neighbor: neighbors)
			{			
				int distOfNeighbor = stepDistancesFromNewRoot.get(neighbor);
				assert Math.abs(distOfThisNode-distOfNeighbor) == 1;
				if (distOfNeighbor < distOfThisNode)
				{
					// Neighbor is parent.
					nParents++;
					newParent = neighbor;
				}
			}
			assert nParents == 0  ||  nParents == 1;
			if (nParents == 0)
				assert node == newRoot;
			nodeToNewParent.put(node, newParent);
			// Remaining neighbors are the new kids.
			if (newParent != null)
				neighbors.remove(newParent);
			nodeToNewKids.put(node, neighbors);
			// If parent changed, then branch length to parent changes as well.
			if (newParent != null  &&  newParent != node.parent)
				nodeToBranchLenToParent.put(node, newParent.distToParent);
		}
		
		// Change nodes to reflect new parent and kids.
		for (Node<P> node: nodeToNewParent.keySet())
		{
			node.parent = nodeToNewParent.get(node);
			node.kids = nodeToNewKids.get(node);
		}
	}
	
	
	
	private Map<Node<P>, Integer> computeAllStepDistancesFromNewRoot(Node<P> center)
	{
		assert this.isRoot();
		assert !center.isRoot();

		Map<Node<P>, Integer> ret = new HashMap<Node<P>, Integer>();
		
		// Center is the only member of 0th generation.
		ret.put(center, 0);
		Set<Node<P>> lastGeneration = new HashSet<Node<P>>();
		lastGeneration.add(center);
		
		// Extend outward from last generation.
		int generationNum = 1;
		while (!lastGeneration.isEmpty())
		{
			// Add any non-redundant parent or kid of any member of the last generation.
			Set<Node<P>> nextGeneration = new HashSet<Node<P>>();
			for (Node<P> lastGenNode: lastGeneration)
			{
				// Parent.
				Node<P> parent = lastGenNode.parent;
				if (parent != null  &&  !ret.containsKey(parent))
				{
					ret.put(parent, generationNum);
					nextGeneration.add(parent);
				}
				// Kids.
				for (Node<P> kid: lastGenNode.kids)
				{
					if (!ret.containsKey(kid))
					{
						ret.put(kid, generationNum);
						nextGeneration.add(kid);
					}
				}
			}
			generationNum++;
			lastGeneration = nextGeneration;
		}
		
		return ret;
	}
	
	
	

	
	
	
	
	
	
	
	
					
					////////////////////////////////////////////////////////////////
					//                                                            //
					//                          MISC                              //
					//                                                            //
					////////////////////////////////////////////////////////////////
					
					
	
	// Returns { # nodes, # orphans }. A correct tree has only 1 orphan (the root).
	public int[] countNodesAndOrphans()
	{
		int[] ret = new int[2];
		recurseCountNodesAndOrphans(ret);
		return ret;
	}
	
	
	private void recurseCountNodesAndOrphans(int[] counts)
	{
		counts[0]++;
		if (parent == null)
			counts[1]++;
		for (Node<P> kid: kids)
			kid.recurseCountNodesAndOrphans(counts);
	}
	
	
	// For newick parsers and clustering algorithms that don't set the parent field of all nodes.
	public void setParentFields()
	{
		recurseSetParentFieldsOfKids();
	}
	
	
	private void recurseSetParentFieldsOfKids()
	{
		for (Node<P> kid: kids)
		{
			kid.parent = this;
			kid.recurseSetParentFieldsOfKids();
		}
	}
	
	
	
	public void setName(String name)		{ this.name = name;			 }
	public boolean isRoot()					{ return parent == null;     }
	public boolean isLeaf()					{ return kids.isEmpty();     }
	public float getDistanceToParent()		{ return distToParent;       }
	public Vector<Node<P>> getKids()		{ return kids;			     }
	public P getPayload()					{ return payload;			 }
	public void setPayload(P payload)		{ this.payload = payload;    }
	public String getName()					{ return name; 		 		 }
	public Node<P> getParent()				{ return parent;		     }
	public static void sop(Object x)		{ System.out.println(x);     }
	
	
	public static void main(String[] args)
	{
		try
		{
			// String s = "((A:5, B:5):9);";
			String s = "(  (A:5, B:5):9,   ((C:8,D:8):2, E:10):4  );";
			NewickParser<String> parser = new NewickParser<String>(s, new NewickPayloadBuilderStringIdentity());
			Node<String> oldRoot = parser.parse();
			oldRoot.setPayload("##OLDROOT##");
			int nnn = 0;
			for (Node<String> node: oldRoot.collectDescendants())
				if (node.getPayload() == null)
					node.setPayload("**" + nnn++ + "**");
			Node<String> newRoot = oldRoot.kids.firstElement();
			sop(oldRoot.toStringWithIndent() + "\n$$$$$$$$$$$$$$$$$$$$$");
			oldRoot.reroot(newRoot);
			sop(newRoot.toStringWithIndent());
		}
		catch (Exception x)
		{
			sop("Stress: " + x.getMessage());
			x.printStackTrace();
		}
		sop("DONE");
	}
}
