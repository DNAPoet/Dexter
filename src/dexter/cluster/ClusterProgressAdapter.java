package dexter.cluster;


public class ClusterProgressAdapter implements ClusterProgressListener
{
	public void clusteringStarted(ClusterProgressEvent e) 	{ }
	public void phaseStarted(ClusterProgressEvent e) 		{ }
	public void phaseProgressed(ClusterProgressEvent e) 	{ }
	public void phaseFinished(ClusterProgressEvent e) 		{ }
	public void clusteringFinished(ClusterProgressEvent e) 	{ }	
}
