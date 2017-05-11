package dexter.proximity;

import java.awt.*;
import java.util.*;
import java.io.IOException;
import javax.swing.*;
import dexter.coreg.CoregulationGroup;
import dexter.model.*;
import dexter.view.graph.*;
import dexter.util.gui.*;


class SingleOrganismProximityPanel extends JPanel implements dexter.VisualConstants
{
	final static Color[]					COLOR_BY_DISTANCE 		=
	{
		Color.BLUE, Color.MAGENTA, Color.RED, Color.BLACK
	};
	
	final static int[]						PROXIMITY_DIST_TO_PIX 	=
	{
		50, 65, 80, 95
	};
	
	final static int[] 						STROKE_SIZES			=
	{
		2, 4, 6, 8
	};

	private final static int				Y_TOP_CBOX				=  66;
	private final static int				LEFT_EDGE_TO_SPARK		=  14;
	private final static int				SPARK_TO_CBOX			= -20;
	private final static int				CBOX_LEFT_TO_VERT_LINE	=  42;
	private final static int[]				CBOX_TO_RIGHT_EDGE		=  { 22, 62 }; // without/with operon marks
	private final static Color				BG						= VERY_LIGHT_GRAY;
	private final static Font				DISTANCE_FONT			= new Font("SansSerif", Font.PLAIN, 12);
	private final static Font				ELIPSIS_FONT			= new Font("Serif", Font.PLAIN, 25);
	private final static Color				COREG_COLOR				= NEUTRAL_BROWN;
	
	private ProximityDialog					dialog;
	private LargeGraphPanel 				srcPan;
	private Organism						organism;
	private OrganismProximityReport			report;
	private Vector<Gene> 					visibleGenes;
	private Vector<TaggedCheckBox<Gene>> 	cboxes;
	private Vector<SparklineGraph>			sparklines;
	private JLabel							organismLabel;
	private Vector<Integer>					cboxYs;
	private Set<Paintable>					linePaintables;
	private TransformStack					xformStack;
	private Map<Point, String>				distanceLabelBaselineCenterToSval;
	private Vector<OperonRange>				operonRanges;			// null if no predicted operons for these genes
	
	
	SingleOrganismProximityPanel(ProximityDialog dialog, 
								 LargeGraphPanel srcPan, 
								 Organism organism, 
								 OrganismProximityReport report) throws IOException
	{
		this.dialog = dialog;
		this.srcPan = srcPan;
		this.organism = organism;
		this.report = report;
		
		setLayout(new Lom());
		xformStack = new TransformStack();
		
		// Label.
		organismLabel = new JLabel("" + organism.getShortestName(), JLabel.CENTER);
		Font font = organismLabel.getFont();
		organismLabel.setFont(new Font(font.getFamily(), Font.PLAIN, 28));
		add(organismLabel);
		
		// Checkboxes and sparklines.
		cboxes = new Vector<TaggedCheckBox<Gene>>();
		sparklines = new Vector<SparklineGraph>();
		visibleGenes = report.getGenes();
		Vector<Integer> distances = report.getdDistancesNegMeansDifferentContig();
		assert visibleGenes.size() == distances.size() + 1;
		cboxYs = new Vector<Integer>();
		int y = Y_TOP_CBOX;
		for (int i=0; i<visibleGenes.size()-1; i++)
		{
			cboxYs.add(y);
			y += proximityDistToPix(Math.abs(distances.get(i)));
			Gene gene = visibleGenes.get(i);
			buildCboxAndSparkline(gene);
		}
		cboxYs.add(y);
		buildCboxAndSparkline(visibleGenes.lastElement());
		
		// If operon predictions are available, genes in a shared predicted operon are specially marked. For now, 
		// assume there's only 1 file. 
		SessionModel session = srcPan.getSession();
		if (session.hasCoregulationFor(organism))
		{
			// Load all predicted operons for this organism.
			Vector<CoregulationGroup> allPredictedOperons = 
				session.getCoregulationFiles().getCoregulationGroups(organism);
			// Retain operons that contain any displayed genes.
			Vector<CoregulationGroup> predictedOperonsForDisplayedGenes = new Vector<CoregulationGroup>();
			for (CoregulationGroup operon: allPredictedOperons)
			{
				for (Gene gene: visibleGenes)
				{
					if (operon.contains(gene))
					{
						predictedOperonsForDisplayedGenes.add(operon);
						break;
					}
				}
			}
			// Determine operon ranges.
			if (!predictedOperonsForDisplayedGenes.isEmpty())
			{
				operonRanges = new Vector<SingleOrganismProximityPanel.OperonRange>();
				for (CoregulationGroup operon: predictedOperonsForDisplayedGenes)
					operonRanges.add(new OperonRange(operon));
			}
		}
	}  // End of ctor
	
	
	// Returns null if gene with specified id is not visible. Not efficient but data is small.
	private Gene idToVisibleGene(String id)
	{
		for (Gene gene: visibleGenes)
			if (gene.getId().equals(id))
				return gene;
		return null;
	}
	
	
	private class OperonRange
	{
		CoregulationGroup	operon;
		Gene				firstVisibleGene;
		boolean				operonStartsBeforeFirstVisibleGene;
		Gene				lastVisibleGene;
		boolean				operonEndsAfterLastVisibleGene;
		
