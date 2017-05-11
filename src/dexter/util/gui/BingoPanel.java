//
// At most 1 cell can be selected per column.
//


package dexter.util.gui;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;


public class BingoPanel extends JPanel implements ItemListener
{
	private final static int			CELL_HORIZ_GAP		=  4;
	private final static int			CELL_VERT_GAP		=  4;

	private int							maxCellWidth;
	private Vector<Vector<Cell>>		cells;				// cell's tag is { row, col }. Row major.
	private SelectionMap 				selectionMap;		// same structure as cells, always up to date
	private int							nRows;
	private int 						nCols;
	private Set<BingoListener>			listeners;			// notification is not threadsafe
	private MarginModel					marginModel;		// in addition to cell gaps
	
	
	// Just makes life a little easier
	private class Cell extends TaggedToggle<int[]>
	{		
		Cell(String text, int[] tag)
		{
			super(text, tag); 
		}
	}
	
	
	// Rebuild whenever a selection changes.
	private class SelectionMap extends Vector<Vector<Boolean>>
	{
		SelectionMap(Vector<Vector<Cell>> cells)
		{
			for (Vector<Cell> cellRow: cells)
			{
				Vector<Boolean> selectionRow = new Vector<Boolean>();
				for (Cell cell: cellRow)
					selectionRow.add(cell.isSelected());
				add(selectionRow);
			}
		}
		
		int getSelectedRowForColumn(int col)
		{
			for (int row=0; row<nRows; row++)
				if (get(row).get(col))
					return row;
			return -1;
		}
		
		int[] getSelectedRowForAllColumns()
		{
			int[] ret = new int[nCols];
			for (int col=0; col<nCols; col++)
				ret[col] = selectionMap.getSelectedRowForColumn(col);
			return ret;
		}
		
		Vector<Integer> getColumnNumbersWithSelectedCell()
		{
			Vector<Integer> ret = new Vector<Integer>();
			int[] selRowNums = getSelectedRowForAllColumns();
			for (int col=0; col<selRowNums.length; col++)
				if (selRowNums[col] >= 0)
					ret.add(col);		
			return ret;
		}
	}  // End of inner class SelectionMap
	
	
	public BingoPanel(Vector<Vector<String>> rowses)
	{
		this(rowses, Integer.MAX_VALUE);
	}
	
	
	public BingoPanel(Vector<Vector<String>> rowses, int maxCellWidth)
	{
		this.maxCellWidth = maxCellWidth;
		
		// All rows must have same # of columns.
		nRows = rowses.size();
		nCols = rowses.firstElement().size();
		for (int i=1; i<rowses.size(); i++)
		{
			assert rowses.get(i).size() == nCols  :  
				"Row " + i + " has wrong # of columns. Saw " + rowses.get(i).size() + ", expected " + nCols;
		}
	
		// Build 2d array of components. Coords are [row#][col#].
		setLayout(new Lom());
		cells = new Vector<Vector<Cell>>();
		for (int row=0; row<nRows; row++)
		{
			Vector<Cell> rowVec = new Vector<Cell>();
			for (int col=0; col<nCols; col++)
			{
				String text = rowses.get(row).get(col); 
				int[] tag = { row, col };
				Cell cell = new Cell(text, tag);
				add(cell);
				rowVec.add(cell);
				cell.addItemListener(this);
			}
			cells.add(rowVec);
		}
		selectionMap = new SelectionMap(cells);
		
		// Layout manager might truncate text. Set tooltip to entire text. Caller can override.
		setToolTips(rowses);
		
		// WARNING: Listener notification is supposed to be performed by traversing a clone of the
		// listeners collection, in a separate thread from the GUI thread.
		listeners = new HashSet<BingoListener>();
		
		// Has to be non-opaque in order to work in a scrollpane and have its own paintComponent() method.
		setOpaque(false);
		
		// In case subclasses need extra room.
		marginModel = new MarginModel(0, 0, 0, 0);
	}
	
	
	// In case subclasses want to do additional layout.
	protected void doSubclassLayout()	{ }
	
	
	private class Lom extends LayoutAdapter
	{
	    public void layoutContainer(Container parent)                   
	    {
	    	int[] prefWidthsByCol = getMaxPrefWidthsByColumn();
	    	int prefH = cells.get(0).get(0).getPreferredSize().height;
    		int x = CELL_HORIZ_GAP + marginModel.getLeft();
	    	for (int col=0; col<nCols; col++)
	    	{ 
	    		Dimension sizeForCol = new Dimension(prefWidthsByCol[col], prefH);
	    		int y = CELL_VERT_GAP + marginModel.getTop();
	    		for (int row=0; row<nRows; row++)
	    		{
	    			Component compo = cells.get(row).get(col);   
	    			compo.setSize(sizeForCol);
	    			compo.setLocation(x, y);
		    		y += prefH + CELL_VERT_GAP;
	    		}
    			x += prefWidthsByCol[col] + CELL_HORIZ_GAP;
	    	} 
	    	
	    	doSubclassLayout();
	    }
	    
