package dexter.model;

import java.util.*;
import java.io.*;
import dexter.util.StringUtils;


public class Spreadsheet
{
	private SpreadsheetStructure	structure;
	private Vector<Gene>			genes;
	
	
	public Spreadsheet(Study study, SpreadsheetStructure structure) 
		throws IOException, IllegalArgumentException
	{		
		assert structure != null;
		
		// File must contain exactly 1 ID column. This is the key used for merging multiple files in case
		// there are supplemental spreadsheets.
		//assert structure.getNColsForRole(SpreadsheetColumnRole.ID) == 1;
		
		this.structure = structure;
		
		// Read file and discard header rows.
		List<String[]> rows = StringUtils.readCsvOrTsvRows(structure.getFile());
		for (int i=0; i<structure.getDataStartRowNum(); i++)
			rows.remove(0);
		
		// Convert each body row to a gene.
		genes = new Vector<Gene>();
		for (String[] row: rows)
		{
			try
			{
				Gene gene = new Gene(study, row, structure);
				add(gene);
			}
			catch (IllegalArgumentException x)
			{
				String err = x.getMessage() + "\n" + StringUtils.toString(row);
				throw new IllegalArgumentException(err);
			}
		}
	}
	
	
	public void add(Gene gene)
	{
		genes.add(gene);
	}
	
	
	public Gene get(int n)
	{
		return genes.get(n);
	}
	
	
	public Vector<Gene> getGenes()
	{
		return genes;
	}
	
	
	static void sop(Object x)			{ System.out.println(x); }
	
	
	public static void main(String[] args)			
	{
		//Spreadsheet sheet = getTestInstance();
	}
}
