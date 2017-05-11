package dexter.view.wizard;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.*;
import dexter.util.*;
import dexter.util.gui.LayoutAdapter;


//
// Not generic because stages might not be defined by an enum. Deals only with stage index.
//



public class WizardStagesPanel extends JPanel implements ActionListener
{
	private final static int			PREF_W						= 850;
	private final static int			FONT_SIZE					=  17;
	private final static Font			FONT 						= new Font("Serif", Font.PLAIN, FONT_SIZE);
	private final static int			TOP_BASELINE				=   8 + FONT_SIZE;
	private final static int			BASELINE_TO_BASELINE		=   6 + FONT_SIZE;
	private final static int			BOTTOM_BASELINE_TO_BOTTOM	=   8;
	private final static int			BTN_TO_BTN_V_GAP			=  10;
	private final static Color			BG							= new Color(220, 220, 220);
	private final static Color			SELECTED_BG					= new Color(220, 255, 255);
	private final static int			STAGE_BORDER_STROKE_WIDTH	=   3;
	private final static Stroke			STAGE_BORDER_STROKE			= new BasicStroke(STAGE_BORDER_STROKE_WIDTH);
	
	private Vector<String[]>			linesByStage;
	private Vector<BackNextButton[]>	backAndNextBtns;
	private int							selectedIndex;
	private Set<WizardStageListener>	listeners;
	private WizardTransitionApprover	approver;
	
	
	public WizardStagesPanel(Vector<String[]> linesByStage)
	{
		this.linesByStage = linesByStage;
		
		// Install a BACK and a NEXT button between adjacent stages.
		setLayout(new Lom());
		backAndNextBtns = new Vector<BackNextButton[]>();
		int nStages = linesByStage.size();
		for (int i=0; i<nStages-1; i++)
		{
			BackNextButton[] btns = new BackNextButton[2];
			btns[0] = new BackNextButton(1);
			btns[1] = new BackNextButton(-1);
			for (int j=0; j<2; j++)
			{
				btns[j].addActionListener(this);
				add(btns[j]);
			}
			backAndNextBtns.add(btns);
		}
		
		setIndex(0);
		listeners = new HashSet<WizardStageListener>();
		addMouseListener(new Mlis());
	}
	
	
	private void setIndex(int newIndex)
	{
		selectedIndex = newIndex;
		enableButtonsForSelectedIndex();
	}
	
	
	private class BackNextButton extends JButton
	{
		private int			delta;
		
		BackNextButton(int delta)
		{
			super((delta==-1) ? "<<" : ">>");
			this.delta = delta;
			Dimension pref = getPreferredSize();
			setPreferredSize(new Dimension(pref.width/2, pref.height));
		}
		
