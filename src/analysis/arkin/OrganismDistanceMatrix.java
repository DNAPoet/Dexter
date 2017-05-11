package analysis.arkin;

import java.util.*;
import java.io.*;

import dexter.model.*;
import dexter.cluster.*;


public class OrganismDistanceMatrix extends HalfArrayDistanceMatrix<Gene> implements Serializable, TimeAndExpressionProvider
{
	private static final long 					serialVersionUID = -6006087071587721596L;
	
	private static SessionModel					session;
	
	private Map<Gene, Vector<float[]>> 			geneToTx;

	
	static
	{
		try
		{
			session = SessionModel.deserialize(new File("data/Sessions/CPT.dex"));
		}
		catch (Exception x)
		{
			sop("Feh");
		}
	}


	public OrganismDistanceMatrix(Study study)
	{
		geneToTx = session.mapGenesToTimeExpressionPairs(study);
		int nGenes = geneToTx.size();
		setNKeys(nGenes);
		Vector<Gene> genes = new Vector<Gene>(geneToTx.keySet());
		for (int i=0; i<nGenes-1; i++)
		{
			Gene g1 = genes.get(i);
			Vector<float[]> txs1 = geneToTx.get(g1);
			for (int j=i+1; j<nGenes; j++)
			{
				sop("i=" + i + " of " + (nGenes-2) + ", j=" + j + " of " + (nGenes-1));
				Gene g2 = genes.get(j);
				float dist = Metric.EUCLIDEAN.computeDistance(g1, txs1, g2, geneToTx.get(g2));
				setDistance(g1, g2, dist);
			}
		}
	}


	public Vector<float[]> getTimeAndExpressionPairsForGene(Gene gene) 
	{
		return geneToTx.get(gene);
	}
	
	
	static SessionModel getSession()
	{
		return session;
	}
	
	
	static void dsop(Object x)
	{
		sop(new Date() + ": " + x);
	}
	
	
	public static void main(String[] args)
	{
		try
		{
			dsop("START");
			File dirf = new File("analysis_data/SerializedOrganismDistanceMatrices");
			
			for (Study study: session.getStudies())
			{
				if (!study.getOrganism().equals(Organism.PRO))
					continue;
				String fname = study.getName() + "_distances.ser";
				File serf = new File(dirf, fname);
				

				OrganismDistanceMatrix mat = new OrganismDistanceMatrix(study);
				assert false : mat;
				
				/*******
				OrganismDistanceMatrix mat = new OrganismDistanceMatrix(study);
				FileOutputStream fos = new FileOutputStream(serf);
				ObjectOutputStream oos = new ObjectOutputStream(fos);
				oos.writeObject(mat);
				oos.flush();
				fos.flush();
				oos.close();
				fos.close();
				********/
				
				/*********
				assert serf.exists();
				sop("*******\nWill deserialize " + serf.getAbsolutePath());
				FileInputStream fis = new FileInputStream(serf);
				ObjectInputStream ois = new ObjectInputStream(fis);
				OrganismDistanceMatrix mat = (OrganismDistanceMatrix)ois.readObject();
				sop(mat);
				ois.close();
				fis.close();
				********/
			}
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
