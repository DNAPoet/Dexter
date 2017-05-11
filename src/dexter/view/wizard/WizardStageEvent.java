package dexter.view.wizard;

import java.util.*;


public class WizardStageEvent 
{
	private int				oldIndex;
	private int				newIndex;
	
	
	public WizardStageEvent(int oldIndex, int newIndex)
	{
		this.oldIndex = oldIndex;
		this.newIndex = newIndex;
	}
	
	
	public int[] getOldAndNewIndices()
	{
		return new int[] { oldIndex, newIndex };
	}
	
	
	public String toString()
	{
		return "WizardStageEvent: " + oldIndex + " >-> " + newIndex;
	}
}
