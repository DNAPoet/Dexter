package dexter.util;


import java.util.*;


//
// Key can be anything. Value is a size-1 array containing the count.
//


public class LongBinCounter<K> extends TreeMap<K, long[]> implements java.io.Serializable
{
	public String toString()
	{
		String s = "BinCounter:";
		for (K key: keySet())
			s += "\n  " + key + ": " + get(key)[0];
		return s;
	}
	
	
	public void bumpCountForBin(K bin)
	{
		bumpCountForBin(bin, 1);
	}
	
	
	public void bumpCountForBin(K bin, long delta)
	{
		long[] count = get(bin);
		if (count == null)
			put(bin, new long[]{delta});
		else
			count[0] += delta;
	}
	
	
	public long getCountForBin(K bin)
	{
		return get(bin)[0];
	}
	
	
	public long getCountForBinZeroDefault(K bin)
	{
		return containsKey(bin)  ?  getCountForBin(bin)  :  0;
	}
	
	
	public Vector<K> keySetByPopulation()
	{
		Map<Long, Vector<K>> popToKeySet = new TreeMap<Long, Vector<K>>();
		for (K key: keySet())
		{
			long pop = get(key)[0];
			Vector<K> keysForPop = popToKeySet.get(pop);
			if (keysForPop == null)
			{
				keysForPop = new Vector<K>();
				popToKeySet.put(pop, keysForPop);
			}
			keysForPop.add(key);
		}
		Vector<K> ret = new Vector<K>();
		for (Vector<K> addMe: popToKeySet.values())
			ret.addAll(addMe);
		return ret;
	}
	
	
	public long getSumOfAllCounts()
	{
		long sum = 0;
		for (long[] longarr: values())
			sum += longarr[0];
		return sum;
	}
	
	
	public void ensureBinExists(K bin)
	{
		if (!containsKey(bin))
			put(bin, new long[1]);
	}
	
	
	public boolean isEmpty()
	{
		return getSumOfAllCounts() == 0L;
	}
	
	
	public Vector<Long> collectCounts()
	{
		Vector<Long> ret = new Vector<Long>(size());
		for (long[] longarr: values())
			ret.add(longarr[0]);
		return ret;
	}
}
