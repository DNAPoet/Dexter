package dexter.cluster;

public enum ClusterAlgorithm 
{
	UPGMA(false), NJ(true), NJ_REUSE(true);

	
	private boolean			deployable;			// otherwise just for debugging, e.g. UPGMA
	
	
	ClusterAlgorithm(boolean deployable)
	{
		this.deployable = deployable;
	}
	
	public String toString()
	{
		if (this == NJ)
			return "Neighbor Joining";
		else if (this == NJ_REUSE)
			return "Approximate Neighbor Joining";
		else
			return name();
	}
	
	
	public static ClusterAlgorithm[] deployableAlgorithms()
	{
		int nDeployables = 0;
		for (ClusterAlgorithm algo: values())
			if (algo.deployable)
				nDeployables++;
		ClusterAlgorithm[] ret = new ClusterAlgorithm[nDeployables];
		int n = 0;
		for (ClusterAlgorithm algo: values())
			if (algo.deployable)
				ret[n++] = algo;
		return ret;
	}
	
	
	public String[] getPhaseNames()
	{
		switch (this)
		{
			case NJ:
				return new String[] { "Join neighbors", "Connect", "Establish root" };

			default:
				return new String[] { "Build tree" };		
		}
	}
	
	
	public int[] getNStepsPerPhase(int nTaxa)
	{
		switch (this)
		{
			case NJ:
				return NeighborJoiningTreeBuilder.getNStepsPerPhase(nTaxa);

			default:
				assert false;	
				return null;
		}
	}
}
