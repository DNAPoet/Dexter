package dexter.util.gui;

import java.awt.*;
import java.util.*;

import dexter.util.LocalMath;



public class PositionedRowLayout extends LayoutAdapter
{
	private final static int		DFLT_V_MARGIN		= 4;
	
	private Vector<Integer>			horizontalCenters;
	private int						vMargin = DFLT_V_MARGIN;
	
	
	public PositionedRowLayout(Vector<Integer>	horizontalCenters)
	{
		this.horizontalCenters = new Vector<Integer>(horizontalCenters);
	}
	
	
    public void layoutContainer(Container parent) 
    {
    	assert parent.getComponentCount() == horizontalCenters.size();
    	
    	for (int i=0; i<parent.getComponentCount(); i++)
    	{
    		Component compo = parent.getComponent(i);
    		Dimension pref = compo.getPreferredSize();
    		compo.setSize(pref);
    		int x = horizontalCenters.get(i) - pref.width/2;
    		compo.setLocation(x, vMargin);
    	}
    }
    
    
    public Dimension preferredLayoutSize(Container parent)
    {
    	assert parent.getComponentCount() == horizontalCenters.size();
    	
    	Component rightmost = parent.getComponent(parent.getComponentCount()-1);
    	int w = horizontalCenters.lastElement() + rightmost.getPreferredSize().width/2 + 4;
    	int h = vMargin + getMaxPrefHeight(parent) + vMargin;
    	return new Dimension(w, h);
    }
    
    
    private int getMaxPrefHeight(Container parent)
    {
    	int max = -1;
    	for (int i=0; i<parent.getComponentCount(); i++)
    	{
    		Component compo = parent.getComponent(i);
    		int pref = compo.getPreferredSize().width;
    		max = Math.max(max, pref);
    	}
    	return max; 
    }
}
