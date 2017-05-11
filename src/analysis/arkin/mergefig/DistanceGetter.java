package analysis.arkin.mergefig;

import java.io.File;
import java.util.*;

import dexter.cluster.*;
import dexter.model.*;


public class DistanceGetter {
	
	
	static void sop(Object x)		{ System.out.println(x); }
	
	
	public static void main(String[] args)
	{
		try
		{
			SessionModel session = SessionModel.deserialize(new File("data/Sessions/CPT.dex"));
			Study study = null;
			for (Study s: session.getStudies())
				if (s.getName().contains("Tery"))
					study = s;
			
			Map<String, Gene> idToGene = study.getIdToGeneMap();
			Vector<Gene> the4Genes = new Vector<Gene>();
			for (int i=4140; i<=4143; i++)
			{
				String id = "Tery_" + i;
				Gene gene = idToGene.get(id);
				the4Genes.add(gene);
			}
			Map<Gene, Vector<float[]>> geneToTx = session.mapGenesToTimeExpressionPairs(study);
			for (Gene g1: the4Genes)
			{			
				Vector<float[]> txs1 = geneToTx.get(g1);
				txs1 = Metric.adjustExpressionsToZeroMean(txs1);
				for (Gene g2: the4Genes)
				{
					if (g1 != g2)
					{			
						Vector<float[]> txs2 = geneToTx.get(g2);
						txs2 = Metric.adjustExpressionsToZeroMean(txs2);
						float dist = Metric.EUCLIDEAN.computeDistance(g1, txs1, g2, txs2);
						sop(g1.getId() + "--" + g2.getId() + "  = " + dist);
					}
				}
			}

			sop("Done");
		}
		catch (Exception x)
		{
			sop("Feh");
		}
	}

/**
Tery_4140--Tery_4141  = 5.012777
Tery_4140--Tery_4142  = 5.3641763
Tery_4140--Tery_4143  = 7.1207123
Tery_4141--Tery_4140  = 5.012777
Tery_4141--Tery_4142  = 2.402108
Tery_4141--Tery_4143  = 6.813837
Tery_4142--Tery_4140  = 5.3641763
Tery_4142--Tery_4141  = 2.402108
Tery_4142--Tery_4143  = 7.2158303
Tery_4143--Tery_4140  = 7.1207123
Tery_4143--Tery_4141  = 6.813837
Tery_4143--Tery_4142  = 7.2158303
	***/
}
