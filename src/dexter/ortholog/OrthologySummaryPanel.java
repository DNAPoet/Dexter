package dexter.ortholog;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.*;
import dexter.VisualConstants;
import dexter.model.*;
import dexter.util.*;
import dexter.util.gui.*;
import dexter.view.wizard.DexterWizardDialog;


//
// Creates a thumbnail strip for each census distribution. There might be a lot of these (there's a
// hardcoded maximum of MAX_CENSUS_DISTNS), so this panel could be very high. Serving suggestion: in
// a scroll pane.
//


public class OrthologySummaryPanel extends JPanel implements VisualConstants
{
	private final static int								MAX_CENSUS_DISTNS		=  75;
	private final static int								MAX_GENES_PER_OG		=  20;
	private final static Dimension							STRIP_PREF_SIZE			= new Dimension(675, 132);
	private final static Vector<Color>						ORGANISM_COLORS;
	private final static Font								LEGEND_FONT				= 
																new Font("SansSerif", Font.PLAIN, 10);
	private final static int								LEGEND_PREF_W			= 310;
	private final static int 								TEXT_V_DELTA 			=  15;	
	
	static
	{
		ORGANISM_COLORS = new Vector<Color>();
		for (Color c: DFLT_GENE_COLORS)
			if (c != Color.BLACK)
				ORGANISM_COLORS.add(c);
	}
	
	
	private OrthologyGraph									everythingGraph;
	private Vector<Organism>								organisms;
	private Vector<OrthologyGroup> 							orthologs;
	private Map<OrganismCensus, Vector<OrthologyGroup>> 	censusToMembers;
	private Map<Organism, Color>							colormap;

	
	
	
	
	
	
	
				
				//////////////////////////////////////////////////////////////////
				//                                                              //
				//                         CONSTRUCTION                         //
				//                                                              //
				//////////////////////////////////////////////////////////////////

			
	
	
	public OrthologySummaryPanel(OrthologyGraph everythingGraph)
	{
		this.everythingGraph = everythingGraph;
		
		// Collect organisms.
		GeneIdToOrganismMap geneIdToOrganism = everythingGraph.getGeneIdToOrganismMap();
		organisms = new Vector<Organism>(new TreeSet<Organism>(geneIdToOrganism.values()));
					
		// Don't neglect groups with too many genes for visualization. Collect them for
		// text summary. Note that any orthology group that doesn't get vizzed probably 
		// has unique census.
		Vector<OrthologyGroup> unvizzedOrthologs = new Vector<OrthologyGroup>();
 		
		// Collect and summarize orthologs. Members of censusToMembers are collections of
		// orthology groups that have identical population distributions (e.g. 3 genes from 
		// Croco, 2 from Pro, 1 from Tricho).
		orthologs = everythingGraph.partition();
		censusToMembers = new TreeMap<OrganismCensus, Vector<OrthologyGroup>>();
		for (OrthologyGroup og: orthologs)
		{
			if (og.nOrganisms() == 1)
				continue;
			if (og.size() > MAX_GENES_PER_OG)
			{
				unvizzedOrthologs.add(og);
				continue;
			}
			OrganismCensus census = new OrganismCensus(og);
			if (!censusToMembers.containsKey(census))
				censusToMembers.put(census, new Vector<OrthologyGroup>());
			censusToMembers.get(census).add(og);
		}
		
		// Build a colormap to be shared by all panels.
		colormap = OrthologyPanel.buildColorMap(organisms);

		// For each set of orthologs with identical population distributions, build a panel.
		// Genes in the panel are maximally connected.
		setLayout(new VerticalFlowLayout());
		int nThumbnails = 0;
		for (OrganismCensus census: censusToMembers.keySet())
		{
			add(new ThumbnailStrip(census));
			if (++nThumbnails == MAX_CENSUS_DISTNS)
				break;
		}
		
		setOpaque(true);
		setBackground(Color.WHITE);
	}
	
	
	private class ThumbnailLegend extends JPanel
	{
		private OrganismCensus 		census;
		
		ThumbnailLegend(OrganismCensus census)	{ this.census = census; }
		public Dimension getPreferredSize()		{ return new Dimension(LEGEND_PREF_W, 5); }
		
		public void paintComponent(Graphics g)
		{
			int nOrganisms = census.size();
			int blockH = TEXT_V_DELTA * (nOrganisms + 1) - 3;
			int baseline = (getHeight() - blockH) / 2;
			baseline += 12;
			g.setColor(Color.BLACK);
			int nMembers = censusToMembers.get(census).size();
			assert nMembers > 0;
			String s = nMembers + " orthology group";
			if (nMembers > 1)
				s += "s";
			s += " with:";
			g.drawString(s, 4, baseline);
			baseline += TEXT_V_DELTA;
			for (Organism org: census.keySet())
			{
				g.setColor(colormap.get(org));
				g.fillOval(21, baseline-10, 8, 8);
				int count = census.getCountForBin(org);
				g.drawString(count + "x " + org, 32, baseline);
				baseline += TEXT_V_DELTA;
			}
		}
	}  // End of inner class ThumbnailLegend
	
	
	private class ThumbnailStrip extends JPanel
	{
		// If the census represents just 1 orthology group, depict that group as a thumbnail. If the
		// census represents multiple orthology groups, the thumbnail depicts a maximal interconnection
		// of the census distribution.
		ThumbnailStrip(OrganismCensus census)
		{
			setOpaque(true);
			setBackground(Color.WHITE);
			setLayout(new BorderLayout());
			OrthologyGroup firstOrthoGroup = censusToMembers.get(census).firstElement();
			Collection<GeneRelationship> edges = 
				(censusToMembers.get(census).size() == 1)  ?				// just 1 orthology group?
				collectEdges(firstOrthoGroup)  :							// yes, depict it
				GeneRelationship.maximallyConnect(firstOrthoGroup, false);	// no, maximally connect
			OrthologyPanel opan = new OrthologyPanel(edges, true);
			opan.setColormap(colormap);
			add(opan, BorderLayout.WEST);
			ThumbnailLegend legend = new ThumbnailLegend(census);
			add(legend, BorderLayout.EAST);
			setBorder(BorderFactory.createLineBorder(Color.BLACK));
			addMouseListener(new StripMlis(this, census));
			setPreferredSize(STRIP_PREF_SIZE);
		}
	}  // End of inner class ThumbnailStrip
	
	
	private class StripMlis extends MouseAdapter
	{
		private ThumbnailStrip		strip;		// for debugging
		private OrganismCensus		census;
		
