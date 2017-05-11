package dexter.util.gui;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.*;


//
// getSelectionModel().getSelectedIndex() doesn't work, so keep a list of menu items.
//

public class ColorPopupMenu extends JPopupMenu implements ActionListener
{
	private Vector<Color>			colors;
	private Vector<JMenuItem>		menuItems;
	private ActionListener			listener;
	private int						indexOfLastSelection = -1;
	
	
	public ColorPopupMenu(Vector<Color> colors)
	{
		this(colors, null);
	}
	
	
	public ColorPopupMenu(Vector<Color> colors, ActionListener listener)
	{
		this.colors = new Vector<Color>(colors);
		this.listener = listener;
		
		menuItems = new Vector<JMenuItem>();
		for (Color color: colors)
		{
			JMenuItem mi = new JMenuItem(new ColorIcon(color));
			mi.addActionListener(this);
			add(mi);
			menuItems.add(mi);
		}
	}
	
	
	public int getSelectedIndex()
	{
		return indexOfLastSelection;
	}
	
	
	public Color getSelectedColor()
	{
		int index = getSelectedIndex();
		return (index < 0)  ?  null  :  colors.get(index);
	}
	
	
	public void actionPerformed(ActionEvent e)
	{
		indexOfLastSelection = menuItems.indexOf(e.getSource());
		if (listener != null)
		{
			listener.actionPerformed(e);
		}
	}
	
	
	private class ColorIcon implements Icon
	{
		private Color		color;
		
		ColorIcon(Color color)		{ this.color = color; }
		public int getIconWidth()	{ return 120; }
		public int getIconHeight()	{ return  35; }
		
		public void paintIcon(Component c, Graphics g, int x, int y)
		{
			g.setColor(color);
			g.fillRect(x, y, getIconWidth(), getIconHeight());
			g.setColor(Color.BLACK);
			g.drawRect(x, y, getIconWidth(), getIconHeight());
		}
	}
	
	
	static void sop(Object x)		{ System.out.println(x); }
}