		int getDelta()		{ return delta; }
	}
	
	
	private class Lom extends LayoutAdapter
	{
		public Dimension preferredLayoutSize(Container parent)
		{
			int maxLineCount = -1;
			for (String[] sarr: linesByStage)
				maxLineCount = Math.max(maxLineCount, sarr.length);
			int prefH = TOP_BASELINE + (maxLineCount-1)*BASELINE_TO_BASELINE + BOTTOM_BASELINE_TO_BOTTOM;
			prefH = Math.max(prefH, 75);
			return new Dimension(PREF_W, prefH);			// assume width is constrained
		}

		
		public void layoutContainer(Container parent)
		{
			Vector<Rectangle> stageBoundses = getStageBoundses();
			for (int i=1; i<stageBoundses.size(); i++)
			{
				Rectangle rightBounds = stageBoundses.get(i);
				BackNextButton[] btns = backAndNextBtns.get(i-1);
				for (BackNextButton btn: btns)
					btn.setSize(btn.getPreferredSize());
				int blockH = btns[0].getHeight() + BTN_TO_BTN_V_GAP + btns[1].getHeight();
				int y = (getHeight() - blockH) / 2;
				for (BackNextButton btn: btns)
				{
					int x = rightBounds.x - btn.getWidth()/2;
					btn.setLocation(x, y);
					y += btn.getHeight() + BTN_TO_BTN_V_GAP;
				}
			}
		}
	}
	
	
	public void actionPerformed(ActionEvent e)
	{
		BackNextButton src = (BackNextButton)e.getSource();	
		int delta = src.getDelta();
		shift(delta);
	}
	
	
	private void shift(int delta)
	{
		assert delta == 1  ||  delta == -1;
		
		int oldIndex = selectedIndex;
		int newIndex = selectedIndex + delta;
		
		if (approver != null)
		{
			String err = approver.isTransitionApproved(oldIndex, newIndex);
			if (err != null)
			{
				JOptionPane.showMessageDialog(this, err);
				repaint();
				return;
			}
		}
		
		selectedIndex = newIndex;
		enableButtonsForSelectedIndex();
		repaint();
		WizardStageEvent we = new WizardStageEvent(oldIndex, selectedIndex);
		fireWizardEvent(we);
	}
	
	
	private void enableButtonsForSelectedIndex()
	{
		for (int i=0; i<backAndNextBtns.size(); i++)
		{
			BackNextButton[] btns = backAndNextBtns.get(i);
			btns[0].setEnabled(i == selectedIndex);
			btns[1].setEnabled(i == selectedIndex-1);
		}
	}
	
	
	public void paintComponent(Graphics g)
	{
		if (getWidth() <= 0)
			return;

		Graphics2D g2 = (Graphics2D)g;
		g2.setColor(BG);
		g2.fillRect(0, 0, 3333, 2222);
		g2.setFont(FONT);
		
		Vector<Rectangle> stageBoundses = getStageBoundses();
		for (int i=0; i<stageBoundses.size(); i++)
		{
			Rectangle stageBounds = stageBoundses.get(i);
			if (i == selectedIndex)
			{
				g2.setColor(SELECTED_BG);
				g2.fill(stageBounds);
			}			
			g2.setColor(Color.BLACK);
			g2.setStroke(STAGE_BORDER_STROKE);
			stageBounds.height -= STAGE_BORDER_STROKE_WIDTH;
			g2.draw(stageBounds);
			stageBounds.height += STAGE_BORDER_STROKE_WIDTH;
			g2.setFont(FONT);
			int baseline = TOP_BASELINE;
			String[] lines = linesByStage.get(i);
			for (String line: lines)
			{
				int sw = g.getFontMetrics().stringWidth(line);
				int x = stageBounds.x + (stageBounds.width - sw) / 2;
				g2.drawString(line, x, baseline);
				baseline += BASELINE_TO_BASELINE;
			}
		}
	}
	
	
	private Vector<Rectangle> getStageBoundses()
	{
		Vector<Rectangle> ret = new Vector<Rectangle>();
		Rectangle outerBounds = getBounds();
		int nStages = linesByStage.size();
		int pieceW = outerBounds.width / nStages;
		int remainingW = outerBounds.width;
		int nextX = 0;
		for (int i=0; i<nStages-1; i++)
		{
			ret.add(new Rectangle(nextX, 0, pieceW, outerBounds.height));
			nextX += pieceW;
			remainingW -= pieceW;
		}
		ret.add(new Rectangle(nextX, 0, remainingW, outerBounds.height));
		return ret;
	}
	
	
	public void addWizardStageListener(WizardStageListener l)
	{
		listeners.add(l);
	}
	
	
	public void setTransitionApprover(WizardTransitionApprover approver)
	{
		this.approver = approver;
	}
	
	
	private void fireWizardEvent(WizardStageEvent e)
	{
		for (WizardStageListener l: listeners)
			l.wizardStageChanged(e);
	}
	
	
	private class Mlis extends MouseAdapter
	{
		public void mouseClicked(MouseEvent e)
		{
			int targetIndex = -1;
			Vector<Rectangle> stageBoundses = getStageBoundses();
			for (int i=0; i<stageBoundses.size(); i++)
			{
				Rectangle r = stageBoundses.get(i);
				if (r.contains(e.getX(), e.getY()))
				{
					targetIndex = i;
					break;
				}
			}
			
			if (targetIndex == -1)
				return;
			int direction = (int)Math.signum(targetIndex - selectedIndex);
			while (selectedIndex != targetIndex)
				shift(direction);		
		}
	}
	
	
	static void sop(Object x)				{ System.out.println(x); }
	
	
	public static void main(String[] args)
	{
		Vector<String[]> vec = new Vector<String[]>();
		String[] srcs = { "ABC\nDEF", "Line 1\nLong line 2", "And more" };
		for (String src: srcs)
			vec.add(StringUtils.splitOnLineBreaks(src));
		WizardStagesPanel pan = new WizardStagesPanel(vec);
		JFrame frame = new JFrame();
		frame.add(pan);
		frame.pack();
		frame.setVisible(true);
	}
}
