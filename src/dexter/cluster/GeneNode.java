package dexter.cluster;

import java.util.*;

import dexter.model.*;


//
// Converts a tree or subtree of strings to a tree or subtree of genes. Strings are gene ids. Omits branches to nodes
// whose id isn't represented in the id-to-gene map.
//


public class GeneNode extends Node<Gene>
{
	private boolean			unavailable;
	
	
	private GeneNode()		{ }
	
	static int nUnavailables = 0;
	//
	// Recursively duplicates the branches of the source map, marking leaf nodes whose corresponding gene is 
	// unavailable. A non-leaf is unavailable, and not added to the nascent tree, if all its children are unavailable.
	//
	public GeneNode(Node<String> src, IdToGeneMap idToGeneMap)
	{
		this.distToParent = src.distToParent;
		this.name = src.name;
		
		if (src.isLeaf())
		{
			payload = idToGeneMap.get(src.payload);
			if (payload == null)
				unavailable = true;	
			else
				this.name = payload.getBestAvailableName();		// set name to gene if available, else id (gui displays name)
		}
		else
		{
			for (Node<String> srcKid: src.kids)
			{
				GeneNode kid = new GeneNode(srcKid, idToGeneMap);
				if (kid.unavailable)
					continue;
				kid.parent = this;
				kids.add(kid);
			}
			if (kids.isEmpty())
				unavailable = true;
		}
	}
}