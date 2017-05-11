package dexter.util.gui;

import java.awt.*;
import java.util.*;



public class PolyLinePaintable extends Vector<LinePaintable> implements Paintable
{
	private static int				nextSN;
	
	private int						sn = nextSN++;
	private Point					pen;
	private PolyLinePaintable		limningSegments;
	private Color					lineColor;
	private float					strokeSize = -1;
	
	
	public PolyLinePaintable(Point start)
	{
		this.pen = start;
	}
	
	
	public PolyLinePaintable(int x0, int y0)
	{
		this.pen = new Point(x0, y0);
	}
	
	
	public PolyLinePaintable(PolyLinePaintable src)
	{
		for (LinePaintable lp: src)
			this.add(new LinePaintable(lp));
		this.pen = new Point(src.pen);
	}
	
	
	public String toString()
	{
		String s = "PolyLinePaintable " + sn + ": size=" + size();
		for (LinePaintable line: this)
			s += "\n  " + line;
		s += "\nPen = (" + pen.x + "," + pen.y + ")";
		return s;
	}
	
	
	public boolean equals(Object x)
	{
		PolyLinePaintable that = (PolyLinePaintable)x;
		if (this.sn != that.sn)
			return false;
		if (this.size() != that.size())
			return false;
		for (int i=0; i<size(); i++)
			if (!(this.get(i).equals(that.get(i))))
				return false;
		return true;
	}
	
	
	public void moveTo(Point p)
	{
		this.pen = new Point(p);
	}
	
	
	public void moveTo(int x, int y)
	{
		this.pen = new Point(x, y);
	}
	
	
	public void lineTo(Point p)
	{
		add(new LinePaintable(pen, p));
		pen = new Point(p);
		
		if (lineColor != null)
			lastElement().setLineColor(lineColor);
		
		if (strokeSize >= 0f)
			lastElement().setLineWidth(strokeSize);
	}
	
	
	public void lineTo(int x, int y)
	{
		lineTo(new Point(x, y));
	}
	
	
	// Moves last member and pen.
	public void stretchTo(int x, int y)
	{
		if (isEmpty())
			lineTo(x, y);
		else
			lastElement().stretchTo(x, y);
		pen = new Point(x, y);
	}
	
	
	// Moves last member and pen.
	public void stretchTo(Point p)
	{
		assert !isEmpty();
		
		lastElement().stretchTo(p);
		pen = new Point(p);
	}
	
	
	public Point getPen()
	{
		return pen;
	}
	
	
	public int getSN()
	{
		return sn;
	}
	
	
	public void limn(Color limnColor, int limnWidth)
	{
		limningSegments = new PolyLinePaintable(this);		// Doesn't copy this.limningSegments
		for (LinePaintable lp: limningSegments)
		{
			lp.setLineColor(limnColor);
			lp.setLineWidth(limnWidth);
		}
	}
	
	
	public void unlimn()
	{
		limningSegments = null;
	}
	
	
	public void setLineColor(Color lineColor)
	{
		this.lineColor = lineColor;
	}
	
	
	public void setStrokeSize(float strokeSize)
	{
		this.strokeSize = strokeSize;
	}
	
	
	public void paint(Graphics g)
	{
		if (limningSegments != null)
		{
			assert limningSegments.limningSegments == null;
			limningSegments.paint(g);
		}
		
		for (LinePaintable lp: this)
			lp.paint(g);
	}
	
	
	static void sop(Object x)		{ System.out.println(x); }
}
