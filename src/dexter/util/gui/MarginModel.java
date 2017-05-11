package dexter.util.gui;

import java.awt.*;


public class MarginModel 
{
	private int			left;
	private int			right;
	private int			top;
	private int			bottom;
	
	
	public MarginModel()		{ }
	
	
	public MarginModel(int left, int right, int top, int bottom)
	{
		this.left = left;
		this.right = right;
		this.top = top;
		this.bottom = bottom;
	}
	
	
	public MarginModel(int m)
	{
		this(m, m, m, m);
	}
	
	
	public Dimension fitAround(Dimension dim)
	{
		return fitAround(dim.width, dim.height);
	}
	
	
	public Dimension fitAround(int w, int h)
	{
		return new Dimension(left + w + right, top + h + bottom);
	}
	
	
	public int getLeft()			{ return left; }
	public int getRight()			{ return right; }
	public int getTop()				{ return top; }
	public int getBottom()			{ return bottom; }
}
