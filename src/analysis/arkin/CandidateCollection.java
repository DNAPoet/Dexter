package analysis.arkin;

import java.io.*;
import java.util.*;

import dexter.model.*;
import dexter.util.*;


//
// Values are maps from n intervening genes to candidates.
//


class CandidateCollection extends HashMap<Study, TreeMap<Integer, Vector<MergeCandidate>>>
{
	private final static File		RECO_DIRF	= new File("analysis_data/MyRecommendations");
	
	
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
	
	
	CandidateCollection() throws Exception
	{
		for (Study study: session.getStudies())
		{
			// Collect operon pairs.
			sop("Study = " + study.getName());
			MergeCandidateFinder finder = new MergeCandidateFinder(study);
			Vector<Operon[]> candidates = finder.collectCandidateOperonPairs();
			MergeCandidateEvaluator evaluator = new MergeCandidateEvaluator(study);
			Vector<Vector<Operon[]>> qualifiedByNIntervening = 
				evaluator.collectQualifiedCandidatesByNInterveningGenes(candidates);
			
			// Convert operon pairs to MergeCandidate instances, with E-values not yet set.
			for (int nIntervening=0; nIntervening<qualifiedByNIntervening.size(); nIntervening++)
			{
				Vector<Operon[]> operonPairs = qualifiedByNIntervening.get(nIntervening);
				for (Operon[] operonPair: operonPairs)
				{
					assert operonPair.length == 2;
					MergeCandidate candidate = new MergeCandidate(study, operonPair[0], operonPair[1]);
					Vector<String> interveningGeneIds = new Vector<String>();
					String lastIdIn1stOperon = operonPair[0].lastElement();
					String firstIdIn2ndOperon = operonPair[1].firstElement();
					Map<String, Strand> idToStrand = OrganismToIdToStrandMap.getInstance().get(study.getOrganism());
					Vector<String> ids = new Vector<String>(idToStrand.keySet());
					int n1 = ids.indexOf(lastIdIn1stOperon);
					int n2 = ids.indexOf(firstIdIn2ndOperon);
					if (n1 < 0  ||  n2 < 0)
						continue;
					assert n1 < n2  :  "n1 = " + n1 + ", n2 = " + n2;
					for (int i=n1+1; i<n2; i++)
						interveningGeneIds.add(ids.get(i));
					candidate.setInterveningGenes(interveningGeneIds);
					int indexOfPreFlanker = ids.indexOf(operonPair[0].firstElement());
					int indexOfPostFlanker = ids.indexOf(operonPair[1].lastElement());
					if (indexOfPreFlanker > 0  &&  indexOfPostFlanker > 0)
					{
						indexOfPreFlanker--;
						indexOfPostFlanker++;
						String idOfPreFlanker = ids.get(indexOfPreFlanker);
						String idOfPostFlanker = ids.get(indexOfPostFlanker);
						//candidate.setPreFlanker(idOfPreFlanker);
						//candidate.setPostFlanker(idOfPostFlanker);
					}
					addCandidateForNInterveningGenes(study, nIntervening, candidate);
				}
			}
		}
	}
	

	void addCandidateForNInterveningGenes(Study study, int nInterv, MergeCandidate candidate)
	{
		if (get(study) == null)
			put(study, new TreeMap<Integer, Vector<MergeCandidate>>());
		
		TreeMap<Integer, Vector<MergeCandidate>> nInterveningGenesToCandidates = get(study);
		Vector<MergeCandidate> candidates = nInterveningGenesToCandidates.get(nInterv);
		if (candidates == null)
		{
			candidates = new Vector<MergeCandidate>();
			nInterveningGenesToCandidates.put(nInterv, candidates);
		}
		candidates.add(candidate);
	}
	
	
	public String toString()
	{
		String s = "Candidate Collection:";
		for (Study study: keySet())
		{
			s += "\n  " + study.getName();
			TreeMap<Integer, Vector<MergeCandidate>> map = get(study);
			for (int i=0; i<=2; i++)
			{
				s += "\n    " + i + " intervening genes: " + map.get(i).size();
			}
		}
		return s;
	}
	
	
	Vector<MergeCandidate> collectCandidates()
	{
		Vector<MergeCandidate> ret = new Vector<MergeCandidate>();
		for (TreeMap<Integer, Vector<MergeCandidate>> map: values())
			for (Vector<MergeCandidate> vec: map.values())
				ret.addAll(vec);
		return ret;
	}
	
	
	void computeEValues(boolean includeFlanks)
	{
		for (TreeMap<Integer, Vector<MergeCandidate>> map: values())
			for (Vector<MergeCandidate> vec: map.values())	
				for (MergeCandidate candidate: vec)
					;//candidate.computeEValue(includeFlanks);
	}
	
