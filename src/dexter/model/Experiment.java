package dexter.model;

import java.util.*;
import java.awt.Color;
import dexter.view.graph.*;
import dexter.view.graph.experiment.History;
import dexter.view.graph.experiment.HistoryStep;


//
// Maintains a registry of names, and can generate default names. Before constructing or otherwise
// assigning a name to an experiment, check that the name is available and then register it.
//


public class Experiment extends Vector<Gene> implements Comparable<Experiment>
{
	private static final long 		serialVersionUID 	= 8453812917516330846L;
	
	private static Set<String>		nameRegistry		= new TreeSet<String>();
	private static boolean			verboseRegistry;
	
	private String					name;
	private Set<Graph>				graphs;
	private History					history;
	private boolean 				currentHistoryStepIsOpen;
	private boolean					superclassOpsAffectHistory;

	
	public Experiment()
	{
		this(generateDefaultName());
	}
	
	
	public Experiment(String name)
	{
		setName(name);
		graphs = new HashSet<Graph>();
		history = new History();
		superclassOpsAffectHistory = true;
	}
	
	
	// Experiments not explicitly named have default name "Experiment xxx" where xxx is a serial number.
	// Named experiments come first, followed by defaults which are ordered by SN.
	public int compareTo(Experiment that)
	{
		if (this.name.equals(that.name))
			return 0;

		// One name is assigned, other is default.
		if (nameIsDefault(this.name)  &&  nameIsUserGenerated(that.name))
			return 1;
		if (nameIsUserGenerated(this.name)  &&  nameIsDefault(that.name))
			return -1;
		
		// Both are assigned.
		if (nameIsUserGenerated(this.name)  &&  nameIsUserGenerated(that.name))
			return this.name.compareTo(that.name);
		
		// Both are default. Extract SN and order by that.
		int sn1 = this.extractSNFromDefaultName();
		int sn2 = that.extractSNFromDefaultName();
		return sn1 - sn2;
	}
	
	
	public boolean equals(Object x)
	{
		Experiment that = (Experiment)x;
		return this.name.equals(that.name);
	}
	
	
	public int hashCode()
	{
		return name.hashCode();
	}
	
	
	public boolean graphsAndLegendsContainGene(Gene gene)
	{
		for (Graph graph: graphs)
		{
			if (!graph.containsGene(gene))
				return false;
			if (graph.getLegend() != null)
				if (!graph.getLegend().containsGene(gene))
					return false;
		}
		
		return true;
	}
	
	
	public void addGraph(Graph graph)
	{
		graphs.add(graph);
	}
	
	
	public void removeGraph(Graph graph)
	{
		graphs.remove(graph);
	}
	
	
	public Set<Graph> getGraphs()
	{
		return graphs;
	}
	
	
	public String toString()
	{
		String s = "Experiment " + name + ", " + size() + " genes: ";
		for (Gene gene: this)
			s += gene.getBestAvailableName() + "  ";
		return s;
	}
	

					
	

	
	
				
				
				//////////////////////////////////////////////////////////////
				//                                                          //
				//                         OVERRIDING                       //
				//                                                          //
				//////////////////////////////////////////////////////////////

	
	
	
	public void add(Gene gene, Vector<float[]> timeAndExpressionPairs)
	{
		if (contains(gene))
			return;
		
		// Add to this experiment.
		super.add(gene);
			
		// Record in history. If current step is open, record in current step as one of a series.
		// Otherwise add a new step just for this gene.
		if (superclassOpsAffectHistory)
		{
			if (currentHistoryStepIsOpen)
				assert history.lastElement().representsAdded();
			else
				history.push(new HistoryStep(true));
			history.lastElement().put(gene, timeAndExpressionPairs);
		}
		
		for (Graph graph: graphs)
		{
			graph.addGeneAndDataNonRedundant(gene, timeAndExpressionPairs);	// updates graph and legend
			graph.enforceColorScheme();
			graph.repaint();
		}
	}
	
	
	public boolean remove(Gene gene)
	{
		assert this.contains(gene);
		
		// History.
		if (superclassOpsAffectHistory)
		{
			if (currentHistoryStepIsOpen)
				assert history.lastElement().representsRemoved();
			else
				history.push(new HistoryStep(false));
			history.lastElement().put(gene, null);
		}
		
		// Inform graphs.
		for (Graph graph: graphs)
		{
			graph.removeGene(gene);			// removes from legend as well, if graph has a legend
			graph.enforceColorScheme();
			graph.repaint();
		}
		
		// Remove from this Vector subclass.
		return super.remove(gene);
	}
	
	
	public void clear()
	{
		// History.
		if (superclassOpsAffectHistory)
			history.addClearStep();
		
		// Graphs.
		for (Graph graph: graphs)
		{
			for (Gene gene: this)
			{
				graph.removeGene(gene);			// removes from legend as well, if graph has a legend
			}
			graph.repaint();
		}
		
		// Clear this Vector subclass.
		super.clear();		
	}
	
	
	
	
	
	
			
						
						////////////////////////////////////////////////////////
						//                                                    //
						//                       HISTORY                      //
						//                                                    //
						////////////////////////////////////////////////////////


	
	
