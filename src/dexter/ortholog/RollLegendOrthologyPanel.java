package dexter.ortholog;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import dexter.model.*;


public class RollLegendOrthologyPanel extends OrthologyPanel implements MouseMotionListener
{	
	private final static int 							TEXT_V_DELTA 			=  15;	

	private int											prefWidthExtension;
	private LightweightGene								armedGene;
	private Map<LightweightGene, Point>					geneToLocationInGraph;
	private Map<Organism, Vector<LightweightGene>>		organismToGenes;
	private Map<LightweightGene, Rectangle>				geneToLabelBounds;
	
	
	// prefWidthExtension provides space for a legend.
	RollLegendOrthologyPanel(Collection<GeneRelationship> edges, int prefWidthExtension)
	{
		super(edges, false);		// false => not thumbnail, i.e. blowup
		
		this.prefWidthExtension = prefWidthExtension;
		addMouseMotionListener(this);
	}
	
	
	// Called by superclass at construction time.
	public void setEdges(Collection<GeneRelationship> edges, Map<Organism, Color> colormap)
	{
		super.setEdges(edges, colormap);
		
		geneToLocationInGraph = getAbsoluteGeneLocations();
		
		// Map each organism to its genes.
		organismToGenes = new TreeMap<Organism, Vector<LightweightGene>>();
		for (LightweightGene gene: geneToLocationInGraph.keySet())
		{
			Vector<LightweightGene> genesForOrg = organismToGenes.get(gene.getOrganism());
			if (genesForOrg == null)
			{
				genesForOrg = new Vector<LightweightGene>();
				organismToGenes.put(gene.getOrganism(), genesForOrg);
			}
			genesForOrg.add(gene);
		}
	}
	
	
	public Dimension getPreferredSize()
	{
		Dimension superPref = super.getPreferredSize();
		return new Dimension(superPref.width + prefWidthExtension, superPref.height);
	}
	
	
	public void paintComponent(Graphics g)
	{
		super.paintComponent(g);
		
		boolean needLabelBounds = false;
		if (geneToLabelBounds == null)
		{
			needLabelBounds = true;
			geneToLabelBounds = new HashMap<LightweightGene, Rectangle>();
		}
		
		// Paint gene id legends. If first time, collect legend bounds.
		g.setFont(OrthologyPanel.getLabelFont());
		FontMetrics fm = g.getFontMetrics();
		int xOrgName = super.getPreferredSize().width + 25;
		int xGeneId = xOrgName + 22;
		int baseline = 20;
		for (Organism org: organismToGenes.keySet())
		{
			Vector<LightweightGene> genesForOrg = organismToGenes.get(org);
			if (genesForOrg.isEmpty())
				continue;
			g.setColor(getOrganismColorMap().get(org));
			g.drawString(org.toString(), xOrgName, baseline);
			baseline += TEXT_V_DELTA;
			for (LightweightGene gene: geneToLocationInGraph.keySet())
			{
				if (gene.getOrganism() != org)
					continue;
				g.drawString(gene.getId(), xGeneId, baseline);
				if (needLabelBounds)
				{
					int sw = fm.stringWidth(gene.getId());
					int sh = TEXT_V_DELTA - 3;
					Rectangle labelBounds = new Rectangle(xGeneId-2, baseline-sh, sw+5, TEXT_V_DELTA+2);
					geneToLabelBounds.put(gene, labelBounds);
				}
				baseline += TEXT_V_DELTA;
			}
			baseline += TEXT_V_DELTA;
		}
		
		// Highlight armed gene.
		if (armedGene != null)
		{
			g.setColor(Color.DARK_GRAY);
			((Graphics2D)g).draw(geneToLabelBounds.get(armedGene));
			Point p = geneToLocationInGraph.get(armedGene);
			assert p != null  :  "No location in graph for " + armedGene;
			g.drawRect(p.x-7, p.y-7, 14, 14);
		}
		
		// Framing.
		g.setColor(Color.BLACK);
		Dimension pref = getPreferredSize();
		Dimension superPref = super.getPreferredSize();
		g.drawRect(0, 1, pref.width-1, pref.height-2);
		g.drawRect(1, 2, pref.width-3, pref.height-4);
		g.drawLine(superPref.width+1, 1, superPref.width+1, 2222);
		g.drawLine(superPref.width+2, 1, superPref.width+2, 2222);
	}

	public void mouseMoved(MouseEvent e)
	{
		// Check for mouse in a label.
		LightweightGene nextArmedGene = null;
		int x = e.getX();
		int y = e.getY();
		for (LightweightGene gene: geneToLabelBounds.keySet())
		{
			if (geneToLabelBounds.get(gene).contains(x, y))
			{
				nextArmedGene = gene;
				break;
			}
		}
		
		// Check for mouse in a dot.
		if (nextArmedGene == null)
		{
			int dotR = getDotRadius();
			for (LightweightGene gene: geneToLocationInGraph.keySet())
			{
				Point p = geneToLocationInGraph.get(gene);
				Rectangle r = new Rectangle(p.x-dotR, p.y-dotR, 2*dotR, 2*dotR);
				if (r.contains(x, y))
				{
					nextArmedGene = gene;
					break;
				}
			}
		}
		
		armedGene = nextArmedGene;
		repaint();
	}
	
	public void mouseDragged(MouseEvent e)		{ }
}
