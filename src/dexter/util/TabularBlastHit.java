package dexter.util;

import java.util.*;


// 
// Tabular (-m 8) blast output is e.g.
// SCRPIER:1:1101:14543:1467#NGTAGC/1	646312021	85.47	117	17	0	35	151	1291015	1290899	8e-22	97.6
// Fields are query, subject, %ident, length, mismatches, gap opens, q start, q end, s start, s end, e, score
//

public class TabularBlastHit implements Comparable<TabularBlastHit>
{
	public String				query;
	public String 				subject;
	public float				pctIdent;
	public int					length;
	private int					mismatches;
	private int					gapOpens;
	public double				e;
	public int					queryStart;
	public int					queryEnd;
	public int					subjectStart;
	public int					subjectEnd;
	public float				score;
	
	
	private enum Field
	{
		QUERY, SUBJECT, IDENT, LENGTH, MISMATCHES, GAP_OPENS, QSTART, QEND, SSTART, SEND, E, SCORE;	
	}
	
	
	public TabularBlastHit(String s) throws IllegalArgumentException
	{
		if (s.startsWith("#") || s.startsWith(">"))
			throw new IllegalArgumentException("Unexpected 1st char in " + s);
		String[] pieces = s.split("\\s");
		Vector<String> vec = new Vector<String>();
		for (String piece: pieces)
			if (!piece.trim().isEmpty())
				vec.add(piece);		
		if (vec.size() != Field.values().length)
			throw new IllegalArgumentException("Wrong number of fields in " + s);
		int n = 0;
		try
		{
			query = vec.get(n);
			subject = vec.get(++n);
			pctIdent = Float.parseFloat(vec.get(++n));
			length = Integer.parseInt(vec.get(++n));
			mismatches = Integer.parseInt(vec.get(++n));
			gapOpens = Integer.parseInt(vec.get(++n));
			int q1 = Integer.parseInt(vec.get(++n));
			int q2 = Integer.parseInt(vec.get(++n));
			queryStart = Math.min(q1, q2);
			queryEnd = Math.max(q1, q2);
			int s1 = Integer.parseInt(vec.get(++n));
			int s2 = Integer.parseInt(vec.get(++n));
			subjectStart = Math.min(s1, s2);
			subjectEnd = Math.max(s1, s2);
			e = Double.parseDouble(vec.get(++n));
			score = Float.parseFloat(vec.get(++n));
		}
		catch (NumberFormatException nfx)
		{
			String err = "Can't parse field: " + Field.values()[n] + "\n" +
				"Fields: query, subject, %ident, length, mismatches, gap opens, q start, q end, s start, s end, e, score\n" +
				s + "\n";
			for (int i=0; i<pieces.length; i++)
				err += "\n  " + i + ": " + pieces[i];
			err += "\nNFE message: " + nfx.getMessage();
			assert false : err;	
			throw new IllegalArgumentException(err);
		}
		assert n == Field.values().length - 1;
	}
	
	
	// TODO: consider all fields.
	public int compareTo(TabularBlastHit that)
	{
		if (!this.query.equals(that.query))
			return this.query.compareTo(that.query);
		if (this.length != that.length)
			return (int)Math.signum(this.length - that.length);
		if (this.pctIdent != that.pctIdent)
			return (int)Math.signum(this.pctIdent - that.pctIdent);
		if (!this.subject.equals(that.subject))
			return this.subject.compareTo(that.subject);
		if (this.e != that.e)
			return (int)Math.signum(that.e - this.e);
		return this.hashCode() - that.hashCode();
	}
	
	
	public boolean meetsCriteria(float pctIdent, int length)
	{
		return this.pctIdent >= pctIdent  &&  this.length >= length;
	}
	
	
	public String toString()
	{
		return "Query=" + query + ", Sbjct=" + subject + ", %ident=" + pctIdent + " over " + length +
			" at " + queryStart + "-" + queryEnd + ", e-value=" + e;
	}
	
	
	public int queryLength()
	{
		return Math.abs(queryEnd - queryStart) + 1;
	}
	
	
	public int subjectLength()
	{
		return Math.abs(subjectEnd - subjectStart) + 1;
	}
	
	
	static void sop(Object x)		{ System.out.println(x); }
}
