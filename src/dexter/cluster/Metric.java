package dexter.cluster;

import java.util.*;
import dexter.model.*;
import dexter.interpolate.*;


public enum Metric 
{
	EUCLIDEAN, PCC, FOURIER;
	
	
	public String toString()
	{
		if (this == PCC)
			return name();
		
		return name().charAt(0) + name().toLowerCase().substring(1);
	}
	
	
	public float computeDistance(Gene gene0, Vector<float[]> txs0, Gene gene1, Vector<float[]> txs1)
	{
		switch (this)
		{
			case EUCLIDEAN:
				return computeEuclideanDistance(gene0, txs0, gene1, txs1);
			case PCC:
				return computePCCDistance(gene0, txs0, gene1, txs1);
			case FOURIER:
				return computeFourierDistance(gene0, txs0, gene1, txs1);
		}
		
		assert false;
		return -12345f;
	}
	
	
				
	
	
	
	
	
				///////////////////////////////////////////////////////////////
				//                                                           //
				//                         EUCLIDEAN                         //
				//                                                           //
				///////////////////////////////////////////////////////////////

	
	
	
	private float computeEuclideanDistance(Gene gene0, Vector<float[]> txs0, 
										   Gene gene1, Vector<float[]> txs1)
	{
		return computeEuclideanDistance(txs0, txs1);
	}
	
	
	// Euclidean distance doesn't need the genes.
	public static float computeEuclideanDistance(Vector<float[]> txs0, Vector<float[]> txs1)
	{
		// Collect all represented times.
		Set<Float> representedTimesSet = new TreeSet<Float>();
		for (float[] tx: txs0)
			representedTimesSet.add(tx[0]);
		for (float[] tx: txs1)
			representedTimesSet.add(tx[0]);
		Vector<Float> representedTimesVec = new Vector<Float>(representedTimesSet);
		
		// Interpolate so that both genes contain all represented points.
		Map<Float, Float> txMap0 = interpolateUnrepresentedTimepoints(txs0, 
				 													  representedTimesVec, 
				 													  InterpolationAlgorithm.LINEAR);
		Map<Float, Float> txMap1 = interpolateUnrepresentedTimepoints(txs1, 
																	  representedTimesVec, 
																	  InterpolationAlgorithm.LINEAR);
		
		// Add areas.
		float area = 0f;
		for (int nStart=0; nStart<representedTimesVec.size()-1; nStart++)
		{
			Float tStart = representedTimesVec.get(nStart);
			float expr0Start = txMap0.get(tStart);
			float expr1Start = txMap1.get(tStart);
			Float tEnd = representedTimesVec.get(nStart+1);
			float expr0End = txMap0.get(tEnd);
			float expr1End = txMap1.get(tEnd);
			area += areaBetweenSegments(tStart, expr0Start, expr1Start, tEnd, expr0End, expr1End);
		}
		return area;
	}
	
	
	// y0a and y0b have common x. Ditto y1a and y1b. Returns true if segment a crosses segment b.
	private static boolean segmentsIntersect(float yStartA, float yStartB, float yEndA, float yEndB)
	{
		// If segments meet without crossing at either endpoint, they don't strictly intersect.
		if (yStartA == yStartB  ||  yEndA == yEndB)
			return false;
		
		return (yStartA < yStartB) != (yEndA < yEndB);
	}
	

	// Returns coordinates of intersection of 2 lines that are defined by member points. Lines are
	// x1--x2 and x3--x4
	// Source: http://en.wikipedia.org/wiki/Line-line_intersection
	private static float[] intersect2lines(float x1, float y1, float x2, float y2,
								           float x3, float y3, float x4, float y4)
	{
		float[] ret = new float[2];		// { x, y }
		
		// x
		float numer = (x1*y2 - y1*x2) * (x3-x4)  -  (x1-x2) * (x3*y4 - y3*x4);
		float denom = (x1-x2) * (y3-y4)  -  (y1-y2) * (x3-x4);
		ret[0] = numer / denom; 
		
		// y
		numer = (x1*y2 - y1*x2) * (y3-y4)  -  (y1-y2) * (x3*y4 - y3*x4);
		ret[1] = numer / denom;
		
		return ret;
	}
	
	
	static int areaBetweenSegmentsRecursionDepth = 0;
	
	
	// Segments A and B have same starting/ending xs.
	private static float areaBetweenSegments(float xStart, float yStartA, float yStartB, 
											 float xEnd, float yEndA, float yEndB)
	{
		assert areaBetweenSegmentsRecursionDepth <= 1  :  "depth = " + areaBetweenSegmentsRecursionDepth;
		
		// If segments intersect, divide area into 2 triangles and recurse.
		if (segmentsIntersect(yStartA, yStartB, yEndA, yEndB))
		{
			assert areaBetweenSegmentsRecursionDepth == 0;
			float[] intersection = intersect2lines(xStart, yStartA, xEnd, yEndA, 
												   xStart, yStartB, xEnd, yEndB);
			areaBetweenSegmentsRecursionDepth++;
			float leftArea = 
				areaBetweenSegments(xStart, yStartA, yStartB, intersection[0], intersection[1], intersection[1]);
			float rightArea = 
				areaBetweenSegments(intersection[0], intersection[1], intersection[1], xEnd, yEndA, yEndB);
			areaBetweenSegmentsRecursionDepth--;
			return leftArea + rightArea;
		}

		// Simple case: trapezoid.
		float deltaX = xEnd - xStart;
		assert deltaX >= -.1f  :  deltaX + " at " + new Date();
		float meanHeight = ( (yStartB-yStartA) + (yEndB-yEndA)) / 2;
		meanHeight = Math.abs(meanHeight);
		return deltaX * meanHeight;
	}


	
			
			
				
