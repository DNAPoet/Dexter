package dexter.cluster;

import java.util.Date;


public class ProgressReport implements ClusterProgressListener
{
	private String[]			phaseNames;
	private Date[]				startTimes;	
	private Date[]				latestReportTimes;
	private boolean[]			completedPhases;
	private int					currentPhaseIndex;
	private int[]				completedUnitsByPhase;		// only valid up to current phase
	private int[]				totalUnitsByPhase;
	private boolean				clusteringStarted;
	private boolean				clusteringFinished;
	
	
	public ProgressReport(String[] phaseNames)
	{
		int nPhases = phaseNames.length;
		
		phaseNames = new String[nPhases];
		startTimes = new Date[nPhases];
		latestReportTimes = new Date[nPhases];
		completedPhases = new boolean[nPhases];
		completedUnitsByPhase = new int[nPhases];
		totalUnitsByPhase = new int[nPhases];
	}
	
	
	public String toString()
	{
		String s = "";
		for (int i=0; i<=currentPhaseIndex; i++)
		{
			s += "\nPhase \"" + phaseNames[i] + "\" "; 
			if (completedPhases[i])
			{
				// Completed phase.
				s += "completed at " + latestReportTimes[i] + ": " + totalUnitsByPhase[i] + " units, " + 
						unitsPerSec(i) + " per sec.";
			}
			else
			{
				// Phase in progress.
				float ups = unitsPerSec(i);
				if (ups < 0f)
					s+= "... not started yet";
				else
					s += "in progress at " + latestReportTimes[i] + ", completed " + completedUnitsByPhase[i] +
						" of " +  + totalUnitsByPhase[i] + " units, " + ups + " per sec.";
			}
		}
		
		return s.substring(1);
	}
	
	
	public float unitsPerSec(int phaseIndex)
	{
		assert latestReportTimes != null  :  "null latestReportTimes";
		assert startTimes != null  :  "null startTimes";
		assert latestReportTimes[phaseIndex] != null  :  "null latest report time for phase " + phaseIndex;
		assert startTimes[phaseIndex] != null  :  "null start time for phase " + phaseIndex;
		
		long deltaTMillis = latestReportTimes[phaseIndex].getTime() - startTimes[phaseIndex].getTime();
		if (deltaTMillis == 0)
			return -12345.678f;
		float deltaTSecs = deltaTMillis / 1000f;
		//String s = latestReportTimes[0] + " = " + latestReportTimes[0].getTime() + "\n";
		//s += startTimes[0] + " = " + startTimes[0].getTime() + "\n";
		//s += "delta millis = " + deltaTMillis + ", #units=" + completedUnitsByPhase[0];
		//assert false:s;
		return completedUnitsByPhase[phaseIndex] / deltaTSecs;
	}
	
	
	public void clusteringStarted(ClusterProgressEvent e) 	
	{
		assert !clusteringStarted  :  "Started cluster job sent extra STARTED event";
		clusteringStarted = true;
	}
	
	
	public void phaseStarted(ClusterProgressEvent e) 		
	{
		currentPhaseIndex = e.getPhaseIndex();
		latestReportTimes[currentPhaseIndex] = startTimes[currentPhaseIndex] = e.getTimestamp();
	}
	
	
	public void phaseProgressed(ClusterProgressEvent e)
	{
		assertPhaseIndexIntegrity(e);
		
		completedUnitsByPhase[currentPhaseIndex] = e.getCompletion()[0];
		if (totalUnitsByPhase[currentPhaseIndex] == 0)
			totalUnitsByPhase[currentPhaseIndex] = e.getCompletion()[1];
		else
			assert e.getCompletion()[1] == totalUnitsByPhase[currentPhaseIndex];
		
		latestReportTimes[currentPhaseIndex] = e.getTimestamp();
	}
	
	
	public void phaseFinished(ClusterProgressEvent e) 
	{
		assertPhaseIndexIntegrity(e);
		
		latestReportTimes[currentPhaseIndex] = e.getTimestamp();
		completedPhases[currentPhaseIndex] = true;
	}
	
	
	public void clusteringFinished(ClusterProgressEvent e) 	
	{		
		clusteringFinished = true;
	}
	
	
	private void assertPhaseIndexIntegrity(ClusterProgressEvent e)
	{
		assert e.getPhaseIndex() == currentPhaseIndex  :  
			"Unexpected phase index: saw " + e.getPhaseIndex() + ", expected " + currentPhaseIndex;
	}
	

	public Date getStartTime(int phaseIndex)			{ return startTimes[phaseIndex]; }
	public Date getStartTimeCurrentPhase()				{ return startTimes[currentPhaseIndex]; }
	public Date getLatestReportTime(int phaseIndex)		{ return latestReportTimes[phaseIndex]; }
	public Date getLatestReportTimeCurrentPhase()		{ return latestReportTimes[currentPhaseIndex]; }
	public boolean clusteringStarted()					{ return clusteringStarted; }
	public boolean clusteringFinished()					{ return clusteringFinished; }
	static void sop(Object x)							{ System.out.println(x); }
}
