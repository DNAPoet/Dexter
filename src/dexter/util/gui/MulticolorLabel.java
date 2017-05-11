package dexter.util.gui;

import java.awt.*;
import javax.swing.*;
import java.util.*;


public class MulticolorLabel extends JPanel
{
	// Top/bottom of margin model are distance from baseline to top/bottom of component.
	private final static MarginModel		DFLT_MARGINS 	= new MarginModel(4, 4, 20, 4);	// l, r, t, b
	private final static Font				DFLT_FONT;
	
	static
	{
		DFLT_FONT = (new JLabel("never see me")).getFont();
	}

	
	private MarginModel						margins 		= DFLT_MARGINS;
	private MulticolorTextPainter			painter;
	private Font							textFont		= DFLT_FONT;
	
	
	public MulticolorLabel(Vector<String> textPieces, Vector<Color> colors)
	{		
		painter = new MulticolorTextPainter(textPieces, colors, margins.getLeft(), margins.getTop());
	}
	
	
	public MulticolorLabel(Vector<String> textPieces, Vector<Color> colors, MarginModel margins)
	{		
		this.margins = margins;
		painter = new MulticolorTextPainter(textPieces, colors, margins.getLeft(), margins.getTop());
	}
	
	
	public MulticolorLabel()
	{
		this(DFLT_MARGINS);
	}
	
	
	public MulticolorLabel(MarginModel margins)
	{
		this.margins = margins;
		painter = new MulticolorTextPainter(margins.getLeft(), margins.getTop());
	}
	
	
	public void add(String s, Color color)
	{
		assert painter != null;
		painter.add(s, color);
	} 
	
	
	public void add(Object x, Color color)
	{
		assert painter != null;
		painter.add(x, color);
	}
	
	
	public Dimension getPreferredSize()
	{
		int sw = getFontMetrics(textFont).stringWidth(painter.getText());
		int w = margins.getLeft() + sw + margins.getRight();
		int h = margins.getTop() + margins.getBottom();
		return new Dimension(w, h);
	}
	
	
	public void setFont(Font newFont)
	{
		this.textFont = newFont;
	}
	
	
	public void setFontSize(int size)
	{
		textFont = new Font(textFont.getFamily(), textFont.getStyle(), size);
	}
	
	
	public void paintComponent(Graphics g)
	{
		painter.paint(g);
	}
	
	
	public static void main(String[] args)
	{
		MulticolorLabel label = new MulticolorLabel();
		label.add("RED  ", Color.RED);
		label.add("GREEN  ", Color.GREEN);
		label.add("CYAN  ", Color.CYAN);
		JFrame frame = new JFrame();
		frame.add(label);
		frame.pack();
		frame.setVisible(true);
	}
}
