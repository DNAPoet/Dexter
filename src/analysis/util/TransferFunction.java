package analysis.util;

import java.awt.Color;
import java.util.LinkedHashMap;


public interface TransferFunction 
{
	public Color transfer(int count);			// null color is ok
	public LinkedHashMap<String, Color> getLegendTextToColor();
}
