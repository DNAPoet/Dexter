package dexter.util;

import java.io.*;
import java.util.*;


public class TabularBlastReader 
{	
	private BufferedReader				srcReader;
	
	
	public TabularBlastReader(BufferedReader srcReader) throws IOException
	{
		this.srcReader = srcReader;
	}
	
	
	public void close() throws IOException
	{
	}
	
	
	public TabularBlastHit readBlastHit() throws IOException
	{
		String line = srcReader.readLine();
		if (line == null)
			return null;
		else
			return new TabularBlastHit(line);
	}
	
	
	static void sop(Object x)			{ System.out.println(x);  }
	
	
	public static void main(String[] args)
	{
		try
		{
		

		}
		catch (Exception x)
		{
			sop("Stress");
			x.printStackTrace();
		}
		finally
		{
			sop("Done");
		}
	}
}
