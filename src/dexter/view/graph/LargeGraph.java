package dexter.view.graph;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.*;

import dexter.model.*;
import dexter.util.gui.*;


//
// Legend is managed by LargeGraphPanel, which is this class' immediate container.
//

public class LargeGraph extends Graph
{
	private final static float[]				MAX_V_RANGE_NO_NORM		= { 0f, 16f };
	private final static float[]				MAX_V_RANGE_YES_NORM	= { -8f, 8f };
	private final static int					H_PIX_PER_HOUR			= 24;
	private final static int					V_PIX_PER_EXPR			= 32;
	private final static int					GRAPH_HEIGHT			= 16 * V_PIX_PER_EXPR;
	private final static MarginModel			MARGINS					= new MarginModel(58, 97, 20, 45);
	private final static Font					AXIS_FONT				= new Font("SansSerif", Font.PLAIN, 12);
	private final static int					N_VERTICAL_TICKS		=  5;
	private final static int					TICK_LENGTH				=  9;
	private final static int					PIN_LENGTH				= 19;
	private final static int					PIN_GRID_SIZE			= 16;
	private final static int					LOW_ZOOM_PIN_START_Y	= MARGINS.getTop() + GRAPH_HEIGHT + PIN_GRID_SIZE/2;
	private final static int					HIGH_ZOOM_PIN_START_Y	= MARGINS.getTop() + PIN_GRID_SIZE/2;
	
