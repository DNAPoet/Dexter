package dexter.view.graph;

import java.util.*;
import dexter.event.*;
import dexter.util.gui.ArmState;


//
// Like a button group. At most 1 member can be selected at a time. Manages ThumbnailGraphs
// without requiring them to know about this class.
//


public class ThumbnailSelectionGroup implements ThumbnailListener
{
	private ThumbnailSelectionStrategy			strategy;
	private Collection<ThumbnailGraph>			allGraphs;
	
	
	ThumbnailSelectionGroup(ThumbnailSelectionStrategy strategy)
	{
		this.strategy = strategy;
		allGraphs = new HashSet<ThumbnailGraph>();
	}
	
	
	void setStrategy(ThumbnailSelectionStrategy strategy)
	{
		this.strategy = strategy;
	}
	
	
	void addStrip(ThumbnailStrip strip)
	{
		allGraphs.addAll(strip.getThumbnails());
		for (ThumbnailGraph graph: strip.getThumbnails())
		{
			graph.setMouseArmsAndSelects(true);
			graph.addThumbnailListener(this);
		}
	}


	public void thumbnailSelectionChanged(ThumbnailEvent e) 
	{
		assert e.getArmState() != ArmState.NONE  :  "Got event with NONE arm state: " + e;
		
		if (strategy == ThumbnailSelectionStrategy.NONE)
			return;
		
		// Determine which other graphs are to be disarmed/deselected.
		ThumbnailGraph source = e.getThumbnail();
		Collection<ThumbnailGraph> graphs = allGraphs;
		if (strategy == ThumbnailSelectionStrategy.ONE_PER_COLUMN)
			graphs = source.getStrip().getThumbnails();
		
		// Disarm/deselect. If the source graph got selected, all other graphs get their
		// arm state set to NONE. If the source graph got armed, all other graphs except
		// the selected graph get their arm state set to NONE.
		boolean sourceWasSelected = e.getArmState() == ArmState.SELECTED;
		for (ThumbnailGraph g: graphs)
		{
			if (g == source)
			{
				// Source thumbnail has already taken care of itself.
				continue;
			}
			else
			{
				if (sourceWasSelected)
				{
					// Source got selected, disarm everything else.
					g.setArmState(ArmState.NONE);		// repaints
				}
				else
				{
					// Source got armed, disarm everything else except the current selection.
					assert e.getArmState() == ArmState.ARMED;
					if (g.getArmState() == ArmState.ARMED)
						g.setArmState(ArmState.NONE);
				}
			}
		}
	}
	
	
	void dispose()
	{
		for (ThumbnailGraph graph: allGraphs)
			graph.removeThumbnailListener(this);
	}
	
	
	Collection<ThumbnailGraph> getGraphs()
	{
		return allGraphs;
	}
	

	public void thumbnailRequestedExpansion(ThumbnailEvent e)		{ }
	static void sop(Object x)										{ System.out.println(x); }
	
	
	public static void main(String[] args)
	{
		ThumbnailStrip.main(args);
	}


}
