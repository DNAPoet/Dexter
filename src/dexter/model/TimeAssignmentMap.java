package dexter.model;

import java.util.*;
import dexter.util.*;


//
// Keys are timepoint column names from spreadsheets. Values are elapsed hours since start.
//


public class TimeAssignmentMap extends OrderPreservingMap<String, Float>
{	
	//
	// Elapsed hours to each timepoint can be inferred if all name formats are e.g. "2D6" or all name 
	// formats are # of hours as a literal string (e.g. Zinser's Prochlorococcus study). The startDark
	// arg only matters if xDLx format is used.
	//
	public TimeAssignmentMap(Vector<String> timepointColumnNames)
	{
		assert timepointColumnNames != null;
		assert !timepointColumnNames.isEmpty();
		
		// Literal int names.
		for (String colName: timepointColumnNames)
		{
			try
			{
				put(colName, Float.parseFloat(colName));
			}
			catch (NumberFormatException x)
			{
				clear();
				break;
			}
		}
		if (!isEmpty())
			return;
		
		// xDLx names. Assume D and L phases are 12/12. If they aren't, user will have to correct manually.
		boolean xdlx = true;
		for (String colName: timepointColumnNames)
		{
			if (!StringUtils.isValidXDLXString(colName))
			{
				xdlx = false;
				break;
			}
		}
		if (xdlx)
		{
			// Arbitrarily start at hour=2. User can shift to correct position by interpolating.
			// The goal here is to get deltas right.
			put(timepointColumnNames.firstElement(), 2f);
			float prevTime = 2f;
			for (int i=1; i<timepointColumnNames.size(); i++)
			{
				int delta = xdlxDelta(timepointColumnNames.get(i), timepointColumnNames.get(i-1));
				prevTime += delta;
				put(timepointColumnNames.get(i), prevTime);
			}
			return;
		}
		
		// Unrecognized format. Distribute across 24 hours, or more if >12 points.
		clear();
		int delta = (timepointColumnNames.size() <= 12)  ?  24 / timepointColumnNames.size()  :  2;
		int hour = 1;
		for (String colName: timepointColumnNames)
		{
			put(colName, (float)hour);
			hour += delta;
		}
		return;
	}
	
	
	// Copy ctor.
	public TimeAssignmentMap(TimeAssignmentMap src)
	{
		super(src);
	}
	
	
	// Probably only for testing.
	public TimeAssignmentMap()			{ }
	
	
	private static int xdlxDelta(String later, String earlier)
	{
		if (!Character.isDigit(later.charAt(0)))
			later = "1" + later;
		char laterPhase = later.contains("L")  ?  'L'  :  'D';
		String sLaterHour = later.substring(later.indexOf(laterPhase) + 1);
		int laterHour = Integer.parseInt(sLaterHour);
		
		if (!Character.isDigit(earlier.charAt(0)))
			earlier = "1" + earlier;
		char earlierPhase = earlier.contains("L")  ?  'L'  :  'D';
		String sEarlierHour = earlier.substring(earlier.indexOf(earlierPhase) + 1);
		int earlierHour = Integer.parseInt(sEarlierHour);
		
		int delta = laterHour - earlierHour;
		if (laterPhase != earlierPhase)
			delta += 12;
		return delta;
	}
	
	
	public float getLatestTimepoint()
	{
		float latest = -1;
		for (Float f: values())
			latest = Math.max(latest, f);
		return latest;
	}
	
	
	public void setLatestTimepoint(float latestHour)
	{		
		Set<String> lateTimepointNames = new HashSet<String>();
		for (String tpName: keySet())
		{
			if (get(tpName) > latestHour)
			{
				lateTimepointNames.add(tpName);
			}
		}
		for (String tpName: lateTimepointNames)
			remove(tpName);
	}
	
	
	public String toString()
	{
		String s = "TimeAssignmentMap, " + size() + " columns:";
		for (String col: keySet())
			s += "\n  " + col + " = " + get(col);
		return s;
	}
	
	
	static void sop(Object x)			{ System.out.println(x); }
	
	
	public static void main(String[] args)
	{
		String[] cols = { "D11", "L1", "L6", "L11", "D1", "D6", "2D11", "2L1"  };
		Vector<String> colvec = new Vector<String>();
		for (String col: cols)
			colvec.add(col);
		TimeAssignmentMap that = new TimeAssignmentMap(colvec);
		sop(that);
		sop("************");
	}
}
