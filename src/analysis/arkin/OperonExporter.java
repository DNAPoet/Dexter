package analysis.arkin;

import java.io.*;
import java.util.*;
import dexter.model.*;


public class OperonExporter 
{
	static File					DIRF		= new File("analysis_data/Export");
	static String				DELIM		= ",";
	static SessionModel			session;
	

	private Study 							study;
	private Map<Gene, Vector<float[]>> 		geneToTx;
	private Map<String, Gene> 				idToGene;
	private Collection<Operon>				operons;
	

	OperonExporter(Study study, Collection<Operon> operons)
	{
		this.operons = operons;
		this.study = study;
		sop(study.getOrganism());
		geneToTx = session.mapGenesToTimeExpressionPairs(study);
		idToGene = study.getIdToGeneMap();
	}
	
	
	void export(File ofile) throws IOException
	{
		export(operons, ofile);
	}
	
	
	void export(Collection<Operon> operons, File ofile) throws IOException
	{
		FileWriter fw = new FileWriter(ofile);
		
		// Header.
		String h = "Operon Index" + DELIM + "ID" + DELIM + "Name" + DELIM + "KEGG" + DELIM + "Anno";
		fw.write(h + "\n");
		
		// Operons.
		int index = 1;
		for (Operon op: operons)
		{
			for (String id: op)
			{
				Gene gene = idToGene.get(id);
				if (gene == null)
					continue;
				String name = gene.getName();
				if (name == null  ||  name.trim().isEmpty())
					name = "n/a";
				String kegg = gene.getPathway();
				if (kegg == null  ||  kegg.trim().isEmpty())
					kegg = "n/a";
				else 
					kegg = quoth(kegg);
				String anno = gene.getValueForPredefinedRole(PredefinedSpreadsheetColumnRole.ANNOTATION);
				if (anno == null  ||  anno.trim().isEmpty())
					anno = "n/a";
				else
					anno = quoth(anno);
				String line = index + DELIM + id + DELIM + name + DELIM + kegg + DELIM + anno;
				fw.write(line + "\n");
 			}
			index++;
		}
		
		fw.flush();fw.close();
	}
	
	
	void exportMerges(File ofile) throws IOException
	{
		File ifile = new File("analysis_data/MyRecommendations/AllCandidatesSorted_E_LE_5pct__CSV.csv");
		assert ifile.exists();
		FileReader fr = new FileReader(ifile);
		BufferedReader br = new BufferedReader(fr);
		String line = null;
		br.readLine();
		Vector<Operon> ops = new Vector<Operon>();
		while ((line = br.readLine()) != null)
		{
			if (line.charAt(0) != study.getOrganism().getName().charAt(0))
				continue;
			sop(line);
			Operon op = new Operon();
			ops.add(op);
			String[] pieces = line.split(",");
			for (int i=0; i<6; i++)
			{
				String id = pieces[i];
				if (!id.startsWith("-"))
				{
					assert idToGene.containsKey(id);
					op.add(id);
				}
			}
			assert op.size() >= 4;
		}
		br.close();
		export(ops, ofile);
	}
	
	
	static String quoth(String s)
	{
		return '"' + s + '"';
	}

	
	static void sop(Object x)		{ System.out.println(x); }
	
	
	public static void main(String[] args)
	{
		assert DIRF.exists();
		try
		{
			session = SessionModel.deserialize(new File("data/Sessions/CPT.dex"));
			assert session != null;
			
			sop("CROCO:");
			Collection<Operon> crocoOps = Operon.extractFromArkinPredictionsForCroco();
			OperonExporter that = new OperonExporter(session.getCrocoStudy(), crocoOps);
			that.exportMerges(new File(DIRF, "Croco_Merged_Operons.csv"));
			
			sop("**************\nPRO:");
			Collection<Operon> proOps = Operon.extractFromArkinPredictionsForPro();
			that = new OperonExporter(session.getProStudy(), proOps);
			that.exportMerges(new File(DIRF, "Pro_Merged_Operons.csv"));

			sop("**************\nTERY:");
			Collection<Operon> teryOps = Operon.extractFromArkinPredictionsForTery();
			that = new OperonExporter(session.getTeryStudy(), teryOps);
			that.exportMerges(new File(DIRF, "Tery_Merged_Operons.csv"));
		}

		catch (Exception x)
		{
			sop("Feh");
			sop(x.getMessage());
			x.printStackTrace();
		}
	}
}
