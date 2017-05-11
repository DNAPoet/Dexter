package dexter.view.cluster;

import java.awt.*;
import java.awt.event.*;
import java.io.*;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import javax.swing.filechooser.FileNameExtensionFilter;

import java.util.*;

import dexter.MainDexterFrame;
import dexter.cluster.*;
import dexter.model.*;
import dexter.util.*;
import dexter.util.gui.*;


public class ClusterDialog extends JDialog implements ActionListener
{
	private final static File				DIRF 			= new File("data/Clusters");
	private final static JFileChooser		fileChooser;
	
	
	static
	{
		fileChooser = new JFileChooser(DIRF);
	    FileNameExtensionFilter filter = new FileNameExtensionFilter("Gene clusters", "tre");
	    fileChooser.setFileFilter(filter);
	}
	

	private JButton									openBtn;
	private JComboBox								algorithmCombo;
	private TaggedButtonGroup<GeneSelectionLevel>	selBgrp;
	private JButton									computeBtn;
	private JButton									cancelBtn;
	private boolean									cancelled;
	private File									newickFile;
	private boolean									exitOnClose;			// for debugging
	
	
	// Null session model is ok if debugging.
	public ClusterDialog()
	{
		setModal(true);
		
		JPanel north = new JPanel(new VerticalFlowLayout());
		
		JPanel strip = new JPanel();
		openBtn = new JButton("Open saved tree...");
		openBtn.addActionListener(this);
		strip.add(openBtn);
		north.add(strip);
		
		strip = new JPanel();
		strip.add(new JLabel("Algorithm:"));
		algorithmCombo = new JComboBox(ClusterAlgorithm.deployableAlgorithms());
		strip.add(algorithmCombo);
		
		strip = new JPanel();
		selBgrp = new TaggedButtonGroup<GeneSelectionLevel>(GeneSelectionLevel.values());
		strip.add(selBgrp.buildPanel());
		north.add(strip);
		add(north, BorderLayout.NORTH);
		
		JPanel controls = new JPanel();
		computeBtn = new RedClockButton("Compute");
		computeBtn.addActionListener(this);
		controls.add(computeBtn);
		cancelBtn = new JButton("Cancel");
		cancelBtn.addActionListener(this);
		Dimension pref = new Dimension(cancelBtn.getPreferredSize().width, computeBtn.getPreferredSize().height);
		cancelBtn.setPreferredSize(pref);
		controls.add(cancelBtn);
		add(controls, BorderLayout.SOUTH);
		pack();
	}
	
	
	public void actionPerformed(ActionEvent e)
	{
		if (e.getSource() == cancelBtn)
		{
			cancelled = true;
			setVisible(false);
			if (exitOnClose)
				System.exit(0);
		}
		
		else if (e.getSource() == openBtn)
		{
			if (fileChooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION)
				return;		
			newickFile = fileChooser.getSelectedFile();
			setVisible(false);
		}
		
		else if (e.getSource() == computeBtn)
		{
			setVisible(false);
			if (exitOnClose)
				System.exit(0);
		}
	}
	
	
	public ClusterAlgorithm getAlgorithm()				{ return (ClusterAlgorithm)algorithmCombo.getSelectedItem(); }
	public GeneSelectionLevel getGeneSelectionLevel()	{ return selBgrp.getSelectedTag(); }
	public File getNewickFile()							{ return newickFile; }
	public void setExitOnClose()						{ exitOnClose = true; }
	public boolean cancelled()							{ return cancelled; }
	public static JFileChooser getFileChooser()			{ return fileChooser; }
	static void sop(Object x)							{ System.out.println(x); }
	
	
	public static void main(String[] args)
	{
		try
		{
			ClusterDialog dia = new ClusterDialog();
			dia.setLocation(500,  450);
			dia.setVisible(true);
			
			File newickFile = dia.getNewickFile();
			if (newickFile == null)
				System.exit(0);
			else
			{
				try
				{
					NewickParser<String> parser = new NewickParser<String>(newickFile, new NewickPayloadBuilderStringIdentity());
					Node<String> root = parser.parse();
					TreeDialog<String> hairlineDia = new TreeDialog<String>(root);
					hairlineDia.setTitle(root.collectLeafNodes().size() + " genes");
					hairlineDia.setModal(true);
					hairlineDia.setVisible(true);
				}
				catch (IOException x)
				{
					String err = "Couldn't load newick file " + newickFile.getName() + ": " + x.getMessage();
					JOptionPane.showMessageDialog(dia, err);
					return;
				}
			}
		}
		catch (Exception x)
		{
			sop("STRESS: " + x.getMessage());
			x.printStackTrace(System.out);
			System.exit(1);
		}
	}
}
