package analysis.arkin;

import java.io.*;
import java.util.*;

import dexter.util.*;
import dexter.model.*;
import dexter.cluster.Metric;
import dexter.coreg.*;
import dexter.proximity.*;


class OperonChecker
{
	private Study							study;
	private Map<String, Gene>				idToGene;
	private CoregulationFile				coregFile;
	private Vector<Operon>					operons;
	private IdListForOrganism 				orderedIdList;
	private Map<String, Integer> 			geneIdToGeneIndex;
	private Operon[] 						geneNameToOperon;
	private OrganismDistanceMatrix			distances;
	
	
	OperonChecker(Study study) throws IOException
	{
		this.study = study;
		
		idToGene = study.getIdToGeneMap();
		assert idToGene != null;
		
		// Read operon list.
		coregFile = new CoregulationFile(study.getOrganism());
		operons = new Vector<Operon>();
		for (CoregulationGroup cgrp: coregFile.getCoregulationGroups())
			operons.add(new Operon(cgrp));
		
		orderedIdList = IdListForOrganism.forProvidedOrganism(study.getOrganism());
		
		// Cull unstudied genes from operons, then cull empty operons.
		Set<Operon> emptyOperons = new HashSet<Operon>();
		Collection<String> studiedIds = study.getIds();
		for (Operon operon: operons)
		{
			operon.retainAll(studiedIds);
			if (operon.size() < 2)
				emptyOperons.add(operon);
		}
		operons.removeAll(emptyOperons);
		
		// Distance matrix for all genes in the organism.
		distances = new OrganismDistanceMatrix(study);
		
		// Set mean internal distances for operons.
		Map<String, Gene> idToGene = study.getIdToGeneMap();
		for (Operon op: operons)
		{
			Set<Gene> genes = new HashSet<Gene>();
			for (String id: op)
				genes.add(idToGene.get(id));
			float meanDist = Metric.EUCLIDEAN.getMeanDistance(genes, distances);
			op.setMeanInternalPairwiseDist(meanDist);
		}

		// Get entire genome as ordered list of gene ids. Map i.d. to offset.
		geneIdToGeneIndex = new HashMap<String, Integer>();
		int offset = 0;
		for (String id: orderedIdList)
			geneIdToGeneIndex.put(id, offset++);
		
		// Compute operon layout. If gene n belongs to an operon, then operonByGeneNum[n] = that operon,
		// otherwise operonByGeneNum[n] = null;
		Operon[] geneNameToOperon = new Operon[orderedIdList.size()];
		int nameNum = 1;
		for (Operon operon: operons)
		{
			operon.setName("Operon#" + nameNum++);
			for (String id: operon)
			{
				if (!geneIdToGeneIndex.containsKey(id))
					continue;
				offset = geneIdToGeneIndex.get(id);
				geneNameToOperon[offset] = operon;
			}
		}
	}
	
	
	void checkSpread()
	{
		BinCounter<Integer> operonSizeBinCtr = new BinCounter<Integer>();
		float minMeanDist = Float.MAX_VALUE;
		float maxMinDist = -1f;
		
		for (Operon op: operons)
		{
			operonSizeBinCtr.bumpCountForBin(op.size());
			minMeanDist = Math.min(minMeanDist, op.getMeanInternalPairwiseDist());
			maxMinDist = Math.max(maxMinDist, op.getMeanInternalPairwiseDist());
		}
		
		sop(operonSizeBinCtr);
		sop("********\n" + minMeanDist + " .. " + maxMinDist);
	}
	
	
	Vector<OperonPair> check()
	{
		Vector<OperonPair> predictedPairs = new Vector<OperonPair>();
		
		for (int i=0; i<operons.size()-1; i++)
		{
			Operon op1 = operons.get(i);
			Operon op2 = operons.get(i+1);
			OperonPair pair = new OperonPair(op1, op2, distances, idToGene);
			if (pair.predictedSameOperon())
				predictedPairs.add(pair);
		}
		
		return predictedPairs;
	}
	
	
	Vector<Operon> getOperons()	{ return operons; }
	static void sop(Object x)	{ System.out.println(x); }
	
	
	public static void main(String[] args)
	{
		try
		{
			sop("START");
			SessionModel session = OrganismDistanceMatrix.getSession();
			for (Study study: session.getStudies())
			{
				OperonChecker checker = new OperonChecker(study);
				sop(study.getName());
				checker.checkSpread();
			}
		}
		catch (Exception x)
		{
			sop("Stress: " + x.getMessage());
			x.printStackTrace(System.out);
		}
		finally
		{
			sop("DONE");
		}
	}
}