		OperonRange(CoregulationGroup operon)
		{
			this.operon = operon;
			
			// Find 1st visible gene in operon.
			for (int i=0; i<operon.size(); i++)
			{
				String id = operon.get(i);
				firstVisibleGene = idToVisibleGene(id);
				if (firstVisibleGene != null)
				{
					operonStartsBeforeFirstVisibleGene = (i > 0);
					break;
				}
			}
			
			// Find last visible gene in operon.
			for (int i=operon.size()-1; i>=0; i--)
			{
				String id = operon.get(i);
				lastVisibleGene = idToVisibleGene(id);
				if (lastVisibleGene != null)
				{
					operonEndsAfterLastVisibleGene = (i < operon.size()-1);
					break;
				}
			}
		}

		public String toString()
		{
			String s = "OperonRange: operon = " + operon + "\n";
			s += operonStartsBeforeFirstVisibleGene  ?  "..." : "[";
			s += firstVisibleGene.getId() + " - " + lastVisibleGene.getId() + " ";
			s += operonEndsAfterLastVisibleGene  ?  "..."  :  "]";
			return s;
		}
	}  // End of inner class OperonRange
	
	
	private String buildToolTipText(Gene gene)
	{
		return buildToolTipText(report.getContigPositionForGene(gene));
	}
	
	
	//  contigInfo = { position within contig, # genes in contig, contig #, # contigs }.
	private String buildToolTipText(int[] contigInfo)
	{
		String s = "Gene " + (contigInfo[0]+1) + " of " + contigInfo[1];
		if (contigInfo[3] > 1)
			s += " in contig " + (contigInfo[2]+1) + " of " + contigInfo[3];
		return s;
	}
	
	
	private void buildCboxAndSparkline(Gene gene)
	{
		TaggedCheckBox<Gene> cbox = buildCbox(gene);
		cbox.setOpaque(true);
		cbox.setBackground(BG);
		cbox.setSelected(true);
		cbox.setToolTipText(buildToolTipText(gene));
		cbox.addItemListener(dialog);
		cboxes.add(cbox);
		add(cbox);
		SparklineGraph spark = new SparklineGraph(gene, srcPan.getGraph());
		sparklines.add(spark);
		add(spark);
	}
	
	
	private TaggedCheckBox<Gene> buildCbox(Gene gene)
	{
		assert gene != null;
		assert gene.getBestAvailableName() != null;
		
		TaggedCheckBox<Gene> cbox = new TaggedCheckBox<Gene>(gene, gene.getBestAvailableName());
		cbox.setOpaque(true);
		cbox.setBackground(BG);
		return cbox;
	}
	
	
	public String toString()
	{
		String s = "SingleOrganismSyntenyPanel for " + organism.getName() + ":\n" + report;
		
		if (cboxes != null)
		{
			s += "\n  " + cboxes.size() + " checkboxes: ";
			for (TaggedCheckBox<Gene> cbox: cboxes)
				s += cbox.getTag() + ",";
			s = s.substring(0, s.length()-1);
		}
		
		if (cboxYs != null)
		{
			s += "\n  " + cboxYs.size() + " Ys: ";
			for (Integer y: cboxYs)
				s += y + ",";
			s = s.substring(0, s.length()-1);
		}
		
		return s;
	}
	
	
	private static int proximityDistToPix(int proxDist)
	{
		return PROXIMITY_DIST_TO_PIX[Math.min(proxDist-1, PROXIMITY_DIST_TO_PIX.length-1)];
	}
	
	
	private static Color proximitDistToColor(int proxDist)
	{
		return COLOR_BY_DISTANCE[Math.min(proxDist-1, COLOR_BY_DISTANCE.length-1)];
	}
	
	
	private static int proximityDistToLineWidth(int proxDist)
	{
		return STROKE_SIZES[Math.min(proxDist-1, STROKE_SIZES.length-1)];
	}
	
	
	private class Lom extends LayoutAdapter
	{
		// Preferred width is a margin around the widest checkbox.
		public Dimension preferredLayoutSize(Container parent)
		{
			int maxCboxOrLabelW = organismLabel.getPreferredSize().width;
			for (TaggedCheckBox<Gene> cbox: cboxes)
				maxCboxOrLabelW = Math.max(maxCboxOrLabelW, cbox.getPreferredSize().width);
			int cboxToRightIndex = (operonRanges == null)  ?  0  :  1;
			int prefW = LEFT_EDGE_TO_SPARK + sparklines.firstElement().getPreferredSize().width + 
				SPARK_TO_CBOX + maxCboxOrLabelW + CBOX_TO_RIGHT_EDGE[cboxToRightIndex];
			int prefH = cboxYs.lastElement() + 25;
			return new Dimension(prefW, prefH);
		}

