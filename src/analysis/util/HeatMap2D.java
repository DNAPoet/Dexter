package analysis.util;

import java.util.*;
import java.awt.*;
import java.awt.geom.*;

import javax.swing.*;
import dexter.util.gui.*;


public class HeatMap2D extends JPanel implements TransferFunction
{
	
	private final static MarginModel	DFLT_MARGINS				= new MarginModel(90, 15, 35, 75);
	private final static Dimension		DFLT_CELL_SIZE				= new Dimension(5, 5);
	private final static int			AXIS_LABEL_FONT_SIZE		= 22;
	private final static Font			AXIS_LABEL_FONT				= new Font("Serif", Font.PLAIN, AXIS_LABEL_FONT_SIZE);
	private final static Font			TICK_FONT					= new Font("SansSerif", Font.PLAIN, 10);
	private final static Font			TITLE_FONT					= new Font("Serif", Font.PLAIN, 40);
	private final static Font			LEGEND_FONT					= new Font("Monospaced", Font.PLAIN, 18);
	private final static Color			DFLT_BG						= Color.LIGHT_GRAY;
	private final static int			MINOR_TICK_LEN_PIX			=  7;
	private final static int			MAJOR_TICK_LEN_PIX			= 17;
	private final static int			AXIS_ARROW_LEN_PIX			= 60;
	private final static int			DFLT_GRIDS_PER_MINOR_TICK	=  1;
	private final static int			DFLT_GRIDS_PER_MAJOR_TICK	= 10;
	private final static int			LEGEND_H_MARGIN				= 10;
	private final static int			LEGEND_TOP_BASELINE			= 32;
	private final static int			LEGEND_DELTA_BASELINE		= 20;
	private final static int			LEGEND_BOTTOM_MARGIN		= 10;
	private final static int			LEGEND_BOX_SIZE				= 14;
	private final static int			LEGEND_BOX_TO_TEXT			=  4;

