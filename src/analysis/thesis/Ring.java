package analysis.thesis;

import java.io.*;
import java.util.*;
import java.awt.*;
import java.awt.geom.*;

import javax.swing.*;

import dexter.model.*;
import dexter.util.gui.*;
import dexter.util.*;
import au.com.bytecode.opencsv.CSVReader;


public class Ring extends JPanel 
{
	private final static int				PREF_W_H			= 400;
	private final static int				RING_INRADIUS		= 165;
	private final static int				RING_OUTRADIUS		= 185;
	private final static Color				RING_COLOR			= Color.LIGHT_GRAY;
	private final static Color[]			HEAT_COLORS			= 
	{
		Color.BLUE, Color.CYAN, Color.GREEN, Color.YELLOW, Color.ORANGE, Color.RED
	};
	private final static Font				FONT				= new Font("Serif", Font.ITALIC, 28);
	
	private Organism						org;
	private Vector<Candidate>				candidates;
	private TransformStack					xformStack;
	private Map<String, int[]>				geneIdToLoci;
	private int 							maxLocus;
	
	
	private class Candidate
	{
		String 		idStartGene;
		String 		idEndGene;
		double		expect;
		
		Candidate(String csvLine)
		{
			String[] pieces = csvLine.split(",");
			idStartGene = pieces[0];
			idEndGene = pieces[5];
			expect = Double.parseDouble(pieces[6]);
		}
		
		public String toString() { return idStartGene + "-" + idEndGene + ": " + expect; }
	}
	
	
	Ring(Organism org) throws IOException
	{
		this.org = org;
		
		setPreferredSize(new Dimension(PREF_W_H, PREF_W_H));
		setBorder(BorderFactory.createLineBorder(Color.BLACK, 3));
		
		// Call with org = null for a legend.
		if (org == null)
			return;
		
		// Collect candidates.
		candidates = new Vector<Candidate>();
		String prefix = "Tery";
		if (org != Organism.TERY)
			prefix = (org == Organism.CROCO)  ?  "Cwat"  :  "PMM";
		File csv = new File("analysis_data/MyRecommendations/AllCandidates.csv");
		FileReader fr = new FileReader(csv);
		BufferedReader br = new BufferedReader(fr);
		String line = null;
		br.readLine();
		while ((line = br.readLine()) != null)
		{
			if (line.startsWith(prefix))
				candidates.add(new Candidate(line));
		}
		br.close();
		fr.close();	
		
		// Collect gene coordinates.
		geneIdToLoci = collectGeneLoci();
		for (int[] coords: geneIdToLoci.values())
		{
			maxLocus = Math.max(maxLocus, coords[0]);
			maxLocus = Math.max(maxLocus, coords[1]);
		}
		if (org == Organism.CROCO)
			maxLocus = 6237905;
		xformStack = new TransformStack();
	}
	
	
	private Map<String, int[]> collectGeneLoci() throws IOException
	{
		Map<String, int[]> ret = new HashMap<String, int[]>();
		
		if (org == Organism.PRO)
		{
			File dirf = new File("analysis_data/GenomeInfo");
			File orfsFile = new File(dirf, "Pro_MED4_Genes_From_JGI.fa");
			FileReader fr = new FileReader(orfsFile);
			BufferedReader br = new BufferedReader(fr);
			String line = null;
			while ((line = br.readLine()) != null)
			{
				if (!line.startsWith(">"))
					continue;
				String[] pieces = line.split("\\s");
				for (String piece: pieces)
				{
					if (piece.contains(".."))
					{
						// 4990..6219(-)
						String geneId = pieces[1];
						int from = Integer.parseInt(piece.substring(0, piece.indexOf(".")));
						int to = Integer.parseInt(piece.substring(1+piece.lastIndexOf("."), piece.indexOf("(")));
						ret.put(geneId, new int[] { from, to });
						continue;
					}
				}
			}
			br.close();
			fr.close();	
		}
		
		else if (org == Organism.TERY)
		{
			File orfsFile = new File("data/Studies/terySet_fData.csv");
			FileReader fr = new FileReader(orfsFile);
			BufferedReader br = new BufferedReader(fr);
			br.readLine();
			String line = null;
			while ((line = br.readLine()) != null)
			{
				String[] pieces = line.split(",");
				String geneId = stripQuotes(pieces[0]);
				String sfrom = stripQuotes(pieces[2]);
				String sto = stripQuotes(pieces[3]);
				int from = Integer.parseInt(sfrom);
				int to = Integer.parseInt(sto);
				ret.put(geneId, new int[] { from, to });
			}
			br.close();
			fr.close();	
		}
		
		else
		{
			assert org == Organism.CROCO;
			File csv = new File("analysis_data/GenomeInfo/croco_genome_protein_table2.csv");
			java.util.List<String[]> rows = StringUtils.readCsvRows(csv);
			rows.remove(0);
			for (String[] row: rows)
			{
				String geneId = row[6];
				String sfrom = row[1];
				String sto = row[2];
				int from = Integer.parseInt(sfrom);
				int to = Integer.parseInt(sto);
				ret.put(geneId, new int[] { from, to });
			}
		}
		
		return ret;
	}
	
	
	private static String stripQuotes(String s)
	{
		return StringUtils.strip(s, "\"");
	}
	
	
	public void paintComponent(Graphics g)
	{
		// Fill.
		g.setColor(Color.WHITE);
		g.fillRect(0, 0, 2222, 2222);
		
		// Legend.
		if (org == null)
		{
			paintLegend(g);
			return;
		}
		
		// Ring.
		g.setColor(RING_COLOR);
		Graphics2D g2 = (Graphics2D)g;
		xformStack.push(g2);
		translateToCenter(g);
		g2.fillOval(-RING_OUTRADIUS, -RING_OUTRADIUS, 2*RING_OUTRADIUS, 2*RING_OUTRADIUS);
		g.setColor(Color.WHITE);
		g2.fillOval(-RING_INRADIUS, -RING_INRADIUS, 2*RING_INRADIUS, 2*RING_INRADIUS);
		
		// Candidates.
		for (Candidate candi: candidates)
			paintCandidate(g2, candi);
		
		// Label.
		g.setColor(Color.BLACK);
		g.setFont(FONT);
		String s = org.getName();
		int sw = g.getFontMetrics().stringWidth(s);
		int baseline = 5;
		g.drawString(s, -sw/2, baseline);

		xformStack.pop(g2);
		assert xformStack.isEmpty();
	}
	
	
	private void paintCandidate(Graphics2D g2, Candidate candi)
	{
		int[] startCoords = geneIdToLoci.get(candi.idStartGene);
		assert startCoords[0] < startCoords[1];
		int[] endCoords = geneIdToLoci.get(candi.idEndGene);
		assert endCoords[0] < endCoords[1];
		int startCoord = min4(startCoords, endCoords);
		int endCoord = max4(startCoords, endCoords);
		assert startCoord < endCoord;
		assert endCoord <= maxLocus;
		double expect = candi.expect;
		g2.setColor(expectToColor(expect));
		paintArc(g2, startCoord, endCoord);
	}
	
	
	// Assumes origin is at center.
	private void paintArc(Graphics2D g2, int startCoord, int endCoord)
	{
		sop("start/end/max coords = " + startCoord + "/" + endCoord + "/" + maxLocus);
		double startFrac = startCoord / (double)maxLocus;
		double endFrac = endCoord / (double)maxLocus;
		sop("   start/end frac = " + startFrac + "/" + endFrac);
		int startDegs = (int)Math.round(360 * startFrac);		// 0 is at 3:00, increasing ccw
		int endDegs = (int)Math.round(360 * endFrac);
		int deltaDegs = endDegs - startDegs;
		sop("   start/end/delta degs = " + startDegs + "/" + endDegs + "/" + deltaDegs);
		if (deltaDegs == 0) 
			deltaDegs = 1;
		for (int r=RING_INRADIUS+1; r<RING_OUTRADIUS; r++)
			g2.drawArc(-r, -r, 2*r, 2*r, startDegs+90, -deltaDegs);
	}
	
	
	private Color expectToColor(double expect)
	{
		assert expect >= 0;
		
		if (expect >= .1)
			return HEAT_COLORS[0];
		else if (expect >= .05)
			return HEAT_COLORS[1];
		else if (expect >= .01)
			return HEAT_COLORS[2];
		else if (expect >= .001)
			return HEAT_COLORS[3];
		else if (expect >= .0001)
			return HEAT_COLORS[4];
		else
			return HEAT_COLORS[5];
	}
	
	
	private int min4(int[] i1i2, int[] i3i4)
	{
		return Math.min(Math.min(i1i2[0], i1i2[1]), Math.min(i3i4[0], i3i4[1]));
	}
	
	
	private int max4(int[] i1i2, int[] i3i4)
	{
		return Math.max(Math.max(i1i2[0], i1i2[1]), Math.max(i3i4[0], i3i4[1]));
	}
	
	
	private void translateToCenter(Graphics g)
	{
		g.translate(PREF_W_H/2, PREF_W_H/2);
	}
	
	
	private final static String[] LEGEND_STRINGS =
	{
		"E-value >= .1",
		".1 < E-value >= .05",
		".05 < E-value >= .01",
		".01 < E-value >= .001",
		".001 < E-value >= .0001",
		"E-value < .0001"
	};
	
	
	private final static Font LEGEND_FONT = new Font("SansSerif", Font.PLAIN, 18);
	private final static int TOP_BASELINE = 65;
	private final static int DELTA_BASELINE = 60;
	private final static int COLOR_BAR_TO_BASELINE = -1;
	private final static int COLOR_BAR_X = 38;
	private final static int COLOR_BAR_W = 110;
	private final static int COLOR_BAR_H = 18;
	
