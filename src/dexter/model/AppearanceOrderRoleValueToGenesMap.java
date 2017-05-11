package dexter.model;

import java.util.*;

import dexter.cluster.Metric;


//
// Partitions genes of a study by order of appearance in the study's spreadsheet. Extends 
// RoleValueToGenesMap to take advantage of polymorphism during thumbnail construction,
// but shares no functionality.
//


public class AppearanceOrderRoleValueToGenesMap extends RoleValueToGenesMap
{
	private final static int				DFLT_PARTITION_SIZE		= 20;
	
	
	public AppearanceOrderRoleValueToGenesMap(Study study, 
			  								  OrderGeneGroupsBy orderBy,
			  								  ExpressionRestrictionModel restrictions,
			  								  Metric metric, 
			  								  TimeAndExpressionProvider timeAndExpressionProvider)
	{
		this(study, DFLT_PARTITION_SIZE, orderBy, restrictions, metric, timeAndExpressionProvider);
	}
	
	
	public AppearanceOrderRoleValueToGenesMap(Study study, 
											  int partitionSize, 
			  								  OrderGeneGroupsBy orderBy,
			  								  ExpressionRestrictionModel restrictions,
			  								  Metric metric, 
			  								  TimeAndExpressionProvider timeAndExpressionProvider)
	{
		super(study);
		assert !study.isExperimentsStudy();
		
		// Collect passing genes.
		Collection<Gene> passingGenes = (restrictions == null)  ?  
			study  :  
			study.collectGenesThatPassRestrictions(restrictions);
		
		// Collect all genes by group. First collect partitions without regard to restrictions.
		Vector<TreeSet<Gene>> unnamedUnrestrictedGroups = new Vector<TreeSet<Gene>>();
		for (int i=0; i<study.size(); i++)
		{
			if (i % partitionSize == 0)
				unnamedUnrestrictedGroups.add(new TreeSet<Gene>());
			unnamedUnrestrictedGroups.lastElement().add(study.get(i));
		}
		
		// Collect groups that pass restrictions. If ordering is lexical, collect directly into this
		// object, otherwise into a temporary collection.
		Map<String, TreeSet<Gene>> dest = (orderBy == OrderGeneGroupsBy.NAME)  ?  
			this  :  
			new HashMap<String, TreeSet<Gene>>();
		int from = 1;
		for (TreeSet<Gene> group: unnamedUnrestrictedGroups)
		{
			int to = from + group.size() - 1;
			String name = indexRangeToGroupName(from, to);
			from = to + 1;
			if (restrictions != null)
			{
				// Simpler would be group.retainAll(passingGenes) but it's too slow.
				Set<Gene> failers = new HashSet<Gene>();
				for (Gene gene: group)
					if (!passingGenes.contains(gene))
						failers.add(gene);
				if (group.size() - failers.size() < restrictions.getMinGenesPerThumbnail())
					continue;
				group.removeAll(failers);
			}
			dest.put(name, group);
		}

		// Sort into this object if necessary.
		switch (orderBy)
		{
			case NAME:
				break;				// groups are already correctly ordered and in this object.
				
			case DIFFERENTIAL_EXPRESSION:
				sortIntoSelfByDifferentialExpression(dest, metric, timeAndExpressionProvider);
				break;
		}
	}
	
	
	private String indexRangeToGroupName(int from, int to)
	{
		String sMaxIndex = "" + (study.size() - 1);
		int len = sMaxIndex.length();
		
		String sFrom = "" + from;
		while (sFrom.length() < len)
			sFrom = "0" + sFrom;
		
		String sTo = "" + to;
		while (sTo.length() < len)
			sTo = "0" + sTo;
		
		return sFrom + "-" + sTo;
	}
}
