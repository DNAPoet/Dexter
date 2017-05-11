package dexter.cluster.tune;

import java.util.Random;


public class PseudoRandomDistanceGenerator 
{
	private float			minDistance;
	private float			distanceSpread;
	private Random 			generator;
				
	
	public PseudoRandomDistanceGenerator(float minDistance, float maxDistance)
	{
		this(minDistance, maxDistance, 0L);
	}
	
	
	public PseudoRandomDistanceGenerator(float minDistance, float maxDistance, long seed)
	{
		assert minDistance < maxDistance;
		
		this.minDistance = minDistance;
		
		distanceSpread = maxDistance - minDistance;
		generator = new Random(seed);
	}
	
	
	public float nextDistance()
	{
		return minDistance + distanceSpread * generator.nextFloat();
	}
	
	
	static void sop(Object x)		{ System.out.println(x); }
	
	
	public static void main(String[] args)
	{
		for (int i=0; i<5; i++)
		{
			PseudoRandomDistanceGenerator gen = new PseudoRandomDistanceGenerator(10, 100);
			String s = "";
			for (int j=0; j<3; j++)
				s += gen.nextDistance() + "  ";
			sop(s);
		}
		sop("DONE");
	}
}
