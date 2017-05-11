package analysis.arkin;

import java.io.*;
import java.util.*;

import dexter.coreg.*;
import dexter.model.*;


//
// A candidate is 2 consecutive predicted operons of size 2. Later, qualification steps will eliminate some candidates 
// based on strand, intervening genes, and probability rules.
//


class MergeCandidateFinder 
{
	private Study			study;
	
	
	MergeCandidateFinder(Study study)
	{
		this.study = study;
	}
	
	
	// All Operon[]s are size=2.
	Vector<Operon[]> collectCandidateOperonPairs() throws IOException
	{
		Vector<Operon[]> ret = new Vector<Operon[]>();
		
		CoregulationFile coregFile = new CoregulationFile(study.getOrganism());
		Vector<CoregulationGroup> coregGps = coregFile.getCoregulationGroups();
		CoregulationGroup lastCoregGp = null;
		for (CoregulationGroup currentCoregGp: coregGps)
		{
			if (lastCoregGp != null)
			{
				if (lastCoregGp.size() == 2  &&  currentCoregGp.size() == 2)
				{
					Operon op1 = new Operon(lastCoregGp);
					Operon op2 = new Operon(currentCoregGp);
					ret.add(new Operon[] { op1, op2 });
				}
			}
			lastCoregGp = currentCoregGp;
			
		}
		return ret;
	}
	
	
	static void sop(Object x)			{ System.out.println(x); }
	
	
	public static void main(String[] args)
	{
		try
		{
			sop("START");
			SessionModel session = SessionModel.deserialize(new File("data/Sessions/CPT.dex"));
			for (Study study: session.getStudies())
			{
				sop("Study = " + study.getName());
				MergeCandidateFinder finder = new MergeCandidateFinder(study);
				Vector<Operon[]> candidates = finder.collectCandidateOperonPairs();
				sop("# original candidates = " + candidates.size());
				MergeCandidateEvaluator evaluator = new MergeCandidateEvaluator(study);
				int nQualified = 0;
				for (Operon[] candidate: candidates)
					if (evaluator.qualify(candidate))
						nQualified++;
				sop("  # qualified candidates = " + nQualified);
				break;
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
