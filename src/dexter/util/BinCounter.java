package dexter.util;

import java.util.*;


//
// Key can be anything. Value is a size-1 array containing the count.
//


public class BinCounter<K> extends TreeMap<K, int[]> implements java.io.Serializable
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1792236704634573227L;


	public BinCounter()				{ }
	
	
	public BinCounter(Collection<BinCounter<K>> srces)
	{
		for (BinCounter<K> src: srces)
			add(src);
	}
	
	
	public void add(BinCounter<K> that)
	{
		for (K bin: that.keySet())
			bumpCountForBin(bin, that.getCountForBin(bin));
	}
	
	
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
	
	
	public void bumpCountForBin(K bin, int delta)
	{
		int[] count = get(bin);
		if (count == null)
			put(bin, new int[]{delta});
		else
			count[0] += delta;
	}
	
	
	public int getCountForBin(K bin)
	{
		return get(bin)[0];
	}
	
	
	public int getCountForBinZeroDefault(K bin)
	{
		return containsKey(bin)  ?  getCountForBin(bin)  :  0;
	}
	
	
	public int getIntCountForBin(K bin)
	{
		return(int)get(bin)[0];
	}
	
	
	public int getIntCountForBinZeroDefault(K bin)
	{
		return containsKey(bin)  ?  getIntCountForBin(bin)  :  0;
	}
	
	
	public Vector<K> keySetByPopulation()
	{
		Map<Integer, Vector<K>> popToKeySet = new TreeMap<Integer, Vector<K>>();
		for (K key: keySet())
		{
			int pop = get(key)[0];
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
	
	
	public int getSumOfAllCounts()
	{
		int sum = 0;
		for (int[] intarr: values())
			sum += intarr[0];
		return sum;
	}
	
	
	public void ensureBinExists(K bin)
	{
		if (!containsKey(bin))
			put(bin, new int[1]);
	}
	
	
	public boolean isEmpty()
	{
		return getSumOfAllCounts() == 0;
	}
	
	
	public Vector<Integer> collectCounts()
	{
		Vector<Integer> ret = new Vector<Integer>(size());
		for (int[] iarr: values())
			ret.add(iarr[0]);
		return ret;
	}


	public static void main(String[] args) { System.out.println(Integer.MAX_VALUE); }
}
