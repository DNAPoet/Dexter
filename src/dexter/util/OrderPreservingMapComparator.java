package dexter.util;

import java.util.*;


class OrderPreservingMapComparator<K> implements Comparator<K>, java.io.Serializable
{
	private Map<K, Integer>				keyToInsertionOrder = new HashMap<K, Integer>();
	
	
	public int compare(K k1, K k2)
	{
		assert keyToInsertionOrder.containsKey(k1)  :  "No insertion order for " + k1;
		assert keyToInsertionOrder.containsKey(k2)  :  "No insertion order for " + k2;
		return keyToInsertionOrder.get(k1).compareTo(keyToInsertionOrder.get(k2));
	}
	
	
	void setInsertionOrderMap(Map<K, Integer> keyToInsertionOrder)
	{
		this.keyToInsertionOrder = keyToInsertionOrder;
	}
}
