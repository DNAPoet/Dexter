package dexter.view.graph;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.*;
import java.io.*;
import dexter.*;
import dexter.interpolate.*;
import dexter.model.*;
import dexter.util.LocalMath;
import dexter.util.gui.*;
import dexter.view.graph.experiment.*;


//
// Implements the mouse interfaces but doesn't do anything with events. Subclasses can
// override handlers that matter.
//
// Expression values are passed in via the geneToTimeAndExpression map. Values are
// calculated externally; this class doesn't have access to a TimeAssignmentMap, and
// shouldn't have to.
//
// The geneToTimeAndExpression map must have consistent traversal order ... don't use a hash map.
//
// The geneToVisibleName map provides a mechanism for graphs with legends to override genes'
// given names. This works fine e.g. for experiments that combine multiple studies, so "nifH"
// should be called "Croco.nifH" or something like that. The mechanism doesn't work with the
// normalization wizard, which can show a single gene multiple times.
//


public class Graph extends JPanel implements MouseListener, MouseMotionListener, VisualConstants
{	
	private static Map<Study, Color>			studyToColor;
	private static Map<Organism, Color>			organismToColor;

	protected SessionModel						session;		// null is ok at wizard time
	protected GraphBackgroundModel				backgroundModel;
	protected ColorScheme						colorScheme;
	protected InterpolationAlgorithm			interpolationAlgorithm;
	protected Map<Gene, Vector<float[]>>		geneToTimeAndExpression;
	protected Map<Gene, GeneDisplayStyle>		geneToDisplayStyle;
	protected float								hPixPerHour;
	protected MarginModel						margin;
	protected Map<Gene, String>					geneToVisibleName;
	protected ColorMap							geneToColor;
	protected ColorMap							geneToColorByGene;
	protected Rectangle							graphBounds;
	protected Paintable							backgroundPainter;
	protected float								vPixPerExprUnit;
	protected int								dotRadius;		// if >0, paint data points as dots
	protected TransformStack					xformStack;
	protected Experiment						experiment;		// null unless this is an experiment graph
	protected GenesLegend						legend;			// often null;
	protected History							history;		// read-only
	protected boolean							normalizeToMeans;
	
	
	


	
	
	
						
						
						////////////////////////////////////////////////////////
						//                                                    //
						//                    CONSTRUCTION                    //
						//                                                    //
						////////////////////////////////////////////////////////
						

	
	
	

	
	
