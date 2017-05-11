package dexter.model;

import java.util.*;

import dexter.coreg.*;
import dexter.cluster.Metric;


public class CoregulationRoleValueToGenesMap extends RoleValueToGenesMap
{
	public CoregulationRoleValueToGenesMap(Study study,
										   Collection<CoregulationGroup> coregGroups,
			  							   OrderGeneGroupsBy orderBy,
			  							   ExpressionRestrictionModel restrictions,
			  							   Metric metric, 
			  							   TimeAndExpressionProvider timeAndExpressionProvider)
	{
		super(study);
		assert !study.isExperimentsStudy();
		
		// Collect genes that pass restrictions.
		Collection<Gene> passingGenes = (restrictions == null)  ?  
										study  :  
										study.collectGenesThatPassRestrictions(restrictions);
				
		// Convert coreg groups to collections of genes that pass restrictions.
		Vector<TreeSet<Gene>> restrictedGroups = new Vector<TreeSet<Gene>>();
		Vector<String> providedGroupNames = new Vector<String>();
		Map<String, Gene> idToGene = study.getIdToGeneMap();
		for (CoregulationGroup coregGroup: coregGroups)
		{
			TreeSet<Gene> genes = new TreeSet<Gene>();
			for (String id: coregGroup)
			{
				Gene gene = idToGene.get(id);
				if (gene == null)
					continue;
				if (!passingGenes.contains(gene))
					continue;
				genes.add(gene);
			}
			if (genes.size() < 2)
				continue;
			if (restrictions == null  ||  genes.size() >= restrictions.getMinGenesPerThumbnail())
			{
				restrictedGroups.add(genes);
				providedGroupNames.add(coregGroup.getName());
			}
		}
		
		// Assign names to surviving unnamed groups. 
		Map<String, TreeSet<Gene>> nameSorter = new TreeMap<String, TreeSet<Gene>>();
		int maxGroupNum = coregGroups.size() + 1;
		int nDigits = ("" + maxGroupNum).length();
		int groupNum = 1;
		for (int i=0; i<restrictedGroups.size(); i++)
		{
			String name = providedGroupNames.get(i);
			if (name == null)
			{
				name = "" + groupNum++;
				while (name.length() < nDigits)
					name = "0" + name;
				name = "Unnamed coreg group " + name;
			}	
			nameSorter.put(name, restrictedGroups.get(i));	
		}
						
		// Sort into this object.
		switch (orderBy)
		{
			case NAME:
				for (String name: nameSorter.keySet())
					put(name, nameSorter.get(name));
				break;		

			case DIFFERENTIAL_EXPRESSION:
				sortIntoSelfByDifferentialExpression(nameSorter, metric, timeAndExpressionProvider);
				break;
				
			case POPULATION:
				Map<Integer, TreeSet<String>> populationToNames = new TreeMap<Integer, TreeSet<String>>();
				for (String groupName: nameSorter.keySet())
				{
					Integer population = nameSorter.get(groupName).size();
					if (!populationToNames.containsKey(population))
						populationToNames.put(population, new TreeSet<String>());
					populationToNames.get(population).add(groupName);
				}
				for (TreeSet<String> roleValsForPopulation: populationToNames.values())
				{
					for (String roleVal: roleValsForPopulation)	
						this.put(roleVal, nameSorter.get(roleVal));
				}
				break;
		}
	}
	
	
	public static void main(String[] args)
	{
		dexter.MainDexterFrame.main(args);
	}
}