	private void paintLegend(Graphics g)
	{
		int baseline = TOP_BASELINE;
		g.setFont(LEGEND_FONT);
		assert HEAT_COLORS.length == LEGEND_STRINGS.length;
		for (int i=0; i<HEAT_COLORS.length; i++)
		{
			int barTop = baseline - COLOR_BAR_TO_BASELINE - COLOR_BAR_H;
			g.setColor(HEAT_COLORS[i]);
			g.fillRect(COLOR_BAR_X, barTop, COLOR_BAR_W, COLOR_BAR_H);
			g.setColor(Color.BLACK);
			g.drawRect(COLOR_BAR_X, barTop, COLOR_BAR_W, COLOR_BAR_H);
			g.drawString(LEGEND_STRINGS[i], COLOR_BAR_X + COLOR_BAR_W + 12, baseline);
			baseline += DELTA_BASELINE;
		}
	}
	
	
	static void sop(Object x)				{ System.out.println(x); }
	
	
	public static void main(String[] args)
	{
		try
		{
			sop("START");
			JFrame frame = new JFrame();
			frame.setLayout(new GridLayout(2, 2));
			frame.add(new Ring(Organism.CROCO));
			frame.add(new Ring(Organism.PRO));
			frame.add(new Ring(Organism.TERY));
			frame.add(new Ring(null));
			frame.pack();
			frame.setVisible(true);
		}
		catch (Exception x)
		{
			sop("STRESS: " + x.getMessage());
			x.printStackTrace(System.out);
			System.exit(1);
		}
		sop("Done");
	}
}