	// Copies model data from source graph. Caller supplies scale-specific arguments.
	Graph(Graph src, SessionModel session, float hPixPerHour, int graphHeight, MarginModel margin)
	{
		this(session, 
			 src.backgroundModel, 
			 src.geneToTimeAndExpression, 
			 hPixPerHour, 
			 graphHeight, 
			 margin,
			 src.colorScheme,
			 src.geneToColor,
			 src.geneToColorByGene);
	}
	
	
	public Graph(GraphBackgroundModel backgroundModel, 
			     Map<Gene, Vector<float[]>> geneToTimeAndExpression,
			     float hPixPerHour,
			     int graphHeight,
			     MarginModel margin)							// null margin is ok
	{
		this(null, backgroundModel, geneToTimeAndExpression, hPixPerHour, graphHeight, margin);
	}
	
	
	public Graph(SessionModel session,
				 GraphBackgroundModel backgroundModel, 
				 Map<Gene, Vector<float[]>> geneToTimeAndExpression,
				 float hPixPerHour,
				 int graphHeight,
				 MarginModel margin)							// null margin is ok
	{				
		this(session, backgroundModel, geneToTimeAndExpression, hPixPerHour, graphHeight, margin, null, null, null);
	}
		
		
	public Graph(SessionModel session,							// null session is ok
			 GraphBackgroundModel backgroundModel, 
			 Map<Gene, Vector<float[]>> geneToTimeAndExpression,
			 float hPixPerHour,
			 int graphHeight,
			 MarginModel margin,								// null margin is ok
			 ColorScheme colorScheme,
			 ColorMap geneToColor,
			 ColorMap geneToColorByGene)
	{				
		this.session = session;
		this.backgroundModel = backgroundModel;
		this.geneToTimeAndExpression = geneToTimeAndExpression;
		this.hPixPerHour = hPixPerHour;
		if (margin == null)  
			margin = new MarginModel(0, 0, 0, 0);
		this.margin = margin;
		geneToDisplayStyle = new HashMap<Gene, GeneDisplayStyle>();
		
		geneToVisibleName = new HashMap<Gene, String>();
		for (Gene gene: geneToTimeAndExpression.keySet())
			geneToVisibleName.put(gene, gene.getBestAvailableName());		// name if present, else id
		
		// If none supplied by caller, build a default color map that cycles through the default color list.
		if (colorScheme == null)
		{
			this.geneToColorByGene = new ColorMap();
			this.enforceColorScheme(ColorScheme.Gene);
		}
		else
		{
			this.colorScheme = colorScheme;
			this.geneToColor = geneToColor;
			this.geneToColorByGene = geneToColorByGene;
		}
		
		int graphWidth = (int)Math.ceil(backgroundModel.getDuration() * hPixPerHour);
		setPreferredSize(margin.fitAround(graphWidth, graphHeight));
		graphBounds = new Rectangle(margin.getLeft(), margin.getTop(), graphWidth, graphHeight);
		vPixPerExprUnit = graphHeight / 16f;
		
		// Create background painter if a background model is provided (all classes except sparkline graphs).
		boolean usesDL = backgroundModel.getUsesStyle(GraphBackgroundStyle.DL);
		boolean usesTreatment = backgroundModel.getUsesStyle(GraphBackgroundStyle.TREATMENT);
		if (usesDL && usesTreatment)  
			backgroundPainter = new DualStyleGraphBackgroundPainter(backgroundModel, graphBounds, hPixPerHour);
		else if (usesDL || usesTreatment)
		{
			GraphBackgroundStyle style = usesDL ? GraphBackgroundStyle.DL : GraphBackgroundStyle.TREATMENT;
			backgroundPainter = new GraphBackgroundPainter(style, backgroundModel, graphBounds, hPixPerHour);
		}
		
		xformStack = new TransformStack();
		
		// History.
		history = new History();
		history.add(new HistoryStep(geneToTimeAndExpression, true));
		
		interpolationAlgorithm = InterpolationAlgorithm.LINEAR;
	}

	
	
		
	
	
	
	
					
