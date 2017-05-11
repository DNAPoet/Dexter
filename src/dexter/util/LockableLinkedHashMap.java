package dexter.util;

import java.util.*;


public class LockableLinkedHashMap<K, V> extends LinkedHashMap<K, V>
{
	private boolean			locked;
	
	
	public void lock()
	{
		this.locked = true;
	}
	
	
	public boolean isLocked()
	{
		return locked;
	}
	
	
	public void clear()
	{
		tossIfLocked();
		super.clear();
	}
	
	
	public V put(K k, V v)
	{
		tossIfLocked();
		return super.put(k, v);
	}
	
	
	public void putAll(Map<? extends K,? extends V> src)
	{
		tossIfLocked();
		super.putAll(src);
	}
	
	
	public V remove(Object key)
	{
		tossIfLocked();
		return super.remove(key);
	}
	
	
	private void tossIfLocked()
	{
		if (locked)
			throw new IllegalStateException("Attempt to modify a locked map.");
	}
	
	
	public static void sop(Object x)
	{
		System.out.println(x);
	}
}
