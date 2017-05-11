package dexter.view.graph.experiment;

import java.awt.*;
import java.awt.geom.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.*;
import dexter.VisualConstants;
import dexter.model.*;
import dexter.util.gui.*;


public class GenesByDistancePanel extends AbstractGeneSelectionPanel implements VisualConstants
{
	private final static int			GRAPH_H_PIX						= 450;
	private final static int			GRAPH_W_PIX						= 233;
	private final static int[]			CALLOUT_H_LENGTHS				= { 40 };
	private final static MarginModel	MARGINS							= new MarginModel(190, 190, 140, 60);
	private final static Color			BEYOND_CUTOFF_WASH				= new Color(50, 50, 50, 200);
	private final static Font			GENE_FONT						= new Font("SansSerif", Font.ITALIC, 12);
	private final static Stroke			AXIS_STROKE						= new BasicStroke(2);
	private final static Font			AXIS_FONT						= new Font("SansSerif", Font.PLAIN, 14);
	private final static int			N_VERTICAL_AXIS_LABELS			=   4;
	private final static int			PIN_LENGTH_RIGHT_OF_RIGHT_AXIS	=  18;
	private final static int			PIN_GRID_SIZE					=  18;	
	private final static Font			N_SEL_GENES_FONT				= new Font("Serif", Font.PLAIN, 28);
	private final static Font			MORE_LESS_FONT					= new Font("Serif", Font.ITALIC, 22);
	private final static String[]		INSTRUX							=
	{
		"Lower/raise pin to", "tighten/relax proximity cutoff", "and accept fewer/more genes"
	};

	
	private LinkedHashMap<Gene, Float>  geneToDistanceSorted;
	private float 						maxDistance;
	private int							cutoffVPix;			
	private float 						distPerVPix;
	private HorizontalPinDraggable		pin;
	
	
	GenesByDistancePanel(Gene fromHere, Map<Gene, Float> geneToDistance, 
			             Map<Study, Color> studyToColor, int nVisibleGenes)
	{
		super(AddBy.Expression_Similarity);
		
		setStudyToColorMap(studyToColor);
		
		// Invert the distance map.
		Map<Float, TreeSet<Gene>> distanceToGenes = new TreeMap<Float, TreeSet<Gene>>();
		maxDistance = -12345f;
		for (Gene gene: geneToDistance.keySet())
		{
			if (gene == fromHere)
				continue;
			Float dist = geneToDistance.get(gene);
			maxDistance = Math.max(maxDistance, dist);
			TreeSet<Gene> genesForDistance = distanceToGenes.get(dist);
			if (genesForDistance == null)
			{
				genesForDistance = new TreeSet<Gene>();
				distanceToGenes.put(dist, genesForDistance);
			}
			genesForDistance.add(gene);
		}
		
		// Retain ~nVisibleGenes closest genes.
		geneToDistanceSorted = new LinkedHashMap<Gene, Float>();
		for (Float dist: distanceToGenes.keySet())
		{
			maxDistance = dist;
			TreeSet<Gene> genesForDistance = distanceToGenes.get(dist);
			for (Gene gene: genesForDistance)
				geneToDistanceSorted.put(gene, dist);
			if (geneToDistanceSorted.size() >= nVisibleGenes)
				break;
		}
		
		// Pixometry.
		distPerVPix = maxDistance / GRAPH_H_PIX;
		cutoffVPix = MARGINS.getTop();
		
		// Pin and its events.
		pin = new HorizontalPinDraggable("" + maxDistance, 
										 MARGINS.getLeft(), 
										 MARGINS.getLeft() + GRAPH_W_PIX + PIN_LENGTH_RIGHT_OF_RIGHT_AXIS, 
										 cutoffVPix + PIN_GRID_SIZE/2, 
										 PIN_GRID_SIZE, 
										 true);
		pin.setMinMaxYPix(MARGINS.getTop(), MARGINS.getTop() + GRAPH_H_PIX);
		MLis mlis = new MLis();
		addMouseListener(mlis);
		addMouseMotionListener(mlis);
		
		// Instructions.
		setLayout(new BorderLayout());
		JPanel instruxLabels = new JPanel(new GridLayout(0, 1));
		JLabel perdu = new JLabel("Perdu");
		Font font = new Font(perdu.getFont().getFamily(), Font.PLAIN, 20);
		for (String s: INSTRUX)
		{
			JPanel strip = new JPanel();
			JLabel label = new JLabel(s);
			label.setFont(font);
			strip.add(label);
			instruxLabels.add(strip);
		}
		add(instruxLabels, BorderLayout.NORTH);
		instruxLabels.setBorder(BorderFactory.createLineBorder(Color.BLACK));
	}
	
	
	public Dimension getPreferredSize()
	{
		return MARGINS.fitAround(new Dimension(GRAPH_W_PIX, GRAPH_H_PIX));
	}
	
	
	public void paintComponent(Graphics g)
	{
		// Clear.
		g.setColor(Color.WHITE);
		g.fillRect(0, 0, 3333, 2222);
		
		// Pixometry.
		int yOfHorizAxis = getHeight() - MARGINS.getBottom();
		int axisRight = MARGINS.getLeft() + GRAPH_W_PIX;		
		
		// Genes.
		g.setFont(GENE_FONT);
		int n = 0;
		for (Gene gene: geneToDistanceSorted.keySet())
		{
			if (gene.getStudy() != null)
				addRepresentedStudy(gene.getStudy());
			g.setColor(getColorForStudy(gene.getStudy()));
			float dist = geneToDistanceSorted.get(gene);
			int deltaYPix = Math.round(dist/distPerVPix);
			int y = yOfHorizAxis - deltaYPix;
			int calloutLength = CALLOUT_H_LENGTHS[n];
			int calloutRight = MARGINS.getLeft() + calloutLength;
			n = (n + 1) % CALLOUT_H_LENGTHS.length;
			g.drawLine(MARGINS.getLeft(), y, calloutRight, y);
			int baseline = y + 6;
			g.drawString(gene.getBestAvailableName(), calloutRight+2, baseline);
		}
		
		// Cutoff.
		int yCutoffCorrected = Math.min(cutoffVPix, yOfHorizAxis-2);
		yCutoffCorrected = Math.max(yCutoffCorrected, MARGINS.getTop());
		int washHeight = yCutoffCorrected - MARGINS.getTop() + 1;
		washHeight = Math.min(washHeight, GRAPH_H_PIX);
		if (washHeight > 0)
		{
			g.setColor(BEYOND_CUTOFF_WASH);		
			g.fillRect(MARGINS.getLeft(), MARGINS.getTop(), GRAPH_W_PIX, washHeight);
		}
		
		// Axes.
		Graphics2D g2 = (Graphics2D)g;
		g.setColor(Color.BLACK);
		Stroke cachedStroke = g2.getStroke();
		g2.setStroke(AXIS_STROKE);
		g.drawLine(MARGINS.getLeft(), yOfHorizAxis, axisRight, yOfHorizAxis);				// horizontal
		g.drawLine(MARGINS.getLeft(), yOfHorizAxis, MARGINS.getLeft(), MARGINS.getTop());	// left vertical
		g.drawLine(axisRight, yOfHorizAxis, axisRight, MARGINS.getTop());					// right vertical
		g2.setStroke(cachedStroke);
		
		// Axis labels.
		Stack<Integer> ysVertAxisTicks = new Stack<Integer>();
		Stack<String> vertAxisLabels = new Stack<String>();
		float deltaDist = maxDistance / (N_VERTICAL_AXIS_LABELS - 1);
		float deltaPix = GRAPH_H_PIX / (float)(N_VERTICAL_AXIS_LABELS - 1);
		float dist = 0;
		int yPix = yOfHorizAxis - 1;
		while (ysVertAxisTicks.size() < N_VERTICAL_AXIS_LABELS)
		{
			ysVertAxisTicks.add(yPix);
			yPix = (Math.round(yPix - deltaPix));
			vertAxisLabels.add("" + dist);
			dist += deltaDist;
		}
		ysVertAxisTicks.pop();			// correct for possible roundoff
		ysVertAxisTicks.add(MARGINS.getTop());
		g.setFont(AXIS_FONT);
		while (!ysVertAxisTicks.isEmpty())
		{
			yPix = ysVertAxisTicks.pop();
			g.drawLine(MARGINS.getLeft(), yPix, MARGINS.getLeft()-10, yPix);
			String s = vertAxisLabels.pop();
			int sw = g.getFontMetrics().stringWidth(s);
			int xText = MARGINS.getLeft() - 13 - sw;
			g.drawString(s, xText, yPix+5);
		}
		
		// Pin.
		pin.paint(g);
		
		// # selected genes.
		g.setColor(Color.BLACK);
		g.setFont(N_SEL_GENES_FONT);
		int nSel = getSelectedGenes().size();
		String s = nSel + " gene";
		if (nSel != 1)
			s += "s";
		s += " selected";
		int sw = g.getFontMetrics().stringWidth(s);
		int graphCenterX = MARGINS.getLeft() + GRAPH_W_PIX/2;
		int xText = graphCenterX - sw/2;
		int baseline = MARGINS.getTop() + GRAPH_H_PIX + 38;
		g.drawString(s, xText, baseline);
		
		// Color-by-study legend.
		int x = axisRight + 15;
		int y = MARGINS.getTop() + 45;
		paintLegend(g, x, y);
		
		// Vertical "more ... less" text, and "distance->" label.
		AffineTransform entryXform = g2.getTransform();
		g2.translate(30, yOfHorizAxis);
		g2.rotate(1.5*Math.PI);
		g2.setColor(Color.BLACK);
		g2.setFont(MORE_LESS_FONT);
		s = LEFT_ARROW + " More Similar";
		g2.drawString(s, 0, 0);
		s = "Less Similar " + RIGHT_ARROW;
		sw = g2.getFontMetrics().stringWidth(s);
		g2.drawString(s, GRAPH_H_PIX-sw, 0);
		g2.translate(0, axisRight);
		s = "Distance " + RIGHT_ARROW;
		g2.drawString(s, 0, 0);
		g2.setTransform(entryXform);
	}
	
	
	private class MLis extends MouseAdapter
	{
		public void mouseMoved(MouseEvent e)
		{			
			if (pin.contains(e))
				pin.arm();
			else
				pin.disarm();
			repaint();
		}
		
