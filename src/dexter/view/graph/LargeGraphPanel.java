package dexter.view.graph;

import java.awt.*;
import javax.swing.*;
import java.util.*;
import dexter.MainDexterFrame;
import dexter.model.*;
import dexter.util.gui.*;
import dexter.event.*;


//
// Aggregates a graph and a legend. Always contained by a LargeGraphDialog (or subclass).
//


public class LargeGraphPanel extends JPanel implements LegendListener<Gene>
{
	private final static int					GRAPH_TO_LEGEND_H_GAP	=  35;
	private final static int					LEGEND_TO_RIGHT_EDGE	=  35;
	
	private LargeGraphDialog					parentDialog;
	private LargeGraph							graph;
	private GenesLegend							legend;
	private JScrollPane							legendSpane;
	private boolean								hideSelectedGenes;
	
	
	public LargeGraphPanel(LargeGraphDialog parentDialog, Graph source)
	{
		assert source.getSession() != null  :  "null session model in LargeGraphPanel ctor";
		
		this.parentDialog = parentDialog;
		
		setOpaque(true);
		setBackground(Color.WHITE);
		
		setLayout(new Lom());
		
		// Graph.
		graph = new LargeGraph(source);
		add(graph);
		
		// Legend.
		legend = new GenesLegend(source.getGenes(), source.getGeneToVisibleNameMap(), graph);
		legend.setDoubleBuffered(true);		// TODO: need this?
		legend.addLegendListener(this);
		graph.setLegend(legend);
		legendSpane = new JScrollPane(legend, 
									  JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, 
									  JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		int spaneW = legend.getPreferredSize().width + 10;
		spaneW = Math.max(spaneW, 110);
		int graphVisibleHeight = 
			graph.getPreferredSize().height - graph.getMarginModel().getTop() - graph.getMarginModel().getBottom();
		legendSpane.setPreferredSize(new Dimension(spaneW, graphVisibleHeight));
		
		add(legendSpane);
	}
	
	
	private class Lom extends LayoutAdapter
	{		
		public Dimension preferredLayoutSize(Container parent)
	    {
	    	Dimension graphPref = graph.getPreferredSize();
	    	int prefH = Math.max(graphPref.height, legendSpane.getPreferredSize().height);
	    	int prefW = 
	    		graphPref.width + 
	    		GRAPH_TO_LEGEND_H_GAP + 
	    		legend.getPreferredSize().width + 
	    		LEGEND_TO_RIGHT_EDGE;
	    	return new Dimension(prefW, prefH);
	    }
	    
	    public void layoutContainer(Container parent)    
	    {
	    	// Graph.
	    	Dimension graphPref = graph.getPreferredSize();
	    	graph.setSize(graphPref);
	    	graph.setLocation(0, 0);					// graph's margin takes care of insetting correctly
	    	
	    	// Legend in scrollpane.
	    	Dimension spanePref = legendSpane.getPreferredSize();
	    	legendSpane.setSize(spanePref);
	    	int x = graphPref.width + GRAPH_TO_LEGEND_H_GAP;	
	    	int y = graph.getMarginModel().getTop();
	    	legendSpane.setLocation(x, y);
	    }
	}  // End of inner class Lom
	
	
	// Repaints.
	public void legendStateChanged(LegendEvent<Gene> e)
	{
		Gene gene = e.getTag();
		graph.setDisplayStyleForGene(gene, getDisplayStyleForGene(gene, e.isArmed(), e.isSelected()));
		parentDialog.enableButtonsAfterSelectionChange();
	}
	
	
	// Display styles are NORMAL, THICK, and HIDDEN.
	private GeneDisplayStyle getDisplayStyleForGene(Gene gene, boolean armed, boolean selected)
	{
		GeneDisplayStyle ret = GeneDisplayStyle.NORMAL;
		if (hideSelectedGenes  &&  !selected)
			ret = GeneDisplayStyle.HIDDEN;
		else if (armed)
			ret = GeneDisplayStyle.THICK;
		return ret;
	}
	
	
	// Repaints graph.
	void setHideUnselectedGenes(boolean hide)
	{
		this.hideSelectedGenes = hide;
		
		Gene armedGene = legend.getArmedGene();
		Set<Gene> selectedGenes = new HashSet<Gene>(getSelectedGenes());
		for (Gene gene: graph.getGenes())
		{
			boolean armed = gene == armedGene;
			boolean selected = selectedGenes.contains(gene);
			GeneDisplayStyle style = getDisplayStyleForGene(gene, armed, selected);
			graph.setDisplayStyleForGene(gene, style);		// repaints
		}
	}
	
	
	public LargeGraph getGraph()
	{
		return graph;
	}
	
	
	public GenesLegend getLegend()
	{
		return legend;
	}
	
	
	public Vector<Gene> getSelectedGenes()
	{
		return legend.getSelectedGenes();
	}
	
	
	public Vector<Gene> getGenes()
	{
		return legend.getGenes();
	}
	
	
	// Doesn't post events.
	void selectAll(boolean selected)
	{
		legend.selectAll(selected);
	}
	
	
	// Doesn't post events.
	void invertSelections()
	{
		legend.invertSelections();
	}
	
	
	// Proximity can be analyzed if at least 1 organism is represented by at least 2 genes. This is
	// equivalent to #genes > #organisms.
	public boolean canAnalyzeProximity()
	{
		Set<Organism> organisms = new HashSet<Organism>();
		Collection<Gene> selGenes = getSelectedGenes();
		for (Gene gene: selGenes)
			organisms.add(gene.getOrganism());
		return selGenes.size() > organisms.size();
	}
	
	
	public SessionModel getSession()
	{
		return graph.getSession();
	}
	
	
	// Cubic spline can paint outside its bounds.
	public void paintComponent(Graphics g)
	{
		g.setColor(Color.WHITE);
		g.fillRect(0, 0, 2222, 2222);
	}
	
	
	public MainDexterFrame getMainFrame()
	{
		return parentDialog.getMainFrame();
	}
	
	
	static void sop(Object x)				{ System.out.println(x); }
	
	
	public static void main(String[] args)
	{
		dexter.MainDexterFrame.main(args);
	}
}
