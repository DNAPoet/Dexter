package dexter.model;

import java.util.*;
import java.io.*;


//
// Web archive file is http://www.genome.jp/kegg/catalog/org_list.html as of 2/26/2014.
//

public class OrganismNameToKEGGAbbrevMap extends TreeMap<String, String>
{
	private static final long 					serialVersionUID 	= 6613617311659170696L;
	
	private final static File					DIRF				= new File("data/Organisms");
	private final static File					WEBARCHIVE_FILE		= new File(DIRF, "KEGG_organisms.webarchive");
	private final static File					SER_FILE			= new File(DIRF, "OrganismNameToKEGGMap.ser");
	private final static String					ABBREV_OMEN			= "<a href=\"/kegg-bin/show_organism?org=";
	
	private static OrganismNameToKEGGAbbrevMap	theInstance;
	
	
	//
	// Look for pairs of lines like:
	//
	//	  <td align="center"><a href="/kegg-bin/show_organism?org=cfa">cfa</a></td>
	//	  <td align="left"><a href="/dbget-bin/www_bfind?T01007">Canis familiaris (dog)</a></td>
	//
	private static OrganismNameToKEGGAbbrevMap fromWebArchive() throws IOException
	{
		OrganismNameToKEGGAbbrevMap ret = new OrganismNameToKEGGAbbrevMap();
		
		FileReader fr = new FileReader(WEBARCHIVE_FILE);
		BufferedReader br = new BufferedReader(fr);
		String line = null;
		while ((line = br.readLine()) != null)
		{
			int omenIndex = line.indexOf(ABBREV_OMEN);
			if (omenIndex < 0)
				continue;
			line = line.substring(omenIndex + ABBREV_OMEN.length());
			String abbrev = line.substring(0, line.indexOf('"'));
			line = br.readLine();
			line = line.substring(line.indexOf("dbget-bin"));
			line = line.substring(line.indexOf(">") + 1);
			String name = line.substring(0, line.indexOf("<"));
			ret.put(name, abbrev);
		}
		br.close();
		fr.close();
		
		return ret;
	}
	
	
	// Use once.
	private void serialize(File f) throws IOException
	{
		FileOutputStream fos = new FileOutputStream(SER_FILE);
		ObjectOutputStream oos = new ObjectOutputStream(fos);
		oos.writeObject(this);
		oos.flush();
		fos.flush();
		oos.close();
		fos.close();
	}
	
	
	private static OrganismNameToKEGGAbbrevMap deserialize() throws IOException, ClassNotFoundException
	{
		FileInputStream fis = new FileInputStream(SER_FILE);
		ObjectInputStream ois = new ObjectInputStream(fis);
		OrganismNameToKEGGAbbrevMap ret = (OrganismNameToKEGGAbbrevMap)ois.readObject();
		ois.close();
		fis.close();
		return ret;
	}
	
	
	public static OrganismNameToKEGGAbbrevMap getInstance()
	{
		if (theInstance == null)
		{
			try
			{
				theInstance = deserialize();
			}
			catch (Exception x) 
			{
				String err = "Can't deserialize organism name map " + SER_FILE + ": " + x.getMessage();
				throw new IllegalStateException(err);
			}
		}
		
		return theInstance;
	}
	
	
	public String getLongestName()
	{
		String ret = "";
		for (String name: keySet())
			if (name.length() > ret.length())
				ret = name;
		return ret;
	}
	
	
	public String getLongestAbbreviation()
	{
		String ret = "";
		for (String name: values())
			if (name.length() > ret.length())
				ret = name;
		return ret;
	}
	
	
	static void sop(Object x)			{ System.out.println(x); }
	
	
	public static void main(String[] args)
	{				
		try
		{
			//fromWebArchive().serialize(SER_FILE);
			getInstance();
		}
		catch (Exception x)
		{
			sop("Stress: " + x.getMessage());
			x.printStackTrace();
		}
		finally
		{
			sop("DONE");
		}
	}
}
