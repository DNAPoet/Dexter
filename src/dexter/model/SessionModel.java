//
// Aggregation of all information generated during wizard-driven setup. The "official" list
// of studies is the keyset of studyToTimeAssignmentMap, plus the (possibly null or empty)
// experiments study.
//


package dexter.model;

import java.awt.Color;
import java.io.*;
import java.util.*;

import dexter.*;
import dexter.ortholog.*;
import dexter.proximity.*;
import dexter.coreg.*;
import dexter.view.graph.GraphBackgroundModel;


public class SessionModel implements Serializable, VisualConstants
{
	private static final long 					serialVersionUID 		= 2168145128407520079L;
	private final static File					SERF					= new File("data/Sessions/CPT.dex");
	private final static Set<String>			PLACEHOLDER_CELL_SVALS 	= new HashSet<String>();
	
	// Spreadsheet cells containing the following values will be treated as empty.
	static
	{
		PLACEHOLDER_CELL_SVALS.add("-");
		PLACEHOLDER_CELL_SVALS.add("n/a");
	}
	
	private Map<Study, TimeAssignmentMap>		studyToTimeAssignmentMap;
	private GraphBackgroundModel				graphBackgroundModel;
	private OrthologyFileCollection				orthologyFiles;
	private CoregulationFileCollection			coregulationFiles;
	private ExperimentsStudy					experimentsStudy;
	private Map<Organism, ProximityKit>			organismToProximityKit;
	
	
	public SessionModel(Map<Study, TimeAssignmentMap> studyToTimeAssignmentMap,
						GraphBackgroundModel graphBackgroundModel,
						OrthologyFileCollection orthologyFiles,
						CoregulationFileCollection coregulationFiles)
	{
		this.studyToTimeAssignmentMap = studyToTimeAssignmentMap;
		this.graphBackgroundModel = graphBackgroundModel;
		this.orthologyFiles = orthologyFiles;
		this.coregulationFiles = coregulationFiles;
		
		initOrganismToProximityKit();
	}
	
	
	public SessionModel(File f) throws IOException, ClassNotFoundException
	{
		FileInputStream fis = new FileInputStream(f);
		ObjectInputStream ois = new ObjectInputStream(fis);
		SessionModel that = (SessionModel)ois.readObject();
		ois.close();
		fis.close();
		
		for (Study study: that.getStudies())
			study.validateGenes();

		this.studyToTimeAssignmentMap = that.studyToTimeAssignmentMap;
		this.graphBackgroundModel = that.graphBackgroundModel;
		this.orthologyFiles = that.orthologyFiles;
		this.coregulationFiles = that.coregulationFiles;
		
		if (organismToProximityKit == null)
			initOrganismToProximityKit();
		
		nullifyPlaceholderCellValues();
	}
	
	
	public static SessionModel fromDevSerFile() throws Exception
	{
		return new SessionModel(SERF);
	}
	
	
	public String toString()
	{
		String s = "SessionModel has schedules for " + studyToTimeAssignmentMap.size() + " studies:";
		for (Study study: studyToTimeAssignmentMap.keySet())
			s += "\n  " + study.getName() + " has " + study.size() + " genes";
		
		s += "\nSPREADSHEET FILES:";
		for (Study study: studyToTimeAssignmentMap.keySet())
		{
			s += "---  " + study.getName();
			for (SpreadsheetStructure struc: study.getSpreadsheetStructures())
				s += "\n+$+$+$+$+$+\n" + struc;
		}
			
		return s;
	}
	
	
	private void initOrganismToProximityKit()
	{
		organismToProximityKit = new HashMap<Organism, ProximityKit>();
		organismToProximityKit.put(Organism.TERY, ProximityKit.FOR_TERY);
		organismToProximityKit.put(Organism.PRO, ProximityKit.FOR_MED4);
		organismToProximityKit.put(Organism.CROCO, ProximityKit.FOR_CROCO);
	}
	
	
	public void serialize(File f) throws IOException
	{
		FileOutputStream fos = new FileOutputStream(f);
		ObjectOutputStream oos = new ObjectOutputStream(fos);
		oos.writeObject(this);
		oos.flush();
		fos.flush();
		oos.close();
		fos.close();
	}
	
	
	public static SessionModel deserialize(File serfile) throws IOException, ClassNotFoundException
	{
		FileInputStream fis = new FileInputStream(serfile);
		ObjectInputStream ois = new ObjectInputStream(fis);
		SessionModel model = (SessionModel)ois.readObject();
		ois.close();
		fis.close();
		
		// Older serialized sessions might have null experimentStudy field. It needs to be non-null.
		if (model.experimentsStudy == null)
			model.experimentsStudy = new ExperimentsStudy();
		
		return model;
	}
	
	
	public void open() throws IOException
	{
		MainDexterFrame frame = new MainDexterFrame(this);
		frame.setVisible(true);
	}
	
	
	public TimeAssignmentMap getTimeAssignmentMapForStudy(Study study)
	{
		return studyToTimeAssignmentMap.get(study);
	}
	
	
	public StudyList getStudies()							
	{
		StudyList ret = new StudyList();
		ret.addAll(studyToTimeAssignmentMap.keySet());
		return ret;
	}
	
	
	public StudyList getStudiesOmitExperiments()							
	{
		StudyList ret = new StudyList();
		for (Study study: studyToTimeAssignmentMap.keySet())
			if (!study.isExperimentsStudy())
				ret.add(study);
		return ret;
	}
	
	
	public Vector<Organism> getOrganisms()
	{
		Set<Organism> sorter = new TreeSet<Organism>();
		for (Study study: getStudies())
			sorter.add(study.getOrganism());
		return new Vector<Organism>(sorter);
	}
	
	
	// Eventually there will be multiple studies per organism.
	public Map<Organism, Set<Study>> getStudiesByOrganism()
	{
		Map<Organism, Set<Study>> ret = new HashMap<Organism, Set<Study>>();
		for (Study study: getStudies())
		{
			Organism org = study.getOrganism();
			if (!ret.containsKey(org))
				ret.put(org, new HashSet<Study>());
			ret.get(org).add(study);
		}
		return ret;
	}
	

