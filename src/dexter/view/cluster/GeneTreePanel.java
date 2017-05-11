package dexter.view.cluster;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;
import dexter.cluster.*;
import dexter.model.*;
import dexter.util.gui.*;
import dexter.view.graph.*;


class GeneTreePanel extends TreePanel<Gene>
{	
	private final static int					SUMMARY_H_PIX_PER_HR			=   7;
	private final static int					SUMMARY_GRAPH_HEIGHT			= 175;
	
	private ColorBy 							colorBy = ColorBy.getDefault();
	private Map<Study, Color> 					studyToColor;
	private Map<Node<Gene>, Color>				nodeToColorForColorByStudy;
	private Glass								summaryGlassPane;
	private Map<Node<Gene>, HashSet<Gene>> 		nodeToLeaves;
	private GraphBackgroundModel				graphBackgroundModel;
	private Map<Gene, Vector<float[]>>			geneToTxs;
	private Dimension							summaryGraphPreferredSize;
	
	
	GeneTreePanel(Node<Gene> root, int graphWidthPix)
	{
		super(root, graphWidthPix);
	}
	
	
	// Mandatory.
	public void setStudyToColorMap(Map<Study, Color> studyToColor)
	{
		this.studyToColor = studyToColor;
		
		nodeToColorForColorByStudy = new HashMap<Node<Gene>, Color>();
		recurseMapNodeToColorByStudy(getRoot());
	}
	
	
	private Color recurseMapNodeToColorByStudy(Node<Gene> node)
	{
		if (node.isLeaf())
		{
			Gene gene = node.getPayload();
			assert gene != null;
			Color color = studyToColor.get(gene.getStudy());
			nodeToColorForColorByStudy.put(node, color);
			return color;
		}
		
		// Intermediate node. If all kids have same color, use that color, else black.
		else
		{
			Set<Color> colors = new HashSet<Color>();
			for (Node<Gene> kid: node.getKids())
			{
				Color kidColor = recurseMapNodeToColorByStudy(kid);
				colors.add(kidColor);
			}
			Color color = (colors.size() == 1)  ?  colors.iterator().next()  :  Color.BLACK;
			nodeToColorForColorByStudy.put(node, color);
			return color;
		}
	}
	
	
	public void setColorBy(ColorBy colorBy)
	{
		this.colorBy = colorBy;
		repaint();
	}
	
	
	ColorBy getColorBy()
	{
		return colorBy;
	}

	
	protected Color getColorForNode(Node node)
	{
		switch (colorBy)
		{
			case Subtree:
				return super.getColorForNode(node);
				
			case Organism:
				return nodeToColorForColorByStudy.get(node);
				
			default:
				assert false;
				return null;
		}
	}
	
	
	public void setShowNodeGraphs(Map<Gene, Vector<float[]>> geneToTxs, 
								  GraphBackgroundModel graphBackgroundModel)
	{
		this.geneToTxs = geneToTxs;
		this.graphBackgroundModel = graphBackgroundModel;
			
		// Install glass pane which will display summary graphs as mouse rolls over checkboxes.
		summaryGlassPane = new Glass();
		getDialog().setGlassPane(summaryGlassPane);
		summaryGlassPane.setVisible(true);
		
		// Collect leaf children for all nodes.
		nodeToLeaves = collectLeafChildrenOfAllNodes();
		
		// Listener will detect when mouse enters/exits any checkbox.
		BoxMouseListener bmlis = new BoxMouseListener();
		for (TaggedCheckBox<Node<Gene>> cbox: collectCheckBoxes())
			cbox.addMouseListener(bmlis);
	}
	
	
	private Map<Node<Gene>, HashSet<Gene>> collectLeafChildrenOfAllNodes()
	{
		Map<Node<Gene>, HashSet<Gene>> ret = new HashMap<Node<Gene>, HashSet<Gene>>();
		recurseCollectLeafChildrenOfAllNodes(getRoot(), ret);
		return ret;
	}
	
	
	private void recurseCollectLeafChildrenOfAllNodes(Node<Gene> subroot, Map<Node<Gene>, HashSet<Gene>> map)
	{
		HashSet<Gene> leafKids = new HashSet<Gene>();
		map.put(subroot, leafKids);
		
		if (subroot.isLeaf())
			leafKids.add(subroot.getPayload());
		else
		{
			for (Node<Gene> kid: subroot.getKids())
			{
				recurseCollectLeafChildrenOfAllNodes(kid, map);
				leafKids.addAll(map.get(kid));
			}
		}
	}
	
	
	private class BoxMouseListener extends MouseAdapter
	{
		public void mouseEntered(MouseEvent e)
		{		
			TaggedCheckBox<Node<Gene>> cbox = (TaggedCheckBox<Node<Gene>>)e.getSource();
			Node<Gene> node = cbox.getTag();
			Collection<Gene> genes = nodeToLeaves.get(node);
			Map<Gene, Vector<float[]>> rolledGeneToTxs = new HashMap<Gene, Vector<float[]>>();
			for (Gene gene: genes)
				rolledGeneToTxs.put(gene, geneToTxs.get(gene));
			SummaryGraph summary = new SummaryGraph(rolledGeneToTxs);
			summary.setLocation(getBestSummaryLocation(node));
			summaryGlassPane.add(summary);
			summaryGlassPane.repaint();  
		}
		
