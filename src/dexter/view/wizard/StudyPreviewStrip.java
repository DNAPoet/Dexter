package dexter.view.wizard;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;
import dexter.model.*;
import dexter.VisualConstants;


class StudyPreviewStrip extends JPanel implements ActionListener, VisualConstants
{
	private final static int	PREVIEW_HEIGHT_PIX		= 20;
	private final static int	PREVIEW_H_PIX_PRE_HOUR	=  7;
	private final static int	FONT_SIZE				= 10;
	private final static Color	CHECKMARK_COLOR			= DARK_GREEN;
	private final static int	CHECKMARK_FONT_SIZE		= 38;
	private final static Font	FONT;
	private final static Font	CHECKMARK_FONT;
	
	
	static 
	{
		Font standardLabelFont = (new JLabel("xx")).getFont();
		FONT = new Font(standardLabelFont.getFamily(), Font.PLAIN, FONT_SIZE);
		Font standardPanelFont = (new JPanel()).getFont();
		CHECKMARK_FONT = new Font(standardPanelFont.getFamily(), Font.PLAIN, CHECKMARK_FONT_SIZE);
	}
	
	private Study 				study;
	private StudyList			cohort;
	private DexterWizardPanel	wizardPan;
	private JButton				alignBtn;
	private JButton				shareBtn;
	private boolean				aligned;
	private boolean				pushedAlignBtnEnableState;
	private boolean				pushedShareBtnEnableState;
	private boolean				enableStatesArePushed;
	
	
	StudyPreviewStrip(Study study, StudyList cohort, DexterWizardPanel schedulePan)
	{
		this(study, cohort, schedulePan, -1);
	}
	
	
	// Ignores labelWidth if <= 0.
	StudyPreviewStrip(Study study, StudyList cohort, DexterWizardPanel schedulePan, int labelWidth)
	{
		assert cohort.contains(study);
		
		this.study = study;
		this.cohort = cohort;
		this.wizardPan = schedulePan;
		
		setOpaque(false);
		setLayout(new FlowLayout(FlowLayout.LEFT));
		JLabel label = new JLabel(study.getName());
		label.setFont(FONT);
		if (labelWidth > 0)
		{
			Dimension pref = label.getPreferredSize();
			label.setPreferredSize(new Dimension(labelWidth, pref.height));
		}
		add(label);
		alignBtn = new JButton("Align");
		alignBtn.addActionListener(this);
		add(alignBtn);
		shareBtn = new JButton("Share");
		shareBtn.addActionListener(this);
		shareBtn.setEnabled(false);
		add(shareBtn);
		
		setPreferredSize(new Dimension(850, getPreferredSize().height));
	}

	
	public void actionPerformed(ActionEvent e) 
	{
		if (e.getSource() == alignBtn)
		{
			wizardPan.startAlign(study);
		}
		
		else if (e.getSource() == shareBtn)
		{
			assert cohort != null;
			assert cohort.size() >= 2;
			wizardPan.shareAlignment(study, cohort);
		}
	}
	
	
	//
	// Even if some or all of this study's timepoints have been mapped, the appearance of the preview is
	// based on timepoints inferred from names. 
	//
	public void paintComponent(Graphics g)
	{
		// Invariants.
		int x = shareBtn.getLocation().x + shareBtn.getWidth() + 8;
		int y = (getHeight() - PREVIEW_HEIGHT_PIX) / 2;
		Point origin = new Point(x, y);
		Vector<String> colNames = study.getTimepointColumnNames();
		TimeAssignmentMap timeMap = new TimeAssignmentMap(colNames);
		int duration = (int)Math.ceil(Math.max(24, timeMap.getLatestTimepoint()));
		UnmappedStudyGraphPainter painter = 
			new UnmappedStudyGraphPainter(origin, duration, 
										  PREVIEW_H_PIX_PRE_HOUR, PREVIEW_HEIGHT_PIX, 
										  timeMap.values()); 
		painter.paint(g);
		
		// Checkmark to indicate this study has been aligned.
		if (aligned)
		{
			int xCheck = getWidth() - 36;
			g.setColor(CHECKMARK_COLOR);
			g.setFont(CHECKMARK_FONT);
			g.drawString("" + CHECKMARK, xCheck, 33);
		}
	}
	
	
	void setAligned(boolean b)
	{
		aligned = b;
		shareBtn.setEnabled(aligned && cohort.size() > 1);
		repaint();
	}
	
	
	Study getStudy()
	{
		return study;
	}
	
	
	// When an individual study is selected for alignment, buttons of all study preview strips become
	// disabled until alignment is finished.
	void disableButtonsAndPushEnableStates()
	{
		assert !enableStatesArePushed;
		enableStatesArePushed = true;
		
		pushedAlignBtnEnableState = alignBtn.isEnabled();
		alignBtn.setEnabled(false);
		pushedShareBtnEnableState = shareBtn.isEnabled();
		shareBtn.setEnabled(false);
	}
	
	
	void popButtonEnableStates()
	{
		assert enableStatesArePushed;
		enableStatesArePushed = false;
		
		alignBtn.setEnabled(pushedAlignBtnEnableState);
		shareBtn.setEnabled(pushedShareBtnEnableState);
	}
	
	
	void setEnableShare(boolean b)
	{
		shareBtn.setEnabled(b);
	}
	
	
	static void sop(Object x)				{ System.out.println(x); }
}
