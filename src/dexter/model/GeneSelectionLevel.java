package dexter.model;

import dexter.util.StringUtils;


public enum GeneSelectionLevel 
{
	Selected_thumbnails, Selected_datasets, All_selected, All_genes;
	
	
	public String toString()
	{
		return StringUtils.enumConstToPresentableName(name());
	}
}