					////////////////////////////////////////////////////////
					//                                                    //
					//                      PAINTING                      //
					//                                                    //
					////////////////////////////////////////////////////////
				
	

	
	public void paintComponent(Graphics g)
	{
		Graphics2D g2 = (Graphics2D)g;
		
		if (backgroundPainter != null)
			backgroundPainter.paint(g2);
		
		xformStack.push(g2);
		translateToTopLeftOfMargin(g2);
		
		if (interpolationAlgorithm == InterpolationAlgorithm.CUBIC_SPLINE)
			paintSplineAtOrigin(g2);
		else
		{
			assert interpolationAlgorithm == InterpolationAlgorithm.LINEAR;
			paintLinesAtOrigin(g2);
		}
		
		if (dotRadius > 0)
			paintDotsAtOrigin(g2);
		
		xformStack.pop(g2);
	}
	
	
	protected void translateToTopLeftOfMargin(Graphics2D g2)
	{
		g2.translate(margin.getLeft(), margin.getTop());
	}
	
	
	protected void paintLinesAtOrigin(Graphics2D g2)
	{
		for (Gene gene: geneToTimeAndExpression.keySet())
		{
			g2.setColor(getColorForGene(gene));
			GeneDisplayStyle style = getDisplayStyleForGene(gene);
			if (style == GeneDisplayStyle.HIDDEN)
				continue;
			float geneMean = normalizeToMeans  ? LocalMath.getMeanExpression(geneToTimeAndExpression.get(gene))  :  0;
			Vector<float[]> points = geneToTimeAndExpression.get(gene);
			assert points != null  :  "null points for gene " + gene;
			float[] tx0 = points.firstElement();
			Point pFrom = null;
			if (!normalizeToMeans)
				pFrom = hrXprToPoint(tx0);
			else
			{
				pFrom = hrXprToPoint(tx0[0], tx0[1] - geneMean);
				pFrom.y -= graphBounds.height/2;
			}
			for (int i=1; i<points.size(); i++)
			{
				float[] tx = points.get(i);
				Point pTo = null;
				if (!normalizeToMeans)
					pTo = hrXprToPoint(tx);
				else
				{
					pTo = hrXprToPoint(tx[0], tx[1] - geneMean);
					pTo.y -= graphBounds.height/2;
				}
				g2.drawLine(pFrom.x, pFrom.y, pTo.x, pTo.y);
				if (style == GeneDisplayStyle.THICK)
				{
					g2.drawLine(pFrom.x, pFrom.y-1, pTo.x, pTo.y-1);
					g2.drawLine(pFrom.x, pFrom.y+1, pTo.x, pTo.y+1);					
				}
				pFrom = pTo;
			}
		}
	}
	
	
	protected void paintSplineAtOrigin(Graphics2D g2)
	{
		float[] startAndEndHours = getStartAndEndHours();
		float hoursPerHPix = 1f / hPixPerHour;
		for (Gene gene: geneToTimeAndExpression.keySet())
		{
			GeneDisplayStyle style = getDisplayStyleForGene(gene);
			if (style == GeneDisplayStyle.HIDDEN)
				continue;
			float geneMean = normalizeToMeans  ?  LocalMath.getMeanExpression(geneToTimeAndExpression.get(gene))  :  0;
			g2.setColor(getColorForGene(gene));
			Interpolater interpol = new CubicSplineInterpolater(geneToTimeAndExpression.get(gene));
			Point pFrom = null;
			Point pTo = null;
			for (float hour=startAndEndHours[0]; hour<=startAndEndHours[1]; hour+=hoursPerHPix)
			{
				pFrom = pTo;
				float xpr = interpol.interpolate(hour);
				if (normalizeToMeans)
					xpr = xpr - geneMean;					// if normalizing to mean, subtract mean from expression
				pTo = hrXprToPoint(hour, xpr);
				if (normalizeToMeans)
					pTo.y -= graphBounds.height/2;			// ... and display range is from -8 to +8
				if (pFrom == null)
					continue;
				paintLineForStyle(g2, pFrom, pTo, style);
			}
		}
	}
	
	
	protected void paintLineForStyle(Graphics2D g2, Point p1, Point p2, GeneDisplayStyle style)
	{
		if (style == GeneDisplayStyle.HIDDEN)
			return;
		
		g2.drawLine(p1.x, p1.y, p2.x, p2.y);
	
		if (style == GeneDisplayStyle.THICK)
		{
			g2.drawLine(p1.x, p1.y-1, p2.x, p2.y-1);
			g2.drawLine(p1.x, p1.y+1, p2.x, p2.y+1);					
		}
	}
	
	
	protected void paintDotsAtOrigin(Graphics2D g2)
	{
		for (Gene gene: geneToTimeAndExpression.keySet())
		{
			g2.setColor(getColorForGene(gene));
			for (float[] timeAndXpr: geneToTimeAndExpression.get(gene))
			{
				int x = hoursToHPix(timeAndXpr[0]) - dotRadius;
				int y = expressionToVPix(timeAndXpr[1]) - dotRadius;
				g2.fillOval(x, y, 2*dotRadius, 2*dotRadius);
			}
		}
	}
	
							
	
	
	
	
	
	
	
							/////////////////////////////////////////////////
							//                                             //
							//                    COLOR                    //
							//                                             //
							/////////////////////////////////////////////////
							
	

	

