package dexter.util.gui;

import java.util.*;
import java.awt.*;
import javax.swing.*;


public class MultilineLabel extends JPanel
{
	public MultilineLabel(String[] lines)
	{
		this(lines, -1);
	}
	
	
	public MultilineLabel(String[] lines, int fontSize)
	{
		Vector<String> vec = new Vector<String>();
		for (String line: lines)
			vec.add(line);
		init(vec, fontSize);
	}
	
	
	public MultilineLabel(String[] lines, Font font)
	{
		Vector<String> vec = new Vector<String>();
		for (String line: lines)
			vec.add(line);
		init(vec, font);
	}
	
	
	public MultilineLabel(Vector<String> vec)
	{
		this(vec, -1);
	}
	
	
	public MultilineLabel(Vector<String> vec, int fontSize)
	{
		init(vec, fontSize);
	}
	
	
	public MultilineLabel(Vector<String> vec, int fontSize, Font font)
	{
		init(vec, font);
	}
	
	
	public MultilineLabel(String s)
	{
		this(s, -1);
	}
	
	
	public MultilineLabel(String s, int fontSize)
	{
		this(s.split("\\n"), fontSize);
	}
	
	
	public MultilineLabel(String s, Font font)
	{
		this(s.split("\\n"), font);
	}
	
	
	private void init(Vector<String> vec, int fontSize)
	{
		JLabel perdu = new JLabel("Perdu");
		String family = perdu.getFont().getFamily();
		if (fontSize < 0)
			fontSize = perdu.getFont().getSize();
		Font font = new Font(family, Font.PLAIN, fontSize);
		init(vec, font);
	}
	
	
	private void init(Vector<String> vec, Font font)
	{
		VerticalFlowLayout lom = new VerticalFlowLayout();
		lom.setHorizontalAlignment(Component.CENTER_ALIGNMENT);
		lom.setVerticalAlignment(Component.CENTER_ALIGNMENT);
		setLayout(lom);	
		for (String s: vec)
		{
			JLabel label = new JLabel(s, SwingConstants.CENTER);
			label.setFont(font);
			add(label);
		}
	}
	
	
	public void setTextColor(Color color)
	{
		for (Component c: getComponents())
			if (c instanceof JLabel)
				c.setForeground(Color.BLUE);
	}
	
	
	public void setFontFamily(String fam)
	{
		Font font = null;
		
		for (Component c: getComponents())
		{
			if (c instanceof JLabel)
			{
				if (font == null)
				{
					Font oldFont = c.getFont();
					font = new Font(fam, oldFont.getStyle(), oldFont.getSize());
				}
				c.setFont(font);
			}
		}
	}
	
	
	public void setFontStyle(int style)
	{
		Font font = null;
		
		for (Component c: getComponents())
		{
			if (c instanceof JLabel)
			{
				if (font == null)
				{
					Font oldFont = c.getFont();
					font = new Font(oldFont.getFamily(), style, oldFont.getSize());
				}
				c.setFont(font);
			}
		}
	}
	
	
	public static void main(String[] args)
	{
		String s = "abc\ndef\nghijklmnop";
		MultilineLabel that = new MultilineLabel(s, new Font("Serif", Font.BOLD+Font.ITALIC, 36));
		JFrame frame = new JFrame();
		frame.add(that, BorderLayout.CENTER);
		frame.pack();
		frame.setVisible(true);
	}
}