				///////////////////////////////////////////////////////////////
				//                                                           //
				//                            PCC                            //
				//                                                           //
				///////////////////////////////////////////////////////////////
			
			
	
	private static float computePCCDistance(Gene gene0, Vector<float[]> txs0, 
								  			Gene gene1, Vector<float[]> txs1)
	{
		// Collect all represented times.
		Set<Float> representedTimesSet = new TreeSet<Float>();
		for (float[] tx: txs0)
			representedTimesSet.add(tx[0]);
		for (float[] tx: txs1)
			representedTimesSet.add(tx[0]);
		Vector<Float> representedTimesVec = new Vector<Float>(representedTimesSet);
		
		// Interpolate so that both genes contain all represented points.
		Map<Float, Float> txMap0 = interpolateUnrepresentedTimepoints(txs0, 
				 													  representedTimesVec, 
				 													  InterpolationAlgorithm.LINEAR);
		Map<Float, Float> txMap1 = interpolateUnrepresentedTimepoints(txs1, 
																	  representedTimesVec, 
																	  InterpolationAlgorithm.LINEAR);
		assert txMap0.size() == txMap1.size();
		
		// Adjust to zero means. Change nomenclature from tx0/tx1 to xs/ys to match
		// standard PCC nomenclature.
		float[] xs = adjustToZeroMean(txMap0.values());
		float[] ys = adjustToZeroMean(txMap1.values());	
		
		// PCC is in range { -1 - +1 }, which is no good for clustering. Add 1 to translate to { 0 - 2 }.
		float rawPCC = pcc(xs, ys);
		return rawPCC + 1f;
	}
	
	
	private static float pcc(float[] xs, float[] ys)
	{
		assert xs.length == ys.length;
		
		// Meez.
		float sumOfXs = 0f;
		float sumOfYs = 0f;
		float sumOfXYs = 0f;
		float sumOfX2s = 0f;
		float sumOfY2s = 0f;
		for (int i=0; i<xs.length; i++)
		{
			float x = xs[i];
			float y = ys[i];
			sumOfXs += x;
			sumOfYs += y;
			sumOfXYs += x * y;
			sumOfX2s += x * x;
			sumOfY2s += y * y;
		}
		
		// Formula: 
		//		Numer = n * sumOfXYs  -  sumOfXs * sumOfYs
		//		Denom = root { [n*sumOfX2s - sumOfXs^2] [n*sumOfY2s - sumOfYs^2] }
		float n = xs.length;
		float numer = n * sumOfXYs  -  sumOfXs * sumOfYs;
		float denom = (n*sumOfX2s - sumOfXs*sumOfXs)  *   (n*sumOfY2s - sumOfYs*sumOfYs);
		denom = (float)Math.sqrt(denom);
		return numer / denom;
	}



	
	

			
	
	
					///////////////////////////////////////////////////////////////
					//                                                           //
					//                          FOURIER                          //
					//                                                           //
					///////////////////////////////////////////////////////////////



	private static float computeFourierDistance(Gene gene0, Vector<float[]> txs0, 
							  					Gene gene1, Vector<float[]> txs1)
	{
		return 1;
	}
	
	
	
	
					
					
					///////////////////////////////////////////////////////////////
					//                                                           //
					//                         UTILITIES                         //
					//                                                           //
					///////////////////////////////////////////////////////////////


	
	
