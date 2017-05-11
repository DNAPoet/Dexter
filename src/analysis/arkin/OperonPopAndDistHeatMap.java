package analysis.arkin;

import java.io.IOException;
import java.util.*;
import java.awt.*;
import javax.swing.*;
import dexter.model.*;
import dexter.util.gui.*;
import analysis.util.*;


//
// Distance is horizontal, size is vertical.
//
// For these hardcoded scales, max # of operons hitting any pixel is 9 => need 9 colors.
//


public class OperonPopAndDistHeatMap extends HeatMap2D
{
	private final static float					MAX_MEAN_DIST			= 60f;		// actually 55.59
	private final static int 					MAX_GENES_PER_OPERON	= 35;		// actually 30
	private final static Dimension				CELL_SIZE				= new Dimension(8, 10);
	private final static int					N_DIST_CELLS			= 100;
		
	
	OperonPopAndDistHeatMap(Study study) throws IOException
	{
		Vector<Operon> operons = new OperonChecker(study).getOperons();
		short[][] countMap = new short[N_DIST_CELLS][MAX_GENES_PER_OPERON];
		for (Operon op: operons)
		{
			int i = distToCellIndex(op.getMeanInternalPairwiseDist());
			int j = op.size();
			countMap[i][j]++;
		}
		setCounts(countMap);
		
		setCellSize(CELL_SIZE);
		float distPerHorizCell = MAX_MEAN_DIST / N_DIST_CELLS;
		int hCellsPerMajorTick = 10;
		setGridsPerHMajorTick(hCellsPerMajorTick);
		float deltaDistPerMajorTick = hCellsPerMajorTick * distPerHorizCell;
		setHorizontalMajorTickLabelStepProgram(0, deltaDistPerMajorTick);
		setHorizontalAxisText("Mean internal distance");
		setVerticalAxisText("# genes in operon");
		setGridsPerVMajorTick(5);
		setVerticalMajorTickLabelStepProgram(0, 5);
		setTitle(study.getOrganism().getName());
		setLegendUpperLeft(new Point(500, 100));
	}
	
	
	private int distToCellIndex(float dist)
	{
		return Math.round(dist * N_DIST_CELLS / MAX_MEAN_DIST);
	}
	
	
	static void sop(Object x)	{ System.out.println(x); }
	
	
	public static void main(String[] args)
	{
		try
		{
			sop("START");
			SessionModel session = OrganismDistanceMatrix.getSession();
			for (Study study: session.getStudies())
			{
				sop(study.getName());
				OperonPopAndDistHeatMap graph = new OperonPopAndDistHeatMap(study);
				JDialog dia = new JDialog();
				dia.setTitle(study.getName());
				dia.add(graph, BorderLayout.CENTER);
				dia.pack();
				dia.setVisible(true);
			}
		}
		catch (Exception x)
		{
			sop("Stress: " + x.getMessage());
			x.printStackTrace(System.out);
		}
		finally
		{
			sop("DONE");
		}
	}
}
