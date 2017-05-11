package analysis.arkin;

import java.io.*;
import java.util.*;

import dexter.model.*;


public class DielFraction 
{	
	private final static File							COREG_DIRF = new File("data/Coregulation");
	private       static Map<Study, HashSet<String>>	STUDY_TO_PRIOR_OPERON_GENES;

	static SessionModel 			session = getSession();
	
	
	static void initStudyToPriors() throws IOException
	{
		STUDY_TO_PRIOR_OPERON_GENES = new HashMap<Study, HashSet<String>>();
		for (Study study: session.getStudies())
		{
			File priorsFile = getPriorsFileForStudy(study);
			HashSet<String> priors = new HashSet<String>();
			FileReader fr = new FileReader(priorsFile);
			BufferedReader br = new BufferedReader(fr);
			br.readLine();
			String line = null;
			while ((line = br.readLine()) != null)
			{
				if (!line.contains("TRUE"))
					continue;
				String[] pieces = line.split("\\s");
				priors.add(pieces[2]);
				priors.add(pieces[3]);
			}
			STUDY_TO_PRIOR_OPERON_GENES.put(study, priors);
			br.close();fr.close();
		}
	}
	
	
	static File getPriorsFileForStudy(Study study)
	{
		String suffix = study.getName().toUpperCase();
		suffix = suffix.substring(suffix.length()-3);
		for (String kid: COREG_DIRF.list())
			if (kid.contains(suffix))
				return new File(COREG_DIRF, kid);
		assert false;
		return null;
	}
	
	
	static SessionModel getSession()
	{
		try
		{
			return SessionModel.deserialize(new File("data/Sessions/CPT.dex"));
		}
		catch (Exception x)
		{
			sop("Couldn't unpickle session: " + x.getMessage());
			System.exit(0);
		}
		return null;
	}
	
	
	static void check(Study study)
	{
		sop("Checking study " + study.getName());
		float nGenesTotal = study.size();
		int nDielGenes = 0;
		for (Gene g: study)
			if (g.isDiel())
				nDielGenes++;
		float dielFrac = nDielGenes / nGenesTotal;
		sop("  diel fraction = " + dielFrac);
	}
	
	
	static float getDielFraction(Collection<String> geneIds, Study study)
	{
		Map<String, Gene> idToGene = study.getIdToGeneMap();
		float nDiel = 0f;
		for (String id: geneIds)
		{
			Gene gene = idToGene.get(id);
			if (gene == null)
				continue;
			if (gene.isDiel())
				nDiel++;
		}
		sop("   diel frac = " + nDiel + "/" + geneIds.size());
		return nDiel / geneIds.size();
	}
	
	
	static void sop(Object x)				{ System.out.println(x); }
	
	
	public static void main(String[] args)
	{
		try
		{
			sop("START");
			initStudyToPriors();
			for (Study study: STUDY_TO_PRIOR_OPERON_GENES.keySet())
			{
				Set<String> priorOpGenes = STUDY_TO_PRIOR_OPERON_GENES.get(study);
				float fractPriorDiel = getDielFraction(priorOpGenes, study);
				sop(study.getName() + ": " + fractPriorDiel + " diel genes in prior operons");
			}
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