	public void setGeneToColorMap(Map<Gene, Color> geneToColor)
	{
		repaint();
	}
	
	
	public void setColorForGene(Color color, Gene gene)
	{
		geneToColor.put(gene, color);
	}
	
	
	protected Color getColorForGene(Gene gene)
	{
		return geneToColor.get(gene);
	}
	
	
	public Map<Study, Color> getStudyToColorMap()
	{
		if (studyToColor == null)
			studyToColor = session.getStudyToColorMap();
		
		return studyToColor;
	}
	
	
	// Usually there will be one study per organism and vice versa. In this case use the same
	// colors as used by the corresponding study.
	public Map<Organism, Color> getOrganismToColorMap()
	{
		if (organismToColor == null)
		{
			organismToColor = new HashMap<Organism, Color>();
			Map<Study, Color> studyToColor = getStudyToColorMap();
			Collection<Organism> organisms = session.getOrganisms();
			if (organisms.size() == studyToColor.size())
			{
				// 1-1 organism to study. Use corresponding colors from study color map.
				for (Study study: studyToColor.keySet())
					organismToColor.put(study.getOrganism(), studyToColor.get(study));
			}
			else
			{
				// Multiple studies for at least 1 organism. For now, keep it simple.
				int n = 0;
				for (Organism org: session.getOrganisms())
					organismToColor.put(org, DFLT_GENE_COLORS[n++ % DFLT_GENE_COLORS.length]);	
			}
		}
		
		return organismToColor;
	}
	
	
	public void enforceColorScheme()
	{
		enforceColorScheme(colorScheme);
	}
	
	
	// Enforces the existing color scheme, presumably because genes have been added/deleted. Sets local 
	// colorScheme variable. If this graph has a legend, applies colors to the legend.
	public void enforceColorScheme(ColorScheme colorScheme)
	{
		this.colorScheme = colorScheme;
		
		geneToColor = new ColorMap();
		
		switch (colorScheme)
		{		
			case Gene:
				// Under this scheme, a gene's color shouldn't change as the result of any user activity. For
				// example, a gene that is originally blue under this scheme should still be blue after genes
				// are added/deleted or the color scheme is changed and then changed back. This class maintains
				// a colormap containing the original color under this scheme for all genes.
				assert geneToColorByGene != null;
				for (Gene gene: geneToTimeAndExpression.keySet())
				{
					Color color = geneToColorByGene.get(gene);
					if (color == null)
					{
						// This gene has never been colored under this scheme. Generate a permanent
						// (for this scheme) color for it.
						int n = geneToColorByGene.size() % DFLT_GENE_COLORS.length;
						color = DFLT_GENE_COLORS[n];
						geneToColorByGene.put(gene, color);
					}
					geneToColor.put(gene, color);	
				}
				break;
				
			case Organism:
				getOrganismToColorMap();		// builds if needed, stores statically in organismToColor
				for (Gene gene: geneToTimeAndExpression.keySet())
				{
					Organism org = gene.getOrganism();
					geneToColor.put(gene, organismToColor.get(org));
				}
				break;
				
			case Study:
				getStudyToColorMap();		// builds if needed, stores statically in studyToColor
				for (Gene gene: geneToTimeAndExpression.keySet())
				{
					Study study = gene.getStudy();
					geneToColor.put(gene, studyToColor.get(study));
				}
				break;
				
			case Pathway:
			{
				Set<String> pathwaySorter = new TreeSet<String>();
				for (Gene gene: geneToTimeAndExpression.keySet())
				{
					String pathway = gene.getPathway();
					if (pathway != null)
						pathwaySorter.add(pathway);
				}
				Map<String, Color> pathwayToColor = new TreeMap<String, Color>();
				for (String pathway: pathwaySorter)
					pathwayToColor.put(pathway, DFLT_GENE_COLORS[pathwayToColor.size() % DFLT_GENE_COLORS.length]);
				for (Gene gene: geneToTimeAndExpression.keySet())
				{
					String pathway = gene.getPathway();
					Color color = (pathway != null)  ?  pathwayToColor.get(pathway)  :  Color.LIGHT_GRAY;
					geneToColor.put(gene, color);
				}
				break;
			}
			
			case Addition_order:
				for (Gene gene: geneToTimeAndExpression.keySet())
				{
					int stepNum = history.indexOfLastAdditionOfGene(gene);
					assert stepNum >= 0;
					Color color = DFLT_GENE_COLORS[stepNum % DFLT_GENE_COLORS.length];
					geneToColor.put(gene, color);
				}
				break;
				
			case All_blue:
			case All_red:
				Color color = (colorScheme == ColorScheme.All_blue)  ?  Color.BLUE  :  Color.RED;
				for (Gene gene: geneToTimeAndExpression.keySet())
					geneToColor.put(gene, color);
				break;
				
			case BlueRed:
				Map<String, Gene> idToGene = new TreeMap<String, Gene>();
				for (Gene gene: geneToTimeAndExpression.keySet())
					idToGene.put(gene.getId(), gene);
				for (String id: idToGene.keySet())
				{
					color = geneToColor.size() <= idToGene.size()/2  ?  Color.blue  :  Color.red;
					geneToColor.put(idToGene.get(id), color);
				}
				break;
			
			default:
				assert false;
		}
		
		// Legend.
		if (legend != null)
			legend.recolor();
		
		repaint();
	}
	
	
	public ColorMap getColorMap()
	{
		return geneToColor;
	}

	
	

					
	
	
	
	
					
