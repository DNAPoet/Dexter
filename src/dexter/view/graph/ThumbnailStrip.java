package dexter.view.graph;

import java.util.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.IOException;

import javax.print.attribute.HashAttributeSet;
import javax.swing.*;

import dexter.MainDexterFrame;
import dexter.VisualConstants;
import dexter.event.ThumbnailListener;
import dexter.model.*;
import dexter.util.gui.*;
import dexter.view.graph.experiment.ExperimentThumbnailStrip;
import dexter.cluster.Metric;
import dexter.coreg.CoregulationGroup;


public class ThumbnailStrip extends JPanel implements ItemListener, VisualConstants
{
	private final static int			TITLE_FONT_SIZE			=  18;
	private final static Font			TITLE_FONT;
	private final static int			SPANE_PREF_H			= 620;
	
	static
	
	{
		JLabel label = new JLabel("Perdu: you should never see this");
		Font font = label.getFont();
		TITLE_FONT = new Font(font.getFamily(), Font.PLAIN, TITLE_FONT_SIZE);
	}
	
	
	protected SessionModel					session;
	protected Study							study;
	protected MainDexterFrame				mainFrame;
	protected GroupGenesBy	 				groupBy; 
	protected OrderGeneGroupsBy 			orderBy;
	protected Metric 						metric;
	protected String 						title;
	protected JCheckBox						selBox;
	protected Vector<ThumbnailGraph>		thumbnails;
	protected JScrollPane					spane;
	protected JPanel						thumbnailHolderPan;	// lives in spane	
	protected Set<ThumbnailListener>		thumbnailListeners;

	
	public ThumbnailStrip(SessionModel sessionModel, 
						  Study study, 
						  Map<Gene, Vector<float[]>> geneToTimeAndExpression,
						  MainDexterFrame mainFrame)
	{
		this(sessionModel, study, GroupGenesBy.buildKEGG(), OrderGeneGroupsBy.NAME, geneToTimeAndExpression, mainFrame);
	}
	
	
	public ThumbnailStrip(SessionModel session, 
						  Study study, 
						  GroupGenesBy groupBy, 
						  OrderGeneGroupsBy orderBy,
						  Map<Gene, Vector<float[]>> geneToTimeAndExpression,
						  MainDexterFrame mainFrame)
	{
		// Meez.
		assert session != null  :  "null session model in ThumbnailStrip ctor.";
		assert study != null  :  "null study in ThumbnailStrip ctor.";
		this.session = session;
		this.study = study;
		this.mainFrame = mainFrame;
		this.groupBy = groupBy;
		this.orderBy = orderBy;
		TimeAssignmentMap timeAssignments = session.getTimeAssignmentMapForStudy(study);
		assert timeAssignments != null  ||  study.isExperimentsStudy()  :  
			"No time assignments map for " + study.getName();
		GraphBackgroundModel backgroundModel = session.getGraphBackgroundModel();
		
		// Collect genes. Each map entry will be a single thumbnail graph in the strip. Keys in the
		// map are role values, e.g. "Nitrogen Metabolism".
		Metric metric = mainFrame.getMetric();
		TimeAndExpressionProvider txProvider = mainFrame.getTXProvider();
		RoleValueToGenesMap roleToGenes = (groupBy.isSpreadsheetColumnRole())  ?
			new RoleValueToGenesMap(study, groupBy, orderBy, null, metric, txProvider)  :
			new AppearanceOrderRoleValueToGenesMap(study, orderBy, null, metric, txProvider);
		
		// Build individual thumbnail graphs.
		Vector<ThumbnailGraph> thumbnails = buildThumbnails(roleToGenes);
		
		// Build/populate/add scroll pane.
		init(study.getName(), backgroundModel, thumbnails);
	}
	
	
	// Builds thumbnail graphs but doesn't install them into any GUI parents.
	protected Vector<ThumbnailGraph> buildThumbnails(RoleValueToGenesMap roleToGenes)
	{
		Vector<ThumbnailGraph> thumbnails = new Vector<ThumbnailGraph>();
		
		GraphBackgroundModel backgroundModel = session.getGraphBackgroundModel();
		
		for (String name: roleToGenes.keySet())
		{		
			Map<Gene, Vector<float[]>> geneToTXForThumbnail = new TreeMap<Gene, Vector<float[]>>();
			for (Gene gene: roleToGenes.get(name))
			{
				TimeAssignmentMap timeAssignments = session.getTimeAssignmentMapForStudy(gene.getStudy());
				Vector<float[]> timepointsForGene = gene.getTimeAndExpressionPairs(timeAssignments);
				geneToTXForThumbnail.put(gene, timepointsForGene);
			}	
			ThumbnailGraph thumbnail = new ThumbnailGraph(name, session, backgroundModel, geneToTXForThumbnail);
			thumbnail.setStudy(study);
			thumbnail.setMouseArmsAndSelects(true);
			thumbnail.setStrip(this);
			thumbnails.add(thumbnail);
		}
		
		return thumbnails;
	}
	
	
	// Just for subclasses.
	protected ThumbnailStrip()		{ } 
	
	
	// Call this version if this.thumbnails already is populated.
	protected void init(String title, GraphBackgroundModel backgroundModel)
	{
		init(title, backgroundModel, null);
	}
	
	
	// Initializes from the inputThumbnails collection, unless inputThumbnails is null, in which case
	// assumes this.thumbnails is already populated.
	protected void init(String title, 
					    GraphBackgroundModel backgroundModel,
					    Collection<ThumbnailGraph> inputThumbnails)
	{
		if (inputThumbnails == null)
			assert this.thumbnails != null  &&  !this.thumbnails.isEmpty();
		
		this.title = title;
		if (inputThumbnails != null)
			this.thumbnails = new Vector<ThumbnailGraph>(inputThumbnails);
		this.thumbnailListeners = new HashSet<ThumbnailListener>();
		
		setLayout(new BorderLayout());		
		JPanel north = new JPanel(new GridLayout(2, 1));
		north.add(buildTitleLabel(title));
		JPanel controlPan = new JPanel();
		populateControlPanel(controlPan);
		north.add(controlPan);
		add(north, BorderLayout.NORTH);
		
		// Collect thumbnails into a panel.
		thumbnailHolderPan = new JPanel(new VerticalFlowLayout());
		for (ThumbnailGraph thumbnail: thumbnails)
		{
			thumbnailHolderPan.add(thumbnail);
			thumbnail.setStrip(this);
		}

		// Install panel in a scrollpane.
		spane = new JScrollPane(thumbnailHolderPan, 
				JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
				JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		Dimension thumbnailPref = ThumbnailGraph.getPreferredSizeForBackground(backgroundModel);
		spane.setPreferredSize(new Dimension(thumbnailPref.width+28, SPANE_PREF_H));
		add(spane, BorderLayout.CENTER);
		
		setBorderColorForSelectionState(false);
	}
	
	
	protected JLabel buildTitleLabel(String title)
	{
		JLabel label = new JLabel(title, SwingConstants.CENTER);
		label.setFont(TITLE_FONT);
		return label;
	}
	
	
	protected void addSelectBtnToControlPanel(JPanel controlPan, boolean abbreviateSelect)
	{
		String s = abbreviateSelect ? "Sel" : "Select";
		selBox = new JCheckBox(s);
		selBox.addItemListener(this);
		controlPan.add(selBox);
	}
	

	// Subclasses can override.
	protected void populateControlPanel(JPanel controlPan)
	{
		addSelectBtnToControlPanel(controlPan, false);		// Don't abbreviate "Select"
	}
	
	
	
	private void setBorderColorForSelectionState(boolean isSelected)
	{
		Color borderColor = isSelected  ?  SELECTION_COLOR  :  Color.BLACK;
		setBorder(BorderFactory.createLineBorder(borderColor, 2));
	}
	
	
	void addThumbnailListener(ThumbnailListener listener)
	{
		thumbnailListeners.add(listener);
		for (ThumbnailGraph thumbnail: thumbnails)
			thumbnail.addThumbnailListener(listener);
	}
	
	
	void removeThumbnailListener(ThumbnailListener listener)
	{
		thumbnailListeners.remove(listener);
		for (ThumbnailGraph thumbnail: thumbnails)
			thumbnail.removeThumbnailListener(listener);
	}
	
	
	public void setThumbnailToolTipText(String s)
	{
		for (ThumbnailGraph thumbnail: thumbnails)
			thumbnail.setToolTipText(s);
	}
	

	protected Collection<ThumbnailListener> getThumbnailListeners()
	{
		return thumbnailListeners;
	}
	
	
	public void itemStateChanged(ItemEvent e)
	{
		if (e.getSource() == selBox)
			setBorderColorForSelectionState(selBox.isSelected());
	}
	
	
	void deselectAll()
	{
		// Thumbnails.
		for (ThumbnailGraph thumbnail: thumbnails)
			thumbnail.setArmState(ArmState.NONE);		// repaints
		
		// This strip.
		if (selBox != null)
			selBox.setSelected(false); 						// sends an item event
	}
	
	
	protected MultiThumbnailStripPanel getMultiStripPanel()
	{
		Component compo = this;
		while (compo != null  &&  !(compo instanceof MultiThumbnailStripPanel))
			compo = compo.getParent();
		return (MultiThumbnailStripPanel)compo;
	}
	
	
	public Vector<ThumbnailGraph> getSelectedThumbnails()
	{
		Vector<ThumbnailGraph> ret = new Vector<ThumbnailGraph>();
		for (ThumbnailGraph thumbnail: thumbnails)
			if (thumbnail.isSelected())
				ret.add(thumbnail);
		return ret;
	}
	
	
	public void addThumbnailGraph(ThumbnailGraph addMe)
	{
		getThumbnails().add(addMe);
		getThumbnailHolder().add(addMe);
		getScrollPane().validate();
	}
	
	
	public void removeThumbnailGraph(ThumbnailGraph removeMe)
	{
		thumbnails.remove(removeMe);
		thumbnailHolderPan.remove(removeMe);
		validate();
	}
	
	
	public void removeAllThumbnailGraphs()
	{
		for (ThumbnailGraph thumbnail: new HashSet<ThumbnailGraph>(thumbnails))
			removeThumbnailGraph(thumbnail);
	}
	
	
	// Returns new # of thumbnails.
	int regroupAndRestrict(GroupGenesBy groupBy, 
						   OrderGeneGroupsBy orderBy, 
						   Metric metric, 
						   ExpressionRestrictionModel restrictions)
	{
		this.groupBy = groupBy;
		this.orderBy = orderBy;
		this.metric = metric;
		return regroupAndRestrict(restrictions);
	}
	
	
	// Call with null restrictions to unrestrict. Returns new # of thumbnails.
	int regroupAndRestrict(ExpressionRestrictionModel restrictions)
	{		
		// Collect clusters of genes. Each cluster models one thumbnail graph. For studies (this class),
		// grouping is according to groupBy. For experiments and cluster trees, grouping is invariant.
		// So subclasses should override inherited version of mapRoleValuesToGenes().
		RoleValueToGenesMap roleToGenes = (restrictions != null)  ?  
										  mapRoleValuesToGenes(restrictions)  :		// restrict
										  getUnrestrictedRoleValueToGenes();		// unrestrict
		
		// Remove old thumbnails from the holder panel, which is contained in the scrollpane.
		removeAllThumbnailGraphs();
		assert thumbnails.isEmpty();
		
		// Build new thumbnails. Can't collect into thumbnails directly due to concurrent modification problems.
		Vector<ThumbnailGraph> newThumbnails = buildThumbnails(roleToGenes);
		for (ThumbnailGraph thumbnail: newThumbnails)
			for (ThumbnailListener listener: thumbnailListeners)
				thumbnail.addThumbnailListener(listener);
		
		// Add thumbnails to the holder panel.
		for (ThumbnailGraph thumbnail: newThumbnails)
			addThumbnailGraph(thumbnail);
		
		return newThumbnails.size();
	}
	
	
	void unrestrict()
	{
		regroupAndRestrict(null);
	}
	
	
	private RoleValueToGenesMap getUnrestrictedRoleValueToGenes()
	{
		return mapRoleValuesToGenes(null);
	}
	
	
	// Call with null restrictions to unrestrict. Not appropriate for subclasses.
	protected RoleValueToGenesMap mapRoleValuesToGenes(ExpressionRestrictionModel restrictions)
	{
		TimeAndExpressionProvider txProvider = mainFrame.getTXProvider();

		if (groupBy.isSpreadsheetColumnRole())
			return new RoleValueToGenesMap(study, groupBy, orderBy, restrictions, metric, txProvider);
		
		else if (groupBy == GroupGenesBy.getCoregulationInstance())
		{
			try
			{
				Vector<CoregulationGroup> coregGroups = session.getCoregulationFiles().getCoregulationGroups(study);
				return new CoregulationRoleValueToGenesMap(study, coregGroups, orderBy, restrictions, metric, txProvider);
			}
			catch (IOException x)
			{
				String err = "Couldn't read coregulation file " + x.getMessage();
				err += ", can't group " + study.getName() + " by coregulation";
				JOptionPane.showMessageDialog(this, err);
				return new RoleValueToGenesMap();			// empty map
			}
		}
		
		else
			return new AppearanceOrderRoleValueToGenesMap(study, orderBy, restrictions, metric, txProvider);			
	}
	
	
	public Set<Gene> collectGenesBySelectionLevel(GeneSelectionLevel level)
	{
		Set<Gene> ret = new HashSet<Gene>();
		
		for (ThumbnailGraph thumbnail: thumbnails)
		{
			if (level == GeneSelectionLevel.Selected_thumbnails  &&  !thumbnail.isSelected())
				continue;
			ret.addAll(thumbnail.getGenes());
		}
		
		return ret;
	}
	
	
	public Map<Gene, Vector<float[]>> collectGenesToTxsBySelectionLevel(GeneSelectionLevel level)
	{
		Map<Gene, Vector<float[]>> ret = new HashMap<Gene, Vector<float[]>>();
		
		if (level == GeneSelectionLevel.Selected_datasets  &&  !isSelected())
			return ret;
		
		for (ThumbnailGraph thumbnail: thumbnails)
		{
			if (level == GeneSelectionLevel.Selected_thumbnails  &&  !thumbnail.isSelected())
				continue;
			Map<Gene, Vector<float[]>> submap = thumbnail.getGeneToTimeAndExpressionMap();
			for (Gene gene: submap.keySet())
				ret.put(gene, submap.get(gene));
		}
		
		return ret;
	}
	
	
	public Vector<ThumbnailGraph> collectSelectedThumbnails()
	{
		Vector<ThumbnailGraph> ret = new Vector<ThumbnailGraph>();
		for (ThumbnailGraph thumbnail: thumbnails)
			if (thumbnail.isSelected())
				ret.add(thumbnail);
		return ret;
	}
	
	
	// Buttons default to fairly tall, which makes this control panels with buttons taller than the control panels for 
	// study strips. With shorter buttons, all control panels are the same height. This method also reduces button
	// width.
	protected void shrinkButtonForContolPanel(JButton btn)
	{
		Dimension oldPref = btn.getPreferredSize();
		Dimension newPref = new Dimension(oldPref.width-30, oldPref.height-10);
		btn.setPreferredSize(newPref);
	}
	
	
	protected boolean hasSelectedThumbnails()
	{
		for (ThumbnailGraph thumbnail: thumbnails)
			if (thumbnail.isSelected())
				return true;
		return false;
	}
	
	
	public int countGenes()
	{
		int n = 0;
		for (ThumbnailGraph thumbnail: thumbnails)
			n += thumbnail.getGenes().size();
		return n;
	}
	
	
	protected JPanel getThumbnailHolder()				{ return thumbnailHolderPan;					   }
	protected JScrollPane getScrollPane()				{ return spane;									   }
	protected MainDexterFrame getMainFrame()			{ return mainFrame;								   }
	protected static void sop(Object x)					{ System.out.println(x); 			 			   }
	
	
	public boolean isExperimentsStudy()					{ return this instanceof ExperimentThumbnailStrip; }
	public boolean isExperimentsStrip()					{ return isExperimentsStudy(); 					   }
	public boolean isClusterStrip()						{ return this instanceof ClusterThumbnailStrip;    }
	public boolean isSelected()							{ return selBox.isSelected();					   }
	public String getTitle()							{ return title;									   }
	public Vector<ThumbnailGraph> getThumbnails()		{ return thumbnails;    						   }
	public Study getStudy()								{ return study;									   }


	public static void main(String[] args)
	{
		try
		{
			dexter.MainDexterFrame.main(args);
		}
		catch (Exception x)
		{
			sop("Stress: " + x.getMessage());
			x.printStackTrace();
		}
	}
}
