package dexter.view.wizard;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.*;
import java.io.*;

import dexter.model.*;
import dexter.util.*;


class SupplementalSpreadsheetDialog extends JDialog implements ActionListener
{
	private SpreadsheetStructureEditor				strucEd;
	private boolean									wasCancelled;
	private JButton									applyBtn;
	private JButton									cancelBtn;
	
	
	SupplementalSpreadsheetDialog(File file, SpreadsheetStructure structure) throws IOException
	{	
		JPanel pan = new JPanel();
		applyBtn = new JButton("Apply");
		applyBtn.addActionListener(this);
		pan.add(applyBtn);
		cancelBtn = new JButton("Cancel");
		cancelBtn.addActionListener(this);
		pan.add(cancelBtn);
		add(pan, BorderLayout.SOUTH);
		
		strucEd = new SpreadsheetStructureEditor(file, structure, null, false);		// null structure is ok
		add(strucEd, BorderLayout.CENTER);
		
		setTitle("Import a supplemental spreadsheet");
		setModal(true);
		pack();
	}
	
	
	public void actionPerformed(ActionEvent e)
	{
		wasCancelled = e.getSource() == cancelBtn;
		setVisible(false);
	}	
	
	
	boolean getWasCancelled()
	{
		return wasCancelled;
	}
	
	
	SpreadsheetStructure getStructure()
	{
		return wasCancelled  ?  null  :  strucEd.getStructure();
	}
	
	
	static void sop(Object x)			{ System.out.println(x); }
	
	
	public static void main(String[] args)
	{
		try
		{
			//File f = new File("data/Studies/pmm_KEGG_module_pathway_and_genename.tsv");
			File f = new File("data/Studies/MED4_Pathways.tsv");
			SupplementalSpreadsheetDialog dia = new SupplementalSpreadsheetDialog(f, null);
			dia.setVisible(true);
		}
		catch (Exception x)
		{
			sop("STRESS: " + x.getMessage());
			x.printStackTrace(System.out);
		}
		sop("DONE");
	}
}
