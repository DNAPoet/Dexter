package analysis.arkin;

import java.util.*;
import java.io.*;

import dexter.cluster.Metric;
import dexter.model.*;
import dexter.util.*;


class DeNovoOperonFinder 
{
	private final static int			MIN_OPERON_SIZE_EXCL_FLANKS		=  4;
	private final static int			MIN_OPERON_SIZE_INCL_FLANKS		= MIN_OPERON_SIZE_EXCL_FLANKS + 2;
	private final static int			MAX_OPERON_SIZE_EXCL_FLANKS		= 10;
	private final static int			MAX_OPERON_SIZE_INCL_FLANKS		= MAX_OPERON_SIZE_EXCL_FLANKS + 2;
	
	private static SessionModel			session = getSession();
	private static Map<Organism, LocalNormalDistribution[]> 			
										orgToDistributionsForSameAndDifferentOperon;
	private static Map<Study, Map<Gene, Vector<float[]>>> 		
										studyToGeneToRawTXs;		// not zero mean
	
	
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
	
	
	static
	{		
		session = CandidateCollection.session;
		if (session == null)
			session = CandidateCollection.getSession();

		orgToDistributionsForSameAndDifferentOperon = new HashMap<Organism, LocalNormalDistribution[]>();
		LocalNormalDistribution[] forTery = 
			{ new LocalNormalDistribution(6.9054623, 4.615163), new LocalNormalDistribution(10.509467, 6.537533) };
		orgToDistributionsForSameAndDifferentOperon.put(Organism.TERY, forTery);
		LocalNormalDistribution[] forPro = 
			{ new LocalNormalDistribution(4.567571, 4.379395), new LocalNormalDistribution(9.306983, 8.071912) };
		orgToDistributionsForSameAndDifferentOperon.put(Organism.PRO, forPro);
		LocalNormalDistribution[] forCroco = 
			{ new LocalNormalDistribution(10.97963, 8.0738945), new LocalNormalDistribution(17.830431, 11.690574) };
		orgToDistributionsForSameAndDifferentOperon.put(Organism.CROCO, forCroco);
		
		studyToGeneToRawTXs = new HashMap<Study, Map<Gene, Vector<float[]>>>();
	}
	
