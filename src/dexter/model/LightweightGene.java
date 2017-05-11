package dexter.model;



//
// A lightweight gene. e.g. for computing orthologs. The full-blown dexter.model.Gene implementation might
// be inappropriate because orthologs could be needed before measurement values are loaded. (Loading is
// time-consuming.)
//
// Note: doesn't extend Gene, nor vice versa.
//


public class LightweightGene implements Comparable<LightweightGene>, java.io.Serializable
{
	private static final long 	serialVersionUID = 4990111696787397362L;
	
	private Organism			organism;
	private String				id;
	
	
	public LightweightGene(Organism organism, String id)
	{
		this.organism = organism;
		this.id = id;
	}
	
	
	public LightweightGene(Gene gene)
	{
		this(gene.getOrganism(), gene.getId());
	}
	
	
	public Organism getOrganism()
	{
		return organism;
	}
	
	
	public void setOrganism(Organism o)
	{
		assert this.organism == null;
		this.organism = o;
	}
	
	
	public String getId()
	{
		return id;
	}
	
	
	public String toString()
	{
		return organism + "." + id;
	}
	
	
	public boolean equals(Object x)
	{
		if (x instanceof LightweightGene)
		{
			LightweightGene that = (LightweightGene)x;
			return this.organism == that.organism  &&  this.id.equals(that.id);
		}
		
		else if (x instanceof Gene)
		{
			Gene that = (Gene)x;
			return this.organism == that.getOrganism()  &&  this.id.equals(that.getId());
		}
		
		else
			return false;
	}
	
	
	public int hashCode()
	{
		return id.hashCode();
	}


	public int compareTo(LightweightGene that) 
	{
		int orgComp = this.organism.compareTo(that.organism);
		if (orgComp != 0)
			return orgComp;
		return this.id.compareTo(that.id);
	}
}
