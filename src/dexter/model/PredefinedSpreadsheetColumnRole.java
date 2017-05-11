// 1/31 - Rex says instead of kegg only, offer kegg/cog/pfam/other (functional categories)


package dexter.model;

import java.util.Collection;
import dexter.util.StringUtils;


public enum PredefinedSpreadsheetColumnRole 
{
	TIMEPOINT, ID, NAME, ANNOTATION, KEGG_PATHWAY;
	

	private final static String[]	ID_SYNONYMS 		= { "ID", "GI", "GENEID" };
	
	
	public String toString()
	{
		String[] pieces = name().split("_");
		String ret = "";
		for (String piece: pieces)
			ret += piece.charAt(0) + piece.toLowerCase().substring(1) + ' ';
		ret = ret.trim();
		return ret;
	}
	
	
	public static PredefinedSpreadsheetColumnRole fromString(String s)
	{
		assert s != null  :  "null input";
		
		s = s.trim().toUpperCase();
		if (s.isEmpty())
			return null;
		
		try
		{
			Integer.parseInt(s);
			return TIMEPOINT;
		}
		catch (NumberFormatException x) { }
		
		if (StringUtils.isTimestampString(s))
			return TIMEPOINT;
		
		for (String syno: ID_SYNONYMS)
			if (s.equals(syno))
				return ID;
		
		if (s.startsWith("ANNOTATION"))
			return ANNOTATION;
		
		if (s.contains("KEGG")  &&  s.contains("PATHWAY"))
			return KEGG_PATHWAY;
		
		return null;
	}
	
	
	public static PredefinedSpreadsheetColumnRole fromStrings(Collection<String> strings)
	{
		for (String line: strings)
		{
			if (line == null)
				continue;
			PredefinedSpreadsheetColumnRole type = fromString(line);
			if (type != null)
				return type;
		}
		return null;
	}
	
	
	public int[] getMinMaxColsThisTypeAllowedPerFile()
	{
		switch (this)
		{
			case TIMEPOINT:		return new int[] { 4, Integer.MAX_VALUE };
			case ID:			return new int[] { 1, 1 };
			default:			return new int[] { 0, 1 };
		}
	}
	
	
	public boolean isGroupable()
	{
		switch (this)
		{
			case TIMEPOINT:		
			case ID:		
			case NAME:			
			case ANNOTATION:	
				return false;
			default:		
				return true;
		}
	}
	
	
	static void sop(Object x)			{ System.out.println(x); }
}
