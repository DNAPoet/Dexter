package dexter.cluster;

import java.util.*;
import java.util.concurrent.ThreadPoolExecutor.AbortPolicy;

import dexter.util.*;


//
// Handles progress reporting and listener notification. Listener collection is not threadsafe; for
// 100% safety, add all listeners before beginning clustering.
//


public abstract class TreeBuilder<P> implements ClusterProgressEventSource
{
	protected DistanceMatrix<Node<P>>		distanceMatrix;
	private ClusterAlgorithm				algorithm;
	
	
	// Original and generated nodes have serial numbers encoded in their names.
	protected int							nextIntermediateNodeSN;
	protected Set<ClusterProgressListener>	listeners;
	protected int							currentPhaseIndex = -1;
	protected int							nUnitsTotalCurrentPhase;
	protected int[]							reportingIntervalsByPhase;
	protected Vector<ClusterProgressEvent>	eventQueue;
	protected boolean						requestAbort;
	protected boolean						verbose;
	
	
	public TreeBuilder(DistanceMatrix<Node<P>> distanceMatrix, ClusterAlgorithm algorithm)
	{
		this.distanceMatrix = distanceMatrix;
		this.algorithm = algorithm;
		
		listeners = new HashSet<ClusterProgressListener>();
		eventQueue = new Vector<ClusterProgressEvent>();
		(new EventThread()).start();
	}
	
	
	public synchronized void addClusterProgressListener(ClusterProgressListener l)
	{
		listeners.add(l);
	}
	
	
	protected void fireClusteringStarted()
	{
		fireEvent(ClusterProgressEvent.createClusteringStartedEvent(this));
	}
	
	
	protected void firePhaseStarted()
	{
		fireEvent(ClusterProgressEvent.createPhaseStartedEvent(this, currentPhaseIndex));
	}
	
	
	protected void reportPhaseProgress(int nUnitsCompleted)
	{
		if (reportingIntervalsByPhase == null)
			return;
		int interval = reportingIntervalsByPhase[currentPhaseIndex];
		if ((nUnitsCompleted % interval) != 0)
			return;
		
		ClusterProgressEvent e = new ClusterProgressEvent(ClusterProgressEvent.EventType.PHASE_PROGRESSED, 
									 					  this, 
									 					  currentPhaseIndex, 
									 					  nUnitsCompleted, 
									 					  nUnitsTotalCurrentPhase);
		fireEvent(e);	
	}
	
	
	protected void firePhaseFinished()
	{		
		fireEvent(ClusterProgressEvent.createPhaseFinishedEvent(this, currentPhaseIndex));

	}
	
	
	protected void fireClusteringFinished()
	{
		fireEvent(ClusterProgressEvent.createClusteringFinishedEvent(this));
	}
	
	
	// Assume the event queue empties much faster than it fills. (If not, the reporting
	// intervals are inappropriate.) So no need for this method to wait on queue space.
	protected void fireEvent(ClusterProgressEvent e)
	{
		if (!doesReportProgress())
			return;
		
		synchronized (eventQueue)
		{
			eventQueue.add(e);
			eventQueue.notify();
		}
	}
	
	
	private class EventThread extends Thread
	{		
		public void run()
		{
			while (true)
			{
				synchronized (eventQueue)
				{
					while (eventQueue.isEmpty())
					{
						try
						{
							eventQueue.wait();			// fireEvent() notifies
						}
						catch (InterruptedException x)		{ }
					}
					ClusterProgressEvent e = eventQueue.remove(0);
					for (ClusterProgressListener listener: listeners)
						e.dispatchYourself(listener);
				}
			}
		}
	}  // End of inner class EventThread
	
	
	public void setReportingIntervalsByPhase(int[] intervals)
	{
		assert intervals.length == getPhaseNames().length;
		this.reportingIntervalsByPhase = intervals;
	}
	
	
	public void setReportingInterval(int interval)
	{
		reportingIntervalsByPhase = new int[getPhaseNames().length];
		for (int i=0; i< reportingIntervalsByPhase.length; i++)
			reportingIntervalsByPhase[i] = interval;
	}
	
	
	// Bumps the phase index, notifies listeners.
	protected void startNewPhase(int nUnitsForPhase)
	{
		// No need to do anything if not tracking progress. Do this here so that subclasses
		// don't need to check.
		if (!doesReportProgress())
			return;
		
		// Meez. Phase index is initially -1.
		String[] phases = getPhaseNames();
		assert currentPhaseIndex < phases.length  :
			"Can't step beyond phase #" + currentPhaseIndex + " = " + phases[currentPhaseIndex];
		currentPhaseIndex++;
		this.nUnitsTotalCurrentPhase = nUnitsForPhase;
		
		// Asynchronously notify listeners that a new phase started.
		firePhaseStarted();
	}
	
	
	public String[] getPhaseNames()
	{
		return algorithm.getPhaseNames();
	}
	
	
	public static Vector<Node<String>> createStringPayloadNodes(String prefix, int nNodes, int firstSN)
	{
		if (prefix == null)
			prefix = "";
		int lastSN = firstSN + nNodes - 1;
		int strlen = ("" + lastSN).length();
		Vector<Node<String>> nodes = new Vector<Node<String>>();
		for (int i=1; i<=nNodes; i++)
		{
			String payload = "" + i;
			while (payload.length() < strlen)
				payload = "0" + payload;
			payload = prefix + payload;
			Node<String> node = new Node<String>(payload);
			nodes.add(node);
		}
		return nodes;
	}
	
	
	// Subclasses should periodically check the abort-request flag and terminate early if set.
	public void requestAbort()
	{
		requestAbort = true;
	}


	abstract public Node<P> 						buildTree();
	
	
	protected boolean doesReportProgress()			{ return reportingIntervalsByPhase != null; }
	public ClusterAlgorithm getAlgorithm()			{ return algorithm; }	
	public int[] getReportingIntervalsByPhase()		{ return reportingIntervalsByPhase; }
	public void setVerbose(boolean verbose)			{ this.verbose = verbose; }
	protected static void sop(Object x)				{ System.out.println(x); }
}	
