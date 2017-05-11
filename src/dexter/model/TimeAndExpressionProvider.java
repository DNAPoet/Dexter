package dexter.model;

import java.util.Vector;


public interface TimeAndExpressionProvider 
{
	public Vector<float[]>		getTimeAndExpressionPairsForGene(Gene gene);
}
