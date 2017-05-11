package dexter.util.gui;

import java.util.*;
import java.awt.Color;

import dexter.util.FloatBlender;
import dexter.util.LocalMath;


public class ColorBlender extends Stack<Color>
{
	public ColorBlender(Color startColor, Color endColor, int len)
	{
		FloatBlender rs = new FloatBlender(startColor.getRed(), endColor.getRed(), len);
		FloatBlender gs = new FloatBlender(startColor.getGreen(), endColor.getGreen(), len);
		FloatBlender bs = new FloatBlender(startColor.getBlue(), endColor.getBlue(), len);
		for (int i=0; i<len; i++)
		{
			Color color = new Color(Math.round(rs.get(i)), 
									Math.round(gs.get(i)),
									Math.round(bs.get(i)));
			push(color);
		}
	}
	
	
	public Color[] toArray()
	{
		return toArray(0, size()-1);
	}
	
	
	public Color[] toArray(int startIndex, int endIndex)
	{
		assert endIndex > startIndex;
		Color[] ret = new Color[endIndex-startIndex+1];
		int n = 0;
		for (int i=startIndex; i<=endIndex; i++)
			ret[n++] = get(i);
		return ret;
	}
}
