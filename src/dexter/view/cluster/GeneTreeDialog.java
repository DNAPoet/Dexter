package dexter.view.cluster;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.*;
import dexter.model.*;
import dexter.util.gui.*;
import dexter.cluster.DistanceMatrix;
import dexter.cluster.Node;
import dexter.view.graph.GraphBackgroundModel;


public class GeneTreeDialog extends TreeDialog<Gene> implements ItemListener
{
	private Map<Study, Color> 					studyToColor;
	private TaggedButtonGroup<ColorBy> 			colorByBtnGrp;
	
	
	public GeneTreeDialog(Node<Gene> root, Map<Study, Color> studyToColor)
	{
		super(root, new GeneTreePanel(root, TREE_WIDTH_PIX));
		
		this.studyToColor = studyToColor;
		
		getGeneTreePanel().setStudyToColorMap(studyToColor);
		
		colorByBtnGrp = new TaggedButtonGroup<ColorBy>(ColorBy.values());
		JPanel colorByPan = colorByBtnGrp.buildPanel(this);
		addToBottomFlowPanel(colorByPan, 0);
		addToBottomFlowPanel(new JLabel("Color by"), 0);
	}
	
	
	private GeneTreePanel getGeneTreePanel()
	{
		return (GeneTreePanel)treePan;
	}
	
	
	public void itemStateChanged(ItemEvent e)
	{
		if (!colorByBtnGrp.getRadios().contains(e.getSource()))
		{
			super.itemStateChanged(e);
			return;
		}
		
		ColorBy colorBy = colorByBtnGrp.getSelectedTag();
		getGeneTreePanel().setColorBy(colorBy);
	}
	
	
	protected TreePanel<Gene> buildNewTreePanelForRerooting(Node<Gene> newRoot)
	{
		GeneTreePanel ret = new GeneTreePanel(newRoot, TREE_WIDTH_PIX);
		ret.setStudyToColorMap(studyToColor);
		ret.setColorBy(getGeneTreePanel().getColorBy());
		return ret;
	}
	
	
	public void setShowSparklines(Map<Gene, Vector<float[]>> geneToTxs, 
								  GraphBackgroundModel backgroundModel)
	{
		getGeneTreePanel().setShowNodeGraphs(geneToTxs, backgroundModel);
	}
	
	
	public static void main(String[] args)
	{
		dexter.MainDexterFrame.main(args);
	}
}
