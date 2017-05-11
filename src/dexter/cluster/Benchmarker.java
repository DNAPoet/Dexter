package dexter.cluster;

import java.util.*;

import dexter.cluster.tune.*;



class Benchmarker implements ClusterProgressListener
{
	private final static int		MIN_RANDOM_DISTANCE		=  10;
	private final static int		MAX_RANDOM_DISTANCE		= 100;
	
	private ClusterAlgorithm 		clusterAlgorithm;
	private int						nNodes;
    private TreeBuilder<String> 	treeBuilder;
    private int						nNJReuses;				// if NJ, and reusing
    private float					njOversampleFactor;		// if NJ, and reusing
	private int						reportingInterval;
	private int[]					reportingIntervals;		// if null, use single reporting interval for all phases
	private ProgressReport			progressReport;
	private boolean					verbose;
	
	
	Benchmarker(ClusterAlgorithm clusterAlgorithm, 
			    int nNodes, 
			    int reportingInterval)
	{
		this.clusterAlgorithm = clusterAlgorithm;
		this.nNodes = nNodes;
		this.reportingInterval = reportingInterval;
	}	
	
	
	Benchmarker(ClusterAlgorithm clusterAlgorithm, 
			    int nNodes, 
			    int[] reportingIntervals)
	{
		this.clusterAlgorithm = clusterAlgorithm;
		this.nNodes = nNodes;
		this.reportingIntervals = reportingIntervals;
	}
	
	
	void execute()
	{
		// Create nodes.
		Vector<Node<String>> nodes = TreeBuilder.createStringPayloadNodes("Leaf_", nNodes, 0);

		// Create matrix with pseudo-random distances. Capacity needs to be 2x the
		// number of original nodes.
		HalfArrayDistanceMatrix<Node<String>> distances = new HalfArrayDistanceMatrix<Node<String>>(nNodes*2);
		distances.randomize(nodes, MIN_RANDOM_DISTANCE, MAX_RANDOM_DISTANCE);
	    
		// Create tree builder.
	    switch (clusterAlgorithm)
	    {
		    case NJ:
		    	treeBuilder = new NeighborJoiningTreeBuilder<String>(distances);
		    	break;
		    case NJ_REUSE:
		    	treeBuilder = new MonitoredReuseNJTreeBuilder<String>(distances, nNJReuses);
		    	break;
	    }
	    if (reportingIntervals != null)
	    	treeBuilder.setReportingIntervalsByPhase(reportingIntervals);
	    else
	    	treeBuilder.setReportingInterval(reportingInterval);
	    treeBuilder.addClusterProgressListener(this);
	    
	    // Progress report gets events before this object, so when this object receives
	    // events the progress report is valid.
	    progressReport = new ProgressReport(treeBuilder.getPhaseNames());
	    treeBuilder.addClusterProgressListener(progressReport);
	    
	    // Build tree.
	    treeBuilder.buildTree();
	}
	

	public void clusteringStarted(ClusterProgressEvent e)	
	{ 
		if (verbose)		
			sop("CLUSTERING STARTED");
	}
	
	
	public void phaseStarted(ClusterProgressEvent e)		
	{
		if (verbose)		
			sop("---------  PHASE STARTED: " + e.getPhaseName() + " at " + e.getTimestamp());
	}
	
	
	public void phaseProgressed(ClusterProgressEvent e)
	{
		if (verbose)		
			sop(progressReport);
	}
	
	
	public void phaseFinished(ClusterProgressEvent e) 
	{
		if (verbose)		
			sop("---------  PHASE FINISHED: " + e.getPhaseName() + " at " + e.getTimestamp());
		System.exit(1);
	}
	
	
	public void clusteringFinished(ClusterProgressEvent e)
	{
		if (verbose)		
			dsop("CLUSTERING FINISHED");
	}

	
	public void setNNJReuses(int nNJReuses)					
	{ 
		this.nNJReuses = nNJReuses; 
		if (treeBuilder != null  &&  treeBuilder instanceof MonitoredReuseNJTreeBuilder)
			((MonitoredReuseNJTreeBuilder<String>)treeBuilder).setNReuses(nNJReuses);
	}

	
	public void setNJOversampleFactor(float f)					
	{ 
		this.njOversampleFactor = f; 
		if (treeBuilder != null  &&  treeBuilder instanceof MonitoredReuseNJTreeBuilder)
			((MonitoredReuseNJTreeBuilder<String>)treeBuilder).setPreselectOversampleFactor(f);
	}
	
	
	public String toString()
	{
		String s = "Benchmarker for " + clusterAlgorithm + " on " + nNodes;
		if (clusterAlgorithm == ClusterAlgorithm.NJ_REUSE)
			s += ", " + nNJReuses + " reuses";
		return s;
	}
	
	
	public void setVerbose(boolean verbose)					{ this.verbose = verbose; }
	static void sop(Object x)								{ System.out.println(x); }	
	static void dsop(Object x)								{ sop(new Date() + ": " + x); }
	
	
	public static void main(String[] args)
	{
		try
		{
			dsop("START");
			int[] intervals = { 5, 5000, 100 };
			Benchmarker marker = new Benchmarker(ClusterAlgorithm.NJ, 10000, intervals);
			marker.setNNJReuses(100);		// only has effect on NJ with reuse
			marker.setNJOversampleFactor(10f);
			marker.setVerbose(true);
			sop(marker + "\n");
			marker.execute();
		}
		catch (Exception x)
		{
			sop("Stress: " + x.getMessage());
			x.printStackTrace();
		}
		dsop("DONE");
	}
}






