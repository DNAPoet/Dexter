package analysis.arkin;

// ? Maybe also frac operon genes that are diel? 

import java.io.*;
import java.util.*;

import dexter.cluster.Metric;
import dexter.model.*;


public class FracDielGenesInOperons implements GenomeSizes
{
	static float[]							CUTOFFS = { 1.5f, 1.75f, 2f };
	private static SessionModel				session;
	
	private Study 							study;
	private Map<Gene, Vector<float[]>> 		geneToTx;
	private Map<String, Gene> 				idToGene;
	Collection<Operon>						operons;
	
	
	FracDielGenesInOperons(Study study, Collection<Operon> operons)
	{
		this.operons = operons;
		this.study = study;
		geneToTx = session.mapGenesToTimeExpressionPairs(study);
		idToGene = study.getIdToGeneMap();
		
		// Compute mean internal distances.
		Vector<Operon> keptOperons = new Vector<Operon>();
		for (Operon op: operons)
		{
			float dist = computeMeanInternalDist(op);
			if (dist >= 0f)
			{
				op.setMeanInternalPairwiseDist(dist);
				keptOperons.add(op);
			}
		}
		operons = keptOperons;
	}
	
	
	// Returns < 0 if trouble e.g. <2 measured genes.
	float computeMeanInternalDist(Operon op)
	{
		Vector<Gene> measuredGenes = new Vector<Gene>();
		for (String id: op)
			if (idToGene.containsKey(id))
				measuredGenes.add(idToGene.get(id));
		if (measuredGenes.size() < 2)
			return -1f;
			
		Vector<Float> distances = new Vector<Float>();
		for (Gene g1: measuredGenes)
		{
			Vector<float[]> txs1 = geneToTx.get(g1);
			txs1 = Metric.adjustExpressionsToZeroMean(txs1);
			for (Gene g2: measuredGenes)
			{
				if (g2 == g1)
					continue;
				Vector<float[]> txs2 = geneToTx.get(g2);
				txs2 = Metric.adjustExpressionsToZeroMean(txs2);
				float dist = Metric.EUCLIDEAN.computeDistance(g1, txs1, g2, txs2);
				assert dist >= 0f;
				distances.add(dist);
			}
		}
		
		float ret = 0f;
		for (Float d: distances)
			ret += d;
		return ret / distances.size();
	}
	
	
	void check()
	{
		for (float cutoff: CUTOFFS)
		{
			float fracDiel = computeFracDiel(cutoff);
			sop("At cutoff = " + cutoff + ", fraction of operon genes that are diel = " + fracDiel);
		}
	}
	
	
	float computeFracDiel(float deltaCutoff)
	{
		float nMeetingCutoff = 0;
		for (Operon op: operons)
		{
			for (String id: op)
			{
				Gene gene = idToGene.get(id);
				if (gene == null)
					continue;
				float[] minMeanMax = gene.getMinMeanMaxExpressions();
				if (minMeanMax[2] - minMeanMax[0] >= deltaCutoff)
				{
					nMeetingCutoff++;
					break;
				}
			}
		}
		
		return nMeetingCutoff / operons.size();
	}

	
	static void sop(Object x)		{ System.out.println(x); }
	
	
	public static void main(String[] args)
	{
		try
		{
			session = SessionModel.deserialize(new File("data/Sessions/CPT.dex"));
			assert session != null;
			
			sop("CROCO:");
			Collection<Operon> crocoOps = Operon.extractFromArkinPredictionsForCroco();
			FracDielGenesInOperons that = new FracDielGenesInOperons(session.getCrocoStudy(), crocoOps);
			that.check();
			
			sop("**************\nPRO:");
			Collection<Operon> proOps = Operon.extractFromArkinPredictionsForPro();
			that = new FracDielGenesInOperons(session.getProStudy(), proOps);
			that.check();

			sop("**************\nTERY:");
			Collection<Operon> teryOps = Operon.extractFromArkinPredictionsForTery();
			that = new FracDielGenesInOperons(session.getTeryStudy(), teryOps);
			that.check();
		}

		catch (Exception x)
		{
			sop("Feh");
			sop(x.getMessage());
			x.printStackTrace();
		}
	}
}


/*********

CROCO:
At cutoff = 1.5, fraction of operon genes that are diel = 0.62343097
At cutoff = 1.75, fraction of operon genes that are diel = 0.5606695
At cutoff = 2.0, fraction of operon genes that are diel = 0.47698745

PRO:
At cutoff = 1.5, fraction of operon genes that are diel = 0.27169812
At cutoff = 1.75, fraction of operon genes that are diel = 0.2
At cutoff = 2.0, fraction of operon genes that are diel = 0.13962264


TERY:
At cutoff = 1.5, fraction of operon genes that are diel = 0.5042373
At cutoff = 1.75, fraction of operon genes that are diel = 0.3834746
At cutoff = 2.0, fraction of operon genes that are diel = 0.28283897

****/
