package dexter.view.graph;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.io.*;
import javax.swing.*;
import dexter.MainDexterFrame;
import dexter.model.*;
import dexter.proximity.*;
import dexter.interpolate.*;
import dexter.util.StringUtils;
import dexter.view.graph.experiment.*;


public class LargeGraphDialog extends JDialog implements ActionListener, ItemListener
{
	private static JFileChooser		exportFileChooser;		// shared by all instances and by the main frame
	
	private MainDexterFrame			mainFrame;
	private ThumbnailGraph			sourceGraph;
	private JCheckBox				zeroMeanBox;
	private JButton					toExperimentBtn;		// optional
	private JCheckBox				hideUnselectedGenesBox;
	private JButton					selectAllBtn;
	private JButton					flipSelBtn;
	private JButton					deselectAllBtn;
	private JComboBox				interpolationCombo;
	private JButton					zoomBtn;
	private JButton					unzoomBtn;
	private JButton					okBtn;
	private LargeGraphPanel			graphPan;				// includes legend
	
	
	public LargeGraphDialog(MainDexterFrame mainFrame, ThumbnailGraph sourceGraph, boolean useExperimentButton)
	{
		this.mainFrame = mainFrame;
		this.sourceGraph = sourceGraph;
		
		graphPan = new LargeGraphPanel(this, sourceGraph);
		add(graphPan, BorderLayout.CENTER);
		
		zeroMeanBox = new JCheckBox("Zero mean");
		zeroMeanBox.addItemListener(this);
		zeroMeanBox.setToolTipText("Adjust each gene to mean = 0 (subtract gene's mean from all its values).");
		
		if (useExperimentButton)
		{
			toExperimentBtn = new JButton("Selected to experiment");
			toExperimentBtn.addActionListener(this);
		}
		
		hideUnselectedGenesBox = new JCheckBox("Hide unselected");
		hideUnselectedGenesBox.addItemListener(this);
		
		selectAllBtn = new JButton("Select all");
		selectAllBtn.addActionListener(this);
		
		flipSelBtn = new JButton("Invert selection");
		flipSelBtn.addActionListener(this);
		
		deselectAllBtn = new JButton("Deselect all");
		deselectAllBtn.addActionListener(this);
		
		interpolationCombo = new JComboBox(InterpolationAlgorithm.values());
		interpolationCombo.addItemListener(this);
		
		zoomBtn = new JButton("Zoom");
		zoomBtn.addActionListener(this);
		
		unzoomBtn = new JButton("Unzoom");
		unzoomBtn.addActionListener(this);
		
		okBtn = new JButton("Ok");
		okBtn.addActionListener(this);

		JPanel south = buildSouthPanel(zeroMeanBox, 
									   hideUnselectedGenesBox, 
									   toExperimentBtn, 
									   selectAllBtn, 
									   flipSelBtn, 
									   deselectAllBtn, 
									   interpolationCombo,
									   zoomBtn,
									   unzoomBtn,
									   okBtn);
		add(south, BorderLayout.SOUTH);
		
		// Usually this dialog is displayed because user wants to expand a thumbnail. If the thumbnail has
		// previously been expanded, and then the expansion dialog was dismissed, the thumbnail holds a
		// descriptor of the dialog's most recent configuration.
		if (sourceGraph instanceof ThumbnailGraph)
		{
			LargeGraphDisplayConfig config = ((ThumbnailGraph)sourceGraph).getExpansionConfig();
			if (config != null)
				setDisplayConfig(config);
		}
		
		pack();
	}
	
	
	// Subclasses can override to add southern controls wherever they want.
	protected JPanel buildSouthPanel(JCheckBox normalizeBox, 
									 JCheckBox hideUnselsBox,
									 JButton toExperimentBtn, 
									 JButton selectAllBtn,
									 JButton flipSelBtn,
									 JButton deselectAllBtn,
									 JComboBox interpolationCombo,
									 JButton zoomBtn,
									 JButton unzoomBtn,
									 JButton okBtn)
	{
		JPanel south = new JPanel(new GridLayout(0, 1));
		
		JPanel strip = new JPanel();
		strip.add(normalizeBox);
		strip.add(hideUnselsBox);
		strip.add(selectAllBtn);
		strip.add(flipSelBtn);
		strip.add(deselectAllBtn);
		south.add(strip);
		
		strip = new JPanel();
		strip.add(new JLabel("Interpolation"));
		strip.add(interpolationCombo);
		if (toExperimentBtn != null)
			strip.add(toExperimentBtn);
		strip.add(zoomBtn);
		strip.add(unzoomBtn);
		strip.add(okBtn);
		south.add(strip);
		
		return south;
	}
		
	
	public void actionPerformed(ActionEvent e) 
	{
		if (e.getSource() == okBtn)
		{
			setVisible(false);
			assert sourceGraph instanceof ThumbnailGraph;
			LargeGraphDisplayConfig config = buildDisplayConfig();
			((ThumbnailGraph)sourceGraph).setExpansionConfig(config);
		}
		
		else if (e.getSource() == selectAllBtn  || e.getSource() == deselectAllBtn)
		{
			graphPan.selectAll(e.getSource() == selectAllBtn);
			enableButtonsAfterSelectionChange();
		}
		
		else if (e.getSource() == flipSelBtn)
		{
			graphPan.invertSelections();			
			enableButtonsAfterSelectionChange();
		}
		
		else if (e.getSource() == toExperimentBtn)
		{
			// Add selected genes of the graph to a selected experiment, or to a new blank experiment. New 
			// experiment name is auto-generated; user can edit later via the experiment dialog. If this is 
			// the first experiment, just create. If there are pre-existing experiments, offer user the choice 
			// of creating a new one or adding to an existing one.
			Experiment destXper = null;
			SessionModel session = mainFrame.getSessionModel();
			ExperimentsStudy xpers = session.getExperimentsStudy();
			assert xpers != null;
			if (!xpers.isEmpty())
			{
				ExperimentDestinationDialog experDestDia = new ExperimentDestinationDialog(xpers);
				experDestDia.setVisible(true);
				if (experDestDia.wasCancelled())
					return;
				destXper = experDestDia.getSelectedExperiment();
			}
			if (destXper == null)
			{
				// New experiment, initialized by copying from graph associated with this dialog.
				String experName = Experiment.generateDefaultName();
				Vector<Gene> selGenes = graphPan.getSelectedGenes();
				mainFrame.addExperimentFor(selGenes, experName);  // registers the name
			}
			else
			{
				// Add genes and data to destination experiment. The experiment will automatically
				// pass the data to its graph(s).
				Map<Gene, Vector<float[]>> geneTimeMap = graphPan.getGraph().getGeneToTimeAndExpressionMap();
				for (Gene gene: graphPan.getSelectedGenes())
					destXper.add(gene, geneTimeMap.get(gene));
			}
		}
		
		else if (e.getSource() == zoomBtn)
		{
			getGraph().zoomIn();
		}
		
		else if (e.getSource() == unzoomBtn)
		{
			getGraph().unzoom();
		}
	}
	
	
	protected void setSelectedGenes(Collection<Gene> genes)
	{
		getLegend().selectGenes(genes);
	}
	
	
	protected void enableButtonsAfterSelectionChange()
	{
		int nGenes = graphPan.getGenes().size();
		int nSel = graphPan.getSelectedGenes().size();
		int nUnsel = nGenes - nSel;
		assert nUnsel >= 0;
		enableButtonsAfterSelectionChange(nSel, nUnsel);
	}
	
	
	// Subclasses should override to take care of their specific buttons.
	protected void enableButtonsAfterSelectionChange(int nSel, int nUnsel)
	{
		if (toExperimentBtn != null)
			toExperimentBtn.setEnabled(nSel > 0);
	}
	
	
	public void itemStateChanged(ItemEvent e)
	{
		if (e.getSource() == zeroMeanBox)
			normalizeToMeans(zeroMeanBox.isSelected());
		
		else if (e.getSource() == hideUnselectedGenesBox)
			getGraphPanel().setHideUnselectedGenes(hideUnselectedGenesBox.isSelected());
		
		else if (e.getSource() == interpolationCombo)
		{
			if (e.getStateChange() != ItemEvent.SELECTED)
				return;
			InterpolationAlgorithm algo = (InterpolationAlgorithm)interpolationCombo.getSelectedItem();
			getGraph().setInterpolationAlgorithm(algo);		// repaints the graph
			// Spline can paint outside the graph. GraphPanel.paintComponent() clears the background.
			graphPan.repaint();
		}
	}
	
	
	private void normalizeToMeans(boolean doNormalize)
	{
		getGraph().normalizeToMeans(doNormalize);
	}
	
	
	// After this dialog is hidden, the next time its graph is shown the configuration should
	// persist. The main frame holds configurations for previously seen graphs.
	private LargeGraphDisplayConfig buildDisplayConfig()
	{
		return new LargeGraphDisplayConfig(getSelectedGenes(), 
										   zeroMeanBox.isSelected(), 
										   hideUnselectedGenesBox.isSelected(),
										   getGraph().getInterpolationAlgorithm());
	}
	
	
	private void setDisplayConfig(LargeGraphDisplayConfig config)
	{
		// Selected genes.
		getLegend().selectGenes(config.getSelectedGenes());
		
		// Zero-mean normalization.
		if (config.getZeroMean() != zeroMeanBox.isSelected())
		{
			zeroMeanBox.removeItemListener(this);
			zeroMeanBox.setSelected(config.getZeroMean());
			normalizeToMeans(config.getZeroMean());
			zeroMeanBox.addItemListener(this);
		}
		
		// Hide unselected genes.
		if (config.getHideUnselected() != hideUnselectedGenesBox.isSelected())
		{
			hideUnselectedGenesBox.removeItemListener(this);
			hideUnselectedGenesBox.setSelected(config.getHideUnselected());
			getGraphPanel().setHideUnselectedGenes(hideUnselectedGenesBox.isSelected());
			hideUnselectedGenesBox.addItemListener(this);
		}
		
		// Interpolation.
		if (config.getInterpolationAlgorithm() != interpolationCombo.getSelectedItem())
		{
			interpolationCombo.removeItemListener(this);
			interpolationCombo.setSelectedItem(config.getInterpolationAlgorithm());
			getGraph().setInterpolationAlgorithm(config.getInterpolationAlgorithm());
		}
	}
	
	
	// If ostream is null, displays in a dialog.
	protected void export(PrintStream ostream) throws IOException
	{
		// Get represented studies.
		Set<Study> repStudies = new TreeSet<Study>();
		for (Gene gene: getSelectedGenes())
			repStudies.add(gene.getStudy());
		
		// Get represented non-timepoint predefined spreadsheet column roles.
		Vector<PredefinedSpreadsheetColumnRole> predefinedRoles = new Vector<PredefinedSpreadsheetColumnRole>();
		predefinedRoles.add(PredefinedSpreadsheetColumnRole.ID);
		for (Study study: repStudies)
		{
			Vector<PredefinedSpreadsheetColumnRole> predefRolesForStudy = study.getNonTimepointPredefinedColumnRoles();
			for (PredefinedSpreadsheetColumnRole role: predefRolesForStudy)
			{
				if (!predefinedRoles.contains(role))
				{
					predefinedRoles.add(role);
				}
			}
		}
		
		// Get represented user-defined roles.
		Vector<String> udefRoles = new Vector<String>();
		for (Study study: repStudies)
		{
			Vector<String> udefRolesForStudy = study.getNonTimepointUserDefinedColumnRoles();
			for (String role: udefRolesForStudy)
			{
				if (!udefRoles.contains(role))
				{
					udefRoles.add(role);
				}
			}
		}
		
		// Get timepoints. For now, don't sort.
		Vector<String> timepointColNames = new Vector<String>();
		for (Study study: repStudies)
		{
			Vector<String> timepointColsForStudy = study.getTimepointColumnNames();
			for (String colName: timepointColsForStudy)
			{
				if (!timepointColNames.contains(colName))
				{
					timepointColNames.add(colName);
				}
			}
		}
		
		// Header.
		Vector<String> headerPieces = new Vector<String>();
		headerPieces.add("Dataset");
		for (PredefinedSpreadsheetColumnRole role: predefinedRoles)
			headerPieces.add(role.toString());
		headerPieces.addAll(udefRoles);
		headerPieces.addAll(timepointColNames);
		String header = StringUtils.unsplit(headerPieces, '\t');
		if (ostream != null)
			ostream.println(header);
		
		// Lines. Each line is a gene.
		Vector<Vector<String>> cellsForDisplay = new Vector<Vector<String>>();
		for (Gene gene: getSelectedGenes())
		{
			Vector<String> genePieces = new Vector<String>();
			genePieces.add(gene.getStudy().getName());
			// Predefined roles.
			for (PredefinedSpreadsheetColumnRole predefRole: predefinedRoles)
			{
				String sval = gene.getValueForPredefinedRole(predefRole);
				if (sval == null)
					sval = "";
				genePieces.add(sval);
			}
			// User defined roles.
			for (String userDefRole: udefRoles)
			{
				String sval = gene.getValueForUserDefinedRole(userDefRole);
				if (sval == null)
					sval = "";
				genePieces.add(sval);
			}
			// Timepoints.
			int timepointIndex = 0;
			Vector<Float> geneExpressions = gene.getRawExpressions();
			Vector<String> timepointColNamesForStudy = gene.getStudy().getTimepointColumnNames();
			for (String timeptColName: timepointColNames)
			{
				if (!timepointColNamesForStudy.contains(timeptColName))
					continue;
				if (timepointIndex >= geneExpressions.size())
					continue;			// might happen if missing values somewhere, better to be safe
				float expression = geneExpressions.get(timepointIndex++);
				genePieces.add("" + expression);
			}
			//assert genePieces.size() == headerPieces.size()  :  
			//	genePieces.size() + " gene cells, " + headerPieces.size() + " header cells";
			String geneLine = StringUtils.unsplit(genePieces, '\t');
			if (ostream == null)
				cellsForDisplay.add(genePieces);
			else
				ostream.println(geneLine);
		}
		
		// Display in a dialog if ostream is null.
		if (ostream != null)
			return;
		ExportPreviewDialog previewDia = new ExportPreviewDialog(headerPieces, cellsForDisplay);
		for (int wideColNum=1; wideColNum<predefinedRoles.size(); wideColNum++)
			previewDia.setColumnIsWide(wideColNum);
		previewDia.setVisible(true);		// modal
	}
	
	
	public static JFileChooser getExportFileChooser()
	{
		if (exportFileChooser == null)
			exportFileChooser = new JFileChooser(new File("data/Exports"));
		return exportFileChooser;
	}
	

	public MainDexterFrame getMainFrame()					{ return mainFrame;					  }
	public LargeGraphPanel getGraphPanel()					{ return graphPan;					  }
	public LargeGraph getGraph()							{ return graphPan.getGraph(); 		  }
	public GenesLegend getLegend()							{ return graphPan.getLegend();		  }
	public Vector<Gene> getGenes()							{ return graphPan.getGenes(); 		  }
	public Vector<Gene> getSelectedGenes()					{ return graphPan.getSelectedGenes(); }
	public SessionModel getSession()						{ return mainFrame.getSessionModel(); }
	public static void sop(Object x)						{ System.out.println(x);			  }
	
	
	public static void main(String[] args)
	{
		dexter.MainDexterFrame.main(args);
	}
}
