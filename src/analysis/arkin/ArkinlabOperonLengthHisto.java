package analysis.arkin;

import java.io.*;
import java.net.*;
import java.util.Vector;
import dexter.coreg.*;
import dexter.util.*;


//
// Keys are operon lengths.
//
// An entry in the HTML index page is like this:
//		"<LI><A HREF=\"gnc329726.html\">Acaryochloris marina MBIC11017</A></LI>",
// The link for a tsv file is http://meta.microbesonline.org/operons/gnc329726.named
//

public class ArkinlabOperonLengthHisto extends IntegerKeyBinCounter 
	implements ArkinlabIndexByOrganism, ArkinlabSampleCyanos, ArkinlabSampleNonCyanos
{
	private final static File		ODIRF = new File("analysis_data/AllArkinlabOperonPredictions");
	private final static File		CPT_DIRF = new File("analysis_data/ArkinlabOperonPredictions");
	private final static File		CROCO_FILE = new File(CPT_DIRF, "Crocosphaera_watsonii_WH_8501.rkn");
	private final static File		PRO_FILE = new File(CPT_DIRF, "Prochlorococcus_marinus_sp_MED4.rkn");
	private final static File		TERY_FILE = new File(CPT_DIRF, "Trichodesmium_erythraeum_IMS101.rkn");
	
	private String					organismName;
	private String 					surl;
	
	
	ArkinlabOperonLengthHisto(String indexLine)
	{
		// Extract organism name.
		String organismName = extractOrganismNameFromIndexLine(indexLine);
		
		// See if there's a downloaded file.
		
		
		// Extract URL.
		surl = "http://meta.microbesonline.org/operons/";
		int n1 = indexLine.indexOf("gnc");
		int n2 = indexLine.indexOf(".html");
		String gncPiece = indexLine.substring(n1, n2);
		surl += gncPiece + ".named";
	}
	
	
	ArkinlabOperonLengthHisto(File rknFile) throws IOException
	{
		organismName = rknFile.getName();
		organismName = organismName.substring(0, organismName.length()-4);
		CoregulationFile coregFile = new CoregulationFile(rknFile, null);		// null organism
		Vector<CoregulationGroup> operons = coregFile.getCoregulationGroups();
		for (CoregulationGroup operon: operons)
			bumpCountForBin(operon.size());
	}
	
	
	ArkinlabOperonLengthHisto()		{ }
	
	
	private static String extractOrganismNameFromIndexLine(String indexLine)
	{
		int n = indexLine.indexOf(">", 10) + 1;
		String organismName = indexLine.substring(n);
		organismName = organismName.substring(0, organismName.indexOf("<"));
		organismName = StringUtils.strip(organismName, "'.()/+");
		return organismName;
	}
	
	
	// Takes ~4 secs.
	// Mean/sdev operon size = 3.1185436 / 2.120884
	static ArkinlabOperonLengthHisto forAllGenomes() throws IOException
	{
		ArkinlabOperonLengthHisto ret = new ArkinlabOperonLengthHisto();
		ret.organismName = "All available organisms";
		for (String kid: ODIRF.list())
		{
			File kidf = new File(ODIRF, kid);
			IntegerKeyBinCounter histo = new ArkinlabOperonLengthHisto(kidf);
			ret.add(histo);
		}
		return ret;
	}
	
	
	// Mean/sdev operon size = 2.578281 / 1.2693605
	static ArkinlabOperonLengthHisto forSampleCyanos() throws IOException
	{
		ArkinlabOperonLengthHisto ret = forIndexLines(CYANO_HTMLLINES);
		ret.organismName = "41 Sample Cyanobacteria";
		return ret;
	}
	
	// Mean/sdev operon size = 3.1119792 / 2.0733883
	static ArkinlabOperonLengthHisto forSampleNonCyanos() throws IOException
	{
		ArkinlabOperonLengthHisto ret = forIndexLines(NON_CYANO_HTMLLINES);
		ret.organismName = "672 Sample Non-Cyanobacteria";
		return ret;
	}
	
	
	static File indexLineToFile(String indexLine)
	{
		String orgname = extractOrganismNameFromIndexLine(indexLine);
		String[] pieces = orgname.split("\\s");
		String fname = "";
		for (String piece: pieces)
			fname += "_" + piece;
		fname = fname.substring(1);
		fname += ".rkn";
		return new File(ODIRF, fname);		
	}
	
	
	static ArkinlabOperonLengthHisto forIndexLines(String[] indexLines) throws IOException
	{
		ArkinlabOperonLengthHisto ret = new ArkinlabOperonLengthHisto();
		int nth = 0;
		for (String indexLine: indexLines)
		{
			String orgname = extractOrganismNameFromIndexLine(indexLine);
			String[] pieces = orgname.split("\\s");
			String fname = "";
			for (String piece: pieces)
				fname += "_" + piece;
			fname = fname.substring(1);
			fname += ".rkn";
			File ifile = new File(ODIRF, fname);
			nth++;
			assert ifile.exists()  :  nth + ": " + indexLine + "\nfilename = " + fname;
			ArkinlabOperonLengthHisto singleOrganismCounter = new ArkinlabOperonLengthHisto(ifile);
			ret.add(singleOrganismCounter);
		}
		return ret;
	}
	
	
	public String toString()
	{
		float[] meanAndSdev = meanAndSdevBinPopulation();
		
		String s = super.toString();
		s = s.substring(s.indexOf('\n') + 1);
		s = organismName + ":\nMean/sdev operon size = " + meanAndSdev[0] + " / " + meanAndSdev[1] + "\n" + s;
		s += "\n" + getSumOfAllCounts() + " predicted operons.";
		return s;
	}
	
	
	private void webPageToTSV() throws IOException
	{	
		sop(organismName + ": will download");
		URL url = new URL(surl);
		URLConnection conn = url.openConnection();
		BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
		String[] ofileNamePieces = organismName.split("\\s");
		String ofileName = "";
		for (String piece: ofileNamePieces)
			ofileName += "_" + piece;
		ofileName = ofileName.substring(1);
		File ofile = new File(ODIRF, ofileName);
		FileWriter fw = new FileWriter(ofile);
		String line;
		while ((line = br.readLine()) != null)
			fw.write(line + "\n");
		fw.flush();
		fw.close();
		br.close();
		sop(" .. done");
	}	
	
	
	String getOrganismName()	{ return organismName;   }
	static void sop(Object x)	{ System.out.println(x); }
	
	
	public static void main(String[] args)
	{
		try
		{
			sop("START");
			ArkinlabOperonLengthHisto histo = new ArkinlabOperonLengthHisto(TERY_FILE);
			sop(histo);
		}
		catch (Exception x)
		{
			sop("Stress: " + x.getMessage());
			x.printStackTrace(System.out);
		}
		finally
		{
			sop("DONE");
		}
	}
}
