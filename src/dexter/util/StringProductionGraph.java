package dexter.util;

import java.util.*;

import dexter.model.OrganismNameToKEGGAbbrevMap;


public class StringProductionGraph 
{
	private SPGNode		root;
	private SPGNode		terminator;
	
	
	public StringProductionGraph()
	{
		this(new HashSet<String>());
	}
	
	
	public StringProductionGraph(Collection<String> strings)
	{
		root = new SPGNode((char)-1);
		terminator = new SPGNode((char)-2);
		
		for (String s: strings)
			incorporate(s);
	}
	
	
	public void incorporate(String s)
	{
		char[] charArray = s.toCharArray();
		SPGNode currentNode = root;
		for (char ch: charArray)
		{
			// Ensure the current node has a child representing ch.
			SPGNode nextNode = currentNode.getChild(ch);
			if (nextNode == null)
				nextNode = currentNode.addChild(ch);
			currentNode = nextNode;
		}
		if (!currentNode.isTerminated())
			currentNode.children.add(terminator);
	}
	
	
	// Produces all strings that start with the prefix. Call with null or empty prefix to produce all strings.
	// Returns null if graph doesn't produce prefix.
	public Vector<String> produce(String prefix)
	{
		if (prefix == null)
			prefix = "";
		SPGNode prefixEndNode = getPathEndNode(prefix);
		if (prefixEndNode == null)
			// Prefix is not represented in this graph.
			return null;
		Vector<String> ret = new Vector<String>();
		produceRecurse(prefixEndNode, prefix, ret, false);
		return ret;
	}
	
	
	// On entry, prefix is the string represented by the path from root to node. Writes into vec all
	// paths from root through this node to a terminator node.
	private void produceRecurse(SPGNode node, String prefix, Vector<String> vec, boolean includeCurrentNode)
	{
		if (node == terminator)
			vec.add(prefix);
		
		else
		{
			assert !node.children.isEmpty();
			if (includeCurrentNode)
				prefix += node.chval;
			for (SPGNode child: node.children)
				produceRecurse(child, prefix, vec, true);
		}
	}
	
	
	private SPGNode getPathEndNode(String s)
	{
		SPGNode node = root;
		for (char ch: s.toCharArray())
		{
			node = node.getChild(ch);
			if (node == null)
				return null;
		}
		return node;		
	}
	
	
	// Returns the produced suffix.
	public String produceWhileNoDecisions(String prefix)
	{
		if (prefix == null)
			prefix = "";
		SPGNode node = getPathEndNode(prefix);
		if (node == null)			
			// Prefix is not represented in this graph.
			return null;
		String ret = "";
		while (node.nChildren() == 1)
		{
			node = node.children.iterator().next();
			if (node != terminator)
				ret += node.chval;
		}
		return ret;
	}	
	
	
	public int depth()
	{
		int n = 0;
		for (String production: produce(null))
			n = Math.max(n, production.length());
		return n;
	}
	
	
	private class SPGNode implements Comparable<SPGNode>
	{
		char			chval;
		Set<SPGNode>		children;
		
		SPGNode(char ch)
		{
			this.chval = ch;
			children = new TreeSet<SPGNode>();
		}
		
		public int compareTo(SPGNode that)
		{
			return this.chval - that.chval;
		}
		
		SPGNode getChild(char c)
		{
			for (SPGNode child: children)
				if (child.chval == c)
					return child;
			return null;
		}
		
		SPGNode addChild(char c)
		{
			SPGNode child = new SPGNode(c);
			children.add(child);
			return child;
		}
		
		int nChildren()
		{
			return children.size();
		}
		
		boolean isTerminated()
		{
			return children.contains(terminator);
		}
		
		String toShallowString()
		{
			if (this == root)
				return "ROOT";
			else if (this == terminator)
				return "TERMINATOR";
			else
				return "" + chval;
 		}
		
		public String toString()
		{
			if (this == root)
				return "ROOT";
			else if (this == terminator)
				return "TERMINATOR";
			else
			{
				String s = chval + " --> ";
				for (SPGNode child: children)
				{
					if (child == terminator)
						s += " ";
					s += child.toShallowString();
				}
				return s;
			}
		}
	}  // End of inner class Node
	
	
	// Some KEGG organisms end with parenthesized familiar names. Strip if present.
	public static StringProductionGraph forKEGGOrganisms()
	{
		OrganismNameToKEGGAbbrevMap map = OrganismNameToKEGGAbbrevMap.getInstance();
		Set<String> names = new HashSet<String>();
		for (String name: map.keySet())
		{
			if (name.endsWith(")"))
				name = name.substring(0, name.lastIndexOf("("));
			names.add(name);	
		}
		return new StringProductionGraph(names);
	}
	
	
	public static StringProductionGraph forKEGGAbbreviations()
	{
		OrganismNameToKEGGAbbrevMap map = OrganismNameToKEGGAbbrevMap.getInstance();
		return new StringProductionGraph(map.values());
	}
		
	
	static void sop(Object x)			{ System.out.println(x); }
	
	
	public static void main(String[] args)
	{				
		try
		{
			StringProductionGraph graph = forKEGGOrganisms();
			sop(graph.depth());
		}
		catch (Exception x)
		{
			sop("Stress: " + x.getMessage());
			x.printStackTrace();
		}
		finally
		{
			sop("DONE");
		}
	}
}
