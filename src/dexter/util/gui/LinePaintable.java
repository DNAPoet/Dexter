package dexter.util.gui;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.*;



public class LinePaintable implements Paintable
{
	protected int			x0;
	protected int			y0;
	protected int			x1;
	protected int			y1;
	protected Color 		lineColor 		= Color.BLACK;
	protected Stroke 		stroke 			= new BasicStroke(1f);
	
	
	public LinePaintable(int x0, int y0, int x1, int y1)
	{
		this.x0 = x0;
		this.y0 = y0;
		this.x1 = x1;
		this.y1 = y1;
	}
	
	
	public LinePaintable(int x, int y)
	{
		this(x, y, x, y);
	}
	
	
	public LinePaintable(Point p0, Point p1)
	{
		this(p0.x, p0.y, p1.x, p1.y);
	}
	
	
	public LinePaintable(LinePaintable src)
	{
		this.x0 = src.x0;
		this.y0 = src.y0;
		this.x1 = src.x1;
		this.y1 = src.y1;
		this.lineColor = src.lineColor;
		this.stroke = src.stroke;
	}
	
	
	public String toString()
	{
		return "LinePaintable (" + x0 + "," + y0 + ") -> (" + x1 + "," + y1 + ")";
	}
	
	
	public boolean equals(Object x)
	{
		LinePaintable that = (LinePaintable)x;
		return this.x0 == that.x0  &&  this.y0 == that.y0  &&  this.x1 == that.x1  &&  this.y1 == that.y1  &&
			this.lineColor.equals(that.lineColor)  &&  this.stroke.equals(that.stroke);
	}

	
	public void paint(Graphics g) 
	{
		Graphics2D g2 = (Graphics2D)g;
		g2.setColor(lineColor);
		Stroke entryStroke = g2.getStroke();
		g2.setStroke(stroke);
		g2.drawLine(x0, y0, x1, y1);
		g2.setStroke(entryStroke);
	}

	
	public void setLineWidth(float width) 
	{
		stroke = new BasicStroke(width);
	}

	
	public void setLineColor(Color lineColor) 
	{
		this.lineColor = lineColor;
	}
	
	
	// For rubber-band lines.
	public void stretchTo(Point tip)
	{
		stretchTo(tip.x, tip.y);
	}
	
	
	public void stretchTo(MouseEvent me)
	{
		stretchTo(me.getX(), me.getY());
	}
	
	
	public void stretchTo(int x, int y)
	{
		this.x1 = x;
		this.y1 = y;
	}
	
	
	public Point getTip()
	{
		return new Point(x1, y1);
	}
	
	
	public Point getCenter()
	{
		return new Point((x0+x1)/2, (y0+y1)/2);
	}
	
	
	public void setStroke(Stroke s)
	{
		this.stroke = s;
	}
	
	
	public Color getLineColor()
	{
		return lineColor;
	}
}
