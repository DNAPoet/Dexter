package dexter.util.gui;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import javax.swing.*;
import dexter.event.*;


//
// Exit from a child other than the selected one is possible. In fact, it's the reason for this
// class. When multiple instances are vertically adjacent (as in a graph legend), low mouse event
// frequency causes expected events not to happen. This class makes sure than a child is unhighlighted
// even if its mouseExited event doesn't happen.
//
// Intended for legends, so capable of sending legend events.
//


public class VariableBorderCheckboxManager<T> extends HashMap<T, VariableBorderColorTaggedCheckBox<T>>
	implements MouseListener
{
	private T							highlightedTag;
	private Set<LegendListener<T>>		legendListeners = new HashSet<LegendListener<T>>();
	
	
	public void manage(VariableBorderColorTaggedCheckBox<T> cbox)
	{
		cbox.setExternallyManaged(this);
		put(cbox.getTag(), cbox);
	}
	
	
	public void mouseEntered(MouseEvent e)		
	{ 
		// The checkbox is internal to a container that should remain highlighted.
		if (e.getSource() instanceof JCheckBox)
		{
			TaggedCheckBox<T> src = (TaggedCheckBox<T>)e.getSource();
			replaceHighlightedTag(src.getTag());
			updateHighlights();
		}
		
		else
		{
			VariableBorderColorTaggedCheckBox<T> src = (VariableBorderColorTaggedCheckBox<T>)e.getSource();
			replaceHighlightedTag(src.getTag());
			updateHighlights();
		}
	}
	
	
	public void mouseExited(MouseEvent e)		
	{ 
		// Ignore exit from a checkbox.
		if (e.getSource() instanceof JCheckBox)
			return;

		// If the mouse exited any child, then highlighted child should be unhighlighted. 
		replaceHighlightedTag(null);
		updateHighlights();
	}
	
	
	private void replaceHighlightedTag(T newTag)
	{
		LegendEvent<T> event = new LegendEvent<T>(highlightedTag, false, false);
		for (LegendListener<T> listener: legendListeners)
			listener.legendStateChanged(event);
		
		highlightedTag = newTag;
		event = new LegendEvent<T>(highlightedTag, true, true);
		for (LegendListener<T> listener: legendListeners)
			listener.legendStateChanged(event);
	}
	
	
	// Doesn't send legend events.
	public void setHighlightedTag(T newTag)
	{
		highlightedTag = newTag;
		updateHighlights();
	}
	
	
	private void updateHighlights()
	{
		for (T tag: keySet())
			get(tag).setBorderForMouseState(tag==highlightedTag);
	}
	
	
	public void addLegendListener(LegendListener<T> ll)
	{
		legendListeners.add(ll);
	}
	
	
	public void removeLegendListener(LegendListener<T> ll)
	{
		legendListeners.remove(ll);
	}
	
	
	public void addItemListener(ItemListener il)
	{
		for (VariableBorderColorTaggedCheckBox<T> cbox: values())
			cbox.addItemListener(il);
	}
	
	
	public Vector<T> getSelectedTags()
	{
		Vector<T> ret = new Vector<T>();
		for (VariableBorderColorTaggedCheckBox<T> cbox: values())
			if (cbox.isSelected())
				ret.add(cbox.getTag());
		return ret;
	}
	
	
	public void setSelectionForTag(T tag, boolean newState)
	{
		assert containsKey(tag);
		get(tag).getCheckBox().setSelected(newState);
	}
	
	
	public void invertSelectionForTag(T tag)
	{
		assert containsKey(tag);
		JCheckBox cbox = get(tag).getCheckBox();
		cbox.setSelected(!cbox.isSelected());
	}
	
	
	public void selectAll(boolean newState)
	{
		for (VariableBorderColorTaggedCheckBox<T> cbox: values())
			cbox.setSelected(newState);
	}
	
	
	public void mousePressed(MouseEvent e)		{ }
	public void mouseReleased(MouseEvent e)		{ }
	public void mouseClicked(MouseEvent e)		{ }
	public void mouseDragged(MouseEvent e)		{ }
	
	
	public static void main(String[] args)
	{
		JPanel pan = new JPanel(new GridLayout(0, 1));
		VariableBorderCheckboxManager<Integer> mgr = new VariableBorderCheckboxManager<Integer>();
		for (int i=0; i<10; i++)
		{
			VariableBorderColorTaggedCheckBox<Integer> box = 
				new VariableBorderColorTaggedCheckBox<Integer>(i, Color.RED, Color.blue);
			mgr.manage(box);
			pan.add(box);
		}
		JPanel north = new JPanel();
		north.add(pan);
		JFrame frame = new JFrame();
		frame.add(north, BorderLayout.NORTH);
		frame.pack();
		frame.setVisible(true);
	}
}
