package dexter.ortholog;

import java.awt.*;
import java.awt.geom.*;
import javax.swing.*;
import java.io.*;
import java.util.*;
import dexter.model.*;
import dexter.util.gui.*;
import dexter.VisualConstants;


public class OrthologyPanel extends JPanel implements VisualConstants
{
	private final static int[]					DFLT_MARGINS				= { 15,  55 };
	private final static int[]					DFLT_RADII  				= { 50, 200 };
	private final static int[]					DFLT_DOT_RADII  			= {  3,   6 };
	private final static Stroke					THIN_STROKE					= new BasicStroke(1);
	private final static Stroke					THICK_STROKE				= new BasicStroke(2);
	private final static double					TWO_PI						= 2 * Math.PI;
	private final static double					ANGULAR_SPACING_FEW_GENES	= TWO_PI / 30;
	private final static Font					FONT						= new Font("SansSerif", Font.PLAIN, 13);
	private final static Vector<Color>			ORGANISM_COLORS;
	
	static
	{
		ORGANISM_COLORS = new Vector<Color>();
		for (Color c: DFLT_GENE_COLORS)
			if (c != Color.BLACK)
				ORGANISM_COLORS.add(c);
	}

	private Collection<GeneRelationship> 		edges;
	private Collection<GeneRelationship> 		bidirectionalEdges;
	private int									margin;
	private int									radius;
	private int									dotRadius;
	private boolean								emphasizeBidirectionalEdges;
	private boolean								showOrganismLabels;
	private Map<Organism, Color>				organismToColor;
	private Map<Organism, Double> 				organismToTheta;
	private Map<LightweightGene, Point>			geneToLocation;
	
	
	public OrthologyPanel(Collection<GeneRelationship> edges, boolean thumbnail)
	{
		this(edges, 
			 !thumbnail,								// thumbnails don't emphasize bidirectional
			 DFLT_MARGINS[thumbnail ? 0 : 1], 			// edges ... too small, and might be maximally
			 DFLT_RADII[thumbnail ? 0 : 1], 			// connected. Determining bidirectionals for a
			 DFLT_DOT_RADII[thumbnail ? 0 : 1]);		// maxcon graph is too slow.
	}
	
	
	public OrthologyPanel(Collection<GeneRelationship> edges, 
						  boolean emphasizeBidirectionalEdges,
						  int margin, 
						  int radius, 
						  int dotRadius)
	{
		this.edges = edges;
		this.emphasizeBidirectionalEdges = emphasizeBidirectionalEdges;
		this.margin = margin;
		this.radius = radius;
		this.dotRadius = dotRadius;
		
		setEdges(edges);		// overridden by subclasses
	}
	
	
	// Repaints. Overridden by subclasses. ? Should be protected?
	public void setEdges(Collection<GeneRelationship> edges)
	{
		setEdges(edges, null);
	}
	
	
	// Builds a color map if colormap arg is null. Repaints.
	public void setEdges(Collection<GeneRelationship> edges, Map<Organism, Color> colormap)
	{
		this.edges = edges;
		
		// Collect bidirectional edges.
		bidirectionalEdges = new HashSet<GeneRelationship>();
		if (emphasizeBidirectionalEdges)
		{
			for (GeneRelationship edge1: edges)
				for (GeneRelationship edge2: edges)
					if (edge1.isReverseOf(edge2))
						bidirectionalEdges.add(edge1);
		}
		
		// Map organisms to genes.
		Map<Organism, TreeSet<LightweightGene>> organismToGenes = 
			new TreeMap<Organism, TreeSet<LightweightGene>>();
		for (GeneRelationship edge: edges)
		{
			for (LightweightGene gene: edge.toArray())
			{
				Organism org = gene.getOrganism();
				if (!organismToGenes.containsKey(org))
					organismToGenes.put(org, new TreeSet<LightweightGene>());
				organismToGenes.get(org).add(gene);
			}
		}
				
		// Build a colormap if not provided by caller.
		organismToColor = (colormap != null)  ?  colormap  :  buildColorMap(organismToGenes.keySet());
		
		// Map organisms to centerpoints on rim of invisible circle, starting at west.
		// Assume all painting is done with origin at center of circle.
		organismToTheta = new TreeMap<Organism, Double>();
		double theta = Math.PI;
		double deltaTheta = 2 * Math.PI / organismToGenes.size();
		for (Organism org: organismToGenes.keySet())
		{
			organismToTheta.put(org, theta);
			theta += deltaTheta;
			if (theta >= TWO_PI)
				theta -= TWO_PI;
		}
		
		// For each organism, distribute organism's genes around the centerpoint.
		geneToLocation = new TreeMap<LightweightGene, Point>();
		for (Organism org: organismToGenes.keySet())
		{
			Set<LightweightGene> genes = organismToGenes.get(org);
			int nGenes = genes.size();
			double angStep = ANGULAR_SPACING_FEW_GENES;
			double angularSpread = (nGenes - 1) * angStep;
			theta = organismToTheta.get(org) - angularSpread / 2;
			for (LightweightGene gene: genes)
			{
				double dx = radius * Math.cos(theta);
				double dy = radius * Math.sin(theta);
				Point p = new Point((int)Math.round(dx), (int)Math.round(dy));
				geneToLocation.put(gene, p);
				theta += angStep;
			}
		}
		
		repaint();
	}
	
	
	public Dimension getPreferredSize()
	{
		int d = 2 * (radius + margin);
		return new Dimension(d, d);
	}
	
	
	public static Dimension getPreferredSize(boolean thumbnail)
	{
		int n = thumbnail ? 0 : 1;
		int d = 2 * (DFLT_RADII[n] + DFLT_MARGINS[n]);
		return new Dimension(d, d);
	}
	
	
	// When # of organisms is > number of colors in the palette, make sure adjacent 
	// organisms aren't the same color.
	public static Map<Organism, Color> buildColorMap(Collection<Organism> organisms)
	{
		Vector<Color> trialColors = new Vector<Color>();
		for (int i=0; i<organisms.size(); i++)
			trialColors.add(ORGANISM_COLORS.get(i%ORGANISM_COLORS.size()));
		trialColors.add(ORGANISM_COLORS.firstElement());
		for (int i=0; i<organisms.size(); i++)
		{
			int j = (i + ORGANISM_COLORS.size()/2) % ORGANISM_COLORS.size();
			while (trialColors.get(i) == trialColors.get(i+1))
			{
				trialColors.set(i, ORGANISM_COLORS.get(j));
				j = ++j % ORGANISM_COLORS.size();
			}
		}
		
		Map<Organism, Color> ret = new TreeMap<Organism, Color>();
		int n = 0;
		for (Organism org: organisms)
			ret.put(org, trialColors.get(n++));
		
		return ret;
	}
	


	
	
	
				
