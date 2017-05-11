package dexter.util;

import java.util.Vector;


//
// Maps all members of interpolateUs onto range defined by range0-range1. 1st and last members of interpolateUs
// map to range0 and range1.
//

public class BatchInterpolator extends Vector<Float>
{	
	public BatchInterpolator(Vector<Float> interpolateUs, float range0, float range1)
	{
		this(interpolateUs, range0, range1, false);
	}
	
	
	public BatchInterpolator(Vector<Float> interpolateUs, float range0, float range1, boolean verbose)
	{
		if (interpolateUs.size() < 3)
			throw new IllegalArgumentException("Interpolation requires at least 3 values");
		if (range0 >= range1)
			throw new IllegalArgumentException("Range must be low-to-high (saw " + range0 + "-" + range1 + ")");
		
		if (verbose)
		{
			String s = "***********\nInterpolating ";
			for (Float f: interpolateUs)
				s += f + " / ";
			s += "onto " + range0 + " - " + range1;
			sop(s);
		}
		
		add(range0);

		float domain0 = interpolateUs.firstElement();
		float domain1 = interpolateUs.lastElement();
		float domainSpan = domain1 - domain0;
		float rangeSpan = range1 - range0;
		for (int i=1; i<interpolateUs.size()-1; i++)
		{
			float f = interpolateUs.get(i);
			float penetrationFraction = (f - domain0) / domainSpan;
			float interpolated = range0 + penetrationFraction*rangeSpan;
			if (verbose)
				sop("  " + f + ": penetration=" + penetrationFraction + " -=> " + interpolated);
			add(interpolated);
		}
		
		add(range1);
	}
	
	
	static void sop(Object x)			{ System.out.println(x); }
	
	
	public static void main(String[] args)
	{
		Vector<Float> interpolateUs = new Vector<Float>();
		interpolateUs.add(11f);
		interpolateUs.add(13f);
		interpolateUs.add(22f);
		BatchInterpolator bi = new BatchInterpolator(interpolateUs, 11, 15, true);
	}
}
