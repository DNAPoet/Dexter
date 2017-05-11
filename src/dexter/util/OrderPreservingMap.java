package dexter.util;

import java.util.*;


//
// Preserves insertion order, even when a key is re-inserted after having been deleted.
//

public class OrderPreservingMap<K, V> extends TreeMap<K, V>
{
	private Map<K, Integer>			keyToInsertionPosition;
	
	
	public OrderPreservingMap()
	{
		super(new OrderPreservingMapComparator<K>());
		keyToInsertionPosition = new HashMap<K, Integer>();
		((OrderPreservingMapComparator)comparator()).setInsertionOrderMap(keyToInsertionPosition);
	}
	
	
	public OrderPreservingMap(OrderPreservingMap src)
	{
		super(src);
		this.keyToInsertionPosition = new HashMap<K, Integer>(src.keyToInsertionPosition);
	}
	
	
	public V put(K key, V val)
	{
		// Haven't seen this key before. Put it at the end.
		if (!keyToInsertionPosition.containsKey(key))
		{
			Integer nextInsertionPosition = keyToInsertionPosition.size();
			keyToInsertionPosition.put(key, nextInsertionPosition);
		}
		return super.put(key, val);
	}
	
	
	public String toString()
	{
		String s = "OrderPreservingMap";
		for (K k: keySet())
			s += "\n  " + k + " ==> " + get(k);
		return s;
	}
	
	
	static void sop(Object x)			{ System.out.println(x); }
	
	
	public static void main(String[] args)
	{
		OrderPreservingMap<String, Boolean> that = new OrderPreservingMap<String, Boolean>();
		that.put("One", true);
		that.put("Two", true);
		that.put("Three", true);
		that.put("Four", true);
		that.remove("Two");
		that.put("Two", true);
		sop(that);
	}
}
