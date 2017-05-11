package dexter.event;

import dexter.model.*;


public class LegendEvent<T> 
{
	private T					tag;
	private boolean				selected;
	private boolean				armed;
	
	
	public LegendEvent(T tag, boolean selected, boolean armed)
	{
		this.tag = tag;
		this.selected = selected;
		this.armed = armed;
	}
	
	
	public String toString()
	{
		String s = "LegendEvent<" + tag.getClass().getName() + ">  tag=" + tag + 
			" selected=" + selected + " armed=" + armed;
		return s;
	}
	
	
	public T getTag()				{ return tag; }
	public boolean isSelected()		{ return selected; }
	public boolean isArmed()		{ return armed; }
}
