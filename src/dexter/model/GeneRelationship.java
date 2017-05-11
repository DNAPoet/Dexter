package dexter.model;

import java.util.*;


//
// This class can model any 1-1 relationship between lightweight genes, e.g. orthology.
//

public class GeneRelationship implements java.io.Serializable
{
	public LightweightGene			from;
	public LightweightGene			to;
	
	
	public GeneRelationship(LightweightGene queryGene, LightweightGene subjectGene)
	{
		this.from = queryGene;
		this.to = subjectGene;
	}
	
	
	public boolean isReverseOf(GeneRelationship that)
	{
		return this.from.equals(that.to)  &&  this.to.equals(that.from);
	}
	
	
	public boolean queryAndSubjectAreSameOrganism()
	{
		return from.getOrganism() == to.getOrganism();
	}
	
	
	public boolean equals(Object x)
	{
		GeneRelationship that = (GeneRelationship)x;
		return this.from.equals(that.from)  &&  this.to.equals(that.to);
	}
	
	
	public LightweightGene[] toArray()
	{
		return new LightweightGene[] { from, to };
	}
	
	
	public static Collection<GeneRelationship> maximallyConnect(Set<LightweightGene> genes, boolean bidirectional)
	{
		Set<GeneRelationship> ret = new HashSet<GeneRelationship>();
		
		for (LightweightGene g1: genes)
		{
			for (LightweightGene g2: genes)
			{
				if (g1 == g2)
					continue;
				GeneRelationship rel = new GeneRelationship(g1, g2);
				if (bidirectional)
					ret.add(rel);
				else
				{
					GeneRelationship rev = new GeneRelationship(g2, g1);
					if (!ret.contains(rev))
						ret.add(rel);
				}
			}
		}
		
		return ret;
	}
	
	
	public String toString()
	{
		return from + " >--> " + to + ": " ;//+ fracIdent + " identity over " + fracLen + " of length";
	}
}
