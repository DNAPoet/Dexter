package dexter.util.gui;

import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import dexter.VisualConstants;


public class VerticalTextListClickToRemove<T> extends JPanel 
	implements MouseListener, MouseMotionListener, VisualConstants
{
	private final static Font		FONT;
	private final static int		STRIP_H			=  38;
	private final static int		BASELINE		=  25;
	private final static int		H_MARGIN		=  18;		// Left to border
	private final static int		INDENT			=   7;		// Border to text
	private final static int		DFLT_PREF_W		= 400;
	
	
	static
	{
		JPanel pan = new JPanel();
		JButton btn = new JButton("Perdu");
		pan.add(btn);
		FONT = btn.getFont();
	}
	
	
	private Vector<T>				members;
	private int						selectedIndex = -1;
	private int						prefW = DFLT_PREF_W;
	private int 					prefH = -1;
			
		
	public VerticalTextListClickToRemove()
	{
		members = new Vector<T>();
		
		addMouseListener(this);
		addMouseMotionListener(this);
		
		setOpaque(true);
	}
	
	
	// Assumes memberHorizBounds is valid.
	public Dimension getPreferredSize()
	{	
		int h = (prefH > 0)  ?  prefH  :  STRIP_H * Math.max(1, members.size()) + 1;
		return new Dimension(prefW, h);
	}
	
	
	public void add(T member)
	{
		members.add(member);
		repaint();
	}
	
	
	public void remove(T member) throws IllegalArgumentException
	{
		if (!members.contains(member))
			throw new IllegalArgumentException("Can't remove nonexistent member: " + member);
		
		members.remove(member);
		repaint();
	}
	
	
	public void paintComponent(Graphics g)
	{
		g.setColor(getBackground());
		g.fillRect(0, 0, 3333, 2222);
		
		g.setFont(FONT);
		int sline = BASELINE;
		for (T tag: members)
		{
			g.setColor(Color.BLACK);
			g.drawString(tag.toString(), H_MARGIN + INDENT, sline);
			g.drawRect(H_MARGIN, sline-BASELINE, prefW-2*H_MARGIN, STRIP_H);
			sline += STRIP_H;
		}
		
		if (selectedIndex > -1)
		{
			g.setColor(SELECTION_COLOR);
			int y = selectedIndex * STRIP_H;
			g.drawRect(H_MARGIN, y, prefW-2*H_MARGIN, STRIP_H);
			g.drawRect(H_MARGIN+1, y+1, prefW-2*H_MARGIN-2, STRIP_H-2);
		}
	}
	
	
	public void setPreferredWidth(int prefW)
	{
		this.prefW = prefW;
	}
	
	
	public void setPreferredHeight(int prefH)
	{
		this.prefH = prefH;
	}
	
	
	public void setPreferredNStrips(int nStrips)
	{
		prefH = STRIP_H*nStrips + 1;
	}
	
	
	public void mouseMoved(MouseEvent e)
	{
		selectedIndex = yToMemberIndex(e);
		repaint();
	}
	
	
	public void mouseClicked(MouseEvent e) 
	{
		int index = yToMemberIndex(e);
		if (index < 0)
			return;
		members.remove(index);
		mouseMoved(e);
		repaint();
	}

	
	public void mouseExited(MouseEvent e) 	
	{
		selectedIndex = -1;
		repaint();
	}
	
	
	private int yToMemberIndex(MouseEvent e)
	{
		return yToMemberIndex(e.getY());
	}
	
	
	private int yToMemberIndex(int y)
	{
		int n = y / STRIP_H;
		return (n < members.size())  ?  n  :  -1;
	}
	
	
	public Vector<T> getMembers()
	{
		return members;
	}

	
	public void mouseEntered(MouseEvent e) 	{ }
	public void mousePressed(MouseEvent e) 	{ }
	public void mouseReleased(MouseEvent e) { }
	public void mouseDragged(MouseEvent e) 	{ }
	
	
	static void sop(Object x)				{ System.out.println(x); }
	
	
	public static void main(String[] args)
	{
		sop("START");
		VerticalTextListClickToRemove<String> that = new VerticalTextListClickToRemove<String>();
		that.setPreferredWidth(150);
		that.add("first");
		that.add("second");
		that.add("third");
		JFrame frame = new JFrame();
		JPanel pan = new JPanel();
		pan.add(that);
		frame.add(pan, BorderLayout.CENTER);
		frame.setSize(500, 500);
		frame.setVisible(true);
	}
}
