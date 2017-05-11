package dexter.view.graph;

import java.util.*;
import dexter.interpolate.InterpolationAlgorithm;
import dexter.model.*;


public class LargeGraphDisplayConfig 
{
	private Collection<Gene>			selectedGenes;
	private boolean						zeroMean;
	private boolean						hideUnselected;
	private InterpolationAlgorithm		interpolationAlgorithm;
	
	
	LargeGraphDisplayConfig(Collection<Gene> selectedGenes, 
							boolean	zeroMean, 
							boolean	hideUnselected, 
						    InterpolationAlgorithm interpolationAlgorithm)
	{
		this.selectedGenes = selectedGenes;
		this.zeroMean = zeroMean;
		this.hideUnselected = hideUnselected;
		this.interpolationAlgorithm = interpolationAlgorithm;
	}
	
	
	Collection<Gene> getSelectedGenes()
	{
		return selectedGenes;
	}
	
	
	boolean getZeroMean()
	{
		return zeroMean;
	}
	
	
	boolean getHideUnselected()
	{
		return hideUnselected;
	}
	
	
	InterpolationAlgorithm getInterpolationAlgorithm()
	{
		return interpolationAlgorithm;
	}
}
