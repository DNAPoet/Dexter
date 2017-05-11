package dexter.view.graph;

import java.util.*;
import java.awt.Color;
import java.io.*;


public class GraphBackgroundModel implements java.io.Serializable
{
	private static final long 			serialVersionUID 	= 2842464658853221612L;
	
	private final static Color			DL_DARK_COLOR		= Color.LIGHT_GRAY;
	private final static Color			DL_LIGHT_COLOR		= Color.WHITE;
	
	private int							duration;			// hours
	private boolean						startsDark = true;
	private Map<GraphBackgroundStyle, Stack<Integer>>
										styleToPhaseChanges = new HashMap<GraphBackgroundStyle, Stack<Integer>>();
	private Vector<Color>				treatmentColors;

	
	public GraphBackgroundModel(int duration)				
	{ 
		this.duration = duration; 
	}
	
	
	// Either or both nPhases args can be 0 to override distribution.
	public GraphBackgroundModel(int duration, int nDLPhases, int nTreatmentPhases)
	{
		this(duration);
		if (nDLPhases > 1)
			distributePhaseChanges(GraphBackgroundStyle.DL, nDLPhases);
		if (nTreatmentPhases > 1)
			distributePhaseChanges(GraphBackgroundStyle.TREATMENT, nTreatmentPhases);
	}
	
	
	public String toString()
	{
		String s = "GraphBackgroundModel duration = " + duration;
		for (GraphBackgroundStyle style: GraphBackgroundStyle.values())
		{
			s += "   ... " + style + ": ";
			if (getUsesStyle(style))
				for (Integer i: getPhaseChanges(style))
					s += i + " ";
			else
				s += "not used";
		}
		return s;
	}
	
	
	public void distributePhaseChanges(GraphBackgroundStyle style, int nPhases)
	{
		assert nPhases <= duration;
		
		Stack<Integer> stack = styleToPhaseChanges.get(style);
		if (stack == null)
		{
			stack = new Stack<Integer>();
			styleToPhaseChanges.put(style, stack);
		}
		
		stack.clear();
		int phaseDuration = duration / nPhases;
		for (int i=1; i<nPhases; i++)
			stack.push(phaseDuration * i);		// last phase might be longer
	}
	
	
	public int[] getToleranceForChange(GraphBackgroundStyle style, int index)
	{
		int[] ret = new int[2];
		Stack<Integer> stack = styleToPhaseChanges.get(style);
		
		// Lower bound: 1 greater than next left neighbor.
		if (index == 0)
			ret[0] = 1;
		else
			ret[0] = stack.get(index-1) + 1;
		
		// Upper bound: 1 less than next right neighbor.
		if (index == stack.size()-1)
			ret[1] = duration - 1;
		else
			ret[1] = stack.get(index+1) - 1;

		return ret;
	}
	
	
	public void adjustChangeAt(GraphBackgroundStyle style, int index, int newValue)
	{
		Stack<Integer> stack = styleToPhaseChanges.get(style);
		assert index < stack.size();
		int[] tolerances = getToleranceForChange(style, index);
		assert newValue >= tolerances[0]  &&  newValue <= tolerances[1];
		stack.set(index, newValue);
	}
	
	
	// Typically called when user changes # of phases via GUI.
	public void adjustNPhases(GraphBackgroundStyle style, int newNPhases)
	{
		assert newNPhases >= 0;
		
		// Special case: zero phases means style isn't used.
		if (newNPhases == 0)
		{
			if (styleToPhaseChanges.containsKey(style))
				styleToPhaseChanges.remove(style);
			return;
		}
		
		// Make sure there's a stack to adjust.
		if (!styleToPhaseChanges.containsKey(style))
			styleToPhaseChanges.put(style, new Stack<Integer>());
		
		// Reduce # of phases from late end.
		Stack<Integer> changes = styleToPhaseChanges.get(style);
		while (changes.size() + 1 > newNPhases)
			changes.pop();
		
		// Add changes. Ignore current pattern and just distribute evenly.
		if (changes.size() + 1 < newNPhases)
		{
			distributePhaseChanges(style, newNPhases);
		}
	}
	
	
	public void setPhaseChangeAtIndex(GraphBackgroundStyle style, int hour, int index)
	{
		setPhaseChangeAt(styleToPhaseChanges.get(style), hour, index);
	}
	
	
	private void setPhaseChangeAt(Stack<Integer> stack, int hour, int index)
	{
		assert hour <= duration;
		stack.set(index, hour);
	}
	
	
	public void reduceNChangesDropFromRight(GraphBackgroundStyle style, int newNChanges)
	{
		Stack<Integer> changes = styleToPhaseChanges.get(style);
		assert changes != null  &&  newNChanges <= changes.size();
		changes.setSize(newNChanges);
	}
	
	
	public boolean getUsesStyle(GraphBackgroundStyle style)
	{
		return styleToPhaseChanges.containsKey(style);
	}	
	
	
	public Stack<Integer> getPhaseChanges(GraphBackgroundStyle style)
	{
		return styleToPhaseChanges.get(style);
	}
	
	
	public int getNPhases(GraphBackgroundStyle style)
	{
		Stack<Integer> changes = styleToPhaseChanges.get(style);
		return (changes == null)  ?  0  :  changes.size() + 1;
	}
	
	
	public void serialize(File f) throws IOException
	{
		FileOutputStream fos = new FileOutputStream(f);
		ObjectOutputStream oos = new ObjectOutputStream(fos);
		oos.writeObject(this);
		oos.flush();
		fos.flush();
		oos.close();
		fos.close();
	}
	
	
	// Useful for creating debug instances of higher-level classes.
	public static GraphBackgroundModel deserialize(File serf)
	{
		try
		{
			FileInputStream fis = new FileInputStream(serf);
			ObjectInputStream ois = new ObjectInputStream(fis);
			GraphBackgroundModel ret = (GraphBackgroundModel)ois.readObject();
			ois.close();
			fis.close();
			return ret;
		}
		catch (Exception x)
		{
			sop("STRESS: " + x.getMessage());
			x.printStackTrace();
			return null;
		}
	}

	
	static void sop(Object x)								{ System.out.println(x); }
	public int getDuration()								{ return duration; }
	public void setDuration(int d)							{ duration = d; }
	public boolean getStartsDark()							{ return startsDark; }
	public void setStartsDark(boolean b)					{ startsDark = b; }
	public void setTreatmentColors(Vector<Color> colors)	{ this.treatmentColors = colors; }
	public void setTreatmentColor(Color color, int n)		{ treatmentColors.set(n, color); }
	public Vector<Color> getTreatmentColors()				{ return treatmentColors; }
	public static Color	getDLDarkColor()					{ return DL_DARK_COLOR; }
	public static Color	getDLLightColor()					{ return DL_LIGHT_COLOR; }
}