	private float[]								verticalXprRange;			// { low, high }
	private HorizontalPinDraggable				lowZoomPin;
	private HorizontalPinDraggable				highZoomPin;
	private float								unzoomedVPixPerExprUnit;
	
	
	LargeGraph(Graph source)
	{
		super(source, source.getSession(), H_PIX_PER_HOUR, GRAPH_HEIGHT, MARGINS);
		
		verticalXprRange = normalizeToMeans  ?  MAX_V_RANGE_YES_NORM  :  MAX_V_RANGE_NO_NORM;
		
		int graphR = MARGINS.getLeft() + graphBounds.width;
		lowZoomPin = new HorizontalPinDraggable(formatForVerticalAxis(verticalXprRange[0]),  // title
												graphR, 									 // x0
												graphR + PIN_LENGTH, 						 // x1
												LOW_ZOOM_PIN_START_Y,						 // y 
												PIN_GRID_SIZE,	 							 // gridSizePix
												true);										 // handleAtRight
		highZoomPin = new HorizontalPinDraggable(formatForVerticalAxis(verticalXprRange[1]), // title
												graphR, 									 // x0
												graphR + PIN_LENGTH, 						 // x1
												HIGH_ZOOM_PIN_START_Y,						 // y 
												PIN_GRID_SIZE,								 // gridSizePix
												true);										 // handleAtRight
		
		addMouseListener(this);
		addMouseMotionListener(this);
		
		unzoomedVPixPerExprUnit = vPixPerExprUnit;
	}
	
	
	public void paintComponent(Graphics g)
	{
		g.setColor(Color.WHITE);
		g.fillRect(0, 0, getWidth(), getHeight());
		
		// If zoomed, super.paintComponent() paints above and below the axes. Clipping is unreliable in this
		// jdk, so overpaint with white.
		super.paintComponent(g);
		g.setColor(Color.WHITE);
		g.fillRect(0, 0, getWidth(), MARGINS.getTop());
		g.fillRect(0, getGraphBottom(), getWidth(), getHeight()-getGraphBottom());
		
		paintVerticalScale(g);
		paintHorizontalScale(g);
		
		lowZoomPin.paint(g);
		highZoomPin.paint(g);
		
		HorizontalPinDraggable armedPin = null;
		if (lowZoomPin.isArmed())
			armedPin = lowZoomPin;
		else if (highZoomPin.isArmed())
			armedPin = highZoomPin;
		if (armedPin != null)
		{
			g.setColor(Color.BLACK);
			g.drawLine(MARGINS.getLeft()+1, armedPin.getYPix(), MARGINS.getLeft()+graphBounds.width-1, armedPin.getYPix());
		}
	}
	
	
	private void paintVerticalScale(Graphics g)
	{
		// Meez.
		g.setColor(Color.BLACK);
		g.setFont(AXIS_FONT);
		int tickDeltaPix = GRAPH_HEIGHT / (N_VERTICAL_TICKS - 1);
		Vector<Integer> tickYPixs = new Vector<Integer>();
		Vector<String> tickTexts = new Vector<String>();
		float xpr = verticalXprRange[0];
		float deltaXpr = (verticalXprRange[1] - verticalXprRange[0]) / (N_VERTICAL_TICKS - 1);
		int tickYPix = MARGINS.getTop() + GRAPH_HEIGHT;
		for (int i=0; i<N_VERTICAL_TICKS; i++)
		{
			tickTexts.add(formatForVerticalAxis(xpr));
			tickYPixs.add(tickYPix);
			if (i < N_VERTICAL_TICKS-1)
			{
				xpr += deltaXpr;
				tickYPix -= tickDeltaPix;
			}
			else
			{
				xpr = verticalXprRange[1];
				tickYPix = MARGINS.getTop();
			}
		}
		
		// Paint.
		for (int i=0; i<N_VERTICAL_TICKS; i++)
		{
			String s = tickTexts.get(i);
			int sw = g.getFontMetrics().stringWidth(s);
			int xText = MARGINS.getLeft() - TICK_LENGTH - 2 - sw;
			int yPix = tickYPixs.get(i);
			int baseline = yPix + 4;
			g.drawString(s, xText, baseline);
			g.drawLine(MARGINS.getLeft()-TICK_LENGTH, yPix, getWidth()-MARGINS.getRight(), yPix);
		}
	}
	
	
	private static boolean isRoundEnough(float f)
	{
		return Math.abs(f - Math.round(f)) < .0001f;
	}
	
	
	private static String formatForVerticalAxis(float f)
	{
		String ret = isRoundEnough(f)  ?  "" + (int)Math.round(f)  :  "" + f;
		if (ret.length() > 5)
			ret = ret.substring(0, 5);
		return ret;
  	}
	
	
	void setVerticalScale(float from, float to)
	{
		verticalXprRange = new float[] { from, to };
		repaint();
	}
	
	
	private void paintHorizontalScale(Graphics g)
	{
		g.setColor(Color.BLACK);
		g.setFont(AXIS_FONT);
		
		int yOfHAxis = getGraphBottom();
		int baseline = yOfHAxis + TICK_LENGTH + 24;
		int durationHours = backgroundModel.getDuration();
		for (int hour=0; hour<=durationHours; hour++)
		{
			boolean major = hour % 4 == 0;
			int len = major ? TICK_LENGTH + 10 : TICK_LENGTH;
			int xPix = MARGINS.getLeft() + Math.round(hour * hPixPerHour);
			g.drawLine(xPix, yOfHAxis, xPix, yOfHAxis+len);
			if (hour % 4 == 0)
			{
				String s = "" + hour;
				if (hour == 0)
					s += " hrs";
				int sw = g.getFontMetrics().stringWidth(s);
				int sx = xPix - sw/2;
				g.drawString(s, sx, baseline);
			}
		}
	}
	
	
	static int getGraphBottom()
	{
		return MARGINS.getTop() + GRAPH_HEIGHT;
	}
	
	
	// Superclass provides do-nothing versions of all mouse event handlers.
	public void mouseMoved(MouseEvent e)
	{
		if (lowZoomPin.contains(e))
			lowZoomPin.arm();
		else
			lowZoomPin.disarm();
		
		if (highZoomPin.contains(e))
			highZoomPin.arm();
		else
			highZoomPin.disarm();
		
		repaint();
	}
	

	public void mousePressed(MouseEvent e)
	{		
		mouseMoved(e);				// catch up
		
		if (lowZoomPin.isArmed())
			lowZoomPin.startDrag(e);
		
		else if (highZoomPin.isArmed())
			highZoomPin.startDrag(e);

		repaint();
	}
	
