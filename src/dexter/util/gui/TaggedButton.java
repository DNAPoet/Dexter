package dexter.util.gui;

import java.awt.event.*;
import javax.swing.*;


public class TaggedButton<T> extends JButton 
{
	private T		tag;
	
	
	public TaggedButton(T tag)
	{
		this(tag, tag.toString(), null);
	}
	
	
	public TaggedButton(T tag, ActionListener al)
	{
		this(tag, tag.toString(), al);
	}
	
	
	public TaggedButton(T tag, String text, ActionListener al)
	{
		super(text);
		this.tag = tag;
		if (al != null)
			addActionListener(al);
	}
	
	
	public TaggedButton(T tag, String text, ActionListener al, Icon icon)
	{
		super(text, icon);
		this.tag = tag;
		if (al != null)
			addActionListener(al);
	}


    public T getTag()
    {
        return tag;
    }
}