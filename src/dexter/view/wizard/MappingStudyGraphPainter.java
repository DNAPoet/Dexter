package dexter.view.wizard;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.*;
import dexter.model.*;

import static dexter.util.gui.Armable.*;


class MappingStudyGraphPainter extends UnmappedStudyGraphPainter
{
	private final static Font	FONT 			= new Font("SansSerif", Font.PLAIN, 10);
	private final static int	ARM_WIDTH		= 6;
	
	private Point 				origin;
	private	int 				hPixPerHour;
	private int					heightPix;
	private TimeAssignmentMap	inferredAssignmentMap;
	private Rectangle			bounds;
	private int					armRadius;
	private String				armedTimepoint;
	private boolean				armedTimepointIsSelected;
	
	
	MappingStudyGraphPainter(Point origin, int durationHours, int hPixPerHour, 
							 int heightPix, TimeAssignmentMap assignmentMap)
	{
		super(origin, durationHours, hPixPerHour, heightPix, assignmentMap.values());
		this.origin = origin;
		this.durationHours = durationHours;
		this.hPixPerHour = hPixPerHour;
		this.heightPix = heightPix;
		this.inferredAssignmentMap = new TimeAssignmentMap(assignmentMap);
		
		bounds = new Rectangle(origin.x, origin.y, durationHours*hPixPerHour, heightPix);
		armRadius = (int)Math.floor(hPixPerHour*0.4f);
		armRadius = Math.min(armRadius, 4);		
	}
	
	
	boolean contains(int x, int y)
	{
		return bounds.contains(x, y);
	}
	
	
	boolean contains(MouseEvent me)
	{
		return contains(me.getX(), me.getY());
	}
	
	
	String xyToTimepointName(int x, int y)
	{
		if (!contains(x, y))
			return null;
		
		for (String tpName: inferredAssignmentMap.keySet())
		{
			float xOfTp = origin.x + hPixPerHour*inferredAssignmentMap.get(tpName);
			if (x >= xOfTp-armRadius  &&  x <= xOfTp+armRadius)
				return tpName;
		}
		return null;
	}
	
	
	String xyToTimepointName(MouseEvent me)
	{
		return xyToTimepointName(me.getX(), me.getY());
	}
	
	
	// Null if nothing is armed.
	String getArmedTimepoint()
	{
		return armedTimepoint;
	}
	
	
	// Null to disarm.
	void setArmedTimepoint(String s)
	{
		armedTimepoint = s;
		armedTimepointIsSelected = false;
	}
	
	
	void selectArmedTimepoint()
	{
		assert armedTimepoint != null;
		armedTimepointIsSelected = true;
	}
	
	
	int getXPixOfArmedTimepoint()
	{
		assert armedTimepoint != null;
		float xRel = inferredAssignmentMap.get(armedTimepoint) * hPixPerHour;
		float xAbs = xRel + origin.x;
		return (int)Math.round(xAbs);
	}
	
	
	Point getOrigin()
	{
		return origin;
	}
	
	
	TimeAssignmentMap getTimeAssignmentMap()
	{
		return inferredAssignmentMap;
	}

	
	public void paint(Graphics g)
	{
		super.paint(g);
		
		xformStack.push(g);
		g.translate(originPix.x, originPix.y);

		// Timepoint names.
		int baseline = heightPix + 15;
		g.setColor(Color.BLACK);
		g.setFont(FONT);
		for (String name: inferredAssignmentMap.keySet())
		{
			if (inferredAssignmentMap.get(name) > durationHours)
				continue;
			int xCenter = (int)Math.round(hPixPerHour * inferredAssignmentMap.get(name));
			int sw = g.getFontMetrics().stringWidth(name);
			g.drawString(name, xCenter-sw/2, baseline);
		}
		
		if (armedTimepoint != null)
		{
			g.setColor(DFLT_ARM_COLOR);
			Float hours = inferredAssignmentMap.get(armedTimepoint);
			assert hours != null;
			int xPix = (int)Math.round(hours * hPixPerHour);
			g.fillRect(xPix-ARM_WIDTH/2, 1, ARM_WIDTH+1, heightPix-2);
			g.setColor(Color.BLACK);
			g.drawLine(xPix, 0, xPix, heightPix);
			if (armedTimepointIsSelected)
			{
				g.setColor(DFLT_SEL_COLOR);
				g.drawRect(xPix-ARM_WIDTH/2-1, 1, ARM_WIDTH+2, heightPix-2);
			}
		}
		
		xformStack.pop(g);
	}
}
