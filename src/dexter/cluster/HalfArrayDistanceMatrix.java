package dexter.cluster;

import java.util.*;
import dexter.model.*;
import dexter.cluster.tune.*;


//
// Storage is half of a 2d array, with 2nd index > 1st index:
//
// T0-T1	T0-T2	T0-T3	T0-T4	T0-T5	T0-T6	T0-T7	T0-T8	T0-T9 ...
// 			T1-T2	T1-T3	T1-T4	T1-T5	T1-T6	T1-T7	T1-T8	T1-T9 ...
// 			 		T2-T3	T2-T4	T2-T5	T2-T6	T2-T7	T2-T8	T2-T9 ...
//                                   ...
//
// For d(Ta,Tb) the 1st index is just a. The 2nd index is b-a-1.
//
// With heap set to -Xmx2000m, quickly creates a matrix for 10k x 10k. Runs out of heap space for 
// n x n where n ~= 23,000.
//
// If the array is for tree building, the nKeys param should include all internal nodes.
//


public class HalfArrayDistanceMatrix<T> implements DistanceMatrix<T>
{
	private Map<T, Integer>			keyToIndex = new HashMap<T, Integer>();
	private float[][]				theArray;
	
	
	// This ctor version must be followed by a call to setNKeys().
	public HalfArrayDistanceMatrix()		{ }
	
	
	public HalfArrayDistanceMatrix(int nKeys)
	{
		setNKeys(nKeys);
	}
	
	
	public String toString()
	{
		String s = "HalfArrayDistanceMatrix on " + keyToIndex.size() + " keys:\n   ";
		int n = 0;
		for (T key: keyToIndex.keySet())
		{
			s += key + ", ";
			if (n++ == 5)
				break;
		}
		s = s.substring(0, s.length()-1);
		return s;
	}
	
	
	public void setNKeys(int nKeys)
	{
		theArray = new float[nKeys-1][];
		for (int i=0; i<theArray.length; i++)
			theArray[i] = new float[nKeys-1];
	}

	
	private int	getOrCreateIndex(T t)
	{
		Integer i = keyToIndex.get(t);
		if (i == null)
		{
			int size = keyToIndex.size();
			keyToIndex.put(t, size);
			return size;
		}
		else
		{
			return i;
		}
	}
	
	
	public void setDistance(T t1, T t2, float distance) 
	{
		int i1 = getOrCreateIndex(t1); 
		int i2 = getOrCreateIndex(t2); 
		int lowIndex = (i1 < i2)  ?  i1  :  i2;
		int highIndex = (i1 < i2)  ?  i2  :  i1;
		assert lowIndex >= 0  &&  lowIndex < theArray.length  :  
			"lowIndex is " + lowIndex + ", should be in [0:" + (theArray.length-1) + "]";
		assert highIndex > lowIndex  &&  (highIndex-lowIndex-1) < theArray[lowIndex].length  :  
			"highIndex is " + lowIndex + " for lowIndex=" + lowIndex;
		theArray[lowIndex][highIndex-lowIndex-1] = distance;
	}
	
	
	public float getDistance(T t1, T t2) 
	{
		if (t1 == t2)
			return 0;
		
		int i1 = getOrCreateIndex(t1); 
		int i2 = getOrCreateIndex(t2); 
		if (i1 < i2)
			return theArray[i1][i2-i1-1];
		else
			return theArray[i2][i1-i2-1];
	}

	
	public Collection<T> keys() 
	{
		return keyToIndex.keySet();
	}

	
	public int nKeys() 
	{
		return theArray.length;
	}
	
	
	public boolean containsKey(T key)
	{
		return keyToIndex.containsKey(key);
	}
	
	
	// Bypasses key lookup. Random generator uses constant seed=0 for repeatable results.
	public void randomize(Vector<T> keys, float minDistance, float maxDistance)
	{
		int n = 0;
		for (T key: keys)
			keyToIndex.put(key, n++);
		
		n = 0;
		PseudoRandomDistanceGenerator gen = new PseudoRandomDistanceGenerator(minDistance, maxDistance);
		for (int i=0; i<theArray.length; i++)
		{
			for (int j=0; j<theArray[i].length; j++)
			{
				float dist = gen.nextDistance();
				theArray[i][j] = dist;
			}
		}
	}

		
	public static void sop(Object x)			{ System.out.println(x); }
	
	
	public static void main(String[] args)
	{
		/****
		int nKeys = 23000;
		HalfArrayDistanceMatrix<String> mat = new HalfArrayDistanceMatrix<String>(nKeys);
		Vector<String> keys = new Vector<String>(nKeys);
		for (int i=0; i<nKeys; i++)
			keys.add("" + i);
		mat.randomize(keys, 10, 100);
		sop("DONE");
		****/
		
		dexter.MainDexterFrame.main(args);
	}
}
