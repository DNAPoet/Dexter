package analysis.arkin;

import java.io.*;
import java.util.*;

import dexter.model.Organism;
import dexter.util.StringUtils;


public class OrganismToIdToStrandMap extends HashMap<Organism, LinkedHashMap<String, Strand>>
{
	private static OrganismToIdToStrandMap			theInstance;
	
	
	public static OrganismToIdToStrandMap getInstance()
	{
		if (theInstance == null)
		{
			try
			{
				theInstance = new OrganismToIdToStrandMap();
			}
			catch (Exception x)
			{
				sop("Couldn't build IdToStrandMap: " + x.getMessage());
				x.printStackTrace(System.out);
				return null;
			}
		}
		return theInstance;
	}
	
	
	private OrganismToIdToStrandMap() throws IOException
	{
		put(Organism.CROCO, loadCroco());
		put(Organism.PRO, loadPro());
		put(Organism.TERY, loadTery());
	}
	
	
	private LinkedHashMap<String, Strand> loadCroco() throws IOException
	{
		// Lines are e.g. 
		// >638428765 CwatDRAFT_6751 hypothetical protein 54..1580(-) [Crocosphaera watsonii WH 8501]
		LinkedHashMap<String, Strand> ret = new LinkedHashMap<String, Strand>();
		File ifile = new File("analysis_data/GenomeInfo/Croco_8501_ORFs_JGI.fa");
		FileReader fr = new FileReader(ifile);
		BufferedReader br = new BufferedReader(fr);
		String line;
		while ((line = br.readLine()) != null)
		{
			if (!line.startsWith(">"))
				continue;
			String id = line.split("\\s")[1];
			assert id.startsWith("CwatDRAFT_");
			int n = line.lastIndexOf('(') + 1;
			Strand strand = Strand.valueOf(line.charAt(n));
			assert line.charAt(n+1) == ')'  :  line.substring(n) + "\n" + line;
			ret.put(id, strand);
		}
		br.close();
		fr.close();
		return ret;
	}
	
	
	private  LinkedHashMap<String, Strand> loadPro() throws IOException
	{		
		// Lines are e.g. 
		//>637448992 PMM0001 174..1331(+)(NC_005072) [Prochlorococcus marinus pastoris CCMP1986]
		LinkedHashMap<String, Strand> ret = new LinkedHashMap<String, Strand>();
		File ifile = new File("analysis_data/GenomeInfo/Pro_MED4_Genes_From_JGI.fa");
		FileReader fr = new FileReader(ifile);
		BufferedReader br = new BufferedReader(fr);
		String line;
		while ((line = br.readLine()) != null)
		{
			if (!line.startsWith(">"))
				continue;
			String id = line.split("\\s")[1];
			assert id.startsWith("PMM")  ||  id.startsWith("RNA")  :  id + "\n" + line;
			int n = line.indexOf('(') + 1;
			Strand strand = Strand.valueOf(line.charAt(n));
			assert line.charAt(n+1) == ')'  :  line.substring(n) + "\n" + line;
			ret.put(id, strand);
		}
		br.close();
		fr.close();
		return ret;
	}
	
	
	// CSV file. Column A is id, column E is strand. CAUTION: Values are double-quote delimited. You
	// can't see it in a spreadsheet, but you can if you open with textedit.
	private LinkedHashMap<String, Strand> loadTery() throws IOException
	{
		LinkedHashMap<String, Strand> ret = new LinkedHashMap<String, Strand>();
		File ifile = new File("data/Studies/terySet_fData.csv");
		FileReader fr = new FileReader(ifile);
		BufferedReader br = new BufferedReader(fr);
		br.readLine();
		String line;
		while ((line = br.readLine()) != null)
		{
			String[] pieces = line.split(",");
			assert pieces.length >= 5;
			String id = pieces[0].trim();
			id = StringUtils.strip(id, "\"");
			String plusOrMinus = pieces[4].trim(); 
			plusOrMinus = StringUtils.strip(plusOrMinus, "\"");
			char chpm = plusOrMinus.charAt(0);
			Strand strand = Strand.valueOf(chpm);
			ret.put(id, strand);
		}
		br.close();
		fr.close();
		return ret;
	}
	
	
	static void sop(Object x)			{ System.out.println(x); }
	
	
	public static void main(String[] args)
	{
		try
		{
			sop("START");
			OrganismToIdToStrandMap that = getInstance();
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
