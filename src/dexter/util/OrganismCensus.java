package dexter.util;

import java.util.*;
import dexter.model.*;
import dexter.util.*;


public class OrganismCensus extends BinCounter<Organism> implements Comparable<OrganismCensus>
{
	public OrganismCensus(Collection<LightweightGene> genes)
	{
		this(genes, new HashSet<Organism>());
	}
	
	
	// Ensures all organisms are represented, by adding count-zero entries if necessary.
	public OrganismCensus(Collection<LightweightGene> genes, HashSet<Organism> allOrganisms)
	{
		for (LightweightGene gene: genes)
		{
			Organism org = gene.getOrganism();
			bumpCountForBin(org);
		}
		
		for (Organism org: allOrganisms)
			ensureBinExists(org);
	}
	
	
	// Only count organisms with non-zero populations.
	private int countRepresentedOrganisms()
	{
		int n = 0;
		for (int[] count: values())
			if (count[0] > 0)
				n++;
		return n;
	}
	
	
	private SortableList<Integer> collectPopulationSizes(boolean zeroSizeOk)
	{
		SortableList<Integer> ret = new SortableList<Integer>();
		for (int[] sizes: values())
			if (zeroSizeOk  ||  sizes[0] > 0)
				ret.add(sizes[0]);
		ret.sort();
		return ret;
	}
	
	
	public String toSummaryString()
	{
		String s = "";
		for (Organism org: keySet())
			if (getCountForBin(org) > 0)
				s += org + " ";
		return s.trim();
	}
	
	
	public String organismsToString()
	{
		String s = "";
		for (Organism org: keySet())
			s += org + "|";
		return s.substring(0, s.length()-1);
	}
	
	
	public String toString()
	{
		String ret = "";
		for (Organism org: keySet())
			ret += org + "  ";
		return ret.trim();
	}
	
	
	public void ensureBinsExist(Collection<Organism> organisms)
	{
		for (Organism org: organisms)
			ensureBinExists(org);
	}
	
	
	public int compareTo(OrganismCensus that) 
	{
		// 1st criterion: # of organisms.
		int nOrgsDiff = this.countRepresentedOrganisms() - that.countRepresentedOrganisms();
		if (nOrgsDiff != 0)
			return nOrgsDiff;
		
		// Same # of organisms. If same organisms, compare by organism in lexical order.
		if (this.keySet().equals(that.keySet()))
		{
			for (Organism org: keySet())
			{
				int countDiff = this.getCountForBin(org) - that.getCountForBin(org);
				if (countDiff != 0)
					return countDiff;
			}
			return 0;
		}
		
		// Same # of organisms, but different organisms. Check populations of organisms; if equal, 
		// check 2nd-most represented organisms, and so on. SortableList makes this easy.
		SortableList<Integer> theseSizes = this.collectPopulationSizes(false);
		SortableList<Integer> thoseSizes = that.collectPopulationSizes(false);
		int diff = theseSizes.compareTo(thoseSizes);
		if (diff != 0)
			return diff;
		
		// Last criterion: represented organisms, alphabetically.
		return this.organismsToString().compareTo(that.organismsToString());
	}
}
