package dexter.util;

import java.util.*;


public class FloatRange 
{
	public float		min;
	public boolean		minIsInclusive;
	public float		max;
	public boolean		maxIsInclusive;
	
	
	public FloatRange()		{ }
	
	
	public FloatRange(float min, float max)
	{
		this(min, true, max, true);
	}
	
	
	public FloatRange(float min, boolean minIsInclusive, float max, boolean maxIsInclusive)
	{
		this.min = min;
		this.minIsInclusive = minIsInclusive;
		this.max = max;
		this.maxIsInclusive = maxIsInclusive;
	}
	
	
	public FloatRange(String s) throws IllegalArgumentException
	{
		char c0 = s.charAt(0);
		if (c0 != '='  &&  c0 != '>')
			throw new IllegalArgumentException("Illegal 1st char " + c0 + " in " + s);
		minIsInclusive = (c0 == '=');
		
		char cLast = s.charAt(s.length()-1);
		if (cLast != '='  &&  cLast != '<')
			throw new IllegalArgumentException("Illegal last char " + cLast + " in " + s);
		maxIsInclusive = (cLast == '=');
		
		s = s.substring(1, s.length()-1);
		int nComma = s.indexOf(',');
		if (s.lastIndexOf(',') != nComma)
			throw new IllegalArgumentException("Illegal FloatRange argument: " + s);
		
		String smin = s.substring(0, nComma).trim();
		try
		{
			min = Float.parseFloat(smin);
		}
		catch (NumberFormatException x)
		{
			throw new IllegalArgumentException("Illegal FloatRange argument: " + s);
		}
	
		String smax = s.substring(nComma+1).trim();
		try
		{
			max = Float.parseFloat(smax);
		}
		catch (NumberFormatException x)
		{
			throw new IllegalArgumentException("Illegal FloatRange argument: " + s);
		}
	}
	
	
	public String toString()
	{
		String s = "FloatRange: ";
		s += minIsInclusive  ?  "="  :  ">";
		s += min + ", " + max;
		s += maxIsInclusive  ?  "="  :  "<";
		return s;
	}
	
	
	public boolean equals(FloatRange that)
	{
		return this.sameStart(that)  &&  this.sameEnd(that);
	}

	
	public boolean contains(float f)
	{
		if (f < min)
			return false;
		if (f == min  &&  !minIsInclusive)
			return false;
		if (f == max  &&  !maxIsInclusive)
			return false;
		if (f > max)
			return false;
		return true;
	}
	
	
	public boolean startsBefore(FloatRange that)
	{
		if (this.min < that.min)
			return true;
		else if (this.min == that.min)
			return this.minIsInclusive  &&  !that.minIsInclusive;
		else
			return false;
	}
	
	
	public boolean endsAfter(FloatRange that)
	{
		if (this.max < that.max)
			return false;
		else if (this.max == that.max)
			return this.maxIsInclusive  &&  !that.maxIsInclusive;
		else
			return true;
	}
	
	
	public boolean sameStart(FloatRange that)
	{
		return this.min == that.min  &&  this.minIsInclusive == that.minIsInclusive;
	}
	
	
	public boolean sameEnd(FloatRange that)
	{
		return this.max == that.max  &&  this.maxIsInclusive == that.maxIsInclusive;
	}
	
	
	// (For now) This range must properly include entire subtrahend range.
	public Vector<FloatRange> minus(FloatRange that)
	{
		assert this.startsBefore(that)  ||  this.sameStart(that);
		assert this.endsAfter(that)  ||  this.sameEnd(that);
		
		Vector<FloatRange> ret = new Vector<FloatRange>(2);			// contains 1 or 2 instances
		if (this.startsBefore(that))
		{
			// Add a range from this start to that start
			FloatRange lower = new FloatRange();
			lower.min = this.min;
			lower.minIsInclusive = this.minIsInclusive;
			lower.max = that.min;
			lower.maxIsInclusive = !that.maxIsInclusive;
			ret.add(lower);
		}
		if (this.endsAfter(that))
		{
			// Add a range from this start to that start
			FloatRange higher = new FloatRange();
			higher.min = that.max;
			higher.minIsInclusive = !that.minIsInclusive;
			higher.max = this.max;
			higher.maxIsInclusive = this.maxIsInclusive;
			ret.add(higher);
		}
		
		assert ret.size() == 1  ||  ret.size() == 2  ||  (ret.isEmpty()  &&  this.equals(that))  :
			this + "  MINUS  " + that;
		return ret;
	}
	
	
	static void sop(Object x)		{ System.out.println(x); }
	
	
	public static void main(String[] args)
	{
		// =0.0, 24.0=  MINUS  FloatRange: =0.0, 24.0=
		FloatRange fr1 = new FloatRange("=0.0, 24.0=");
		sop(fr1);
		FloatRange fr2 = new FloatRange(">3.141, 10=");
		sop(fr2);
		sop(fr1.minus(fr1));
	}
}