		public void mouseExited(MouseEvent e)
		{
			if (!e.isControlDown())
			{
				summaryGlassPane.removeAll();
				summaryGlassPane.repaint(); 
			}
		}
	}  // End of inner class BoxMouseListener
	
	
	private class Glass extends JComponent
	{
		Glass()
		{
			setLayout(null);
			setOpaque(false);
		}
		
		public void paintComponent(Graphics g) 
		{
			Component[] kidCompos = getComponents();
			if (kidCompos.length == 0)
				return;
			SummaryGraph graph = (SummaryGraph)kidCompos[0];
		}
	}  // End of inner class Glass
	
	
	
	private class SummaryGraph extends Graph
	{	
		SummaryGraph(Map<Gene, Vector<float[]>> rolledGeneToTxs)
		{	
			super(null,							// session 
				  graphBackgroundModel, 
				  rolledGeneToTxs,
				  SUMMARY_H_PIX_PER_HR,
				  SUMMARY_GRAPH_HEIGHT,
				  null,							// margin
				  null,							// color scheme
				  new ColorMap(Color.RED),		// gene to color
				  new ColorMap(Color.BLUE));	// gene to color for coloring by gene
			if (summaryGraphPreferredSize == null)
				summaryGraphPreferredSize = getPreferredSize();
			setSize(summaryGraphPreferredSize);
			setOpaque(false);
		}
		
		public void paintComponent(Graphics g)
		{
			super.paintComponent(g);
			g.setColor(Color.BLACK);
			g.drawRect(0, 0, getWidth()-2, getHeight()-2);
			g.drawRect(1, 1, getWidth()-4, getHeight()-4);
		}
	}  // End of inner class SummaryGraph
	
	
	private Point getBestSummaryLocation(Node<Gene> node)
	{
		Point loc = new Point(getNodeLocation(node));
		
		// Assume this tree panel is in a scrollpane. Get the viewport.
		JViewport viewport = (JViewport)getParent();
		Point viewPosition = viewport.getViewPosition();
		Dimension viewSize = viewport.getSize();
		
		// Horizontal preference: to the right of the checkbox, where nodes are more sparse.
		int prefX = loc.x + 15;
		if (prefX + summaryGraphPreferredSize.width + 10 < viewSize.width)
			loc.x = prefX;
		else
			loc.x -= summaryGraphPreferredSize.width + 15;
		if (loc.x < 0)
			loc.x = 0;
		
		// Adjust for vertical scrolling.
		loc.y -= viewPosition.y;
		
		// Vertical preference: a little below the checkbox, so that text can be read if
		// node is a leaf.
		loc.y += 20;
		if (loc.y + summaryGraphPreferredSize.height > viewSize.height - 10)
			loc.y -= summaryGraphPreferredSize.height + 10;
		
		return loc;
	}
	
	
	private JDialog getDialog()
	{
		Component compo = this;
		while (!(compo instanceof JDialog))
			compo = compo.getParent();
		return (JDialog)compo;
	}
	
	
	public static void main(String[] args)
	{
		dexter.MainDexterFrame.main(args);
	}
}
