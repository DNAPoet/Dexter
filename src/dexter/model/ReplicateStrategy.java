package dexter.model;

import dexter.util.StringUtils;


public enum ReplicateStrategy 
{
	First_replicate, Mean_of_replicates;
	
	
	public String toString()
	{
		return StringUtils.enumConstToPresentableName(name());
	}
}
