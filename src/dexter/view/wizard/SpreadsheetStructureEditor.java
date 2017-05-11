package dexter.view.wizard;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.*;
import java.io.*;
import dexter.model.*;
import dexter.util.*;
import dexter.util.gui.*;


class SpreadsheetStructureEditor extends JPanel implements BingoListener
{
	private final static Font				HEADER_FONT;
	private final static int				N_SS_ROWS_TO_DISPLAY	=   7;
	private final static int				SPANE_PREF_W			= 600;
	
	
	static
	{
		String fam = new JLabel("?").getFont().getFamily();
		HEADER_FONT = new Font(fam, Font.PLAIN, 16);
	}
	
	
	private File							spreadsheetFile;
	private JFileChooser					fileChooser;
	private boolean							primary;
	private BingoPanelWithRowSepRadios		bingoPan;
	private JScrollPane						bingoSpane;
	private ColumnAssigner[]				columnAssignersBySSCol;		// nulls are ok
	private JPanel							colAssignerPan;
	private JScrollPane						colAssignerSpane;
	private Map<File, SupplementalSpreadsheetDialog>
											fileToSuppDia;				// if non-null, this editor is primary
	
	
	// Initializes from structure if not null. File must be csv or tsv.
	SpreadsheetStructureEditor(File spreadsheetFile, 
							   SpreadsheetStructure structure, 
							   JFileChooser fileChooser,
							   boolean primary) 
	    throws IOException
	{	
		String fileName = spreadsheetFile.getName();
		assert fileName.endsWith(".csv")  ||  fileName.endsWith(".tsv");
		
		this.spreadsheetFile = spreadsheetFile;
		this.fileChooser = fileChooser;
		this.primary = primary;
		
		if (primary)			
			fileToSuppDia = new LinkedHashMap<File, SupplementalSpreadsheetDialog>();
		
		setLayout(new BorderLayout());
		JPanel workPan = new JPanel(new BorderLayout());
		workPan.setOpaque(true);
		workPan.setBackground(Color.BLACK);
		
		// Bingo panel depicting top few rows of spreadsheet.
		java.util.List<String[]> rawRows = fileName.endsWith(".csv")  ?
			StringUtils.readCsvRows(spreadsheetFile)  :
			StringUtils.readTsvRows(spreadsheetFile);
		Vector<Vector<String>> vec2 = convertToLimitedVec2(rawRows);
		rawRows = null;			// done with rawRows[], force null pointer if used by accident
		int dataStartRow = (structure == null)  ?  1  : structure.getDataStartRowNum(); 
		bingoPan = new BingoPanelWithRowSepRadios(vec2, dataStartRow);
		bingoPan.addBingoListener(this);
		bingoSpane = new JScrollPane(bingoPan, 
									 JScrollPane.VERTICAL_SCROLLBAR_NEVER,
									 JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
		int bingoPrefH = bingoPan.getPreferredSize().height;	
		Dimension spanePref = new Dimension(SPANE_PREF_W, bingoPrefH + 22);
		bingoSpane.setPreferredSize(spanePref);
		workPan.add(bingoSpane, BorderLayout.NORTH);

		// Select cells in bingo panel. If building from scratch, initially select topmost non-empty cell
		// in each column. If all cells are empty, select topmost.
		int nCols = vec2.get(0).size();
		{
			for (int col=0; col<nCols; col++)
			{
				int selRow = -12345;
				if (structure == null)
				{
					// From scratch.
					selRow = 0;
					for (int row=0; row<vec2.size(); row++)
					{
						if (!vec2.get(row).get(col).isEmpty())
						{
							selRow = row;
							break;
						}
					}
				}
				else
				{
					// From a model.
					String text = structure.getNameForColumnNumber(col);
					selRow = getRowOfTextInColumn(text, col, vec2);
				}
				bingoPan.setSelectedRowForColumn(selRow, col);
			}
		}
		
		// Column assigners.
		columnAssignersBySSCol = new ColumnAssigner[nCols];
		int[] selectedRowByCol = bingoPan.getSelectedRowForAllColumns();		
		colAssignerPan = new SolidBGPanel(getBackground());
		for (int col=0; col<nCols; col++)
		{ 
			if (structure != null  &&  !structure.usesColumn(col))
				continue;
			int selRow = selectedRowByCol[col];
			if (selRow < 0)
				continue;
			String text = vec2.get(selRow).get(col);
			ColumnAssigner cola = new ColumnAssigner(col, text/*, 0*/);
			if (structure != null)
			{
				PredefinedSpreadsheetColumnRole role = structure.getRoleForColumnNumber(col);
				cola.selectRole(role);
			}
			else if (col == 0)
				cola.selectRole(PredefinedSpreadsheetColumnRole.ID);		// 1st column is almost always ID
			colAssignerPan.add(cola);
			columnAssignersBySSCol[col] = cola;
		}
		colAssignerSpane = new JScrollPane(colAssignerPan, 
				JScrollPane.VERTICAL_SCROLLBAR_NEVER,
				JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
		Dimension supersize = colAssignerSpane.getPreferredSize();
		colAssignerSpane.setPreferredSize(new Dimension(SPANE_PREF_W, supersize.height+24));
		workPan.add(colAssignerSpane, BorderLayout.SOUTH);
		add(workPan, BorderLayout.CENTER);

		setOpaque(true);
		
		//if (spreadsheetFile.getName().contains("Zinser")) configureBingoPanelForZinserPro();
	}  // End of ctor
	
	
	// The Zinser/Prochlorococcus spreadsheet has lots of timepoint columns, alternating between
	// needed and not needed. Call this to avoid lots of clicks to deselect columns. Some manual
	// finishing is required, but this method eliminates most of the mouse work.
	private void configureBingoPanelForZinserPro()
	{
		Set<Integer> unwantedCols = new HashSet<Integer>();
		unwantedCols.add(1);
		for (int i=3; i<=10; i++)
			unwantedCols.add(i);
		for (int i=12; i<bingoPan.getNColumns(); i+=2)
			unwantedCols.add(i);
		for (int i: unwantedCols)
		{
			bingoPan.setSelectedRowForColumn(-1, i);
			removeColumn(i);
		}
		
		for (int i=11; i<bingoPan.getNColumns(); i+=2)
			bingoPan.setSelectedRowForColumn(2, i);
	}
	
	
	private Vector<Vector<String>> convertToLimitedVec2(java.util.List<String[]> raw)
	{
		Vector<Vector<String>> vec2 = new Vector<Vector<String>>();
		for (String[] sarr: raw)
		{
			Vector<String> vec = new Vector<String>();
			for (String s: sarr)
				vec.add(s);
			vec2.add(vec);
			if (vec2.size() == N_SS_ROWS_TO_DISPLAY)
				break;
		}
		return vec2;
	}
	
	
	// If text is whitespace, returns row of 1st whitespace cell. Returns -1 if not found.
	private int getRowOfTextInColumn(String text, int col, Vector<Vector<String>> vec2)
	{
		for (int row=0; row<vec2.size(); row++)
		{
			String cell = vec2.get(row).get(col).trim();
			if (cell.equals(text))
				return row;
		}
		return -1;
	}

	
	// Make 3 pixels higher than default preferred height. Black background will show through as if
	// part of a multiple border.
	public Dimension getPreferredSize()
	{
		Dimension superPref = super.getPreferredSize();
		return new Dimension(superPref.width, superPref.height+3);
	}
	
	
	public void paintComponent(Graphics g)
	{
		g.setColor(Color.BLACK);
		g.fillRect(0, 0, 2222, 1111);
	}
	
	
	// Label plus combo for assigning a role to a spreadsheet column.
	private class ColumnAssigner extends JPanel implements ItemListener
	{
		private	String			cellText;
		private JComboBox 		combo;
		
		// If role is null, tries to infer the role; if that fails, uses a default.
		ColumnAssigner(int colNum, String cellText)
		{
			if (cellText.isEmpty())
				cellText = "Untitled col" + colNum;
			this.cellText = cellText;
			setOpaque(false);
			setLayout(new VerticalFlowLayout());
			JLabel label = new JLabel(cellText, SwingConstants.CENTER);
			label.setFont(HEADER_FONT);
			add(label);
			combo = new JComboBox(PredefinedSpreadsheetColumnRole.values());	// TODO: user-defined roles
			PredefinedSpreadsheetColumnRole role = PredefinedSpreadsheetColumnRole.fromString(cellText);
			if (role == null)
				role = PredefinedSpreadsheetColumnRole.ANNOTATION;		// have to select something
			combo.setSelectedItem(role);
			//combo.addItemListener(this);								// uncomment for debugging
			add(combo);
			setBorder(BorderFactory.createLineBorder(Color.BLACK));
		}
		
		PredefinedSpreadsheetColumnRole getSelectedRole()		
		{ 
			return (PredefinedSpreadsheetColumnRole)combo.getSelectedItem(); 
		}
		
		void selectRole(PredefinedSpreadsheetColumnRole role)	
		{ 
			combo.setSelectedItem(role); 
		}
		
		// Just for debugging.
		public void itemStateChanged(ItemEvent e)
		{
			if (e.getStateChange() != ItemEvent.SELECTED)
				return;
			sop("===========" + combo.getSelectedItem());
			for (SpreadsheetStructure ss: getAllSpreadsheetStructures())
				sop("  -----\n" + ss);
		}
		
		String getText()							{ return cellText; }
	}  // End of inner class ColumnAssigner
	
	
	public void bingoChanged(BingoEvent e)
	{
		switch (e.getChange())
		{
			case COLUMN_ADDED:
				addColumn(e.getColumn(), e.getNewRow());
				break;
				
			case COLUMN_REMOVED:
				removeColumn(e.getColumn());
				break;
				
			case COLUMN_CHANGED:
				PredefinedSpreadsheetColumnRole role = removeColumn(e.getColumn());
				ColumnAssigner cola = addColumn(e.getColumn(), e.getNewRow());
				cola.selectRole(role);
				break;			
		}
	}
	
	
	// Returns the new assigner.
	private ColumnAssigner addColumn(int ssCol, int row)
	{
		assert columnAssignersBySSCol[ssCol] == null;
		String text = bingoPan.getTextAt(row, ssCol).trim();
		ColumnAssigner cola = new ColumnAssigner(ssCol, text);
		columnAssignersBySSCol[ssCol] = cola;
		colAssignerPan.removeAll();				// to preserve order or appearance
		for (ColumnAssigner ca: columnAssignersBySSCol)
			if (ca != null)
				colAssignerPan.add(ca);
		colAssignerSpane.validate();
		colAssignerPan.repaint();
		return cola;
	}
	
	
	// Returns the selected row of the removed column.
	private PredefinedSpreadsheetColumnRole removeColumn(int ssCol)
	{
		ColumnAssigner assigner = columnAssignersBySSCol[ssCol];
		assert assigner != null;
		PredefinedSpreadsheetColumnRole role = assigner.getSelectedRole();
		columnAssignersBySSCol[ssCol] = null;
		colAssignerPan.remove(assigner);
		colAssignerSpane.validate();
		colAssignerPan.repaint();
		return role;
 	}
	
	
	// Returns null if valid, otherwise an error message.
	String assignmentsAreValid()
	{
		return null;
	}
	
	
	// Sent by the "BROWSE FOR SUPPLEMENTAL" button.
	void browseForSupplementalSpreadsheetModal()
	{		
		if (fileChooser == null)
		{
			// No file chooser was passed to the ctor. Maybe this class is being unit tested.
			// Build a file chooser showing the dir containing the spreadsheet.
			fileChooser = DexterWizardPanel.buildFileChooser();
		}
		
		// Browse for supplemental spreadsheet file.
		if (fileChooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION)
			return;
		File spreadsheetFile = fileChooser.getSelectedFile();
		assert spreadsheetFile != null;
		
		// Present a dialog containing another instance of this class, for designating functions
		// of columns in the supplemental spreadsheet.
		SupplementalSpreadsheetDialog suppDia = null;
		try
		{
			suppDia = new SupplementalSpreadsheetDialog(spreadsheetFile, null);
		}
		catch (IOException x)
		{
			String err = "Couldn't open supplemental spreadsheet file " + spreadsheetFile.getAbsolutePath() +
				": " + x.getMessage();
			JOptionPane.showMessageDialog(this, err);
			return;
		}
		suppDia.setVisible(true);			// modal
		if (!suppDia.getWasCancelled())
			fileToSuppDia.put(spreadsheetFile, suppDia);
	}
	
	
	// Encapsulates the spreadsheet file (easy) and its structure (hard, the whole point of this class).
	SpreadsheetStructure getStructure()
	{
		SpreadsheetStructure ret = new SpreadsheetStructure(spreadsheetFile);
		
		// Add # of header rows.
		int nHeaderRows = bingoPan.getNHeaderRows();
		ret.setDataStartRowNum(nHeaderRows);
		
		// Add column information for standard and (eventually) user-defined roles.
		for (int i=0; i<columnAssignersBySSCol.length; i++)
		{
			ColumnAssigner cola = columnAssignersBySSCol[i];
			if (cola == null)
				continue;
			String name = cola.getText();
			PredefinedSpreadsheetColumnRole role = cola.getSelectedRole();
			ret.setRoleAndNameForColumnNumber(i, role, name);
		}
		
		return ret;
	}
		
	
	// Primary comes first, order of others is not guaranteed.
	Vector<SpreadsheetStructure> getAllSpreadsheetStructures()
	{
		assert primary;
		
		Vector<SpreadsheetStructure> ret = new Vector<SpreadsheetStructure>();
		ret.add(getStructure());
		for (SupplementalSpreadsheetDialog dia: fileToSuppDia.values())
		{
			assert !dia.getWasCancelled();
			SpreadsheetStructure struc = dia.getStructure();
			if (struc != null)
				ret.add(struc);
		}
		return ret;
	}
	
	
	File getSpreadsheetFile()							{ return spreadsheetFile; }	
	static void sop(Object x)							{ System.out.println(x);  }
	

	public static void main(String[] args)
	{
		try
		{
			JFrame frame = new JFrame();			
			File studyFile = new File("data/Studies/TeryIMS101Pathways.tsv");
			assert studyFile.exists();
			frame.setTitle(studyFile.getName());
			SpreadsheetStructureEditor that = new SpreadsheetStructureEditor(studyFile, null, null, true);
			//that.configureBingoPanelForZinserPro();
			frame.add(that, BorderLayout.CENTER);
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
