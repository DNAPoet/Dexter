package dexter.view.restrict;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;
import dexter.VisualConstants;
import dexter.model.*;
import dexter.util.StringUtils;
import dexter.util.gui.*;
import dexter.cluster.Metric;


public class RestrictionPanel extends JPanel implements VisualConstants, ItemListener
{
	private final static int			MAX_MINGENES					=  10;
	private final static int			RESTRICTOR_W_PIX				= 275;
	private final static int			RESTRICTOR_H_PIX				= 550;
	private final static MarginModel	MARGIN							= new MarginModel(60, 120, 45, 20);
	private final static int			PIN_LENGTH_RIGHT_OF_RIGHT_AXIS	=  15;
	private final static int			PIN_GRID_SIZE					=  18;
	private final static int			GRAPH_H_PIX						= 
		RESTRICTOR_H_PIX - MARGIN.getTop() - MARGIN.getBottom();
	private final static float			EXPRESSION_PER_Y_PIX			=  16f / GRAPH_H_PIX;
	private final static int[]			BASELINES						= { 20, 37 };
	
	private static Font					labelFont;

	private SessionModel				session;						// null is ok for debugging
	private TimeAndExpressionProvider	txProvider;
	private GroupGenesBy				groupBy;
	private OrderGeneGroupsBy			orderBy;
	private Metric 						metric;
	private JComboBox					minGenesCombo;
	private Restrictor[]				restrictors;
	private Vector<JLabel>				countLabels;					// Total, then 1 per study
	private boolean						updateCountLabelsRealTime;
	
	
	// For debugging, session can be null. In that case a default restriction model will be used for
	// initialization.
	public RestrictionPanel(SessionModel session)
	{
		this(session, 
			 null,
			 null, 
			 null,
			 Metric.EUCLIDEAN,
			 null);
	}
	
	
	// Restricts target strips and thumbnails in the selection model if provided. If 
	// target is null, restricts everything.
	public RestrictionPanel(SessionModel session, 
				      	    ExpressionRestrictionModel initializer,
				      	    GroupGenesBy groupBy,
					        OrderGeneGroupsBy orderBy,
					        Metric metric,
					        TimeAndExpressionProvider txProvider)
	{
		this.session = session;
		this.groupBy = groupBy;
		this.orderBy = orderBy;
		this.metric = metric;
		this.txProvider = txProvider;
		
		// If no initializing restriction is provided, use one that allows everything.
		if (initializer == null)
			initializer = new ExpressionRestrictionModel();
		
		if (labelFont == null)
			labelFont = (new JLabel("Perdu")).getFont();
		
		setLayout(new BorderLayout());
		
		JPanel north = new JPanel();
		north.add(new JLabel("Min genes per cluster"));
		int minGenes = initializer.getMinGenesPerThumbnail();
		assert minGenes >= 1  &&  minGenes <=MAX_MINGENES;
		Integer[] comboItems = new Integer[MAX_MINGENES];
		for (int i=0; i<MAX_MINGENES; i++)
			comboItems[i] = i+1;
		minGenesCombo = new JComboBox(comboItems);
		minGenesCombo.setSelectedIndex(minGenes-1);
		minGenesCombo.addItemListener(this);
		north.add(minGenesCombo);
		add(north, BorderLayout.NORTH);
		
		JPanel center = new JPanel();
		restrictors = new Restrictor[RestrictBy.values().length];
		Map<RestrictBy, float[]> byToInit = new HashMap<RestrictBy, float[]>();
		byToInit.put(RestrictBy.Mean_expression, initializer.getMinMaxOfMean());
		byToInit.put(RestrictBy.Expression_range, initializer.getMinMaxOfExpression());
		byToInit.put(RestrictBy.Expression_delta, initializer.getMinMaxOfDeltaExpression());
		int n = 0;
		for (RestrictBy by: RestrictBy.values())
		{
			Restrictor restrictor = new Restrictor(by, byToInit.get(by));
			center.add(restrictor);
			restrictors[n++] = restrictor;
		}
		add(center, BorderLayout.CENTER);
		
		if (session != null)
		{
			JPanel south = new JPanel(new GridLayout(0, 1));
			int nStudies = session.getStudiesOmitExperiments().size();
			countLabels = new Vector<JLabel>();
			for (int i=0; i<=nStudies; i++)
			{
				JLabel label = new JLabel("", SwingConstants.LEFT);
				countLabels.add(label);
				south.add(label);
			}
			updateCountLabels();
			add(south, BorderLayout.SOUTH);
			updateCountLabelsRealTime = session.getNGenes() < 2000;
		}
	}
	

