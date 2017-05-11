package dexter.view.cluster;


enum ColorBy 
{
	Subtree, Organism;
	
	
	static ColorBy getDefault()
	{
		return Subtree;
	}
}
