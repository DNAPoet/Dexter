package dexter.model;

import java.util.*;
import java.io.*;

import dexter.util.*;
import dexter.cluster.Metric;


//
// Keys are values of roles, e.g. pathway names. Genes are grouped by role, and role-to-group
// mappings are ordered by a requested criterion. Within a group, genes are ordered by name
// if name is available, otherwise by id.
//
// Usually used to model genes in a vertical strip.
//
// Order is only guaranteed immediately after construction. To ensure order, this class has a
// lockdown feature that forbids modification after lockdown is invoked. Caller is responsible
// for locking.
//


public class RoleValueToGenesMap extends LockableLinkedHashMap<String, TreeSet<Gene>>
{
	protected Study								study;
	
	
	public RoleValueToGenesMap()	{ }
	
	
	// Just for subclasses.
	protected RoleValueToGenesMap(Study study)	
	{
		this.study = study;
	}
	
	
	public RoleValueToGenesMap(Study study, 
							  GroupGenesBy groupBy, 
							  OrderGeneGroupsBy orderBy,
							  ExpressionRestrictionModel restrictions,
							  Metric metric, 
							  TimeAndExpressionProvider timeAndExpressionProvider)
	{
		this(study, groupBy.getSpreadsheetColumnRole(), orderBy, restrictions, metric, timeAndExpressionProvider);
	}
	
	
	public RoleValueToGenesMap(Study study, 
			  SpreadsheetColumnRole groupBySSColRole, 
			  OrderGeneGroupsBy orderBy,
			  ExpressionRestrictionModel restrictions,
			  Metric metric, 
			  TimeAndExpressionProvider timeAndExpressionProvider)
	{
		assert study != null  :  "null study in RoleNameToGenesMap ctor.";
		
		// Collect by role value. Whether the role is predefined (isa PredefinedSpreadsheetRole) or
		// user defined (isa String) or special (e.g. coregulation), its values are Strings.
		TreeMap<String, TreeSet<Gene>> roleValueToGenes = new TreeMap<String, TreeSet<Gene>>();
		Collection<Gene> filteredGenes = (restrictions == null)  ?  
				study  :  
				study.collectGenesThatPassRestrictions(restrictions);
		for (Gene gene: filteredGenes)
		{
			String sval = gene.getValueForRole(groupBySSColRole);
			if (sval == null  ||  sval.trim().isEmpty())
				continue;
			TreeSet<Gene> genesForSval = roleValueToGenes.get(sval);
			if (genesForSval == null)
			{
				genesForSval = new TreeSet<Gene>();
				roleValueToGenes.put(sval, genesForSval);
			}
			genesForSval.add(gene);
		}
		
		// Discard roles with too few genes.
		discardDeficientGroups(roleValueToGenes, restrictions);
		
		// Insert mappings from roleValueToGenes into this object.
		switch (orderBy)
		{
			case NAME:
				for (String sval: roleValueToGenes.keySet())
					this.put(sval, roleValueToGenes.get(sval));
				break;
				
			case DIFFERENTIAL_EXPRESSION:
				sortIntoSelfByDifferentialExpression(roleValueToGenes, metric, timeAndExpressionProvider);
				break;
				
			case POPULATION:
				Map<Integer, TreeSet<String>> populationToRoleVals = new TreeMap<Integer, TreeSet<String>>();
				for (String roleVal: roleValueToGenes.keySet())
				{
					Integer population = roleValueToGenes.get(roleVal).size();
					if (!populationToRoleVals.containsKey(population))
						populationToRoleVals.put(population, new TreeSet<String>());
					populationToRoleVals.get(population).add(roleVal);
				}
				for (TreeSet<String> roleValsForPopulation: populationToRoleVals.values())
				{
					for (String roleVal: roleValsForPopulation)	
						this.put(roleVal, roleValueToGenes.get(roleVal));
				}
				break;
		}
	}
	
	
	protected void discardDeficientGroups(Map<String, TreeSet<Gene>> nameToGroup, 
										  ExpressionRestrictionModel restrictions)
	{
		int minNumGenes = (restrictions != null)  ?  restrictions.getMinGenesPerThumbnail()  :  1;
		Set<String> deficientGroups = new HashSet<String>();
		for (String name: nameToGroup.keySet())
			if (nameToGroup.get(name).size() < minNumGenes)
				deficientGroups.add(name);
		for (String name: deficientGroups)
			nameToGroup.remove(name);		
	}
	
	
	protected void sortIntoSelfByDifferentialExpression(Map<String, TreeSet<Gene>> unsorted, 
														Metric metric,
														TimeAndExpressionProvider txProvider)
	{
		assert !unsorted.isEmpty()  :  "Empty unsorted cluster map.";
		
		Comparator<String> compa = new DifferentialExpressionComparator(unsorted, metric, txProvider);
		TreeSet<String> svalSorter = new TreeSet<String>(compa);
		for (String sval: unsorted.keySet())
			svalSorter.add(sval);
		for (String sval: svalSorter)
			this.put(sval, unsorted.get(sval));
	}
	
	
	public class DifferentialExpressionComparator implements Comparator<String>
	{
		private Map<String, TreeSet<Gene>> 		svalToGenes;
		private Metric 							metric;
		private TimeAndExpressionProvider 		txProvider;
		
		DifferentialExpressionComparator(Map<String, TreeSet<Gene>> svalToGenes, 
										 Metric metric, 
										 TimeAndExpressionProvider txProvider)
		{
			this.svalToGenes = svalToGenes;
			this.metric = metric;
			this.txProvider = txProvider;
		}
		
		public int compare(String s1, String s2) 
		{
			Set<Gene> genes1 = svalToGenes.get(s1);
			float meanDistance1 = metric.getMeanDistance(genes1, txProvider);
			Set<Gene> genes2 = svalToGenes.get(s2);
			float meanDistance2 = metric.getMeanDistance(genes2, txProvider);
			return (int)Math.signum(meanDistance1-meanDistance2);
		}
	}  // End if inner class DifferentialExpressionComparator
	
	
	public int nGenes()
	{
		int n = 0;
		for (TreeSet<Gene> geneSet: values())
			n += geneSet.size();
		return n;
	}
	
	
	public String toString()
	{
		StringBuilder sb = new StringBuilder("RoleNameToGenesMap, size = " + size() + ":");
		for (String roleName: keySet())
		{
			sb.append("\n  " + roleName + ": { ");
			for (Gene gene: get(roleName))
				sb.append(gene.getBestAvailableName() + ",");
			sb.deleteCharAt(sb.length()-1);
			sb.append(" }");
		}
		return sb.toString();
	}
	
	
	public static RoleValueToGenesMap manufacture(Study study, 
							  					  GroupGenesBy groupBy, 
							  					  OrderGeneGroupsBy orderBy,
							  					  ExpressionRestrictionModel restrictions,
							  					  Metric metric, 
							  					  TimeAndExpressionProvider timeAndExpressionProvider)
	{
		if (groupBy.isSpreadsheetColumnRole())
			return new RoleValueToGenesMap(study, groupBy, orderBy, restrictions, metric, timeAndExpressionProvider);
		else
			return new AppearanceOrderRoleValueToGenesMap(study, orderBy, restrictions, metric, timeAndExpressionProvider);
	}
	
	
	public static void main(String[] args)
	{
		dexter.MainDexterFrame.main(args);
	}
}
