package dexter.proximity;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.io.IOException;
import javax.swing.*;
import dexter.util.gui.*;
import dexter.model.*;
import dexter.view.graph.LargeGraphPanel;
import dexter.view.graph.experiment.ExperimentDestinationDialog;

import static dexter.proximity.SingleOrganismProximityPanel.*;


public class ProximityDialog extends OkWithContentDialog implements ActionListener, ItemListener
{
	private final static int						SPANE_H				= 650;
	private final static int						LEGEND_W			= 196;
	private final static int						LEGEND_LINE_GAP		=  20;
	private final static String[]					LEGEND_TEXTS		=
	{
		"Adjacent", "1 intervening gene", "2 intervening genes", ">=3 intervening genes"
	};
	
	private LargeGraphPanel							srcGraphPan;
	private Vector<SingleOrganismProximityPanel>	proximityPans;
	private JButton									selectAllBtn;
	private JButton									deselectAllBtn;
	private JButton									selToExperBtn;
	
	
	public ProximityDialog(LargeGraphPanel srcGraphPan) throws IOException
	{
		this.srcGraphPan = srcGraphPan;
		
		setTitle("Genes by proximity: distances are # of intervening genes.");
		
		setBackground(Color.WHITE);
		
		Vector<Gene> selectedGenes = srcGraphPan.getSelectedGenes();
		MultiOrganismProximityReport report = new MultiOrganismProximityReport(selectedGenes, srcGraphPan.getSession());
		init(report);
	}
	
	
	// For debugging.
	public ProximityDialog(MultiOrganismProximityReport report) throws IOException
	{
		init(report);
	}
	
	
	private void init(MultiOrganismProximityReport report) throws IOException
	{
		proximityPans = new Vector<SingleOrganismProximityPanel>();
		JPanel mainPan = new JPanel(new FlowLayout());
		for (Organism org: report.keySet())
		{
			SingleOrganismProximityPanel orgPan = 
				new SingleOrganismProximityPanel(this, srcGraphPan, org, report.get(org));
			orgPan.setBorder(BorderFactory.createLineBorder(Color.BLACK, 2));
			JScrollPane spane = 
				new JScrollPane(orgPan, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
			spane.setPreferredSize(new Dimension(spane.getPreferredSize().width, SPANE_H));
			mainPan.add(spane);
			proximityPans.add(orgPan);
		}
		mainPan.add(new ProxLegend());
		setContent(mainPan);

		selToExperBtn = new JButton("Selected to experiment");
		selToExperBtn.addActionListener(this);
		addToBottomFlowPanel(selToExperBtn, 0);
		deselectAllBtn = new JButton("Deselect all");
		deselectAllBtn.addActionListener(this);
		addToBottomFlowPanel(deselectAllBtn, 0);
		selectAllBtn = new JButton("Select all");
		selectAllBtn.addActionListener(this);
		addToBottomFlowPanel(selectAllBtn, 0);
		pack();
		setResizable(false);
	}
	
	
	private class ProxLegend extends JPanel
	{
		private boolean		contigsAreCrossed;
		private boolean		showCoregulation;
		
		ProxLegend()
		{
			setOpaque(false);
			setBorder(BorderFactory.createLineBorder(Color.BLACK, 2));
			
			for (SingleOrganismProximityPanel proxPan: proximityPans)
			{
				if (proxPan.displaysOperonRanges())
				{
					showCoregulation = true;
				}
				if (proxPan.crossesContigs())
				{
					contigsAreCrossed = true;
				}
			}
		}
		
		public Dimension getPreferredSize()
		{
			int h = LEGEND_LINE_GAP;
			for (int lineH: PROXIMITY_DIST_TO_PIX)
				h += lineH + LEGEND_LINE_GAP;
			if (contigsAreCrossed)
				h += 36;
			if (showCoregulation)
				h += PROXIMITY_DIST_TO_PIX[PROXIMITY_DIST_TO_PIX.length-1] + 3*LEGEND_LINE_GAP;
			return new Dimension(LEGEND_W, h);
		}
		
		public void paintComponent(Graphics g)
		{
			// Colored line types.
			int y = LEGEND_LINE_GAP;
			Font font = g.getFont();
			for (int i=0; i<PROXIMITY_DIST_TO_PIX.length; i++)
			{
				g.setColor(COLOR_BY_DISTANCE[i]);
				g.fillRect(15, y, STROKE_SIZES[i], PROXIMITY_DIST_TO_PIX[i]);
				g.setColor(Color.BLACK);
				int baseline = y + PROXIMITY_DIST_TO_PIX[i]/2 + 6;
				g.drawString(LEGEND_TEXTS[i], 25, baseline);
				y += PROXIMITY_DIST_TO_PIX[i] + LEGEND_LINE_GAP;
			}
			
			// Text re crossing contigs.
			if (contigsAreCrossed)
			{
				String[] texts = { "Slashed line means", "genes are on ", "different contigs" };
				for (String text: texts)
				{
					g.drawString(text, 15, y);
					y += 15;
				}
			}
			
			// Coregulation (if needed)
			if (showCoregulation)
			{
				// 4 styles of line.
				y += 9 + LEGEND_LINE_GAP;
				int yBottom = y + PROXIMITY_DIST_TO_PIX[PROXIMITY_DIST_TO_PIX.length-1];
				Graphics2D g2 = (Graphics2D)g;
				int x = 15;
				for (int topExtends=0; topExtends<2; topExtends++)
				{
					for (int bottomExtends=0; bottomExtends<2; bottomExtends++)
					{
						SingleOrganismProximityPanel.paintOperonRange(g2, 						// clobbers the font
																	  x, 
																	  y, 
																	  (topExtends==1), 
																	  yBottom, 
																	  (bottomExtends==1));
						x += 28;
					}
				}
				
				// Text. Color remains from the drawings.
				g.setFont(font);
				String[] texts = { "Predicted", "operon" };
				int baseline = (y + yBottom) / 2;
				x -= 8;
				baseline -= 6;
				for (String s: texts)
				{
					g.drawString(s, x, baseline);
					baseline += 18;
				}
			}
		}
	}  // End of inner class ProxLegend
	
	
	public void actionPerformed(ActionEvent e)
	{
		if (e.getSource() == selectAllBtn  ||  e.getSource() == deselectAllBtn)
			for (SingleOrganismProximityPanel pan: proximityPans)
				pan.selectAll(e.getSource() == selectAllBtn);
		
		else if (e.getSource() == selToExperBtn)
		{
			Vector<Gene> selGenes = new Vector<Gene>();
			for (SingleOrganismProximityPanel pan: proximityPans)
				selGenes.addAll(pan.getSelectedGenes());
			SessionModel session = srcGraphPan.getSession();
			Experiment destXper = null;
			ExperimentsStudy xpers = session.getExperimentsStudy();
			assert xpers != null;
			if (!xpers.isEmpty())
			{
				ExperimentDestinationDialog experDestDia = new ExperimentDestinationDialog(xpers);
				experDestDia.setVisible(true);
				if (experDestDia.wasCancelled())
					return;
				destXper = experDestDia.getSelectedExperiment();
			}
			if (destXper == null)
			{
				// New experiment, initialized by copying from graph associated with this dialog.
				String experName = Experiment.generateDefaultName();
				srcGraphPan.getMainFrame().addExperimentFor(selGenes, experName);  // registers the name
			}
			else
			{
				// Add genes and data to destination experiment. The experiment will automatically
				// pass the data to its graph(s).
				Map<Gene, Vector<float[]>> geneTimeMap = srcGraphPan.getGraph().getGeneToTimeAndExpressionMap();
				for (Gene gene: srcGraphPan.getSelectedGenes())
					destXper.add(gene, geneTimeMap.get(gene));
			}
		}
		
		else
			super.actionPerformed(e);
	}
	
	
	// Checkbox click in one of the organism panels.
	public void itemStateChanged(ItemEvent e)
	{
		for (SingleOrganismProximityPanel pan: proximityPans)
		{
			if (!pan.getSelectedGenes().isEmpty())
			{
				selToExperBtn.setEnabled(true);
				return;
			}
		}
		selToExperBtn.setEnabled(false);
	}
	
	
	public boolean requestSelect()
	{
		return false;
	}
	
	
	public Collection<Gene> getSelectedGenes()
	{
		return null;
	}
	

	public static void main(String[] args)
	{
		try
		{
			sop("START");
			dexter.MainDexterFrame.main(args);
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
