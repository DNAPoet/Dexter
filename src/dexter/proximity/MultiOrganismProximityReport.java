package dexter.proximity;

import java.util.*;
import java.io.*;
import dexter.model.*;


//
// Just aggregates proximity reports for multiple organisms.
//


public class MultiOrganismProximityReport extends TreeMap<Organism, OrganismProximityReport>
{
	private Map<Organism, IdListForOrganism> 			organismToIdList;
	
	
	// Exactly 1 of session and organismToGenomeProximityKit should be null. For deployment, the session provides
	// the proximity kits. For testing, avoid creation of a session.
	private MultiOrganismProximityReport(Collection<Gene> genes, 
									     SessionModel session,
									     Map<Organism, ProximityKit> organismToKit)
	{
		assert session == null  ^  organismToKit == null;
		
		// Collect genes by organism.
		Map<Organism, HashSet<Gene>> organismToGenes = new HashMap<Organism, HashSet<Gene>>();
		for (Gene gene: genes)
		{
			Organism org = gene.getStudy().getOrganism();
			if (!organismToGenes.containsKey(org))
				organismToGenes.put(org, new HashSet<Gene>());
			organismToGenes.get(org).add(gene);
		}
		
		// Get proximity file formats for each organism.
		if (organismToKit == null)
			organismToKit = session.getOrganismToProximityKitMap();
		
		// Load genome proximity for each organism.
		organismToIdList = new HashMap<Organism, IdListForOrganism>();
		for (Organism org: organismToGenes.keySet())
		{
			if (!organismToKit.containsKey(org))	
				continue;											// no instrux for loading proximity file for this org'm
			ProximityKit kit = organismToKit.get(org);	
			try
			{
				IdListForOrganism idList = new IdListForOrganism(org, kit);
				organismToIdList.put(org, idList);
			}
			catch (IOException x)
			{
				// Can't load file => skip this organism.
				sop("Can't load proximity file");
			}
		}
		
		// Generate an organism report for each organism for which proximity info was available.
		for (Organism org: organismToIdList.keySet())
		{
			IdListForOrganism idList = organismToIdList.get(org);
			assert idList.getOrganism() != null;
			OrganismProximityReport organismReport = 
				new OrganismProximityReport(organismToGenes.get(org), idList);
			put(org, organismReport);
		}
	}
	
	public MultiOrganismProximityReport(Collection<Gene> genes, SessionModel session)
	{
		this(genes, session, null);
	}
	
	
	public String toString()
	{
		String s = "MultiOrganismProximityReport:\n";
		for (Organism org: keySet())
			s += org.getName() + "\n" + get(org) + "\n*****************\n";
		return s;
	}
	
	
	public IdListForOrganism getIdListForOrganism(Organism org)
	{
		return organismToIdList.get(org);
	}
	
	
	static MultiOrganismProximityReport buildTestInstance()
	{
		int[] deltas = { 1, 1, 1, 2, 3, 4, 5, 6, 100, 1, 100, 1, 2 };
		Set<Gene> genes = new HashSet<Gene>();
		Study proStudy = new Study("Pro", Organism.PRO);
		Study teryStudy = new Study("Tery", Organism.TERY);
		int proIndex = 100;
		int teryIndex = 500;
		for (int delta: deltas)
		{
			String proGeneID = "PMM0" + proIndex;
			Gene proGene = new Gene();
			proGene.setId(proGeneID);
			proGene.setStudy(proStudy);
			genes.add(proGene);
			proIndex += delta;
			String teryGeneID = "Tery_0" + teryIndex;
			Gene teryGene = new Gene();
			teryGene.setId(teryGeneID);
			teryGene.setStudy(teryStudy);
			genes.add(teryGene);
			teryIndex += delta;
		}
		
		Study CrocoStudy = new Study("Croco", Organism.CROCO);
		String[] crocoIds = 
			{ "CwatDRAFT_6751", "CwatDRAFT_6750", "CwatDRAFT_6747", "CwatDRAFT_5990", "CwatDRAFT_5238", "CwatDRAFT_5239" };
		for (String id: crocoIds)
		{
			Gene crocoGene = new Gene();
			crocoGene.setId(id);
			crocoGene.setStudy(CrocoStudy);
			genes.add(crocoGene);
		}
		
		Map<Organism, ProximityKit> organismToProximityKit = new HashMap<Organism, ProximityKit>();
		organismToProximityKit.put(Organism.CROCO, ProximityKit.FOR_CROCO);
		organismToProximityKit.put(Organism.TERY, ProximityKit.FOR_TERY);
		organismToProximityKit.put(Organism.PRO, ProximityKit.FOR_MED4);
		return new MultiOrganismProximityReport(genes, null, organismToProximityKit);
	}
	
	
	static void sop(Object x)					{ System.out.println(x); }
	
	
	public static void main(String[] args)
	{
		try
		{
			sop("START");
			MultiOrganismProximityReport report = buildTestInstance();
			sop(report);
		}
		catch (Exception x)
		{
			sop("Stress: " + x.getMessage());
			x.printStackTrace();
		}
		finally
		{
			sop("DONE");
		}
	}
	

	/***
	public static void main(String[] args)
	{
		dexter.MainDexterFrame.main(args);
	}
	
	***/
}