	/****
	Map<Study, TreeMap<Integer, BinCounter<Integer>>> binProbsByDecile()
	{
		Map<Study, TreeMap<Integer, BinCounter<Integer>>> ret = 
			new HashMap<Study, TreeMap<Integer, BinCounter<Integer>>>();
		
		for (Study study: keySet())
		{
			ret.put(study, new TreeMap<Integer, BinCounter<Integer>>());
			for (int i=0; i<=2; i++)
			{
				BinCounter<Integer> binCtr = new BinCounter<Integer>();
				ret.get(study).put(i, binCtr);
				Vector<MergeCandidate> candis = get(study).get(i);
				for (MergeCandidate candi: candis)
				{
					double p = candi.getEValueNoFlankers();			// 0 - 1
					if (p < 0)
						continue;
					double px10 = p * 10d;							// 0 - 10
					int decile = (int)px10;							// 0 - 9
					binCtr.bumpCountForBin(decile);
				}
			}
		}
		
		return ret;
	}
	
	
	String reportByDecileNoFlankers()
	{
		String s = this + "\nComputing no-flankers E-values";
		computeEValues(false);
		Map<Study, TreeMap<Integer, BinCounter<Integer>>> binCounters = binProbsByDecile();
		for (Study study: binCounters.keySet())
		{
			s += ("*******************\n" + study);
			Map<Integer, BinCounter<Integer>> map = binCounters.get(study);
			for (Integer i: map.keySet())
			{
				s += "  " + i + " intervening genes";
				s += "\n" + map.get(i);
				s += "\n  ---------------\n";
			}
		}
		return s;
	}
	
	
	// Note all relevant operons are size 2.
	void writeMergeRecommendations(File tsvFile, double threshold) throws IOException
	{
		computeEValues(false);
		
		FileWriter fw = new FileWriter(tsvFile);
		
		fw.write("Operon 1, first gene\tAnnotation\tOperon 1, last gene\tAnnotation\t# intervening genes\t" +
			"Operon 2, first gene\tAnnotation\tOperon 2, last gene\tAnnotation\t Expect\n");
		for (MergeCandidate candi: collectCandidates())
		{
			Map<String, Gene> idToGene = candi.getStudy().getIdToGeneMap();
			double expect = candi.getEValueNoFlankers();
			if (expect > threshold  ||  expect < 0)
				continue;
			Operon op1 = candi.getOperons()[0];
			assert op1.size() == 2;
			fw.write(op1.firstElement() + "\t");
			fw.write(getAnnotationFromMap(op1.firstElement(), idToGene) + "\t");
			fw.write(op1.lastElement() + "\t");
			fw.write(getAnnotationFromMap(op1.lastElement(), idToGene) + "\t");
			fw.write(candi.nInterveningGenes() + "\t");
			Operon op2 = candi.getOperons()[1];
			assert op2.size() == 2;
			fw.write(op2.firstElement() + "\t");
			fw.write(getAnnotationFromMap(op2.firstElement(), idToGene) + "\t");
			fw.write(op2.lastElement() + "\t");
			fw.write(getAnnotationFromMap(op2.lastElement(), idToGene) + "\t");
			fw.write(candi.getEValueNoFlankers() + "\n");
		}
		
		fw.flush();
		fw.close();
	}
	*****/
	
	
	static String getAnnotationFromMap(String geneId, Map<String, Gene> idToGene)
	{
		Gene gene = idToGene.get(geneId);
		if (gene == null)
			return "???";
		String anno = gene.getValueForPredefinedRole(PredefinedSpreadsheetColumnRole.ANNOTATION);
		return (anno == null)  ?  "n/a"  :  anno;
	}
	
	
	static void sop(Object x)				{ System.out.println(x); }
	
	
	public static void main(String[] args)
	{
		try
		{
			sop("START");
			CandidateCollection that = new CandidateCollection();
			File f = new File(RECO_DIRF, "MergeRecommendations.tsv");
			sop("Wrote " + f.getAbsolutePath());
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
