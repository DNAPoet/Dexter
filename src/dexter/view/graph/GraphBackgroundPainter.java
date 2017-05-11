package dexter.view.graph;

import java.awt.*;
import java.awt.geom.*;
import java.util.*;
import dexter.util.*;
import dexter.util.gui.Paintable;


public class GraphBackgroundPainter implements Paintable
{
	private GraphBackgroundStyle				style;				// only paints for this style
	private GraphBackgroundModel 				backgroundModel;	// hasa map from style to (int) phase changes
	private Rectangle 							bounds;
	private float								hPixPerHour;
	private float								strokeSize;
	private Color[]								customDLDarkAndLightColors;
	
	
	public GraphBackgroundPainter(GraphBackgroundStyle style, GraphBackgroundModel backgroundModel,
	      						  Rectangle bounds, float hPixPerHour)
	{
		this.style = style;
		this.backgroundModel = backgroundModel;
		this.bounds = new Rectangle(bounds);   
		this.hPixPerHour = hPixPerHour;
		
		strokeSize = (hPixPerHour > 12)  ?  2f  :  1f;
	}
	
	
	public void setStrokeSize(float strokeSize)
	{
		this.strokeSize = strokeSize;
	}
	
	
	public void setCustomDLDarkAndLightColors(Color[] customDLDarkAndLightColors)
	{
		this.customDLDarkAndLightColors = customDLDarkAndLightColors;
	}
	
	
	// Public is for the interface.
	public void paint(Graphics g)
	{
		paint(g, style == null);
	}
	
	
	private void paint(Graphics g, boolean outlineOnly)
	{
		if (!backgroundModel.getUsesStyle(style))
			return;
		
		Graphics2D g2 = (Graphics2D)g;
		
		// Fill.
		if (!outlineOnly)
			fill(g2); 
		
		// Outline.
		paintOutline(g2);
	}
	
	
	private void paintOutline(Graphics2D g2)
	{
		g2.setColor(Color.BLACK);
		Stroke entryStroke = g2.getStroke();
		g2.setStroke(new BasicStroke(strokeSize));
		g2.draw(bounds);
		g2.setStroke(entryStroke);
	}
	
	
	private void fill(Graphics2D g2)
	{
		int nPhases = backgroundModel.getNPhases(style);
		if (nPhases == 0)
			return;
		
		// Meez.
		boolean dlNotTreatment = style == GraphBackgroundStyle.DL;
		Vector<Color> fills = null;
		if (dlNotTreatment)  
		{
			fills = new Vector<Color>();
			for (int i=0; i<nPhases; i++)		// more than enough
			{
				fills.add(GraphBackgroundModel.getDLDarkColor());
				fills.add(GraphBackgroundModel.getDLLightColor());
			}
			if (dlNotTreatment  &&  !backgroundModel.getStartsDark()) 
				fills.remove(0);
		}		
		else
		{
			fills = backgroundModel.getTreatmentColors();
		}
		Vector<Integer> phaseChanges = backgroundModel.getPhaseChanges(style);
		assert phaseChanges.size() < fills.size();
		Vector<Integer> phaseDividerXs = new Vector<Integer>();
		phaseDividerXs.add(bounds.x);
		for (Integer phaseChange: phaseChanges)
			phaseDividerXs.add(bounds.x + (int)(phaseChange*hPixPerHour));
		phaseDividerXs.add(bounds.x + bounds.width);
		for (int i=0; i<phaseDividerXs.size()-1; i++)
		{
			g2.setColor(fills.get(i));
			int w = phaseDividerXs.get(i+1) - phaseDividerXs.get(i);
			g2.fillRect(phaseDividerXs.get(i), bounds.y, w, bounds.height);
		}
		g2.setColor(Color.BLACK);
		Stroke entryStroke = g2.getStroke();
		g2.setStroke(new BasicStroke(0));
		for (int i=1; i<phaseDividerXs.size()-1; i++)
			g2.drawLine(phaseDividerXs.get(i), bounds.y, phaseDividerXs.get(i), bounds.y+bounds.height);
		g2.setStroke(entryStroke);
	}
	
	
	public Color getDLDarkColor()
	{
		return (customDLDarkAndLightColors != null)  ? 
		    customDLDarkAndLightColors[0]  :  
		    GraphBackgroundModel.getDLDarkColor();
	}
	
	
	public Color getDLLightColor()
	{
		return (customDLDarkAndLightColors != null)  ? 
		    customDLDarkAndLightColors[1]  :  
		    GraphBackgroundModel.getDLLightColor();
	}
	
	
	public void paintOutline(Graphics g)
	{
		paint(g, true);		// true => outline only
	}
}
