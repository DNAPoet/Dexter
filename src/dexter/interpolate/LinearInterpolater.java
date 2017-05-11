package dexter.interpolate;

import java.util.*;

import dexter.model.*;


class LinearInterpolater extends Interpolater
{

	LinearInterpolater(Vector<float[]> timeAndExpressionPairs) 
	{
		super(timeAndExpressionPairs);
	}
	
	
	// String is same format as super.toString().
	public LinearInterpolater(String s)
	{
		super(s);
	}

	
	// Guarantee: time does not define a point in the time-and-expression pairs.
	protected float interpolateStrict(float time) 
	{
		// Before 1st timepoint.
		if (time <= timeAndExpressionPairs.firstElement()[0])
			return interpolate(timeAndExpressionPairs.get(0), time, timeAndExpressionPairs.get(1));
		
		// After last timepoint.
		else if (time >= timeAndExpressionPairs.lastElement()[0])
		{
			int nPoints = timeAndExpressionPairs.size();
			return interpolate(timeAndExpressionPairs.get(nPoints-2), time, timeAndExpressionPairs.get(nPoints-1));
		}
		
		// Between two timepoints.
		for (int n=1; n<timeAndExpressionPairs.size(); n++)
		{
			float[] highPoint = timeAndExpressionPairs.get(n);
			if (time <= highPoint[0])
			{
				float[] lowPoint = timeAndExpressionPairs.get(n-1);
				assert time >= lowPoint[0];
				return interpolate(lowPoint, time, highPoint);
			}
		}
		
		// Shouldn't ever happen.
		assert false;
		return -12345f;
	}

	
	// The underlying arithmetic.
	private static float interpolate(float x0, float y0, float xInterpolateMe, float x1, float y1)
	{
		float fracPenetration = (xInterpolateMe - x0) / (x1 - x0);
		float deltaY = y1 - y0;
		float yPenetration = fracPenetration * deltaY;
		return y0 + yPenetration;
	}
	
	
	private static float interpolate(float[] point0, float xInterpolateMe, float[] point1)
	{
		return interpolate(point0[0], point0[1], xInterpolateMe, point1[0], point1[1]);
	}
	
	
	protected int getMinNumTimepoints()
	{
		return 2;
	}
	
	
	public static void main(String[] args)
	{
		sop("START");
		String s = "dexter.interpolate.LinearInterpolater: [1.0,9.82] [3.0,7.65] [8.0,8.23] [13.0,12.46] " +
			       "[15.000001,12.96] [20.0,11.2] [25.0,8.87] [27.0,7.11]";
		LinearInterpolater lin = new LinearInterpolater(s);
		float f = lin.interpolate(26);
		sop("f=" + f);
	}
}
