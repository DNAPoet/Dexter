package dexter.cluster;

import java.util.*;


//
// Maps payloads to nodes for tree building. In deployment, payloads will be genes and their
// expression profiles. Optionally can manage a distance matrix, supporting access by payload.
//


public class NodeTracker<P> extends HashMap<P, Node<P>>
{
	private DistanceMatrix<Node<P>>			distanceMatrix;
	
	
	public NodeTracker()					{ }
	
	
	public NodeTracker(DistanceMatrix<Node<P>> distanceMatrix)
	{
		this.distanceMatrix = distanceMatrix;
	}
	
	
	public Node<P> getNode(P payload)
	{
		Node<P> node = get(payload);
		if (node == null)
		{
			node = new Node<P>(payload);
			node.setName(payload.toString());
			put(payload, node);
		}
		return node;
	}
	
	
	public float getDistance(P p1, P p2)
	{
		return distanceMatrix.getDistance(getNode(p1), getNode(p2));
	}
	
	
	public void setDistance(P p1, P p2, float distance)
	{
		distanceMatrix.setDistance(getNode(p1), getNode(p2), distance);
	}
}
