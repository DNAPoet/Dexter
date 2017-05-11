package analysis.arkin;

import java.io.*;
import dexter.model.*;
import java.util.*;


public class FracCPTGenesInOperons implements GenomeSizes
{
	static int countGenesInOperons(Collection<Operon> ops)
	{
		Set<String> ids = new HashSet<String>();
		for (Operon op: ops)
			ids.addAll(op);
		return ids.size();
	}
	
	
	static void sop(Object x)		{ System.out.println(x); }
	
	
	public static void main(String[] args)
	{
		try
		{
			sop("START");

			Collection<Operon> crocoOps = Operon.extractFromArkinPredictionsForCroco();
			float count = countGenesInOperons(crocoOps);
			sop("Croco: " + count + " genes in prior operons. Frac = " + (count/N_GENES_CROCO));
			
			Collection<Operon> proOps = Operon.extractFromArkinPredictionsForPro();
			count = countGenesInOperons(proOps);
			sop("Pro: " + countGenesInOperons(proOps) + " genes in prior operons. Frac = " + (count/N_GENES_PRO));
			
			Collection<Operon> teryOps = Operon.extractFromArkinPredictionsForTery();
			count = countGenesInOperons(teryOps);
			sop("Tery: " + countGenesInOperons(teryOps) + " genes in prior operons. Frac = " + (count/N_GENES_TERY));
		}
		

		catch (Exception x)
		{
			sop("Feh");
			sop(x.getMessage());
			x.printStackTrace();
		}
	}
}


/******
Croco: 2909.0 genes in prior operons. Frac = 0.48475254
Pro: 703 genes in prior operons. Frac = 0.39807475
Tery: 2472 genes in prior operons. Frac = 0.5553808
****/
