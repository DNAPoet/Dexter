package dexter.cluster.tune;

import java.util.*;

import dexter.cluster.*;
import dexter.util.*;


//
// Might not be efficient enough for heavy lifting.
//


public class DualKeyDistanceMatrix<T> extends DualKeyMap<T, Float> implements DistanceMatrix<T>
{

	public float getDistance(T t1, T t2) throws IllegalArgumentException
	{
		Float f = get(t1, t2);
		if (f != null)
			return f;
		f = get(t2, t1);
		if (f == null)
			throw new IllegalArgumentException("No distance between " + t1 + " and " + t2);
  		return f;
	}

	
	public void setDistance(T t1, T t2, float distance) 
	{
		put(t1, t2, distance);
	}
	
	
	public Collection<T> keys()
	{
		Set<T> ret = new HashSet<T>();
		ret.addAll(keySet());
		for (Map<T, Float> submap: values())
			ret.addAll(submap.keySet());
		return ret;
	}
	
	
	public int nKeys()
	{
		return keys().size();
	}
	
	
	public String toString()
	{
		String s = "DualKeyDistanceMatrix:";
		Collection<T> keys = keys();
		for (T t1: keys)
			for (T t2: keys)
				if (t1 != t2  &&  containsKey(t1)  &&  get(t1).containsKey(t2))
					s += "\n  " + t1 + " <--> " + t2 + " @ " + getDistance(t1, t2);
		return s;
	}
}
