package dexter.cluster.tune;

class DistanceReuseReport 
{
	int			nRemainingNodes;

	// E.g. { 0, 4, 2... } would mean than in the first 3 pair selections represented by this
	// report, the first selection was the best (despite the reuse shortcut), the next selection
	// was 4th-best (from zero), and the third selection was 2nd-best. Length of array = reuse size.
	int[]		ordinals;			
	
	// For pair selection #n, sdsOfError[n] is the amount by which the separation of the pair is
	// greater than the separation of the actual closest pair. Units are normalized to standard
	// deviation of the distribution of pairwise distances. Length of array = reuse size.
	float[] 	sdsOfError;
	
	
	DistanceReuseReport(int nRemainingNodes, int[] ordinals, float[] sdsOfError)
	{
		this.nRemainingNodes = nRemainingNodes;
		this.ordinals = ordinals;
		this.sdsOfError = sdsOfError;
	}
}
