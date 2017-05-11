package dexter.util.gui;

import javax.swing.*;


public class TaggedLabel<T> extends JLabel
{
	private T			tag;
	
	
	public TaggedLabel(String text, T tag)
	{
		super(text);
		this.tag = tag;
	}
	
	
	public T getTag()
	{
		return tag;
	}
}
