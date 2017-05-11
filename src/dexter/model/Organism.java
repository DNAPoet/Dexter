package dexter.model;


//
// For now this class just encapsulates a string. Later it may be necessary to
// encapsulate an abbreviation as well, and to internalize the name.
//


public class Organism implements Comparable<Organism>, java.io.Serializable
{
	private static final long 			serialVersionUID 	= -6243048799082274518L;
	
	public final static Organism		CROCO				= new Organism("Crocosphaera watsonii", "Croco");
	public final static Organism		PRO					= new Organism("Prochlorococcus marinus", "Pro");
	public final static Organism		TERY				= new Organism("Trichodesmium erythraeum", "Tery");
	public final static Organism[]		PROVIDED			= { CROCO, PRO, TERY };
	
	
	private String		name;
	private String		shortName;
	
	
	public Organism(String name)
	{
		this.name = name;
	}
	
	
	public Organism(String name, String shortName)
	{
		this(name);
		this.shortName = shortName;
	}
	
	
	public String getName()
	{
		return name;
	}
	
	
	public String getShortName()
	{
		return shortName;
	}
	
	
	public String getShortestName()
	{
		return (shortName != null)  ?  shortName  :  name;
	}
	

	public String toString()
	{
		return name;
	}
	
	
	public boolean equals(Object x)
	{
		return this.name.equals(((Organism)x).name);
	}
	
	
	public int hashCode()
	{
		return name.hashCode();
	}


	public int compareTo(Organism that)
	{
		return this.name.compareTo(that.name);
	}
	
	
	public boolean isProvided()
	{
		for (Organism o: PROVIDED)
			if (this == o)
				return true;
		return false;
	}
	
	
	public Organism toProvided()
	{
		for (Organism o: PROVIDED)
			if (this.name.equals(o.name)  &&  this.shortName.equals(o.shortName))
				return o;
		return null;
	}
	
	
	static void sop(Object x)			{ System.out.println(x); }	
}