	// Subsequent adds/deletes will be recorded in top event in history stack, until next closeHistory() call.
	public void openHistoryStep(boolean adding)
	{
		HistoryStep event = new HistoryStep(adding);
		history.add(event);
		currentHistoryStepIsOpen = true;
	}
	
	
	public void closeHistoryStep()
	{
		currentHistoryStepIsOpen = false;
	}
	
	
	// For a graph derived from another graph (e.g. an experiment initially copied from a non-experiment
	// thumbnail), the first history step should non be undoable. That's probably the only legitimate
	// use of this method.
	public void setHistoryPrimordial()
	{
		history.setUnpoppable();
	}
	
	
	public boolean canUndo()
	{
		return history.canUndo();
	}
	
	
	public void undo()
	{
		// Undo history.
		assert canUndo();
		history.undo();
		
		// Remove all from this experiment without affecting history. 
		superclassOpsAffectHistory = false;
		clear();
		
		// Add back everything in history up to but not including last step.
		replayHistory();
		
		superclassOpsAffectHistory = true;
	}
	
	
	public boolean canRedo()
	{
		return history.canRedo();
	}
	
	
	// Steps history forward 1, and notifies graphs.
	public void redo()
	{
		// Redo history.
		assert canRedo();
		history.redo();
		
		// Replay last history step.
		superclassOpsAffectHistory = false;
		HistoryStep step = history.peek();
		if (step.representsAdded())
		{
			for (Gene gene: step.keySet())
				add(gene, step.get(gene));
		}
		else
		{
			for (Gene gene: step.keySet())
				remove(gene);
		}
	}
	
	
	private void replayHistory()
	{
		boolean cachedSuperclassOpsAffectHistory = superclassOpsAffectHistory;
		superclassOpsAffectHistory = false;
		
		for (HistoryStep step: history)
		{
			if (step.representsAdded())
			{
				for (Gene gene: step.keySet())
					add(gene, step.get(gene));
			}
			else
			{
				for (Gene gene: step.keySet())
					remove(gene);
			}
		}

		superclassOpsAffectHistory = cachedSuperclassOpsAffectHistory;
	}

	
	
	
	
	
				
				//////////////////////////////////////////////////////////////
				//                                                          //
				//                       NAME REGISTRY                      //
				//                                                          //
				//////////////////////////////////////////////////////////////




	public static void setVerboseRegistry(boolean b)
	{
		verboseRegistry = b;
	}
	
	private static boolean nameIsDefault(String s)
	{
		return s.startsWith("Experiment ");
	}
	
	
	private static boolean nameIsUserGenerated(String s)
	{
		return !nameIsDefault(s);
	}
	
	
	private int extractSNFromDefaultName()
	{
		assert nameIsDefault(name);
		int n = "Experiment ".length();
		return Integer.parseInt(name.substring(n));
	}
	
	
	public String getName()
	{
		return name;
	}
	
	
	public void setName(String name)
	{
		this.name = name;
	}
	
	
	public static String generateDefaultName()
	{
		int max = 0;
		int numIndex = "Experiment ".length();
		for (String name: nameRegistry)
		{
			if (nameIsUserGenerated(name))
			continue;
			int sn = Integer.parseInt(name.substring(numIndex));
			max = Math.max(max, sn);
		}
		max++;
		return "Experiment " + max;
	}
	
	
	public static boolean nameIsAvailable(String name)
	{
		return !nameRegistry.contains(name);
	}
	
	
	private static String registryToString()
	{
		String s = "";
		for (String name: nameRegistry)
			s += name + " ";
		return s.trim();
	}
	
	
	public static void registerName(String name) throws IllegalArgumentException
	{
		if (verboseRegistry)
		{
				sop("-----\nWill register " + name);
				sop("BEFORE: " + registryToString());
		}
	
		if (!nameIsAvailable(name))
			throw new IllegalArgumentException("Name is unavailable: " + name);
	
		nameRegistry.add(name);
	
		if (verboseRegistry)
			sop("AFTER:  " + registryToString());
	}
	
	
	public static void deregisterName(String name)
	{
		if (verboseRegistry)
		{
			sop("-----\nWill deregister " + name);
			sop("BEFORE: " + registryToString());
		}
		
		assert name != null  :  "Attempt to deregister null experiment name";
		
		nameRegistry.remove(name);
		
		if (verboseRegistry)
			sop("AFTER:  " + registryToString());
	}



				
	
	
	
				
				
					//////////////////////////////////////////////////////////////
					//                                                          //
					//                       MISC AND MAIN                      //
					//                                                          //
					//////////////////////////////////////////////////////////////


	
	public String colorsToString()
	{
		String s = "Experiment has " + graphs.size() + " graphs";
		for (Graph graph: graphs)
		{
			s += "-----------\nGraph " + graph.hashCode() + " isa " + graph.getClass().getName();
			Map<Gene, Color> map = graph.getColorMap();
			for (Gene gene: map.keySet())
				s += "\n  " + gene.getId() + "  " + map.get(gene);
		}
		return s;
	}

	
	static void sop(Object x)		{ System.out.println(x); }
	
	
	public static void main(String[] args)
	{
		dexter.MainDexterFrame.main(args);
	}
}
