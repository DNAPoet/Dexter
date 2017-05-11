package dexter.interpolate;

import org.apache.commons.math3.analysis.*;
import org.apache.commons.math3.analysis.interpolation.*;


// Source:   http://commons.apache.org/proper/commons-math/userguide/analysis.html

class ApacheTest 
{
	public static void main(String[] args)
	{
		double xs[] = { 0.0, 1.0, 2.0 };
		double ys[] = { 0, 1, 0};
		UnivariateInterpolator interpolator = new SplineInterpolator();
		UnivariateFunction function = interpolator.interpolate(xs, ys);
		for (double x=0; x<2; x+=0.1)
			System.out.println(x + "   " + function.value(x));
	}
}
