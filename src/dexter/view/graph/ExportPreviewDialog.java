package dexter.view.graph;

import java.awt.*;
import java.awt.event.*;
import java.io.*;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;

import java.util.*;

import dexter.util.StringUtils;
import dexter.util.gui.OkWithContentDialog;


public class ExportPreviewDialog extends OkWithContentDialog
{
	private JButton			saveBtn;
	private JFileChooser	fileChooser;
	private String[] 		headerSarr;
	private String[][] 		cells;
	private JTable			table;
	
	public ExportPreviewDialog(Vector<String> headers, Vector<Vector<String>> cellRows)
	{
		// Convert vectors to arrays for table ctor.
		headerSarr = (String[])headers.toArray(new String[0]);		// ugh
		cells = new String[cellRows.size()][headerSarr.length];		// row major
		int rowNum = 0;
		for (Vector<String> row: cellRows)
		{
			for (int colNum=0; colNum<row.size(); colNum++)
				cells[rowNum][colNum] = row.get(colNum);
			rowNum++;
		}
		
		// Build table in a scrollpane.
		table = new JTable(cells, headerSarr);
		JScrollPane spane = new JScrollPane(table, 
											JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, 
											JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

		spane.setPreferredSize(new Dimension(1080, 550));
		setContent(spane);
		
		// Install "Save" button.
		saveBtn = new JButton("Save...");
		saveBtn.addActionListener(this);
		addToBottomFlowPanel(saveBtn, 0);
	}
	
	
	public void setColumnIsWide(int colNum)
	{
		table.getColumnModel().getColumn(colNum).setPreferredWidth(160);
	}
	
	
	private class Filter extends FileFilter
	{
		public boolean accept(File f) 
		{
			return filenameIsOk(f.getName());
		}

		public String getDescription()
		{
			return "TSV";
		}
		
	}
	
	
	private static boolean filenameIsOk(String fname)
	{
		return !(fname.equals("display")  ||  fname.equals("stdout"));
	}
	
	
	public void actionPerformed(ActionEvent e)
	{
		if (e.getSource() != saveBtn)
		{
			super.actionPerformed(e);
			return;
		}
		
		if (fileChooser == null)
		{
			fileChooser = new JFileChooser(new File("data/Exports"));
			fileChooser.setFileFilter(new Filter());
		}
		
		if (fileChooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION)
			return;	
		
		File ofile = fileChooser.getSelectedFile();
		if (!filenameIsOk(ofile.getName()))
		{
			String err = "Reserved name, please choose another.";
			JOptionPane.showMessageDialog(this, err);
			return;
		}
		try
		{
			FileWriter fw = new FileWriter(ofile);
			fw.write(StringUtils.unsplit(headerSarr, '\t') + "\n");
			for (String[] row: cells)
				fw.write(StringUtils.unsplit(row, '\t') + "\n");
			fw.flush();
			fw.close();
		}
		catch (Exception x)
		{
			String err = "Couldn't export to file " + ofile.getName() + ": " + x.getMessage();
			JOptionPane.showMessageDialog(this, err);
		}
	}
}
