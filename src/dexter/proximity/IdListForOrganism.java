package dexter.proximity;

import java.io.*;
import java.util.*;

import dexter.model.*;
import dexter.util.*;


//
// Members of the vector are ids. The gene field is non-null if a corresponding gene is available. Maintains
// an id-to-contig map for multi-contig genomes. The contig index is arbitrary, based on order of appearance
// in the genome file, and has no relationship to the official contig id; it should only be used to determine
// whether two genes are in the same contig.
//


public class IdListForOrganism extends Vector<String>
{
	private Organism				organism;
	private Gene					gene;
	private boolean					isOperon;
	private Map<String, Integer>	idToContigIndex;		// from 0
	private Map<String, Integer>	idToPositionInContig;	// from 0
	private Vector<Integer>			contigLengths;
	
	
	public IdListForOrganism()		{ }
	
	
	public IdListForOrganism(Organism organism)
	{
		this.organism = organism;
	}
	
	
	public IdListForOrganism(Organism organism, ProximityKit kit) throws IOException
	{
		this(organism, kit.file, kit.format);
	}
	
	
	public IdListForOrganism(Organism organism, File file, ProximityFileFormat format) throws IOException
	{
		this.organism = organism;
		
		FileReader fr = new FileReader(file);
		
		switch (format.getFileType())
		{
			case FASTA:
				FastaReader far = new FastaReader(fr);
				String[] rec = null;
				while ((rec = far.readSequence()) != null)
				{
					String id = format.extractId(rec[0].substring(1));
					if (id != null)
						add(id);
				}
				break;
				
			case DELIMITED:
				BufferedReader br = new BufferedReader(fr);
				String line = null;
				for (int i=0; i<format.getNHeaderLines(); i++)
					br.readLine();
				while ((line = br.readLine()) != null)
				{
					String id = format.extractId(line);
					if (id != null)
						add(id);
				}
				br.close();
				break;
				
			case GENBANK:
				int contigIndex = 0;
				int positionInContig = 0;
				idToContigIndex = new HashMap<String, Integer>();
				idToPositionInContig = new HashMap<String, Integer>();
				contigLengths = new Vector<Integer>();
				br = new BufferedReader(fr);
				line = null;
				while ((line = br.readLine()) != null)
				{
					line = line.trim();
					if (line.equals("//"))
					{
						contigIndex++;
						contigLengths.add(positionInContig+1);
						positionInContig = 0;
						continue;
					}
					if (!line.startsWith("/locus_tag"))				// e.g. /locus_tag="PMM0004"
						continue;
					line = line.substring(line.indexOf('=')+1);		// "PMM0004"
					line = ProximityFileFormat.stripLeadingAndTrailingQuotes(line);
					if (isEmpty()  ||  !lastElement().equals(line))
					{
						add(line);
						idToContigIndex.put(line, contigIndex);
						idToPositionInContig.put(line, positionInContig++);
					}
				}
				br.close();
				break;
		}
		
		fr.close();
	}
	
	
	public static IdListForOrganism forProvidedOrganism(Organism org) throws IOException
	{
		ProximityKit kit = ProximityKit.forProvidedOrganism(org);
		return new IdListForOrganism(org, kit);
	}
	
	
	public String toString()
	{
		String s = "ProximityGroup, size = " + size() + "\n";
		if (organism != null)
			s += "  Organism: " + organism + "\n";
		if (idToContigIndex != null)
		{
			Set<Integer> contigNums = new HashSet<Integer>(idToContigIndex.values());
			sop(contigNums.size() + " contigs");
		}

		
		for (int i=0; i<20; i++)
			s += "\n  " + get(i);
		
		s += "\n...";
		
		for (int i=size()-20; i<size(); i++)
			s += "\n  " + get(i);
		
		return s;
	}
	
	
	public Organism getOrganism()
	{
		return organism;
	}
	
	
	public void setOrganism(Organism organism)
	{
		this.organism = organism;
	}
	
	
	public boolean isOperon()
	{
		return isOperon;
	}
	
	
	public void setIsOperon(boolean isOperon)
	{
		this.isOperon = isOperon;
	}
	
	
	public int getContigIndex(Gene gene)
	{
		return getContigIndex(gene.getId());
	}
	
	
	public int getContigIndex(String id)
	{
		return (idToContigIndex == null)  ?  0  :  idToContigIndex.get(id);
	}
	
	
	public int getPositionInContig(Gene gene)
	{
		return getPositionInContig(gene.getId());
	}
	
	
	public int getPositionInContig(String id)
	{
		return (idToPositionInContig == null)  ?  0  :  idToPositionInContig.get(id);
	}
	
	
	public int getNGenesInContig(int contigNum)
	{
		return (contigLengths == null)  ?  size()  :  contigLengths.get(contigNum);
	}
	
	
	public int getNContigs()
	{
		return (contigLengths == null)  ?  1  :  contigLengths.size();
	}
	
	
	static void sop(Object x)	{ System.out.println(x); }
	
	
	public static void main(String[] args)
	{
		try
		{
			sop("START");
			IdListForOrganism that = new IdListForOrganism(Organism.CROCO, ProximityKit.FOR_CROCO);
			sop(that);
		}
		catch (Exception x)
		{
			sop("Stress: " + x.getMessage());
			x.printStackTrace(System.out);
		}
	}
}
