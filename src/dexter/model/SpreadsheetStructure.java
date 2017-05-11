package dexter.model;

import java.io.*;
import java.util.*;


public class SpreadsheetStructure implements java.io.Serializable
{
	private static final long 								serialVersionUID = -3413214109325750846L;

	private File											file;
	private Map<Integer, String>							colNumToName;				// ordered by column
	private Map<Integer, PredefinedSpreadsheetColumnRole>	colNumToRole;				// ordered by column
	private Map<Integer, String>							colNumToUserDefinedRole;	// ordered by column
	private int												dataStartRow;
	
	
	public SpreadsheetStructure(File file)
	{
		this.file = file;
		colNumToName = new TreeMap<Integer, String>();
		colNumToRole = new TreeMap<Integer, PredefinedSpreadsheetColumnRole>();
		colNumToUserDefinedRole = new TreeMap<Integer, String>();
		dataStartRow = 1;
	}
	
	
	public String toString()
	{
		String s = "SpreadsheetStructure for file " + file.getAbsolutePath() + "\nData starts at row " + dataStartRow;
		for (Integer i: colNumToRole.keySet())
			s += "\n  col# " + i + " = \"" + colNumToName.get(i) + "\" has role " + colNumToRole.get(i);
		return s;
	}
	
	
	public void setRoleAndNameForColumnNumber(int colNum, PredefinedSpreadsheetColumnRole role, String name)
	{
		setRoleForColumnNumber(colNum, role);
		setNameForColumnNumber(colNum, name);
	}
	
	
	public void setUserDefinedRoleAndNameForColumnNumber(int colNum, String role, String name)
	{
		setUserDefinedRoleForColumnNumber(colNum, role);
		setNameForColumnNumber(colNum, name);
	}
	
	
	public Vector<String> getTimepointColumnNames()
	{
		Vector<String> ret = new Vector<String>();
		for (Integer i: colNumToName.keySet())
			if (colNumToRole.get(i) == PredefinedSpreadsheetColumnRole.TIMEPOINT)
				ret.add(colNumToName.get(i));
		return ret;
	}
	
	
	public Vector<Integer> getColNumsOfTimepoints()
	{
		Vector<Integer> ret = new Vector<Integer>();
		for (Integer col: colNumToRole.keySet())
			if (colNumToRole.get(col) == PredefinedSpreadsheetColumnRole.TIMEPOINT)
				ret.add(col);
		return ret;
	}	
	
	
	public int getColNumOfId()
	{
		for (Integer i: colNumToRole.keySet())
			if (colNumToRole.get(i) == PredefinedSpreadsheetColumnRole.ID)
				return i;
		assert false : "No ID column in spreadsheet column model.";
		return -1;
	}
	

	public Vector<Integer> getUsedColumnNumbers()
	{
		return new Vector<Integer>(colNumToName.keySet());
	}
	

	public void setRoleForColumnNumber(int col, PredefinedSpreadsheetColumnRole role)																
	{ 
		colNumToRole.put(col, role); 
	}
	

	public void setUserDefinedRoleForColumnNumber(int col, String role)																
	{ 
		colNumToUserDefinedRole.put(col, role); 
	}
	
	
	public int getNColsForRole(PredefinedSpreadsheetColumnRole role)
	{
		int n = 0;
		for (PredefinedSpreadsheetColumnRole r: colNumToRole.values())
			if (r == role)
				n++;
		return n;
	}
	
	
	// All columns except timepoints, names, ids, and annotations.
	public Set<SpreadsheetColumnRole> collectGroupableColumnRoles()
	{
		Set<SpreadsheetColumnRole> ret = new HashSet<SpreadsheetColumnRole>();
		ret.add(SpreadsheetColumnRole.buildKEGG());
		return ret;
	}
	
	
	public Vector<PredefinedSpreadsheetColumnRole> getNonTimepointPredefinedColumnRoles()
	{
		Vector<PredefinedSpreadsheetColumnRole> ret = new Vector<PredefinedSpreadsheetColumnRole>();
		for (Integer colNum: colNumToRole.keySet())
		{
			if (colNumToRole.get(colNum) != PredefinedSpreadsheetColumnRole.TIMEPOINT)
			{
				ret.add(colNumToRole.get(colNum));
			}
		}
		return ret;
	}
	
	
	public Vector<String> getUserDefinedColumnRoles()
	{
		return new Vector<String>(colNumToUserDefinedRole.values());
	}
	
	
	public static SpreadsheetStructure buildLightweightTestInstance()
	{
		SpreadsheetStructure ret = new SpreadsheetStructure(null);		// null file
		ret.setRoleAndNameForColumnNumber(1, PredefinedSpreadsheetColumnRole.NAME, "Name");
		for (int i=1; i<=5; i++)
			ret.setRoleAndNameForColumnNumber(i, PredefinedSpreadsheetColumnRole.TIMEPOINT, "T" + i);
		return ret;
	}
	
	
	public File getFile()											{ return file; }
	public PredefinedSpreadsheetColumnRole getRoleForColumnNumber(int col)	{ return colNumToRole.get(col); }
	public void setNameForColumnNumber(int col, String name)		{ colNumToName.put(col, name); }
	public String getNameForColumnNumber(int col)					{ return colNumToName.get(col); }
	public int getNTimepoints()										{ return getColNumsOfTimepoints().size(); }	
	public boolean usesColumn(int col)								{ return colNumToRole.containsKey(col); }
	public int getDataStartRowNum() 								{ return dataStartRow; }	
	public void setDataStartRowNum(int n)							{ dataStartRow = n; }
	static void sop(Object x)										{ System.out.println(x); }
}
