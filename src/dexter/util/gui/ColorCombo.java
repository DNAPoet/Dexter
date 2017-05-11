package dexter.util.gui;

import java.awt.*;
import javax.swing.*;
import java.util.*;


public class ColorCombo extends JComboBox
{
	private final static int			SWATCH_PREF_W		= 70;
	private final static int			SWATCH_PREF_H		= 22;
	
	
	public ColorCombo(Vector<Color> colors)
	{
		DefaultComboBoxModel model = new DefaultComboBoxModel(colors);
		setModel(model);
		setRenderer(new Renderer());
	}
	
	
	private class Renderer extends JPanel implements ListCellRenderer
	{
		private Color		fill;
		private boolean		selected;
		
	    public Component getListCellRendererComponent(JList list, Object val, int index, boolean sel, boolean focus) 
	    {
	    	fill = (Color)val;
	    	selected = sel;
	    	return this;
	    }
	    
	    public Dimension getPreferredSize()		{ return new Dimension(SWATCH_PREF_W, SWATCH_PREF_H); }
	    
	    public void paintComponent(Graphics g)
	    {
			g.setColor(fill);
			int w = getWidth();
			int h = getHeight();
			g.fillRect(0, 0, w, h);
			if (selected)
			{
				g.setColor(Color.BLACK);
				for (int i=0; i<2; i++)
					g.drawRect(i, i, w-2*i-1, h-2*i-1);
			}
	    }
	}
	
	
	public Color getSelectedColor()
	{
		return (Color)getSelectedItem();
	}
	
	
	static void sop(Object x)		{ System.out.println(x); }
	
	
	public static void main(String[] args)
	{
		sop("START");
		JFrame frame = new JFrame();
		frame.setLayout(new FlowLayout());
		Vector<Color> colors = new Vector<Color>();
		colors.add(Color.BLUE);
		colors.add(Color.GREEN);
		colors.add(Color.MAGENTA);
		colors.add(new Color(200, 200, 255));
		ColorCombo combo = new ColorCombo(colors);
		frame.add(combo);
		sop(combo.getSelectedColor());
		frame.setSize(500, 500);
		frame.setVisible(true);
	}
}