					/////////////////////////////////////////////////////////////
					//                                                         //
					//                      CONFIGURATION                      //
					//                                                         //
					/////////////////////////////////////////////////////////////



	
	public void setDotRadius(int r)
	{
		dotRadius = r;
		repaint();
	}
	
	
	public void setGeneToTimeAndExpressionMap(Map<Gene, Vector<float[]>> geneToTimeAndExpression)
	{
		this.geneToTimeAndExpression = geneToTimeAndExpression;
		repaint();
	}
	
	
	protected String getVisibleNameForGene(Gene gene)
	{
		return geneToVisibleName.get(gene);
	}
	
	
	public void setVisibleNameForGene(Gene gene, String visName)
	{
		geneToVisibleName.put(gene, visName);
	}
	
	
	public Map<Gene, String> getGeneToVisibleNameMap()
	{
		return geneToVisibleName;
	}
	
	
	// Normal, thick, or hidden.
	private GeneDisplayStyle getDisplayStyleForGene(Gene gene)
	{
		GeneDisplayStyle style = geneToDisplayStyle.get(gene);
		return (style != null)  ?  style  :  GeneDisplayStyle.NORMAL;
	}
	

	// Normal, thick, or hidden.
	void setDisplayStyleForGene(Gene gene, GeneDisplayStyle style)
	{
		geneToDisplayStyle.put(gene, style);
		repaint();
	}
	

	
			
	
	
			
					////////////////////////////////////////////////////////////////////
					//                                                                //
					//                       EXPERIMENT SUPPORT                       //
					//                                                                //
					////////////////////////////////////////////////////////////////////
		
			
	

	// Does nothing if this already contains the gene. Rebuilds the color map to accommodate the new gene. Repaints.
	public void addGeneAndDataNonRedundant(Gene addMe, Vector<float[]> timeAndExpressionPairs)
	{
		if (geneToTimeAndExpression.containsKey(addMe))
			return;											// already contains the gene
		
		// Add entry to geneToTimeAndExpression map. Key order in this map affects order of appearance in
		// legend, so rebuild the map, preserving existing order and appending new entry to end.
		Map<Gene, Vector<float[]>> newTxMap = new LinkedHashMap<Gene, Vector<float[]>>(geneToTimeAndExpression);
		newTxMap.put(addMe, timeAndExpressionPairs);
		geneToTimeAndExpression = newTxMap;
		enforceColorScheme();
		
		// If this graph has a legend, add a checkbox for the new gene.
		if (legend != null)
			legend.addCheckBoxForGene(addMe, true);
		
		repaint();
	}
	
	
	public Experiment getExperiment()
	{
		return experiment;
	}
	
	
	public void setExperiment(Experiment experiment)
	{
		this.experiment = experiment;
	}
	
	
	public boolean isExperiment()
	{
		return experiment != null;
	}
	
	
	// Removing means removing from geneToTimeAndExpression map. Since the map might be shared, the
	// gene might have already been removed. Container probabaly needs to be revalidated.
	public void removeGene(Gene gene)
	{
		geneToTimeAndExpression.remove(gene);
		if (legend != null)
			legend.removeCheckBoxForGene(gene);		
		repaint();
	}
	
	
	public void removeGenes(Collection<Gene> genes)
	{
		for (Gene gene: genes)
			removeGene(gene);
	}

	
							
	
	
	
	
	
							
							/////////////////////////////////////////////////////////////
							//                                                         //
							//                      MISC AND MAIN                      //
							//                                                         //
							/////////////////////////////////////////////////////////////


	
	
