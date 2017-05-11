package analysis.arkin.mergefig;

import java.util.*;
import java.awt.*;
import javax.swing.*;
import java.awt.geom.*;
import org.apache.commons.math3.distribution.*;


public class MergeFigure extends JPanel
{
	private final static int				GENE_W_PIX				=  85;
	private final static int				GENE_H_PIX				=  60;
	private final static int				GENE_GAP_PIX			=  14;
	private final static int				GENE_TOP_PIX			=  340;
	private final static int				GENE_TIP_W				=  18;
	private final static Font				GENE_ID_FONT			= new Font("SansSerif", Font.PLAIN, 13);
	private final static Font				DIST_FONT				= new Font("Monospaced", Font.PLAIN, 13);
	private final static float				H_SCALE					= (GENE_W_PIX-GENE_TIP_W) / 672f;	// 672 = max original x
	private final static Stroke				BOX_STROKE				= new BasicStroke(2);
	private final static Stroke				GRAPH_STROKE			= new BasicStroke(14);
	private final static int[]				TIER_V_MIDLINES =
	{
		GENE_TOP_PIX -190,
		GENE_TOP_PIX -30,
		GENE_TOP_PIX + GENE_H_PIX + 55,
		GENE_TOP_PIX + GENE_H_PIX + 215
	};
	private final static String[] 			CPD_STRINGS =
	{
		".20", ".14", ".07", ".19"
	};
	private final static NormalDistribution	TERY_NEG_DIST			= new NormalDistribution(13.2, 7.22);
	private final static int				GAUSS_BOX_W				= 160;
	private final static int				GAUSS_BOX_H				= 120;
	private final static double				GAUSS_MIN_DIST			= -10;
	private final static double				GAUSS_MAX_DIST			=  35;
	private final static double				GAUSS_MAX_DENSITY		= .06;
	private final static double				GAUSS_DIST_PER_H_PIX	= (GAUSS_MAX_DIST-GAUSS_MIN_DIST) / GAUSS_BOX_W;
	private final static double				GAUSS_DENSITY_PER_V_PIX	= GAUSS_MAX_DENSITY / GAUSS_BOX_H;
	private final static Color				GAUSS_SHADE_COLOR		= new Color(222, 222, 222);
	private final static Color				GAUSS_BOUNDARY_COLOR	= new Color(0, 0, 0);
	private final static Font				CPD_FONT				= new Font("SansSerif", Font.BOLD, 10);
	
	
	Map<String, EndpointPairsForGene> 		idToEndpointPairs;
	Map<String, Point>						idToUpperLeft;
	
	
	MergeFigure()
	{
		idToEndpointPairs = new TreeMap<String, EndpointPairsForGene>();
		Vector<EndpointPairsForGene> vec = EndpointPairsForGene.getAll();
		for (EndpointPairsForGene epfg: vec)
			idToEndpointPairs.put(epfg.id, epfg);
		
		idToUpperLeft = new TreeMap<String, Point>();
		int x = GENE_GAP_PIX;
		for (String id: idToEndpointPairs.keySet())
		{
			idToUpperLeft.put(id, new Point(x, GENE_TOP_PIX));
			x += GENE_GAP_PIX + GENE_W_PIX;
		}
		
		
		int w = 10 * GENE_W_PIX + 11 * GENE_GAP_PIX;
		setPreferredSize(new Dimension(w, 650));
	}
	
	
	public void paintComponent(Graphics g)
	{
		g.setColor(Color.WHITE);
		g.fillRect(0, 0, 2222, 2222);
		
		for (String id: idToUpperLeft.keySet())
		{
			paintGene(g, id);
		}

		paintDist(g, 4140, 4143, 7.12f, 0);	// last arg is tier
		paintDist(g, 4140, 4142, 5.36f, 1);	// last arg is tier
		paintDist(g, 4141, 4142, 2.4f, 2);	// last arg is tier
		paintDist(g, 4141, 4143, 6.81f, 3);	// last arg is tier
	}
	
	
	private void paintGene(Graphics g, String id)
	{
		Graphics2D g2 = (Graphics2D)g;
		AffineTransform entryXform = g2.getTransform();
		Stroke entryStroke = g2.getStroke();
		Point ul = idToUpperLeft.get(id);
		g2.translate(ul.x, ul.y);
		
		int n = Integer.parseInt(id.substring(5));
		boolean firstPrior = n<=4141;
		Color colorForPrior = firstPrior ? Color.BLUE : Color.RED;
		g2.setColor(colorForPrior);
		g2.setStroke(BOX_STROKE);
		int[] xPoints = { 0, GENE_W_PIX-GENE_TIP_W, GENE_W_PIX, GENE_W_PIX-GENE_TIP_W, 0, 0 };
		int[] yPoints = { 0, 0,						GENE_H_PIX/2, GENE_H_PIX, GENE_H_PIX , 0};
		
		g2.drawPolyline(xPoints, yPoints, 6);
		g2.scale(H_SCALE, .17f);
		g2.setStroke(GRAPH_STROKE);
		EndpointPairsForGene pairs = idToEndpointPairs.get(id);
		for (EndpointPair ep: pairs)
		{
			g2.drawLine(ep.x0, ep.y0+15, ep.x1, ep.y1+15);
		}
		
		g2.setTransform(entryXform);
		g2.setStroke(entryStroke);
		g2.setFont(GENE_ID_FONT);
		g2.drawString(id, ul.x, ul.y+GENE_H_PIX+18);
	}
	
	
	private void paintDist(Graphics g, int idi1, int idi2, float dist, int tier)
	{
		Graphics2D g2 = (Graphics2D)g;
		AffineTransform entryXform = g2.getTransform();
		Stroke entryStroke = g2.getStroke();
		int yTierMidline = TIER_V_MIDLINES[tier];
		String id1 = "Tery_" + idi1;
		String id2 = "Tery_" + idi2;
		int xLeft = idToUpperLeft.get(id1).x;
		int xRight = idToUpperLeft.get(id2).x + GENE_W_PIX;
		g2.translate(xLeft, yTierMidline);
		xRight -= xLeft;
		
		// Arrow stops.
		g2.setStroke(BOX_STROKE);
		g2.setColor(Color.BLACK);
		g2.drawLine(0, -24, 0, 24);
		g2.drawLine(xRight, -24, xRight, 24);
		
		// Arrows.
		g2.drawLine(5, 0, xRight-5, 0);
		g2.drawLine(5, 0, 10, 5);
		g2.drawLine(5, 0, 10, -5);
		g2.drawLine(xRight-5, 0, xRight-10, 5);
		g2.drawLine(xRight-5, 0, xRight-10, -5);
		
		// Arrow text.
		int xCenter = xRight / 2;
		String[] textLines = { "Expr Dist", id1 + "," + id2, "= "+dist };
		if (textLines[2].length() == 3)
			textLines[2]+= "0";
		if (dist < 3)
		{
			textLines[1] = id1.substring(5) + ", " + id2.substring(5);
			textLines[2] = "2.40";
		}
		g2.setFont(DIST_FONT);
		FontMetrics fm = g2.getFontMetrics();
		int baseline = -17;
		for (int i=0; i<3; i++)
		{
			String text = textLines[i];
			int sw = fm.stringWidth(text);
			int xText = xCenter - sw/2;
			if (i == 1)
			{
				g.setColor(Color.WHITE);
				g.fillRect(xText-8, -10, sw+16, 20);
			}
			g.setColor(Color.BLACK);
			g.drawString(text, xText, baseline);
			baseline += 17;
		}
		
		g2.setTransform(entryXform);
		g2.setStroke(entryStroke);
		
		// Gaussian box.
		int xGauss = idToUpperLeft.get(id2).x + GENE_W_PIX + 14;
		int yGauss = yTierMidline-GAUSS_BOX_H + 9;
		if (tier == 2)
		{
			xGauss = idToUpperLeft.get(id1).x - GAUSS_BOX_W - 14;
			yGauss += GAUSS_BOX_H - 18;
		}
		paintGaussian(g2, xGauss, yGauss, dist, CPD_STRINGS[tier]);
	}
	
	
	private void paintGaussian(Graphics2D g2, int x, int y, double xprDist, String cpdString)
	{
		AffineTransform entryXform = g2.getTransform();
		Stroke entryStroke = g2.getStroke();
		g2.translate(x, y);		

		String sXprDist = (""+xprDist).substring(0, 4);
		if (sXprDist.startsWith("6"))
			sXprDist = "6.81";
		else if (sXprDist.startsWith("7"))
			sXprDist = "7.12";

		double dist = GAUSS_MIN_DIST;
		int xPix = 0;
		int densityYPix = 0;
		for (xPix=0; xPix<GAUSS_BOX_W; xPix++)
		{
			double density = TERY_NEG_DIST.density(dist);
			densityYPix = (int)(density / GAUSS_DENSITY_PER_V_PIX);
			if (dist < xprDist)
			{
				g2.setColor(GAUSS_SHADE_COLOR);
				g2.drawLine(xPix, GAUSS_BOX_H, xPix, GAUSS_BOX_H-densityYPix);
				dist += GAUSS_DIST_PER_H_PIX;
			}
			else
				break;
		}
		g2.setColor(GAUSS_BOUNDARY_COLOR);
		g2.drawLine(xPix, GAUSS_BOX_H, xPix, GAUSS_BOX_H-densityYPix);
		
		g2.setColor(Color.BLACK);
		g2.setFont(CPD_FONT);
		int sw = g2.getFontMetrics().stringWidth(sXprDist);
		g2.drawString(sXprDist, xPix-sw/2, GAUSS_BOX_H+11);
		
		dist = GAUSS_MIN_DIST;
		for (xPix=0; xPix<GAUSS_BOX_W; xPix++)
		{
			double density = TERY_NEG_DIST.density(dist);
			densityYPix = (int)(density / GAUSS_DENSITY_PER_V_PIX);
			g2.setColor(Color.BLACK);
			int boxH = (densityYPix < 4)  ?  densityYPix-1  :  3;
			g2.fillRect(xPix, GAUSS_BOX_H-densityYPix, 3, boxH);
			dist += GAUSS_DIST_PER_H_PIX;
		}
		
		g2.setColor(Color.BLACK);
		g2.setStroke(BOX_STROKE);
		g2.drawRect(0, 0, GAUSS_BOX_W, GAUSS_BOX_H);
		
		g2.setFont(CPD_FONT);
		g2.drawString(cpdString, 36, GAUSS_BOX_H-3);
		
		String summary = "p(d<=" + sXprDist + "|nop) = " + cpdString;
		sop(summary);
		g2.drawString(summary, 30, -3);
				
		g2.setTransform(entryXform);
		g2.setStroke(entryStroke);
	}
	

	
	
	static void sop(Object x)			{ System.out.println(x); }
	
	
	public static void main(String[] args)
	{
		MergeFigure fig = new MergeFigure();
		JFrame frame = new JFrame();
		frame.add(fig, BorderLayout.CENTER);
		frame.pack();
		frame.setVisible(true);
	}
}
