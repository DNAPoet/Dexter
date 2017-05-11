package dexter.model;

import java.io.File;
import java.util.*;


//
// Extends study to take advantage of polymorphism but doesn't aggregate any genes. Instead,
// aggregates experiments, which aggregate genes.
//


public class ExperimentsStudy extends Study implements java.io.Serializable
{
	private static final long 					serialVersionUID = -3808196232603856162L;

	
	private Vector<Experiment>					experiments;
	
	
	public ExperimentsStudy()
	{
		experiments = new Vector<Experiment>();
		setName("Experiments");
	}
	
	
	public boolean add(Gene gene) throws UnsupportedOperationException
	{
		throw new UnsupportedOperationException();
	}
	
	
	public boolean remove(Gene gene) throws UnsupportedOperationException
	{
		throw new UnsupportedOperationException();
	}
	
	
	public void addExperiment(Experiment addMe)
	{
		experiments.add(addMe);
	}
	
	
	public void removeExperiment(Experiment removeMe)
	{
		experiments.remove(removeMe);
	}
	
	
	public Vector<Experiment> getExperiments()
	{
		return experiments;
	}
	
	
	public boolean isEmpty()
	{
		return experiments == null  ||  experiments.isEmpty();
	}	
	
	
	public boolean isExperimentsStudy()					
	{
		return true; 
	}	
	
	
	public static ExperimentsStudy buildSimpleTestInstance(SessionModel session)
	{
		ExperimentsStudy ret = new ExperimentsStudy();
		Study shiStudy = null;
		for (Study study: session.getStudies())
			if (study.getName().toUpperCase().contains("SHI"))
				shiStudy = study;
		assert shiStudy != null;
		
		Experiment x1 = new Experiment("Some Croco nifs");
		Set<String> seen = new HashSet<String>();
		for (Gene gene: shiStudy)
		{
			if (gene.getName().toUpperCase().startsWith("NIF"))
			{
				if (!seen.contains(gene.getName()))
				{
					seen.add(gene.getName());
					x1.add(gene);
				}
			}
		}
		ret.addExperiment(x1);
		
		Experiment x2 = new Experiment("DNA metabolism & synthesis");
		for (Study study: session.getStudies())
		{
			for (Gene gene: study)
			{
				String kegg = gene.getValueForPredefinedRole(PredefinedSpreadsheetColumnRole.KEGG_PATHWAY);
				if (kegg == null)
					continue;
				if (kegg.toUpperCase().contains("DNA"))
					if (x2.size() < 12)
						x2.add(gene);
			}
		}
		ret.addExperiment(x2);
		
		return ret;
	}	
	
	
	
	public static void main(String[] args)
	{
		sop("START");
		try
		{
			File serfile = new File("data/Sessions/ProCrocoTery.dex");
			SessionModel session = SessionModel.deserialize(serfile);
			ExperimentsStudy that = buildSimpleTestInstance(session);
			for (Experiment x: that.getExperiments())
			{
				sop("----------------\n" + x);
				for (Gene gene: x)
					sop("  " + gene);
			}
		}
		catch (Exception x)
		{
			sop("Stress: " + x.getMessage());
			x.printStackTrace();
		}
		sop("DONE");
	}
}
