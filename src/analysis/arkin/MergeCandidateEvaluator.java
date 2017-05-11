package analysis.arkin;

import java.io.*;
import java.util.*;
import dexter.coreg.*;
import dexter.model.*;

/***************************************************
Study = Shilova_Tery
# original candidate pairs = 403
  # intervening genes = 0: 58 candidate pairs
  # intervening genes = 1: 18 candidate pairs
  # intervening genes = 2: 1 candidate pairs
Study = Shi_Croco
# original candidate pairs = 607
  # intervening genes = 0: 83 candidate pairs
  # intervening genes = 1: 27 candidate pairs
  # intervening genes = 2: 12 candidate pairs
Study = Zinser_Pro
# original candidate pairs = 110
  # intervening genes = 0: 11 candidate pairs
  # intervening genes = 1: 5 candidate pairs
  # intervening genes = 2: 2 candidate pairs
  
**************************************************/  

class MergeCandidateEvaluator
{
	private Study			study;
	
	
	MergeCandidateEvaluator(Study study)
	{
		this.study = study;
	}

	
	boolean qualify(Operon[] candidates)
	{
		assert candidates.length == 1;
		
		// Collect ids of intervening genes.
		Vector<String> interveningGeneIds = new Vector<String>();
		String lastIdIn1stOperon = candidates[0].lastElement();
		String firstIdIn2ndOperon = candidates[1].firstElement();
		Map<String, Strand> idToStrand = OrganismToIdToStrandMap.getInstance().get(study.getOrganism());
		Vector<String> ids = new Vector<String>(idToStrand.keySet());
		int n1 = ids.indexOf(lastIdIn1stOperon);
		int n2 = ids.indexOf(firstIdIn2ndOperon);
		if (n1 < 0  ||  n2 < 0)
			return false;
		assert n1 < n2  :  "n1 = " + n1 + ", n2 = " + n2;
		for (int i=n1+1; i<n2; i++)
			interveningGeneIds.add(ids.get(i));
		
		// Allow at most 2 intervening genes.
		if (interveningGeneIds.size() > 2)
			return false;
		
		// All genes must be on same strand.
		Strand strand = idToStrand.get(lastIdIn1stOperon);
		for (int i=n1+1; i<=n2; i++)
			if (idToStrand.get(ids.get(i)) != strand)
				return false;
		
		return true;
	}

	
	// If candidate pair qualifies, returns # of intervening gene (0-2), otherwise returns -1.
	// Candidates qualify if they and all intervening genes are on the same strand, and if the
	// # of intervening genes is <=2.
	int getNInterveningGenesIfQualified(Operon[] candidates)
	{		
		// Collect ids of intervening genes.
		Vector<String> interveningGeneIds = new Vector<String>();
		String lastIdIn1stOperon = candidates[0].lastElement();
		String firstIdIn2ndOperon = candidates[1].firstElement();
		Map<String, Strand> idToStrand = OrganismToIdToStrandMap.getInstance().get(study.getOrganism());
		Vector<String> ids = new Vector<String>(idToStrand.keySet());
		int n1 = ids.indexOf(lastIdIn1stOperon);
		int n2 = ids.indexOf(firstIdIn2ndOperon);
		if (n1 < 0  ||  n2 < 0)
			return -1;
		assert n1 < n2  :  "n1 = " + n1 + ", n2 = " + n2;
		for (int i=n1+1; i<n2; i++)
			interveningGeneIds.add(ids.get(i));
		
		// Allow at most 2 intervening genes.
		if (interveningGeneIds.size() > 2)
			return -1;
		
		// All genes must be on same strand.
		Strand strand = idToStrand.get(lastIdIn1stOperon);
		for (int i=n1+1; i<=n2; i++)
			if (idToStrand.get(ids.get(i)) != strand)
				return -1;
		
		return interveningGeneIds.size();
	}
	
	
	Vector<Vector<Operon[]>> collectQualifiedCandidatesByNInterveningGenes(Collection<Operon[]> candidates)
	{
		Vector<Vector<Operon[]>> ret = new Vector<Vector<Operon[]>>();
		for (int i=0; i<=2; i++)
			ret.add(new Vector<Operon[]>());
		
		for (Operon[] candidate: candidates)
		{
			int nIntervening = getNInterveningGenesIfQualified(candidate);
			if (nIntervening >= 0)
				ret.get(nIntervening).add(candidate);
		}
		
		return ret;
	}
	
	
	//
	// Probability that a run of genes in a single operon is this loose or looser is a single operon. E.g. if the
	// 2 Arkin operons are A-B and D-E, with one intervening gene C, this method returns
	// p(dBC | {B,C in same operon} 
	float probArkinIsWrong(Operon[] boundingArkinOperons, Vector<String> idsOfInteveningGenes)
	{
		return 0f;
	}
	
	
	// Probability that a run of genes this tight or tighter is a pair of unrelated operons.
	float probMergeIsWrong(Operon[] boundingArkinOperons, Vector<String> idsOfInteveningGenes)
	{
		return 0f;
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
				sop("# original candidate pairs = " + candidates.size());
				MergeCandidateEvaluator evaluator = new MergeCandidateEvaluator(study);
				Vector<Vector<Operon[]>> qualifiedByNIntervening = 
					evaluator.collectQualifiedCandidatesByNInterveningGenes(candidates);
				for (int i=0; i<=2; i++)
					sop("  # intervening genes = " + i + ": " + qualifiedByNIntervening.get(i).size() + " candidate pairs");
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
