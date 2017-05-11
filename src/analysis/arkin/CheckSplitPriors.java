package analysis.arkin;

import java.io.*;
import java.util.*;

import dexter.model.*;


public class CheckSplitPriors 
{
	private final static File							COREG_DIRF = new File("data/Coregulation");
	private       static Map<Study, HashSet<String>>	STUDY_TO_PRIOR_OPERON_GENES;

	static SessionModel 			session = getSession();
	
	
	static SessionModel getSession()
	{
		try
		{
			return SessionModel.deserialize(new File("data/Sessions/CPT.dex"));
		}
		catch (Exception x)
		{
			sop("Couldn't unpickle session: " + x.getMessage());
			System.exit(0);
		}
		return null;
	}
	
	
	static File getPriorsFileForStudy(Study study)
	{
		String suffix = study.getName().toUpperCase();
		suffix = suffix.substring(suffix.length()-3);
		for (String kid: COREG_DIRF.list())
			if (kid.contains(suffix))
				return new File(COREG_DIRF, kid);
		assert false;
		return null;
	}
	
	
	static void checkSplits(Vector<Operon> ops)
	{
		for (Operon op: ops)
		{
			if (op.size() < 5)
				continue;
			sop(op);
		}
	}

	
	
	static void sop(Object x)				{ System.out.println(x); }
	
	
	public static void main(String[] args)
	{
		try
		{
			sop("START");
			for (Study study: session.getStudies())
			{
				File f = getPriorsFileForStudy(study);
				Vector<Operon> ops = Operon.extractFromArkinPredictions(f);
				sop("*****************************");
				sop(study.getName() + ": " + ops.size() + " prior operons.");
			}
		}
		catch (Exception x)
		{
			sop("STRESS: " + x.getMessage());
			x.printStackTrace(System.out);
			System.exit(1);
		}
		sop("Done");
	}
}
