package dexter.model;

import java.awt.Color;
import java.util.*;


public class ColorMap extends LinkedHashMap<Gene, Color>
{
	private Color				defaultColor;
	
	
	public ColorMap()		{ }
	
	
	public ColorMap(ColorMap src)
	{
		for (Gene gene: src.keySet())
			put(gene, src.get(gene));
	}
	
	
	public ColorMap(Color defaultColor)
	{
		this.defaultColor = defaultColor;
	}
	
	
	public Color get(Gene gene)
	{
		Color c = super.get(gene);
		return (c != null)  ?  c  :  defaultColor;
	}
	
	
	public String toString()
	{
		String s = "";
		for (Gene gene: keySet())
			s += gene.getId() + "  " + get(gene) + "\n";
		return s.trim();
	}
}