	// Legacy serialized instances have null experimentsStudy field.
	public ExperimentsStudy getExperimentsStudy()						
	{ 
		if (experimentsStudy == null)
			experimentsStudy = new ExperimentsStudy();
		return experimentsStudy;		   
	}
	
	
	public void setExperimentsStudy(ExperimentsStudy experimentsStudy)
	{
		this.experimentsStudy = experimentsStudy;
	}
	
	
	public GeneIdToOrganismMap buildGeneIdToOrganismMap()
	{
		return new GeneIdToOrganismMap(getStudiesOmitExperiments());
	}
	
	
	public Gene lightweightGeneToGene(LightweightGene lwg)
	{
		for (Study study: studyToTimeAssignmentMap.keySet())
		{
			if (study.isExperimentsStudy())
				continue;
			if (!study.getOrganism().equals(lwg.getOrganism()))
				continue;
			for (Gene gene: study)
				if (gene.getId().equals(lwg.getId()))
						return gene;
		}
		return null;
	}
	

	public Vector<OrthologyGroup> getOrthoGroups() throws IOException
	{
		OrthologyGraph comprehensiveOrthologyGraph = 
			new OrthologyGraph(this, orthologyFiles.getListFiles(), orthologyFiles.getTabularBLASTFiles());
		return comprehensiveOrthologyGraph.partition();
	}
	
	
	public int getNGenes()
	{
		int ret = 0;
		for (Study study: getStudies())
			ret += study.size();
		return ret;
	}
	
	
	public Set<Gene> collectAllGenes()
	{
		Set<Gene> ret = new TreeSet<Gene>();
		for (Study study: getStudies())
			ret.addAll(study);
		return ret;
	}
	
	
	public Map<String, Gene> mapIdsToGenes()
	{
		Map<String, Gene> ret = new HashMap<String, Gene>();
		for (Gene gene: collectAllGenes())
			ret.put(gene.getId(), gene);
		return ret;
	}
	
	
	public Map<String, Gene> getGenesForBestNames(Collection<String> bestNames)
	{
		Map<String, Gene> ret = new HashMap<String, Gene>();
		for (Study study: getStudies())
		{
			for (Gene gene: study)
			{
				String bestName = gene.getBestAvailableName();
				if (bestNames.contains(bestName))
					ret.put(bestName, gene);
			}
		}
		return ret;
	}
	
	
	// Any role can be the basis of grouping except timepoint, name, id, and annotation. A role is a tag
	// defined by the system (e.g. KEGG pathway) or the user (e.g. COG category) and assigned to at most
	// 1 spreadsheet column per study. Returned collection presents KEGG pathway first if present, then
	// all others in alpha order.
	public Vector<SpreadsheetColumnRole> getGroupableColumnRoles()
	{
		TreeSet<SpreadsheetColumnRole> sorter = new TreeSet<SpreadsheetColumnRole>();
		for (Study study: getStudies())
			sorter.addAll(study.collectGroupableColumnRoles());
		Vector<SpreadsheetColumnRole> ret = new Vector<SpreadsheetColumnRole>(sorter);
		return ret;
	}
	
	
	// Some spreadsheets might indicate no-value in a cell with "-" or "n/a".
	public void nullifyPlaceholderCellValues()
	{
		for (Study study: getStudies())
			study.nullifyPlaceholderCellValues(PLACEHOLDER_CELL_SVALS);
	}
	

