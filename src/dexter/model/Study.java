package dexter.model;

import java.io.*;
import java.util.*;

import dexter.cluster.Metric;
import dexter.coreg.CoregulationFileCollection;
import dexter.util.BinCounter;
import dexter.util.HashMapIgnoreKeyCase;
import dexter.view.wizard.DexterWizardDialog;


public class Study extends Vector<Gene> implements java.io.Serializable, Comparable<Study>
{

	private static final long 				serialVersionUID = 400934952296052184L;

	private String 							name;
	private Organism 						organism;
	private Vector<SpreadsheetStructure>	spreadsheetStructures;
	private Vector<String>					timepointColumnNames;
	private CoregulationFileCollection		coregFiles;
	
	
				
	
	
	
	
	
	
				
				////////////////////////////////////////////////////////////////////////////
				//                                                                        //
				//                              CONSTRUCTION                              //
				//                                                                        //
				////////////////////////////////////////////////////////////////////////////
				
				
	
	
	// Primary spreadsheet must be 1st member of spreadsheetStructures<>. Each spreadsheet must
	// contain an ID field.
	public Study(Organism organism, Vector<SpreadsheetStructure> spreadsheetStructures)
	{
		this.organism = organism;
		
		// Derive name from primary spreadsheet filename.
		String primaryFname = spreadsheetStructures.firstElement().getFile().getName();
		assert primaryFname.endsWith(".csv")  ||  primaryFname.endsWith(".tsv")  :
			"Unexpected file name " + primaryFname + " (expected .csv or .tsv)";
		this.name = primaryFname.substring(0, primaryFname.lastIndexOf('.'));
		
		// Collect names of timepoint columns. Other column names are available as
		// keys into 2 maps in the Gene instances.
		timepointColumnNames = new Vector<String>();
		this.spreadsheetStructures = spreadsheetStructures;
		for (SpreadsheetStructure struc: spreadsheetStructures)
			timepointColumnNames.addAll(struc.getTimepointColumnNames());
		
		// Collect gene IDs from each spreadsheet.
		Vector<SpreadsheetIdToGeneMap> idMaps = new Vector<SpreadsheetIdToGeneMap>(); 
		for (SpreadsheetStructure struc: spreadsheetStructures)
		{
			assert struc.getColNumOfId() >= 0  :  "No ID column in spreadsheet structure " + name;
			try
			{
				Spreadsheet sheet = new Spreadsheet(this, struc);
				idMaps.add(new SpreadsheetIdToGeneMap(sheet));
			}
			catch (IOException x)
			{
				String err = "Can't read spreadsheet file " + struc.getFile().getAbsolutePath() + 
					": " + x.getMessage();
				javax.swing.JOptionPane.showMessageDialog(null, err);
				return;
			}
		}
		
		// For each gene in the primary spreadsheet, merge any genes from other spreadsheets
		// that have the same ID.
		SpreadsheetIdToGeneMap primaryIdToGene = idMaps.firstElement();
		for (String id: primaryIdToGene.keySet())
		{
			Gene gene = primaryIdToGene.get(id);
			for (int i=1; i<idMaps.size(); i++)
			{
				SpreadsheetIdToGeneMap secondaryIdToGene = idMaps.get(i);
				if (secondaryIdToGene.containsKey(id))
					gene.absorb(secondaryIdToGene.get(id));
			}
			add(gene);
		}
	}
	
	
	public Study()		{ }
	
	
	public Study(String name, Organism organism)
	{
		this.name = name;
		this.organism = organism;
	}
	
	
	public Study cloneWithoutGenes()
	{
		Study theClone = new Study();
		theClone.name = this.name;
		theClone.organism = this.organism;
		theClone.spreadsheetStructures = this.spreadsheetStructures; 
		theClone.timepointColumnNames = this.timepointColumnNames;
		return theClone;
	}

	
	// Only used by the ctor.
	private class SpreadsheetIdToGeneMap extends HashMap<String, Gene>
	{
		SpreadsheetIdToGeneMap(Spreadsheet sheet)
		{
			for (Gene gene: sheet.getGenes())
				put(gene.getId(), gene);
		}
	}
	
	
	
	
	

			
			
	
			////////////////////////////////////////////////////////////////////////////
			//                                                                        //
			//                              SERIALIZATION                             //
			//                                                                        //
			////////////////////////////////////////////////////////////////////////////
			
	
	
	
	