		public void mousePressed(MouseEvent e)
		{			
			mouseMoved(e);				// catch up
			if (pin.isArmed())
			{
				pin.startDrag(e);
				repaint();
			}
		}
		
		public void mouseDragged(MouseEvent e)
		{			
			if (pin.isDragging())
			{
				pin.drag(e);
				cutoffVPix = pin.getYPix();
				pin.setTitle("" + vPixToDistance(cutoffVPix));
				repaint();
			}
		}
		
		public void mouseReleased(MouseEvent e)
		{
			mouseDragged(e);			// catch up
			if (pin.isDragging())
			{
				pin.stopDrag();
				mouseMoved(e);			// disarms if mouse no longer in handle
				repaint();
			}
		}
	}  // End of inner class MLis
	
	
	private float vPixToDistance(int vPix)
	{
		int toTopPix = vPix - MARGINS.getTop();
		float toTop = toTopPix * distPerVPix;
		return maxDistance - toTop;
	}
	
	
	Vector<Gene> getSelectedGenes()
	{
		Vector<Gene> ret = new Vector<Gene>();
		
		float cutoffDist = vPixToDistance(cutoffVPix);
		for (Gene gene: geneToDistanceSorted.keySet())
		{
			float dist = geneToDistanceSorted.get(gene);
			if (dist <= cutoffDist)
				ret.add(gene);
		}
		
		return ret;
	}
	
	
	boolean supportsSelectAll()
	{
		return false;
	}
	
	
	boolean supportsDeselectAll()
	{
		return false;
	}
		
	
	public static void main(String[] args)
	{
		try
		{
			sop("STARTING");
			
			// Build 300 genes with distances clustered around 20, 40, and 60.
			Vector<DebugGene> genes = DebugGene.buildTestCases(300);
			Map<Gene, Float> geneToDistance = new HashMap<Gene, Float>();
			int n = 0;
			Random rand = new Random(0L);
			for (Gene gene: genes)
			{
				n++;
				double sdev = rand.nextGaussian();
				sdev *= 10;
				double mean = 20;
				if (n > 100)
					mean = 40;
				if (n > 200)
					mean = 60;
				double dist = mean + sdev;
				dist = Math.max(dist, 1);
				dist = Math.min(dist, 80);
				geneToDistance.put(gene, (float)dist);
			}
			
			GenesByDistancePanel that = new GenesByDistancePanel(null, geneToDistance, null, 50);
			JFrame frame = new JFrame();
			frame.add(that);
			frame.pack();
			frame.setVisible(true);
		}
		catch (Exception x)
		{
			sop("STRESS: " + x.getMessage());
			x.printStackTrace(System.out);
		}
		finally
		{
			sop("DONE");
		}
	}
}
