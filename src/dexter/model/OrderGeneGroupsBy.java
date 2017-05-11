package dexter.model;

import dexter.util.StringUtils;


public enum OrderGeneGroupsBy 
{
	NAME, DIFFERENTIAL_EXPRESSION, POPULATION;
	
	
	public String toString()
	{
		return StringUtils.enumConstToPresentableName(name());
	}
}
