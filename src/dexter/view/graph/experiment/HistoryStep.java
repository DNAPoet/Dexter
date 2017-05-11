package dexter.view.graph.experiment;

import java.util.*;
import dexter.model.*;


public class HistoryStep extends LinkedHashMap<Gene, Vector<float[]>>
{
	private boolean 			added;				// otherwise removed
	private boolean				poppable = true;
	
	
	public HistoryStep()		{ }
	
	
	public HistoryStep(boolean added)
	{
		this.added = added;
	}
	
	
	public HistoryStep(Map<Gene, Vector<float[]>> initialContents, boolean added)
	{
		putAll(initialContents);
		this.added = added;
	}
	
	
	public HistoryStep(boolean added, Map<Gene, Vector<float[]>> initialContents)
	{
		this(initialContents, added);
	}
	
	
	public boolean representsAdded()
	{
		return added;
	}
	
	
	public boolean representsRemoved()
	{
		return !added;
	}
	
	
	public boolean isPoppable()
	{
		return poppable;
	}
	
	
	public void setPoppable(boolean poppable)
	{
		this.poppable = poppable;
	}
	
	
	// Overridden by a private subclass in History.
	public boolean clears()
	{
		return false;
	}
}