	// Returns the file to which this object was serialized.
	public File serialize(File dirf) throws IOException
	{
		// Study and organism fields in gene instances must be set before serializing.
		for (Gene gene: this)
		{
			assert gene.getStudy() != null;
			assert gene.getOrganism() != null;
		}
		
		// Ok to serialize.
		File ofile = getSerFileForSpreadsheetFile(dirf, getPrimarySpreadsheetFile());
		FileOutputStream fos = new FileOutputStream(ofile);
		ObjectOutputStream oos = new ObjectOutputStream(fos);
		oos.writeObject(this);
		oos.flush();
		fos.flush();
		oos.close();
		fos.close();
		return ofile;
	}
	
	
	public static Study deserialize(File file) throws IOException, ClassNotFoundException, IllegalStateException
	{		
		FileInputStream fis = new FileInputStream(file);
		ObjectInputStream ois = new ObjectInputStream(fis);
		Study ret = (Study)ois.readObject();
		ois.close();
		fis.close();
				
		// Check Study instance and the Gene instances it contains for non-null organism fields.
		ret.validateGenes();
		return ret;
	}
	
	
	public void validateGenes() throws IllegalStateException
	{
		String err = null;
		for (Gene gene: this)
		{
			if (gene.getStudy() == null)
				err = "has null study field";
			else if (gene.getStudy() != this)
				err = "has wrong study field (saw " + gene.getStudy().getName() + ", expected " +
					getName() + ")";
			else if (gene.getOrganism() == null)
				err = "has null organism field";
		}
		if (err != null)
			throw new IllegalStateException("Gene " + err + " in " + getName());
	}	
	
	
	public static File getSerFileForSpreadsheetFile(File dirf, File primarySpreadsheetFile)
	{
		String ssName = primarySpreadsheetFile.getName();
		assert ssName.endsWith(".csv")  ||  ssName.endsWith(".tsv");
		String serName = ssName.substring(0, ssName.length()-4);
		serName += "__imported.ser";
		return new File(dirf, serName);
	}
	
	
	
	
	
	
	
	
	
				//////////////////////////////////////////////////////////////////////////
				//                                                                      //
				//                         EXPRESSION RESTRICTION                       //
				//                                                                      //
				//////////////////////////////////////////////////////////////////////////
				
	

	
	// Null restrictions means accept everything.
	public Vector<Gene> collectGenesThatPassRestrictions(ExpressionRestrictionModel restrictions)
	{
		Vector<Gene> ret = new Vector<Gene>();
		
		for (Gene gene: this)
			if (restrictions == null  ||  restrictions.accepts(gene.getRawExpressions()))
				ret.add(gene);
		
		return ret;
	}
	
	
	public int countGenesThatPassRestrictions(ExpressionRestrictionModel restrictions)
	{
		int n = 0;
		
		for (Gene gene: this)
			if (restrictions.accepts(gene.getRawExpressions()))
				n++;
		
		return n;
	}
	
	
	
	
	

	
			
