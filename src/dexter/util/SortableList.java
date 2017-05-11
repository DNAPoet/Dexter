package dexter.util;

import java.util.*;


public class SortableList<T extends Comparable<T>> extends Vector<T> implements Comparable<SortableList<T>>
{
	public void sort()
	{
		Set<T> representatives = new HashSet<T>(this);
		Map<T, Vector<T>> representativeToIdenticals = new TreeMap<T, Vector<T>>();
		for (T rep: representatives)
			representativeToIdenticals.put(rep, new Vector<T>());
		for (T member: this)
		{
			for (T rep: representatives)
			{
				if (member.equals(rep))
				{
					representativeToIdenticals.get(rep).add(member);
					break;
				}
			}
		}
		clear();
		for (Vector<T> identicals: representativeToIdenticals.values())
			addAll(identicals);
	}


	public int compareTo(SortableList<T> that) 
	{
		int sizeDiff = this.size() - that.size();
		if (sizeDiff != 0)
			return sizeDiff;
		
		for (int i=this.size()-1; i>=0; i--)
		{
			int memberComp = this.get(i).compareTo(that.get(i));
			if (memberComp != 0)
				return memberComp;
		}
		
		return 0;
	}
	
	
	static void sop(Object x)		{ System.out.println(x); }
	
	
	public static void main(String[] args)
	{
		SortableList<Integer> vec = new SortableList<Integer>();
		for (int i=0; i<5; i++)
			vec.add(100);
		vec.add(1);
		vec.add(555);
		vec.sort();
		for (Integer i: vec)
			sop(i);
	}
}
