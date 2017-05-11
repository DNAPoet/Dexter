package dexter.model;

//
// If get finds no key, infers organism for Croco, Pro, and Tery IDs. FOR DEBUGGING ONLY!
//


public class InferringGeneIdToOrganismMap extends GeneIdToOrganismMap
{
	public Organism get(Object x)
	{
		if (super.containsKey(x))
			return super.get(x);
		
		String id = (String)x;
		if (id.startsWith("Cwat"))
			return Organism.CROCO;
		else if (id.startsWith("PMM"))
			return Organism.PRO;
		else if (id.startsWith("Tery"))
			return Organism.TERY;
		
		return null;
	}
	

	public boolean containsKey(Object x)
	{
		if (!(x instanceof String))
			return false;

		if (super.containsKey(x))
			return true;
		
		String id = (String)x;
		return id.startsWith("Cwat")  ||  id.startsWith("PMM")  ||  id.startsWith("Tery");
	}
	
	
	// Returns true if organism could be inferred.
	public boolean putInferred(String id)
	{
		Organism org = null;
		if (id.startsWith("Cwat"))
			org = Organism.CROCO;
		else if (id.startsWith("PMM"))
			org = Organism.PRO;
		else if (id.startsWith("Tery"))
			org = Organism.TERY;
		if (org == null)
			return false;
		put(id, org);
		return true;
	}
	
	
	public static void main(String[] args)
	{
		InferringGeneIdToOrganismMap map = new InferringGeneIdToOrganismMap();
		sop(map.containsKey("PMM1234"));
	}
}
