package analysis.merge;

import java.io.*;
import java.text.DecimalFormat;
import java.util.*;

import dexter.coreg.*;
import dexter.model.*;
import analysis.arkin.*;
import dexter.util.*;


// 
// Candidates are predicted operon pairs on the same strand, separated by at most 2 genes, where the
// majority of genes have differential expression >= 2x.
//


class QualifiedCandidateFinder
{
	
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
	
	
	private Study							study;
	private Map<Gene, Vector<float[]>> 		geneToRawTXs;
	private DiffCriterion					diffCrit = DiffCriterion.ANY;
	
	
	static enum DiffCriterion 
	{ 
		ANY, GE_1_GENE, HALF, ALL;			// use GE_1_GENE!!!
	
		boolean enoughDifferentialExpression(MergeCandidate candi, Map<Gene, Vector<float[]>> geneToRawTXs)
		{
			Vector<String> allIds = candi.getAllIds();
			int nGenesWithOkDiffEx = 0;
			for (String id: allIds)
			{
				Gene gene = candi.idToGene.get(id);
				Vector<float[]> txs = geneToRawTXs.get(gene);
				if (txs == null)
					return false;
				float delta = getExpressionDelta(txs);
				if (delta >= 1)
					nGenesWithOkDiffEx++;
			}
			
			switch (this)
			{
				case ANY:				return true;
				case GE_1_GENE:			return nGenesWithOkDiffEx >= 1;
				case HALF:				return nGenesWithOkDiffEx >= allIds.size() / 2;
				default:				return nGenesWithOkDiffEx == allIds.size();			
			}
		}
	}
	
	
	QualifiedCandidateFinder(Study study)
	{
		this.study = study;
		TimeAssignmentMap timeAssignments = session.getTimeAssignmentMapForStudy(study);
		geneToRawTXs = study.mapGenesToTimeExpressionPairs(timeAssignments);
	}
	
	
	Vector<MergeCandidate> collectQualifiedCandidates() throws IOException
	{
		// Collect all adjacent operon pairs.
		Vector<Operon[]> adjacentOperonPairs = new Vector<Operon[]>();		// all operon[]s are size=2
		CoregulationFile coregFile = new CoregulationFile(study.getOrganism());
		Vector<CoregulationGroup> coregGps = coregFile.getCoregulationGroups();
		CoregulationGroup lastCoregGp = null;
		for (CoregulationGroup currentCoregGp: coregGps)
		{
			assert currentCoregGp.size() >= 2;
			if (lastCoregGp != null)
			{
				if (lastCoregGp.size() >= 2  &&  currentCoregGp.size() >= 2)
				{
					Operon op1 = new Operon(lastCoregGp);
					Operon op2 = new Operon(currentCoregGp);
					adjacentOperonPairs.add(new Operon[] { op1, op2 });
				}	
			}
			lastCoregGp = currentCoregGp;
		}
		
		// Convert adjacent operon pairs to MergeCandidate instances, with intervening genes. Collect those
		// with <= 2 intervening genes and strand consistency.
		Map<String, Strand> idToStrand = OrganismToIdToStrandMap.getInstance().get(study.getOrganism());
		Vector<String> ids = new Vector<String>(idToStrand.keySet());
		Vector<MergeCandidate> candidatesBeforeDiffExCheck = new Vector<MergeCandidate>();
		outer: for (Operon[] opPair: adjacentOperonPairs)
		{		
			// Collect ids of intervening genes.
			Vector<String> interveningGeneIds = new Vector<String>();
			String lastIdIn1stOperon = opPair[0].lastElement();
			String firstIdIn2ndOperon = opPair[1].firstElement();
			int n1 = ids.indexOf(lastIdIn1stOperon);
			int n2 = ids.indexOf(firstIdIn2ndOperon);
			if (n1 < 0  ||  n2 < 0)
				continue;
			assert n1 < n2  :  "n1 = " + n1 + ", n2 = " + n2;
			for (int i=n1+1; i<n2; i++)
				interveningGeneIds.add(ids.get(i));
			// Allow at most 2 intervening genes.
			if (interveningGeneIds.size() > 2)
				continue;
			// All genes must be on same strand.
			Strand strand = idToStrand.get(lastIdIn1stOperon);
			for (int i=n1+1; i<=n2; i++)
				if (idToStrand.get(ids.get(i)) != strand)
					continue outer;
			MergeCandidate candidate = new MergeCandidate(study, opPair[0], opPair[1]);
			candidate.setInterveningGenes(interveningGeneIds);
			candidatesBeforeDiffExCheck.add(candidate);
		}
		
		// Retain measured candidates.
		Vector<MergeCandidate> measuredCandidatesBeforeDiffExCheck = new Vector<MergeCandidate>();
		for (MergeCandidate candi: candidatesBeforeDiffExCheck)
		{
			Vector<String> allIds = candi.getAllIds();
			boolean fail = false;
			for (String id: allIds)
			{
				Gene gene = candi.idToGene.get(id);
				Vector<float[]> txs = geneToRawTXs.get(gene);
				if (txs == null)
				{
					fail = true;
					break;
				}
			}
			if (!fail)
				measuredCandidatesBeforeDiffExCheck.add(candi);
		}
		
		// Retain by measured differential expression. Reject if any genes are not measured or 
		// differential expression is too small.
		Vector<MergeCandidate> qualifiedCandidates = new Vector<MergeCandidate>();
		for (MergeCandidate candi: measuredCandidatesBeforeDiffExCheck)
			if (diffCrit.enoughDifferentialExpression(candi, geneToRawTXs))
				qualifiedCandidates.add(candi);
		
		for (MergeCandidate candi: qualifiedCandidates)
			candi.computeGaussianEvalueExhaustivePairs();

		return qualifiedCandidates;
	}
	
	
	static float getExpressionDelta(Vector<float[]> txs)
	{
		assert txs != null : "null txs[]";
		float min = Float.MAX_VALUE;
		float max = -1f;
		for (float[] tx: txs)
		{
			min = Math.min(min, tx[1]);
			max = Math.max(max, tx[1]);
		}
		assert max >= min;
		return max - min;
	}
	
	
	static void makeTable33() throws IOException
	{
		File odirf = new File("analysis_data/MyRecommendations");
		File ofile = new File(odirf, "Table3dot3.tsv");
		FileWriter fw = new FileWriter(ofile);
		
		fw.write("Organism\tOp1, 1st Gene\tOp2, 1st Gene\tE-value\tMerged Length\n");
		
		sop("START");
		Vector<Study> studies = new Vector<Study>();
		for (Study study: session.getStudies())
			if (study.getName().toUpperCase().contains("CROCO"))
				studies.add(study);
		for (Study study: session.getStudies())
			if (study.getName().toUpperCase().contains("PRO"))
				studies.add(study);
		for (Study study: session.getStudies())
			if (study.getName().toUpperCase().contains("SHILOVA"))
				studies.add(study);
		
		for (Study study: studies)
		{
			sop("********************\n" + study.getName());
			QualifiedCandidateFinder finder = new QualifiedCandidateFinder(study);
			finder.diffCrit = DiffCriterion.GE_1_GENE;
			Vector<MergeCandidate> qualifiedCandidates = finder.collectQualifiedCandidates();
			sop("Unsorted=" + qualifiedCandidates.size());
			TreeSet<MergeCandidate> sorter = new TreeSet<MergeCandidate>(qualifiedCandidates);
			sop("Sorted=" + sorter.size());
			Study lastStudy = null;
			for (MergeCandidate candi: qualifiedCandidates)
			{
				if (candi.expect > 0.05d)
					continue;
				String s = "  ";
				if (candi.study != lastStudy)
				{
					s = study.getOrganism().getShortestName();
					lastStudy = candi.study;
				}
				s += "\t" + candi.op1.firstElement() + "\t" + candi.op2.firstElement() + "\t";
				DecimalFormat formatter = new DecimalFormat("0.##E0");
				String se = formatter.format(candi.expect);
				sop(se);
				s += se + "\t" + candi.getAllIds().size();
				fw.write(s + "\n");
			}
		}
		fw.flush();
		fw.close();
	}
	
	
	
