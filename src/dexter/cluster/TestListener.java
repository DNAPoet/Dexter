package dexter.cluster;

import java.util.*;


class TestListener implements ClusterProgressListener
{
	private void test()
	{
		// Create nodes;
		int nNodes = 100;
		Vector<Node<String>> nodes = TreeBuilder.createStringPayloadNodes("Leaf", nNodes, 0);
		
		// Create matrix with pseudo-random distances. Capacity needs to be 2x the
		// number of original nodes.
		HalfArrayDistanceMatrix<Node<String>> distances = new HalfArrayDistanceMatrix<Node<String>>(nNodes*2);
		distances.randomize(nodes, 10, 100);
		NeighborJoiningTreeBuilder<String> treeBuilder = new NeighborJoiningTreeBuilder<String>(distances);
		treeBuilder.setReportingInterval(10);
		treeBuilder.addClusterProgressListener(this);
		treeBuilder.buildTree();
	}

	
	public void clusteringStarted(ClusterProgressEvent e) 	{ sop(e); }
	public void phaseStarted(ClusterProgressEvent e) 		{ sop(e); }
	public void phaseProgressed(ClusterProgressEvent e) 	{ sop(e); }
	public void phaseFinished(ClusterProgressEvent e) 		{ sop(e); }
	public void clusteringFinished(ClusterProgressEvent e) 	{ sop(e); }
	

	static void sop(Object x)		
	{
		System.out.println(x);
	}

	
	
	public static void main(String[] args)
	{
		sop("Starting");
		(new TestListener()).test();
		sop("Done");
	}
}
