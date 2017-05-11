package dexter.view.graph.experiment;

import java.awt.*;
import java.awt.color.CMMException;
import java.awt.event.*;
import javax.swing.*;
import java.util.*;
import java.io.*;
import dexter.VisualConstants;
import dexter.event.LegendEvent;
import dexter.event.LegendListener;
import dexter.model.*;
import dexter.ortholog.*;
import dexter.util.gui.*;
import dexter.view.graph.Graph;



class GenesByOrthologyPanel extends AbstractGeneSelectionPanel implements VisualConstants
{
	private final static int		GRAPH_STRIP_MARGIN 		=  28;
	private final static int		GRAPH_STRIP_RADIUS 		= 125;
	private final static int		GRAPH_STRIP_DOT_RADIUS 	=   5;
		
	private Collection<Gene> 		starterGenes;
	private	SessionModel			session;
	private VariableBorderCheckboxManager<LightweightGene> 
									cboxManager;

	
	
	// Debug only. Builds a simple color map.
	private GenesByOrthologyPanel(Collection<Gene> starterGenes, SessionModel session) throws IOException
	{
		this(starterGenes, session, null);
	}
	
	
	GenesByOrthologyPanel(Collection<Gene> starterGenes, SessionModel session, Map<Organism, Color> colorMap) 
		throws IOException
	{
		super(AddBy.Orthology);
		
		this.session = session;
		
		setLayout(new BorderLayout());
		
		// Build an orthology graph and extract its groups. Note OrthologyGroup aggregates
		// LightweightGene instances, which arenta Gene.
		OrthologyFileCollection orthoFiles = session.getOrthologyFiles();
		assert orthoFiles != null  :  "null orthoFiles";
		OrthologyGraph comprehensiveOrthologyGraph = 
			new OrthologyGraph(session, orthoFiles.getListFiles(), orthoFiles.getTabularBLASTFiles());
		Vector<OrthologyGroup> allOrthologyGroups = comprehensiveOrthologyGraph.partition();

		// Retain orthology groups that contain any of the starter genes.
		Set<OrthologyGroup> orthoGroups = new HashSet<OrthologyGroup>();
		outer: for (OrthologyGroup ogroup: allOrthologyGroups)
		{
			for (Gene starterGene: starterGenes)
			{
				if (ogroup.contains(starterGene.getOrganism(), starterGene.getId()))
				{
					orthoGroups.add(ogroup);
					continue outer;
				}
			}
		}
		allOrthologyGroups = null;		// won't be needing this any more.
		
		// For debugging, build a simple color map.
		if (colorMap == null)
		{
			sop("DEBUGGING: Building simple color map.");
			Set<Organism> organisms = new TreeSet<Organism>();
			for (OrthologyGroup ogroup: orthoGroups)
				for (LightweightGene gene: ogroup)
					organisms.add(gene.getOrganism());

			colorMap = new HashMap<Organism, Color>();
			int n = 0;
			for (Organism org: organisms)
				colorMap.put(org, DFLT_GENE_COLORS[n++ % DFLT_GENE_COLORS.length]);	
		}
		
		// Convert orthology groups to collections of (lightweight) gene relationships.
		Vector<HashSet<GeneRelationship>> edgeSets = new Vector<HashSet<GeneRelationship>>();
		for (OrthologyGroup ogroup: orthoGroups)
		{
			HashSet<GeneRelationship> edges = new HashSet<GeneRelationship>();
			for (LightweightGene gene: ogroup)
			{
				if (comprehensiveOrthologyGraph.get(gene) != null)
					edges.addAll(comprehensiveOrthologyGraph.get(gene));
			}
			edgeSets.add(edges);
		}
		
		// Convert each relationship collection to an orthology panel.
		JPanel orthoPanHolder = new JPanel(new GridLayout(0, 1));
		cboxManager = new VariableBorderCheckboxManager<LightweightGene>();
		for (HashSet<GeneRelationship> edges: edgeSets)
		{
			OrthologyPanelWithCheckboxLegend orthoPan = 
					new OrthologyPanelWithCheckboxLegend(edges, 
														 GRAPH_STRIP_MARGIN, 
														 GRAPH_STRIP_RADIUS, 
														 GRAPH_STRIP_DOT_RADIUS, 
														 cboxManager, 
														 colorMap, 
														 starterGenes);
			orthoPanHolder.add(orthoPan);
		}
		add(orthoPanHolder, BorderLayout.CENTER);
	}
	
	
	Vector<Gene> getSelectedGenes()
	{
		Collection<LightweightGene> lwGenes = cboxManager.getSelectedTags();
		Vector<Gene> ret = new Vector<Gene>(lwGenes.size());
		
		for (LightweightGene lwg: lwGenes)
		{
			Gene gene = session.lightweightGeneToGene(lwg);
			assert gene != null  :  "no gene for lwg " + lwg;
			ret.add(gene);
		}
		
		return ret;
	}
	
	
	public int getNOrthologyGroups()
	{
		return cboxManager.size();
	}
	
	
	protected boolean shouldBeContainedInAScrollPane()
	{
		return true;
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
		cboxManager.selectAll(true);
	}
	
	
	public static void main(String[] args)
	{				
		try
		{
			sop("Starting");
			SessionModel session = SessionModel.fromDevSerFile();
			Vector<Gene> starterGenes = new Vector<Gene>();
			for (Study study: session.getStudies())
			{
				if (!study.getName().toUpperCase().contains("ZINSER"))
					continue;
				for (Gene gene: study)
				{
					if (gene.getBestAvailableName().equals("PMM1352")  ||  
						gene.getBestAvailableName().equals("PMM0580"))
					{
						starterGenes.add(gene);
					}
				}
			}
			GenesByOrthologyPanel that = new GenesByOrthologyPanel(starterGenes, session, null);
			JFrame frame = new JFrame();
			frame.add(that, BorderLayout.CENTER);
			frame.pack();
			frame.setVisible(true);
		}
		catch (Exception x)
		{
			sop("Stress: " + x.getMessage());
			x.printStackTrace();
		}
		finally
		{
			sop("DONE");
		}
	}
}

