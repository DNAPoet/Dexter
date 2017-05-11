package dexter.view.graph.experiment;

import javax.swing.*;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import dexter.model.*;
import dexter.util.gui.OkWithContentDialog;


abstract class AbstractGeneSelectionPanel extends JPanel implements ActionListener
{	
	private final static Font			LEGEND_FONT						= new Font("Serif", Font.PLAIN, 19);

	private OkWithContentDialog			dialog;
	private AddBy						addBy;
	private Map<Study, Color>			studyToColor;
	private Set<Study>					representedStudies;
	private JButton						selectAllBtn;
	private JButton						deselectAllBtn;

	
	
	AbstractGeneSelectionPanel(AddBy addBy)
	{
		this.addBy = addBy;
		representedStudies = new TreeSet<Study>();
	}
	
	
	AddBy getAddBy()
	{
		return addBy;
	}
	
	
	protected void setDialog(OkWithContentDialog dialog)
	{
		this.dialog = dialog;
	}
	
	
	protected OkWithContentDialog getDialog()
	{
		return dialog;
	}
	
	
	protected void setStudyToColorMap(Map<Study, Color>	studyToColor)
	{
		this.studyToColor = studyToColor;
	}
	
	
	protected Color getColorForStudy(Study study)
	{
		return (studyToColor != null)  ?  studyToColor.get(study)  :  Color.BLACK;
	}
	
	
	protected void addRepresentedStudy(Study study)
	{
		assert study != null  :  "null study";
		representedStudies.add(study);
	}
	
	
	protected void paintLegend(Graphics g, int x, int y)
	{
		if (studyToColor == null)
			return;
	
		g.setFont(LEGEND_FONT);
		for (Study representedStudy: representedStudies)
		{
			g.setColor(studyToColor.get(representedStudy));
			g.drawLine(x, y, x+35, y);
			g.drawLine(x, y+1, x+35, y+1);
			g.drawString(representedStudy.getName(), x+38, y+6);
			y += 24;
		}
	}
	
	
	protected boolean shouldBeContainedInAScrollPane()
	{
		return false;
	}
	
	
	abstract Vector<Gene> getSelectedGenes();
	
	
	abstract boolean supportsSelectAll();
	
	
	abstract boolean supportsDeselectAll();
	
	
	// For subclasses that support SELECT ALL. 
	public void setSelectAllButton(JButton selectAllBtn)
	{
		this.selectAllBtn = selectAllBtn;
		selectAllBtn.addActionListener(this);
	}
	
	
	// For subclasses that support DESELECT ALL. 
	public void setDeselectAllButton(JButton deselectAllBtn)
	{
		this.deselectAllBtn = deselectAllBtn;
		deselectAllBtn.addActionListener(this);
	}
	
	
	public void actionPerformed(ActionEvent e)
	{
		if (e.getSource() == selectAllBtn)
			doSelectAll();
		
		else if (e.getSource() == deselectAllBtn)
			doDeselectAll();
	}
	
	
	OkWithContentDialog embedInDialog()
	{
		OkWithContentDialog dia = null;
		
		if (shouldBeContainedInAScrollPane())
		{
			JScrollPane spane = new JScrollPane(this, 
												JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, 
												JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
			dia = new OkWithContentDialog(spane, true);
		}
		else
			dia = new OkWithContentDialog(this, true);
		
		dia.setOkButtonText("Apply");
		setDialog(dia);		
		dia.setTitle("Add genes by " + addBy);
		dia.setModal(true);
		
		if (this.supportsDeselectAll())
		{
			JButton deselAllBtn = new JButton("Deselect all");
			this.setDeselectAllButton(deselAllBtn); 			// adds pan as btn's action listener
			dia.addToBottomFlowPanel(deselAllBtn, 0);
		}
		
		if (this.supportsSelectAll())
		{
			JButton selAllBtn = new JButton("Select all");
			this.setSelectAllButton(selAllBtn); 			// adds pan as btn's action listener
			dia.addToBottomFlowPanel(selAllBtn, 0);
		}
		
		dia.pack();
		
		return dia;
	}
	

	protected void doSelectAll()	{ }
	protected void doDeselectAll()	{ }
	static void sop(Object x)		{ System.out.println(x); }
	
	
	public static void main(String[] args)
	{
		dexter.MainDexterFrame.main(args);
	}
}
