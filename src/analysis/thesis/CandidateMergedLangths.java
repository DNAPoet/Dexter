package analysis.thesis;

import java.io.*;
import java.util.*;


public class CandidateMergedLangths 
{

	
	static float[] minMeanMax(Vector<Integer> vec)
	{
		float[] ret = new float[] { Integer.MAX_VALUE, 0f, -1 };
		for (Integer i: vec)
		{
			ret[0] = Math.min(ret[0], i);
			ret[1] += i;
			ret[2] = Math.max(ret[2], i);
		}
		ret[1] /= vec.size();
		return ret;
	}
	
	
	static void sop(Object x)				{ System.out.println(x); }
	
	
	public static void main(String[] args)
	{
		try
		{
			sop("START");
			File csv = new File("analysis_data/MyRecommendations/AllCandidates.csv");
			FileReader fr = new FileReader(csv);
			BufferedReader br = new BufferedReader(fr);
			String line = null;
			br.readLine();
			
			Map<String, Vector<Integer>> orgToLens = new HashMap<String, Vector<Integer>>();
			orgToLens.put("Ter", new Vector<Integer>());
			orgToLens.put("PMM", new Vector<Integer>());
			orgToLens.put("Cwa", new Vector<Integer>());
			
			while ((line = br.readLine()) != null)
			{
				String[] pieces = line.split(",");
				assert pieces.length == 10;
				String org = pieces[0].substring(0,  3);
				int len = Integer.parseInt(pieces[7]) + Integer.parseInt(pieces[8]) + Integer.parseInt(pieces[9]);
				orgToLens.get(org).add(len);
			}
			
			for (String org: orgToLens.keySet())
			{
				float[] mmm = minMeanMax(orgToLens.get(org));
				sop(orgToLens.get(org).size() + " for " + org + ": " + mmm[0] + "  " + mmm[1] + "  " + mmm[2]);
			}
			
			br.close();
			fr.close();
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

/******
 * 
144 for Ter: 4.0  5.6666665  15.0
36 for PMM: 4.0  6.7222223  26.0
124 for Cwa: 4.0  5.4919353  15.0

*****/