	private enum RestrictBy
	{
		Mean_expression, Expression_range, Expression_delta;
		
		public String toString()
		{
			String[] pieces = name().split("_");
			return pieces[0] + " " + pieces[1];
		}
	}
	
	
	private enum Range
	{
		RESTRICT_ALL, RESTRICT_SELECTED;
		
		public String toString()
		{
			return StringUtils.enumConstToPresentableName(name());
		}
	}
	
	
	private class Restrictor extends JPanel
	{
		private RestrictBy					restrictBy;
		private HorizontalPinDraggable[]	pins;			// { low, high }
		
		Restrictor(RestrictBy restrictBy, float[] initExpressions)
		{			
			assert initExpressions[0] <= initExpressions[1]  :  initExpressions[0] + " > " + initExpressions[1];
			
			this.restrictBy = restrictBy;
			
			// Pins.
			pins = new HorizontalPinDraggable[2];
			for (int i=0; i<2; i++)
			{
				int pinY = expressionToYPix(initExpressions[i]);
				pins[i] = new HorizontalPinDraggable(expressionToText(initExpressions[i]),	// label
							MARGIN.getLeft(),												// pin left
							getGraphRight() + PIN_LENGTH_RIGHT_OF_RIGHT_AXIS, 				// pin right
							pinY + PIN_GRID_SIZE/2,											// pin y
							PIN_GRID_SIZE,													// grid size
							true);															// handle at right
			}
			updatePinPixLimits(pins);
			
			// Events.
			MLis mlis = new MLis(this, pins);
			addMouseListener(mlis);
			addMouseMotionListener(mlis);
			
			setBorder(BorderFactory.createLineBorder(Color.BLACK));
		}
		
		public Dimension getPreferredSize()
		{
			return new Dimension(RESTRICTOR_W_PIX, RESTRICTOR_H_PIX);
		}
		
		public void paintComponent(Graphics g)
		{
			// Background fill.
			g.setColor(Color.WHITE);
			g.fillRect(0, 0, 3333, 3333);
			
			// Text.
			String[] texts = { "Restrict by", restrictBy.toString() };
			g.setColor(Color.BLACK);
			g.setFont(labelFont);
			FontMetrics fm = g.getFontMetrics();
			for (int i=0; i<2; i++)
			{
				String s = texts[i];
				int sw = fm.stringWidth(s);
				int x = MARGIN.getLeft() + (getGraphWidth() - sw) / 2;
				g.drawString(s, x, BASELINES[i]);
			}
			
			// "Forbidden zones" paint.
			Graphics2D g2 = (Graphics2D)g;
			g2.setPaint(FORBIDDEN_ZONE_PAINT);
			int h = pins[1].getYPix() - MARGIN.getTop();
			g2.fillRect(MARGIN.getLeft(), MARGIN.getTop(), getGraphWidth(), h);
			h = getGraphBottom() - pins[0].getYPix();
			g2.fillRect(MARGIN.getLeft(), pins[0].getYPix(), getGraphWidth(), h);
			
			// Graph outline.
			g.setColor(Color.BLACK);
			g.drawRect(MARGIN.getLeft(), MARGIN.getTop(), getGraphWidth(), GRAPH_H_PIX);
			
			// Pins.
			for (HorizontalPinDraggable pin: pins)
				pin.paint(g);
		}
		
		float[] getRange()
		{
			float[] ret = new float[2];
			for (int i=0; i<2; i++)
				ret[i] = yPixToExpression(pins[i].getYPix());
			return ret;
		}
	}  // end of inner class Restrictor	
	
	
	private static String expressionToText(float xpr)
	{
		if (xpr == Math.round(xpr))
		{
			String s =  "" + (int)xpr;
			if (xpr < 10f)
				s = " " + s;
			return s;
		}
		
		String s = "" + xpr;
		int len = (xpr >= 10f)  ?  5  :  4;
		if (s.length() > len)
			s = s.substring(0, len);
		if (xpr < 10f)
			s = " " + s;
		return s;
	}
	
	
	private class MLis extends MouseAdapter
	{
		private Restrictor						restrictor;
		private HorizontalPinDraggable[]		pins;
		
		MLis(Restrictor restrictor, HorizontalPinDraggable[] pins)
		{
			this.restrictor = restrictor;
			this.pins = pins;
		}
		
		public void mouseMoved(MouseEvent e)
		{			
			for (HorizontalPinDraggable pin: pins)
			{
				if (pin.contains(e))
					pin.arm();
				else
					pin.disarm();
			}
			repaint();
		}
		
