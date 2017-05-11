package dexter.util.gui;

import java.awt.*;


//
// The pin is horizontal, the drag direction is vertical. The bounds circumscribe the triangular handle, which is
// 2 grid units wide by 1 grid unit high.
//


public class HorizontalPinDraggable extends Draggable
{
	private final static Font	DFLT_FONT = new Font("SansSerif", Font.PLAIN, 12);
	
	private Font 				font;
	private String				title;
	private Color				lineColor = Color.BLACK;
	private Color				textColor = Color.BLACK;
	private int					lineLength;
	private int					gridSizePix;				// for snapping and defining handle size
	private boolean				handleAtRight;
	private int					minYGrids = Integer.MIN_VALUE;
	private int 				maxYGrids = Integer.MAX_VALUE;
	private int					minBoundsYPix = Integer.MIN_VALUE;
	private int 				maxBoundsYPix = Integer.MAX_VALUE;
	
	
	public HorizontalPinDraggable(String title, 
						 	      int x0Pix, int x1Pix, int yPix,
						 	      int gridSizePix, boolean handleAtRight)
	{
		this.font = DFLT_FONT;
		this.title = title;
		this.lineLength = Math.abs(x0Pix - x1Pix);
		this.gridSizePix = gridSizePix;
		this.handleAtRight = handleAtRight;
		setMotionPermissions(false, true);		// H yes, V no

		if (handleAtRight)
		{
			int xRight = Math.max(x0Pix, x1Pix);
			setBounds(new Rectangle(xRight, yPix-gridSizePix, 2*gridSizePix, gridSizePix));
		}
		else
		{
			int xLeft = Math.min(x0Pix, x1Pix);
			setBounds(new Rectangle(xLeft-gridSizePix, yPix, 2*gridSizePix, gridSizePix));
		}
	}
	

	public String toString()							
	{ 
		return "HorizontalPin: \"" + title + "\" @ (" + bounds.x + "," + bounds.y + ") <" + 
			bounds.width + "x" + bounds.height + ">   line length=" + lineLength; 
	}
	
	
	public void paint(Graphics g)
	{		
		// Line.
		g.setColor(lineColor);
		int yCenter = bounds.y + bounds.height / 2;
		if (handleAtRight)
			g.drawLine(bounds.x, yCenter, bounds.x-lineLength, yCenter);
		else
			g.drawLine(bounds.x, yCenter, bounds.x+lineLength, yCenter);
		
		// Handle.
		g.setColor(getFillColor());
		int[] xs =  handleAtRight  ?  
			new int[] { bounds.x, bounds.x+bounds.width, bounds.x+bounds.width }  :
			new int[] { bounds.x+bounds.width, bounds.x, bounds.x };
		int[] ys = { bounds.y+bounds.height/2, bounds.y, bounds.y+bounds.height }; 
		g.fillPolygon(xs, ys, 3);
		g.setColor(lineColor);
		g.drawPolygon(xs, ys, 3);
		
		// Title.
		g.setFont(font);
		g.setColor(textColor);
		int baseline = bounds.y + bounds.height/2 + 6;
		int xText = handleAtRight  ?
			bounds.x + bounds.width + 5  :
			bounds.x - g.getFontMetrics().stringWidth(title) - 3;
		g.drawString(title, xText, baseline);
	}
	
	
	public void snap()
	{
		int yCenter = bounds.y + bounds.height/2;
		int yCenterSnap = snapVPixToGrid(yCenter);
		int snapDelta = yCenterSnap - yCenter;
		bounds.translate(0, snapDelta);
	}
	
	
	private int snapVPixToGrid(int vPix)
	{
		int grids = Math.round(vPix/(float)gridSizePix);
		grids = Math.min(grids, maxYGrids);
		grids = Math.max(grids, minYGrids);
		return grids * gridSizePix;
	}
	
	
	public void setMinMaxYGrids(int minYGrids, int maxYGrids)
	{
		assert minYGrids <= maxYGrids;
		
		this.minYGrids = minYGrids;
		this.maxYGrids = maxYGrids;
	}
	
	
	public void setMinMaxYPix(int minYPix, int maxYPix)
	{
		assert minYPix <= maxYPix;
		
		this.minBoundsYPix = minYPix - gridSizePix/2;
		this.maxBoundsYPix = maxYPix - gridSizePix/2;
	}	
	
	
	public int[] drag(int xHandle, int yHandle)
	{
		int[] ret = super.drag(xHandle, yHandle);

		// Clamp on top.
		if (bounds.y < minBoundsYPix)
		{
			int fixDelta = minBoundsYPix - bounds.y;
			ret[1] += fixDelta;
			bounds.y = minBoundsYPix;
		}
		
		// Clamp on bottom.
		else if (bounds.y > maxBoundsYPix)
		{
			int fixDelta = bounds.y - maxBoundsYPix;
			ret[1] -= fixDelta;
			bounds.y = maxBoundsYPix;
		}
		
		return ret;
	}
	
	
	public int getYPix()
	{
		return bounds.y + bounds.height/2;
	}
	
	
	public int getYGrids()
	{
		return Math.round(getYPix()/(float)gridSizePix);
	}
	
	
	public void setFont(Font font)				{ this.font = font; }
	public boolean contains(int x, int y)		{ return bounds.contains(x, y); }	
	public void setTitle(String s)				{ title = s; }
	public boolean isHandleAtRight()			{ return handleAtRight; }
	public void setLineColor(Color lineColor)	{ this.lineColor = lineColor; }
	public void setTextColor(Color textColor)	{ this.textColor = textColor; }
	public void setLineLength(int lineLength)	{ this.lineLength = lineLength; }
}
