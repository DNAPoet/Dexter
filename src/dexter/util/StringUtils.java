package dexter.util;

import java.util.*;
import java.awt.FontMetrics;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import javax.swing.plaf.SliderUI;
import au.com.bytecode.opencsv.CSVReader;


public class StringUtils 
{
	private StringUtils()		{ }
	
	
	public static String[] splitOnLineBreaks(String splitMe)
	{
		return splitMe.split("\\n");
	}
	
	
	// Returns true if string is in "2L6" format.
	public static boolean isValidXDLXString(String s)
	{
		if (!(s.contains("D") || s.contains("L")))
			return false;
		
		if (s.startsWith("D")  ||  s.startsWith("L"))
			s = "1" + s;
		
		String[] pieces = s.contains("D")  ?  s.split("D")  :  s.split("L");
		if (pieces.length != 2)
			return false;
		for (String piece: pieces)
		{
			try
			{
				Integer.parseInt(piece);
			}
			catch (NumberFormatException x)
			{
				return false;
			}
		}
		return true;
	}
	
	
	public static String hoursToHM(float hours)
	{
		int h = (int)Math.floor(hours);
		int m = (int)Math.round((hours - h) * 60);
		if (m == 0)
			return "" + h;
		String sm = (m < 10)  ?  "0" + m  :  "" + m;
		return h + ":" + sm;
	}
	
	
	//
	// Values in xCenterToText are labels of horizontal positions (the keys), which ideally would appear 
	// along an X axis. If some of the labels are long, there isn't room and some text will overlap. To 
	// circumvent this problem, this method assigns labels to tiers (vertical positions). Returned value 
	// maps horizontal position (key in xCenterToText) to tier. Algorithm is greedy: the lowest non-overlapping 
	// tier is assigned in each case.
	//
	public static Map<Integer, Integer> 
		horizontalLabelsToVerticalTier(TreeMap<Integer, String> xCenterToText, FontMetrics fm)
	{
		Map<Integer, Integer> ret = new TreeMap<Integer, Integer>();
		
		Map<Integer, Integer> tierToNextAvailableX = new TreeMap<Integer, Integer>();
		for (int tier=0; tier<xCenterToText.size(); tier++)
			tierToNextAvailableX.put(tier, -1);		// won't need more than 1 tier per label
		for (Integer xCenter: xCenterToText.keySet())
		{
			String s = xCenterToText.get(xCenter);
			int sw = fm.stringWidth(s);
			int xTextStart = xCenter - sw/2;
			Integer assignedTier = null;
			for (Integer tier: tierToNextAvailableX.keySet())
			{
				if (tierToNextAvailableX.get(tier) < xTextStart)
				{
					assignedTier = tier;
					break;
				}
			}
			assert assignedTier != null;
			int nextXTextStart = xTextStart + sw + 4;
			tierToNextAvailableX.put(assignedTier, nextXTextStart);
			ret.put(xCenter, assignedTier);
		}
		
		assert ret.size() == xCenterToText.size();
		return ret;
	}
	
	
	// E.g. "L6" or "2D5". Or maybe (e.g. in Zinser) "0" - "47". Try-catch is in case
	// of trouble parsing an unanticipated format.
	public static boolean isTimestampString(String parseMe)
	{
		try
		{
			return Double.parseDouble(parseMe) >= 0 ;
		}
		catch (NumberFormatException x) { }
		
		try
		{
			String s = parseMe;
			while (Character.isDigit(s.charAt(0)))
				s = s.substring(1);						// "2D5" => "D5"
			if (s.charAt(0) != 'L'  &&  s.charAt(0) != 'D')
				return false;
			s = s.substring(1);							// "D5" => 5
			if (!StringUtils.isPureIntString(s))
				return false;
			int hour = Integer.parseInt(s);
			return hour >= 0 && hour <= 18;				// Weird light experiments might have long L or D phases
		}
		catch (Exception x)
		{
			sop("Trouble parsing \"" + parseMe + "\": " + x.getMessage());
			x.printStackTrace(System.out);
			System.exit(13);
		}

		assert false  :  "Should never get here";
		return false;
	}
	
	
	public static boolean isPureIntString(String s)
	{
		try
		{
			Integer.parseInt(s);
			return true;
		}
		catch (NumberFormatException x)
		{
			return false;
		}
	}	
	
	
	public static java.util.List<String[]> readCsvRows(File f) throws IOException
	{
		FileReader fr = new FileReader(f);
		CSVReader csvr = new CSVReader(fr);
		java.util.List<String[]> rows = csvr.readAll();
		csvr.close();
		fr.close();
		return rows;
	}
	
	
	// Return type is compatible with readCsvRows.
	public static java.util.List<String[]> readTsvRows(File f) throws IOException
	{
		FileReader fr = new FileReader(f);
		BufferedReader br = new BufferedReader(fr);
		java.util.List<String[]> rows = new Vector<String[]>();
		String line = null;
		while ((line=br.readLine()) != null)
		{
			String[] pieces = line.split("\\t");
			rows.add(pieces);
		}
		fr.close();
		return rows;
	}
	
	
	public static java.util.List<String[]> readTsvOrCsvRows(File f) throws IOException
	{
		if (f.getName().endsWith(".csv"))
			return readCsvRows(f);
		else if (f.getName().endsWith(".tsv"))
			return readTsvRows(f);
		else
			throw new IllegalArgumentException("Not a csv or tsv file: " + f.getAbsolutePath());
	}
	
	
	public static java.util.List<String[]> readCsvOrTsvRows(File f) throws IOException
	{
		return readTsvOrCsvRows(f);
	}
	
	
	public static String toString(String[] sarr)
	{
		String ret = "";
		for (String s: sarr)
			ret += "|" + s;
		return ret.substring(1);
	}
	
	
	public static String enumConstToPresentableName(String s)
	{
		String ret = "";
		String[] pieces = s.split("_"); 
		ret = pieces[0].toUpperCase().charAt(0) + pieces[0].toLowerCase().substring(1);
		
		if (pieces.length > 1)
		{
			for (int i=1; i<pieces.length; i++)
				ret += " " + pieces[i].toLowerCase();
		}
		
		return ret;
	}
	
	
	// Recognizes that a delimiter inside a literal quote shouldn't delimit.
	public static String[] splitHonorQuotes(String splitMe, char delim)
	{
		assert delim != '"';
				
		// Trim off leading/trailing delimiters.
		while (splitMe.charAt(0) == delim)
			splitMe = splitMe.substring(1);
		while (splitMe.charAt(splitMe.length()-1) == delim)
			splitMe = splitMe.substring(0, splitMe.length()-1);
		
		// Locate genuine delimiters.
		Vector<Integer> delimiterIndices = new Vector<Integer>();
		boolean insideQuote = false;
		for (int i=0; i<splitMe.length(); i++)
		{
			char ch = splitMe.charAt(i);
			if (ch == '"')
				insideQuote = !insideQuote;
			else if (ch == delim  &&  !insideQuote)
				delimiterIndices.add(i);				
		}
		delimiterIndices.insertElementAt(new Integer(-1), 0);
		delimiterIndices.add(splitMe.length());
		
		// Split.
		String[] ret = new String[delimiterIndices.size() - 1];
		int n = 0;
		for (int i=0; i<delimiterIndices.size()-1; i++)
			ret[n++] = splitMe.substring(delimiterIndices.get(i)+1, delimiterIndices.get(i+1));
		return ret;
	}
	
	
	public static String unsplit(String[] pieces, char delimiter)
	{
		String ret = "";
		for (String piece: pieces)
			ret += delimiter + piece;
		return ret.substring(1);
	}
	
	
	public static String unsplit(Collection<String> pieces, char delimiter)
	{
		String ret = "";
		for (String piece: pieces)
			ret += delimiter + piece;
		return ret.substring(1);
	}
	
	
	public static Vector<Integer> allIndicesOf(String target, String src)
	{
		Vector<Integer> ret = new Vector<Integer>();
		for (int i=0; i<src.length()-target.length(); i++)
			if (src.substring(i).startsWith(target))
				ret.add(i);
		return ret;
	}
	
	
	public static String readFile(File f) throws IOException
	{
		StringBuilder sb = new StringBuilder();
		FileReader fr = new FileReader(f);
		BufferedReader br = new BufferedReader(fr);
		String line = null;
		while ((line = br.readLine()) != null)
		{
			sb.append(line);
			sb.append('\n');
		}
		br.close();
		fr.close();
		return sb.toString();
	}
	
	
	public static String strip(String src, String removeUs)
	{
		StringBuilder sb = new StringBuilder();
		for (int i=0; i<src.length(); i++)
			if (removeUs.indexOf(src.charAt(i)) < 0)
				sb.append(src.charAt(i));
		return sb.toString();
	}
	
	
	public static void sop(String s)
	{
		System.out.println(s);
	}
	
	
	public static void main(String[] args)
	{
		String s = "abcdef,123,123,\"xyz\",\"xy,z\",1234";
		String[] pieces = splitHonorQuotes(s, ',');
		for (int i=0; i<pieces.length; i++)
			sop(i + ": " + pieces[i]);
	}
}
