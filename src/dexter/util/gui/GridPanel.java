package dexter.util.gui;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.geom.*;
import java.util.Stack;
import javax.swing.*;


public class GridPanel extends JPanel
{
	private final static int		DFLT_GRID_W_PIX			= 17;
	private final static int		DFLT_GRID_H_PIX			= 17;
	private final static Color		DFLT_BG					= Color.WHITE;
	private final static Color		DFLT_GRID_COLOR			= new Color(215, 225, 255);

	private int						gridWidth = DFLT_GRID_W_PIX;
	private int						gridHeight = DFLT_GRID_H_PIX;
	private Color					bg = DFLT_BG;
	private Color					gridColor = DFLT_GRID_COLOR;	
	private Stack<AffineTransform>	xformStack = new Stack<AffineTransform>();
	private boolean					suppressGridPainting;


	
	protected GridPanel()
	{
		this(-1, -1, null, null);
	}
	
	
	protected GridPanel(int gridWidth, int gridHeight)
	{
		this(gridWidth, gridHeight, null, null);
	}
	
	
	protected GridPanel(int gridWidth, int gridHeight, Color bg, Color gridColor)
	{
		if (gridWidth > 0)
			this.gridWidth = gridWidth;
		
		if (gridHeight > 0)
			this.gridHeight = gridHeight;
		
		if (bg != null)
			this.bg = bg;
		
		if (gridColor != null)
			this.gridColor = gridColor;
	}
	
	
	protected void paintGrid(Graphics g)
	{
		g.setColor(gridColor);
		int w = getWidth();
		int h = getHeight();
		for (int x=0; x<w; x+=gridWidth)
			g.drawLine(x, 0, x, h);
		for (int y=0; y<h; y+=gridHeight)
			g.drawLine(0, y, w, y);
	}
	
	
	public void paintComponent(Graphics g)
	{
		g.setColor(bg);
		g.fillRect(0, 0, 3333, 2222);
		if (!suppressGridPainting)
			paintGrid(g);
	}
	
	
	protected void pushXform(Graphics g)
	{
		xformStack.push(((Graphics2D)g).getTransform());
	}
	
	
	protected void popXform(Graphics g)
	{
		((Graphics2D)g).setTransform(xformStack.pop());
	}
	
	
	protected void scaleToGrid(Graphics g)
	{
		assert gridWidth > 0  &&  gridHeight > 0;
		
		((Graphics2D)g).scale(gridWidth, gridHeight);
	}
	
	
	protected void translateGrid(Graphics g, int deltaXGrids, int deltaYGrids)
	{
		assert gridWidth > 0  &&  gridHeight > 0;
		
		((Graphics2D)g).translate(deltaXGrids*gridWidth, deltaYGrids*gridHeight);
	}
	
	
	protected int getGridWidth()
	{
		return gridWidth;
	}
	
	
	protected int getGridHeight()
	{
		return gridHeight;
	}
	
	
	protected Dimension getGridWH()
	{
		return new Dimension(gridWidth, gridHeight);
	}
	
	
	protected int hGridsToPix(int hGrids)
	{
		return hGrids * gridWidth;
	}
	
	
	protected int vGridsToPix(int vGrids)
	{
		return vGrids * gridHeight;
	}
	
	
	protected Point gridsToPix(int xGrids, int yGrids)
	{
		return new Point(hGridsToPix(xGrids), vGridsToPix(yGrids));
	}
	
	
	protected void drawRectGrids(Graphics g, GridRectangle gr)
	{
		g.fillRect(hGridsToPix(gr.xGrids), vGridsToPix(gr.yGrids), hGridsToPix(gr.wGrids), vGridsToPix(gr.hGrids));
	}
	
	
	protected void fillRectGrids(Graphics g, GridRectangle gr)
	{
		g.fillRect(hGridsToPix(gr.xGrids), vGridsToPix(gr.yGrids), hGridsToPix(gr.wGrids), vGridsToPix(gr.hGrids));
	}
	
	
	protected void fillRectGrids(Graphics g, int x, int y, int w, int h)
	{
		g.fillRect(hGridsToPix(x), vGridsToPix(y), hGridsToPix(w), vGridsToPix(h));
	}
	
	
	protected Rectangle gridRectToPix(GridRectangle gridRect)
	{
		return new Rectangle(hGridsToPix(gridRect.xGrids), vGridsToPix(gridRect.yGrids), 
							 hGridsToPix(gridRect.wGrids), vGridsToPix(gridRect.hGrids));
	}
	
	
	protected Point gridPointToPix(GridPoint gridPoint)
	{
		return new Point(hGridsToPix(gridPoint.xGrids), vGridsToPix(gridPoint.yGrids));
	}
	
	
	protected void drawLineGrids(Graphics g, int x0, int y0, int x1, int y1)
	{
		g.drawLine(hGridsToPix(x0), vGridsToPix(y0), hGridsToPix(x1), vGridsToPix(y1));
	}
	
	
	protected GridPoint snapPixToGrid(Point pix)
	{
		return snapPixToGrid(pix.x, pix.y);
	}
	
	
	protected GridPoint snapPixToGrid(int xPix, int yPix)
	{
		int xGrids = (xPix + gridWidth/2) / gridWidth;
		int yGrids = (yPix + gridHeight/2) / gridHeight;
		return new GridPoint(xGrids, yGrids);
	}
	
	
	protected GridPoint snapPixToGrid(MouseEvent me)
	{
		return snapPixToGrid(me.getX(), me.getY());
	}
	
	
	public void setSuppressGridPainting(boolean suppressGridPainting)
	{
		this.suppressGridPainting = suppressGridPainting;
	}
	
	
	public Window getContainingFrameOrDialog()
	{
		Component c = this;
		while (!(c.getParent() instanceof Window))
			c = c.getParent();
		return (Window)c;
	}
	
	
	public void setTitleOfContainingFrameOrDialog(String title)
	{
		Window win = getContainingFrameOrDialog();
		if (win instanceof JFrame)
			((JFrame)win).setTitle(title);
		else if (win instanceof JDialog)
			((JDialog)win).setTitle(title);
	}
	
	
	public static void sop(Object x)
	{
		System.out.println(x);
	}
	
	
	public static void dsop(Object x)
	{
		sop(new java.util.Date() + ": " + x);
	}
}		
