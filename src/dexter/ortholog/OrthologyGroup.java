package dexter.ortholog;

import java.util.*;
import dexter.model.*;


public class OrthologyGroup extends HashSet<LightweightGene> implements java.io.Serializable 
{
	private static final long 	serialVersionUID = 7971283850745256473L;
	
	private OrthologyGraph		graph;
	
	
	OrthologyGroup(OrthologyGraph graph)
	{ 
		this.graph = graph;
	}		
	
	
	public String toString()
	{
		String s = "";
		for (LightweightGene g: this)
			s += "," + g.getId();
		return s.substring(1);
	}
	
	
	// Uses the session to look up names of any named genes.
	public String toString(SessionModel session)
	{
		int nNamedGenes = 0;
		String s = "";
		for (LightweightGene lwg: this)
		{
			s += ",";
			Gene g = session.lightweightGeneToGene(lwg);
			if (g == null)
				s += "?" + lwg.getId() + "?";
			else
			{
				s += g.getBestAvailableName();
				if (g.getName() != null)
					nNamedGenes++;
			}
		}
		s = s.substring(1);
		if (nNamedGenes > 0)
			s += "{" + nNamedGenes + "} " + s;
		return s;
	}

	
	boolean overlaps(OrthologyGroup that)
	{
		for (LightweightGene g: this)
			if (that.contains(g))
				return true;
		return false;
	}
	
	
	public HashSet<GeneRelationship> collectEdges()
	{
		HashSet<GeneRelationship> ret = new HashSet<GeneRelationship>();
		for (LightweightGene g: this)
			if (graph.containsKey(g)  &&  graph.get(g) != null)
				ret.addAll(graph.get(g));
		return ret;
	}
	
	
	public Set<Organism> collectOrganisms()
	{
		Set<Organism> ret = new TreeSet<Organism>();
		for (LightweightGene gene: this)
			ret.add(gene.getOrganism());
		return ret;
	}
	
	
	public int nOrganisms()
	{
		return collectOrganisms().size();
	}
	
	
	public Set<Gene> toHeavyweight(SessionModel session)
	{
		Set<Gene> ret = new HashSet<Gene>();
		
		for (Study study: session.getStudies())
		{
			if (study.isExperimentsStudy())
				continue;
			for (Gene gene: study)
				for (LightweightGene lwg: this)
					if (lwg.equals(gene))
						ret.add(gene);
		}
		
		return ret;
	}
	
	
	public boolean contains(Organism organism, String id)
	{
		for (LightweightGene lwg: this)
			if (lwg.getOrganism().equals(organism)  &&  lwg.getId().equals(id))
				return true;
		
		return false;
	}
}
