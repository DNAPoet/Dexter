package dexter.util;

import java.util.*;


public class DualKeyMap<K, V> extends HashMap<K, HashMap<K, V>>
{
	public void put(K k1, K k2, V v)
	{
		HashMap<K, V> submap = get(k1);
		if (submap == null)
		{
			submap = new HashMap<K, V>();
			put(k1, submap);
		}
		submap.put(k2, v);
	}
	
	
	public V get(K k1, K k2)
	{
		HashMap<K, V> submap = get(k1);
		return (submap == null)  ?  null  :  submap.get(k2);
	}
	
	
	public Collection<K> primaryKeySet()
	{
		return keySet();
	}
	
	
	public Collection<K> secondaryKeySet()
	{
		Set<K> ret = new HashSet<K>();
		for (HashMap<K, V> submap: values())
			ret.addAll(submap.keySet());
		return ret;
	}
	
	
	public Collection<V> deepValues()
	{
		Collection<V> ret = new HashSet<V>();
		for (HashMap<K, V> submap: values())
			ret.addAll(submap.values());
		return ret;
	}
}
