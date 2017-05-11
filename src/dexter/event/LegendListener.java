package dexter.event;

public interface LegendListener<T> 
{
	public void legendStateChanged(LegendEvent<T> e);
}
