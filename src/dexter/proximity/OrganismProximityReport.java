package dexter.proximity;

import java.util.*;

import dexter.model.*;


//
// Models the result of comparing a set of genes for an organism against that organism's gene order. Gets aggregated
// into a MultiOrganismProximityReport instance.
//


class OrganismProximityReport
{
	private IdListForOrganism				idListForOrganism;
	private LinkedHashMap<Gene, Integer>	geneToContigIndex;	// index is arbitrary
	private Vector<Integer>					distances;			// [n] = distance from gene n to gene n+1
	
	
	OrganismProximityReport(Collection<Gene> rawGenes, IdListForOrganism idListForOrganism)
	{
		this.idListForOrganism = idListForOrganism;
		
		// Collect genes by index.
		Map<Integer, Gene> indexToGene = new TreeMap<Integer, Gene>();
		for (Gene gene: rawGenes)
		{
			assert idListForOrganism.getOrganism() != null;
			assert gene.getOrganism().equals(idListForOrganism.getOrganism());
			int index = idListForOrganism.indexOf(gene.getId());
			if (index >= 0)
				indexToGene.put(index, gene);
		}
		
		// Compute distances.
		geneToContigIndex = new LinkedHashMap<Gene, Integer>();
		distances = new Vector<Integer>();
		Gene g1 = null;
		Gene g2 = null;
		int indexOfG1 = -12345;
		for (Integer indexOfG2: indexToGene.keySet())
		{
			g2 = indexToGene.get(indexOfG2);
			if (g1 != null)
			{
				int distG1G2 = indexOfG2 - indexOfG1;
				assert distG1G2 >= 1;
				geneToContigIndex.put(g1, idListForOrganism.getContigIndex(g1));
				distances.add(distG1G2);
			}
			g1 = g2;
			indexOfG1 = indexOfG2;
		}
		geneToContigIndex.put(g2, idListForOrganism.getContigIndex(g2));
		assert geneToContigIndex.size() == distances.size() + 1;
	}
	
	
	public String toString()
	{
		String s = "OrganismProximityReport:\n  ";
		Vector<Gene> genes = getGenes();
		Gene firstGene = genes.remove(0);
		assert genes.size() == distances.size();
		s += firstGene.getId();
		int lastContig = geneToContigIndex.get(firstGene);
		for (int i=0; i<genes.size(); i++)
		{
			Gene gene = genes.get(i);
			int dist = distances.get(i);
			int nextContig = geneToContigIndex.get(gene);
			s += (lastContig == nextContig)  ?  "--" + dist + "--"  :  "-/" + dist + "/-";
			s += gene.getBestAvailableName();
			lastContig = nextContig;
		}
		return s;
	}
	
	
	Vector<Gene> getGenes()
	{
		return new Vector<Gene>(geneToContigIndex.keySet());
	}
	
	
	Vector<Integer> getdDistancesNegMeansDifferentContig()
	{
		Vector<Integer> ret = new Vector<Integer>();
		
		Vector<Gene> genes = getGenes();
		Gene firstGene = genes.remove(0);
		assert genes.size() == distances.size();
		int lastContig = geneToContigIndex.get(firstGene);
		for (int i=0; i<genes.size(); i++)
		{
			Gene gene = genes.get(i);
			int dist = distances.get(i);
			int nextContig = geneToContigIndex.get(gene);
			if (nextContig != lastContig)
				dist = -dist;
			ret.add(dist);
			lastContig = nextContig;
		}
		
		return ret;
	}
	
	
	// Returns { position within contig, # genes in contig, contig #, # contigs }.
	int[] getContigPositionForGene(Gene gene)
	{
		assert geneToContigIndex.containsKey(gene);

		int position = idListForOrganism.getPositionInContig(gene);
		int contigIndex = idListForOrganism.getContigIndex(gene);
		int nGenesInContig = idListForOrganism.getNGenesInContig(contigIndex);
		int nContigs = idListForOrganism.getNContigs();
		return new int[] { position, nGenesInContig, contigIndex, nContigs };
	}
	
	
	public IdListForOrganism getIdListForOrganism()
	{
		assert idListForOrganism != null;
		assert !idListForOrganism.isEmpty();
		
		return idListForOrganism;
	}
	
	
	static void sop(Object x)			{ System.out.println(x); }
	
	
	public static void main(String[] args)
	{
		MultiOrganismProximityReport multiReport = MultiOrganismProximityReport.buildTestInstance();
		OrganismProximityReport that = multiReport.get(Organism.CROCO);
		sop(that);
	}
} 
