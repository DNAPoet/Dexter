package dexter.util.gui;

import java.awt.*;
import java.util.*;
import javax.swing.*;
import dexter.VisualConstants;
import dexter.util.LocalMath;


public class CheckoffTable<T> extends JPanel implements VisualConstants
{
	private final static int				DFLT_FONT_SIZE		= 18;
	private final static int				BOTTOM_MARGIN		= 18;
	private final static int				TOP_BASELINE		= BOTTOM_MARGIN + DFLT_FONT_SIZE + 5;
	private final static int				BASELINE_DELTA		= DFLT_FONT_SIZE + 8;
	private final static int				SIDE_MARGIN			= 16;
	private final static int				TEXT_TO_CHECK_H_GAP	= 14;
	private final static Color				CHECKED_COLOR		= DARK_GREEN;
	private final static Color				UNCHECKED_COLOR		= Color.RED;
	
	private Vector<T>						tags;
	private Collection<T>					checkedTags;
	private boolean							markUncheckedTags;
	private int								xOfCheck;
	private Font							font;
	
	
	public CheckoffTable(Collection<T> rawTags)
	{
		this(rawTags, DFLT_FONT_SIZE);
	}
	
	
	public CheckoffTable(Collection<T> rawTags, int fontSize)
	{
		this.tags = new Vector<T>(rawTags);
		checkedTags = new HashSet<T>();
		setOpaque(true);
		font = new Font("Serif", Font.PLAIN, fontSize);
	}
	
	
	public Dimension getPreferredSize()
	{
		FontMetrics fm = getFontMetrics(font);
		int maxSW = 0;
		for (T tag: tags)
			maxSW = Math.max(maxSW, fm.stringWidth(tag.toString()));
		int checkSW = fm.stringWidth("" + CHECKMARK);
		checkSW = Math.max(checkSW, fm.stringWidth("" + XMARK));
		xOfCheck = SIDE_MARGIN + maxSW + TEXT_TO_CHECK_H_GAP;
		int w = SIDE_MARGIN + maxSW + TEXT_TO_CHECK_H_GAP + checkSW + SIDE_MARGIN;
		int h = TOP_BASELINE + (tags.size()-1)*BASELINE_DELTA + BOTTOM_MARGIN;
		return new Dimension(w, h);
	}
	
	
	public void paintComponent(Graphics g)
	{
		g.setColor(Color.WHITE);
		g.fillRect(0, 0, 2222, 1111);
		
		int baseline = TOP_BASELINE;
		g.setFont(font);
		for (T tag: tags)
		{
			g.setColor(Color.BLACK);
			g.drawString(tag.toString(), SIDE_MARGIN, baseline);
			if (tagIsChecked(tag) || tagIsXed(tag))
			{
				String s = tagIsChecked(tag)  ?  "" + CHECKMARK  :  "" + XMARK;
				g.setColor(getColorForTag(tag));
				g.drawString(s, xOfCheck, baseline);
			}
			baseline += BASELINE_DELTA;
		}
	}
	
	
	private boolean tagIsChecked(T tag)
	{
		return checkedTags.contains(tag);
	}
	
	
	private boolean tagIsXed(T tag)
	{
		return markUncheckedTags  &&  !tagIsChecked(tag);
	}
	
	
	private Color getColorForTag(T tag)
	{
		return tagIsChecked(tag)  ?  CHECKED_COLOR  :  UNCHECKED_COLOR;
	}
	
	
	public void setChecked(T tag, boolean checked)
	{
		if (checked)
			checkedTags.add(tag);
		else
			checkedTags.remove(tag);
		repaint();
	}
	
	
	public void setAllChecked(boolean checked)
	{
		if (checked)
			checkedTags.addAll(tags);
		else
			checkedTags.clear();
		repaint();
	}
	
	
	public void setMarkUnchecked(boolean b)
	{
		this.markUncheckedTags = b;
		repaint();
	}

	
	static void sop(Object x)	{ System.out.println(x); }
	
	
	public static void main(String[] args)
	{				
		try
		{
			JFrame frame = new JFrame();
			Vector<String> vec = new Vector<String>();
			for (int i=0; i<9; i++)
				vec.add("abcdefg" + i);
			CheckoffTable<String> pan = new CheckoffTable<String>(vec, 22);
			for (int i=3; i<8; i++)
				pan.setChecked(vec.get(i), true);
			pan.setMarkUnchecked(true);
			frame.add(pan, BorderLayout.CENTER);
			frame.pack();
			frame.setVisible(true);
		}
		catch (Exception x)
		{
			sop("Stress: " + x.getMessage());
			x.printStackTrace();
		}
		finally
		{
			sop("DONE");
		}
	}
}
