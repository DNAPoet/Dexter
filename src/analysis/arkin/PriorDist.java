package analysis.arkin;

import java.io.*;
import java.util.*;
import dexter.util.LocalMath;
import dexter.cluster.Metric;
import dexter.model.*;


public class PriorDist 
{
	private static SessionModel				session;
	
	private Study 							study;
	private Map<Gene, Vector<float[]>> 		geneToTx;
	private Map<String, Gene> 				idToGene;

	
	
	PriorDist(Study study)
	{
		this.study = study;
		assert session != null;
		geneToTx = session.mapGenesToTimeExpressionPairs(study);
		idToGene = study.getIdToGeneMap();
	}
	
	
	void getMuSigma() throws IOException
	{
		// Find predictions file.
		File dirf = new File("analysis_data/ArkinlabOperonPredictions");
		String[] kids = dirf.list();
		char c1 = study.getOrganism().getName().charAt(0);
		File rknFile = null;
		for (String kid: kids)
			if (kid.charAt(0) == c1)
				rknFile = new File(dirf, kid);
		assert rknFile != null;
		
		// Collect operons.
		Vector<Operon> allOperons = Operon.extractFromArkinPredictions(rknFile);
		
		// Compute mean internal distances.
		Vector<Operon> keptOperons = new Vector<Operon>();
		for (Operon op: allOperons)
		{
			float dist = computeMeanInternalDist(op);
			if (dist >= 0f)
			{
				op.setMeanInternalPairwiseDist(dist);
				keptOperons.add(op);
			}
		}
		
		// Sort internal distances.
		Set<Float> sorter = new TreeSet<Float>();
		for (Operon op: keptOperons)
			sorter.add(op.getMeanInternalPairwiseDist());
		int n = 1;
		for (Float f: sorter)
		{
			sop(study.getName() + ": " + n++ + " of " + keptOperons.size() + ": " + f);
		}
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
	
	
	static void sop(Object x)		{ System.out.println(x); }
	
	
	public static void main(String[] args)
	{
		try
		{
			session = SessionModel.deserialize(new File("data/Sessions/CPT.dex"));
			assert session != null;
			for (Study s: session.getStudies())
			{
				(new PriorDist(s)).getMuSigma();
			}
		}
		

		catch (Exception x)
		{
			sop("Feh");
			sop(x.getMessage());
			x.printStackTrace();
		}
	}
}


/***
Croco: 820/836 are m.i.p.d. < chance
Pro:   254/259
Tery:  675/851
***/