			////////////////////////////////////////////////////////////////////////////
			//                                                                        //
			//                                 GROUPING                               //
			//                                                                        //
			////////////////////////////////////////////////////////////////////////////

	
	
	
	// Any column can be the basis of grouping except timepoint, name, id, and annotation.
	public Set<SpreadsheetColumnRole> collectGroupableColumnRoles()
	{
		Set<SpreadsheetColumnRole> ret = new HashSet<SpreadsheetColumnRole>();
		for (SpreadsheetStructure struc: spreadsheetStructures)
			ret.addAll(struc.collectGroupableColumnRoles());
		return ret;
	}

	
	public RoleValueToGenesMap buildRoleValueToGenesMap(GroupGenesBy groupBy, 
														OrderGeneGroupsBy orderBy, 
														ExpressionRestrictionModel restrictions,
														Metric metric, 
														TimeAndExpressionProvider txp)
	{
		return (groupBy.isSpreadsheetColumnRole())  ?
			new RoleValueToGenesMap(this, groupBy.getSpreadsheetColumnRole(), orderBy, restrictions, metric, txp)  :
			new AppearanceOrderRoleValueToGenesMap(this, orderBy, restrictions, metric, txp);
	}
	
	
	
			
	
	
	
			
			////////////////////////////////////////////////////////////////////////////
			//                                                                        //
			//                                   MISC                                 //
			//                                                                        //
			////////////////////////////////////////////////////////////////////////////
			


	
	public File getPrimarySpreadsheetFile()				
	{ 
		return getPrimarySpreadsheetStructure().getFile(); 
	}
	
	
	public SpreadsheetStructure getPrimarySpreadsheetStructure()
	{ 
		return spreadsheetStructures.firstElement();
	}
	
	
	// The singleton "Experiments" study always comes last.
	public int compareTo(Study that) 
	{
		if (this.isExperimentsStudy())
		{
			assert !that.isExperimentsStudy();
			return 1;
		}
		else if (that.isExperimentsStudy())
		{
			assert !this.isExperimentsStudy();
			return -1;
		}
		else
			return this.name.compareTo(that.name);
	}

	
	public boolean equals(Object x) 
	{
		Study that = (Study)x;
		return this.name.equals(that.name);
	}

	
	public int hashCode() 
	{
		return name.hashCode();
	}

	
	public String toString() 
	{
		String s = "Study " + name + " of " + size() + " genes, organism = " + organism + "\n";
		return s;
	}
	
	
	public String spreadsheetStructuresToString()
	{
		String ret = "";
		for (SpreadsheetStructure sss: spreadsheetStructures)
			ret += "************************\n" + sss;
		return ret;
	}

	
	// Used by an assertion in the main wizard panel.
	public Vector<Gene> collectGenesWithNullOrganism() 
	{
		Vector<Gene> ret = new Vector<Gene>();
		for (Gene g: this)
			if (g.getOrganism() == null)
				ret.add(g);
		return ret;
	}

	
	public float[] getMinMeanMaxRawExpressions() 
	{
		float[] ret = { Float.MAX_VALUE, 0f, -1.0e6f };
		for (Gene gene: this)
		{
			float[] geneMinMeanMax = gene.getMinMeanMaxExpressions();
			ret[0] = Math.min(ret[0], geneMinMeanMax[0]);
			ret[1] += geneMinMeanMax[1];
			ret[2] = Math.max(ret[2], geneMinMeanMax[2]);
		}
		ret[1] /= this.size();
		return ret;
	}
	
	
	public HashMapIgnoreKeyCase<Gene> getNameToGeneMap()
	{
		HashMapIgnoreKeyCase<Gene> ret = new HashMapIgnoreKeyCase<Gene>();
		for (Gene gene: this)
			if (gene.getName() != null)
				ret.put(gene.getName(), gene);
		return ret;
	}
	

