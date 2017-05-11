package dexter.view.graph.experiment;

import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.*;
import javax.swing.*;
import dexter.model.*;


public class ExperimentDestinationDialog extends JDialog implements ActionListener, ItemListener
{
	private ExperimentsStudy 	experStudy;
	private int 				nExistingExperiments;
	private JComboBox			combo;					// if >= 2 studies
	private JRadioButton		existingExperRadio;		// if 1 study
	private JRadioButton		newExperRadio;			// if 1 study
	private JButton				okBtn;
	private JButton				cancelBtn;
	private boolean				cancelled;
	
	
	//
	// If the experiments study contains 1 experiment, use radios. If >= 2 studies, radios offer choice of a 
	// combo for the existing studies, or a new study.
	//
	public ExperimentDestinationDialog(ExperimentsStudy experStudy)
	{
		assert !experStudy.isEmpty();
		
		setModal(true);
		this.experStudy = experStudy;
		
		// Main controls at center.
		JPanel mainPan = new JPanel();
		Collection<Experiment> existingExperiments = experStudy.getExperiments();
		
		nExistingExperiments = existingExperiments.size();
		String sNew = "Create new experiment (\"" + Experiment.generateDefaultName() + "\")";
		newExperRadio = new JRadioButton(sNew, false);			
		ButtonGroup bgrp = new ButtonGroup();
		bgrp.add(newExperRadio);
		newExperRadio.addItemListener(this);
		if (nExistingExperiments == 1)
		{
			// 1 experiment exists. Radios offer choice between that experiment and a new one.
			String sExisting = "Copy into " + existingExperiments.iterator().next().getName();
			existingExperRadio = new JRadioButton(sExisting, true);
			bgrp.add(existingExperRadio);
			existingExperRadio.addItemListener(this);
			mainPan.add(existingExperRadio);
		}
		
		else
		{
			// Multiple experiments exist. Radios and a combo.
			existingExperRadio = new JRadioButton("Existing experiment", true);
			bgrp.add(existingExperRadio);
			existingExperRadio.addItemListener(this);
			mainPan.add(existingExperRadio);
			Vector<String> experNames = new Vector<String>();
			for (Experiment x: existingExperiments)
				experNames.add(x.getName());
			combo = new JComboBox(experNames);
			mainPan.add(combo);
		}
		mainPan.add(newExperRadio);
		
		add(mainPan, BorderLayout.CENTER);
		
		// Dialog controls at south.
		JPanel south = new JPanel();
		cancelBtn = new JButton("Cancel");
		cancelBtn.addActionListener(this);
		south.add(cancelBtn);
		okBtn = new JButton("Ok");
		okBtn.addActionListener(this);
		south.add(okBtn);
		add(south, BorderLayout.SOUTH);
		pack();
	}

	
	public void actionPerformed(ActionEvent e) 
	{
		cancelled = (e.getSource() == cancelBtn);
		setVisible(false);
	}
	
	
	public void itemStateChanged(ItemEvent e)
	{
		if (e.getStateChange() != ItemEvent.SELECTED)
			return;
		
		if (e.getSource() instanceof JRadioButton)
		{
			if (combo != null)
				combo.setEnabled(e.getSource() == existingExperRadio);
		}
	}
	
	
	public boolean wasCancelled()
	{
		return cancelled;
	}
	
	
	// If null, user wants a new experiment with default generated name.
	public Experiment getSelectedExperiment()
	{
		assert !wasCancelled();
		
		// Compute index into experiments list of selected experiment. -1 means new experiment.
		int xperIndex = -1;
		if (nExistingExperiments == 1)
			if (existingExperRadio.isSelected())
				xperIndex = 0;
		else 
			if (existingExperRadio.isSelected())
				xperIndex = combo.getSelectedIndex();
		
		if (xperIndex == -1)
			return null;
		
		assert experStudy != null;
		assert experStudy.getExperiments() != null;
		assert experStudy.getExperiments().size() > xperIndex;
		return experStudy.getExperiments().get(xperIndex);
	}
	
	
	static void sop(Object x)
	{
		System.out.println(x);
	}
	
	
	public static void main(String[] args)
	{
		sop("START");
		try
		{
			File serfile = new File("data/Sessions/ProCrocoTery.dex");
			SessionModel session = SessionModel.deserialize(serfile);
			ExperimentsStudy xs = ExperimentsStudy.buildSimpleTestInstance(session);
			ExperimentDestinationDialog dia = new ExperimentDestinationDialog(xs);
			dia.setVisible(true);
		}
		catch (Exception x)
		{
			sop("Stress: " + x.getMessage());
			x.printStackTrace();
		}
		sop("DONE");
	}
}
