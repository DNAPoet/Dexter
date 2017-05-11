package dexter.coreg;

import java.io.*;
import java.util.*;

import dexter.model.*;


public class CoregulationFile extends File implements Serializable
{
	private static final long serialVersionUID = -5206899461965468359L;
	
	
	private final static Map<Organism, File>		ORGANISM_TO_RAW_FILE;
	
	
	static
	{
		ORGANISM_TO_RAW_FILE = new TreeMap<Organism, File>();
		String prefix = "data/Coregulation/Arkinlab_Predicted_Operons_";
		for (Organism org: Organism.PROVIDED)
		{
			File f = new File(prefix + org.getShortName() + ".rkn");
			assert f.exists();
			ORGANISM_TO_RAW_FILE.put(org, f);
		}
	}

	
	private Organism 			organism;
	

	public CoregulationFile(File src, Organism organism)
	{
		super(src.getAbsolutePath());
		this.organism = organism;
	}
	
	
	public CoregulationFile(String path, Organism organism)
	{
		super(path);
		this.organism = organism;
	}
	
	
	public CoregulationFile(CoregulationFile src)
	{
		super(src.getPath());
		this.organism = src.organism;
	}
	
	
	public CoregulationFile(Organism organism) 
	{
		this(ORGANISM_TO_RAW_FILE.get(organism), organism);
	}
	
	
	public String toString()
	{
		return getName();
	}
	
	
	public Organism getOrganism()
	{
		return organism;
	}
	
	
	private boolean isArkin()
	{
		return getName().endsWith(".rkn");
	}
	
	
	public Vector<CoregulationGroup> getCoregulationGroups() throws IOException
	{
		return isArkin()  ?  getCoregulationGroupsArkinFormat()  :  getCoregulationGroupsSingleLineFormat();
	}

	
	//
	// Arkin format files are downloaded from the Arkin lab site at 
	//			http://www.microbesonline.org/operons/OperonList.html
	// Files are csv with one header line. Each row has 2 gene ids that are predicted to be operon partners.
	// Ids are columns 2 & 3 (from zero). Column 6 ("bOp") is "TRUE" or "FALSE". Only accept "TRUE" pairs.
	//
	public Vector<CoregulationGroup> getCoregulationGroupsArkinFormat() throws IOException
	{
		Vector<CoregulationGroup> ret = new Vector<CoregulationGroup>();
		Map<String, CoregulationGroup> idToOperon = new HashMap<String, CoregulationGroup>();
		
		FileReader fr = new FileReader(this);
		BufferedReader br = new BufferedReader(fr);
		br.readLine();				// skip header
		String line = null;
		while ((line = br.readLine()) != null)
		{
			String[] fields = line.split("\\t");
			assert fields.length >= 5;
			if (!fields[6].equals("TRUE"))
				continue;
			String id1 = fields[2];
			String id2 = fields[3];
			// If neither id has been seen (almost always), create a new operon. If 1 id has been seen,
			// add the other to the operon of the seen gene. If both have been seen, might need to merge
			// 2 operons.
			int nSeen = 0;
			if (idToOperon.containsKey(id1))
				nSeen++;
			if (idToOperon.containsKey(id2))
				nSeen++;
			switch (nSeen)
			{
				case 0:
					// Neither gene has been seen. Make a new operon.
					CoregulationGroup newOperon = new CoregulationGroup();
					newOperon.add(id1);
					newOperon.add(id2);
					idToOperon.put(id1, newOperon);
					idToOperon.put(id2, newOperon);
					ret.add(newOperon);
					break;
				case 1:
					// Add unseen gene to operon of seen gene.
					String seenId = idToOperon.containsKey(id1)  ?  id1  :  id2;
					String unseenId = (seenId == id1)  ?  id2  :  id1;
					assert !idToOperon.containsKey(unseenId);
					CoregulationGroup operon = idToOperon.get(seenId);
					operon.add(unseenId);
					idToOperon.put(unseenId, operon);
					break;
				default:
					// Both genes have been seen. If they are in the same operon, do nothing. If they
					// are in different operons, merge the operons.
					CoregulationGroup operon1 = idToOperon.get(id1);
					CoregulationGroup operon2 = idToOperon.get(id2);
					if (operon1 != operon2)
					{
						CoregulationGroup mergedOperon = new CoregulationGroup();
						mergedOperon.addAll(operon1);
						mergedOperon.addAll(operon2);
						ret.remove(operon1);
						ret.remove(operon2);
						for (String id: mergedOperon)
							idToOperon.put(id, mergedOperon);
					}
					break;
			}
		}
		br.close();
		fr.close();
		
		return ret;
	}
	
	
	public Vector<CoregulationGroup> getCoregulationGroupsSingleLineFormat() throws IOException
	{
		Vector<CoregulationGroup> ret = new Vector<CoregulationGroup>();
		
		FileReader fr = new FileReader(this);
		BufferedReader br = new BufferedReader(fr);
		String line = null;
		while ((line = br.readLine()) != null)
		{
			String name = null;
			int nColon = line.indexOf(':');
			if (nColon > 0)
			{
				name = line.substring(0, nColon);
				line = line.substring(nColon+1);
			}
			String[] ids = line.split(",");
			CoregulationGroup group = new CoregulationGroup(name);
			for (String id: ids)
				group.add(id.trim());
			ret.add(group);
		}
		br.close();
		fr.close();
		
		return ret;
	}
	
	
	// Check validity by trying to parse.
	public boolean isValid()
	{
		try
		{
			CoregulationFile that = new CoregulationFile(this);
			that.getCoregulationGroups();
			return true;
		}
		catch (Exception x)
		{
			return false;
		}
	}
	
	
	static void sop(Object x)				{ System.out.println(x); }
	
	
	public static void main(String[] args)
	{
		try
		{
			File f = new File("data/Coregulation/Arkinlab_Predicted_Operons_CROCO.rkn");
			CoregulationFile that = new CoregulationFile(f, Organism.CROCO);
			Vector<CoregulationGroup> operons = that.getCoregulationGroups();
			for (CoregulationGroup operon: operons)
				sop(operon);
		}
		catch (Exception x)
		{
			sop("STRESS: " + x.getMessage());
			x.printStackTrace(System.out);
			System.exit(1);
		}
		finally
		{
			sop("DONE");
		}
	}
}
