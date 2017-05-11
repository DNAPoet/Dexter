package dexter.coreg;

import java.util.*;
import dexter.model.*;


// Members are ids.
public class CoregulationGroup extends Vector<String>
{
	private String 					name;
	
	
	public CoregulationGroup()		{ }
	
	
	public CoregulationGroup(String name)
	{
		this.name = name;
	}
	
	
	public boolean contains(Gene gene)
	{
		for (String id: this)
			if (gene.getId().equals(id))
				return true;
		return false;
	}
	
	
	public String toString()
	{
		String s = "CoregulationGroup";
		if (name != null)
			s += " " + name;
		s += ": ";
		for (String id: this)
			s += id + " ";
		return s.trim();
	}
	
	
	public String getName()
	{
		return name;
	}
	
	
	public void setName(String name)
	{
		this.name = name;
	}
}
