package dexter.cluster;

import java.util.*;
import dexter.model.Gene;


//
// Sends Phase Started/Progressed/Finished events, even though building a distance matrix doesn't quite fit the
// paradigm that cluster progress events were intended for. Notification is synchronous. If asynchronous notification
// is required, call buildDistanceMatrix() in a dedicated thread.
//


public class HalfArrayDistanceMatrixBuilder implements ClusterProgressEventSource
{
	private Metric									metric;
	private Map<Gene, Vector<float[]>>				geneToTxsZeroMean;
	private int										nTotalSteps;
	private int										reportingInterval;		// default = 5%
	private Set<ClusterProgressListener>			listeners = new HashSet<ClusterProgressListener>();
	private boolean									abortRequest;
	
	
	public HalfArrayDistanceMatrixBuilder(Metric metric, Map<Gene, Vector<float[]>> geneToTxsZeroMean)
	{	
		this.metric = metric;
		this.geneToTxsZeroMean = geneToTxsZeroMean;
		
		nTotalSteps = (geneToTxsZeroMean.size() * geneToTxsZeroMean.size()) / 2;
		reportingInterval = (int)(nTotalSteps * .05f);
		reportingInterval = Math.max(reportingInterval, 1);
	}
	
	
	public int getNTotalSteps()
	{
		return nTotalSteps;
	}
	
	
	public void	addClusterProgressListener(ClusterProgressListener l)
	{
		listeners.add(l);
	}
	
	
	public void setReportingInterval(int reportingInterval)
	{
		this.reportingInterval = reportingInterval;
	}
	
	
	public DistanceMatrix<Node<Gene>> buildDistanceMatrix()
	{	
		// Tell listeners that we started.
		ClusterProgressEvent e = new ClusterProgressEvent(ClusterProgressEvent.EventType.PHASE_STARTED, this);
		for (ClusterProgressListener l: listeners)
			e.dispatchYourself(l);
		
		// Metric size needs to be twice # of genes, to accommodate intermediate nodes.
		int size = (int)Math.ceil(2 * geneToTxsZeroMean.size());
		DistanceMatrix<Node<Gene>> matrix = new HalfArrayDistanceMatrix<Node<Gene>>(size);
		
		// Collect genes.
		Vector<Gene> genes = new Vector<Gene>(geneToTxsZeroMean.keySet());
		Vector<Node<Gene>> nodes = new Vector<Node<Gene>>();
		for (Gene gene: genes)
			nodes.add(new Node<Gene>(gene, gene.getBestAvailableName(), -12345));
		
		// Populate matrix.
		assert metric != null;
		int nElapsedSteps = 0;
		for (int i=0; i<nodes.size()-1; i++)
		{
			Node<Gene> inode = nodes.get(i);
			Gene igene = inode.getPayload();
			for (int j=i+1; j<nodes.size(); j++)
			{
				if (abortRequest)
					return null;
				Node<Gene> jnode = nodes.get(j);
				Gene jgene = jnode.getPayload();
				Vector<float[]> iTxs = geneToTxsZeroMean.get(igene);
				Vector<float[]> jTxs = geneToTxsZeroMean.get(jgene);
				float distance = 
					metric.computeDistance(igene, iTxs, jgene, jTxs);
				matrix.setDistance(inode, jnode, distance);	
				if (++nElapsedSteps % reportingInterval  ==  0)
				{
					// Tell listeners that we made progress.
					e = new ClusterProgressEvent(ClusterProgressEvent.EventType.PHASE_PROGRESSED, 
												 this, 
												 -1, 
												 nElapsedSteps, 
												 nTotalSteps);
					for (ClusterProgressListener l: listeners)
						e.dispatchYourself(l);
				}
			}
		}
		
		// Tell listeners that we started.
		e = new ClusterProgressEvent(ClusterProgressEvent.EventType.PHASE_FINISHED, this);
		for (ClusterProgressListener l: listeners)
			e.dispatchYourself(l);
		
		return matrix;
	}


	public ClusterAlgorithm getAlgorithm() 
	{
		return null;
	}
	
	
	public void requestAbort()
	{
		abortRequest = true;
	}
	
	
	static void sop(Object x)			{ System.out.println(x); }
}
