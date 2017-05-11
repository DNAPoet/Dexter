package dexter.util.gui;

import java.awt.event.*;
import javax.swing.*;


public class TaggedRadio<T> extends JRadioButton
{
	private T			tag;
	
	
	public TaggedRadio(T tag)
	{
		super(tag.toString());
		this.tag = tag;
		if (tag instanceof ToolTippable)
			setToolTipText(((ToolTippable) tag).getToolTipText());
	}
	
	
	public TaggedRadio(T tag, ItemListener il)
	{
		this(tag);
		addItemListener(il);
	}
	
	
	public TaggedRadio(String label, T tag)
	{
		super(label);
		this.tag = tag;
		if (tag instanceof ToolTippable)
			setToolTipText(((ToolTippable) tag).getToolTipText());
	}
	
	
	public TaggedRadio(String label, T tag, ItemListener il)
	{
		this(label, tag);
		addItemListener(il);
	}
	
	
	public T getTag()		{ return tag; }
}
