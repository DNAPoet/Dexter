package dexter.model;

import java.util.*;


//
// Useful for debugging.
//


public class StudyToTimeAssignmentMap extends HashMap<Study, TimeAssignmentMap>
{
	private boolean			verbose;
	
	
	public StudyToTimeAssignmentMap()			
	{ 
		if (verbose)
		{
			sop("**********\nStudyToTimeAssignmentMap default ctor");
			(new Exception()).printStackTrace(System.out);
		}
	}
	
	
	public StudyToTimeAssignmentMap(Map<Study, TimeAssignmentMap> src)
	{
		if (verbose)
		{
			sop("**********\nStudyToTimeAssignmentMap ctor from a source");
			(new Exception()).printStackTrace(System.out);
		}
		
		for (Study study: src.keySet())
			put(study, src.get(study));
	}
	
	
	public TimeAssignmentMap put(Study study, TimeAssignmentMap tam)
	{
		if (verbose)
			sop("---------\nTimeAssignmentMap.put(), study=\n" + study);
		return super.put(study, tam);
	}
	
	
	public void setVerbose(boolean verbose)		{ this.verbose = verbose; }	
	static void sop(Object x)					{ System.out.println(x);  }
}
