package dexter.view.wizard;

import java.util.*;
import java.awt.*;
import java.awt.geom.*;

import dexter.view.graph.GraphBackgroundModel;
import dexter.util.*;
import dexter.util.gui.Paintable;
import dexter.util.gui.TransformStack;


//
// An unmapped study doesn't have its timepoints associated with the reference. Fill is simple off-white.
//


class UnmappedStudyGraphPainter implements Paintable
{
	private final static Color		BG					= new Color(220, 255, 255);
	
	protected Point					originPix;
	protected int					durationHours;
	protected int					hPixPerHour;
	protected int					height;
	protected Collection<Float>		timepoints;			
	protected TransformStack		xformStack;
	protected Color					bg = BG;
	
	
	UnmappedStudyGraphPainter(Point originPix, int durationHours, int hPixPerHour, 
							  int height, Collection<Float> timepoints)
	{
		this.originPix = originPix;
		this.durationHours = durationHours;
		this.hPixPerHour = hPixPerHour;
		this.height = height;
		this.timepoints = new HashSet<Float>(timepoints);	// order doesn't matter, we're just painting v lines
		
		xformStack = new TransformStack();
	}
	
	
	public void setBackground(Color bg)
	{
		this.bg = bg;
	}
	
	
	public void paint(Graphics g)
	{
		xformStack.push(g);
		g.translate(originPix.x, originPix.y);
		
		g.setColor(bg);
		int wPix = durationHours * hPixPerHour;
		g.fillRect(0, 0, wPix, height);
		g.setColor(Color.BLACK);
		g.drawRect(0, 0, wPix, height);
		for (Float timepoint: timepoints)
		{
			assert timepoint >= 0;
			if (timepoint > durationHours)
				continue;
			int xPix = (int)Math.round(timepoint * hPixPerHour);
			g.drawLine(xPix, 0, xPix, height);
		}
		
		xformStack.pop(g);
	}
	
	
	Point getOrigin()
	{
		return originPix;
	}
}
