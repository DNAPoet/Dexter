package dexter.view.graph;

import java.util.*;
import java.awt.*;
import javax.swing.*;
import dexter.VisualConstants;
import dexter.model.*;
import dexter.event.*;
import dexter.util.gui.*;
import dexter.util.gui.VerticalFlowLayout;


//
// Maintains a collection of legend listeners, even though it doesn't send legend events. The listeners are
// registered with new rollable check boxes as they are added.
//


public class GenesLegend extends JPanel implements VisualConstants
{	
	private final static int 						MIN_PREF_W		= 150;
	
	private LargeGraph								graph;
	private LayoutManager							lom;
	private Vector<EditableRollableCheckBox<Gene>>	rcboxes;
	private Set<LegendListener<Gene>>				legendListeners;
	
	
	//
	// Preferred size is based on widest text, which requires a FontMetrics object from an installed
	// component. The Legend object being constructed can't do it because it isn't yet installed, but
	// presumably construction is triggered by a user event e.g. a request to expand a thumbnail, so the 
	// source graph works. The source graph also provides gene colors. 
	//
	public GenesLegend(Collection<Gene> genes,
			           Map<Gene, String> geneToVisibleName, 
			           LargeGraph graph)
	{
		this.graph = graph;
		
		legendListeners = new HashSet<LegendListener<Gene>>();
		
		if (geneToVisibleName == null)
			geneToVisibleName = new HashMap<Gene, String>();
		setOpaque(false);
		lom = new VerticalFlowLayout();
		setLayout(lom);
		FontMetrics fm = graph.getFontMetrics(RollableCheckBox.getTextFont());
		assert fm != null;
		rcboxes = new Vector<EditableRollableCheckBox<Gene>>();
		for (Gene gene: genes)
		{
			String visibleName = geneToVisibleName.containsKey(gene)  ?
				geneToVisibleName.get(gene)  :
				gene.getName();
			addCheckBoxForGene(gene, visibleName);
		}
		
		setBorder(BorderFactory.createLineBorder(Color.BLACK, 2));
	}
	
	
	public Dimension getPreferredSize()
	{
		Dimension lomPref = lom.preferredLayoutSize(this);
		return new Dimension(Math.max(lomPref.width, MIN_PREF_W), lomPref.height);
	}

	
	public GenesLegend(Collection<Gene> genes, LargeGraph graph)
	{
		this(genes, null, graph);
	}

	
	// Call after gene has been added to graph, to make sure color is available.
	public void addCheckBoxForGene(Gene gene)
	{
		addCheckBoxForGene(gene, gene.getBestAvailableName(), true);
	}
	
	
	// Call after gene has been added to graph, to make sure color is available.
	public void addCheckBoxForGene(Gene gene, boolean isSelected)
	{
		addCheckBoxForGene(gene, gene.getBestAvailableName(), true);
	}
	
	
	// Call after gene has been added to graph, to make sure color is available.
	public void addCheckBoxForGene(Gene gene, String title)
	{
		addCheckBoxForGene(gene, title, true);
	}
	
	
	// Call after gene has been added to graph, to make sure color is available.
	public void addCheckBoxForGene(Gene gene, String title, boolean isSelected)
	{	
		assert graph != null;
		
		Color color = graph.getColorForGene(gene);
		assert color != null;
		if (color == null)
			color = Color.black;		// shouldn't happen
		
		if (title == null)
			title = gene.getBestAvailableName();
		
		EditableRollableCheckBox<Gene> rcbox = new EditableRollableCheckBox<Gene>(gene, title, color, isSelected);
		for (LegendListener<Gene> l: legendListeners)
			rcbox.addLegendListener(l);
		setToolTopForGeneCheckBox(rcbox);
		rcboxes.add(rcbox);
		add(rcbox);
		
		if (getParent() != null)
		{
			getParent().invalidate();
			getParent().validate();
		}
	}
	
	
	public void removeCheckBoxForGene(Gene gene)
	{
		RollableCheckBox<Gene> cbox = getCheckBoxForGene(gene);
		rcboxes.remove(cbox);
		remove(cbox);
		getParent().validate();
	}
	
	
	public void removeCheckBoxesForGenes(Collection<Gene> genes)
	{
		for (Gene gene: genes)
		{
			RollableCheckBox<Gene> cbox = getCheckBoxForGene(gene);
			assert cbox != null : "Null checkbox for gene " + gene.getBestAvailableName();
			rcboxes.remove(cbox);
			remove(cbox);	
		}
		getParent().validate();
	}
	
	
	private void setToolTopForGeneCheckBox(EditableRollableCheckBox<Gene> cbox)
	{
		Gene gene = cbox.getTag();
		cbox.setToolTipText(gene.toHTMLString());
	}

	
	private RollableCheckBox<Gene> getCheckBoxForGene(Gene gene)
	{
		 for (RollableCheckBox<Gene> cbox: rcboxes)
			 if (cbox.getTag().equals(gene))
				 return cbox;
		 return null;
	}
	
	
	Vector<Gene> getGenes()
	{
		Vector<Gene> ret = new Vector<Gene>();
		for (RollableCheckBox<Gene> box: rcboxes)
			ret.add(box.getTag());
		return ret;
	}
	
	
	Vector<Gene> getSelectedGenes()
	{
		Vector<Gene> ret = new Vector<Gene>();
		for (RollableCheckBox<Gene> box: rcboxes)
			if (box.isSelected())
				ret.add(box.getTag());
		return ret;
	}
	
	
	Gene getArmedGene()
	{
		for (RollableCheckBox<Gene> box: rcboxes)
			if (box.isArmed())
				return box.getTag();
		return null;
	}
	
	
	public void addLegendListener(LegendListener<Gene> listener)
	{
		legendListeners.add(listener);
		
		for (RollableCheckBox<Gene> box: rcboxes)
			box.addLegendListener(listener);
	}
	
	
	public void removeLegendListener(LegendListener<Gene> listener)
	{
		legendListeners.remove(listener);

		for (RollableCheckBox<Gene> box: rcboxes)
			box.removeLegendListener(listener);
	}
	
	
	// Doesn't post events.
	void selectAll(boolean sel)
	{
		for (RollableCheckBox<Gene> box: rcboxes)
			box.setSelected(sel);					
	}
	
	
	// Doesn't post events.
	void invertSelections()
	{
		for (RollableCheckBox<Gene> box: rcboxes)
			box.setSelected(!box.isSelected());
	}
	
	
	public void selectGenes(Collection<Gene> selGenes)
	{
		for (RollableCheckBox<Gene> box: rcboxes)
			box.setSelected(selGenes.contains(box.getTag()));
	}
	
	
	public boolean containsGene(Gene gene)
	{
		for (RollableCheckBox<Gene> cbox: rcboxes)
			if (cbox.getTag() == gene)
				return true;
		return false;
	}
	
	
	// Applies gene colors from the graph.
	public void recolor()
	{
		for (RollableCheckBox<Gene> box: rcboxes)
			box.setLineColor(graph.getColorForGene(box.getTag()));
	}
	
	
	static void sop(Object x)				{ System.out.println(x); }
	
	
	public static void main(String[] args)
	{
		dexter.MainDexterFrame.main(args);
	}
}
