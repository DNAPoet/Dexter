package dexter.util.gui;


import java.awt.*;
import java.awt.event.MouseEvent;



abstract public class Draggable extends Armable
{
	protected Rectangle					bounds;
	protected boolean					dragging;
	protected int						grabHandleXFromBounds;			// valid if dragging
	protected int						grabHandleYFromBounds;			// valid if dragging
	protected int						draggingGrabHandleIndex;		// valid if dragging
	protected boolean					canMoveHorizontally = true;
	protected boolean					canMoveVertically = true;
	protected Color						disarmColor = Color.LIGHT_GRAY;	// deliberately ugly
	protected Color						armColor = Color.YELLOW;		// deliberately ugly
	protected Color						dragColor = Color.MAGENTA;		// deliberately ugly
	
	
	public void startDrag(int x, int y)
	{
		assert !dragging;
		assert contains(x, y);
		
		dragging = true;
		grabHandleXFromBounds = x - bounds.x;
		grabHandleYFromBounds = y - bounds.y;
	}
	

	// Returns translation vector.
	public int[] drag(int xHandle, int yHandle)
	{
		assert dragging;
		assert bounds != null  :  "Null bounds";

		int[] ret = { bounds.x, bounds.y };
		if (canMoveHorizontally)
			bounds.x = xHandle - grabHandleXFromBounds;
		if (canMoveVertically)	
			bounds.y = yHandle - grabHandleYFromBounds;
		ret[0] = bounds.x - ret[0];
		ret[1] = bounds.y - ret[1];
		return ret;
	}
	
	
	public void stopDrag()
	{
		assert dragging;
		dragging = false;
	}
	
	
	public void setMotionPermissions(boolean hok, boolean vok)
	{
		canMoveHorizontally = hok;
		canMoveVertically = vok;
	}
	
	
	public void moveTo(int x, int y)
	{
		bounds.x = x;
		bounds.y = y;
	}
	
	
	public void snap()								{ }
	public Rectangle getBounds()					{ return bounds; }
	public void setBounds(Rectangle bounds)			{ this.bounds = new Rectangle(bounds); }
	public void startDrag(MouseEvent e)				{ startDrag(e.getX(), e.getY()); }
	public int[] drag(MouseEvent e)				 	{ return drag(e.getX(), e.getY()); }
	public boolean isDragging()						{ return dragging; }
	public Color getDisarmColor()					{ return disarmColor; }
	public Color getArmColor()						{ return armColor; }
	public Color getDragColor()						{ return dragColor; }
	public void setDisarmColor(Color c)				{ disarmColor = c; }
	public void setArmColor(Color c)				{ armColor = c; }
	public void setDragColor(Color c)				{ dragColor = c; }
	public static void sop(Object x)				{ System.out.println(x); }
}
