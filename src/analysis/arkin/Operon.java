package analysis.arkin;

import dexter.coreg.CoregulationGroup;
import dexter.model.Study;

import java.io.*;
import java.util.*;


public class Operon extends CoregulationGroup
{
	private final static File	ARKIN_DIRF = new File("analysis_data/ArkinlabOperonPredictions");
	
	private float				meanInternalPairwiseDist;
	private double				expect = -55555;
	private boolean				flanked;
	
	
	public Operon(CoregulationGroup src)
	{
		addAll(src);
	}
	
	
	public Operon()
	{
		
	}
	
	
	public Operon(Collection<String> src)
	{
		addAll(src);
	}
	
	
	public String toString()
	{
		int len = size() - (flanked ? 2 : 0);
		String s = "Operon: length=" + len;
		if (expect >= 0)
			s += " expect=" + expect;
		for (String id: this)
			s += " " + id;
		return s;
	}
	
	
	void setFlanked(boolean flanked)
	{
		this.flanked = flanked;
	}
	
	
	void setExpect(double expect)
	{
		this.expect = expect;
	}
	
	
	double getExpect()
	{
		return expect;
	}
	
	
	void setMeanInternalPairwiseDist(float f)
	{
		this.meanInternalPairwiseDist = f;
	}
	
	
	float getMeanInternalPairwiseDist()
	{
		return meanInternalPairwiseDist;
	}
	
	
	static Vector<Operon> extractFromArkinPredictions(File f) throws IOException
	{
		Vector<TreeSet<String>> sets = new Vector<TreeSet<String>>();
		
		FileReader fr = new FileReader(f);
		BufferedReader br = new BufferedReader(fr);
		br.readLine();
		String line = null;
		while ((line = br.readLine()) != null)
		{
			if (!line.contains("TRUE"))
				continue;
			String[] pieces = line.split("\\s");
			String id1 = pieces[2].trim();
			String id2 = pieces[3].trim();
			if (id1.isEmpty()  ||  id2.isEmpty())
				continue;

			TreeSet<String> opOfId1 = null;
			TreeSet<String> opOfId2 = null;
			for (TreeSet<String> op: sets)
			{
				if (op.contains(id1))
					opOfId1 = op;
				if (op.contains(id2))
					opOfId2 = op;
				if (opOfId1 != null  &&  opOfId2 != null)
					break;
			}
			
			if (opOfId1 == null  &&  opOfId2 == null)
			{
				TreeSet<String> op = new TreeSet<String>();
				op.add(id1);
				op.add(id2);
				sets.add(op);
			}
			else
			{
				assert opOfId1 == null  ||  opOfId2 == null;
				TreeSet<String> op = (opOfId1 == null)  ?  opOfId2  :  opOfId1;
				assert op != null;
				op.add(id1);
				op.add(id2);
			}
		}
		br.close();
		fr.close();
		
		Vector<Operon> ret = new Vector<Operon>();
		for (TreeSet<String> set: sets)
			ret.add(new Operon(set));
		
		return ret;
	}
	
	
	static Vector<Operon> extractFromArkinPredictionsForCroco() throws IOException
	{
		File f = new File("analysis_data/ArkinlabOperonPredictions/Crocosphaera_watsonii_WH_8501.rkn");
		return extractFromArkinPredictions(f);
	}
	
	
	static Vector<Operon> extractFromArkinPredictionsForPro() throws IOException
	{
		File f = new File("analysis_data/ArkinlabOperonPredictions/Prochlorococcus_marinus_sp_MED4.rkn");
		return extractFromArkinPredictions(f);
	}
	
	
	static Vector<Operon> extractFromArkinPredictionsForTery() throws IOException
	{
		File f = new File("analysis_data/ArkinlabOperonPredictions/Trichodesmium_erythraeum_IMS101.rkn");
		return extractFromArkinPredictions(f);
	}
	
	
	static void sop(Object x)				{ System.out.println(x); }
	
	
	public static void main(String[] args)
	{
		try
		{
			sop("START");
			File f = new File("analysis_data/ArkinlabOperonPredictions/Prochlorococcus_marinus_sp_MED4.rkn");
			Vector<Operon> ops = extractFromArkinPredictions(f);
			for (Operon op: ops)
				sop(op.toString());
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
