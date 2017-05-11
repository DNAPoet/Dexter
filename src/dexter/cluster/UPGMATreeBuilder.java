package dexter.cluster;

import java.util.*;

import dexter.cluster.*;
import dexter.cluster.tune.DualKeyDistanceMatrix;


public class UPGMATreeBuilder<P> extends TreeBuilder<P>
{
	// Originally a copy of the members of the distance matrix.
	private Collection<Node<P>>		unplacedNodes;
	private int						originalNUnplacedNodes;
	
	// Vars for iterating over nodes. More efficient then creating/destroying on the stack
	// on each pass through the main tree-building loop.
	protected	Node<P>				closestNode1;	 
	protected	Node<P>				closestNode2;
	
	
	public UPGMATreeBuilder(DistanceMatrix<Node<P>> distanceMatrix)
	{
		super(distanceMatrix, null);
		
		nextIntermediateNodeSN = distanceMatrix.nKeys() + 1;
		unplacedNodes = new HashSet<Node<P>>(distanceMatrix.keys());
		originalNUnplacedNodes = unplacedNodes.size();
	}
	
	
	// Destroys the distance matrix.
	public Node<P> buildTree()
	{
		fireClusteringStarted();
		
		startNewPhase(originalNUnplacedNodes);
		int nNodesPlaced = 0;
		
		while (unplacedNodes.size() > 1)
		{
			// Combine closest pair of unplaced nodes under a new parent.
			float dist = findClosestNodes(unplacedNodes);
			Node<P> newParent = new Node<P>();
			newParent.setName("C" + nextIntermediateNodeSN++);
			newParent.addKid(closestNode1, dist/2);
			newParent.addKid(closestNode2, dist/2);
			
			// Remove closest nodes from unplaced collection.
			unplacedNodes.remove(closestNode1);
			unplacedNodes.remove(closestNode2);
			
			// Compute distances from new parent to all other unplaced nodes.
			Vector<Node<P>> leavesFromNewParent = newParent.collectLeafNodes();
			for (Node<P> node: unplacedNodes)
			{
				dist = computeDistance(leavesFromNewParent, node.collectLeafNodes());
				distanceMatrix.setDistance(newParent, node, dist);
			}
			
			// Add the new parent to the collection of unplaced nodes.
			unplacedNodes.add(newParent);
			
			// Report progress if reached a milestone.
			reportPhaseProgress(++nNodesPlaced);
		}
		
		firePhaseFinished();
		fireClusteringFinished();
		
		// Root is the single remaining unplaced node.
		return unplacedNodes.iterator().next();
	}
	
	
	// Sets closestNode1 and closestNode2. Returns the distance between them.
	protected float findClosestNodes(Collection<Node<P>> unplacedNodes)
	{
		closestNode1 = closestNode2 = null;
		float shortestDistance = Float.MAX_VALUE;
		for (Node<P> n1: unplacedNodes)
		{
			for (Node<P> n2: unplacedNodes)
			{
				if (n1 == n2)
					continue;
				float dist = distanceMatrix.getDistance(n1, n2);
				if (dist < shortestDistance)
				{
					shortestDistance = dist;
					closestNode1 = n1;
					closestNode2 = n2;
				}
			}
		}
		return shortestDistance;
	}
	
	
	private float computeDistance(Collection<Node<P>> cluster1, Collection<Node<P>> cluster2)
	{
		float totalDistances = 0f;
		for (Node<P> n1: cluster1)
			for (Node<P> n2: cluster2)
				totalDistances += distanceMatrix.getDistance(n1, n2);
		return totalDistances / (cluster1.size() * cluster2.size());
	}
	
	
	// From Fall 2008 CS223 HW3P1.
	private static DistanceMatrix<Node<String>> buildTestMatrix()
	{
		DistanceMatrix<Node<String>> matrix = new DualKeyDistanceMatrix<Node<String>>();
		NodeTracker<String> tracker = new NodeTracker<String>(matrix);
		
		tracker.setDistance("Spinach", "Rice", 84.9f);
		tracker.setDistance("Spinach", "Mosquito", 105.6f);
		tracker.setDistance("Spinach", "Monkey", 90.8f);
		tracker.setDistance("Spinach", "Human", 86.3f);

		tracker.setDistance("Rice", "Mosquito", 117.8f);
		tracker.setDistance("Rice", "Monkey", 122.4f);
		tracker.setDistance("Rice", "Human", 122.6f);

		tracker.setDistance("Mosquito", "Monkey", 84.7f);
		tracker.setDistance("Mosquito", "Human", 80.8f);
		
		tracker.setDistance("Monkey", "Human", 3.3f);
		
		return matrix;
	}
	
	
	public static void main(String[] args)
	{
		try
		{
			DistanceMatrix<Node<String>> distances = buildTestMatrix();
			UPGMATreeBuilder<String> treeBuilder = new UPGMATreeBuilder<String>(distances);
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
