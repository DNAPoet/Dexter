package dexter.view.cluster;

import java.util.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import javax.swing.*;
import dexter.model.*;
import dexter.cluster.*;
import dexter.util.LocalMath;


//
// Clusters genes, returning a tree. Optionally displays progress. 
//
// Uses a heuristic to decide whether or not to show progress in a GUI, and whether or not to work
// asynchronously in a dedicated thread. Heuristics can be overridden. if synchronous, returns the
// tree. If asynchronous, returns null immediately and later reports completion by a callback to the
// caller. The callback is the clusteringFinished() method of the interface; no other notifications are posted.
//


public class ClusterManagerAndProgressDialog extends JDialog implements ActionListener, Runnable, ClusterProgressListener
{
	private final static int					MIN_N_GENES_REQUIRING_MONITORING		= 500;
	private final static Map<Integer, int[]>	CALIBRATION								= new TreeMap<Integer, int[]>();

	
	static
	{
		CALIBRATION.put( 250, new int[]{ 0, 1 });				// 0 secs, 1 sec
		CALIBRATION.put( 500, new int[]{ 0, 4 });				// 0 secs, 4 secs
		CALIBRATION.put( 750, new int[]{ 2, 23 });				// 2 secs, 23 secs
		CALIBRATION.put(1000, new int[]{ 4, 33 });				// 4 secs, 33 secs
		CALIBRATION.put(2000, new int[]{ 13, 6*60 + 44 });		// 13 secs, 6'44"
	}
	
	
	private Map<Gene, Vector<float[]>>			geneToTxsZeroMean;
	private Metric								metric;
	private ClusterAlgorithm					algorithm;
	private ClusterProgressListener 			callbackListener;		// only gets notified when clustering completes.
	private boolean								forceNoGUI;				// if both false...
	private boolean								forceGUI;				// ... decide by heuristic
	private boolean								forceSynchronous;		// ditto. (synchronous & gui) is not allowed
	private boolean								forceAsynchronous;		// ditto. (synchronous & gui) is not allowed
	private boolean								gui;
	private boolean								synchronous;
	private Vector<JProgressBar>				progressBars;
	private JButton								cancelBtn;
	private HalfArrayDistanceMatrixBuilder		matrixBuilder;
	private DistanceMatrix<Node<Gene>> 			matrix;
	private boolean								buildingMatrix;
	private boolean								buildingTree;
	private TreeBuilder<Gene> 					treeBuilder;
	private Node<Gene>							result;

	
	// GUI always.
	public ClusterManagerAndProgressDialog(Map<Gene, Vector<float[]>> geneToTxsZeroMean, 
										   Metric metric, 
										   ClusterAlgorithm algorithm)
	{	
		this.geneToTxsZeroMean = geneToTxsZeroMean;
		this.metric = metric;
		this.algorithm = algorithm;
		
		// Build/install components whether or not this dialog will be used.
		setResizable(false);
		setTitle("Cluster progress: " + geneToTxsZeroMean.size() + " genes");
		setLayout(new GridLayout(0, 1));
		
		// Progress bar labels.
		Vector<JLabel> labels = new Vector<JLabel>();
		labels.add(new JLabel("Compute distance matrix", SwingConstants.RIGHT));
		for (String phaseName: algorithm.getPhaseNames())
			labels.add(new JLabel(phaseName, SwingConstants.RIGHT));
		int maxW = -1;
		for (JLabel label: labels)
			maxW = Math.max(maxW, label.getPreferredSize().width);
		Dimension pref = new Dimension(maxW, labels.firstElement().getPreferredSize().height);
		for (JLabel label: labels)
			label.setPreferredSize(pref);
		
		// Progress bars.
		progressBars = new Vector<JProgressBar>();
		matrixBuilder = new HalfArrayDistanceMatrixBuilder(metric, geneToTxsZeroMean);
		JProgressBar matrixProgressBar = new JProgressBar(0, matrixBuilder.getNTotalSteps());
		progressBars.add(matrixProgressBar);
		int[] nStepsByPhase = algorithm.getNStepsPerPhase(geneToTxsZeroMean.size());
		for (int nSteps: nStepsByPhase)
		{
			JProgressBar bar = new JProgressBar(0, nSteps);
			progressBars.add(bar);
		}
		
		// Label & bar strips.
		assert labels.size() == progressBars.size();
		for (int i=0; i<labels.size(); i++)
		{
			JPanel strip = new JPanel();
			strip.add(labels.get(i));
			strip.add(progressBars.get(i));
			add(strip);
		}
		
		// Controls.
		JPanel controls = new JPanel();
		cancelBtn = new JButton("Cancel");
		cancelBtn.addActionListener(this);
		controls.add(cancelBtn);
		add(controls);
	}
	
	
	private boolean heuristicSaysGUI()
	{
		return geneToTxsZeroMean.size() >= MIN_N_GENES_REQUIRING_MONITORING;
	}
	
	
	private boolean heuristicSaysAsynchronous()
	{
		return geneToTxsZeroMean.size() >= MIN_N_GENES_REQUIRING_MONITORING;
	}
	
	
	private boolean heuristicSaysSynchronous()
	{
		return !heuristicSaysAsynchronous();
	}
	
	
	//
	// If synchronous, returns the tree after (hopefully) a short amount of time. If synchronous,
	// immediately returns null and will call back when finished.
	//
	public Node<Gene> cluster() throws IllegalStateException
	{ 
		assert !(forceGUI && forceNoGUI);
		assert !(forceSynchronous && forceAsynchronous);
		if (forceGUI && forceSynchronous)
			throw new IllegalStateException("Can't force GUI and synchronous");
		
		// Determine whether or not to show GUI, and synch/asynch.
		gui = (forceGUI || forceNoGUI)  ?  forceGUI  :  heuristicSaysGUI();
		synchronous = (forceSynchronous || forceAsynchronous)  ?  forceSynchronous  :  heuristicSaysSynchronous();
		if (gui)
			assert !forceNoGUI;
		if (synchronous)
			assert !forceAsynchronous;
				
		// Synchronous. Run immediately. Caller is responsible for not bogging down the gui thread.
		if (synchronous)
		{
			assert !gui;
			matrixBuilder = new HalfArrayDistanceMatrixBuilder(metric, geneToTxsZeroMean);
			matrix = matrixBuilder.buildDistanceMatrix();
			TreeBuilder<Gene> treeBuilder = buildTreeBuilder(matrix);
			result = treeBuilder.buildTree();
			return result;
		}		
		
		// Asynchronous: 	
		new Thread(this).start();
		return null;
	}
	
	
	public void run()
	{
		assert !forceSynchronous;
		assert forceAsynchronous  ||  heuristicSaysAsynchronous();
		
		// No GUI.
		if (!gui)
		{
			// Build distance matrix & cluster, without displaying this dialog.
			matrixBuilder = new HalfArrayDistanceMatrixBuilder(metric, geneToTxsZeroMean);
			matrix = matrixBuilder.buildDistanceMatrix();
			TreeBuilder<Gene> treeBuilder = buildTreeBuilder(matrix);
			treeBuilder.buildTree();
			if (callbackListener != null)	
				callbackListener.clusteringFinished(null);
			return;
		}
		
		// GUI.
		else
		{
			// Display this dialog. Require the matrix builder and tree builder to report progress which
			// will be displayed in the progress bars.
			assert forceGUI  ||  heuristicSaysGUI();
			assert !forceNoGUI;
			pack();
			setVisible(true);
			buildingMatrix = true;
			HalfArrayDistanceMatrixBuilder matrixBuilder = new HalfArrayDistanceMatrixBuilder(metric, geneToTxsZeroMean);
			matrixBuilder.setReportingInterval(50);
			matrixBuilder.addClusterProgressListener(this);
			matrix = matrixBuilder.buildDistanceMatrix();	// takes a long time; periodic callbacks will update gui
			buildingMatrix = false;
			buildingTree = true;
			TreeBuilder<Gene> treeBuilder = buildTreeBuilder(matrix);
			treeBuilder.setReportingInterval(5);
			treeBuilder.addClusterProgressListener(this);
			result = treeBuilder.buildTree();			// takes a long time; periodic callbacks will update gui
			buildingTree = false;
			if (callbackListener != null)	
				callbackListener.clusteringFinished(null);
		}
	}
	
	
	private TreeBuilder<Gene> buildTreeBuilder(DistanceMatrix<Node<Gene>> matrix)
	{
		switch (algorithm)
		{
			case NJ:
				return new NeighborJoiningTreeBuilder<Gene>(matrix);
				
			default:
				assert false;
				return null;
		}
	}
	
	
	public void actionPerformed(ActionEvent e)
	{
		if (e.getSource() == cancelBtn)
		{
			if (matrixBuilder != null)
			{
				matrixBuilder.requestAbort();
				matrixBuilder = null;
			}
			if (treeBuilder != null)
			{
				treeBuilder.requestAbort();
				treeBuilder = null;
			}
			setVisible(false);
		}
	}
	
	
	public Node<Gene> getTree()
	{
		assert !buildingMatrix;
		assert !buildingTree;
		
		return result;
	}


