package dexter.cluster.tune;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.*;
import dexter.cluster.*;
import dexter.VisualConstants;
import dexter.util.LongBinCounter;
import dexter.util.gui.*;


public class NJEdgeMonitor extends JPanel implements NJListener, VisualConstants
{
	private final static Color[]				LINE_COLORS 	= { Color.BLUE, Color.RED, DARK_GREEN };
	private final static Color					BG				= Color.WHITE;
	private final static Font					AXIS_FONT		= new Font("SansSerif", Font.PLAIN, 12);
	private final static int					MARGIN			=  35;		
	private final static int					GRAPH_W_PIX		= 800;		
	private final static int					GRAPH_H_PIX		= 600;	
	
	private int									countPerVPix	=   1;					// doubles as needed
	private int									countPerHPix	=   1;					// doubles as needed
	private Vector<long[]>						cumulativeCounts;
	
	
	NJEdgeMonitor(MonitoredReuseNJTreeBuilder treeBuilder)
	{
		treeBuilder.setNJListener(this);
		cumulativeCounts = new Vector<long[]>();
	}
	
	
	public Dimension getPreferredSize()
	{
		return new Dimension(2*MARGIN + GRAPH_W_PIX, 2*MARGIN + GRAPH_H_PIX);
	}
	
	
	// Input is incremental since last report.
	public void	choseNodesToJoin(LongBinCounter<EdgeMixture> counts) 
	{
		// Compute cumulative counts.
		long[] incrementals = new long[3];
		int i = 0;
		for (EdgeMixture mix: EdgeMixture.values())
			incrementals[i++] = counts.getCountForBinZeroDefault(mix);
		long[] cumulatives = new long[3];
		synchronized (cumulativeCounts)
		{
			if (!cumulativeCounts.isEmpty())
			{
				long[] prevs = cumulativeCounts.lastElement();
				for (int j=0; j<3; j++)
					cumulatives[j] = prevs[j] + incrementals[j];
			}
			cumulativeCounts.add(cumulatives);
		}
		
		// Need to rescale?
		long totalCumulativeCounts = cumulatives[0] + cumulatives[1] + cumulatives[2];
		while (countToHPix(totalCumulativeCounts) > GRAPH_W_PIX)
			countPerHPix *= 2;
		long maxVCount = Math.max(cumulatives[0], Math.max(cumulatives[1], cumulatives[2]));
		while (countToVPix(maxVCount) > GRAPH_W_PIX)
			countPerVPix *= 2;
		
		repaint();
	}
	
	
	private int countToHPix(long count)
	{
		return (int)Math.round(count/countPerHPix);
	}
	
	
	private int countToVPix(long count)
	{
		return (int)Math.round(count/countPerVPix);
	}
	
	
	private Point countsToPoint(long hCounts, long vCounts, EdgeMixture mix)
	{
		int hPix = MARGIN + countToHPix(hCounts);
		int vPix = countToVPix(vCounts);
		if (vPix == 0)
			vPix += 1 + mix.ordinal();
		vPix = getHeight() - MARGIN - vPix;
		return new Point(hPix, vPix);
	}
	
	
	public void paintComponent(Graphics g)
	{
		// Fill.
		g.setColor(BG);
		g.fillRect(0, 0, 3333, 2222);
		
		// Clone array of cumulative counts to avoid concurrent modification.
		Vector<long[]> clonedCounts = new Vector<long[]>(cumulativeCounts.size());
		synchronized (cumulativeCounts) 
		{
			clonedCounts.addAll(cumulativeCounts);
		}
		
		// Traces.
		for (int n=0; n<3; n++)
		{
			EdgeMixture mix = EdgeMixture.values()[n];
			g.setColor(getColorForMix(mix));
			Point lastPoint = countsToPoint(0L, 0L, mix);
			for (long[] larry: clonedCounts)
			{
				long totalCountsForAllMixes = larry[0] + larry[1] + larry[2];
				long countForThisMix = larry[n];
				Point nextPoint = countsToPoint(totalCountsForAllMixes, countForThisMix, mix);
				drawLine(g, lastPoint, nextPoint);
				lastPoint = nextPoint;
			}
		}
		
		// Legend.
		int legendBaseline = MARGIN + 35;
		for (EdgeMixture mix: EdgeMixture.values())
		{
			g.setColor(getColorForMix(mix));
			g.drawString(mix.toString(), MARGIN+20, legendBaseline);
			legendBaseline += 20;
		}
	}
	
	
	private static void drawLine(Graphics g, Point p1, Point p2)	{ g.drawLine(p1.x, p1.y, p2.x, p2.y); }
	private static Color getColorForMix(EdgeMixture mix)			{ return LINE_COLORS[mix.ordinal()]; }
	public void reusedDistances(DistanceReuseReport reuseReport) 	{ }
	static void sop(Object x)										{ System.out.println(x); }
	
	
	public static void main(String[] args)
	{
		try
		{
			sop("START");	
			
			// Create nodes;
			int nNodes = 500;
			Vector<Node<String>> nodes = TreeBuilder.createStringPayloadNodes("Leaf_", nNodes, 0);
			
			// Create matrix with pseudo-random distances. Capacity needs to be 2x the number of original nodes.
			HalfArrayDistanceMatrix<Node<String>> distances = new HalfArrayDistanceMatrix<Node<String>>(nNodes*2);
			distances.randomize(nodes, 10, 100);
			
			// Create tree builder.
			MonitoredReuseNJTreeBuilder<String> treeBuilder = new MonitoredReuseNJTreeBuilder<String>(distances);
			treeBuilder.setJoinReportInterval(2);
			
			// Create GUI.
			NJEdgeMonitor that = new NJEdgeMonitor(treeBuilder);
			JFrame frame = new JFrame();
			frame.add(that, BorderLayout.CENTER);
			frame.pack();
			frame.setVisible(true);
			treeBuilder.buildTree();
		}
		catch (Exception x)
		{
			sop("Stress: " + x.getMessage());
			x.printStackTrace();
		}
		sop("DONE");
	}
}
