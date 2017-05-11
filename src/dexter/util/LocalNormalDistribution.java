package dexter.util;

//
// TODO: delete in favor of Apache Commons version.
//

public class LocalNormalDistribution 
{
	private double				mean;
	private double				stdDev;
	private String				name;
	
	
	public LocalNormalDistribution(double mean, double stdDev)
	{
		this.mean = mean;
		this.stdDev = stdDev;
	}
	
	
	public String toString()
	{
		String s = "NormalDistribution";
		if (name != null)
			s += " " + name;
		s += ": mean = " + mean + ", stddev = " + stdDev;
		return s;
	}
	
	
	public void setName(String name)
	{
		this.name = name;
	}
	
	
	public String getName()
	{
		return name;
	}
	
	
	//
	// Left-hand term: 1 over (sigma root 2PI)
	//
	// Right-hand term: e-to-the-minus { (x-mu)^2 / 2sigma^2) }
	//
	public double fOfX(double x)
	{
		double left = 1d / (stdDev * Math.sqrt(2d * Math.PI));
		
		double exponent = (x-mean) * (x-mean) / (2 * stdDev * stdDev);
		double right = Math.exp(-exponent);
		
		return left * right;
	}
	
	
	public double probXBetweenMinAndMax(double min, double max)
	{
		assert min <= max;
		double minStandardized = (min - mean) / stdDev;
		double maxStandardized = (max - mean) / stdDev;

		double probGEMin = LocalMath.probGreaterThanXStandardNormal(minStandardized);
		double probGEMax = LocalMath.probGreaterThanXStandardNormal(maxStandardized);
				
		double ret = probGEMin - probGEMax;
		assert ret >= 0;
		return ret;
	}
	
	
	public double probXGreaterThanMin(double min)
	{
		double minStandardized = (min - mean) / stdDev;
		return LocalMath.probGreaterThanXStandardNormal(minStandardized);
	}
	
	
	static void sop(Object x)		{ System.out.println(x); }
	
	
	public static void main(String[] args)
	{
		LocalNormalDistribution norm = new LocalNormalDistribution(600, 10);
		for (int nSigmas=1; nSigmas<=3; nSigmas++)
		{
			double low = norm.mean - nSigmas*norm.stdDev;
			double high = norm.mean + nSigmas*norm.stdDev;
			sop(nSigmas + ": " + norm.probXBetweenMinAndMax(low, high));
		}
	}

}
