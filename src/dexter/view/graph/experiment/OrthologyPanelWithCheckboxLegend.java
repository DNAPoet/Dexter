package dexter.view.graph.experiment;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import java.util.*;

import dexter.event.LegendEvent;
import dexter.event.LegendListener;
import dexter.model.*;
import dexter.ortholog.OrthologyPanel;
import dexter.util.gui.*;
import dexter.VisualConstants;


//
// Used by GenesByOrthologyPane. Too big for an inner class.
//


class OrthologyPanelWithCheckboxLegend extends JPanel 
	implements VisualConstants, LegendListener<LightweightGene>, ItemListener
{
	private final static Font				CHECKMARK_FONT 	= new Font("Serif", Font.PLAIN, 18);
	
	private MouseAwareOrthologyPanel		orthoPan;
	private LightweightGene					mousedGene;
	private Vector<VariableBorderColorTaggedCheckBox<LightweightGene>>
											cboxes;
	private VariableBorderCheckboxManager<LightweightGene>
											cboxManager;
	private Collection<VariableBorderColorTaggedCheckBox<LightweightGene>> 
											referenceCboxes;

	
	OrthologyPanelWithCheckboxLegend(HashSet<GeneRelationship> edges, 
									 int margin, 
									 int graphRadius, 
									 int dotRadius,
									 VariableBorderCheckboxManager<LightweightGene> cboxManager,
									 Map<Organism, Color> colorMap)
	{
		this(edges, margin, graphRadius, dotRadius, cboxManager, colorMap, new HashSet<Gene>());
	}
	
		
	// Reference genes are genes that were selected when the user requested add-by-orthology in an
	// experiment panel. Since they are present and selected in the experiment, they should be
	// permanently selected in this panel.
	OrthologyPanelWithCheckboxLegend(HashSet<GeneRelationship> edges, 
									 int margin, 
									 int graphRadius, 
									 int dotRadius,
									 VariableBorderCheckboxManager<LightweightGene> cboxManager,
									 Map<Organism, Color> colorMap,
									 Collection<Gene> referenceGenes)
	{
		this.cboxManager = cboxManager;
		cboxManager.addLegendListener(this);
		
		assert colorMap != null  :  "null color map";
		
		// Orthology graph panel.
		orthoPan = new MouseAwareOrthologyPanel(edges, margin, graphRadius, dotRadius);
		orthoPan.setOrganismToColorMap(colorMap);
		add(orthoPan);
		
		// Legend.
		Set<LightweightGene> genes = new TreeSet<LightweightGene>();
		for (GeneRelationship edge: edges)
		{
			genes.add(edge.from);
			genes.add(edge.to);
		}
		cboxes = new Vector<VariableBorderColorTaggedCheckBox<LightweightGene>>();
		referenceCboxes = new HashSet<VariableBorderColorTaggedCheckBox<LightweightGene>>();
		VerticalFlowLayout lom = new VerticalFlowLayout();
		lom.setVerticalAlignment(Component.CENTER_ALIGNMENT);
		JPanel legend = new JPanel(lom);
		for (LightweightGene lwg: genes)
		{
			Organism org = lwg.getOrganism();
			Color geneColor = colorMap.get(org);
			VariableBorderColorTaggedCheckBox<LightweightGene> cbox = 
				new VariableBorderColorTaggedCheckBox<LightweightGene>(lwg, SELECTION_COLOR, CLEAR_COLOR);
			boolean geneIsReference = false;
			for (Gene refGene: referenceGenes)
			{
				if (refGene.getId().equals(lwg.getId()))
				{
					geneIsReference = true;
					break;
				}
			}
			if (geneIsReference)
			{
				cbox.setEnabled(false);
				cbox.setSelected(true);
				referenceCboxes.add(cbox);
			}
			cbox.setCheckboxForeground(geneColor);
			cbox.setOpaque(false);
			cboxManager.manage(cbox);
			cboxes.add(cbox);
			legend.add(cbox);
			cbox.addItemListener(this);
		}
		JScrollPane spane = new JScrollPane(legend, 
											JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
											JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		spane.setPreferredSize(new Dimension(spane.getPreferredSize().width, orthoPan.getPreferredSize().height));
		add(spane);
	}
	
	
	public void legendStateChanged(LegendEvent<LightweightGene> e)
	{
		if (e.isArmed() && e.isSelected())
		{
			if (e.getTag() != mousedGene)
			{
				mousedGene = e.getTag();
				orthoPan.repaint();
			}
		}
		else
		{
			if (mousedGene != null)
			{
				mousedGene = null;
				orthoPan.repaint();
			}
		}
	}
	
	
	public void itemStateChanged(ItemEvent e)
	{
		orthoPan.repaint();
	}
	
	
	void addLegendListener(LegendListener ll)
	{
		cboxManager.addLegendListener(ll);
	}
	
	
	private class MouseAwareOrthologyPanel extends OrthologyPanel implements MouseListener, MouseMotionListener
	{	
		private Set<LegendListener<LightweightGene>>	legendListeners;
		private Map<LightweightGene, Rectangle> 		geneToDotBounds;
		
		public MouseAwareOrthologyPanel(Collection<GeneRelationship> edges, int margin, int radius, int dotRadius) 
		{
			super(edges, true, margin, radius, dotRadius);
			Map<LightweightGene, Point> geneToLocation = getAbsoluteGeneLocations();
			geneToDotBounds = new HashMap<LightweightGene, Rectangle>();
			int dotR = getDotRadius();
			for (LightweightGene gene: geneToLocation.keySet())
			{
				Point p = geneToLocation.get(gene);
				Rectangle r = new Rectangle(p.x-dotR-1, p.y-dotR-1, 2*dotR+2, 2*dotR+2);
				geneToDotBounds.put(gene, r);
			}
			addMouseListener(this);
			addMouseMotionListener(this);
		}

		public void mouseMoved(MouseEvent e) 
		{
			mousedGene = mouseEventToGene(e);
			repaint();
			cboxManager.setHighlightedTag(mousedGene);
		}

		public void mouseClicked(MouseEvent e)
		{
			LightweightGene clickedGene = mouseEventToGene(e);
			if (clickedGene != null)
				cboxManager.invertSelectionForTag(clickedGene);
		}
		
		private LightweightGene mouseEventToGene(MouseEvent e)
		{
			int x = e.getX();
			int y = e.getY();
			for (LightweightGene gene: geneToDotBounds.keySet())
				if (geneToDotBounds.get(gene).contains(x, y))
					return gene;
			return null;
		}
		
		public void paintComponent(Graphics g)
		{
			super.paintComponent(g);

			if (mousedGene != null)
			{
				g.setColor(SELECTION_COLOR);
				Rectangle r = geneToDotBounds.get(mousedGene);
				if (r != null)
				{
					g.drawRect(r.x, r.y, r.width, r.height);
				}
			}
			
			g.setColor(Color.BLACK);
			g.setFont(CHECKMARK_FONT);
			for (LightweightGene checkedGene: cboxManager.getSelectedTags())
			{
				Rectangle r = geneToDotBounds.get(checkedGene);
				if (r != null)
				{
					g.drawString(""+CHECKMARK, r.x+2, r.y+9);
				}
			}
		}

		public void mouseDragged(MouseEvent e)	{ }	
		public void mousePressed(MouseEvent e)	{ }	
		public void mouseReleased(MouseEvent e)	{ }	
		public void mouseEntered(MouseEvent e)	{ }	
		public void mouseExited(MouseEvent e)	{ }		
	}  // end of inner class MouseAwareOrthologyPanel

	
	static void sop(Object x)			{ System.out.println(x); }
	
	
	public static void main(String[] args)
	{
		GenesByOrthologyPanel.main(args);
	}
}
