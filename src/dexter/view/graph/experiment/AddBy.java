package dexter.view.graph.experiment;


enum AddBy 
{
	Expression_Similarity, 			// n most similarly expressed genes, selection is exactly 1 gene
	Gene_Name, 		
	Pathway, 			
	Orthology,						// all genes orthologous to any selected gene
	Operon;
	
	
	public String toString()
	{
		String[] pieces = name().split("_");
		return (pieces.length == 1)  ?  name()  :  pieces[0] + " " + pieces[1];
	}
	
	
	boolean selectionCountIsOk(int nSelectedGenes)
	{
		switch (this)
		{
			case Expression_Similarity:
			case Operon:
				return nSelectedGenes == 1;
				
			case Orthology:
				return nSelectedGenes >= 1;
			
			default:
				return true;
		}
	}
	
	
	// Returns null if no tool tip.
	String getTooTipText()
	{
		switch (this)
		{
			case Expression_Similarity:
				return "Select 1 gene, add genes with most similar expression.";
			case Orthology:
				return "Select 1 or more genes, add genes that are orthologous to selected.";
			default:
				return null;
		}
	}
	
	
	// Buttons for instances that might be slow get a red clock icon.
	boolean mightBeSLow()
	{
		return false;
	}
}
