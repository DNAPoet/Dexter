package dexter.util;

import java.util.*;


//
// Keys are strings converted to uppercase.
//


public class HashMapIgnoreKeyCase<V> extends HashMap<String, V> 
{
	public V put(String key, V val)
	{
		return super.put(key.toUpperCase(), val);
	}
	
	
	public V get(String key)
	{
		return super.get(key.toUpperCase());
	}
}
