package analysis.arkin;

import java.io.*;
import java.util.*;

import dexter.util.*;
import dexter.model.*;
import dexter.coreg.*;
import dexter.proximity.*;


class OperonPair 
{
	private Operon						upstreamOperon;
	private Operon						downstreamOperon;
	private OrganismDistanceMatrix 		distances;
	private Map<String, Gene>			idToGene;
	
	
	OperonPair(Operon upstreamOperon, Operon downstreamOperon, OrganismDistanceMatrix distances, Map<String, Gene> idToGene)
	{
		this.upstreamOperon = upstreamOperon;
		this.downstreamOperon = downstreamOperon;
		this.distances = distances;
	}
	
	
	// Mean of all pairwise distances from a gene in one operon to a gene in the other.
	float getMeanExteriorDistance()
	{
		float totalDist = 0f;
		int nPairs = 0;
		for (String id1: upstreamOperon)
		{
			Gene g1 = idToGene.get(id1);
			for (String id2: downstreamOperon)
			{
				Gene g2 = idToGene.get(id2);
				float dist = distances.getDistance(g1, g2);
				totalDist += dist;
				nPairs++;
			}
		}
		return totalDist / nPairs;
	}
	
	
	boolean predictedSameOperon()
	{
		sop(getMeanExteriorDistance());
		return true;
	}
	
	
	static void sop(Object x)	{ System.out.println(x); }
	
	
	public static void main(String[] args)
	{
		try
		{
			sop("START");
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
