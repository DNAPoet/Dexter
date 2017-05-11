package dexter;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import java.io.*;
import java.security.acl.LastOwnerException;
import java.util.*;

import dexter.cluster.*;
import dexter.event.*;
import dexter.model.*;
import dexter.ortholog.OrthologyFileCollection;
import dexter.view.graph.*;
import dexter.view.graph.experiment.*;
import dexter.view.restrict.RestrictionPanel;
import dexter.view.cluster.*;
import dexter.util.gui.*;
import dexter.util.LocalMath;
import dexter.util.StringUtils;
import dexter.cluster.ClusterProgressListener;


public class MainDexterFrame extends JFrame 
	implements ActionListener, ItemListener, ThumbnailListener, VisualConstants
{
	private SessionModel 							sessionModel;
	private MultiThumbnailStripPanel				multiStripPan;
	private JCheckBox								scrollLockCbox;
	private JComboBox								groupCombo;		
	private JComboBox								orderCombo;		
	private JButton									restrictBtn;
	private JButton									unrestrictBtn;
	private JButton									clearSelBtn;
	private JButton									exportBtn;
	private JButton									quitBtn;
	private JComboBox								metricCombo;
	private JButton									clusterBtn;
	private RestrictionPanel 						restrictionPan;
	private IdToGeneMap								idToGeneMap;
	
	
	
	
	
				
	

	
					
					//////////////////////////////////////////////////////////////////
					//                                                              //
					//                         CONSTRUCTION                         //
					//                                                              //
					//////////////////////////////////////////////////////////////////
					
	
	
	
	
	
	// Probably only for debugging.
	MainDexterFrame(File serializedSessionFile) throws IOException
	{
		setTitle("Dexter");
		
		try
		{
			SessionModel sessionModel = new SessionModel(serializedSessionFile);
			initGUI(sessionModel);
		}
		catch (ClassNotFoundException x)
		{
			sop("Can't deserialize " + serializedSessionFile.getAbsolutePath() + "\n" + x.getMessage());
			x.printStackTrace(System.out);
			System.exit(1);
		}
	}
	
	
	public MainDexterFrame(SessionModel sessionModel) throws IOException
	{		
		initGUI(sessionModel);
	}
	
	
	private void initGUI(SessionModel sessionModel) throws IOException
	{
		// There's only 1 tooltip manager, so settings apply to all components.
	    ToolTipManager.sharedInstance().setInitialDelay(0);
	    ToolTipManager.sharedInstance().setDismissDelay(Integer.MAX_VALUE);
	        
		for (Study study: sessionModel.getStudies())
			study.validateGenes();
		
		JPanel south = new JPanel(new GridLayout(0, 1));
		JPanel southStrip = new JPanel();
		Vector<SpreadsheetColumnRole> colRoles = sessionModel.getGroupableColumnRoles();
		if (!colRoles.isEmpty())
		{
			southStrip.add(new JLabel("Group by"));
			Vector<GroupGenesBy> groupBys = new Vector<GroupGenesBy>();
			for (SpreadsheetColumnRole role: colRoles)
				groupBys.add(new GroupGenesBy(role));
			if (sessionModel.hasCoregulation())
				groupBys.add(GroupGenesBy.getCoregulationInstance());
			groupBys.add(getGroupGenesBy());
			groupCombo = new JComboBox(groupBys);
			groupCombo.addItemListener(this);
			southStrip.add(groupCombo);
		}
		southStrip.add(new JLabel("Order by"));
		orderCombo = new JComboBox(OrderGeneGroupsBy.values());
		orderCombo.setSelectedItem(OrderGeneGroupsBy.NAME);
		orderCombo.addItemListener(this);
		southStrip.add(orderCombo);
		clusterBtn = new JButton("Cluster...");
		clusterBtn.addActionListener(this);
		southStrip.add(clusterBtn);
		scrollLockCbox = new JCheckBox("Lockstep scrolling");
		scrollLockCbox.addItemListener(this);
		southStrip.add(scrollLockCbox);
		quitBtn = new JButton("Quit");
		quitBtn.addActionListener(this);
		southStrip.add(quitBtn);
		south.add(southStrip);
		
		southStrip = new JPanel();
		clearSelBtn = new JButton("Deselect all");
		clearSelBtn.addActionListener(this);
		southStrip.add(clearSelBtn);
		south.add(southStrip);
		southStrip.add(new JLabel("Metric"));
		metricCombo = new JComboBox(Metric.values());
		metricCombo.addItemListener(this);
		southStrip.add(metricCombo);
		restrictBtn = new JButton("Restrict...");
		restrictBtn.addActionListener(this);
		southStrip.add(restrictBtn);
		unrestrictBtn = new JButton("Unrestrict");
		unrestrictBtn.setEnabled(false);
		unrestrictBtn.addActionListener(this);
		southStrip.add(unrestrictBtn);
		exportBtn = new JButton("Export...");
		exportBtn.addActionListener(this);
		southStrip.add(exportBtn);
		south.add(southStrip);
		add(south, BorderLayout.SOUTH);
		
		this.sessionModel = sessionModel;
		multiStripPan = new MultiThumbnailStripPanel(sessionModel, true, this);
		multiStripPan.addThumbnailListener(this);
		multiStripPan.setBorder(BorderFactory.createLineBorder(Color.BLACK, 2));
		add(multiStripPan, BorderLayout.CENTER);
		
		pack();
	}	
	
	

	
	
	
	
	
					//////////////////////////////////////////////////////////////////
					//                                                              //
					//                            EVENTS                            //
					//                                                              //
					//////////////////////////////////////////////////////////////////
					
	
	

	public void actionPerformed(ActionEvent e)
	{
		if (e.getSource() == quitBtn)
		{
			System.exit(0);
		}
		
		else if (e.getSource() == clearSelBtn)
		{
			multiStripPan.deselectAll();
		}
		
		else if (e.getSource() == restrictBtn)
		{
			restrict();
		}
		
		else if (e.getSource() == unrestrictBtn)
		{
			unrestrict();
		}
		
		else if (e.getSource() == clusterBtn)
		{
			cluster();
		}
		
		else if (e.getSource() == exportBtn)
		{
			export();
		}
	}	
	

	public void itemStateChanged(ItemEvent e)
	{
		if (e.getSource() instanceof JComboBox  &&  e.getStateChange() != ItemEvent.SELECTED)
			return;
		
		if (e.getSource() == scrollLockCbox)
		{
			multiStripPan.setScrollLockstep(scrollLockCbox.isSelected());
		}
		
		else if (e.getSource() == groupCombo  ||  e.getSource() == orderCombo)
		{
			if (e.getSource() == groupCombo  &&  restrictionPan != null)
				restrictionPan.setGroupBy(getGroupBy());
			regroup(true);
		}
	}
	
	
	// Side effects: creates and returns an Experiment instance, and registers the experiment name.
	public Experiment addExperimentFor(Collection<Gene> genes, String experimentName)
	{
		Experiment newExperiment = multiStripPan.addExperimentFor(genes, experimentName);
		assert newExperiment.getGraphs().size() == 1;
		return newExperiment;
	}
 
	
	public void thumbnailSelectionChanged(ThumbnailEvent e) 	{  }


	public void thumbnailRequestedExpansion(ThumbnailEvent e) 
	{
		ThumbnailGraph src = e.getThumbnail();
		assert src != null  :  "No thumbnail source in thumbnailRequestedExpansion()";
		assert src.getSession() != null  :  "Null session for thumbnail source in thumbnailRequestedExpansion()";
		assert src.getStrip() != null  :  "Null strip for thumbnail source in thumbnailRequestedExpansion()";
		
		LargeGraphDialog expansionDia = null;
		Study study = src.getStudy();
		String title = null;
		
		if (src.isExperiment())
		{
			assert study != null;
			expansionDia = new ExperimentLargeGraphDialog(this, src);
			title = study.getName() + ": " + src.getTitle();
		}
		
		else if (src.getStrip().isClusterStrip())
		{
			assert study == null;
			expansionDia = new LargeGraphDialog(this, src, true);		// last arg: provide "experiment" button
			title = src.getTitle();
		}
		
		else
		{
			// Expand an ordinary thumbnail.
			expansionDia = new LargeGraphDialog(this, src, true);		// last arg: provide "experiment" button
			assert src != null;
			if (study != null)
				title = study.getName() + ": " + src.getTitle();
		}
		expansionDia.setTitle(title);
		
		expansionDia.setVisible(true);
	}


	
	
	
	
	
					
					
					//////////////////////////////////////////////////////////////////
					//                                                              //
					//                     GROUPING & ORDERING                      //
					//                                                              //
					//////////////////////////////////////////////////////////////////
					
	
	
	
	private GroupGenesBy getGroupGenesBy()
	{
		return (groupCombo != null)  ?  
			(GroupGenesBy)groupCombo.getSelectedItem()  :  
			GroupGenesBy.getForAppearanceOrderBySize(20);
	}

	
	private void regroup()
	{
		regroup(false);
	}
	
	
	private void regroup(boolean verbose)
	{
		multiStripPan.regroup(getGroupGenesBy(), getOrderBy(), getMetric(), getRestrictions());
	}
	
	
	private OrderGeneGroupsBy getOrderBy()
	{
		return (OrderGeneGroupsBy)orderCombo.getSelectedItem();  
	}
	
	
	

	
	
	
						
						
						/////////////////////////////////////////////////////////
						//                                                     //
						//                     CLUSTERING                      //
						//                                                     //
						/////////////////////////////////////////////////////////
			


	
	
	private void cluster()
	{
		// Capture params.
		ClusterDialog clusterDia = new ClusterDialog();
		clusterDia.setLocation(320,  450);
		clusterDia.setVisible(true);  		// modal
		if (clusterDia.cancelled())
			return;
		
		// Build tree de novo or from a newick file.
		Node<Gene> root = null;
		File newickFile = clusterDia.getNewickFile();
		if (newickFile == null)
		{
			// De novo.
			root = buildTreeDenovo(clusterDia);
		}
		else
		{
			// Newick file.
			try
			{
				root = loadNewick(newickFile);
			}
			catch (IOException x)
			{
				String err = "Couldn't load newick file " + newickFile.getName() + ": " + x.getMessage();
				JOptionPane.showMessageDialog(this, err);
				return;
			}
		}
		
		// If tree is available, display it and and capture partitioning. If not available, it is being built
		// asynchronously; completion will be reported by callback, and the callback handler will take care of
		// this.
		if (root != null)
			captureTreePartitioningAndAddClusterStrip(root);
	}
	
	
	private void captureTreePartitioningAndAddClusterStrip(Node<Gene> root)
	{		
		assert root != null;
		
		// In case Newick parser or tree builder doesn't set parent fields correctly.
		root.setParentFields();
		
		GeneTreeDialog treeDia = new GeneTreeDialog(root, sessionModel.getStudyToColorMap());
		treeDia.setTitle(root.collectLeafNodes().size() + " genes");
		Map<Gene, Vector<float[]>> geneToTxs = multiStripPan.getCompleteGeneToTimeAndExpressionMap();
		GraphBackgroundModel backgroundModel = sessionModel.getGraphBackgroundModel();
		treeDia.setShowSparklines(geneToTxs, backgroundModel);
		
		treeDia.setModal(true);
		treeDia.setVisible(true);
		if (treeDia.wasCancelled())
			return;
		Vector<Vector<Gene>> clusters = treeDia.collectSelectedSubtreeLeavesBySubtree();
		assert clusters != null;
		if (clusters.isEmpty())
		{
			JOptionPane.showMessageDialog(this, "No clusters selected");
			return;
		}
		
		multiStripPan.addClusterThumbnailStrip(this, clusters, root);
	}
	
	
	// Deployment: no limit on # of genes.
	private Node<Gene> buildTreeDenovo(ClusterDialog clusterDia)
	{
		return buildTreeDenovo(clusterDia, -1);
	}
	
	
	private Node<Gene> buildTreeDenovo(ClusterDialog clusterDia, int maxGenes)
	{
		// Collect genes of interest, along with expressions. Adjust to zero mean expression for each gene.
		GeneSelectionLevel geneSelLevel = clusterDia.getGeneSelectionLevel();
		Map<Gene, Vector<float[]>> rawGeneToTx = multiStripPan.collectGenesToTxsBySelectionLevel(geneSelLevel);
		assert rawGeneToTx.size() >= 2;
		Map<Gene, Vector<float[]>> geneToTxZeroMean = new HashMap<Gene, Vector<float[]>>();
		for (Gene gene: rawGeneToTx.keySet())
		{
			float meanExpression = LocalMath.getMeanExpression(rawGeneToTx.get(gene));
			Vector<float[]> rawTXs = rawGeneToTx.get(gene);
			Vector<float[]> txsZeroMean = new Vector<float[]>(rawTXs.size());
			for (float[] rawTX: rawTXs)
				txsZeroMean.add(new float[] { rawTX[0], rawTX[1]/meanExpression });
			geneToTxZeroMean.put(gene, txsZeroMean);
			if (maxGenes > 0  &&  geneToTxZeroMean.size() == maxGenes)
				break;
		}
		assert geneToTxZeroMean.size() >= 2;
		
		// The dialog will immediately return null if it needs to work asynchronously, and completion will
		// be reported by callback.
		ClusterManagerAndProgressDialog clusterManagerDia = 
			new ClusterManagerAndProgressDialog(geneToTxZeroMean, getMetric(), clusterDia.getAlgorithm());
		clusterManagerDia.setTerminationCallbackListener(new ClusterTerminationListener(clusterManagerDia));
		clusterManagerDia.setForceGui(); clusterManagerDia.setForceAsynchronous();
		Node<Gene> root = clusterManagerDia.cluster();
		return root;
	}
	
	
	private Node<Gene> loadNewick(File newickFile) throws IOException
	{
		NewickParser<String> parser = new NewickParser<String>(newickFile, new NewickPayloadBuilderStringIdentity());
		Node<String> stringRoot = parser.parse();
		if (idToGeneMap == null)
			idToGeneMap = new IdToGeneMap(sessionModel);
		Node<Gene> geneRoot = new GeneNode(stringRoot, idToGeneMap);
		return geneRoot;
	}	

	
	private class ClusterTerminationListener extends ClusterProgressAdapter
	{
		private ClusterManagerAndProgressDialog		dia;
		
		ClusterTerminationListener(ClusterManagerAndProgressDialog dia)		{ this.dia = dia; }
		
		public void clusteringFinished(ClusterProgressEvent e)
		{
			Node<Gene> root = dia.getTree();
			assert root != null;
			dia.setVisible(false);
			captureTreePartitioningAndAddClusterStrip(root);
		}
	}  // end of inner class ClusterTerminationListener
	
	
	
	
	
	
	
					
					//////////////////////////////////////////////////////////////////
					//                                                              //
					//                          RESTRICTION                         //
					//                                                              //
					//////////////////////////////////////////////////////////////////
					
	
	
	
	private void restrict()
	{
		// Capture restrictions.
		if (restrictionPan == null)
			restrictionPan = new RestrictionPanel(sessionModel, 
											      null,
												  getGroupBy(),
											      getOrderBy(),
											      getMetric(), 
											      multiStripPan);
		OkWithContentDialog dia = new OkWithContentDialog(restrictionPan, true);	// true => show "Cancel" button
		dia.setModal(true);
		dia.setVisible(true);
		if (dia.wasCancelled())
			return;
				
		// Update thumbnails.		
		ExpressionRestrictionModel restrictions = restrictionPan.getExpressionRestrictions();
		multiStripPan.applyExpressionRestrictions(restrictions);
		
		// Permit unrestricting.
		unrestrictBtn.setEnabled(true);
	}
	
	
	// Returns null if no restrictions have been specified yet.
	private ExpressionRestrictionModel getRestrictions()
	{
		return (restrictionPan != null)  ?  restrictionPan.getExpressionRestrictions()  :  null;
	}
	
	
	private void unrestrict()
	{
		multiStripPan.unrestrict();
		unrestrictBtn.setEnabled(false);
		
		// For multiple consecutive restrictions, we want the restriction dialog to appear reflecting its
		// last configuration. But after unrestricting, we want the dialog to be reset.
		restrictionPan = null;
	}
	
	
	
				
						
						
						
						/////////////////////////////////////////////////////////
						//                                                     //
						//                      EXPORTING                      //
						//                                                     //
						/////////////////////////////////////////////////////////
					

	
	
	private void export()
	{
		JFileChooser exportFileChooser = LargeGraphDialog.getExportFileChooser();	// constructs if necessary
		if (exportFileChooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION)
			return;	
		File exportFile = exportFileChooser.getSelectedFile();
		if (!exportFile.getName().endsWith(".tsv"))
			exportFile = new File(exportFile.getAbsolutePath() + ".tsv");
		PrintStream ostream = null;
		try
		{
			if (exportFile.getName().equals("stdout"))
				ostream = System.out;
			else if (!exportFile.getName().equals("display"))
				ostream = new PrintStream(exportFile);
			doExport(ostream);
			if (ostream != null  &&  ostream != System.out)
				ostream.close();
		}
		catch (IOException x)
		{
			JOptionPane.showMessageDialog(this, "Can't export to file");
		}
	}
	
	
	// If ostream is null, displays in a dialog.  
	private void doExport(PrintStream ostream) throws IOException
	{
		// Get represented studies.
		Set<Study> repStudies = new TreeSet<Study>();
		for (ThumbnailStrip strip: multiStripPan.getThumbnailStrips())
		{
			if (strip.isExperimentsStrip()  ||  strip.isClusterStrip())
				continue;
			repStudies.add(strip.getStudy());
		}
		
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
		headerPieces.add("Dataset/Experiment/Tree");					// i.e. vertical strip
		headerPieces.add("Group");										// i.e. thumbnail name
		headerPieces.add("Mean Dist Within Group");
		for (PredefinedSpreadsheetColumnRole role: predefinedRoles)
			headerPieces.add(role.toString());
		headerPieces.addAll(udefRoles);
		headerPieces.addAll(timepointColNames);
		String header = StringUtils.unsplit(headerPieces, '\t');
		if (ostream != null)
			ostream.println(header);
		
		// Determine selected thumbnails. Selection can be explicit or by inclusion in a selected strip.
		Map<ThumbnailGraph, ThumbnailStrip> selThumbnailToStrip = new LinkedHashMap<ThumbnailGraph, ThumbnailStrip>();
		for (ThumbnailStrip strip: multiStripPan.getThumbnailStrips())
		{
			for (ThumbnailGraph thumbnail: strip.getThumbnails())
			{
				if (strip.isSelected()  ||  thumbnail.isSelected())
				{
					selThumbnailToStrip.put(thumbnail, strip);
				}
			}
		}
		
		// Lines. Each line is a gene.
		Vector<Vector<String>> cellsForDisplay = new Vector<Vector<String>>();
		for (ThumbnailGraph thumbnail: selThumbnailToStrip.keySet())
		{
			ThumbnailStrip strip = selThumbnailToStrip.get(thumbnail);
			String stripName = strip.getTitle();
			String thumbnailName = thumbnail.getTitle();
			Collection<Gene> genes = thumbnail.getGenes();
			float meanDist = getMetric().getMeanDistance(genes, multiStripPan);
			String sMeanDist = "" + meanDist;
			for (Gene gene: genes)
			{
				// Strip and thumbnail values.
				Vector<String> genePieces = new Vector<String>();
				genePieces.add(stripName);
				genePieces.add(thumbnailName);
				genePieces.add(sMeanDist);
				// Predefined roles.
				for (PredefinedSpreadsheetColumnRole predefRole: predefinedRoles)
				{
					String sval = gene.getValueForPredefinedRole(predefRole);
					if (sval == null)
						sval = " ";
					genePieces.add(sval);
				}
				// User defined roles.
				for (String userDefRole: udefRoles)
				{
					String sval = gene.getValueForUserDefinedRole(userDefRole);
					if (sval == null)
						sval = " ";
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
				String geneLine = StringUtils.unsplit(genePieces, '\t');
				if (ostream == null)
					cellsForDisplay.add(genePieces);
				else
					ostream.println(geneLine);
			}
		}
		
		// Display in a dialog if ostream is null.
		if (ostream != null)
			return;
		ExportPreviewDialog previewDia = new ExportPreviewDialog(headerPieces, cellsForDisplay);
		for (int wideColNum=0; wideColNum<predefinedRoles.size()+3; wideColNum++)
			previewDia.setColumnIsWide(wideColNum);
		previewDia.setVisible(true);		// modal
	}
				
	
	
	
	
	
	
	
	
					
					//////////////////////////////////////////////////////////////////
					//                                                              //
					//                         MISC & MAIN                          //
					//                                                              //
					//////////////////////////////////////////////////////////////////
	
	
	
	
	
	public Vector<ThumbnailStrip> getThumbnailStrips()
	{
		return multiStripPan.getThumbnailStrips();
	}
	
	
	public Vector<float[]> getTimeAndExpressionPairsForGene(Gene gene)
	{
		return multiStripPan.getTimeAndExpressionPairsForGene(gene);
	}
	
	
	public Vector<Gene> collectGenesBySelectionLevel(GeneSelectionLevel level)
	{
		return multiStripPan.collectGenesBySelectionLevel(level);
	}
	
	
	public Vector<ThumbnailGraph> collectSelectedThumbnails()
	{
		return multiStripPan.collectSelectedThumbnails();
	}
	

	public GroupGenesBy getGroupBy()				
	{ 
		return (groupCombo != null)  ?  (GroupGenesBy)groupCombo.getSelectedItem()  :  null;
	}
	
	
	public Map<Gene, Vector<float[]>> getCompleteGeneToTxMap()
	{
		return multiStripPan.getCompleteGeneToTimeAndExpressionMap();
	}
	
	
	public MultiThumbnailStripPanel getMultiStripPanel()	{ return multiStripPan; 						}
	public TimeAndExpressionProvider getTXProvider()		{ return multiStripPan;							}
	public Metric getMetric()								{ return (Metric)metricCombo.getSelectedItem(); }
	public SessionModel getSessionModel()					{ return sessionModel;						    }
	static void sop(Object x)								{ System.out.println(x); 						}
	
	
	public static void main(String[] args)
	{
		try
		{
			File serfile = new File("data/Sessions/CPT.dex");
			MainDexterFrame frame = new MainDexterFrame(serfile);
			frame.setVisible(true);
		}
		catch (Exception x)
		{
			sop("STRESS: " + x.getMessage());
			x.printStackTrace(System.out);
			System.exit(1);
		}
	}
}