	// Keys are times, values are expressions. Returned map contains an entry for all timepoints;
	// points not represented in representedTxs are interpolated.
	private static Map<Float, Float> interpolateUnrepresentedTimepoints(Vector<float[]> representedTxs, 
																        Collection<Float> allTimepoints,
																        InterpolationAlgorithm interpolation)
	{
		Map<Float, Float> ret = new TreeMap<Float, Float>();
		Interpolater interpolater = Interpolater.createInterpolater(interpolation, representedTxs);
		
		// Represented.
		for (float[] tx: representedTxs)
			ret.put(tx[0], tx[1]);
				
		// Unrepresented.
		for (Float time: allTimepoints)
		{
			if (ret.containsKey(time))
				continue;
			float interpolatedValue = interpolater.interpolate(time);
			ret.put(time, interpolatedValue);
		}
		
		assert ret.size() == allTimepoints.size();
		return ret;
	}
	
	
	private final static Vector<float[]> buildFarrVec(float[] ts, float[] xprs)
	{
		Vector<float[]> ret = new Vector<float[]>();
		for (int i=0; i<Math.min(ts.length, xprs.length); i++)
			ret.add(new float[] { ts[i], xprs[i] });
		return ret;
	}
	
	
	private final static Vector<float[]> buildFarrVec(float[] ts, float constantXpr)
	{
		Vector<float[]> ret = new Vector<float[]>();
		for (int i=0; i<ts.length; i++)
			ret.add(new float[] { ts[i], constantXpr });
		return ret;
	}
	
	
	private final static float[] adjustToZeroMean(Collection<Float> raw)
	{
		float sum = 0f;
		for (Float f: raw)
			sum += f;
		float mean = sum / raw.size();
		float[] ret = new float[raw.size()];
		int n = 0;
		for (Float f: raw)
			ret[n++] = f - mean;
		return ret;
	}
	
	
	public final static Vector<float[]> adjustExpressionsToZeroMean(Vector<float[]> raw)
	{
		Vector<Float> rawExpressions = new Vector<Float>();
		for (float[] tx: raw)
			rawExpressions.add(tx[1]);
		float[] zeroMeanExpressions = adjustToZeroMean(rawExpressions);
		Vector<float[]> ret = new Vector<float[]>(raw.size());
		for (int i=0; i<raw.size(); i++)
			ret.add(new float[] { raw.get(i)[0], zeroMeanExpressions[i] });
		return ret;
	}
	
	
	// Expressions are adjusted to zero mean.
	public float getMeanDistance(Collection<Gene> genes, TimeAndExpressionProvider txProvider)
	{
		// Every pair contributes twice. Whatevvs.
		float totalDistances = 0f;
		int nPairs = 0;
		for (Gene g1: genes)
		{
			Vector<float[]> txs1 = txProvider.getTimeAndExpressionPairsForGene(g1);
			Vector<float[]> txs1ZeroMean = adjustExpressionsToZeroMean(txs1);
			for (Gene g2: genes)
			{
				if (g1 == g2)
					continue;
				Vector<float[]> txs2 = txProvider.getTimeAndExpressionPairsForGene(g2);
				Vector<float[]> txs2ZeroMean = adjustExpressionsToZeroMean(txs2);
				float dist = computeDistance(g1, txs1ZeroMean, g2, txs2ZeroMean);
				totalDistances += dist;
				nPairs++;
			}
		}
		return totalDistances / nPairs;
	}
	
	
			
	
	
	
	
				///////////////////////////////////////////////////////////////
				//                                                           //
				//                          TESTING                          //
				//                                                           //
				///////////////////////////////////////////////////////////////


	
	
	
	private static void testVsZeroSameTs()
	{
		testVsConstantSameTs(0f);
	}
	
	
	private static void testVsConstantSameTs(float constant)
	{
		float[] ts = { 0, 1, 2, 3 };
		float[] xprsA = { 0, 1, 2, 3 };
		Vector<float[]> txsA = buildFarrVec(ts, xprsA);
		Vector<float[]> txsB = buildFarrVec(ts, constant);
		sop(computeEuclideanDistance(txsA, txsB));
	}
	
	
	private static void testIntersect()
	{
		float[] sect = intersect2lines(0, 0, 10, 100, 0, 10, 10, 0);
		sop("Intersection at " + sect[0] + "," + sect[1]);
	}
	
	
	private static void testAreaBetweenSegmentsCrossing()
	{
		float area = areaBetweenSegments(0, 0, 1, 100, 1, 0);
		sop(area);
	}
	
	
	private static void testCrossing()
	{
		float[] ts = { 0, 1, 2, 3 };
		float[] xprsA = { 0, 1, 2, 3 };
		float[] xprsB = { 3, 2, 1, 0 };
		Vector<float[]> txsA = buildFarrVec(ts, xprsA);
		Vector<float[]> txsB = buildFarrVec(ts, xprsB);
		sop(computeEuclideanDistance(txsA, txsB));
	}
	
	
	// http://www.statisticshowto.com/how-to-compute-pearsons-correlation-coefficients/
	private final static float[][] TEST_XS_AND_YS__POINT_529809 =
	{
		{ 43, 21, 25, 42, 57, 59 }, { 99, 65, 79, 75, 87, 81 }
	};
	
	
	private static void testPCC()
	{
		float pcc = pcc(TEST_XS_AND_YS__POINT_529809[0], TEST_XS_AND_YS__POINT_529809[1]);
		sop("Actual pcc = " + pcc + ", expected .529809");
	}
	
	
	static void sop(Object x)			{ System.out.println(x); }
	
	
	public static void main(String[] args)
	{
		try
		{
			//testCrossing();
			testPCC();
		}
		catch (Exception x)
		{
			sop("STRESS: " + x.getMessage());
			x.printStackTrace(System.out);
		}
	}
}