	    public void layoutContainer(Container parent)                
	    {
	    	assert !cboxes.isEmpty();
	    	assert !sparklines.isEmpty();
	    	assert sparklines.size() == cboxes.size()  :  sparklines.size() + " != " + cboxes.size();
	    	
	    	// Label.
	    	organismLabel.setSize(parent.getWidth(), organismLabel.getPreferredSize().height);
	    	organismLabel.setLocation(0, 7);
	    	
	    	// Checkboxes.
    		Dimension sparkPref = sparklines.firstElement().getPreferredSize();
	    	int n = 0;
	    	int x = LEFT_EDGE_TO_SPARK + sparkPref.width + SPARK_TO_CBOX;
	    	for (JCheckBox cbox: cboxes)
	    	{
	    		Dimension pref = cbox.getPreferredSize();
	    		cbox.setSize(pref);
	    		int y = cboxYs.get(n++);
	    		cbox.setLocation(x, y);
	    	}
	    	
	    	// Sparklines.
	    	int cboxHalfHeight = cboxes.firstElement().getPreferredSize().height / 2;
	    	for (int i=0; i<sparklines.size(); i++)
	    	{
	    		SparklineGraph spark = sparklines.get(i);
	    		spark.setSize(sparkPref);
	    		JCheckBox cbox = cboxes.get(i);
	    		int yVertMidline = cbox.getLocation().y + cboxHalfHeight;
	    		spark.setLocation(LEFT_EDGE_TO_SPARK, yVertMidline-sparkPref.height/2);
	    	}
	    }
	}  // End of inner class Lom
	
	
	// Initialization that has to happen after size is valid.
	private void initLate()
	{
		// Compute x of vertical lines.
		Dimension sparkPref = sparklines.firstElement().getPreferredSize();
		assert sparkPref != null;
		int xOfVerticalLines = LEFT_EDGE_TO_SPARK + sparkPref.width + SPARK_TO_CBOX + CBOX_LEFT_TO_VERT_LINE; 
		
		linePaintables = new HashSet<Paintable>();
		distanceLabelBaselineCenterToSval = new HashMap<Point, String>();
		Vector<Integer> distances = report.getdDistancesNegMeansDifferentContig();
		for (int i=0; i<distances.size(); i++)
		{
			Integer yTop = cboxYs.get(i);
			Integer yBottom = cboxYs.get(i+1);
			LinePaintable lp = new LinePaintable(xOfVerticalLines, yTop+16, xOfVerticalLines, yBottom+16);
			int dist = distances.get(i);
			int distAbs = Math.abs(dist);
			Color color = proximitDistToColor(distAbs);
			lp.setLineColor(color);
			lp.setLineWidth(proximityDistToLineWidth(distAbs));
			linePaintables.add(lp);
			if (dist < 0)
				linePaintables.add(buildLinePaintableForContigBoundarySlash(lp, distAbs));
			if (distAbs >= COLOR_BY_DISTANCE.length)
			{
				int yCenter = 13 + (yTop + yBottom) / 2;
				Point labelOrigin = new Point(xOfVerticalLines+23, yCenter); 
				distanceLabelBaselineCenterToSval.put(labelOrigin, ""+distAbs);
			}
		}
	}
	
	
	private LinePaintable buildLinePaintableForContigBoundarySlash(LinePaintable verticalLP, int distAbs)
	{
		Point center = verticalLP.getCenter();
		center.y -= proximityDistToLineWidth(distAbs);
		LinePaintable slash = new LinePaintable(center.x-9, center.y-9, center.x+9, center.y+9);
		if (distAbs >= 3)
			slash.setStroke(new BasicStroke(2f));
		slash.setLineColor(verticalLP.getLineColor());
		return slash;
	}
	
	
	public void paintComponent(Graphics g)
	{
		Graphics2D g2 = (Graphics2D)g;
		g2.setColor(BG); 
		g2.fillRect(0, 0, getWidth(), getHeight());
		
		if (getWidth() <= 0)
			return;
		
		if (linePaintables == null)
			initLate();
		
		for (Paintable p: linePaintables)
			p.paint(g2);
		
		g2.setColor(Color.BLACK);
		g2.setFont(DISTANCE_FONT);
		FontMetrics fm = g2.getFontMetrics();
		for (Point origin: distanceLabelBaselineCenterToSval.keySet())
		{
			xformStack.push(g2);
			g2.translate(origin.x, origin.y);
			g2.rotate(3*Math.PI/2);
			String s = distanceLabelBaselineCenterToSval.get(origin);
			int sw = fm.stringWidth(s);
			g2.drawString(s, -sw/2, 0);
			xformStack.pop(g2);
		}
		
		if (operonRanges != null  &&  !operonRanges.isEmpty())
			for (OperonRange range: operonRanges)
				paintOperonRange(g2, range);
	}
	
	
	private void paintOperonRange(Graphics2D g2, OperonRange range)
	{
		int x = getWidth() - 18;
		int yTopGene = getCheckBoxY(range.firstVisibleGene) + 6;
		int yBottomGene = getCheckBoxY(range.lastVisibleGene) + 18;
		
		paintOperonRange(g2, 
						 x, 
						 yTopGene, 
						 range.operonStartsBeforeFirstVisibleGene, 
						 yBottomGene, 
						 range.operonEndsAfterLastVisibleGene);
	}
	
	
	static void paintOperonRange(Graphics2D g2, int x, int yTop, boolean topExtends, int yBottom, boolean bottomExtends)
	{
		Stroke entryStroke = g2.getStroke();
		g2.setStroke(new BasicStroke(3));
		g2.setColor(COREG_COLOR);
		
		// Vertical line.
		g2.drawLine(x, yTop, x, yBottom);
		
		// Top decorations.
		g2.setFont(ELIPSIS_FONT);
		int elipsisWidth = g2.getFontMetrics().stringWidth(S_DOT_DOT_DOT);		// if needed
		if (topExtends)
		{
			g2.drawString(S_DOT_DOT_DOT, x-elipsisWidth/2, yTop-6);
			g2.drawLine(x, yTop, x-8, yTop+8);
			g2.drawLine(x, yTop, x+8, yTop+8);
		}
		else
		{
			g2.drawLine(x-5, yTop, x+5, yTop);
		}
		
		// Bottom decorations.
		if (bottomExtends)
		{
			g2.drawString(S_DOT_DOT_DOT, x-elipsisWidth/2, yBottom+10);
			g2.drawLine(x, yBottom, x-8, yBottom-8);
			g2.drawLine(x, yBottom, x+8, yBottom-8);
		}
		else
		{
			g2.drawLine(x-5, yBottom, x+5, yBottom);
		}
		
		g2.setStroke(entryStroke);
	}
	
	
	private int getCheckBoxY(Gene gene)
	{
		return cboxYs.get(visibleGenes.indexOf(gene));
	}
	
	
	void selectAll(boolean b)
	{
		for (TaggedCheckBox<Gene> cbox: cboxes)
			cbox.setSelected(b);
	}
	
	
	Set<Gene> getSelectedGenes()
	{
		Set<Gene> ret = new HashSet<Gene>();
		for (TaggedCheckBox<Gene> cbox: cboxes)
			if (cbox.isSelected())
				ret.add(cbox.getTag());
		return ret;
	}
	
	
	boolean displaysOperonRanges()
	{
		return operonRanges != null  &&  !operonRanges.isEmpty();
	}
	
	
	boolean crossesContigs()
	{
		Vector<Integer> distances = report.getdDistancesNegMeansDifferentContig();
		for (Integer dist: distances)
			if (dist < 0)
				return true;
		return false;
	}
	
	
	static void sop(Object x)			{ System.out.println(x); }
	
	
	public static void main(String[] args)
	{
		dexter.MainDexterFrame.main(args);
	}
}
