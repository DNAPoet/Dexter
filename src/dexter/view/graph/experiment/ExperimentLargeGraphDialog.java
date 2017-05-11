package dexter.view.graph.experiment;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import javax.swing.*;
import java.util.*;
import dexter.MainDexterFrame;
import dexter.cluster.Metric;
import dexter.coreg.*;
import dexter.event.*;
import dexter.model.*;
import dexter.proximity.ProximityDialog;
import dexter.view.graph.*;
import dexter.util.gui.*;


//
// For experiments, the user has extra controls not available in regular graphs: color scheme, and searching.
//


public class ExperimentLargeGraphDialog extends LargeGraphDialog implements LegendListener<Gene>
{	
	private Experiment						experiment;
	private JButton							editNameBtn;
	private JButton							removeUnselectedGenesBtn;
	private JComboBox						colorSchemeCombo;
	private Map<AddBy, TaggedButton<AddBy>>	criterionToBtn;
	private JButton							proximityBtn;			// note proximity is not an add-by criterion	
	private JButton							importBtn;
	private JButton							exportBtn;
	private JButton							factorBtn;
	private JButton							undoBtn;
	private JButton							redoBtn;
	
	
	public ExperimentLargeGraphDialog(MainDexterFrame mainFrame, ThumbnailGraph source)
	{
		super(mainFrame, source, false);
		assert source.isExperiment();
		
		experiment = source.getExperiment();
		getGraph().setExperiment(experiment);
		experiment.addGraph(getGraph());
		setTitleByExperimentName(experiment.getName());
		
		// Register this object as a listener to legend events. As the number of selected genes
		// changes, some "Add by ..." buttons, and others, will enable/disable.
		getLegend().addLegendListener(this);
		
		// Don't allow UNDO to remove the original genes.
		getGraph().getHistory().setUnpoppable();
	}
	
	
	protected JPanel buildSouthPanel(JCheckBox normalizeBox, 		
									 JCheckBox hideUnselBox,
									 JButton toExperimentBtn, 
									 JButton selectAllBtn,
									 JButton flipSelBtn,
									 JButton deselectAllBtn,
									 JComboBox interpolationCombo,
									 JButton zoomBtn,
									 JButton unzoomBtn,
									 JButton okBtn)	
	{
		assert toExperimentBtn == null;
		JPanel southPanel = new JPanel(new GridLayout(0, 1));
		
		JPanel upperStrip = new JPanel();
		upperStrip.add(new JLabel("Color by"));
		colorSchemeCombo = new JComboBox(ColorScheme.values());
		colorSchemeCombo.addItemListener(this);
		upperStrip.add(colorSchemeCombo);		
		editNameBtn = new JButton("Edit name...");
		editNameBtn.addActionListener(this);
		upperStrip.add(editNameBtn);
		upperStrip.add(normalizeBox);
		upperStrip.add(selectAllBtn);
		upperStrip.add(flipSelBtn);
		upperStrip.add(deselectAllBtn);
		upperStrip.add(hideUnselBox);
		removeUnselectedGenesBtn = new JButton("Remove unselected");
		removeUnselectedGenesBtn.setEnabled(getSelectedGenes().size() > 0);
		removeUnselectedGenesBtn.addActionListener(this);
		upperStrip.add(removeUnselectedGenesBtn);
		southPanel.add(upperStrip);
		
		JPanel middleStrip = new JPanel();
		JPanel addGenesSubpan = new JPanel();		
		addGenesSubpan.setBorder(BorderFactory.createLineBorder(Color.BLACK));
		criterionToBtn = new HashMap<AddBy, TaggedButton<AddBy>>();
		addGenesSubpan.add(new JLabel("Add genes by "));
		Icon clockIcon = null;
		if (RedClockIcon.supportedOnThisPlatform())
			clockIcon = new RedClockIcon();
		Set<TaggedButton<AddBy>> clockedBtns = new HashSet<TaggedButton<AddBy>>();
		int nSelectedGenes = getSelectedGenes().size();
		for (AddBy crit: AddBy.values())
		{
			// If the clock icon is available on this platform, and response to the button
			// might be time-consuming, add the clock icon to the button.
			TaggedButton<AddBy> btn = null;
			if (clockIcon != null  &&  crit.mightBeSLow())
			{
				btn = new TaggedButton<AddBy>(crit, crit + "...", this, clockIcon);
				Dimension pref = btn.getPreferredSize();
				btn.setIconTextGap(-140);	// ?WTF? Probably very plaf-dependent
				btn.setPreferredSize(new Dimension(pref.width+40, pref.height));
				clockedBtns.add(btn);
			}
			else
				btn = new TaggedButton<AddBy>(crit, crit + "...", this);
			String tip = crit.getTooTipText();
			if (tip != null)
				btn.setToolTipText(tip);
			btn.setEnabled(crit.selectionCountIsOk(nSelectedGenes));
			addGenesSubpan.add(btn);
			criterionToBtn.put(crit, btn);
		}
		middleStrip.add(addGenesSubpan);
		proximityBtn = new JButton("Proximity...");
		proximityBtn.addActionListener(this);
		importBtn = new JButton("Import...");
		importBtn.addActionListener(this);
		exportBtn = new JButton("Export...");
		exportBtn.addActionListener(this);
		factorBtn = new JButton("Factor");
		factorBtn.addActionListener(this);
		middleStrip.add(interpolationCombo);
		JPanel historyPan = new JPanel();
		historyPan.setBorder(BorderFactory.createLineBorder(Color.BLACK));
		undoBtn = new JButton("Undo");
		undoBtn.addActionListener(this);		
		undoBtn.setEnabled(false);
		historyPan.add(undoBtn);
		redoBtn = new JButton("Redo");
		redoBtn.setEnabled(false);
		redoBtn.addActionListener(this);
		historyPan.add(redoBtn);
		middleStrip.add(historyPan);

		enableButtonsAfterSelectionChange();
		southPanel.add(middleStrip);
		
		// If some buttons have a clock icon and some don't, the ones with the icon will be taller. Adjust
		// heights of the ones with no icon to enforce consistent height.
		if (clockedBtns.size() >= 1  &&  clockedBtns.size() < criterionToBtn.size())
		{
			int prefH = clockedBtns.iterator().next().getPreferredSize().height;
			for (TaggedButton<AddBy> btn: criterionToBtn.values())
			{
				if (clockedBtns.contains(btn))
					continue;
				Dimension pref = btn.getPreferredSize();
				btn.setPreferredSize(new Dimension(pref.width, prefH));
			}
		}
		
		JPanel bottomStrip = new JPanel();
		bottomStrip.add(proximityBtn);
		bottomStrip.add(zoomBtn);
		bottomStrip.add(unzoomBtn);
		bottomStrip.add(exportBtn);
		bottomStrip.add(factorBtn);
		bottomStrip.add(okBtn);
		southPanel.add(bottomStrip);
		
		return southPanel;
	}
	
	
	public void setVisible(boolean b)
	{
		super.setVisible(b);
		if (b == false)
			getLegend().removeLegendListener(this);
	}
	
	
	protected void enableButtonsAfterSelectionChange(int nSel, int nUnsel)
	{
		super.enableButtonsAfterSelectionChange(nSel, nUnsel);
		
		// Do nothing if primordial.
		if (criterionToBtn == null)
			return;
		
		for (AddBy crit: criterionToBtn.keySet())
		{
			boolean enable = crit.selectionCountIsOk(nSel);
			criterionToBtn.get(crit).setEnabled(enable);
		}
		
		proximityBtn.setEnabled(getGraphPanel().canAnalyzeProximity());
	}
	
	
	private void setTitleByExperimentName(String xname)
	{
		if (xname.toUpperCase().startsWith("EXPERIMENT"))
			setTitle(xname);
		else
			setTitle("Experiment: " + xname);
	}
	
	
	public void actionPerformed(ActionEvent e)
	{
		if (e.getSource() == editNameBtn)
		{
			// Get new experiment name.
			ExperimentNameDialog dia = new ExperimentNameDialog(getExperiment().getName());
			dia.setVisible(true);	// modal
			if (dia.wasCancelled())
				return;
			String name = dia.getExperimentName();
			
			// Change name in experiment instance.
			getExperiment().setName(name);
			
			// Change name in this dialog's title.
			setTitleByExperimentName(name);
			
			// Change name in thumbnail title.
			for (Graph graph: getExperiment().getGraphs())
			{
				if (graph instanceof ThumbnailGraph)
				{
					ThumbnailGraph thumbnail = (ThumbnailGraph)graph;
					thumbnail.setTitle(name);
				}
			}
		}
		
		else if (e.getSource() == removeUnselectedGenesBtn)
		{
			experiment.openHistoryStep(false);					// false => step is deletion
			Collection<Gene> genesToDelete = getGenes();
			genesToDelete.removeAll(getSelectedGenes());
			for (Gene gene: genesToDelete)
			{
				assert experiment.contains(gene);
				assert experiment.graphsAndLegendsContainGene(gene);
				experiment.remove(gene);
			}
			experiment.closeHistoryStep();
			getGraphPanel().invalidate();
			getGraphPanel().validate();
		}
		
		else if (e.getSource() == proximityBtn)
		{
			assert getGraphPanel().canAnalyzeProximity();
			try
			{
				ProximityDialog dia = new ProximityDialog(getGraphPanel());
				dia.setVisible(true);
				if (dia.wasCanceled())
					return;
				if (dia.requestSelect())
					setSelectedGenes(dia.getSelectedGenes());
			}
			catch (IOException x)
			{
				String err = "Couldn't read file " + x.getMessage();
				JOptionPane.showMessageDialog(this, err);
			}
		}
		
		else if (e.getSource() == exportBtn)
		{
			JFileChooser exportChooser = getExportFileChooser();
			if (exportChooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION)
				return;	
			File exportFile = exportChooser.getSelectedFile();
			PrintStream ostream = null;
			try
			{
				if (exportFile.getName().equals("stdout"))
					ostream = System.out;
				else if (!exportFile.getName().equals("display"))
					ostream = new PrintStream(exportFile);
				export(ostream);
				if (ostream != null  &&  ostream != System.out)
					ostream.close();
			}
			catch (IOException x)
			{
				JOptionPane.showMessageDialog(this, "Can't export to file");
			}
		}
		
		else if (e.getSource() instanceof TaggedButton<?>)
		{
			// Add genes by a criterion.
			AddBy addBy = ((TaggedButton<AddBy>)e.getSource()).getTag();
			addGenesBy(addBy);		
			int nSelectedGenes = getSelectedGenes().size();
			criterionToBtn.get(AddBy.Expression_Similarity).setEnabled(nSelectedGenes == 1);
			enableButtonsAfterSelectionChange();
		}
		
		else if (e.getSource() == undoBtn)
		{
			assert experiment.canUndo();
			experiment.undo();
			undoBtn.setEnabled(getGraph().getHistory().canUndo());
			redoBtn.setEnabled(getGraph().getHistory().canRedo());
		}
		
		else if (e.getSource() == redoBtn)
		{
			assert experiment.canRedo();
			experiment.redo();
			undoBtn.setEnabled(getGraph().getHistory().canUndo());
			redoBtn.setEnabled(getGraph().getHistory().canRedo());
		}
		
		else if (e.getSource() == factorBtn)
		{
			factor();
		}
		
		else
		{
			super.actionPerformed(e);
		}
	}
	

