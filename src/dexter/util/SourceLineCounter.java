package dexter.util;

import java.io.*;


public class SourceLineCounter 
{
	private static int countLinesInFile(File f) throws IOException
	{
		if (!f.getName().endsWith(".java"))
			return 0;
		int nLines = 0;
		FileReader fr = new FileReader(f);
		BufferedReader br = new BufferedReader(fr);
		while (br.readLine() != null)
			nLines++;
		br.close();
		fr.close();
		return nLines;
	}
	
	
	private static int countRecurse(File f) throws IOException
	{
		int nLines = 0;
		String[] kids = f.list();
		for (String kid: kids)
		{
			File kidf = new File(f, kid);
			if (kid.endsWith(".java")  &&  !kid.startsWith("SourceLineCounter")  &&  !kid.startsWith("Old"))
				nLines += countLinesInFile(kidf);
			else if (kidf.isDirectory())
				nLines += countRecurse(kidf);
		}
		return nLines;
	}
	
	
	public static void main(String[] args)
	{
		try
		{
			File f = new File("src/dexter");
			System.out.println(countRecurse(f) + " source lines");
		}
		catch (IOException x) { }
	}
}
