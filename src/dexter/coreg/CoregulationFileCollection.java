package dexter.coreg;

import java.util.*;
import java.io.*;

import dexter.model.*;
import dexter.util.HashMapIgnoreKeyCase;


//
// Files are comma-separated lines of gene ids. Optional: line may begin with name of
// operon/pathway followed by colon.
//


public class CoregulationFileCollection extends TreeMap<Organism, Vector<CoregulationFile>> implements Serializable
{
	private static final long 		serialVersionUID 			= 388060247215899966L;
	
	
	public CoregulationFileCollection()		{ }
	
	
	public CoregulationFileCollection(CoregulationFileCollection src)
	{
		for (Organism org: src.keySet())
			put(org, src.get(org));
	}
	
	
	public String toString()
	{
		String s = "CoregulationFileCollection:\n";
		for (Organism org: keySet())
		{
			s += "  " + org.getShortestName() + " => \n";
			for (CoregulationFile file: get(org))
				s += "    " + file.getName() + "\n";
		}
		return s;
	}
	
	public void add(CoregulationFile coregFile)
	{
		if (!containsKey(coregFile.getOrganism()))
			put(coregFile.getOrganism(), new Vector<CoregulationFile>());
		get(coregFile.getOrganism()).add(coregFile);
	}
	
	
	public Vector<CoregulationGroup> getCoregulationGroups(Study study) throws IOException
	{
		return getCoregulationGroups(study.getOrganism());
	}
	
	
	// Returns an empty container if no files for specified organism.
	public Vector<CoregulationGroup> getCoregulationGroups(Organism org) throws IOException
	{
		Vector<CoregulationGroup> ret = new Vector<CoregulationGroup>();
		if (containsKey(org))
			for (CoregulationFile coregFile: get(org))
				ret.addAll(coregFile.getCoregulationGroups());
		return ret;
	}
	
	
	public Map<Organism, Vector<CoregulationGroup>> getCoregulationGroups() throws IOException
	{ 				
		Map<Organism, Vector<CoregulationGroup>> ret = new TreeMap<Organism, Vector<CoregulationGroup>>();
		for (Organism org: keySet())
			ret.put(org, getCoregulationGroups(org));
		return ret;
	}
	
	
	static void sop(Object x)				{ System.out.println(x); }
}
