package analysis.arkin;

import java.io.File;
import java.util.*;
import dexter.model.*;
import dexter.cluster.*;
import org.apache.commons.math3.distribution.*;


public class MergeCandidate implements Comparable<MergeCandidate>
{	
	private static SessionModel									session;
	private static Map<Study, Map<Gene, Vector<float[]>>> 		studyToGeneToRawTXs;		// not zero mean
	
	/********************
	Trichodesmium erythraeum: 
		  Same operon: 1333 pairs.
		  m/sd = 6.9054623  4.615163
		  Not-same operon: 1739 pairs.
		  m/sd = 10.509467  6.537533
		Prochlorococcus marinus: 
		  Same operon: 429 pairs.
		  m/sd = 4.567571  4.379395
		  Not-same operon: 637 pairs.
		  m/sd = 9.306983  8.071912
		Crocosphaera watsonii: 
		  Same operon: 1246 pairs.
		  m/sd = 10.97963  8.0738945
		  Not-same operon: 1171 pairs.
		  m/sd = 17.830431  11.690574  
	********************/
	
	static
	{		
		session = CandidateCollection.session;
		if (session == null)
			session = CandidateCollection.getSession();
		
		studyToGeneToRawTXs = new HashMap<Study, Map<Gene, Vector<float[]>>>();
	}
	
	
	public Study							study;
	public Map<String, Gene>				idToGene;
	public Operon							op1;
	public Operon 							op2;
	public Vector<String>					idsOfInterveningGenes;
	public double							expect;
	
	
	public MergeCandidate(Study study, Operon op1, Operon op2)
	{
		this.study = study;
		this.op1 = op1;
		this.op2 = op2;
		
		idToGene = study.getIdToGeneMap();
		
		idsOfInterveningGenes = new Vector<String>();
	}
	
	
	public String toString()
	{
		String s = "Coregulation Group:\n" + op1 + "\n";
		s += "  " + nInterveningGenes() + " intervening genes";
		s += "\n" + op2;
		return s;
	}
	
	
	public String toTerseString()
	{
		return op1.size()  + ":" + idsOfInterveningGenes.size() + ":" + op2.size();
	}
	
	
	public static String getCSVHeader()
	{
		return "Op1 1st gene,Op1 last gene,Intervening gene 1,Intervening gene 2,Op2 1st gene,Op2 last gene, Expect,"
				+ "# genes Op1,# intervening genes,# genes Op2";
	}
	
	
	public String toCSVString()
	{
		return op1.firstElement() + "," + op1.lastElement() + "," + 
			(idsOfInterveningGenes.isEmpty() ? "-" : idsOfInterveningGenes.firstElement()) + "," +
			(idsOfInterveningGenes.size() < 2  ?  "-" : idsOfInterveningGenes.get(1)) + "," +
			op2.firstElement() + "," + op2.lastElement() + "," + expect + "," + op1.size() + "," + nInterveningGenes()
			+ "," + op2.size();
	}
	
	
	public int nInterveningGenes()
	{
		return idsOfInterveningGenes.size();
	}
	
	
	void addInterveningGene(String id)
	{
		idsOfInterveningGenes.add(id);
	}
	
	
	public void setInterveningGenes(Collection<String> ids)
	{
		idsOfInterveningGenes.addAll(ids);
	}
	
	public Vector<String> getAllIds()
	{
		Vector<String> allIds = new Vector<String>();
		allIds.addAll(op1);
		allIds.addAll(op2);
		allIds.addAll(idsOfInterveningGenes);
		return allIds;
	}
	
	
	Study getStudy()
	{
		return study;
	}
	
	Operon[] getOperons()
	{
		return new Operon[] { op1, op2 };
	}
	
	
	
	private final static double[]	CROCO_GAMMA_SHAPE_AND_RATE =
	{
		3.08205508925048, 0.155627053807212
	};
	
	
	private final static double[]	PRO_GAMMA_SHAPE_AND_RATE =
	{
		2.31378025800251, 0.14219440466546	
	};
	
	
	private final static double[]	TERY_GAMMA_SHAPE_AND_RATE =
	{
		3.56546892418401, 0.270654048428999
	};