	static void sop(Object x)			{ System.out.println(x); }
	
	
	public static void main(String[] args)
	{
		try
		{
			/***
			File odirf = new File("analysis_data/MyRecommendations");
			assert odirf.exists();
			File ofile = new File(odirf, "AllCandidates.csv");
			FileWriter fw = new FileWriter(ofile);
			fw.write(MergeCandidate.getCSVHeader() + "\n");
			DiffCriterion crit = DiffCriterion.GE_1_GENE;
			sop("\nDIFF CRIT = " + crit);
			sop("START");
			for (Study study: session.getStudies())
			{
				sop("********************\n" + study.getName());
				QualifiedCandidateFinder finder = new QualifiedCandidateFinder(study);
				finder.diffCrit = crit;
				Vector<MergeCandidate> qualifiedCandidates = finder.collectQualifiedCandidates();
				sop("Unsorted=" + qualifiedCandidates.size());
				TreeSet<MergeCandidate> sorter = new TreeSet<MergeCandidate>(qualifiedCandidates);
				sop("Sorted=" + sorter.size());
				for (MergeCandidate candi: qualifiedCandidates)
					//if (candi.expect <= 0.05d)
						fw.write(candi.toCSVString() + "\n");
			}
			fw.flush();
			fw.close();
			********/
			
			/***
			DecimalFormat formatter = new DecimalFormat("0.###E0");
			double d = .0000000000000000000004567;
			sop(formatter.format(d));
			***/

			makeTable33();
			
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

