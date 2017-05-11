package dexter.interpolate;

import java.util.Vector;


public enum InterpolationAlgorithm
{
	LINEAR, CUBIC_SPLINE;
	
	
	public String toString()
	{
		switch (this)
		{
			case LINEAR:	
				return "Linear";
			case CUBIC_SPLINE:	
				return "Cubic spline";
			default:
				assert false;
				return null;
		}
	}
	
	
	public Interpolater buildInterpolater(Vector<float[]> txs)
	{
		switch (this)
		{
			case LINEAR:	
				return new LinearInterpolater(txs);
			case CUBIC_SPLINE:	
				assert false : "Not implemented yet.";
				return null;
			default:
				assert false;
				return null;
		}
	}
}
