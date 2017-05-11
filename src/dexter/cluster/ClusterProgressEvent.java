package dexter.cluster;

import java.util.Date;


public class ClusterProgressEvent 
{
	public enum EventType
	{
		CLUSTERING_STARTED, PHASE_STARTED, PHASE_PROGRESSED, PHASE_FINISHED, CLUSTERING_FINISHED;
	}
	
	
	private EventType					type;
	private Date						timestamp = new Date();
	private ClusterProgressEventSource	source;
	private int							currentPhaseIndex;
	private int							nUnitsCompletedThisPhase;
	private int							totalUnitsThisPhase;
	
	
	public ClusterProgressEvent(EventType type, ClusterProgressEventSource source)
	{
		this.type = type;
		this.source = source;
	}
	
	
	public ClusterProgressEvent(EventType type,
								ClusterProgressEventSource source,
							    int currentPhaseIndex,
							    int nUnitsCompletedThisPhase,
							    int totalUnitsThisPhase)
	{
		this(type, source);
		assert nUnitsCompletedThisPhase <= totalUnitsThisPhase;
		this.currentPhaseIndex = currentPhaseIndex;
		this.nUnitsCompletedThisPhase = nUnitsCompletedThisPhase;
		this.totalUnitsThisPhase = totalUnitsThisPhase;
	}
	
	
	public static ClusterProgressEvent createClusteringStartedEvent(ClusterProgressEventSource src)
	{
		return new ClusterProgressEvent(EventType.CLUSTERING_STARTED, src);
	}
	
	
	public static ClusterProgressEvent createClusteringFinishedEvent(ClusterProgressEventSource src)
	{
		return new ClusterProgressEvent(EventType.CLUSTERING_FINISHED, src);
	}
	
	
	public static ClusterProgressEvent createPhaseStartedEvent(ClusterProgressEventSource src, int phaseIndex)
	{
		ClusterProgressEvent e = new ClusterProgressEvent(EventType.PHASE_STARTED, src);
		e.currentPhaseIndex = phaseIndex;
		return e;
	}
	
	
	public static ClusterProgressEvent createPhaseFinishedEvent(ClusterProgressEventSource src, int phaseIndex)
	{
		ClusterProgressEvent e = new ClusterProgressEvent(EventType.PHASE_FINISHED, src);
		e.currentPhaseIndex = phaseIndex;
		return e;
	}
	
	
	public String toString()
	{
		String s = "ClusterProgressEvent " + type + " at " + timestamp + " for phase #" + currentPhaseIndex + ". ";
		if (nUnitsCompletedThisPhase > 0  &&  totalUnitsThisPhase > 0)
			s += nUnitsCompletedThisPhase + " of " + totalUnitsThisPhase + " work units completed.";
		return s;
	}
	
	
	public void dispatchYourself(ClusterProgressListener l)
	{
		switch (type)
		{
			case CLUSTERING_STARTED:
				l.clusteringStarted(this);
				break;
			case PHASE_STARTED:
				l.phaseStarted(this);
				break;
			case PHASE_PROGRESSED:
				l.phaseProgressed(this);
				break;
			case PHASE_FINISHED:
				l.phaseFinished(this);
				break;
			case CLUSTERING_FINISHED:
				l.clusteringFinished(this);
				break;
			default:
				assert false;
		}
	}
	
	
	public EventType getType()						{ return type; }	
	public ClusterProgressEventSource getSource()	{ return source; }
	public Date getTimestamp()						{ return timestamp; }
	public int getPhaseIndex()						{ return currentPhaseIndex; }
	public String getPhaseName()					{ return getSource().getAlgorithm().getPhaseNames()[currentPhaseIndex]; }
	public int[] getCompletion()					{ return new int[]{ nUnitsCompletedThisPhase, totalUnitsThisPhase }; }
}
