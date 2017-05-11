package analysis.arkin;

import java.io.*;
import java.util.*;


public class FracGenesInOperon 
{
	private final static File				DIRF = new File("data/Coregulation");
	private final static File				PRO_GB = new File("data/Proximity/Prochlorococcus_MED4.gb");
	
	
	private static void report(File f) throws IOException
	{
		FileReader fr = new FileReader(f);
		BufferedReader br = new BufferedReader(fr);
		br.readLine();
		String line = null;
		
		Set<String> seenGenes = new HashSet<String>();
		while ((line = br.readLine()) != null)
		{
			if (!line.contains("TRUE"))
				continue;
			String[] pieces = line.split("\\s");
			seenGenes.add(pieces[0]);
			seenGenes.add(pieces[1]);
		}
		sop(f.getName() + " " + seenGenes.size() + " genes in operons");
		
		br.close();
		fr.close();
	}
	
	
	private static int getNGenesFromGBFile(File f) throws IOException
	{
		int nGenes = 0;

		FileReader fr = new FileReader(f);
		BufferedReader br = new BufferedReader(fr);
		String line = null;
		
		while ((line = br.readLine()) != null)
		{
			if (line.trim().startsWith("gene")  &&  line.contains(".."))
				nGenes++;
		}
		
		br.close();
		fr.close();
		return nGenes;
	}
	
	
	static void sop(Object x)				{ System.out.println(x); }
	
	
	public static void main(String[] args)
	{
		try
		{
			sop("START");
			//for (String kid: DIRF.list())
			//{
			//	report(new File(DIRF, kid));
			//}
			sop(getNGenesFromGBFile(PRO_GB));
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