	    public Dimension preferredLayoutSize(Container parent)
	    {
	    	int[] prefWidthsByCol = getMaxPrefWidthsByColumn();
	    	int w = CELL_HORIZ_GAP;
	    	for (int pref: prefWidthsByCol)
	    		w += pref + CELL_HORIZ_GAP;
	    	w += marginModel.getLeft() + marginModel.getRight();
	    	int prefH = cells.get(0).get(0).getPreferredSize().height;
	    	int h = nRows * prefH  +  (nRows+1) * CELL_VERT_GAP;
	    	h += marginModel.getTop() + marginModel.getBottom();
	    	return new Dimension(w, h);
	    }
	    
	    private int[] getMaxPrefWidthsByColumn()
	    {
	    	int[] ret = new int[nCols];
	    	for (int col=0; col<nCols; col++)
	    	{
	    		for (int row=0; row<nRows; row++)
	    		{
	    			Component compo = cells.get(row).get(col);
	    			ret[col] = Math.max(ret[col], compo.getPreferredSize().width);
	    		}
	    		ret[col] = Math.min(ret[col], maxCellWidth);
	    	}
	    	return ret;
	    }
	}  // End of inner class Lom

	
	// A toggle button was selected or deselected. If selected, deselect all other toggles in column.
	// Notify this object's item listener.
	public void itemStateChanged(ItemEvent e)
	{
		SelectionMap prevSelMap = selectionMap;
		Vector<Integer> prevColsWithSelectedCells = prevSelMap.getColumnNumbersWithSelectedCell();
		
		// Get selected column.
		Cell src = (Cell)e.getSource();
		int[] rowCol = src.getTag();
		int selectionRow = rowCol[0];
		int selectionCol = rowCol[1];
		
		// Internal response: at most 1 cell can be selected in any column.
		if (e.getStateChange() == ItemEvent.SELECTED)
		{
			for (int row=0; row<nRows; row++)
			{
				if (row == rowCol[0])
					continue;
				TaggedToggle<int[]> other = cells.get(row).get(selectionCol);
				other.removeItemListener(this);
				other.setSelected(false);
				other.addItemListener(this);
			}
		}
		selectionMap = new SelectionMap(cells);
		
		// Was column added, deleted, or changed?
		Vector<Integer> currentColsWithSelectedCells = selectionMap.getColumnNumbersWithSelectedCell();
		int delta = currentColsWithSelectedCells.size() - prevColsWithSelectedCells.size();
		assert Math.abs(delta) <= 1;
		BingoEvent bev = null;
		switch (delta)
		{
			case -1:
				// Column was deleted.
				bev = new BingoEvent(this, BingoEvent.Change.COLUMN_REMOVED, -1, -1, selectionCol);
				break;
			case 0:
				// Selection changed. 
				int oldRowForCol = prevSelMap.getSelectedRowForColumn(selectionCol);
				assert oldRowForCol >= 0;
				bev = new BingoEvent(this, BingoEvent.Change.COLUMN_CHANGED, oldRowForCol, 
									 selectionRow, selectionCol);
				break;
			case 1:
				// Column was inserted.
				bev = new BingoEvent(this, BingoEvent.Change.COLUMN_ADDED, selectionRow, 
									 selectionRow, selectionCol);
				break;
		}
					
		// Notify item listeners. Not threadsafe.
		assert bev != null;
		for (BingoListener bl: listeners)
			bl.bingoChanged(bev);
	}
	
	
	public void addBingoListener(BingoListener b)
	{
		listeners.add(b);
	}
	
	
	public void removeBingoListener(BingoListener b)
	{
		listeners.remove(b);
	}
	
	
	public String getTextAt(int row, int col)
	{
		return cells.get(row).get(col).getText();
	}
	
	
	public void setToolTips(Vector<Vector<String>> tips)
	{
		assert tips.size() == nRows;
		
		for (int row=0; row<nRows; row++)
		{
			Vector<String> rowVec = tips.get(row);
			assert rowVec.size() == nCols;
			for (int col=0; col<nCols; col++)
			{
				String tipText = tips.get(row).get(col);
				if (tipText != null  &&  !tipText.trim().isEmpty())
					cells.get(row).get(col).setToolTipText(tips.get(row).get(col));
			}
		}
	}
	
	
	// At most 1 toggle per column may be selected. Call with row < 0 to deselect all toggles in column.
	public void setSelectedRowForColumn(int row, int col)
	{
		for (Vector<Cell> vec: cells)
			vec.get(col).removeItemListener(this);
		for (int i=0; i<cells.size(); i++)
			cells.get(i).get(col).setSelected(i==row);
		for (Vector<Cell> vec: cells)
			vec.get(col).addItemListener(this);
		selectionMap = new SelectionMap(cells);
	}
	
	
	public static int getPreferredHeightForNRows(int nRows)
	{
		Vector<Vector<String>> rowses = new Vector<Vector<String>>();
		for (int i=0; i<nRows; i++)
		{
			Vector<String> vec = new Vector<String>();
			vec.add("xxx " + i);	// label doesn't matter
			rowses.add(vec);
		}
		BingoPanel pan = new BingoPanel(rowses);
		return pan.getPreferredSize().height;
	}
	
	
	public int[] getSelectedRowForAllColumns()
	{
		return selectionMap.getSelectedRowForAllColumns();
	}
	
	
	public Vector<Integer> getColumnNumbersWithSelectedCell()
	{
		return selectionMap.getColumnNumbersWithSelectedCell();
	}
	
	
	// Order is row-major.
	public Collection<TaggedToggle<int[]>> collectCells()
	{
		Vector<TaggedToggle<int[]>> cellVec = new Vector<TaggedToggle<int[]>>();
		for (Vector<Cell> row: cells)
			for (Cell cell: row)
				cellVec.add(cell);
		return cellVec;
	}
	
	
	public void setMarginModel(MarginModel marginModel)
	{
		this.marginModel = marginModel;
		invalidate();
		validate();
	}
	
	
	public int getNColumns()
	{
		return cells.firstElement().size();
	}
	
	
	protected Vector<Rectangle> getCellBoundsForColumn(int col)
	{
		Vector<Rectangle> ret = new Vector<Rectangle>();
		for (Vector<Cell> row: cells)
			ret.add(row.get(col).getBounds());
		return ret;
	}
	
	
	public static void sop(Object x)						{ System.out.println(x); }
	

	public static void main(String[] args)
	{
		try
		{
			JFrame frame = new JFrame();
			Vector<Vector<String>> rowses = new Vector<Vector<String>>();
			for (int row=0; row<4; row++)
			{
				Vector<String> r = new Vector<String>();
				for (int col=0; col<6; col++)
				{
					String s = "RC " + row + ":" + col;
					if (row==0 && col==0)
						s = "xxxxxxxxxxxxxxxxx";
					r.add(s);
				}
				rowses.add(r);
			}

			BingoPanel pan = new BingoPanel(rowses);
			pan.setMarginModel(new MarginModel(30, 30, 30, 30));
			frame.add(pan, BorderLayout.CENTER);
			frame.pack();
			frame.setVisible(true);
		}
		catch (Exception x)
		{
			sop("Stress: " + x.getMessage());
			x.printStackTrace();
		}
	}
}
