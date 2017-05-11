package dexter.util.gui;

import java.util.*;

import javax.swing.*;

import java.awt.*;
import java.awt.event.*;

import dexter.MainDexterFrame;
import dexter.view.graph.ThumbnailStrip;


public class ScrollpaneLockstepManager implements AdjustmentListener
{
	private Set<JScrollPane>			members;
	private boolean						enabled;
	
	
	public ScrollpaneLockstepManager()
	{
		members = new HashSet<JScrollPane>();
	}
	
	
	public void setEnabled(boolean b)
	{
		enabled = b;
	}
	
	
	public void add(JScrollPane spane)
	{
		members.add(spane);
		spane.getVerticalScrollBar().addAdjustmentListener(this);
	}
	
	
	public void remove(JScrollPane spane)
	{
		members.remove(spane);
		spane.getVerticalScrollBar().removeAdjustmentListener(this);
	}
	

	public void adjustmentValueChanged(AdjustmentEvent e)
	{
		if (!enabled)
			return;
		if (!e.getValueIsAdjusting())
			return;

		// Meez.
		JScrollBar srcSbar = (JScrollBar)e.getSource();
		JScrollPane srcSpane = (JScrollPane)(srcSbar.getParent());
		
		// Get new fractional position of source.
		JComponent view = (JComponent)srcSpane.getViewport().getView();
		float viewHeight = view.getBounds().height;
		Rectangle viewVisible = view.getVisibleRect();
		float visibleYFrac = viewVisible.y / viewHeight;    
		visibleYFrac = viewVisible.y / (viewHeight-viewVisible.height);
		
		// Adjust all scrollbars except source. 
		for (JScrollPane slaveSpane: members)
		{
			if (slaveSpane == srcSpane)
				continue;
			JComponent slaveView = (JComponent)slaveSpane.getViewport().getView();
			float slaveHeight = slaveView.getBounds().height;
			Rectangle slaveVisible = slaveView.getVisibleRect();
			int newSlaveY = (int)(visibleYFrac * slaveHeight);
			Rectangle newSlaveVisible = new Rectangle(0, newSlaveY, slaveVisible.width, slaveVisible.height);
			//sop("new: " + newSlaveVisible);
			slaveView.scrollRectToVisible(newSlaveVisible);
		}
	}
	
	
	static void sop(Object x)			{ System.out.println(x); }
	
	
	public static void main(String[] args)
	{
		MainDexterFrame.main(args);
	}
}
