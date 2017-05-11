package dexter.view.graph.experiment;

import java.util.*;

import org.apache.commons.math3.ode.nonstiff.GillIntegrator;

import dexter.model.*;


public class History extends Stack<HistoryStep>
{
	private Stack<HistoryStep>			redoStack = new Stack<HistoryStep>();
	
	
	public boolean canPop()
	{
		return size() > 0  &&  lastElement().isPoppable();
	}
	
	
	public boolean canUndo()
	{
		return canPop();
	}
	
	
	public HistoryStep pop()
	{
		assert peek().isPoppable()  :  "Attempt to pop history while in unpoppable state.";
		
		return super.pop();
	}
	
	
	public void setUnpoppable()
	{
		for (HistoryStep step: this)
			step.setPoppable(false);
	}
	
	
	public HistoryStep lastStep()
	{
		return lastElement();
	}
	
	
	public void clearRedo()
	{
		redoStack.clear();
	}
	
	
	public void undo()
	{
		assert canPop();
		redoStack.push(pop());
	}
	
	
	public boolean canRedo()
	{
		return !redoStack.isEmpty();
	}
	
	
	public void redo()
	{
		assert canRedo();
		push(redoStack.pop());
	}
	
	
	public boolean add(HistoryStep step)
	{
		redoStack.clear();
		return super.add(step);
	}
	
	
	private class ClearStep extends HistoryStep
	{
		public boolean clears()
		{
			return true;
		}
	}
	
	
	public void addClearStep()
	{
		add(new ClearStep());
	}
	
	
	public int indexOfLastAdditionOfGene(Gene gene)
	{
		for (int n=size()-1; n>=0; n--)
		{
			HistoryStep step = get(n);
			if (step.representsAdded()  &&  step.containsKey(step))
				return n;
		}
		return -1;
	}
}
