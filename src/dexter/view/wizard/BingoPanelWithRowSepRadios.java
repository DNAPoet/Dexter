package dexter.view.wizard;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;
import dexter.util.*;
import dexter.util.gui.*;


public class BingoPanelWithRowSepRadios extends BingoPanel // implements ItemListener by inheritance
{
	private final static MarginModel		MARGIN_MODEL	= new MarginModel(26, 0, 0, 0);
	private final static int				RADIO_X			= 7;
	
	private Vector<JRadioButton>			radios;
	
	
	BingoPanelWithRowSepRadios(Vector<Vector<String>> rowses, int dataStartRow)
	{
		this(rowses, dataStartRow, Integer.MAX_VALUE);
	}
	
	
	BingoPanelWithRowSepRadios(Vector<Vector<String>> rowses, int dataStartRow, int maxCellWidth)
	{
		super(rowses, maxCellWidth);
		
		assert dataStartRow >= 1;
		
		setMarginModel(MARGIN_MODEL);
		
		radios = new Vector<JRadioButton>();
		ButtonGroup bgrp = new ButtonGroup();
		for (int i=0; i<rowses.size()-1; i++)
		{
			JRadioButton radio = new JRadioButton("");
			radio.setSelected(i == dataStartRow-1);
			radio.setToolTipText("Click to designate where header rows end and data rows begin.");
			bgrp.add(radio);
			radio.addItemListener(this);
			add(radio);
			radios.add(radio);
		}
	}
	
	
	// Called by superclass' layout manager.
	protected void doSubclassLayout()
	{
		Vector<Rectangle> col0CellBoundses = getCellBoundsForColumn(0);
		Dimension radioPref = radios.firstElement().getPreferredSize();
		for (int i=0; i<col0CellBoundses.size()-1; i++)
		{
			Rectangle upperCellBounds = col0CellBoundses.get(i);
			Rectangle lowerCellBounds = col0CellBoundses.get(i+1);
			int y = (upperCellBounds.y + upperCellBounds.height + lowerCellBounds.y) / 2;
			y -= radioPref.height / 2;
			y -= 1;
			radios.get(i).setSize(radioPref);
			radios.get(i).setLocation(RADIO_X, y);
		}
	}
	
	
	public void paintComponent(Graphics g)
	{
		super.paintComponent(g);
		
		for (JRadioButton radio: radios)
		{
			if (!radio.isSelected())
				continue;
			Rectangle r = radio.getBounds();
			int x = r.x + r.width/2;
			int y = r.y + r.height/2;
			g.setColor(Color.LIGHT_GRAY);
			g.drawLine(x, y-1, getWidth(), y-1);
			g.drawLine(x, y+1, getWidth(), y+1);
			g.setColor(Color.DARK_GRAY);
			g.drawLine(x, y, getWidth(), y);
		}
	}
		
	
	int getNHeaderRows()
	{
		for (JRadioButton radio: radios)
			if (radio.isSelected())
				return 1 + radios.indexOf(radio);
		return -1;
	}
	
	
	public void itemStateChanged(ItemEvent e)
	{
		if (radios.contains(e.getSource()))
		{
			repaint();
		}
	
		else
		{
			super.itemStateChanged(e);
		}
	}
	

	public static void main(String[] args)
	{
		try
		{
			JFrame frame = new JFrame();
			Vector<Vector<String>> rowses = new Vector<Vector<String>>();
			for (int row=0; row<4; row++)
			{
				Vector<String> r = new Vector<String>();
				for (int col=0; col<6; col++)
				{
					String s = "RC " + row + ":" + col;
					if (row==0 && col==0)
						s = "xxxxxxxxxxxxxxxxx";
					r.add(s);
				}
				rowses.add(r);
			}

			BingoPanelWithRowSepRadios pan = new BingoPanelWithRowSepRadios(rowses, 2);
			frame.add(pan, BorderLayout.CENTER);
			frame.pack();
			frame.setVisible(true);
		}
		catch (Exception x)
		{
			sop("Stress: " + x.getMessage());
			x.printStackTrace();
		}
	}
}
