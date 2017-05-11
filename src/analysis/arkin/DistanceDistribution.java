package analysis.arkin;

import java.util.*;
import java.io.*;

import org.apache.commons.math3.distribution.GammaDistribution;
import org.apache.commons.math3.special.Gamma;

import dexter.model.*;
import dexter.cluster.*;
import dexter.coreg.*;


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

class DistanceDistribution extends Vector<Float> implements Serializable
{
	static SessionModel						session;
	
	static
	{
		try
		{
			session = SessionModel.deserialize(new File("data/Sessions/CPT.dex"));
		}
		catch (Exception x)
		{
			sop("STRESS");
		}
	}
	
	boolean						forOperons;
	
	
	// Keys in idToStrand are all ids in genome, in proper order.
	public DistanceDistribution(Study study, 
								LinkedHashMap<String, Strand> idToStrand, 
								boolean sameOperon)  throws IOException
	{
		Map<Gene, Vector<float[]>> geneToTx = session.mapGenesToTimeExpressionPairs(study);
		if (sameOperon)
			loadForSameOperon(study, geneToTx);
		else
			loadForNotSameOperon(study, idToStrand, geneToTx);
	}
	
	
	private void loadForSameOperon(Study study, Map<Gene, Vector<float[]>> geneToTx) throws IOException
	{
		CoregulationFile coregFile = new CoregulationFile(study.getOrganism());
		Vector<CoregulationGroup> operons = coregFile.getCoregulationGroups();
		Map<String, Gene> idToStudiedGene = study.getIdToGeneMap();
		for (CoregulationGroup operon: operons)
		{
			String lastId = null;
			for (String currentId: operon)
			{
				if (lastId == null)
				{
					// 1st time thru, just prime the pump.
					lastId = currentId;
					continue;
				}
				Gene lastGene = idToStudiedGene.get(lastId);
				Gene currentGene = idToStudiedGene.get(currentId);	
				if (lastGene != null  &&  currentGene != null)
				{
					Vector<float[]> lastTxs = geneToTx.get(lastGene);
					lastTxs = Metric.adjustExpressionsToZeroMean(lastTxs);
					Vector<float[]> currentTxs = geneToTx.get(currentGene);				
					currentTxs = Metric.adjustExpressionsToZeroMean(currentTxs);
					float dist = Metric.EUCLIDEAN.computeDistance(lastGene, lastTxs, currentGene, currentTxs);
					add(dist);
				}
				lastId = currentId;
			}
		}
	}
	
	
	// Load distances between all consecutive pairs of genes that are on opposite strands, with both
	// genes represented in the study.
	private void loadForNotSameOperon(Study study, LinkedHashMap<String, Strand> idToStrand, Map<Gene, Vector<float[]>> geneToTx)
		throws IOException
	{
		File odirf = new File("analysis_data/NegativeSamples");
		File csvofile = new File(odirf, study.getOrganism().getShortestName() + "_negative_sample.csv");
		File distofile = new File(odirf, study.getOrganism().getShortestName() + "_distances.txt");
		FileWriter csvfw = new FileWriter(csvofile);
		FileWriter distfw = new FileWriter(distofile);
		csvfw.write("Gene1,Gene2,Distance\n");
		
		Map<String, Gene> idToStudiedGene = study.getIdToGeneMap();
		String lastId = null;
		for (String currentId: idToStrand.keySet())
		{
			if (lastId == null)
			{
				// 1st time thru, just prime the pump.
				lastId = currentId;
				continue;
			}
			Gene lastGene = idToStudiedGene.get(lastId);
			Gene currentGene = idToStudiedGene.get(currentId);	
			if (lastGene != null  &&  currentGene != null  &&
				idToStrand.get(lastId) != null  &&  idToStrand.get(currentId) != null  &&
				idToStrand.get(lastId) != idToStrand.get(currentId))
			{
				// 2 consecutive studied genes on opposite strands ... can't possibly be in an operon.
				Vector<float[]> lastTxs = geneToTx.get(lastGene);
				lastTxs = Metric.adjustExpressionsToZeroMean(lastTxs);
				Vector<float[]> currentTxs = geneToTx.get(currentGene);
				currentTxs = Metric.adjustExpressionsToZeroMean(currentTxs);
				float dist = Metric.EUCLIDEAN.computeDistance(lastGene, lastTxs, currentGene, currentTxs);
				add(dist);
				String s = lastGene.getId() + "," + currentGene.getId() + "," + dist;
				csvfw.write(s + "\n");
				distfw.write(dist + "\n");
			}
			lastId = currentId;
		}

		csvfw.flush();
		csvfw.close();
		distfw.flush();
		distfw.close();
	}
	
	
	float getExpressionDelta(Vector<float[]> txs)
	{
		float min = Float.MAX_VALUE;
		float max = -1f;
		for (float[] tx: txs)
		{
			min = Math.min(min, tx[1]);
			max = Math.max(max, tx[1]);
		}
		return max - min;
	}
	
	
	float[] getMeanAndStdDev() 
	{
		float mean = 0f;
		for (Float distance: this)
			mean += distance;
		mean /= size();
		
		float var = 0;
		for (Float distance: this)
			var += (distance-mean) * (distance-mean);
		var /= (size()-1);
		float sd = (float)Math.sqrt(var);
		
		return new float[] { mean, sd };
	}
	
	
	// Keys are { distribution for same operon, distribution for not same operon }.
	static Map<Organism, DistanceDistribution[]> mapOrganismsToDistributions() throws Exception
	{
		OrganismToIdToStrandMap orgToIdToStrand = OrganismToIdToStrandMap.getInstance();
		Map<Organism, DistanceDistribution[]> ret = new HashMap<Organism, DistanceDistribution[]>();
		for (Study study: session.getStudies())
		{
			Organism org = study.getOrganism();
			DistanceDistribution sameOperonDistn = new DistanceDistribution(study, orgToIdToStrand.get(org), true);
			DistanceDistribution notSameOperonDistn = new DistanceDistribution(study, orgToIdToStrand.get(org), false);
			ret.put(org, new DistanceDistribution[] { sameOperonDistn, notSameOperonDistn });
		}

		return ret;
	}
	
	
	static void sop(Object x)			{ System.out.println(x); }
	
	
	public static void main(String[] args)
	{
		try
		{
			sop("START");
			Map<Organism, DistanceDistribution[]> orgToDists = mapOrganismsToDistributions();
			for (Organism org: orgToDists.keySet())
			{
				if (org == null)
					continue;
				sop(org + ": ");
				DistanceDistribution distn = orgToDists.get(org)[0];
				if (distn == null)
					sop("  No same-operon data");
				else
				{
					sop("  Same operon: " + distn.size() + " pairs.");
					float[] ms = distn.getMeanAndStdDev();
					sop("  m/sd = " + ms[0] + "  " + ms[1]);
				}
				distn = orgToDists.get(org)[1];
				if (distn == null)
					sop("  No not-same-operon data");
				else
				{
					sop("  Not-same operon: " + distn.size() + " pairs.");
					float[] ms = distn.getMeanAndStdDev();
					sop("  m/sd = " + ms[0] + "  " + ms[1]);
				}
			}
			File serf = new File("analysis_data/OrganismTo2DistanceDistributions.ser");
			FileOutputStream fos = new FileOutputStream(serf);
			ObjectOutputStream oos = new ObjectOutputStream(fos);
			oos.writeObject(orgToDists);
			oos.flush();
			fos.flush();
			oos.close();
			fos.close();
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