	public String genesToString()
	{
		String s = getClass().getName() + " contains genes: ";
		for (Gene gene: getGenes())
			s += gene.getBestAvailableName() + " ";
		return s;
	}
	
	
	protected float[] getStartAndEndHours()
	{
		float[] ret = new float[2];
		Vector<float[]> txVec = geneToTimeAndExpression.values().iterator().next();
		ret[0] = txVec.firstElement()[0];
		ret[1] = txVec.lastElement()[0];
		return ret;
	}
	
	
	// Useful for debugging. No guarantee regarding which genes are retained.
	public void setMaxGenes(int maxGenes)
	{
		assert maxGenes >= 0;
		
		while (geneToTimeAndExpression.size() > maxGenes)
		{
			Gene gene = geneToTimeAndExpression.keySet().iterator().next();
			geneToTimeAndExpression.remove(gene);
		}
	}
	
	
	public void setBackgroundPainter(GraphBackgroundPainter backgroundPainter)
	{
		this.backgroundPainter = backgroundPainter;
	}
	
	
	static Map<Gene, Vector<float[]>> deserializeSmallTestGeneToPointsMap()
	{
		return deserializeTestGeneToPointsMap("ser/nifhGeneToPoints.ser");
	}
	
	
	static Map<Gene, Vector<float[]>> deserializeLargeTestGeneToPointsMap()
	{
		return deserializeTestGeneToPointsMap("ser/ABCGenesToPoints.ser");
	}
	
	
	static Map<Gene, Vector<float[]>> deserializeTestGeneToPointsMap(String pathname)
	{
		try
		{
			FileInputStream fis = new FileInputStream(new File(pathname));
			ObjectInputStream ois = new ObjectInputStream(fis);
			Map<Gene, Vector<float[]>> geneToPoints = (Map<Gene, Vector<float[]>>)ois.readObject();
			ois.close();
			fis.close();
			return geneToPoints;
		}
		catch (Exception x)
		{
			return null;
		}
	}
	
	
	public Map<Gene, Vector<float[]>> getGeneToTimeAndExpressionMap()
	{
		return geneToTimeAndExpression;
	}
	
	
	public Vector<float[]> getTimeAndExpressionPairs(Gene gene)
	{
		assert geneToTimeAndExpression.containsKey(gene);
		return geneToTimeAndExpression.get(gene);
	}

	
	// Null session is ok during wizard time.
	public SessionModel getSession()
	{
		return session;
	}
	
	
	public InterpolationAlgorithm getInterpolationAlgorithm()
	{
		return interpolationAlgorithm;
	}
	
	
	public void setInterpolationAlgorithm(InterpolationAlgorithm algo)
	{
		if (this.interpolationAlgorithm == algo)
			return;
		
		this.interpolationAlgorithm = algo;
		repaint();
	}
	
	
	protected Point hrXprToPoint(float hours, float expression)
	{
		return new Point(hoursToHPix(hours), expressionToVPix(expression));
	}
	
	
	void normalizeToMeans(boolean doNormalize)
	{
		this.normalizeToMeans = doNormalize;
		repaint();
	}

	
	// Assumes translation to top-left corner of graph, y increasing down.
	protected int expressionToVPix(float xpr)			
	{ 
		return graphBounds.height - Math.round(xpr * vPixPerExprUnit); 
	}
	
	
	protected Point hrXprToPoint(float[] hx)			{ return new Point(hoursToHPix(hx[0]), expressionToVPix(hx[1])); }
	protected int hoursToHPix(float hours)				{ return Math.round(hours * hPixPerHour); }	
	public Vector<Gene> getGenes()						{ return new Vector<Gene>(geneToTimeAndExpression.keySet()); }
	public GenesLegend getLegend()						{ return legend; }
	public void setLegend(GenesLegend legend)			{ this.legend = legend; }
	public History getHistory()							{ return history; }
	public boolean containsGene(Gene gene)				{ return geneToTimeAndExpression.containsKey(gene); }
	public MarginModel getMarginModel()					{ return margin; }
	public GraphBackgroundModel getBackgroundModel()	{ return backgroundModel; }
	public Rectangle getGraphBounds()					{ return graphBounds; }
	static void sop(Object x)							{ System.out.println(x); }
	

	public void mouseMoved(MouseEvent arg0)		{ }
	public void mousePressed(MouseEvent arg0) 	{ }
	public void mouseDragged(MouseEvent arg0) 	{ }
	public void mouseReleased(MouseEvent arg0) 	{ }
	public void mouseClicked(MouseEvent arg0) 	{ }
	public void mouseEntered(MouseEvent arg0) 	{ }
	public void mouseExited(MouseEvent arg0) 	{ }
	
	
	public static void main(String[] args)
	{
		MainDexterFrame.main(args);
	}
}
