package dexter.view.graph;

import java.util.*;

import dexter.model.*;


//
// Abstract superclass for thumbnnail strips that can't be regrouped or resorted (i.e. experiments and clusters).
// 


abstract public class InvariantGroupingThumbnailStrip extends ThumbnailStrip
{
	protected InvariantGroupingThumbnailStrip(SessionModel session)
	{
		this.session = session;
	}
	
	
	// Debug only.
	protected InvariantGroupingThumbnailStrip()		{ }
	
	
	// Supersuperclass, which supports Study strips, clusters by spreadsheet column role value. This
	// class always clusters by tree, and ordering is by appearance order in the tree,
	// so the only restriction that has any effect is the minimum # of genes.
	protected RoleValueToGenesMap mapRoleValuesToGenes(ExpressionRestrictionModel restrictions)
	{
		if (restrictions != null)
		{
			// Restricting.
			RoleValueToGenesMap ret = new RoleValueToGenesMap();
			for (ThumbnailGraph thumb: thumbnails)
			{
				TreeSet<Gene> passingGenes = new TreeSet<Gene>();
				Map<Gene, Vector<float[]>> geneToTx = thumb.getGeneToTimeAndExpressionMap();
				for (Gene gene: geneToTx.keySet())
					if (restrictions.acceptsTXs(geneToTx.get(gene)))
						passingGenes.add(gene);
				if (passingGenes.size() >= restrictions.getMinGenesPerThumbnail())
					ret.put(thumb.getTitle(), passingGenes);				
			}
			
			ret.lock();
			return ret;
		}
		
		else
		{
			// Unrestricting.
			return mapRoleValuesToGenesNoRestrictions();
		}
	}
	
	
	abstract protected RoleValueToGenesMap mapRoleValuesToGenesNoRestrictions();
}
