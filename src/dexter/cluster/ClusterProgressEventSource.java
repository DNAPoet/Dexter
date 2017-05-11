package dexter.cluster;


//
// Typically implemented by clustering algorithms to make themselves available for progress reporting.
//


public interface ClusterProgressEventSource 
{
	public void					addClusterProgressListener(ClusterProgressListener l);
	public ClusterAlgorithm		getAlgorithm();
}
