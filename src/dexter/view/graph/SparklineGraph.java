package dexter.view.graph;

import java.awt.*;
import java.util.*;
import javax.swing.*;
import dexter.model.*;
import dexter.view.graph.*;
import dexter.util.gui.MarginModel;


//
// Displays a single gene graph, with no background. Overrides the superclass' method for getting the
// color for a gene.
//


public class SparklineGraph extends Graph
{
	private final static int			H_PIX_PER_HR		=  3;
	private final static int			GRAPH_HEIGHT_PIX	= 48;
	private final static MarginModel	MARGIN				= new MarginModel();		// zero margins on all sides
	
	private Color						color;											// overrides everything else
	
	
	public SparklineGraph(Gene gene, Graph sourceGraph)
	{	
		// This superclass ctor gets us pretty close. The gene-to-tx map needs to be changed. Some
		// maps, like gene-to-color
		super(sourceGraph, null, H_PIX_PER_HR, GRAPH_HEIGHT_PIX, MARGIN);
		
		// Build a new gene-to-tx map that only contains our gene.
		geneToTimeAndExpression = new HashMap<Gene, Vector<float[]>>();
		geneToTimeAndExpression.put(gene, sourceGraph.getTimeAndExpressionPairs(gene));
		
		backgroundPainter = null;
		
		setOpaque(false);
		setPreferredSize(new Dimension(100, 55));
	}
	
	
	public SparklineGraph(Gene gene, Vector<float[]> txs, Color color, GraphBackgroundModel backgroundModel)
	{
		super(null,										// session
			  backgroundModel,
			  new SingleEntryGeneToTxMap(gene, txs),	
			  H_PIX_PER_HR,
			  GRAPH_HEIGHT_PIX,
			  MARGIN,
			  ColorScheme.Gene,							// color scheme
			  null,										// gene to color
			  null);									// gene to color by gene
			  
		this.color = color;
		
		backgroundPainter = null;
		
		setOpaque(false);
		setPreferredSize(new Dimension(100, 55));
	}
	
	
	private static class SingleEntryGeneToTxMap extends HashMap<Gene, Vector<float[]>>
	{
		SingleEntryGeneToTxMap(Gene gene, Vector<float[]> tx)
		{
			put(gene, tx);
		}
	}
	
	
	protected Color getColorForGene(Gene gene)
	{
		if (color != null)
			return color;
		else
			return super.getColorForGene(gene);
	}
	
	
	public void setColor(Color color)
	{
		this.color = color;
		repaint();
	}
	
	
	public Color getColor()
	{
		return color;
	}
}



