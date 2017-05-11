package dexter.util;


public class IntegerKeyBinCounter extends BinCounter<Integer>
{
	public float[] meanAndSdevBinPopulation() 
	{
		int nObjects = 0;
		
		float meanSize = 0f;
		for (int size: keySet())
		{
			int count = getCountForBin(size);
			nObjects += count;
			meanSize += count * size;
		}
		meanSize /= nObjects;
		
		float var = 0;
		for (int size: keySet())
		{
			int count = getCountForBin(size);
			var += count * (size-meanSize) * (size-meanSize);
		}
		var /= (nObjects-1);
		float sd = (float)Math.sqrt(var);
		
		return new float[] { meanSize, sd };
	}
}
