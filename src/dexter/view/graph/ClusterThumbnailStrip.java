package dexter.view.graph;

import java.awt.event.*;
import java.util.*;

import javax.swing.*;

import dexter.MainDexterFrame;
import dexter.model.*;
import dexter.cluster.*;


public class ClusterThumbnailStrip extends InvariantGroupingThumbnailStrip implements ActionListener
{
	private static int				nextSN;
	
	private int						sn = ++nextSN;
	private Node<Gene>				root;
	private JButton					deleteBtn;
	private RoleValueToGenesMap		originalRoleValueToGenesMap;
	
	
	ClusterThumbnailStrip(SessionModel session, 
						  Vector<Vector<Gene>> clusteredGenes, 
						  Node<Gene> root, 
						  MainDexterFrame mainFrame)
	{
		super(session);
		
		this.root = root;
		this.mainFrame = mainFrame;
		
		thumbnails = new Vector<ThumbnailGraph>();

		// Build thumbnail graphs. Also build originalRoleValueToGenesMap, to be used when removing restrictions.
		GraphBackgroundModel backgroundModel = session.getGraphBackgroundModel();
		int clusterNum = 1;
		originalRoleValueToGenesMap = new RoleValueToGenesMap();
		for (Vector<Gene> cluster: clusteredGenes)
		{
			Map<Gene, Vector<float[]>> geneToTXForThumbnail = new TreeMap<Gene, Vector<float[]>>();
			for (Gene gene: cluster)
			{
				TimeAssignmentMap timeAssignments = session.getTimeAssignmentMapForStudy(gene.getStudy());
				assert timeAssignments != null;
				Vector<float[]> timepointsForGene = gene.getTimeAndExpressionPairs(timeAssignments);
				geneToTXForThumbnail.put(gene, timepointsForGene);		
			}	
			String name = "Cluster " + clusterNum++;
			ThumbnailGraph thumbnail = new ThumbnailGraph(name, session, backgroundModel, geneToTXForThumbnail);
			thumbnail.setMouseArmsAndSelects(true);
			thumbnails.add(thumbnail);
			originalRoleValueToGenesMap.put(name, new TreeSet<Gene>(cluster));
		}
		init("Tree #" + sn, backgroundModel, null);		// null means thumbnails vector is already populated
	}
	

	protected RoleValueToGenesMap mapRoleValuesToGenesNoRestrictions()
	{
		return originalRoleValueToGenesMap;
	}

	
	protected void populateControlPanel(JPanel controlPan)
	{
		addSelectBtnToControlPanel(controlPan, false); 	// don't abbreviate "Select"
		
		deleteBtn = new JButton("X");
		shrinkButtonForContolPanel(deleteBtn);
		deleteBtn.addActionListener(this);
		controlPan.add(deleteBtn);
	}
	
	
	public void actionPerformed(ActionEvent e)
	{
		if (e.getSource() == deleteBtn)
		{
			String msg = "Really delete this tree?";
			int confirm = JOptionPane.showConfirmDialog(this, msg, "", JOptionPane.YES_NO_OPTION);
			if (confirm != JOptionPane.YES_OPTION)
				return;
			mainFrame.getMultiStripPanel().removeClusterStrip(this);
		}
	}	
	
	
	public static void main(String[] args)
	{
		try
		{
			MainDexterFrame.main(args);
		}
		catch (Exception x)
		{
			sop("Stress: " + x.getMessage());
			x.printStackTrace();
		}
	}
}
