package dexter.util.gui;

import java.awt.*;
import java.awt.image.BufferedImage;


public class PaintFactory 
{
	private PaintFactory()			{ }
	
	
	public static Paint makeDotTexturePaint(Color fg, Color bg, int tileSize)
	{
	    BufferedImage bim = new BufferedImage(tileSize, tileSize, BufferedImage.TYPE_INT_RGB);
	    Graphics2D g = bim.createGraphics();
	    g.setColor(bg);
	    g.fillRect(0, 0, tileSize, tileSize);
	    g.setColor(fg);
	    g.fillOval(0, 0, tileSize, tileSize);
		return new TexturePaint(bim, new Rectangle(0, 0, tileSize, tileSize));
	}
		
	
	public static Paint makeDiagonalTexturePaint(Color color1, Color color2, int tileSize)
	{
		while (tileSize % 4 != 0)
			tileSize++;
	    BufferedImage bim = new BufferedImage(tileSize, tileSize, BufferedImage.TYPE_INT_RGB);
	    Graphics2D g = bim.createGraphics();
	    g.setColor(color2);
	    g.fillRect(0, 0, tileSize, tileSize);
	    g.setColor(color1);
	    for (int x=0; x<2*tileSize; x+=tileSize/2)
	    	for (int i=0; i<tileSize/4; i++)
	    		g.drawLine(x+i, 0, 0, x+i);
		return new TexturePaint(bim, new Rectangle(0, 0, tileSize, tileSize));
	}
}