		StripMlis(ThumbnailStrip strip, OrganismCensus census)		
		{
			this.strip = strip;
			this.census = census; 
		}
		
		public void mouseClicked(MouseEvent e)
		{
			BlowupPanel dpan = new BlowupPanel(census);
			JDialog dia = new OkWithContentDialog(dpan); 
			dia.setTitle("Orthology groups with gene distribution " + census.toSummaryString());
			dia.setLocation(100, 100);
			dia.setVisible(true);		// modal
		}
	}  // End of inner class StripMlis
	
		
	
	

					
					
					
					/////////////////////////////////////////////////////////////////
					//                                                             //
					//                        BLOWUP PANEL                         //
					//                                                             //
					/////////////////////////////////////////////////////////////////
				
					
	
	
	private class BlowupPanel extends JPanel implements ActionListener
	{
		private Vector<OrthologyGroup>		orthoGroups;
		private int							ogIndex;
		private JButton						prevBtn;
		private JTextField					tf;
		private JButton						nextBtn;
		private RollLegendOrthologyPanel	mainPan;
		
		
		BlowupPanel(OrganismCensus census)
		{
			orthoGroups = censusToMembers.get(census);
			
			setLayout(new BorderLayout());
			
			JPanel pan = new JPanel();
			prevBtn = new JButton("<");
			prevBtn.addActionListener(this);
			pan.add(prevBtn);
			tf = new JTextField("    1");
			tf.setEditable(false);
			pan.add(tf);
			pan.add(new JLabel("of " + orthoGroups.size()));
			nextBtn = new JButton(">");
			nextBtn.addActionListener(this);
			pan.add(nextBtn);
			enablePrevNextBtns();
			add(pan, BorderLayout.NORTH);
			
			Collection<GeneRelationship> edges = getEdgesForIndex(0);
			mainPan = new RollLegendOrthologyPanel(edges, LEGEND_PREF_W);
			mainPan.setColormap(colormap);
			mainPan.setShowOrganismLabels(true);
			add(mainPan, BorderLayout.CENTER);
		}
		
		private void enablePrevNextBtns()
		{
			prevBtn.setEnabled(ogIndex > 0);
			nextBtn.setEnabled(ogIndex < orthoGroups.size()-1);
		}
		
		private Collection<GeneRelationship> getEdgesForIndex(int n)	
		{ 
			return collectEdges(orthoGroups.get(n)); 
		}
		
		public void actionPerformed(ActionEvent e)
		{
			int delta = (e.getSource() == prevBtn)  ?  -1  :  1;
			ogIndex += delta;
			tf.setText("" + (ogIndex+1));
			enablePrevNextBtns();
			mainPan.setEdges(getEdgesForIndex(ogIndex), colormap);
		}
	}  // End of inner class DetailPanel
	
	
	
	
	
	
	
	
						
		
					/////////////////////////////////////////////////////////////////
					//                                                             //
					//                         MISC & MAIN                         //
					//                                                             //
					/////////////////////////////////////////////////////////////////
				
				
	
	
	public OrthologyGraph getGraph()
	{
		return everythingGraph;
	}
	
	
	// Input collection can be an OrthologyGroup (which isa HashSet<LightweightGene>). Returns
	// all edges afferent from all the specified genes.
	private Collection<GeneRelationship> collectEdges(Collection<LightweightGene> genes)
	{
		assert genes != null  :  "null gene list";
		
		Set<GeneRelationship> edges = new HashSet<GeneRelationship>();
		for (LightweightGene gene: genes)
		{
			if (everythingGraph.get(gene) != null)
				edges.addAll(everythingGraph.get(gene));
		}
		return edges;
	}
	
	
	String getDialogTitle()
	{
		int nOrthoGroups = 0;
		for (Vector<OrthologyGroup> vec: censusToMembers.values())
			nOrthoGroups += vec.size();
		return nOrthoGroups + " orthologous groups of genes";
	}
	
	
	public Vector<OrthologyGroup> getOrthologyGroups()
	{
		return orthologs;
	}
	
	
	public int getNOrthologyGroups()
	{
		return orthologs.size();
	}
	
	
	// Could be different from getNOrthologyGroups(), e.g. if there are more orthology groups than
	// can be displayed. CAUTION: assumes simple containment: this container contains strips.
	public int getNThumbnailStrips()
	{
		int n = 0;
		for (Component kid: getComponents())
			if (kid instanceof ThumbnailStrip)
				n++;
		return n;
	}
	
	
	static void sop(Object x)	{ System.out.println(x); }
	
	
	public static void main(String[] args)
	{				
		try
		{
			DexterWizardDialog.main(args);
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