		public void mousePressed(MouseEvent e)
		{		
			mouseMoved(e);				// catch up
			for (HorizontalPinDraggable pin: pins)
			{		
				if (pin.isArmed())
					pin.startDrag(e);
			}
			repaint();
		}
		
		public void mouseDragged(MouseEvent e)
		{	
			for (HorizontalPinDraggable pin: pins)
			{			
				if (pin.isDragging())
				{
					pin.drag(e);
					int pinYPix = pin.getYPix();
					float expr = yPixToExpression(pinYPix);
					pin.setTitle(expressionToText(expr));
					updatePinPixLimits(pins);
				}
			}
			repaint();
			if (updateCountLabelsRealTime)
				updateCountLabels();
		}
		
		public void mouseReleased(MouseEvent e)
		{
			mouseDragged(e);			// catch up
			for (HorizontalPinDraggable pin: pins)
			{
				if (pin.isDragging())
				{
					pin.stopDrag();
					mouseMoved(e);			// disarms if mouse no longer in handle
				}
			}
			repaint();
			updateCountLabels();
		}
	}  // End of inner class MLis
	
	
	private void updatePinPixLimits(HorizontalPinDraggable[] pins)
	{
		// Set min/max for low-end pin.
		int lowerPinMinY = Math.min(getGraphBottom(), pins[1].getYPix()+PIN_GRID_SIZE+3);
		pins[0].setMinMaxYPix(lowerPinMinY, getGraphBottom());
		
		// Set min/max for high-end pin.
		int upperPinMaxYPix = Math.max(MARGIN.getTop(), pins[0].getYPix()-PIN_GRID_SIZE-3);
		pins[1].setMinMaxYPix(MARGIN.getTop(), upperPinMaxYPix);
	}

	
	private float yPixToExpression(int yPix)
	{
		int deltaPix = getGraphBottom() - yPix;
		float xpr = deltaPix * EXPRESSION_PER_Y_PIX;
		if (xpr <= .001f)
			return 0f;
		if (xpr >= 15.999f)
			return 16f;
		return xpr;
	}
	
	
	private int expressionToYPix(float xpr)
	{
		return getGraphBottom() - Math.round(xpr / EXPRESSION_PER_Y_PIX);
	}
	
	
	public ExpressionRestrictionModel getExpressionRestrictions()
	{
		ExpressionRestrictionModel ret = new ExpressionRestrictionModel();
		ret.setMinGenesPerThumbnail((Integer)minGenesCombo.getSelectedItem());
		ret.setMinMaxOfMean(restrictors[0].getRange());
		ret.setMinMaxOfExpression(restrictors[1].getRange());
		ret.setMinMaxOfDeltaExpression(restrictors[2].getRange());
		return ret;
	}
	
	
	public void setGroupBy(GroupGenesBy groupBy)
	{
		this.groupBy = groupBy;
		updateCountLabels();
	}
	
	
	private void updateCountLabels()
	{
		if (session == null)
			return; 
		
		ExpressionRestrictionModel restrictions = getExpressionRestrictions();
		int labelIndex = 1;
		int totalAcceptedGenes = 0;
		for (Study study: session.getStudiesOmitExperiments())
		{
			RoleValueToGenesMap roleToGenes = 
				RoleValueToGenesMap.manufacture(study, groupBy, orderBy, restrictions, metric, txProvider);
			int acceptedThisStudy = 0;
			for (Collection<Gene> genes: roleToGenes.values())
				acceptedThisStudy += genes.size();
			totalAcceptedGenes += acceptedThisStudy;
			JLabel label = countLabels.get(labelIndex++);
			label.setText("  " + study.getName() + ": " + acceptedThisStudy + 
					      " genes in " + roleToGenes.size() + " clusters");
		}
		countLabels.firstElement().setText("  " + totalAcceptedGenes + " accepted genes");
	}
	
	
	// Change in min # of genes per thumbnail.
	public void itemStateChanged(ItemEvent e)
	{
		updateCountLabels();
	}

	private static int getGraphWidth()		{ return RESTRICTOR_W_PIX - MARGIN.getLeft() - MARGIN.getRight(); }
	private static int getGraphRight()		{ return RESTRICTOR_W_PIX - MARGIN.getRight(); }
	private static int getGraphBottom()		{ return RESTRICTOR_H_PIX - MARGIN.getBottom(); }
	static void sop(Object x)				{ System.out.println(x); }
	
	
	public static void main(String[] args)
	{
		RestrictionPanel that = new RestrictionPanel(null);
		OkWithContentDialog dia = new OkWithContentDialog(that, true);
		dia.setTerminateOnAnyClick();
		dia.pack();
		dia.setVisible(true);
	}
}
