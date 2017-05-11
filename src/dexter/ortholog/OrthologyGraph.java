package dexter.ortholog;

import java.util.*;
import java.io.*;
import dexter.util.*;
import dexter.model.*;



/*
 * Key is query gene, value is collection of branches from key to subjects hit by key. Genes are
 * modeled as LightweightGene instances, but LightweightGene.organism is not used. Later the
 * organism field will be filled in so that orthologs can be visualized.
 */

public class OrthologyGraph extends HashMap<LightweightGene, Vector<GeneRelationship>>
{	
	private final static int				DFLT_MIN_BLAST_HIT_LEN		= 100;
	private final static float				DFLT_MIN_BLAST_PCT_IDENTITY	=  80f;
	private final static float				DFLT_MIN_BLAST_PCT_LENGTH	=  80f;
	
	private static int						minBLASTHitLen				= DFLT_MIN_BLAST_HIT_LEN;
	private static float					minBLASTPctIdentity			= DFLT_MIN_BLAST_PCT_IDENTITY;
	private static float					minBLASTPctLength			= DFLT_MIN_BLAST_PCT_LENGTH;
	
	private GeneIdToOrganismMap				geneIdToOrganism;
	private Map<String, LightweightGene>	geneIdToGene 				= new HashMap<String, LightweightGene>();
	private Set<LightweightGene>			connectedGenes 				= new HashSet<LightweightGene>();
	private Vector<OrthologyGroup>			cliques;					// null until partition() is called

	
	public OrthologyGraph(GeneIdToOrganismMap geneIdToOrganism)
	{
		this.geneIdToOrganism = geneIdToOrganism;
	}
	
	
	public OrthologyGraph(SessionModel session, 
			Vector<File> commaDelimitedListFiles,
			Vector<File> tabularBlastFiles) throws IOException
	{
		this(session.buildGeneIdToOrganismMap(), commaDelimitedListFiles, tabularBlastFiles);
	}
	
	
	public OrthologyGraph(GeneIdToOrganismMap geneIdToOrganism, 
			Vector<File> commaDelimitedListFiles,
			Vector<File> tabularBlastFiles) throws IOException
	{
		this(geneIdToOrganism);
		loadCommaDelimitedListFiles(commaDelimitedListFiles);
		loadTabularBlastFiles(tabularBlastFiles);
	}
	
	
	public void loadCommaDelimitedListFiles(Collection<File> commaDelimitedFiles) throws IOException
	{
		if (commaDelimitedFiles == null)
			return;
		
		for (File f: commaDelimitedFiles)
			loadCommaDelimitedListFile(f);
	}
	
	
	// Each line defines an orthology group. Adds group to the graph with maximal connection.
	public void loadCommaDelimitedListFile(File f) throws IOException, IllegalArgumentException
	{
		FileReader fr = new FileReader(f);
		BufferedReader br = new BufferedReader(fr);
		String line = null;
		while ((line = br.readLine()) != null)
		{
			String[] ids = line.split(",");
			if (ids.length < 2)
			{
				String err = "Comma-delimited list file " + f.getName() +
					":\nMust have at least 2 ids in comma-separated orthology line, saw\"" + line + "\"";
				throw new IllegalArgumentException(err);
			}
			Set<LightweightGene> genes = new HashSet<LightweightGene>();
			for (String id: ids)
			{
				// Reject if organism is unknown for gene id.
				if (!geneIdToOrganism.containsKey(id))
					continue;
				genes.add(getGeneForId(id));
			}
			for (LightweightGene g1: genes)
				for (LightweightGene g2: genes)
					if (g1 != g2)
						acceptQueryHitsSubject(g1, g2);
		}
		br.close();
		fr.close();
	}
	
	
	public void loadTabularBlastFiles(Collection<File> tabularBlastFiles) throws IOException
	{
		if (tabularBlastFiles == null)
			return;
		
		for (File f: tabularBlastFiles)
			loadTabularBlastFile(f);
	}
	

