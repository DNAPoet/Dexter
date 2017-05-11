package analysis.arkin;

import java.io.*;
import java.util.*;

import dexter.util.StringUtils;


public class AnnoCount 
{
	private final static File DIRF 			= new File("analysis_data/Export");
	
	
	static class LocalOperon implements Comparable<LocalOperon>
	{
		
		Integer		index;
		String		id, kegg, anno;
		
		LocalOperon(String csv)
		{
			String[] pieces = StringUtils.splitHonorQuotes(csv, ',');
			assert pieces.length == 5  ||  pieces.length == 6;
			index = Integer.valueOf(pieces[0]);
			id = pieces[1];
			kegg = StringUtils.strip(pieces[3], "\"");
			anno = StringUtils.strip(pieces[pieces.length-1], "\"");
		}
		
		public String toString() { return "" + index + "|" + id + "|" + kegg + "|" + anno; }
		public int compareTo(LocalOperon that) { return this.id.compareTo(that.id); }
	}
	
	
	static void check(String target) throws IOException
	{
		sop("******************************\nChecking " + target);
		File fPrior = new File(DIRF, target+"_Prior_Operons.csv");
		File fMerged = new File(DIRF, target+"_Merged_Operons.csv");
		assert fPrior.exists();
		assert fMerged.exists();
		Map<Integer, TreeSet<LocalOperon>> mergedIndexToOp = getIndexToOp(fMerged);
		Map<Integer, TreeSet<LocalOperon>> priorIndexToOp = getIndexToOp(fPrior);
		Set<String> allIdsInMerged = new TreeSet<String>();
		for (TreeSet<LocalOperon> ops: mergedIndexToOp.values())
		{
			for (LocalOperon op: ops)
			{
				allIdsInMerged.add(op.id);
				sop("MERGED: " + op);
			}
		}
		
		// Dump every gene in a prior that got merged.
		for (TreeSet<LocalOperon> priorOp: priorIndexToOp.values())
		{
			for (LocalOperon op: priorOp)
			{
				if (allIdsInMerged.contains(op.id))
					sop("  PRIOR: " + op);
			}
		}		
	}
	
	
	static Map<Integer, TreeSet<LocalOperon>> getIndexToOp(File f) throws IOException
	{
		Map<Integer, TreeSet<LocalOperon>> ret = new HashMap<Integer, TreeSet<LocalOperon>>();
		
		FileReader fr = new FileReader(f);
		BufferedReader br = new BufferedReader(fr);
		br.readLine();
		String line = null;
		while ((line = br.readLine()) != null)
		{
			LocalOperon op = new LocalOperon(line);
			if (!ret.containsKey(op.index))
				ret.put(op.index, new TreeSet<LocalOperon>());
			ret.get(op.index).add(op);
		}
		
		fr.close();
		br.close();
		
		return ret;
	}
	
	
	static Vector<Integer> findUnquotedCommas(String s)
	{
		Vector<Integer> ret = new Vector<Integer>();
		boolean inQuote = false;
		for (int i=0; i<s.length(); i++)
		{
			char ch = s.charAt(i);
			if (ch == '"')
				inQuote = !inQuote;
			else if (ch == ','  &&  !inQuote)
				ret.add(i);
		}
		return ret;
	}
	
	
	static void sop(Object x)				{ System.out.println(x); }
	
	
	public static void main(String[] args)
	{
		try
		{
			sop("START");
			String[] names = { "Croco", /*"Pro", */"Tery" };
			for (String s: names)
				check(s);
		}
		catch (Exception x)
		{
			sop("STRESS: " + x.getMessage());
			x.printStackTrace(System.out);
			System.exit(1);
		}
		sop("Done");
	}
}
