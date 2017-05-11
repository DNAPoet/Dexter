package dexter.util.gui;

import java.awt.*;
import java.awt.geom.*;


//
// Just like a point, but expressed in grid units. This way the compiler catches confusion between
// pixel and grid units.
//


public class GridRectangle
{
	public int				xGrids;
	public int				yGrids;
	public int				wGrids;
	public int				hGrids;
	
	
	public GridRectangle()		{ }
	
	
	public GridRectangle(int xGrids, int yGrids, int wGrids, int hGrids)
	{
		this.xGrids = xGrids;
		this.yGrids = yGrids;
		this.wGrids = wGrids;
		this.hGrids = hGrids;
	}
	
	
	public GridRectangle(java.awt.Rectangle r)
	{
		this(r.x, r.y, r.width, r.height);
	}
	
	
	public int getRight()
	{
		return xGrids + wGrids;
	}
	
	
	public int getBottom()
	{
		return yGrids + hGrids;
	}
	
	
	// Assumes g's affine transform has been properly scaled.
	public void draw(Graphics g)
	{
		g.drawRect(xGrids, yGrids, wGrids, hGrids);
	}
	
	
	// Assumes g's affine transform has been properly scaled.
	public void fill(Graphics g)
	{
		g.fillRect(xGrids, yGrids, wGrids, hGrids);
	}
	
	
	public String toString()
	{
		return "GridRectangle (" + xGrids + "," + yGrids + ") " + wGrids + "x" + hGrids;
	}
}
