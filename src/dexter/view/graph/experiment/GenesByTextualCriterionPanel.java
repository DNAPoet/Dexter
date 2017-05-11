package dexter.view.graph.experiment;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.*;
import java.io.*;
import dexter.model.*;
import dexter.util.gui.*;


class GenesByTextualCriterionPanel extends AbstractGeneSelectionPanel implements ActionListener
{
	private final static Font				BIG_FONT			= new Font("Serif", Font.ITALIC, 36);
	
	private SessionModel					session;
	private AddBy							addBy;
	private JTextField						tf;
	private JButton							searchBtn;
	private Vector<TaggedCheckBox<Gene>>	cboxes;
	private JScrollPane						spane;
	
	
	GenesByTextualCriterionPanel(AddBy addBy, SessionModel session)
	{
		super(AddBy.Gene_Name);
		
		this.session = session;
		this.addBy = addBy;
		
		setLayout(new BorderLayout());
		
		JPanel north = new JPanel();
		north.setOpaque(true);
		north.add(new JLabel("Add genes by " + addBy + ":"));
		tf = new JTextField(8);
		tf.addActionListener(this);
		tf.setToolTipText("Wildcards: *, +, ?");
		north.add(tf);
		searchBtn = new JButton("Search");
		searchBtn.addActionListener(this);
		north.add(searchBtn);
		add(north, BorderLayout.NORTH);
		spane = new JScrollPane(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		spane.setPreferredSize(new Dimension(350, 350));
		add(spane, BorderLayout.CENTER);
	}
	
	
	public void actionPerformed(ActionEvent e)
	{
		if (e.getSource() == tf  ||  e.getSource() == searchBtn)
			search();
		
		else
			super.actionPerformed(e);
	}
	
	
	private void search()
	{
		// Get regular expression.
		String regex = tf.getText().trim().toUpperCase();
		if (regex.isEmpty())
		{
			JOptionPane.showMessageDialog(this, "Specify a search term in the textfield.");
			return;
		}
		
		// Convert regex to java-compatible, e.g. "nif*" to "nif.*".
		StringBuilder sb = new StringBuilder();
		for (int i=0; i<regex.length(); i++)
		{
			char ch = regex.charAt(i);
			if (ch == '*'  ||  ch == '+'  ||  ch == '?')
				sb.append('.');
			sb.append(ch);
		}
		regex = sb.toString();
		
		// Collect genes that match the regex.
		Vector<Gene> matchingGenes = new Vector<Gene>();
		for (Study study: session.getStudies())
		{
			if (study.isExperimentsStudy())
				continue;
			for (Gene gene: study)
			{
				if (gene.getName() != null  &&  gene.getName().toUpperCase().matches(regex))
					matchingGenes.add(gene);
				else if (gene.getId().toUpperCase().matches(regex))
					matchingGenes.add(gene);
			}
		}
		
		// Build checkboxes for matching genes.
		cboxes = new Vector<TaggedCheckBox<Gene>>();
		JPanel cboxHolder = new JPanel(new GridLayout(0, 1));
		cboxHolder.setBackground(Color.WHITE);
		if (!matchingGenes.isEmpty())
		{
			for (Gene gene: matchingGenes)
			{
				JPanel strip = new JPanel(new FlowLayout(FlowLayout.LEFT));
				strip.setOpaque(false);
				String text = gene.getStudy().getName() + "." + getCriterionSvalForGene(gene);
				TaggedCheckBox<Gene> cbox = new TaggedCheckBox<Gene>(gene, text);
				cboxes.add(cbox);
				JPanel spacer = new JPanel();
				spacer.setOpaque(false);
				spacer.setPreferredSize(new Dimension(120, cbox.getPreferredSize().height));
				strip.add(spacer);
				strip.add(cbox);
				cboxHolder.add(strip);
			}
		}
		else
		{
			JLabel label = new JLabel("No matching genes.", SwingConstants.CENTER);
			label.setFont(BIG_FONT);
			label.setForeground(Color.RED);
			cboxHolder.add(label);
		}
		
		spane.setViewportView(cboxHolder);
	}	
	
	
	private String getCriterionSvalForGene(Gene gene)
	{
		switch (addBy)
		{
			case Gene_Name:
				return gene.getBestAvailableName();
				
			case Pathway:
				return gene.getPathway();
				
			default: 
				assert false;
				return null;
		}
	}

	
	Vector<Gene> getSelectedGenes()
	{
		Vector<Gene> ret = new Vector<Gene>();
		if (cboxes == null)
			return ret;
		for (TaggedCheckBox<Gene> cbox: cboxes)
			if (cbox.isSelected())
				ret.add(cbox.getTag());	
		return ret;
	}
	
	
	boolean supportsSelectAll()
	{
		return true;
	}
	
	
	boolean supportsDeselectAll()
	{
		return false;
	}
	
	
	protected void doSelectAll()
	{
		for (TaggedCheckBox<Gene> cbox: cboxes)
			cbox.setSelected(true);
	}
	
	
	protected void doDeselectAll()
	{
		for (TaggedCheckBox<Gene> cbox: cboxes)
			cbox.setSelected(false);
	}
	
	
	public static void main(String[] args)
	{
		try
		{
			sop("STARTING");
			
			File sessionSerf = new File("data/Sessions/CPT.dex");
			SessionModel session = SessionModel.deserialize(sessionSerf);
			GenesByTextualCriterionPanel that = new GenesByTextualCriterionPanel(AddBy.Gene_Name, session);
			JFrame frame = new JFrame();
			frame.add(that);
			frame.pack();
			frame.setVisible(true);
		}
		catch (Exception x)
		{
			sop("STRESS: " + x.getMessage());
			x.printStackTrace(System.out);
		}
		finally
		{
			sop("DONE");
		}
	}
}
