package dexter.interpolate;

import java.util.*;

import dexter.model.Gene;


public abstract class Interpolater 
{
	protected Vector<float[]>		timeAndExpressionPairs;
	
	
	Interpolater(Vector<float[]> timeAndExpressionPairs)
	{
		assert timeAndExpressionPairs.size() >= getMinNumTimepoints();
		this.timeAndExpressionPairs = timeAndExpressionPairs;
	}
	
	
	// Compatible with toString().
	Interpolater(String s)
	{
		timeAndExpressionPairs = new Vector<float[]>();
		
		String[] pieces = s.split("\\s");
		for (int i=1; i<pieces.length; i++)
		{
			String piece = pieces[i];
			assert piece.startsWith("[")  &&  piece.endsWith("]");
			piece = piece.substring(1, piece.indexOf(']'));
			String[] stx = piece.split(",");
			float[] tx = new float[2];
			for (int j=0; j<2; j++)
				tx[j] = Float.parseFloat(stx[j].trim());
			timeAndExpressionPairs.add(tx);
		}
	}
	
	
	// Compatible with ctor(String).
	public String toString()
	{
		String s = getClass().getName() + ": ";
		for (float[] farr: timeAndExpressionPairs)
			s += "[" + farr[0] + "," + farr[1] + "]" + " ";
		return s.trim();
	}
	
	
	public float interpolate(float time)
	{
		// Quick check for exact time match. Subclasses can be guaranteed that time is strictly
		// between 2 points, or strictly before/after earlier/latest.
		for (float[] tx: timeAndExpressionPairs)
			if (tx[0] == time)
				return tx[1];
		
		return interpolateStrict(time);
	}
	
	
	protected abstract float interpolateStrict(float time);
	
	
	protected abstract int getMinNumTimepoints();
	
	
	public static void sop(Object x)
	{
		System.out.println(x);
	}
	
	
	public static Interpolater createInterpolater(InterpolationAlgorithm interpolation, Vector<float[]> txs)
	{
		switch (interpolation)
		{
			case LINEAR:
				return new LinearInterpolater(txs);
				
			case CUBIC_SPLINE:
				return new CubicSplineInterpolater(txs);
		}
		
		assert false;
		return null;
	}
}
