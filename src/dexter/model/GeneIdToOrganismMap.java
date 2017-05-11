package dexter.model;

import java.util.*;


//
// Not much more than a map. Provides some debugging help.
//


public class GeneIdToOrganismMap extends HashMap<String, Organism>
{
	public GeneIdToOrganismMap()		{ }
	
	
	public GeneIdToOrganismMap(Collection<Study> studies)
	{
		for (Study study: studies)
		{
			for (Gene gene: study)
			{
				assert gene != null  :  "null gene in study " + study.getName();
				assert gene.getOrganism() != null  :  "null organism for gene " + gene;
				put(gene.getId(), gene.getOrganism());
			}
		}
	}
	
	
	public String toString()
	{
		return getClass().getName() + " containing " + size() + " gene IDs and " + values().size() + " organisms";
	}
	
	
	public static GeneIdToOrganismMap buildSmallTestInstance()
	{
		GeneIdToOrganismMap ret = new GeneIdToOrganismMap();
		ret.put("CwatAAA", Organism.CROCO);
		ret.put("PMM12345", Organism.PRO);
		ret.put("TerySomething", Organism.TERY);
		return ret;
	}
	
	
	static void sop(Object x)		{ System.out.println(x); }
}
