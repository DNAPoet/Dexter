package dexter.model;

import java.util.*;


//
// Descriptor for restricting visible thumbnail graphs.
//


public class ExpressionRestrictionModel implements java.io.Serializable 
{
	private static final long 	serialVersionUID = -975972955377958763L;
	
	private int					minGenesPerThumbnail;
	private float[]				minMaxOfMean;
	private float[]				minMaxOfExpression;
	private float[]				minMaxOfDeltaExpression;
	
	
	public ExpressionRestrictionModel()
	{
		minGenesPerThumbnail = 1;
		minMaxOfMean = new float[] { 0, 16 };
		minMaxOfExpression = new float[] { 0, 16 };
		minMaxOfDeltaExpression = new float[] { 0, 16 };
	}
	
	
	public String toString()
	{
		return "Restriction: min genes = " + minGenesPerThumbnail + ", mean in " + farrToString(minMaxOfMean) +
				" ... expression in " + farrToString(minMaxOfExpression) + " ... delta expression in " +
				farrToString(minMaxOfDeltaExpression);
	}
	
	
	private static String farrToString(float[] farr)
	{
		return "{" + farr[0] + "," + farr[1] + "}";
	}
	
	
	public int getMinGenesPerThumbnail()
	{
		return minGenesPerThumbnail;
	}
	
	
	public void setMinGenesPerThumbnail(int minGenesPerThumbnail)
	{
		this.minGenesPerThumbnail = minGenesPerThumbnail;
	}
	
	
	public boolean accepts(Vector<Float> expressions)
	{
		float minExpression = Float.MAX_VALUE;
		float maxExpression = Float.MIN_VALUE;
		float totalExpression = 0f;
		
		for (float xpr: expressions)
		{
			if (xpr < minMaxOfExpression[0]  ||  xpr > minMaxOfExpression[1])
				return false;
			minExpression = Math.min(minExpression, xpr);
			maxExpression = Math.max(maxExpression, xpr);
			totalExpression += xpr;
		}
		
		float deltaExpression = maxExpression - minExpression;
		if (deltaExpression < minMaxOfDeltaExpression[0]  ||  deltaExpression > minMaxOfDeltaExpression[1])
			return false;
		
		float meanExpression = totalExpression / expressions.size();
		return meanExpression >= minMaxOfMean[0]  &&  meanExpression <= minMaxOfMean[1];
	}
	
	
	// Expressions are often encapsulated in a Vector<float[]>, where for each timepoint the
	// float[] is { elapsed time, expression }.
	public boolean acceptsTXs(Vector<float[]> txs)
	{
		Vector<Float> expressions = new Vector<Float>(txs.size());
		for (float[] tx: txs)
			expressions.add(tx[1]);
		return accepts(expressions);
	}
	

	public float[] getMinMaxOfMean() 						{ return minMaxOfMean; }
	public void setMinMaxOfMean(float[] farr)				{ minMaxOfMean = farr; }
	public float[] getMinMaxOfExpression() 					{ return minMaxOfExpression; }
	public void setMinMaxOfExpression(float[] farr)			{ minMaxOfExpression = farr; }
	public float[] getMinMaxOfDeltaExpression() 			{ return minMaxOfDeltaExpression; }
	public void setMinMaxOfDeltaExpression(float[] farr)	{ minMaxOfDeltaExpression = farr; }
}
