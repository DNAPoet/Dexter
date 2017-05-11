package analysis.arkin;

import java.io.*;
import java.util.*;

import org.apache.commons.math3.distribution.*;

import dexter.cluster.Metric;
import dexter.model.*;


public class CutPriors 
{
	static SessionModel 			session = getSession();
	final static File				DIRF = new File("analysis_data/Export");	
	final static NormalDistribution	CROCO_NEG_DIST			= new NormalDistribution(19.8, 12);	
	final static NormalDistribution	PRO_NEG_DIST			= new NormalDistribution(16.3, 10.7);	
	final static NormalDistribution	TERY_NEG_DIST			= new NormalDistribution(13.2, 7.22);


	Study									study;
	private Map<Gene, Vector<float[]>> 		geneToTx;
	private Map<String, Gene> 				idToGene;
	Vector<Operon>							longOps;		// len >= 4
	NormalDistribution						negDist;
	boolean									terse = true;
	
	
	CutPriors(Study study) throws IOException
	{
		this.study = study;
		Map<Gene, Vector<float[]>> geneToTxRaw = session.mapGenesToTimeExpressionPairs(study);
		geneToTx = new HashMap<Gene, Vector<float[]>>();
		for (Gene g: geneToTxRaw.keySet())
		{
			geneToTx.put(g, Metric.adjustExpressionsToZeroMean(geneToTxRaw.get(g)));
		}
		assert geneToTxRaw.size() == geneToTx.size();
		idToGene = study.getIdToGeneMap();
		
		longOps = new Vector<Operon>();
		FileReader fr = new FileReader(getPriorsFileForStudy(study));
		BufferedReader br = new BufferedReader(fr);
		int lastOpNum = -1;
		br.readLine();
		String line = null;
		while ((line = br.readLine()) != null)
		{
			String[] pieces = line.split(",");
			int opNum = Integer.parseInt(pieces[0]);
			String id = pieces[1];
			if (opNum != lastOpNum)
			{
				lastOpNum = opNum;
				Operon op = new Operon();
				op.add(id);
				longOps.add(op);
			}
			else
				longOps.lastElement().add(id);
		}
		sop("BEFORE: " + longOps.size());
		
		Set<Operon> shortOps = new HashSet<Operon>();
		for (Operon op: longOps)
			if (op.size() < 4)
				shortOps.add(op);
		longOps.removeAll(shortOps);
		sop("AFTER: " + longOps.size());
		
		if (study.getOrganism().getName().toUpperCase().charAt(0) == 'C')
			negDist = CROCO_NEG_DIST;
		else if (study.getOrganism().getName().toUpperCase().charAt(0) == 'P')
			negDist = PRO_NEG_DIST;
		else
			negDist = TERY_NEG_DIST;
	}
	
	
	void check()
	{
		for (Operon op: longOps)
		{
			assert op.size() >= 4;
			Vector<Double> es = checkAllCuts(op);
			String s = study.getOrganism().getShortestName() + ":";
			double max = -1;
			for (Double d: es)
			{
				s += "  " + d;
				max = Math.max(max, d);
			}
			if (!terse)
				sop(s);
			else
			{
				if (max != -1)
					sop(study.getOrganism().getShortestName() + ": " + max);
			}
		}
	}
	
	
	Vector<Double> checkAllCuts(Operon op)
	{
		assert op.size() >= 4;
		Vector<Double> ret = new Vector<Double>();
		int indexOfA = 1;
		while (indexOfA+3 < op.size())
		{
			ret.add(e(op, indexOfA++));
		}
		return ret;
	}
	
	
	double e(Operon op, int indexOfA)
	{
		assert op.size() >= 4;
		
		Gene geneA = idToGene.get(op.get(indexOfA));
		Gene geneB = idToGene.get(op.get(indexOfA+1));
		Gene geneC = idToGene.get(op.get(indexOfA+2));
		Gene geneD = idToGene.get(op.get(indexOfA+3));
		
		Vector<Double> distances = new Vector<Double>();
		distances.add(dist(geneA, geneC));
		distances.add(dist(geneA, geneD));
		distances.add(dist(geneB, geneC));
		distances.add(dist(geneB, geneD));
		
		double e = 1;
		for (double d: distances)
		{
			double cum = negDist.cumulativeProbability(d);
			e *= cum;
		}
		return e;
	}
	
	
	double dist(Gene g1, Gene g2)
	{
		Vector<float[]> txs1 = geneToTx.get(g1);
		Vector<float[]> txs2 = geneToTx.get(g2);
		return Metric.EUCLIDEAN.computeDistance(g1, txs1, g2, txs2);
	}
	
	
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

	
	static File getPriorsFileForStudy(Study study)
	{
		if (study.getOrganism().getName().toUpperCase().charAt(0) == 'C')
			return new File(DIRF, "Croco_Prior_Operons.csv");
		else if (study.getOrganism().getName().toUpperCase().charAt(0) == 'P')
			return new File(DIRF, "Pro_Prior_Operons.csv");
		else
			return new File(DIRF, "Tery_Prior_Operons.csv");
	}
	
	
	static void sop(Object x)				{ System.out.println(x); }
	
	
	public static void main(String[] args)
	{
		try
		{
			sop("START");
			for (Study study: session.getStudies())
			{
				sop(study.getName());
				CutPriors that = new CutPriors(study);
				that.check();
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
