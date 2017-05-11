package dexter.util.gui;

import java.awt.*;
import java.awt.event.MouseEvent;
import dexter.VisualConstants;


public abstract class Armable implements Paintable, VisualConstants
{
	public static Color								DFLT_DISARM_COLOR 	= Color.LIGHT_GRAY;
	public static Color								DFLT_ARM_COLOR 		= ARM_COLOR;
	public static Color								DFLT_SEL_COLOR		= SELECTION_COLOR;
	
	protected boolean								armed;
	protected Color									disarmColor 		= DFLT_DISARM_COLOR;
	protected Color									armColor 			= DFLT_ARM_COLOR;
	
	
	public Color getFillColor() 					{ return armed ? armColor : disarmColor; }	
	public void arm()								{ armed = true;	}
	public void disarm()							{ armed = false; }
	public void setArmed(boolean armed)				{ this.armed = armed; }
	public boolean isArmed()						{ return armed;	}
	public Color getDisarmColor()					{ return disarmColor; }
	public Color getArmColor()						{ return armColor; }
	public void setDisarmColor(Color c)				{ disarmColor = c; }
	public void setArmColor(Color c)				{ armColor = c; }
	public boolean contains(MouseEvent e)			{ return contains(e.getX(), e.getY()); }
	public static void sop(Object x)				{ System.out.println(x); }
	
	
	public abstract boolean contains(int x, int y);
}