	private final static double[]	CROCO_MEAN_SD = { 19.80411, 11.99027 };
	private final static double[]	PRO_MEAN_SD = { 16.27196, 10.74248 };
	private final static double[]	TERY_MEAN_SD = { 13.17353, 7.219261 };
	
	
	public void computeGaussianEvalueExhaustivePairs()
	{
		// Collect last 2 ids of 1st operon, all intervening, and 1st 2 ids of 2nd operon.
		assert op1.size() >= 2  &&  op2.size() >= 2;
		Vector<String> ids = new Vector<String>();
		ids.add(op1.get(op1.size()-2));
		ids.add(op1.get(op1.size()-1));
		ids.addAll(idsOfInterveningGenes);
		ids.add(op2.get(0));
		ids.add(op2.get(1));
		
		// Build normal distribution.
		double[] normalParams = null;
		if (study.getOrganism().equals(Organism.CROCO))
			normalParams = CROCO_GAMMA_SHAPE_AND_RATE;
		else if (study.getOrganism().equals(Organism.PRO))
			normalParams = PRO_GAMMA_SHAPE_AND_RATE;
		else if (study.getOrganism().equals(Organism.TERY))
			normalParams = TERY_GAMMA_SHAPE_AND_RATE;
		assert normalParams != null;
		NormalDistribution norm = new NormalDistribution(normalParams[0], normalParams[1]);
		 
		// Check all pairs except same-operon pairs.
		expect = 1d;
		for (int i=0; i<ids.size()-1; i++)
		{
			String id1 = ids.get(i);
			Gene g1 = idToGene.get(id1);
			assert g1 != null;
			for (int j=i+1; j<ids.size(); j++)
			{
				String id2 = ids.get(j);
				if (op1.contains(id1)  &&  op1.contains(id2))
					continue;
				if (op2.contains(id1)  &&  op2.contains(id2))
					continue;
				Gene g2 = idToGene.get(id2);
				assert g2 != null;
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
				double expressionDistance = Metric.EUCLIDEAN.computeDistance(g1, zeroMeanTXs1, g2, zeroMeanTXs2);
				double ple = norm.cumulativeProbability(expressionDistance);
				expect *= ple;
			}
		}
	}	
	
	
	public void computeGammaEvalueExhaustivePairs()
	{
		// Collect last 2 ids of 1st operon, all intervening, and 1st 2 ids of 2nd operon.
		assert op1.size() >= 2  &&  op2.size() >= 2;
		Vector<String> ids = new Vector<String>();
		ids.add(op1.get(op1.size()-2));
		ids.add(op1.get(op1.size()-1));
		ids.addAll(idsOfInterveningGenes);
		ids.add(op2.get(0));
		ids.add(op2.get(1));
		
		// Build gamma distribution.
		double[] gammaParams = null;
		if (study.getOrganism().equals(Organism.CROCO))
			gammaParams = CROCO_GAMMA_SHAPE_AND_RATE;
		else if (study.getOrganism().equals(Organism.PRO))
			gammaParams = PRO_GAMMA_SHAPE_AND_RATE;
		else if (study.getOrganism().equals(Organism.TERY))
			gammaParams = TERY_GAMMA_SHAPE_AND_RATE;
		assert gammaParams != null;
		GammaDistribution gamma = new GammaDistribution(gammaParams[0], gammaParams[1]);
		 
		// Check all pairs except same-operon pairs.
		expect = 1d;
		for (int i=0; i<ids.size()-1; i++)
		{
			String id1 = ids.get(i);
			Gene g1 = idToGene.get(id1);
			assert g1 != null;
			for (int j=i+1; j<ids.size(); j++)
			{
				String id2 = ids.get(j);
				if (op1.contains(id1)  &&  op1.contains(id2))
					continue;
				if (op2.contains(id1)  &&  op2.contains(id2))
					continue;
				Gene g2 = idToGene.get(id2);
				assert g2 != null;
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
				double expressionDistance = Metric.EUCLIDEAN.computeDistance(g1, zeroMeanTXs1, g2, zeroMeanTXs2);
				double ple = gamma.probability(0, expressionDistance);
				expect *= ple;
			}
		}
	}


	public int compareTo(MergeCandidate that) 
	{
		if (this.study != that.study)
			return this.study.getOrganism().getShortestName().compareTo(that.getStudy().getOrganism().getShortestName());
		
		/*****/
		if (this.nInterveningGenes() != that.nInterveningGenes())
			return this.nInterveningGenes() - that.nInterveningGenes();
		/*****/
		
		if (this.expect != that.expect)
			return (this.expect > that.expect)  ?  1  :  -1;
		
		return this.op1.firstElement().compareTo(that.op1.firstElement());
	}
	
	
	static void sop(Object x)	{ System.out.println(x); }
	
	
	public static void main(String[] args) {
		NormalDistribution norm = new NormalDistribution(TERY_MEAN_SD[0], TERY_MEAN_SD[1]);
		for (double d=.2; d<=20; d+=.2)
			sop(d + ": " + norm.probability(0, d));
	}
}

/*
R code for reading neg samples:
	
> pro_table = read.table("Pro_distances.txt", header=T)
> attach (pro_table)
> names(pro_table)
[1] "pro_ds"
> mean(pro_ds)
[1] 16.27196
> sd(pro_ds)
[1] 10.74248


*/