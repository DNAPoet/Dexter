package dexter.util;

import java.util.*;


//
// Each "boarding group" has a priority. If p1 < p2, all members of group 1 will appear
// before any members of group 2. Lower means prior.
//
// (Can't extend DualKeyMap, which isa HashMap at the outermost layer.)
//


public class BoardingGroupList<T> extends TreeMap<Integer, HashSet<T>>
{
	public void put(Integer priority, T t)
	{
		HashSet<T> group = get(priority);
		if (group == null)
		{
			group = new HashSet<T>();
			put(priority, group);
		}
		group.add(t);
	}
	
	
	public void deepRemove(T t)
	{
		for (HashSet<T> group: values())
			group.remove(t);
	}
	
	
	public Vector<T> deepCollect()
	{
		Vector<T> vec = new Vector<T>();
		for (HashSet<T> group: values())
			vec.addAll(group);
		return vec;
	}
	
	
	public Iterator<T> deepIterator()
	{
		return deepCollect().iterator();
	}
}
