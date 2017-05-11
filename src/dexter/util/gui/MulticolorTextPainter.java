package dexter.util.gui;

import java.awt.*;
import java.util.*;


public class MulticolorTextPainter implements Paintable
{
	private Vector<String>		textPieces;
	private Vector<Color>		colors;
	private int					x;
	private int 				baseline;
	
	
	public MulticolorTextPainter(Vector<String>	textPieces, Vector<Color> colors, int x, int baseline)
	{
		assert textPieces.size() == colors.size();
		
		this.textPieces = textPieces;
		this.colors = colors;
		this.x = x;
		this.baseline = baseline;
	}
	
	
	public MulticolorTextPainter(int x, int baseline)
	{
		this.x = x;
		this.baseline = baseline;
		
		textPieces = new Vector<String>();
		colors = new Vector<Color>();
	}
	
	
	public void add(String s, Color color)
	{
		textPieces.add(s);
		colors.add(color);
	}
	
	
	public void add(Object x, Color color)
	{
		add(x.toString(), color);
	}


	public void paint(Graphics g) 
	{
		FontMetrics fm = g.getFontMetrics();
		int x = this.x;
		for (int i=0; i<textPieces.size(); i++)
		{
			g.setColor(colors.get(i));
			g.drawString(textPieces.get(i),	x, baseline);
			x += fm.stringWidth(textPieces.get(i));
		}
	}
	
	
	public String getText()
	{
		StringBuilder sb = new StringBuilder();
		for (String s: textPieces)
			sb.append(s);
		return sb.toString();
	}
}
