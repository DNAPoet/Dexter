package dexter.coreg;

import javax.swing.*;

import java.awt.*;
import java.util.*;

import dexter.model.*;
import dexter.util.gui.*;


public class CoregulationGroupInspector extends JPanel implements dexter.VisualConstants
{
	private final static int						PREF_STRIP_W		= 230;
	private final static int						PREF_STRIP_H		=  36;
	private final static Font						LABEL_FONT;
	
	
	static
	{
		LABEL_FONT = new Font((new JLabel("Perdu")).getFont().getFamily(), Font.PLAIN, 18);
	}
	
	private Map<String, Gene>						idToStudiedGene;
	private Vector<TaggedCheckBox<Gene>>			checkboxes;
	private boolean									supportsSelection;
	
	
	// The coregGroup is possibly from a file, and may include genes that weren't studied in
	// any study. The studiedGenes are specially marked, and are selectable if this object supports 
	// selection.
	public CoregulationGroupInspector(CoregulationGroup coregGroup, Map<String, Gene> idToStudiedGene)
	{
		this(coregGroup, idToStudiedGene, false);
	}
	
	
	public CoregulationGroupInspector(CoregulationGroup coregGroup, 
									  Map<String, Gene> idToStudiedGene, 
									  boolean supportsSelection)
	{
		this.supportsSelection = supportsSelection;
		this.idToStudiedGene = (idToStudiedGene != null)  ?  idToStudiedGene  :  new HashMap<String, Gene>();
		
		setLayout(new VerticalFlowLayout());
		if (supportsSelection)
			checkboxes = new Vector<TaggedCheckBox<Gene>>();
		for (String id: coregGroup)
		{
			boolean studied = this.idToStudiedGene.containsKey(id);
			Strip strip = new Strip(id, studied, supportsSelection);
			strip.setBorder(BorderFactory.createLineBorder(Color.BLACK));
			add(strip);
		}
	}
	
	
	private class Strip extends JPanel
	{
		private boolean			geneIsStudied;
		
		Strip(String id, boolean studied, boolean supportsSelection)
		{
			this.geneIsStudied = studied;
			
			setPreferredSize(new Dimension(PREF_STRIP_W, PREF_STRIP_H));
			
			// Add an empty label as a spacer.
			add(new JLabel("   "));
			
			// Always use a real label.
			JLabel label = new JLabel(id);
			label.setFont(LABEL_FONT);
			Gene gene = idToStudiedGene.get(id);
			String toolTipText = (gene != null)  ?  gene.toHTMLString()  :  id + " is not in any study for this organism.";
			label.setToolTipText(toolTipText);
			add(label);
			
			// Add a checkbox if selection is supported.
			if (supportsSelection)
			{
				TaggedCheckBox<Gene> cbox = new TaggedCheckBox<Gene>(gene, "");
				cbox.setToolTipText(toolTipText);
				add(cbox);
				checkboxes.add(cbox);
			}
		}
		
		public void paintComponent(Graphics g)
		{
			g.setColor(Color.WHITE);
			g.fillRect(0, 0, getWidth(), getHeight());
			g.setColor(geneIsStudied ? Color.GREEN : Color.RED);
			g.setFont(new Font("Serif", Font.PLAIN, 20));
			char mark = geneIsStudied ? CHECKMARK : XMARK;
			g.drawString(""+mark, 5, PREF_STRIP_H-10);
		}
	}  // End of inner class Strip


	public Vector<Gene> getSelectedGenes() 
	{
		assert supportsSelection;
		
		Vector<Gene> ret = new Vector<Gene>();
		for (TaggedCheckBox<Gene> cbox: checkboxes)
			if (cbox.isSelected())
				ret.add(cbox.getTag());
		return ret;
	}
	
	
	public void selectAll(boolean b)
	{
		assert supportsSelection;

		if (checkboxes != null)
			for (TaggedCheckBox<Gene> cbox: checkboxes)
				cbox.setSelected(b);
	}
	
	
	private static class Legend extends JPanel
	{
		public Dimension getPreferredSize()
		{
			return new Dimension(377, PREF_STRIP_H);
		}
		
		public void paintComponent(Graphics g)
		{
			g.setColor(Color.WHITE);
			g.fillRect(0, 0, getWidth(), getHeight());
			g.setFont(LABEL_FONT);
			FontMetrics fm = g.getFontMetrics();
			int x = 10;
			int baseline = PREF_STRIP_H - 10;
			String[] texts = { "" + CHECKMARK, "Gene is in study", "" + XMARK, "Gene is not in study" };
			Color[] colors = { Color.GREEN, Color.BLACK, Color.RED, Color.BLACK };
			int[] extraSpace = { 4, 14, 4, 12345 };
			for (int i=0; i<4; i++)
			{
				g.setColor(colors[i]);
				g.drawString(texts[i], x, baseline);
				x += fm.stringWidth(texts[i]);
				x += extraSpace[i];
			}
		}
	}  // End of inner class Legend
	
	
	public static JPanel getLegend()
	{
		return new Legend();
	}
	
	
	static void sop(Object x)			{ System.out.println(x); }
}
