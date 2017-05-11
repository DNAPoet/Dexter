package dexter.view.graph.experiment;

import java.awt.*;

import javax.swing.*;

import java.io.*;
import java.util.*;

import dexter.model.*;
import dexter.util.gui.OkWithContentDialog;
import dexter.coreg.*;


public class GenesByCoregulationPanel extends AbstractGeneSelectionPanel
{
	private CoregulationGroupInspector			inspector;	


	// Creates a panel that presents all genes that are coregulated with the source genes.
	GenesByCoregulationPanel(CoregulationGroup coregGroup, Map<String, Gene> idToStudiedGene)
	{
		super(AddBy.Operon);
		
		setLayout(new BorderLayout());

		JPanel legend = CoregulationGroupInspector.getLegend();
		legend.setBorder(BorderFactory.createLineBorder(Color.BLACK));
		add(legend, BorderLayout.NORTH);
		
		inspector = new CoregulationGroupInspector(coregGroup, idToStudiedGene, true);
		JScrollPane spane = new JScrollPane(inspector,
											JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
											JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		spane.setPreferredSize(new Dimension(spane.getPreferredSize().width+14, 450));
		add(spane, BorderLayout.SOUTH);
	}


	Vector<Gene> getSelectedGenes() 
	{
		return inspector.getSelectedGenes();
	}


	
	boolean supportsSelectAll() 
	{
		return true;
	}
	
	
	protected void doSelectAll()
	{
		inspector.selectAll(true);
	}


	boolean supportsDeselectAll() 
	{
		return true;
	}
	
	
	protected void doDeselectAll()
	{
		inspector.selectAll(false);
	}
	
	
	static void sop(Object x)			{ System.out.println(x); }
	
	
	public static void main(String[] args)
	{
		try
		{
			String[] ids = { "Tery_0125", "Tery_0127", "Tery_0128", "Tery_0129" };
			CoregulationGroup coregGroup = new CoregulationGroup();
			for (String id: ids)
				coregGroup.add(id);
			GenesByCoregulationPanel pan = new GenesByCoregulationPanel(coregGroup, null);
			OkWithContentDialog dia = pan.embedInDialog();
			dia.pack();
			dia.setTerminateOnAnyClick();
			dia.setVisible(true);
		}
		catch (Exception x)
		{
			sop("STRESS: " + x.getMessage());
			x.printStackTrace(System.out);
			System.exit(1);
		}
	}
}
