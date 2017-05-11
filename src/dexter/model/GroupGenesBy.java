package dexter.model;

import java.util.*;


//
// Genes can be grouped by predefined column role (e.g. KEGG pathway), user-defined column role (e.g.
// COG group), or appearance order in their spreadsheet. Grouping by roles is captured by the
// SpreadsheetColumnRole class. This class encapsulates a SpreadsheetColumnRole or describes grouping
// by appearance order.
//


public class GroupGenesBy 
{
	private static Map<Integer, GroupGenesBy>	appearanceOrderSizeToInstance;
	private static GroupGenesBy					groupByCoregulationInstance = new GroupGenesBy(null);
	
	
	static
	{
		appearanceOrderSizeToInstance = new HashMap<Integer, GroupGenesBy>();
		int[] sizes = { 10, 20, 50 };
		for (int size: sizes)
			appearanceOrderSizeToInstance.put(size, new GroupGenesBy(size));
	}
	
	
	private SpreadsheetColumnRole				ssColRole;			// if describing a role, otherwise null
	private int									partitionSize;		// if describing appearance order, otherwise zero
	
	
	public GroupGenesBy(SpreadsheetColumnRole ssRole)
	{
		this.ssColRole = ssRole;
	}
	
	
	private GroupGenesBy(int partitionSize)
	{
		this.partitionSize = partitionSize;
	}
	
	
	public String toString()
	{
		if (isSpreadsheetColumnRole())
			return ssColRole.toString();
		else if (this == groupByCoregulationInstance)
			return "Operon prediction";
		else
			return "Spreadsheet order by " + partitionSize + "s";
	}
	
	
	public boolean isCoregulationInstance()
	{
		return this == groupByCoregulationInstance;
	}
	
	
	public static GroupGenesBy getCoregulationInstance()
	{
		return groupByCoregulationInstance;
	}
	
	
	public int getPartitionSize()
	{
		assert partitionSize > 0;
		return partitionSize;
	}
	
	
	public boolean isSpreadsheetColumnRole()
	{
		return ssColRole != null;
	}
	
	
	public SpreadsheetColumnRole getSpreadsheetColumnRole()
	{
		assert ssColRole != null  :  "null SpreadsheetColumnRole in " + this;
		return ssColRole;
	}
	
	
	public static GroupGenesBy buildKEGG()
	{
		return new GroupGenesBy(SpreadsheetColumnRole.buildKEGG());
	}
	
	
	public static GroupGenesBy getForAppearanceOrderBySize(Integer size)
	{
		if (!appearanceOrderSizeToInstance.containsKey(size))
			appearanceOrderSizeToInstance.put(size, new GroupGenesBy(size));
		return appearanceOrderSizeToInstance.get(size);
	}
}
