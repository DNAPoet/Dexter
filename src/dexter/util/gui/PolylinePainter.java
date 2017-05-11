package dexter.util.gui;

import java.util.*;
import java.awt.*;


public class PolylinePainter implements Paintable
{
	private Vector<Point>	points;
	private Stroke			stroke;
	private Color			color;
	
	
	public PolylinePainter(Vector<Point> points)
	{
		this.points = points;
	}
	
	
	public PolylinePainter(Vector<Point> points, Color color)
	{
		this(points);
		this.color = color;
	}
	
	
	public PolylinePainter(Vector<Point> points, Color color, Stroke stroke)
	{
		this(points, color);
		this.stroke = stroke;
	}


	public void paint(Graphics g)
	{
		assert points.size() > 1;
		
		Graphics2D g2 = (Graphics2D)g;
		g2.setColor(color);
		if (stroke != null)
			g2.setStroke(stroke);
		
		for (int i=0; i<points.size()-1; i++)
		{
			Point p1 = points.get(i);
			Point p2 = points.get(i+1);
			g2.drawLine(p1.x, p1.y, p2.x, p2.y);
		}
	}
}