	public Map<String, Gene> getIdToGeneMap()
	{
		Map<String, Gene> ret = new HashMap<String, Gene>();
		for (Gene gene: this)
			ret.put(gene.getId(), gene);
		return ret;
	}
	
	
	public Collection<String> getIds()
	{
		Collection<String> ret = new HashSet<String>();
		for (Gene gene: this)
			ret.add(gene.getId());
		return ret;
	}
	
	
	// Returns an instance with 2 genes of 5 expression values.
	public static Study buildLightweightTestInstance(String name, Organism organism)
	{
		// Build the study.
		Study ret = new Study();
		ret.name = name;
		ret.organism = organism;
		SpreadsheetStructure struc = SpreadsheetStructure.buildLightweightTestInstance();
		ret.spreadsheetStructures = new Vector<SpreadsheetStructure>();
		ret.spreadsheetStructures.add(struc);
		ret.timepointColumnNames = new Vector<String>();
		ret.timepointColumnNames.addAll(struc.getTimepointColumnNames());
		
		// Build the genes.
		String[] svals = { "ficA", "0", "1", "3", "7", "15" };
		ret.add(new Gene(ret, svals, struc));
		svals = new String[] { "ficB", "2", "2.5", "3", "2.5", "1.5" };
		ret.add(new Gene(ret, svals, struc));
		
		return ret;
	}
	
	public Vector<PredefinedSpreadsheetColumnRole> getNonTimepointPredefinedColumnRoles()
	{
		Vector<PredefinedSpreadsheetColumnRole> ret = new Vector<PredefinedSpreadsheetColumnRole>();
		
		for (SpreadsheetStructure struc: spreadsheetStructures)
			for (PredefinedSpreadsheetColumnRole role: struc.getNonTimepointPredefinedColumnRoles())
				if (!ret.contains(role))
					ret.add(role);
		
		return ret;
	}
	
	
	public Vector<String> getNonTimepointUserDefinedColumnRoles()
	{
		Vector<String> ret = new Vector<String>();

		for (SpreadsheetStructure struc: spreadsheetStructures)
			for (String srole: struc.getUserDefinedColumnRoles())
				if (!ret.contains(srole))
					ret.add(srole);
		
		return ret;
		
	}
	

	public Vector<String> getTimepointColumnNames()		
	{
		Vector<String> ret = new Vector<String>();
		for (SpreadsheetStructure struc: spreadsheetStructures)
			ret.addAll(struc.getTimepointColumnNames());
		return ret;
	}

	
	// Some spreadsheets might indicate no-value in a cell with "-" or "n/a".
	public void nullifyPlaceholderCellValues(Set<String> placeholders)
	{
		for (Gene gene: this)
			gene.nullifyPlaceholderCellValues(placeholders);
	}
	
	
	public Collection<Gene> getNamedGenes()
	{
		Map<String, Gene> sorter = new TreeMap<String, Gene>();
		for (Gene gene: this)
			if (gene.getName() != null)
				sorter.put(gene.getName(), gene);
		return sorter.values();
	}
	
	
	public Map<Gene, Vector<float[]>> mapGenesToTimeExpressionPairs(TimeAssignmentMap timeAssignments)
	{
		assert !this.isExperimentsStudy();
		
		Map<Gene, Vector<float[]>> ret = new HashMap<Gene, Vector<float[]>>();
		for (Gene gene: this)
			ret.put(gene, gene.getTimeAndExpressionPairs(timeAssignments));
		return ret;
	}
	
	
	public String getName() 										{ return name;					}
	public Organism getOrganism() 									{ return organism;				}
	void setOrganism(Organism organism)								{ this.organism = organism;     }
	public boolean isExperimentsStudy()								{ return false; 				} // subclass overrides
	protected void setName(String name)								{ this.name = name; 			}
	public Vector<SpreadsheetStructure> getSpreadsheetStructures()	{ return spreadsheetStructures; }
	public CoregulationFileCollection getCoregFiles()				{ return coregFiles; 			}
	public void setCoregFiles(CoregulationFileCollection files)		{ this.coregFiles = files; 		}
	static void sop(Object x) 										{ System.out.println(x);		}
	
	
	public static void main(String[] args)
	{
		try
		{
			File dirf = new File("data/ImportedStudies");
			File f = new File(dirf, "Shilova_Tery__imported.ser");
			Study study = deserialize(f);
			//sop(study);
			for (SpreadsheetStructure  struc: study.getSpreadsheetStructures())
				sop(struc + "==================");
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
