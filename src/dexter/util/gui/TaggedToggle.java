package dexter.util.gui;

import javax.swing.JToggleButton;

public class TaggedToggle<T> extends JToggleButton
{
	private T			tag;
	
	
	public TaggedToggle(String s, T tag)
	{
		super(s);
		this.tag = tag;
	}
	
	
	public T getTag()
	{
		return tag;
	}
}
