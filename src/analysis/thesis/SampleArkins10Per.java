package analysis.thesis;

import java.io.*;
import java.util.*;

import dexter.cluster.Metric;
import dexter.model.*;
import dexter.util.*;


public class SampleArkins10Per 
{
	public static String[] CROCO_PRED_OP_NAMES =
	{
		"0018", "0023", "0080", "0026", "0814", "0104", "0253", "0395", "0782", "0778"
	};
	
	public static String[] PRO_PRED_OP_NAMES =
	{
		"212", "211", "011", "006", "119", "151", "042", "035", "051", "102"
	};
	
	public static String[] TERY_PRED_OP_NAMES =
	{
		"158", "411", "514", "175", "780", "829", "343", "220", "174", "510"
	};

	static String OP_NAME_PREFIX = "Unnamed coreg group ";
	
	public static String[][] CROCO_IDS_BY_OP = {
		{ "CwatDRAFT_6695", "CwatDRAFT_6696", "CwatDRAFT_6697" },
		{ "CwatDRAFT_0576", "CwatDRAFT_0577" }, 
		{ "CwatDRAFT_1165", "CwatDRAFT_1166" },
		{ "CwatDRAFT_3691", "CwatDRAFT_3692" }, 
		{ "CwatDRAFT_4675", "CwatDRAFT_4676" },
		{ "CwatDRAFT_6389", "CwatDRAFT_6390" }, 
		{ "CwatDRAFT_6486", "CwatDRAFT_6487" },
		{ "CwatDRAFT_6672", "CwatDRAFT_6673" }, 
		{ "CwatDRAFT_0947", "CwatDRAFT_0948" },
		{ "CwatDRAFT_6660", "CwatDRAFT_6661" }
	};
	
	public static String[][] PRO_IDS_BY_OP = {
		{ "PMM0243", "PMM0244", "PMM0245" },
		{ "PMM0054", "PMM0055" }, 
		{ "PMM0087", "PMM0088" },
		{ "PMM0289", "PMM0290" }, 
		{ "PMM0650", "PMM0651" },
		{ "PMM0779", "PMM0780" }, 
		{ "PMM0979", "PMM0980" },
		{ "PMM1436", "PMM1437" }, 
		{ "PMM1438", "PMM1439" },
		{ "PMM0320", "PMM0321" }
	};
	
	public static String[][] TERY_IDS_BY_OP = {
		{ "Tery_0996", "Tery_0997", "Tery_0998" },
		{ "Tery_2483", "Tery_2484", "Tery_2485" }, 
		{ "Tery_1090", "Tery_1091" }, 
		{ "Tery_1092", "Tery_1093" },
		{ "Tery_1345", "Tery_1346" }, 
		{ "Tery_2090", "Tery_2091" },
		{ "Tery_3090", "Tery_3091" },
		{ "Tery_3111", "Tery_3112" }, 
		{ "Tery_4704", "Tery_4705" },
		{ "Tery_5048", "Tery_5049" }
	};
	
	
	
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
	
	
	static Vector<Float> getPairwiseDistances(Study study, Map<Gene, Vector<float[]>> geneToTx, String[] ids)
	{
		assert ids.length == 2  ||  ids.length == 3;
		
		Map<String, Gene> idToGene = study.getIdToGeneMap();
		Vector<Gene> genes = new Vector<Gene>();
		for (String id: ids)
		{
			Gene gene = idToGene.get(id);
			assert gene != null;
			genes.add(gene);
		}
		assert genes.size() == ids.length;
		Vector<Vector<float[]>> txsVec = new Vector<Vector<float[]>>();
		for (Gene gene: genes)
			txsVec.add(geneToTx.get(gene));
		
		Vector<Float> ret = new Vector<Float>();
		ret.add(dist(txsVec.get(0), txsVec.get(1)));
		if (ids.length == 3)
		{
			ret.add(dist(txsVec.get(0), txsVec.get(2)));
			ret.add(dist(txsVec.get(1), txsVec.get(2)));
		}
		
		return ret;
	}
	
	
	static float dist(Vector<float[]> txs1, Vector<float[]> txs2) {
		Vector<float[]> txs1ZeroMean = Metric.adjustExpressionsToZeroMean(txs1);
		Vector<float[]> txs2ZeroMean = Metric.adjustExpressionsToZeroMean(txs2);
		return Metric.computeEuclideanDistance(txs1ZeroMean, txs2ZeroMean);
	}
	
	
	static float[] minMeanMax(Vector<Float> distances)
	{
		float[] ret = new float[] {Float.MAX_VALUE, 0f, -1 };
		for (Float dist: distances)
		{
			ret[0] = Math.min(ret[0], dist);
			ret[1] += dist;
			ret[2] = Math.max(ret[2], dist);
		}
		ret[1] /= distances.size();
		return ret;
	}
	
	
	static void checkAll(Study study)
	{
		Map<Gene, Vector<float[]>> geneToTx = session.mapGenesToTimeExpressionPairs(study);
		Vector<Float> distances = new Vector<Float>();
		for (Gene g1: geneToTx.keySet())
		{
			Vector<float[]> txs1 = geneToTx.get(g1);
			for (Gene g2: geneToTx.keySet())
			{
				if (g1 == g2)
					continue;
				distances.add(dist(txs1, geneToTx.get(g2)));
				if (distances.size() % 2500 == 0)
					sop(distances.size());
			}
		}
		sop(study.getName());
		float[] minMeanMax = minMeanMax(distances);
		sop("  min=" + minMeanMax[0] + ", mean=" + minMeanMax[1] + ", max=" + minMeanMax[2]);
	}
	
	
	static void sop(Object x)				{ System.out.println(x); }
	
	
	public static void main(String[] args)
	{
		try
		{
			sop("START");
			for (Study study: session.getStudies())
			{
				if (!study.getName().equals("Shi_Croco"))
					continue;
				sop(study.getName());
				checkAll(study);
				/********************
				Map<Gene, Vector<float[]>> geneToTx = session.mapGenesToTimeExpressionPairs(study);
				Vector<Float> distances = new Vector<Float>();
				String[][] idLists = null;
				if (study.getOrganism().equals(Organism.CROCO))
					idLists = CROCO_IDS_BY_OP;
				else if (study.getOrganism().equals(Organism.PRO))
					idLists = PRO_IDS_BY_OP;
				else if (study.getOrganism().equals(Organism.TERY))
					idLists = TERY_IDS_BY_OP;
				for (String[] op: idLists)
					distances.addAll(getPairwiseDistances(study, geneToTx, op));
				float[] minMeanMax = minMeanMax(distances);
				*****/
				sop(".....");
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



/*****  10 per only:
Shilova_Tery
   Min/Mean/Max = 1.2312005/3.349897/6.974141
Shi_Croco
   Min/Mean/Max = 2.9547868/6.225865/16.425219
Zinser_Pro
   Min/Mean/Max = 1.5519122/3.278705/9.1727495
   
   
   Whole genome:
Shilova_Tery
  min=0.37215784, mean=12.291018, max=110.937
Zinser_Pro
  min=0.15967245, mean=8.65261, max=110.167725
Shi_Croco
  min=0.20685457, mean=19.714062, max=134.15054
*****/

