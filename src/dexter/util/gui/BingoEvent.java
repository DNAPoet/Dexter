package dexter.util.gui;

public class BingoEvent 
{
	public enum Change
	{
		COLUMN_ADDED, COLUMN_REMOVED, COLUMN_CHANGED
	}
	
	
	private BingoPanel			source;
	private Change				change;
	private int					oldRow;
	private int					newRow;
	private int					column;
	
	
	BingoEvent(BingoPanel source, Change change, int oldRow, int newRow, int column/*, int chunk*/)
	{
		this.source = source;
		this.change = change;
		this.oldRow = oldRow;
		this.newRow = newRow;
		this.column = column;
	}
	
	
	public String toString()
	{
		return "BingoEvent " + change + " old/new rows = " + oldRow + "," + newRow + ", column=" + column;
	}
	

	public BingoPanel getSource()		{ return source; }
	public Change getChange()			{ return change; }
	public int getOldRow()				{ return oldRow; }
	public int getNewRow()				{ return newRow; }
	public int getColumn()				{ return column; }
}
