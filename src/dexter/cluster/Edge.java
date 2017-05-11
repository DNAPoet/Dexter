package dexter.cluster;

import java.util.*;


//
// Models an edge, and also models a pair of nodes with known distance between them. The
// latter usage is for preselecting, when the n closest pairs of neighbors are determined.
//
// Only intended for use by clustering algorithms that need it, e.g. NJ. 
//


public class Edge<P> implements Comparable<Edge<P>>, Iterator<Node<P>>
{
	public Node<P> 				node1;
	public Node<P> 				node2;
	public float				length;
	public int 					iterIndex;
	
	public Edge(Node<P> node1, Node<P> node2, float length)
	{
		this.node1 = node1;
		this.node2 = node2;
		this.length = length;
	}
	
	
	public Node<P> otherNode(Node<P> node)
	{
		if (node == node1)
			return node2;
		else if (node == node2)
			return node1;
		assert false;
		return null;
	}
	
	
	public Iterator<Node<P>> nodeIterator()
	{
		iterIndex = 0;
		return this;
	}

	
	// The length criterion will take care of nearly all cases. When length is equal, toString() outputs
	// are compared; this is not meaningful but does the job of distinguishing two edges equal length.
	public int compareTo(Edge<P> that)	
	{ 
		double sig = Math.signum(this.length - that.length); 
		if (sig != 0d)
			return (int)sig;
		else
			return this.toString().compareTo(that.toString());
	}
	
	
	public boolean equals(Object x)
	{
		Edge<P> that = (Edge<P>)x;
		return this.compareTo(that) == 0;
	}
	
	
	public String toString()		{ return node1 + " __ " + length + " __ " + node2; 	  }
	public boolean hasNext()		{ return iterIndex < 2; 							  }
	public Node<P> next() 			{ return (iterIndex++ == 0)  ?  node1  :  node2;      }
	public void remove() 			{ }
}