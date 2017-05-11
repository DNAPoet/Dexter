package dexter.cluster;

import java.util.*;


//
// T is the type of abstraction for which distance is stored (probably Node<Gene> unless debugging). 
//


public interface DistanceMatrix<T> 
{
	public float 			getDistance(T t1, T t2);
	public void 			setDistance(T t1, T t2, float distance);
	public Collection<T>	keys();
	public int				nKeys();
}