	public void phaseProgressed(ClusterProgressEvent e) 
	{
		int progressBarIndex = buildingMatrix  ?  0  :  e.getPhaseIndex() + 1;
		int[] completionStats = e.getCompletion();		// { completed units, total units }
		progressBars.get(progressBarIndex).setValue(completionStats[0]);
	}


	public void phaseFinished(ClusterProgressEvent e) 		
	{
		int progressBarIndex = buildingMatrix  ?  0  :  e.getPhaseIndex() + 1;
		JProgressBar bar = progressBars.get(progressBarIndex);
		bar.setValue(bar.getMaximum());
		
		if (buildingMatrix)
		{
			buildingMatrix = false;
			buildingTree = true;
		}
	}
	
	
	public void setForceGui()						
	{ 
		forceGUI = true; 
		forceNoGUI = false;
	}
	
	
	public void setForceNoGui()									
	{ 
		forceNoGUI = true; 
		forceGUI = false;
	}
	
	
	public void setForceSynchronous()							
	{
		forceSynchronous = true; 
		forceAsynchronous = false;
	}
	
	
	public void setForceAsynchronous()							
	{ 
		forceAsynchronous = true; 
		forceSynchronous = false;
	}
	
	
	public void setTerminationCallbackListener(ClusterProgressListener callbackListener)
	{
		this.callbackListener = callbackListener;
	}
	
	
	public DistanceMatrix<Node<Gene>> getDistanceMatrix()
	{
		return matrix;
	}
	