	private String 						title;												// null for no title
	private MarginModel					margins 					= DFLT_MARGINS;
	private Color						bg							= DFLT_BG;
	private Point						legendUpperLeft				= null;					// null for no legend
	private TransferFunction			transferFn					= this;
	private short[][]					counts;												// [horiz][vert]
	private String						horizontalAxisText;
	private String						verticalAxisText;
	private Dimension					cellSize 					= DFLT_CELL_SIZE;
	private int							gridsPerHMinorTick			= DFLT_GRIDS_PER_MINOR_TICK;
	private int							gridsPerHMajorTick			= DFLT_GRIDS_PER_MAJOR_TICK;
	private int							gridsPerVMinorTick			= DFLT_GRIDS_PER_MINOR_TICK;
	private int							gridsPerVMajorTick			= DFLT_GRIDS_PER_MAJOR_TICK;
	private float[]						hMajorTickLabelStepProgram;
	private Vector<String>				hMajorTickLabelTexts;
	private float[]						vMajorTickLabelStepProgram;
	private Vector<String>				vMajorTickLabelTexts;
	private TransformStack				xformStack 					= new TransformStack();
	
	
	public HeatMap2D()			{ }
	
	
	public HeatMap2D(short[][] counts)
	{		
		setCounts(counts);
	}
	
	
	public Dimension getPreferredSize()	
	{
		return new Dimension(margins.getLeft() + getGraphWidth() + margins.getRight(), 
				             margins.getTop() + getGraphHeight() + margins.getBottom());
	}
	
	
	private int getGraphWidth()
	{
		assert counts != null;
		assert cellSize != null;
		return counts.length * cellSize.width;
	}
	
	
	private int getGraphHeight()
	{
		return counts[0].length * cellSize.height;
	}
	
	
	private int getGraphBottom()
	{
		return margins.getTop() + getGraphHeight();
	}
	
	
	private int getGraphRight()
	{
		return margins.getLeft() + getGraphWidth();
	}
	
	
	public void paintComponent(Graphics g)
	{
		// Clear.
		g.setColor(bg);
		g.fillRect(0, 0, getWidth(), getHeight());
		
		
		// Cells.
		int graphBottom = getGraphBottom();
		int x = margins.getLeft();
		for (int col=0; col<counts.length; col++)
		{
			int y = graphBottom - cellSize.height;
			for (int row=0; row<counts[col].length; row++)
			{
				if (counts[col][row] > 0)
				{
					g.setColor(transferFn.transfer(counts[col][row]));
					g.fillRect(x, y, cellSize.width, cellSize.height);
				}
				y -= cellSize.height;
			}
			x += cellSize.width;
		}
		
		// Axes.
		g.setColor(Color.BLACK);
		g.drawLine(margins.getLeft(), margins.getTop(), margins.getLeft(), graphBottom);
		g.drawLine(margins.getLeft(), graphBottom, getGraphRight(), graphBottom);
		
		// Ticks.
		g.setColor(Color.BLACK);
		setFont(TICK_FONT);
		FontMetrics tickFM = g.getFontMetrics();
		x = margins.getLeft();	
		while (x <= getGraphRight())												// minor horizontal
		{
			g.drawLine(x, graphBottom, x, graphBottom+MINOR_TICK_LEN_PIX);
			x += gridsPerHMinorTick * cellSize.width;
		}
		Vector<String> majorTickTexts = getMajorTickLabels(true);					// major horizontal
		x = margins.getLeft();	
		int baseline = graphBottom + MAJOR_TICK_LEN_PIX + 12;
		while (x <= getGraphRight())
		{
			g.drawLine(x, graphBottom, x, graphBottom+MAJOR_TICK_LEN_PIX);
			if (majorTickTexts != null  &&  !majorTickTexts.isEmpty())
			{
				String s = majorTickTexts.remove(0);
				int sw = tickFM.stringWidth(s);
				g.drawString(s, x - sw/2, baseline);
			}
			x += gridsPerHMajorTick * cellSize.width;
		}
		int y = graphBottom;
		while (y >= margins.getTop())												// minor vertical
		{
			g.drawLine(margins.getLeft()-MINOR_TICK_LEN_PIX, y, margins.getLeft(), y);
			y -= gridsPerVMinorTick * cellSize.height;
		}
		majorTickTexts = getMajorTickLabels(false);									// major vertical
		y = graphBottom;
		while (y >= margins.getTop())								
		{
			g.drawLine(margins.getLeft()-MAJOR_TICK_LEN_PIX, y, margins.getLeft(), y);
			if (majorTickTexts != null  &&  !majorTickTexts.isEmpty())
			{
				String s = majorTickTexts.remove(0);
				int sw = tickFM.stringWidth(s);
				g.drawString(s, margins.getLeft()-MAJOR_TICK_LEN_PIX-sw-3, y+4);
			}
			y -= gridsPerVMajorTick * cellSize.height;
		}
		
		// Axis text (optional).
		if (horizontalAxisText != null)
		{
			baseline = graphBottom + MAJOR_TICK_LEN_PIX + AXIS_LABEL_FONT_SIZE + 16;
			paintAxisTextAndArrow(g, horizontalAxisText, margins.getLeft(), baseline);
		}
		if (verticalAxisText != null)
		{
			xformStack.push(g);
			g.translate(margins.getLeft(), graphBottom);
			((Graphics2D)g).rotate(Math.PI * 1.5);
			baseline = - (MAJOR_TICK_LEN_PIX + AXIS_LABEL_FONT_SIZE + 10);
			paintAxisTextAndArrow(g, verticalAxisText, 0, baseline);
			xformStack.pop(g);
		}
		
		// Title (optional).
		if (title != null)
		{
			g.setFont(TITLE_FONT);
			g.setColor(Color.BLACK);
			int sw = g.getFontMetrics().stringWidth(title);
			int xCenter = (margins.getLeft() + getGraphRight()) / 2;
			x = xCenter - sw/2;
			baseline = margins.getTop();
			g.drawString(title, x, baseline);
		}
		
		// Legend (optional).
		if (legendUpperLeft != null)
		{
			xformStack.push(g);
			g.translate(legendUpperLeft.x, legendUpperLeft.y);
			Dimension legendSize = getLegendSize();
			g.setColor(Color.WHITE);												// fill
			g.fillRect(0, 0, legendSize.width, legendSize.height);
			g.setColor(Color.BLACK);												// border
			g.drawRect(0, 0, legendSize.width, legendSize.height);
			g.drawRect(1, 1, legendSize.width-2, legendSize.height-2);
			Map<String, Color> textToColor = transferFn.getLegendTextToColor();		// boxes and text
			g.setFont(LEGEND_FONT);
			baseline = LEGEND_TOP_BASELINE;
			for (String s: textToColor.keySet())
			{
				g.setColor(Color.BLACK);
				g.drawString(s, LEGEND_H_MARGIN + LEGEND_BOX_SIZE + LEGEND_BOX_TO_TEXT, baseline);
				int boxTop = baseline - LEGEND_BOX_SIZE + 1;
				g.setColor(textToColor.get(s));
				g.fillRect(LEGEND_H_MARGIN, boxTop, LEGEND_BOX_SIZE, LEGEND_BOX_SIZE);
				g.setColor(Color.BLACK);
				g.drawRect(LEGEND_H_MARGIN, boxTop, LEGEND_BOX_SIZE, LEGEND_BOX_SIZE);
				baseline += LEGEND_DELTA_BASELINE;
			}
			xformStack.pop(g);
		}
	}
	
	
	// Assumes g's color is correct.
	private void paintAxisTextAndArrow(Graphics g, String s, int x, int baseline)
	{
		g.setFont(AXIS_LABEL_FONT);
		g.drawString(s, x, baseline);
		FontMetrics fm = g.getFontMetrics();
		int sw = fm.stringWidth(s);
		paintRightArrow(g, x + sw + 7, baseline - AXIS_LABEL_FONT_SIZE/2 + 4);
	}
	
	
	// Assumes g's color is correct.
	private void paintRightArrow(Graphics g, int x, int y)
	{
		int right = x + AXIS_ARROW_LEN_PIX;
		g.drawLine(x, y, right, y);
		g.drawLine(right, y, right-6, y-6);
		g.drawLine(right, y, right-6, y+6);
	}
	
	
	private Vector<String> getRawMajorTickLabels(boolean horizontal)
	{
		if (horizontal  &&  hMajorTickLabelTexts != null)
			return hMajorTickLabelTexts;
		else if (horizontal  &&  hMajorTickLabelStepProgram == null)
			return null;
		
		else if (!horizontal  &&  vMajorTickLabelTexts != null)
			return vMajorTickLabelTexts;
		else if (!horizontal  &&  vMajorTickLabelStepProgram == null)
			return null;
		
		float[] program = horizontal  ?  hMajorTickLabelStepProgram  :  vMajorTickLabelStepProgram;
		int nTicks = horizontal  ?  counts.length / gridsPerHMajorTick  :  counts[0].length / gridsPerVMajorTick;
		Vector<String> ret = new Vector<String>();
		float f = program[0];
		while (ret.size() < nTicks)
		{
			ret.add("" + f);
			f += program[1];
		}
		return ret;
	}
	
	
	private Vector<String> getMajorTickLabels(boolean horizontal)
	{
		Vector<String> raw = getRawMajorTickLabels(horizontal);
		if (raw == null)
			return null;
		Vector<String> ret = new Vector<String>();
		for (String s: raw)
		{
			while (s.endsWith("0"))
				s = s.substring(0, s.length()-1);
			if (s.endsWith("."))
				s = s.substring(0, s.length()-1);
			ret.add(s);
		}
		return ret;
	}
	
	
	public void setHorizontalMajorTickLabelStepProgram(float[] startAndDelta)
	{
		assert startAndDelta.length == 2;
		hMajorTickLabelStepProgram = startAndDelta;
		hMajorTickLabelTexts = null;
	}
	
	
	public void setHorizontalMajorTickLabelStepProgram(float start, float delta)
	{
		setHorizontalMajorTickLabelStepProgram(new float[] { start, delta });
	}
	
	
	public void setHorizontalMajorTickLabelTexts(Vector<String> texts)
	{
		hMajorTickLabelTexts = texts;
		hMajorTickLabelStepProgram = null;
	}
	
	
	public void setVerticalMajorTickLabelStepProgram(float[] startAndDelta)
	{
		assert startAndDelta.length == 2;
		vMajorTickLabelStepProgram = startAndDelta;
		vMajorTickLabelTexts = null;
	}
	
	
	public void setVerticalMajorTickLabelStepProgram(float start, float delta)
	{
		setVerticalMajorTickLabelStepProgram(new float[] { start, delta });
	}
	
	
	public void setVerticalMajorTickLabelTexts(Vector<String> texts)
	{
		vMajorTickLabelTexts = texts;
		vMajorTickLabelStepProgram = null;
	}

	
	public Color transfer(int count)
	{
		assert count >= 0;
		
		switch (count)
		{
			case 0:		return null;
			case 1:		return Color.BLUE;
			case 2:		return Color.CYAN;
			case 3:		return Color.GREEN;
			case 4:		return Color.YELLOW;
			case 5:		return Color.ORANGE;
			default:	return Color.RED;
		}
	}
	
	
	public LinkedHashMap<String, Color> getLegendTextToColor()
	{
		LinkedHashMap<String, Color> ret = new LinkedHashMap<String, Color>();
		for (int i=1; i<=5; i++)
			ret.put(""+i, transfer(i));
		ret.put(">=6", transfer(6));
		return ret;
	}	
	
	
	private Dimension getLegendSize()
	{
		LinkedHashMap<String, Color> textToColor = transferFn.getLegendTextToColor();
		FontMetrics fm = getFontMetrics(LEGEND_FONT);
		int maxSw = 0;
		for (String s: textToColor.keySet())
			maxSw = Math.max(maxSw, fm.stringWidth(s));
		int prefW = LEGEND_H_MARGIN + LEGEND_BOX_SIZE + LEGEND_BOX_TO_TEXT + maxSw + LEGEND_H_MARGIN;
		int prefH = LEGEND_TOP_BASELINE + (textToColor.size() - 1) * LEGEND_DELTA_BASELINE + LEGEND_BOTTOM_MARGIN;
		return new Dimension(prefW, prefH);
	}
	
	
	public void setCellSize(Dimension d)		{ this.cellSize = d; }
	public void setCellSize(int w, int h)		{ setCellSize(new Dimension(w, h)); }
	public void setHorizontalAxisText(String s)	{ horizontalAxisText = s; }
	public void setVerticalAxisText(String s)	{ verticalAxisText = s; }
	public void setGridsPerHMinorTick(int n)	{ gridsPerHMinorTick = n; }
	public void setGridsPerHMajorTick(int n)	{ gridsPerHMajorTick = n; }
	public void setGridsPerVMinorTick(int n)	{ gridsPerVMinorTick = n; }
	public void setGridsPerVMajorTick(int n)	{ gridsPerVMajorTick = n; }
	public void setCounts(short[][] counts)		{ this.counts = counts; }
	public void setTitle(String s)				{ title = s; }
	public void setLegendUpperLeft(Point p)		{ legendUpperLeft = p; }
	static void sop(Object x)					{ System.out.println(x); }
	
	
	public static void main(String[] args)
	{
		try
		{
			sop("START");
			short[][] counts = new short[25][10];
			for (int x=0; x<25; x+=2)
			{
				for (int y=0; y<10; y++)
				{
					counts[x][y] = (short)y; 
				}
			}
			HeatMap2D map = new HeatMap2D(counts);
			map.setCellSize(15, 20);
			map.setHorizontalAxisText("ABCDE");
			map.setVerticalAxisText("WX123");
			map.setGridsPerHMinorTick(2);
			map.setHorizontalMajorTickLabelStepProgram(100, 3);
			//map.setGridsPerVMajorTick(4);
			map.setVerticalMajorTickLabelStepProgram(99, 5);
			JFrame frame = new JFrame();
			frame.add(map, BorderLayout.CENTER);
			frame.pack();
			frame.setVisible(true);
		}
		catch (Exception x)
		{
			sop("Stress: " + x.getMessage());
			x.printStackTrace(System.out);
		}
		finally
		{
			sop("DONE");
		}
	}
}