	public void loadTabularBlastFile(File f) throws IOException
	{
		FileReader fr = new FileReader(f);
		BufferedReader br = new BufferedReader(fr);
		TabularBlastReader tbr = new TabularBlastReader(br);
		TabularBlastHit hit = null;
		while ((hit = tbr.readBlastHit()) != null)
			load(hit);
		tbr.close();
		br.close();
		fr.close();
	}
	
	
	private void load(TabularBlastHit hit)
	{
		// Subject might have format "ID;annotations". If so, truncate.
		int n = hit.subject.indexOf(";");
		if (n > 0)
			hit.subject = hit.subject.substring(0, n);	
		
		// Reject if organism is unknown for query or subject.
		if (!geneIdToOrganism.containsKey(hit.query))
			return;
		if (!geneIdToOrganism.containsKey(hit.subject))
			return;
		
		// Reject if query = subject.
		LightweightGene queryGene = getGeneForId(hit.query);		
		LightweightGene subjectGene = getGeneForId(hit.subject);
		if (queryGene.equals(subjectGene))
			return;
		
		// Reject if too short or too diverged.
		if (hit.length < minBLASTHitLen) 
			return;
		if (hit.pctIdent < minBLASTPctIdentity)
			return;
		float fracLen = hit.length / (float)hit.queryLength();
		if (fracLen*100f < minBLASTPctLength)
			return;	
		
		// Accept.
		acceptQueryHitsSubject(queryGene, subjectGene);
	}
	
	
	private void acceptQueryHitsSubject(LightweightGene queryGene, LightweightGene subjectGene)
	{		
		Vector<GeneRelationship> branchesFromQuery = get(queryGene);
		if (branchesFromQuery == null)
		{
			branchesFromQuery = new Vector<GeneRelationship>();
			put(queryGene, branchesFromQuery);
		}
		else
		{
			for (GeneRelationship b: branchesFromQuery)
				if (b.to == subjectGene)		// Already have a branch from this query to hit subject
					return;	
		}
		GeneRelationship branch = new GeneRelationship(queryGene, subjectGene);
		branchesFromQuery.add(branch);
		connectedGenes.add(queryGene);
		connectedGenes.add(subjectGene);
	}
	
	
	private LightweightGene getGeneForId(String id)
	{
		if (geneIdToGene.containsKey(id))
			return geneIdToGene.get(id);
		else
		{
			Organism org = geneIdToOrganism.get(id);
			assert org != null  :  "No organism known for " + id;
			LightweightGene g = new LightweightGene(org, id);
			geneIdToGene.put(id, g);
			return g;
		}
	}
	
	
	private OrthologyGroup collectDescendants(LightweightGene ancestor)
	{
		OrthologyGroup descendants = new OrthologyGroup(this);
		collectDescendants(ancestor, descendants);
		return descendants;
	}
	
	
	private void collectDescendants(LightweightGene ancestor, OrthologyGroup descendants)
	{
		if (descendants.contains(ancestor))
			return;									// looped back, stop recursing.
		
		descendants.add(ancestor);
		if (containsKey(ancestor))
		{
			for (GeneRelationship branch: get(ancestor))
			{
				collectDescendants(branch.to, descendants);
			}
		}
	}
	
	
	public Vector<OrthologyGroup> partition()
	{
		Set<LightweightGene> unassignedGenes = new HashSet<LightweightGene>(connectedGenes);
		assert !unassignedGenes.isEmpty();
		cliques = new Vector<OrthologyGroup>();
		Map<LightweightGene, OrthologyGroup> geneToClique = new HashMap<LightweightGene, OrthologyGroup>();
		
		while (!unassignedGenes.isEmpty())
		{
			// Pick an unassigned gene. Add it and all its descendants to a clique. Merge
			// any overlapping cliques.
			LightweightGene unassignedGene = unassignedGenes.iterator().next();
			OrthologyGroup newClique = collectDescendants(unassignedGene);
			Set<OrthologyGroup> overlappedCliques = new HashSet<OrthologyGroup>();			
			for (LightweightGene gene: newClique)
				if (geneToClique.containsKey(gene))
					overlappedCliques.add(geneToClique.get(gene));	
			for (OrthologyGroup overlappedPartition: overlappedCliques)
				newClique.addAll(overlappedPartition);
			cliques.removeAll(overlappedCliques);
			cliques.add(newClique);
			for (LightweightGene g: newClique)
				geneToClique.put(g, newClique);
			unassignedGenes.removeAll(newClique);
		}
		
		return cliques;
	}
	
	
	public TreeSet<Organism> collectOrganisms()
	{
		TreeSet<Organism> allOrganisms = new TreeSet<Organism>();
		for (Vector<GeneRelationship> edges: values())
		{
			for (GeneRelationship edge: edges)
			{
				allOrganisms.add(edge.from.getOrganism());
				allOrganisms.add(edge.to.getOrganism());
			}
		}
		return allOrganisms;
	}
	
	
	public Set<LightweightGene> collectGenes()
	{
		Set<LightweightGene> ret = new HashSet<LightweightGene>();
		for (LightweightGene query: keySet())
		{
			ret.add(query);
			for (GeneRelationship edge: get(query))
			{
				ret.add(edge.from);
				ret.add(edge.to);
			}
		}
		return ret;
	}
	
	
	public static OrthologyGraph buildForCrocoProTricho() throws Exception
	{		
		// Use InferringGeneIdToOrganismMap version to debug.
		OrthologyGraph ret = new OrthologyGraph(new GeneIdToOrganismMap());
		File f = new File("data/Orthologs/CrocoProTeryMergedOrthologiesList.txt");
		ret.loadCommaDelimitedListFile(f);
		assert !ret.isEmpty();
		assert !ret.connectedGenes.isEmpty()  :  "No connected genes";
		return ret;
	}
	
	
	public static boolean isTabularBlastFile(File f) throws IOException
	{
		OrthologyGraph ograph = new OrthologyGraph(new GeneIdToOrganismMap());
		try
		{
			ograph.loadTabularBlastFile(f);
			return true;
		}
		catch (IllegalArgumentException x)
		{
			return false;
		}
	}
	
	
	public static boolean isCommaDelimitedListFile(File f) throws IOException
	{
		OrthologyGraph ograph = new OrthologyGraph(new GeneIdToOrganismMap());
		try
		{
			ograph.loadCommaDelimitedListFile(f);
			return true;
		}
		catch (IllegalArgumentException x)
		{
			return false;
		}
	}
	
	
	public GeneIdToOrganismMap getGeneIdToOrganismMap()	{ return geneIdToOrganism; }
	public static int getDefaultMinBLASTHitLength()		{ return DFLT_MIN_BLAST_HIT_LEN; } 
	public static float getDefaultMinBLASTPctIdent()	{ return DFLT_MIN_BLAST_PCT_IDENTITY; }
	public static float getDefaultMinBLASTPctLength()	{ return DFLT_MIN_BLAST_PCT_LENGTH; }
	static void sop(Object x)							{ System.out.println(x); }
	
	
	public static void main(String[] args)
	{				
		try
		{
			sop("Starting");
			OrthologyGraph graph = buildForCrocoProTricho();
			Vector<OrthologyGroup> cliques = graph.partition();
			BinCounter<Integer> bc = new BinCounter<Integer>();
			for (OrthologyGroup c: cliques) 
				bc.bumpCountForBin(c.size());
			sop(bc);
			sop("\n" + cliques.size() + " cliques");

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
