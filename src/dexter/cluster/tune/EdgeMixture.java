package dexter.cluster.tune;


enum EdgeMixture
{	
	LL("Leaf-Leaf"), II("Internal-Internal"), MIXED("Mixed");	
	
	
	private String		longName;
	
	
	EdgeMixture(String s)	
	{
		longName = s;
	}
	
	
	public String toString()
	{
		return longName;
	}
}

