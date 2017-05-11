package dexter.util.gui;

import java.awt.*;


//
// Bounds are 2x1 grid rectangle that encloses the triangular drag handle. The handle is beyond the line.
// The pin is vertical, the drag direction is horizontal.
//


public class VerticalPinDraggable extends Draggable implements java.io.Serializable
{
	private final static Font	FONT = new Font("SansSerif", Font.PLAIN, 12);
	
	private String				title;
	private Color				lineColor = Color.BLACK;
	private Color				textColor = Color.BLACK;
	private int					lineLength;
	private int					gridSizePix;				// for snapping and defining handle size
	private boolean				handleAtTop;
	private int					minXGrids = Integer.MIN_VALUE;
	private int 				maxXGrids = Integer.MAX_VALUE;
	
	
	public VerticalPinDraggable(String title, 
						 	    int xPix, int y0Pix, int y1Pix,
						 	    int gridSizePix, boolean handleAtTop)
	{
		this.title = title;
		this.lineLength = Math.abs(y0Pix - y1Pix);
		this.gridSizePix = gridSizePix;
		this.handleAtTop = handleAtTop;
		setMotionPermissions(true, false);		// H yes, V no

		if (handleAtTop)
		{
			int yTop = Math.min(y0Pix, y1Pix);
			setBounds(new Rectangle(xPix-gridSizePix, yTop-gridSizePix, 2*gridSizePix, gridSizePix));
		}
		else
		{
			int yBottom = Math.max(y0Pix, y1Pix);
			setBounds(new Rectangle(xPix-gridSizePix, yBottom, 2*gridSizePix, gridSizePix));
		}
	}
	

	public String toString()							
	{ 
		return "VerticalPin: \"" + title + "\" @ (" + bounds.x + "," + bounds.y + ") <" + 
			bounds.width + "x" + bounds.height + ">   line length=" + lineLength; 
	}
	
	
	public void paint(Graphics g)
	{		
		// Line.
		g.setColor(lineColor);
		int xCenter = bounds.x + bounds.width / 2;
		if (handleAtTop)
			g.drawLine(xCenter, bounds.y, xCenter, bounds.y+lineLength);
		else
			g.drawLine(xCenter, bounds.y, xCenter, bounds.y-lineLength);
		
		// Handle.
		g.setColor(getFillColor());
		int[] xs = new int[] { xCenter, bounds.x+bounds.width-gridSizePix/2-2, bounds.x+gridSizePix/2+2 };
		int[] ys = handleAtTop  ?
			new int[] { bounds.y+bounds.height, bounds.y, bounds.y }:
			new int[] { bounds.y, bounds.y+bounds.height, bounds.y+bounds.height }; 
		g.fillPolygon(xs, ys, 3);
		g.setColor(lineColor);
		g.drawPolygon(xs, ys, 3);
		
		// Title.
		g.setFont(FONT);
		g.setColor(textColor);
		int sw = g.getFontMetrics().stringWidth(title);
		int baseline = handleAtTop  ?  bounds.y - 2  :  bounds.y + bounds.height + 12;
		g.drawString(title, xCenter-sw/2, baseline);
	}
	
	
	public void snap()
	{
		int xCenter = bounds.x + bounds.width/2;
		int xCenterSnap = snapHPixToGrid(xCenter);
		int snapDelta = xCenterSnap - xCenter;
		bounds.translate(snapDelta, 0);
	}
	
	
	private int snapHPixToGrid(int hPix)
	{
		int grids = Math.round(hPix/(float)gridSizePix);
		grids = Math.min(grids, maxXGrids);
		grids = Math.max(grids, minXGrids);
		return grids * gridSizePix;
	}
	
	
	public void setMinMaxXGrids(int minXGrids, int maxXGrids)
	{
		this.minXGrids = minXGrids;
		this.maxXGrids = maxXGrids;
	}
	
	
	public int getXGrids()
	{
		int xCenter = bounds.x + bounds.width/2;
		return Math.round(xCenter/(float)gridSizePix);
	}
	
	
	public boolean contains(int x, int y)		{ return bounds.contains(x, y); }	
	public void setTitle(String s)				{ title = s; }
	public boolean isHandleAtTop()				{ return handleAtTop; }
	public void setLineColor(Color lineColor)	{ this.lineColor = lineColor; }
	public void setTextColor(Color textColor)	{ this.textColor = textColor; }
	public void setLineLength(int lineLength)	{ this.lineLength = lineLength; }
}