/*
 * 

NJ, 5000 nodes:

Phase "Join neighbors" in progress at Sun Mar 16 13:28:00 PDT 2014, completed 5 of 4998 taxa, 0.088917345 per sec.
Phase "Join neighbors" in progress at Sun Mar 16 13:28:56 PDT 2014, completed 10 of 4998 taxa, 0.08939746 per sec.
Phase "Join neighbors" in progress at Sun Mar 16 13:29:52 PDT 2014, completed 15 of 4998 taxa, 0.08942838 per sec.
Phase "Join neighbors" in progress at Sun Mar 16 13:30:46 PDT 2014, completed 20 of 4998 taxa, 0.08991148 per sec.
Phase "Join neighbors" in progress at Sun Mar 16 13:31:40 PDT 2014, completed 25 of 4998 taxa, 0.09045877 per sec.
Phase "Join neighbors" in progress at Sun Mar 16 13:32:35 PDT 2014, completed 30 of 4998 taxa, 0.090620756 per sec.
Phase "Join neighbors" in progress at Sun Mar 16 13:33:29 PDT 2014, completed 35 of 4998 taxa, 0.09089162 per sec.
Phase "Join neighbors" in progress at Sun Mar 16 13:34:23 PDT 2014, completed 40 of 4998 taxa, 0.091042966 per sec.


NJ++, 5000 nodes, oversample at 3x
Phase "Join neighbors" in progress at Sun Mar 16 13:39:41 PDT 2014, completed 5 of 4998 taxa, 0.16877067 per sec.
Phase "Join neighbors" in progress at Sun Mar 16 13:40:00 PDT 2014, completed 10 of 4998 taxa, 0.20482989 per sec.
Phase "Join neighbors" in progress at Sun Mar 16 13:40:18 PDT 2014, completed 15 of 4998 taxa, 0.22429572 per sec.
Phase "Join neighbors" in progress at Sun Mar 16 13:40:37 PDT 2014, completed 20 of 4998 taxa, 0.23388256 per sec.
Phase "Join neighbors" in progress at Sun Mar 16 13:40:55 PDT 2014, completed 25 of 4998 taxa, 0.24092206 per sec.
SHORT EDGE SEL COLLECTED 46
Phase "Join neighbors" in progress at Sun Mar 16 13:41:23 PDT 2014, completed 30 of 4998 taxa, 0.2272142 per sec.
Phase "Join neighbors" in progress at Sun Mar 16 13:41:42 PDT 2014, completed 35 of 4998 taxa, 0.23234817 per sec.

NJ++, 10k nodes
SHORT EDGE SEL COLLECTED 52
Phase "Join neighbors" in progress at Sun Mar 16 14:08:02 PDT 2014, completed 5 of 9998 taxa, 0.03148932 per sec.
Phase "Join neighbors" in progress at Sun Mar 16 14:09:50 PDT 2014, completed 10 of 9998 taxa, 0.037518945 per sec.
Phase "Join neighbors" in progress at Sun Mar 16 14:11:42 PDT 2014, completed 15 of 9998 taxa, 0.039553206 per sec.
Phase "Join neighbors" in progress at Sun Mar 16 14:13:29 PDT 2014, completed 20 of 9998 taxa, 0.04120755 per sec.
Phase "Join neighbors" in progress at Sun Mar 16 14:15:18 PDT 2014, completed 25 of 9998 taxa, 0.041999724 per sec.

NJ, 10k nodes
Phase "Join neighbors" in progress at Sun Mar 16 14:22:12 PDT 2014, completed 5 of 9998 taxa, 0.020233331 per sec.
Phase "Join neighbors" in progress at Sun Mar 16 14:26:07 PDT 2014, completed 10 of 9998 taxa, 0.020741466 per sec.
Phase "Join neighbors" in progress at Sun Mar 16 14:29:57 PDT 2014, completed 15 of 9998 taxa, 0.02107514 per sec.
Phase "Join neighbors" in progress at Sun Mar 16 14:33:48 PDT 2014, completed 20 of 9998 taxa, 0.02120277 per sec.
Phase "Join neighbors" in progress at Sun Mar 16 14:37:41 PDT 2014, completed 25 of 9998 taxa, 0.0212574 per sec.
Phase "Join neighbors" in progress at Sun Mar 16 14:41:34 PDT 2014, completed 30 of 9998 taxa, 0.021300163 per sec.
Phase "Join neighbors" in progress at Sun Mar 16 14:45:27 PDT 2014, completed 35 of 9998 taxa, 0.021321572 per sec.
Phase "Join neighbors" in progress at Sun Mar 16 14:49:19 PDT 2014, completed 40 of 9998 taxa, 0.021347884 per sec.
*/
