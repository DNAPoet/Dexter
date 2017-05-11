package dexter.util;

import java.util.*;


public class LocalMath 
{
	private LocalMath()		{ }				// static access only
	
	
	public static float mean(Vector<Float> fs)
	{
		float sum = 0f;
		for (Float f: fs)
			sum += f;
		return sum / fs.size();
	}
	
	
	public static float variance(Vector<Float> fs)
	{
		float mean = mean(fs);
		float var = 0f;
		for (Float f: fs)
			var += (f-mean) * (f-mean);
		return var;
	}
	
	
	public static float stddev(Vector<Float> fs)
	{
		return (float)Math.sqrt(variance(fs));
	}
	
	
	public static double logBaseB(double b, double x)
	{
		return Math.log(x) / Math.log(b);
	}
	
	
	public static float logBaseB(float b, float x)
	{
		return (float)logBaseB((double)b, (double)x);
	}
	
	
	// Expression for spreadsheet columns is associated with elapsed time and stored in a
	// Vector<float[]> where expression is float[1].
	public static float getMeanExpression(Vector<float[]> txs)
	{
		float sum = 0f;
		for (float[] tx: txs)
			sum += tx[1];
		return sum / txs.size();
	}


	//
	// Integral of a standard (i.e. mean=0, sd=1) normal distribution from z to infinity.
	//
	// Approximation (from p. App18 of Zar): P = [1-sqrt(1 - exp(-c^2))] / 2
	// where c = z / (1.237 + 0.0249z)
	//
	public static double probGreaterThanXStandardNormal(double x)	
	{
		if (x == 0)
			return 0.5;
		
		else if (x < 0)
			return 1d - probGreaterThanXStandardNormal(-x);
		
		else
		{
			double c = (x / 1.237 + 0.0249*x);
			double cSquared = c * c;
			double p = 1 - Math.sqrt(1 - Math.exp(-cSquared));
			p /= 2d;
			return p;
		}
	}
}
