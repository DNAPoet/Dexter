package dexter.util;

import java.io.*;


public class FastaReader 
{
	private PushbackLineReader			pblr;
	
	
	public FastaReader(Reader src) throws IOException
	{
		pblr = new PushbackLineReader(src);
	}
	
	
	private static boolean isDefline(String s)	{ return s.length() == 0  ||  s.charAt(0) == '>'; }

	
	public String[] readSequence() throws IOException
	{
		boolean skippingBlankLines = true;
		while (skippingBlankLines)
		{
			String line = pblr.readLine();
			if (line == null)
				return null;
			if (line.trim().length() > 0)
			{
				skippingBlankLines = false;
				pblr.push(line);
			}
		}
		String comment = pblr.readLine().trim();
		if (comment == null)
			return null;
		if (!isDefline(comment))
			throw new IllegalArgumentException("Expected comment, found:\n" + comment);
		String seq = "";
		String line = null;
		while ((line = pblr.readLine()) != null)
		{
			if (isDefline(line))
			{
				pblr.push(line);
				return new String[] { comment, seq };
			}
			else
				seq += line.trim();
		}
		
		return new String[] { comment, seq };
	}
	
	
	static void sop(Object x)	{ System.out.println(x); }
	
	
	public static void main(String[] args)
	{
		try
		{
			sop("START");
		}
		catch (Exception x)
		{
			sop("Stress");
		}
	}
}
