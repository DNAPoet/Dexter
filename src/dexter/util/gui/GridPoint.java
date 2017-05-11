package dexter.util.gui;


//
// Just like a point, but expressed in grid units. This way the compiler catches confusion between
// pixel and grid units.
//

public class GridPoint 
{
	public int				xGrids;
	public int				yGrids;
	
	
	public GridPoint()		{ }
	
	
	public GridPoint(int xGrids, int yGrids)
	{
		this.xGrids = xGrids;
		this.yGrids = yGrids;
	}
	
	
	public String toString()
	{
		return "GridPoint (" + xGrids + "," + yGrids + ")";
	}
}