	public void mouseDragged(MouseEvent e)
	{	
		HorizontalPinDraggable draggingPin = null;
		if (lowZoomPin.isDragging())
			draggingPin = lowZoomPin;
		else if (highZoomPin.isDragging())
			draggingPin = highZoomPin;
		if (draggingPin == null)
			return;
		
		draggingPin.drag(e);
		int pinYPix = draggingPin.getYPix();
		float expr = yPixAbsToExpression(pinYPix);
		draggingPin.setTitle(formatForVerticalAxis(expr));
		updatePinPixLimits(draggingPin);

		repaint();
	}
	

	private void updatePinPixLimits(HorizontalPinDraggable draggingPin)
	{		
		// Set min/max for low-end pin.
		int lowPinMinY = Math.min(getGraphBottom(), highZoomPin.getYPix()+PIN_GRID_SIZE+3);
		lowZoomPin.setMinMaxYPix(lowPinMinY, getGraphBottom());
		
		// Set min/max for high-end pin.
		int highPinMaxYPix = Math.max(MARGINS.getTop(), lowZoomPin.getYPix()-PIN_GRID_SIZE-3);
		highZoomPin.setMinMaxYPix(MARGINS.getTop(), highPinMaxYPix);
	}
	
	
	public void mouseReleased(MouseEvent e)
	{
		mouseDragged(e);			// catch up
		
		if (lowZoomPin.isDragging())
		{
			lowZoomPin.stopDrag();
			mouseMoved(e);			// disarms if mouse no longer in handle
		}
		
		else if (highZoomPin.isDragging())
		{
			highZoomPin.stopDrag();
			mouseMoved(e);			// disarms if mouse no longer in handle
		}
		
		repaint();
	}
	
	
	private float yPixAbsToExpression(int yPix)
	{
		yPix = Math.max(yPix, MARGINS.getTop());
		yPix = Math.min(yPix, getGraphBottom());
		
		float graphRange = verticalXprRange[1] - verticalXprRange[0];
		float fracOfRange = (getGraphBottom() - yPix) / (float)GRAPH_HEIGHT;
		return verticalXprRange[0] + fracOfRange * graphRange;
	}
	
	
	void zoomIn()
	{
		float newLow = yPixAbsToExpression(lowZoomPin.getYPix());
		float newHigh = yPixAbsToExpression(highZoomPin.getYPix());
		verticalXprRange = new float[] { newLow, newHigh };
		lowZoomPin.moveTo(lowZoomPin.getBounds().x, LOW_ZOOM_PIN_START_Y - PIN_GRID_SIZE);
		highZoomPin.moveTo(highZoomPin.getBounds().x, HIGH_ZOOM_PIN_START_Y - PIN_GRID_SIZE);
		repaint();
	}
	
	
	void unzoom()
	{
		verticalXprRange = normalizeToMeans  ?  MAX_V_RANGE_YES_NORM  :  MAX_V_RANGE_NO_NORM;
		lowZoomPin.moveTo(lowZoomPin.getBounds().x, LOW_ZOOM_PIN_START_Y - PIN_GRID_SIZE);
		lowZoomPin.setTitle(formatForVerticalAxis(verticalXprRange[0]));
		highZoomPin.moveTo(highZoomPin.getBounds().x, HIGH_ZOOM_PIN_START_Y - PIN_GRID_SIZE);
		highZoomPin.setTitle(formatForVerticalAxis(verticalXprRange[1]));
		vPixPerExprUnit = unzoomedVPixPerExprUnit;
		repaint();
	}


	// Assumes translation to top-left corner of graph margin, y increasing down.
	protected int expressionToVPix(float xpr)			
	{ 
		float fracOfRange = (xpr - verticalXprRange[0]) / (verticalXprRange[1] - verticalXprRange[0]);
		int pixFromBottom = Math.round(fracOfRange * GRAPH_HEIGHT);
		return GRAPH_HEIGHT - pixFromBottom;
	}
	
	
	public static void main(String[] args)
	{
		dexter.MainDexterFrame.main(args);
	}
}
