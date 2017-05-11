package dexter.util.gui;

import java.util.*;
import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import dexter.VisualConstants;
import dexter.event.*;


public class RollableCheckBox<T> extends JPanel implements MouseListener, MouseMotionListener
{
	private final static int			CBOX_SIDE			= 13;
	private final static int			MARGINS				=  6;
	private final static int			TEXT_X				= MARGINS + CBOX_SIDE + MARGINS;
	private final static int			DFLT_TEXT_W_PIX 	= 95;
	private final static Font			FONT	 			= new Font("SansSerif", Font.PLAIN, 11);

	private T							tag;
	private String						text;
	private Color						lineColor;
	private int							lineThickness;
	private boolean						selected;
	private boolean						armed;
	private boolean						outlineWhenArmed;
	private int							textWidthPix = DFLT_TEXT_W_PIX;
	private Set<LegendListener<T>>		legendListeners;
	private SelectionAppearance			selectionAppearance;
	
	
	public RollableCheckBox(T tag, Color lineColor, boolean selected)
	{
		this(tag, tag.toString(), lineColor, selected);
	}

	
	public RollableCheckBox(T tag, String text, Color lineColor, boolean selected)
	{
		this(tag, text, lineColor, 1, selected);
	}
	

	public RollableCheckBox(T tag, String text, Color lineColor, int lineThickness, boolean selected)
	{
		this.tag = tag;
		this.text = text;
		this.lineColor = lineColor;
		this.lineThickness = lineThickness;
		this.selected = selected;
		
		outlineWhenArmed = true;
		selectionAppearance = SelectionAppearance.FILLED_BOX;
		setOpaque(false);
		
		addMouseListener(this);
		addMouseMotionListener(this);
		
		legendListeners = new HashSet<LegendListener<T>>();
		
		setToolTipText(text);
	}

	
	public static enum SelectionAppearance
	{
		FILLED_BOX, CHECK
	}
	
	
	public Dimension getPreferredSize()
	{
		int prefW = TEXT_X + textWidthPix + MARGINS;
		int prefH = MARGINS + CBOX_SIDE + MARGINS;
		return new Dimension(prefW, prefH);
	}
	
	
	public void paintComponent(Graphics g)
	{		
		// Box.
		int h = getHeight();
		int y = (h - CBOX_SIDE) / 2;
		g.setColor(lineColor);
		for (int i=0; i<lineThickness; i++)	
			g.drawRect(3-i, y-i, CBOX_SIDE+2*i, CBOX_SIDE+2*i);
		if (selected)
		{
			switch (selectionAppearance)
			{
				case FILLED_BOX:
					g.fillRect(6, y+3, CBOX_SIDE-5, CBOX_SIDE-5);
					break;
				case CHECK:
					g.setFont(FONT);
					g.drawString(VisualConstants.S_CHECKMARK, 6, y+10);
			}
		}
		
		// Text. Width is enforced by clipping.
		if (text != null)
		{
			g.setFont(FONT);
			g.drawString(text, TEXT_X, getHeight() - 8);
		}
		
		// Outline if armed.
		if (armed  &&  outlineWhenArmed)
			g.drawRect(1, 1, getWidth()-3, getHeight()-3);
	}
	
	
	public void setSelected(boolean b)
	{
		selected = b;
		repaint();
	}
	

	public void setSelectionAppearance(SelectionAppearance selectionAppearance)
	{
		this.selectionAppearance = selectionAppearance;
		repaint();
	}
	
	
	public void setText(String text)
	{
		this.text = text;
		repaint();
	}
	
	
	public void mouseEntered(MouseEvent e)
	{
		armed = true;
		repaint();
		LegendEvent<T> le = new LegendEvent<T>(tag, selected, armed);
		for (LegendListener<T> listener: legendListeners)
			listener.legendStateChanged(le);
	}
	
	
	public void mouseExited(MouseEvent e)
	{
		armed = false;
		repaint();
		LegendEvent<T> le = new LegendEvent<T>(tag, selected, armed);
		for (LegendListener<T> listener: legendListeners)
			listener.legendStateChanged(le);
	}
	
	
	public void mouseClicked(MouseEvent e)
	{
		// Left click: toggle selection.
		if (e.getButton() == MouseEvent.BUTTON1)
		{
			selected = !selected;
			repaint();
			LegendEvent<T> le = new LegendEvent<T>(tag, selected, armed);
			for (LegendListener<T> listener: legendListeners)
				listener.legendStateChanged(le);
		}
		
		// Middle or right click: do nothing, but subclasses can override.
		else
		{
			handleMiddleOrRightMouseClick(e);
		}
	}
	

	public void setLineColor(Color lineColor)						
	{
		this.lineColor = lineColor;
		repaint();
	}

	
	public void mouseMoved(MouseEvent e)							{ }
	public void mouseDragged(MouseEvent e)							{ }
	public void mousePressed(MouseEvent e)							{ }
	public void mouseReleased(MouseEvent e)							{ }
	public void addLegendListener(LegendListener<T> l)				{ legendListeners.add(l); }
	public void removeLegendListener(LegendListener<T> l)			{ legendListeners.remove(l); }
	public void setTextWidthPix(int textWidthPix)					{ this.textWidthPix = textWidthPix; }
	public static Font getTextFont()								{ return FONT; }
	public T getTag()												{ return tag; }
	public boolean isSelected()										{ return selected; }
	public boolean isArmed()										{ return armed; }
	public void setOutlineWhenArmed(boolean outlineWhenArmed)		{ this.outlineWhenArmed = outlineWhenArmed; }
	public String getText()											{ return text; }
	protected void handleMiddleOrRightMouseClick(MouseEvent e)		{ }
	static void sop(Object x)										{ System.out.println(x); }
	
	
	public static void main(String[] args)
	{
		/**/
		String s = "NifHlakjsdhflkasdhfklashdfklahsdfalkjsdhfklasdhf";
		RollableCheckBox<String> rcb = new RollableCheckBox<String>(s, Color.BLUE, true);
		rcb.setTextWidthPix(100);
		rcb.setSelectionAppearance(SelectionAppearance.CHECK);
		JFrame frame = new JFrame();
		JPanel pan = new JPanel();
		pan.add(rcb);
		frame.add(pan, BorderLayout.CENTER);
		frame.pack();
		frame.setVisible(true);
		/**/
		//dexter.view.main.MainDexterFrame.main(args);
	}
} 