				//////////////////////////////////////////////////////////////////
				//                                                              //
				//                            PAINTING                          //
				//                                                              //
				//////////////////////////////////////////////////////////////////

	
	
	
	public void paintComponent(Graphics g)
	{
		Graphics2D g2 = (Graphics2D)g;
		TransformStack xformStack = new TransformStack();
		
		// Clear.
		g2.setColor(Color.WHITE);
		g2.fillRect(0, 0, 3333, 2222);	
		
		// Translate to center of organism ring.
		xformStack.push(g2);
		g2.translate(margin+radius, margin+radius);	
		
		// Edges.
		for (GeneRelationship edge: edges)
		{
			if (emphasizeBidirectionalEdges  &&  bidirectionalEdges.contains(edge))
				g2.setStroke(THICK_STROKE);
			else
				g2.setStroke(THIN_STROKE);
			LightweightGene gene1 = edge.from;
			LightweightGene gene2 = edge.to;
			if (gene1.getOrganism().equals(gene2.getOrganism()))
				g.setColor(organismToColor.get(gene1.getOrganism()));
			else
				g.setColor(Color.BLACK);
			Point p1 = geneToLocation.get(gene1);
			Point p2 = geneToLocation.get(gene2);
			g.drawLine(p1.x, p1.y, p2.x, p2.y);
		}
		
		// Dots.
		for (LightweightGene gene: geneToLocation.keySet())
		{
			g2.setColor(organismToColor.get(gene.getOrganism()));
			Point p = geneToLocation.get(gene);
			g.fillOval(p.x-dotRadius, p.y-dotRadius, 2*dotRadius, 2*dotRadius);
		}
		
		// Labels.
		if (showOrganismLabels)
		{
			g2.setFont(FONT);
			FontMetrics fm = g2.getFontMetrics();
			int nOrganisms = organismToTheta.size();
			int n = 0;
			for (Organism org: organismToTheta.keySet())
			{
				xformStack.push(g2);
				g2.rotate(organismToTheta.get(org) + Math.PI/2);
				g2.translate(0, -(radius + 13));
				if (n > nOrganisms/2)
				{
					g2.rotate(Math.PI);
					g2.translate(0, 9);
				}
				g2.setColor(organismToColor.get(org));
				String s = org.toString();
				int sw = fm.stringWidth(s);
				g2.drawString(s, -sw/2, 0);
				xformStack.pop(g2);
				n++;
			}
		}
		
		xformStack.pop(g2);
	}
	
	

	
	
	

	
	
				
				
				/////////////////////////////////////////////////////////////////
				//                                                             //
				//                         MISC & MAIN                         //
				//                                                             //
				/////////////////////////////////////////////////////////////////



	
	public void setShowOrganismLabels(boolean b)
	{
		showOrganismLabels = b;
		repaint();
	}
	
	
	public void setColormap(Map<Organism, Color> cmap)
	{
		this.organismToColor = cmap;
		repaint();
	}
	
	
	protected Map<Organism, Color> getOrganismColorMap()
	{
		return organismToColor;
	}
	
	
	protected static Font getLabelFont()
	{
		return FONT;
	}
	
	
	protected Map<LightweightGene, Point> getAbsoluteGeneLocations()
	{
		Map<LightweightGene, Point> ret = new TreeMap<LightweightGene, Point>();
		for (LightweightGene g: geneToLocation.keySet())
		{
			Point rel = new Point(geneToLocation.get(g));
			rel.translate(margin+radius, margin+radius);
			ret.put(g, rel);
		}
		return ret;
	}
	
	
	protected int getDotRadius()
	{
		return dotRadius;
	}
	
	
	protected int getRingRadius()
	{
		return radius;
	}
	
	
	protected int getMargin()
	{
		return margin;
	}
	
	
	public void setOrganismToColorMap(Map<Organism, Color> organismToColor)
	{
		this.organismToColor = organismToColor;
		repaint();
	}
	
	
	static void sop(Object x)	{ System.out.println(x); }
}