	public void clusteringStarted(ClusterProgressEvent e) 		{ }
	public void phaseStarted(ClusterProgressEvent e) 			{ }
	public void clusteringFinished(ClusterProgressEvent e) 		{ }
	static void sop(Object x)									{ System.out.println(x); }
	
	
	// For testing gui, simulates the dexter main frame.
	private class Tester extends ClusterProgressAdapter
	{		
		public void clusteringFinished(ClusterProgressEvent e) 		
		{
			sop("Tester: clustering finished, root = " + result);
		}
	}

	
	public ClusterProgressListener buildCallbackTester()
	{
		return new Tester();
	}
	
	
	private static Map<Gene, Vector<float[]>> buildTestData(int nGenes) throws IOException, ClassNotFoundException
	{		
		File serfile = new File("data/Sessions/CPT.dex");
		SessionModel session = new SessionModel(serfile);
		Map<Gene, Vector<float[]>> geneToTxsZeroMean = new HashMap<Gene, Vector<float[]>>();
		for (Study study: session.getStudies())
		{
			if (study.isExperimentsStudy())
				continue;
			TimeAssignmentMap timeAssignments = session.getTimeAssignmentMapForStudy(study);
			for (Gene gene: study)
			{
				Vector<float[]> timepointsForGene = gene.getTimeAndExpressionPairs(timeAssignments);
				float geneMean = LocalMath.getMeanExpression(timepointsForGene);
				Vector<float[]> normalizedTimepoints = new Vector<float[]>(timepointsForGene.size());
				for (float[] tx: timepointsForGene)
				{
					float[] txNorm = new float[] { tx[0], tx[1] - geneMean };
					normalizedTimepoints.add(txNorm);
				}
				geneToTxsZeroMean.put(gene, normalizedTimepoints);
				if (geneToTxsZeroMean.size() > nGenes)
					return geneToTxsZeroMean;
			}	
		}
		assert false;
		return null;
	}
	
	
	public static void main(String[] args)
	{
		try
		{
			// Run this dialog.
			ClusterManagerAndProgressDialog that = 
				new ClusterManagerAndProgressDialog(buildTestData(1000),
													Metric.EUCLIDEAN, 
													ClusterAlgorithm.NJ);
			that.setTerminationCallbackListener(that.buildCallbackTester());
			//that.setForceAsynchronous();
			//that.setForceGui();
			that.cluster();
			Node<Gene> root = that.getTree();
			sop("DONE\nroot = " + root);
		}
		catch (Exception x)
		{
			sop("STRESS: " + x.getMessage());
			x.printStackTrace(System.out);
			System.exit(1);
		}
	}
}