	public void itemStateChanged(ItemEvent e)
	{
		if (e.getSource() == colorSchemeCombo)
		{		
			if (e.getStateChange() != ItemEvent.SELECTED)
				return;
			ColorScheme scheme = (ColorScheme)colorSchemeCombo.getSelectedItem();
			Experiment xper = getExperiment();
			assert !xper.getGraphs().isEmpty()  :  "No graphs for experiment " + xper.getName();
			for (Graph graph: xper.getGraphs())
				graph.enforceColorScheme(scheme);		// repaints
		}
		
		else
		{
			super.itemStateChanged(e);
		}
	}


	public void legendStateChanged(LegendEvent<Gene> e) 
	{
		// Enable/disable add buttons that are only meaningful if the number of selected genes is right.
		Vector<Gene> selectedGenes = getSelectedGenes();
		enableButtonsAfterSelectionChange();
		
		// Enable/disable delete-selected button.
		removeUnselectedGenesBtn.setEnabled(!selectedGenes.isEmpty());
	}
	
	
	public Experiment getExperiment()
	{			
		Experiment xper = getGraph().getExperiment();
		assert xper != null;
		return xper;
	}
	
	
	private void addGenesBy(AddBy addBy)
	{
		AbstractGeneSelectionPanel geneSelPan = null;
		
		Vector<Gene> selectedGenes = getSelectedGenes();
		assert addBy.selectionCountIsOk(selectedGenes.size());
		
		// Create a gene selection panel appropriate to the "add by" type.
		switch (addBy)
		{
			case Expression_Similarity:
				geneSelPan = getPanForAddGenesByExpressionSimilarity();
				break;

			case Pathway:
			case Gene_Name:
				geneSelPan = new GenesByTextualCriterionPanel(addBy, getSession());
				break;
				
			case Orthology:
				try
				{
					geneSelPan = new GenesByOrthologyPanel(selectedGenes,
														   getSession(), 
														   getGraph().getOrganismToColorMap());
				}
				catch (IOException x)
				{
					String err = "Couldn't load orthologies from file system: " + x.getMessage();
					JOptionPane.showMessageDialog(this, err);
					return;
				}
				if (((GenesByOrthologyPanel)geneSelPan).getNOrthologyGroups() == 0)
				{
					String err = "No genes orthologous to selected gene";
					if (selectedGenes.size() > 1)
						err += "s";
					err += ".";
					JOptionPane.showMessageDialog(this, err);
					return;
				}
				break;
				
			case Operon:
				geneSelPan = getPanForAddGenesByOperon();
				if (geneSelPan == null)
				{
					String err = "Selected gene is not in any known coregulation group.";
					JOptionPane.showMessageDialog(this, err);
					return;
				}
				break;
				
			default:
				String err = "Adding genes by " + addBy + " is under construction";
				JOptionPane.showMessageDialog(this, err);
				return;
		}
		
		// Build gene selector dialog.
		assert geneSelPan != null;
		OkWithContentDialog dia = geneSelPan.embedInDialog();
		
		// Identify genes to be added.
		dia.setVisible(true);
		if (dia.wasCancelled())
			return;
		Vector<Gene> genesToAdd = geneSelPan.getSelectedGenes();
		
		// Add genes to selected experiment. Experiment.add() updates all graphs that present the experiment,
		// and maintains history.
		Experiment experiment = getExperiment();
		experiment.openHistoryStep(true);			// true => step is an addition
		for (Gene gene: genesToAdd)
		{
			if (experiment.contains(gene))
				continue;
			Vector<float[]> txs = getMainFrame().getTimeAndExpressionPairsForGene(gene);
			assert txs != null  :  "null time/expression data for gene " + gene;
			experiment.add(gene, txs);				// manages gene color and presence in graph and legend
		}
		experiment.closeHistoryStep();
		undoBtn.setEnabled(getGraph().getHistory().canUndo());
		redoBtn.setEnabled(getGraph().getHistory().canRedo());
	}
	
	
	private AbstractGeneSelectionPanel getPanForAddGenesByExpressionSimilarity()
	{
		// Get query gene.
		Vector<Gene> selectedGenes = getSelectedGenes();
		assert selectedGenes.size() == 1;
		Gene queryGene = selectedGenes.firstElement();
		Vector<float[]> referenceExpressions = getGraph().getTimeAndExpressionPairs(queryGene);
		assert referenceExpressions != null;
		
		// Compute distances to all other genes.
		Metric metric = getMainFrame().getMetric();
		Map<Gene, Float> distances = new HashMap<Gene, Float>();
		Map<Gene, Vector<float[]>> geneToTxs = getMainFrame().getCompleteGeneToTxMap();
		for (Gene otherGene: geneToTxs.keySet())				
		{
			if (otherGene == queryGene)
				continue;
			if (distances.containsKey(otherGene))
				continue;
			float distance = metric.computeDistance(queryGene, referenceExpressions, 
													otherGene, geneToTxs.get(otherGene));
			distances.put(otherGene, distance);
		}
		// Pass distance table to gui to capture user selections.
		Map<Study, Color> studyToColor = getGraph().getStudyToColorMap();
		return new GenesByDistancePanel(queryGene, distances, studyToColor, 100);
	}
	
	
	// Returns null if the query gene doesn't belong to any known coregulation group
	private AbstractGeneSelectionPanel getPanForAddGenesByOperon()
	{
		// Get query gene.
		Vector<Gene> selectedGenes = getSelectedGenes();
		assert selectedGenes.size() == 1;
		Gene queryGene = selectedGenes.firstElement();
		
		// Get study and organism associated with the query gene. Eventually this will need to change, when
		// multiple studies per organism are supported.
		Organism org = queryGene.getOrganism();
		Study study = queryGene.getStudy();
		
		// Check coregulation files for organism for a group that contains the query gene.
		CoregulationFileCollection coregFileCollection = getSession().getCoregulationFiles();	// keyed by Organism
		Vector<CoregulationFile> coregFiles = coregFileCollection.get(org);
		CoregulationGroup coregGroup = null;
		String queryId = queryGene.getId();
		for (CoregulationFile file: coregFiles)
		{
			try
			{
				Vector<CoregulationGroup> groupsToCheck = file.getCoregulationGroups();
				for (CoregulationGroup group: groupsToCheck)
				{
					if (group.contains(queryId))
					{
						coregGroup = group;
						break;
					}
				}
					
			}
			catch (IOException x) { }		// IOException just means this file can't be checked
		}
		if (coregGroup == null)
			return null;
		
		// Found a group containing the query gene. Find any genes in the group that are part of a study.
		Map<String, Gene> idToStudiedGene = new HashMap<String, Gene>();
		for (Gene gene: study)
			if (coregGroup.contains(gene.getId()))
				idToStudiedGene.put(gene.getId(), gene);
		GenesByCoregulationPanel ret = new GenesByCoregulationPanel(coregGroup, idToStudiedGene);
		return ret;
	}
	

	// Creates a new experiment for each gene.
	private void factor()
	{
		Map<String, Gene> idToGene = new TreeMap<String, Gene>();
		for (Gene gene: experiment)
			idToGene.put(gene.getId(), gene);
		for (String id: idToGene.keySet())
		{
			Gene gene = idToGene.get(id);
			Vector<Gene> theGene = new Vector<Gene>();
			theGene.add(gene);
			int n = 1;
			String name = id;
			while (!Experiment.nameIsAvailable(name))
				name = id + "_" + n++;
			getMainFrame().addExperimentFor(theGene, name);  // registers the name
		}
	}
	
	
	public static void main(String[] args)
	{
		dexter.MainDexterFrame.main(args);
	}
}
