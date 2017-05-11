package dexter.view.graph;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.*;
import javax.swing.*;
import javax.swing.border.*;
import dexter.event.*;
import dexter.model.*;
import dexter.util.gui.*;



//
// Implements MouseListener and MouseMotionListener by inheritance. Inherited implementations
// don't do anything.
//


public class ThumbnailGraph extends Graph 
{
	private final static float				HORIZ_PIX_PER_HOUR  =   6;
	private final static int				HEIGHT				= 110;
	private final static MarginModel		MARGIN_MODEL		= new MarginModel(5, 6, 13, 6);	
	private final static Font				SMALL_BORDER_FONT	= new Font("SansSerif", Font.PLAIN, 9);

	private String							title;
	private Study							study;
	private ArmState						armState;
	private ThumbnailStrip					strip;							// null if not in a strip
	private boolean							mouseArmsAndSelects;
	private Set<ThumbnailListener>			listeners;
	private TitledBorder					titledBorder;
	private LargeGraphDisplayConfig			expansionConfig;
	
	
	public ThumbnailGraph(String title,
						  SessionModel session,
						  GraphBackgroundModel backgroundModel,
						  Map<Gene, Vector<float[]>> geneToTimeAndExpression)
	{
		super(session, backgroundModel, geneToTimeAndExpression, HORIZ_PIX_PER_HOUR, HEIGHT, MARGIN_MODEL);
		assert session != null;
		assert getSession() != null;
		
		this.title = title;
		armState = ArmState.NONE;
		listeners = new HashSet<ThumbnailListener>();
		addMouseListener(this);

		Border border = BorderFactory.createLineBorder(Color.BLACK);
		if (title == null)
			title = " ";
		titledBorder = BorderFactory.createTitledBorder(border, title, 
													    TitledBorder.LEFT, TitledBorder.DEFAULT_POSITION, 
													    SMALL_BORDER_FONT);
		setBorder(titledBorder);
	}
	
	
	public void paintComponent(Graphics g)
	{
		// If armed or selected, fill with appropriate color. All but outline will be overpainted.
		switch (armState)
		{
			case NONE:
				g.setColor(Color.WHITE);
				break;
			case ARMED:
				g.setColor(ARM_COLOR);
				break;
			case SELECTED:
				assert isSelected();
				g.setColor(SELECTION_COLOR);
				break;
		}			
		g.fillRect(0, 0, 3333, 3333);

		
		super.paintComponent(g);
	}
	
	
	public void setMouseArmsAndSelects(boolean mouseArmsAndSelects)
	{
		this.mouseArmsAndSelects = mouseArmsAndSelects;
	}
	
	
	public void mouseEntered(MouseEvent me)
	{
		if (!mouseArmsAndSelects)
			return;
		if (armState != ArmState.SELECTED)
		{
			armState = ArmState.ARMED;
			repaint();
			fireThumbnailEvent(false);
		}
	}
	
	
	public void mouseExited(MouseEvent me)
	{
		if (!mouseArmsAndSelects)
			return;

		if (armState != ArmState.SELECTED)
		{
			armState = ArmState.NONE;
			repaint();
		}
	}
	
	
	public void mouseClicked(MouseEvent me)
	{

		if (!mouseArmsAndSelects)
			return;
		
		boolean shifted = ((me.getModifiers()  &  MouseEvent.SHIFT_MASK) != 0);
		armState = (armState == ArmState.SELECTED)  ?  ArmState.ARMED  :  ArmState.SELECTED;		
		repaint();
		fireThumbnailEvent(shifted);
	}
	
	
	public void addThumbnailListener(ThumbnailListener listener)
	{
		listeners.add(listener);
	}
	
	
	public void removeThumbnailListener(ThumbnailListener listener)
	{
		listeners.remove(listener);
	}
	
	
	// Threadsafe and honors event contract.
	private void fireThumbnailEvent(boolean expandNotSelect)
	{
		if (listeners.isEmpty())
			return;
		
		ThumbnailEvent event = new ThumbnailEvent(this, armState, expandNotSelect);
		(new FireThread(event)).run();
	}
	
	
	private class FireThread extends Thread
	{
		private ThumbnailEvent		event;
		
		FireThread(ThumbnailEvent event)	{ this.event = event; }
		
		public void run()
		{
			for (ThumbnailListener listener: listeners)
				if (event.getDidRequestExpansion())
					listener.thumbnailRequestedExpansion(event);
				else
					listener.thumbnailSelectionChanged(event);
		}
	}
	
	
	public void setArmState(ArmState armState)
	{
		this.armState = armState;
		repaint();
	}
	
	
	public static Dimension getPreferredSizeForBackground(GraphBackgroundModel backgroundModel)
	{
		int durationHrs = backgroundModel.getDuration();
		int prefW = 
			MARGIN_MODEL.getLeft() + (int)Math.ceil(durationHrs*HORIZ_PIX_PER_HOUR) + MARGIN_MODEL.getRight();
		int prefH =
			MARGIN_MODEL.getTop() + HEIGHT + MARGIN_MODEL.getBottom();
		return new Dimension(prefW, prefH);
	}
	
	
	public void setTitle(String title)
	{
		this.title = title;
		titledBorder.setTitle(title);
		repaint();
	}
	
	
	// Called when expansion dialog gets hidden. Applied next time this thumbnail graph gets expanded.
	public void setExpansionConfig(LargeGraphDisplayConfig expansionConfig)
	{
		this.expansionConfig = expansionConfig;
	}
	

	public LargeGraphDisplayConfig getExpansionConfig()
	{
		return expansionConfig;
	}
	
	
	public String toString()
	{
		return "ThumbnailGraph \"" + title + "\" in " + 
			((strip == null) ? "no strip" : "strip" + strip.getTitle()); 
 	}
	

	public Dimension getPreferredSize()			{ return getPreferredSizeForBackground(backgroundModel); }
	public ArmState getArmState()				{ return armState; }
	public boolean isSelected()					{ return armState == ArmState.SELECTED; }
	public void setStrip(ThumbnailStrip strip)	{ this.strip = strip; }
	public ThumbnailStrip getStrip()			{ return strip; }
	public String getTitle()					{ return title; }
	public Study getStudy()						{ return study; }
	public void setStudy(Study study)			{ this.study = study; }
	static void sop(Object x)					{ System.out.println(x); }
	
	
	public static void main(String[] args)
	{
		try
		{
			sop("START");
			dexter.MainDexterFrame.main(args);
		}
		catch (Exception x)
		{
			sop("Stress: " + x.getMessage());
			x.printStackTrace();
		}
	}
}
