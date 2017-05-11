package dexter.cluster.tune;

import dexter.util.LongBinCounter;


//
// Only for performance tuning, so bypasses the customary
public interface NJListener 
{
	public void			choseNodesToJoin(LongBinCounter<EdgeMixture> counts);	
	public void 		reusedDistances(DistanceReuseReport reuseReport);
}
