package dexter.model;

import java.util.*;


//
// Easy to construct, for test cases where all you need is a best available name.
//


public class DebugGene extends Gene
{
	private String 			bestAvailableName;
	private Organism		organism;
	
	
	public DebugGene(String bestAvailableName)
	{
		this.bestAvailableName = bestAvailableName;
	}
	
	
	public String getBestAvailableName()
	{
		return bestAvailableName;
	}
	
	
	public void setBestAvailableName(String bestAvailableName)
	{
		this.bestAvailableName = bestAvailableName;
	}
	
	
	public Organism getOrganism()
	{
		return organism;
	}
	
	
	public void setOrganism(Organism o)
	{
		this.organism = o;
	}
	
	
	public static Vector<DebugGene> buildTestCases(int nGenes)
	{
		Vector<DebugGene> ret = new Vector<DebugGene>(nGenes);
		for (int i=0; i<nGenes; i++)
			ret.add(new DebugGene("Test_" + i));
		return ret;
	}
}
