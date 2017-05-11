package dexter.util.gui;

import java.awt.*;
import javax.swing.*;

import java.util.*;


public class RedClockIcon implements Icon, dexter.VisualConstants
{
	private final static String			FONT_FAMILY 		= "Apple Symbols";
	private final static int			RECOMMEND_SIZE		= 24;
	private final static Font			SYSTEM_BTN_FONT		= (new JButton("Perdu")).getFont();
	
	private int 						size;
	private Font						font;
	
	
	public RedClockIcon()
	{
		this(RECOMMEND_SIZE);
	}
	
	
	public RedClockIcon(int size)
	{
		this.size = size;
		font = new Font(FONT_FAMILY, Font.PLAIN, size);
	}
	
	
	// Doesn't seem to have much effect.
	public int getIconWidth() 
	{
		return 10;
	}
	
	
	public int getIconHeight() 
	{
		return size;
	}

	
	// Font returned by g.getFont() might be used to draw button text without checking for
	// modification. Cache and replace the entry font.
	public void paintIcon(Component c, Graphics g, int x, int y) 
	{
		g.setColor(Color.RED);
		Font cachedFont = g.getFont();
		g.setFont(font);
		int baseline = size + (c.getHeight()-size)/2 - 3;
		g.drawString("" + CLOCK, 7, baseline);
		g.setFont(cachedFont);
		
		g.setFont(SYSTEM_BTN_FONT);
	}
	
	
	public static boolean supportedOnThisPlatform()
	{
		String[] familyNames = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
		for (String fam: familyNames)
			if (fam.equals(FONT_FAMILY))
				return true;
		return false;
	}
	
	
	public static void main(String[] args)
	{
		Icon ike = new RedClockIcon();
		JButton ikeBtn = new JButton("With icon", ike);
		ikeBtn.setIconTextGap(6);
		JPanel pan = new JPanel();
		pan.add(ikeBtn);
		JButton noIkeBtn = new JButton("No icon");
		noIkeBtn.setPreferredSize(ikeBtn.getPreferredSize());
		pan.add(noIkeBtn);
		JButton noIkeBtn2 = new JButton("No icon");
		pan.add(noIkeBtn2);
		JFrame frame = new JFrame();
		frame.add(pan, BorderLayout.SOUTH);
		frame.pack();
		frame.setVisible(true);
	}
}
