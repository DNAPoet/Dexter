package dexter.proximity;

import java.io.*;
import dexter.util.StringUtils;


public class ProximityFileFormat implements Serializable
{
	public final static File			PROXIMITY_DIRF	= new File("data/Proximity");
	
	public final static File			TERY_FILE		= new File(PROXIMITY_DIRF, "terySet_fData.csv");	
	public final static File			MED4_FILE		= new File(PROXIMITY_DIRF, "Prochlorococcus_MED4.gb");
	public final static File			CROCO_FILE		= new File(PROXIMITY_DIRF, "AADV02.1.gbff");	
	
	public static ProximityFileFormat	FOR_TERY		= new ProximityFileFormat(ProximityFileType.DELIMITED, ',', 0, 1);
	public static ProximityFileFormat	FOR_MED4		= new ProximityFileFormat(ProximityFileType.GENBANK);
	public static ProximityFileFormat	FOR_CROCO		= new ProximityFileFormat(ProximityFileType.GENBANK);
	
	private ProximityFileType			fileType;
	private char						delimiter;
	private int							fieldNum;
	private int							nHeaderLines;
	
	
	ProximityFileFormat(ProximityFileType fileType, char delimiter, int fieldNum, int nHeaderLines)
	{
		if (fileType == ProximityFileType.DELIMITED)
			assert delimiter == ','  ||  delimiter == '\t'  ||  delimiter == ';'  :  "Illegal delimiter: " + delimiter;
		
		this.fileType = fileType;
		this.delimiter = delimiter;
		this.fieldNum = fieldNum;
		this.nHeaderLines = nHeaderLines;
	}
	
	
	ProximityFileFormat(ProximityFileType fileType)
	{
		assert fileType == ProximityFileType.GENBANK;
		
		this.fileType = fileType;
	}
	
	
	ProximityFileType getFileType()
	{
		return fileType;
	}
	
	
	char getDelimiter()
	{
		return delimiter;
	}
	
	
	int getFieldNum()
	{
		return fieldNum;
	}
	
	
	int getNHeaderLines()
	{
		return nHeaderLines;
	}
	
	
	static String stripLeadingAndTrailingQuotes(String s)
	{
		if (s.startsWith("\"")  ||  s.startsWith("'"))
			s = s.substring(1);
		if (s.endsWith("\"")  ||  s.endsWith("'"))
			s = s.substring(0, s.length()-1);
		return s;
	}

	
	String extractId(String line)
	{
		String[] pieces = StringUtils.splitHonorQuotes(line, delimiter);
		if (pieces.length <= fieldNum)
			return null;
		String ret = pieces[fieldNum];
		ret = stripLeadingAndTrailingQuotes(ret);
		return ret;
	}
}
