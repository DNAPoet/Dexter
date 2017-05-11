package dexter.interpolate;

import java.util.Vector;
import org.apache.commons.math3.analysis.*;
import org.apache.commons.math3.analysis.interpolation.*;


public class CubicSplineInterpolater extends Interpolater
{
	// Splining work is delegated to an Apache class.
	private UnivariateFunction			delegate;
	
	
	public CubicSplineInterpolater(Vector<float[]> timeAndExpressionPairs) 
	{
		super(timeAndExpressionPairs);

		double[] xs = new double[timeAndExpressionPairs.size()];
		double[] ys = new double[timeAndExpressionPairs.size()];
		for (int i=0; i<timeAndExpressionPairs.size(); i++)
		{
			float[] farr = timeAndExpressionPairs.get(i);
			xs[i] = farr[0];
			ys[i] = farr[1];
		}
		UnivariateInterpolator interpolator = new SplineInterpolator();
		delegate = interpolator.interpolate(xs, ys);
	}

	
	// Guarantee: time does not define a point in the time-and-expression pairs.
	protected float interpolateStrict(float time) 
	{
		return (float)delegate.value(time);
	}
	
	
	protected int getMinNumTimepoints()
	{
		return 3;
	}
	
	
	public static void main(String[] args)
	{
		sop("Cubic Spline test");
	}
}
