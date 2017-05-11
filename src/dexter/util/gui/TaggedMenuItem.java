package dexter.util.gui;

import java.awt.event.*;
import javax.swing.*;


public class TaggedMenuItem<T> extends JMenuItem
{
	private T			tag;
	
	
	public TaggedMenuItem(T tag)
	{
		super(tag.toString());
		this.tag = tag;
	}
	
	
	public TaggedMenuItem(T tag, ActionListener al)
	{
		this(tag);
		addActionListener(al);
	}
	
	
	public T getTag()		{ return tag; }
}