	// Build universal study-to-color map.
	public Map<Study, Color> getStudyToColorMap()
	{
		Map<Study, Color> studyToColor = new HashMap<Study, Color>();
		int n = 0;
		for (Study study: getStudies())
			studyToColor.put(study, DFLT_GENE_COLORS[n++ % DFLT_GENE_COLORS.length]);	
		return studyToColor;
	}
	
	
	public boolean hasCoregulation()
	{
		return coregulationFiles != null  &&  !coregulationFiles.isEmpty(); 
	}
	
	
	public boolean hasCoregulationFor(Organism org)
	{
		return coregulationFiles != null  &&  coregulationFiles.containsKey(org);
	}
	
	
	public boolean hasCoregulationFor(Study study)
	{
		return hasCoregulationFor(study.getOrganism());
	}
	
	
	public Vector<CoregulationFile> getCoregulationFilesFor(Organism org)
	{
		return coregulationFiles.get(org);
	}
	
	
	public Vector<CoregulationFile> getCoregulationFilesFor(Study study)
	{
		return getCoregulationFilesFor(study);
	}
	
	
	public Map<Gene, Vector<float[]>> mapGenesToTimeExpressionPairs(Study study)
	{
		assert !study.isExperimentsStudy();
		TimeAssignmentMap timeAssignments = getTimeAssignmentMapForStudy(study);
		Map<Gene, Vector<float[]>> ret = new HashMap<Gene, Vector<float[]>>();
		for (Gene gene: study)
			ret.put(gene, gene.getTimeAndExpressionPairs(timeAssignments));
		return ret;
	}
	
	
	public Study getStudyMatchingName(String matchMe)
	{
		for (Study s: getStudies())
			if (s.getName().toUpperCase().contains(matchMe))
				return s;
		return null;
	}
	
	
	public Study getCrocoStudy()
	{
		return getStudyMatchingName("CROCO");
	}
	
	
	public Study getProStudy()
	{
		return getStudyMatchingName("PRO");
	}
	
	
	public Study getTeryStudy()
	{
		return getStudyMatchingName("TERY");
	}
	
	
	public Map<Organism, ProximityKit> getOrganismToProximityKitMap()	{ return organismToProximityKit;   }
	public Map<Study, TimeAssignmentMap> getStudyToTimeAssignmentMap()	{ return studyToTimeAssignmentMap; }
	public GraphBackgroundModel getGraphBackgroundModel()				{ return graphBackgroundModel;     }
	public OrthologyFileCollection getOrthologyFiles()					{ return orthologyFiles;		   }
	public CoregulationFileCollection getCoregulationFiles()			{ return coregulationFiles;		   }
	public void setCoregulationFiles(CoregulationFileCollection files)	{ this.coregulationFiles = files;  }
	static void sop(Object x)											{ System.out.println(x);		   }
	

	public static void main(String[] args)
	{
		try
		{
			File serf = new File("data/sessions/CPT.dex");
			SessionModel session = new SessionModel(serf);
			for (Study study: session.getStudies())
			{
				sop("***********************\n*****************\n" + study.getName());
				for (SpreadsheetStructure struc: study.getSpreadsheetStructures())
					sop(struc + "\n----------");
			}
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
