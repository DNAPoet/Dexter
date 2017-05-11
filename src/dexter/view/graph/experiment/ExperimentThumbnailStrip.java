package dexter.view.graph.experiment;

import java.util.*;
import java.awt.*;
import java.awt.event.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import javax.print.attribute.HashAttributeSet;
import javax.swing.*;

import dexter.model.*;
import dexter.view.graph.*;
import dexter.event.*;


public class ExperimentThumbnailStrip extends InvariantGroupingThumbnailStrip 
									  implements ActionListener, ThumbnailListener
{
	private JButton				addBtn;
	private JButton				removeBtn;
	private JButton				importBtn;
	private JFileChooser		importFileChooser;	
	private boolean				headless;				// s-a-l debugging

	
	public ExperimentThumbnailStrip(SessionModel session)
	{
		super(session);
		
		ExperimentsStudy xstudy = session.getExperimentsStudy();
		assert xstudy != null;
		
		// Build individual thumbnail graphs.
		Vector<ThumbnailGraph> thumbnails = new Vector<ThumbnailGraph>();
		GraphBackgroundModel backgroundModel = session.getGraphBackgroundModel();
		if (!xstudy.isEmpty())
		{
			for (Experiment experiment: xstudy.getExperiments())
			{				
				ThumbnailGraph thumbnail = buildExperimentThumbnailGraph(experiment);
				thumbnails.add(thumbnail);
			}
		}
		
		init(xstudy.getName(), backgroundModel, thumbnails);
	}
	
	
	// Debug only.
	private ExperimentThumbnailStrip()		{ }
	
	
	private ThumbnailGraph buildExperimentThumbnailGraph(Experiment experiment)
	{
		Map<Gene, Vector<float[]>> geneToTimeAndExpression = new TreeMap<Gene, Vector<float[]>>();
		for (Gene gene: experiment)
		{
			Study geneStudy = gene.getStudy();
			TimeAssignmentMap tam = session.getTimeAssignmentMapForStudy(geneStudy);
			Vector<float[]> timepointsForGene = gene.getTimeAndExpressionPairs(tam);
			geneToTimeAndExpression.put(gene, timepointsForGene);
		}
		ThumbnailGraph thumbnail = 
			new ExperimentThumbnailGraph(experiment, session, session.getGraphBackgroundModel(), geneToTimeAndExpression);
		thumbnail.setMouseArmsAndSelects(true);
		thumbnail.setStudy(session.getExperimentsStudy());
		thumbnail.setStrip(this);
		thumbnail.addThumbnailListener(this);						// sel changes, for enabling "-" button
		experiment.addGraph(thumbnail);
		for (ThumbnailListener listener: getThumbnailListeners())
			thumbnail.addThumbnailListener(listener);				// expansion requests go to some high level
		return thumbnail;
	}
	
	
	protected void populateControlPanel(JPanel controlPan)
	{		
		addBtn = new JButton("+");
		shrinkButtonForContolPanel(addBtn);
		addBtn.addActionListener(this);
		addBtn.setToolTipText("Add new experiment from selected thumbnails.");
		controlPan.add(addBtn);
		
		removeBtn = new JButton("-");
		shrinkButtonForContolPanel(removeBtn);
		removeBtn.setEnabled(false);
		removeBtn.addActionListener(this);
		removeBtn.setToolTipText("Remove all selected experiments");
		controlPan.add(removeBtn);
		
		importBtn = new JButton("...");
		/*********** For now: unsupported feature, just for me & Irina
		shrinkButtonForContolPanel(importBtn);
		importBtn.setEnabled(true);
		importBtn.addActionListener(this);
		importBtn.setToolTipText("Import csv list");
		controlPan.add(importBtn);
		***************/
	}
	
	
	// "Remove" button removes selected experiments, so should only be enabled if
	// there are selected experiments.
	public void enableOrDisableRemoveBtn()
	{
		for (ThumbnailGraph thumb: getThumbnails())
		{
			if (thumb.isSelected())
			{
				removeBtn.setEnabled(true);
				return;
			}
		}

		removeBtn.setEnabled(false);
	}
	
	
	public void actionPerformed(ActionEvent e)
	{
		if (e.getSource() == addBtn)
		{
			addExperimentThumbnail();
		}
		
		else if (e.getSource() == removeBtn)
		{
			removeExperimentThumbnailGraph();
		}
		
		else if (e.getSource() == importBtn)
		{
			importExperiments();
		}
	}  // end of actionPerformed()
	
	
	private void addExperimentThumbnail()
	{			
		// Create an empty experiment with default name.
		String name = Experiment.generateDefaultName();
		Experiment.registerName(name);
		Experiment experiment = new Experiment(name);		
		
		// Collect selected thumbnails from all strips.
		MultiThumbnailStripPanel multiStripPan = getMultiStripPanel();
		assert multiStripPan != null;
		Vector<ThumbnailStrip> allStrips = multiStripPan.getThumbnailStrips();
		Vector<ThumbnailGraph> selGraphs = new Vector<ThumbnailGraph>();
		for (ThumbnailStrip strip: allStrips)
		{
			for (ThumbnailGraph graph: strip.getThumbnails())
				if (graph.isSelected())
					selGraphs.add(graph);
		}

		// Create an experiment instance that aggregates all genes from all selected thumbnails. Allow for
		// duplication of genes.
		Set<Gene> seenGenes = new HashSet<Gene>();
		for (ThumbnailGraph graph: selGraphs)
		{
			for (Gene gene: graph.getGenes())
			{
				if (!seenGenes.contains(gene))
				{
					seenGenes.add(gene);
					experiment.add(gene);
				}
			}
		}
		
		// Build and install a new experiment graph.
		ThumbnailGraph newExperimentThumbnail = buildExperimentThumbnailGraph(experiment);
		addThumbnailGraph(newExperimentThumbnail);
		
		// Deselect everything.
		multiStripPan.deselectAll();
	}
	
	
	private void removeExperimentThumbnailGraph()
	{
		// Collect selected thumbnail graphs.
		Set<ThumbnailGraph> selectedThumbnails = new HashSet<ThumbnailGraph>();
		for (ThumbnailGraph thumbnail: getThumbnails())
			if (thumbnail.isSelected())
				selectedThumbnails.add(thumbnail);
		
		// Remove. Superclass method revalidates. Also deregister all names.
		ExperimentsStudy xstudy = session.getExperimentsStudy();
		for (ThumbnailGraph thumbnail: selectedThumbnails)
		{
			removeThumbnailGraph(thumbnail);
			assert thumbnail.getTitle() != null  :  "null title for thumbnail";
			Experiment.deregisterName(thumbnail.getTitle());
			xstudy.removeExperiment(thumbnail.getExperiment());
		}
		repaint();
		
		enableOrDisableRemoveBtn();
	}
	
	
	private void importExperiments()
	{
		JFileChooser importChooser = getImportFileChooser();
		if (importChooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION)
			return;	
		importExperiments(importChooser.getSelectedFile());
	}
	
	
	//
	//      PREFIX=Tery_
	//		Sigma factors: 0385,0784,0920,1167,2937-2939,2988,3443
	//		...
	//
	//
	private void importExperiments(File importFile)
	{		
		Vector<Experiment> expers = new Vector<Experiment>();
		try
		{
			FileReader fr = new FileReader(importFile); 
			BufferedReader br = new BufferedReader(fr);
			String prefix = null;
			String line = null;
			while ((line = br.readLine()) != null)
			{
				if (line.startsWith("PREFIX="))
				{
					prefix = line.substring(1+line.indexOf('='));
					continue;
				}
				if (line.startsWith("#"))
					continue;
				String xperName = null;
				int nColon = line.indexOf(':');
				if (nColon >= 0)
				{
					xperName = line.substring(0, nColon).trim();
					line = line.substring(nColon+1).trim();
				}
				String[] pieces = line.split(",");
				Vector<String> ids = new Vector<String>();
				for (String piece: pieces)
				{
					// "0385", "Tery_0385", or "2937-2939"
					if (piece.startsWith(prefix))
						ids.add(piece);
					else if (piece.contains("-"))
					{
						int nSep = piece.indexOf('-');
						String sFrom = piece.substring(0, nSep);
						String sTo = piece.substring(nSep+1);
						int from = Integer.parseInt(sFrom);
						int to = Integer.parseInt(sTo);
						if (to < from)
						{
							int temp = to;
							to = from;
							from = temp;
						}
						for (int i=from; i<=to; i++)
						{
							String s = "" + i;
							while (s.length() < sFrom.length())
								s = "0" + s;
							if (prefix != null)
								s = prefix + s;
							ids.add(s);
						}
					}
					else
						ids.add(prefix+piece);
				}	
				
				Experiment exper = (xperName == null)  ?  new Experiment()  :  new Experiment(xperName);
				for (String id: ids)
				{
					Gene gene = null;
					outer: for (Study study: session.getStudies())
					{
						for (Gene g: study)
						{
							if (g.getId().equals(id))
							{
								gene = g;
								break outer;
							}
						}
					}
					if (gene != null)
						exper.add(gene);
				}
				if (!exper.isEmpty())
					expers.add(exper);
			}
			br.close();
			fr.close();
		}
		catch (IOException x)
		{
			JOptionPane.showMessageDialog(this, "Can't import from file");
		}
		
		if (headless)
			return;
		
		for (Experiment x: expers)
		{
			ThumbnailGraph newExperimentThumbnail = buildExperimentThumbnailGraph(x);
			addThumbnailGraph(newExperimentThumbnail);
		}
	}

	
	private JFileChooser getImportFileChooser()
	{
		if (importFileChooser == null)
			importFileChooser = new JFileChooser(new File("data/ExperimentImports"));
		return importFileChooser;
	}
	
	
	private class ExperimentNameDialog extends JDialog implements ActionListener
	{
		private JTextField			tf;
		private JButton				cancelBtn;
		private boolean 			cancelled;
		
		ExperimentNameDialog()
		{
			setModal(true);
			JPanel north = new JPanel();
			north.add(new JLabel("Experiment Name:"));
			String defaultName = Experiment.generateDefaultName();
			tf = new JTextField("   " + defaultName);
			tf.addActionListener(this);
			north.add(tf);
			add(north, BorderLayout.NORTH);
			JPanel south = new JPanel();
			JButton applyBtn = new JButton("Apply");
			applyBtn.addActionListener(this);
			south.add(applyBtn);
			cancelBtn = new JButton("Cancel");
			cancelBtn.addActionListener(this);
			south.add(cancelBtn);
			add(south, BorderLayout.SOUTH);
			pack();
		}
		
		public void actionPerformed(ActionEvent e)
		{
			if (e.getSource() == cancelBtn)
			{
				cancelled = true;
				setVisible(false);
			}
			
			else
			{
				String newName = getExperimentName();
				if (!Experiment.nameIsAvailable(newName))
					JOptionPane.showMessageDialog(this, "Name is in use: " + newName);
				else
					setVisible(false);
			}
		}
		
		boolean wasCancelled()		{ return cancelled; }
		String getExperimentName()	{ return tf.getText().trim(); }
	}  // End of inner class ExperimentNameDialog
	
	
	// Side effect: creates and returns an Experiment instance.
	public Experiment addExperimentForSourceAndRegisterName(String experimentName, Collection<Gene> genes)
	{
		// Register name.
		assert Experiment.nameIsAvailable(experimentName);
		Experiment.registerName(experimentName);
		
		// Create experiment. Initial collection of genes cannot be undone by history mechanism.
		Experiment experiment = new Experiment(experimentName);
		experiment.addAll(genes);
		experiment.setHistoryPrimordial();
		session.getExperimentsStudy().addExperiment(experiment);
		
		// Create thumbnail graph.
		ThumbnailGraph thumbnail = buildExperimentThumbnailGraph(experiment);
		thumbnail.setExperiment(experiment);
		
		// Add to this strip.
		addThumbnailGraph(thumbnail);
		experiment.addGraph(thumbnail);
		assert experiment.getGraphs().size() == 1;
		
		return experiment;
	}


	// Can't unrestrict the experiments.
	protected RoleValueToGenesMap mapRoleValuesToGenesNoRestrictions() 
	{
		return null;
	}


	public void thumbnailSelectionChanged(ThumbnailEvent e) 
	{
		enableOrDisableRemoveBtn();
	}
	
	
	// For the ThumbnailListener interface.
	public void thumbnailRequestedExpansion(ThumbnailEvent e) 		{ }
	
	
	public static void main(String[] args)
	{
		/***/
		try
		{
			SessionModel session =  SessionModel.deserialize(new File("data/Sessions/CPT.dex"));	
			ExperimentThumbnailStrip strip = new ExperimentThumbnailStrip();
			strip.session = session;
			strip.headless = true;
			strip.importExperiments(new File("data/ExperimentImports/TeryDec2014.txt"));
		}
		catch (Exception x)
		{
			sop("Stress: " + x.getMessage());
			x.printStackTrace();
		}
		sop("DONE");
		/****/
		//dexter.MainDexterFrame.main(args);
	}
}