	private Study						study;
	private Map<String, Strand> 		idToStrand;
	private Vector<String> 				allIds;
	private Set<String>					studiedIds;
	private Map<String, Gene>			idToGene;
	private Vector<Operon> 				operons;
	
	
	DeNovoOperonFinder(Study study)
	{
		this.study = study;
		
		idToStrand = OrganismToIdToStrandMap.getInstance().get(study.getOrganism());
		allIds = new Vector<String>(idToStrand.keySet());
		idToGene = study.getIdToGeneMap();
		
		studiedIds = new HashSet<String>();
		for (Gene gene: study)
			studiedIds.add(gene.getId());
		
		operons = findOperonsAndEValues();  sop(operons.size());
	}
	
	
	private Vector<Operon> findOperonsAndEValues()
	{
		Vector<Operon> ret = new Vector<Operon>();
		
		int maxStartIndex = allIds.size() - MIN_OPERON_SIZE_INCL_FLANKS - 1;
		for (int startIndex=0; startIndex<=maxStartIndex; startIndex++)
		{
			for (int endIndex=startIndex+MIN_OPERON_SIZE_EXCL_FLANKS; 
				 endIndex<allIds.size() && endIndex<startIndex+MAX_OPERON_SIZE_INCL_FLANKS; 
				 endIndex++)
			{
				// Find candidate.
				Operon candidate = new Operon();
				for (int i=startIndex; i<=endIndex; i++)
					candidate.add(allIds.get(i));
				// Qualify candidate.
				if (!qualified(candidate))
					continue;
				double expect = evaluate(candidate);
				candidate.setExpect(expect);
				candidate.setFlanked(true);
				ret.add(candidate);
			}
		}
		return ret;
	}
	
	
	private double evaluate(Operon op)
	{
		// Get raw time-expression measurements for genes.
		Map<Gene, Vector<float[]>> geneToRawTXs = studyToGeneToRawTXs.get(study);
		if (geneToRawTXs == null)
		{
			TimeAssignmentMap timeAssignments = session.getTimeAssignmentMapForStudy(study);
			geneToRawTXs = study.mapGenesToTimeExpressionPairs(timeAssignments);
			studyToGeneToRawTXs.put(study, geneToRawTXs);
		}
		
		// Evaluate by internal (non-flanking) genes. For each pair, multiply e by probability of
		// seeing 2 not-same-operon genes by chance with at most this expression distance.
		double expect = 1d;
		for (int i=0; i<op.size()-1; i++)
		{
			String id1 = op.get(i);
			Gene gene1 = idToGene.get(id1);
			String id2 = op.get(i+1);
			Gene gene2 = idToGene.get(id2);
			boolean haveFlanker = (i == 0)  ||  (i == op.size()-2);
			double marginal = haveFlanker  ?  
				p2SameOpGenesAtLeastThisDistant(gene1, gene2)  :  
				p2NotSameOpeGenesAtMostThisDistant(gene1, gene2);
			expect *= marginal;
		}
		
		return expect;
	}
	
	
	// A qualified candidate has studied all genes, and all genes but flankers are on the same strand.
	private boolean qualified(Operon candidate)
	{
		// Studied.
		for (String id: candidate)
			if (!studiedIds.contains(id))
				return false;
		
		// Strand.
		Strand strand = idToStrand.get(candidate.get(1));
		for (int i=2; i<candidate.size()-1; i++)
			if (idToStrand.get(candidate.get(i)) != strand)
				return false;
		
		return true;
	}
	
	
	private double getExpressionDistance(Gene g1, Gene g2)
	{
		assert session != null  :  "Null session";
		
		// Get zero-mean time/expression pairs.
		Map<Gene, Vector<float[]>> geneToRawTXs = studyToGeneToRawTXs.get(study);
		if (geneToRawTXs == null)
		{
			TimeAssignmentMap timeAssignments = session.getTimeAssignmentMapForStudy(study);
			geneToRawTXs = study.mapGenesToTimeExpressionPairs(timeAssignments);
			studyToGeneToRawTXs.put(study, geneToRawTXs);
		}
		Vector<float[]> rawTXs1 = geneToRawTXs.get(g1);
		Vector<float[]> zeroMeanTXs1 = Metric.adjustExpressionsToZeroMean(rawTXs1);
		Vector<float[]> rawTXs2 = geneToRawTXs.get(g2);
		Vector<float[]> zeroMeanTXs2 = Metric.adjustExpressionsToZeroMean(rawTXs2);
		
		// Compute expression distance between the two genes.
		double expressionDistance = Metric.EUCLIDEAN.computeDistance(g1, zeroMeanTXs1, g2, zeroMeanTXs2);
		return expressionDistance;
	}
	
	
	//
	// Returns the probability that 2 not-same-operon adjacent genes have expression distance that is
	// <= the expression distance between g1 and g2. CAUTION: The distributions have left-tails that
	// extend into negative domain, but this is meaningless because expression distance can't be less
	// than zero.
	//
	private double p2NotSameOpeGenesAtMostThisDistant(Gene g1, Gene g2)
	{
		double expressionDistance = getExpressionDistance(g1, g2);
		Organism org = study.getOrganism();
		LocalNormalDistribution distribution = orgToDistributionsForSameAndDifferentOperon.get(org)[1];
		double prob = distribution.probXBetweenMinAndMax(0, expressionDistance);
		return prob;
	}
	
	
	private double p2SameOpGenesAtLeastThisDistant(Gene g1, Gene g2)
	{
		double expressionDistance = getExpressionDistance(g1, g2);
		Organism org = study.getOrganism();
		LocalNormalDistribution distribution = orgToDistributionsForSameAndDifferentOperon.get(org)[0];
		double prob = distribution.probXBetweenMinAndMax(0, expressionDistance);
		return prob;
	}
	
	
	private BinCounter<Integer> binExpectsByNtile(int n)
	{
		BinCounter<Integer> ret = new BinCounter<Integer>();
		for (Operon op: operons)
		{
			double expect = op.getExpect();
			int decile = (int)Math.round(expect * n);
			ret.bumpCountForBin(decile);
		}
		return ret;
	}
	
	
	private BinCounter<Integer> binOperonsByLength()
	{
		BinCounter<Integer> ret = new BinCounter<Integer>();
		for (Operon op: operons)
			ret.bumpCountForBin(op.size());
		return ret;
	}
	
	
	private void cullIfPPerJunctionGT(double pPerJunctionThreshold)
	{
		Set<Operon> deletes = new HashSet<Operon>();
		for (Operon op: operons)
		{
			int nJunctions = op.size() - 1;
			double threshold = Math.pow(pPerJunctionThreshold, nJunctions);
			if (op.getExpect() > threshold)
				deletes.add(op);
		}
		operons.removeAll(deletes);
	}
	
	
	Vector<Operon> getOperons()
	{
		return operons;
	}
	
	
	Map<Integer, Set<Operon>> getOperonsBySize()
	{
		Map<Integer, Set<Operon>> ret = new HashMap<Integer, Set<Operon>>();
		for (Operon op: operons)
		{
			int size = op.size();
			if (!ret.containsKey(size))
				ret.put(size, new HashSet<Operon>());
			ret.get(size).add(op);
		}
		return ret;
	}
	
	
	static void sop(Object x)				{ System.out.println(x); }
	
	
	public static void main(String[] args)
	{
		try
		{
			sop("START");
			for (Study study: session.getStudies())
			{
				sop("-----------------\n" + study.getName());
				DeNovoOperonFinder finder = new DeNovoOperonFinder(study);
				finder.cullIfPPerJunctionGT(0.1);
				//sop(finder.binOperonsByLength());
				Map<Integer, Set<Operon>> sizeToOps = finder.getOperonsBySize();
				for (Set<Operon> ops: sizeToOps.values())
				{
					for (Operon op: ops)
						sop(op);
					sop("+++++++++++++++++++");
				}
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
