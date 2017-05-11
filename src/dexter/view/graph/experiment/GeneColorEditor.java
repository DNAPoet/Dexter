package dexter.view.graph.experiment;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.colorchooser.*;

import java.io.*;
import java.util.*;

import dexter.model.*;
import dexter.util.gui.*;
import dexter.view.graph.GraphBackgroundModel;
import dexter.view.graph.SparklineGraph;
import static dexter.VisualConstants.*;


class GeneColorEditor extends JPanel implements ItemListener, ActionListener, ChangeListener
{
 
	private JColorChooser					colorChooser;
	private ColorMap						originalColorMap;
	private ColorMap						currentColorMap;
	private Collection<SingleGeneStrip>		geneStrips;
	private JButton							applyColorBtn;
	private GraphBackgroundModel			backgroundModel;
	

	//
	// Gene order is provided by the vector. There is no contract regarding order in the maps.
	//
	// The "apply color" button is created but not installed. The intention is that it will go in
	// the control panel of an OkWithContentDialog.
	//
	GeneColorEditor(Vector<Gene> orderedGenes, 
				    Map<Gene, Vector<float[]>> geneToTxs, 
				    ColorMap originalColorMap,
				    GraphBackgroundModel backgroundModel)
	{
		this.originalColorMap = new ColorMap(originalColorMap);
		this.backgroundModel = backgroundModel;
	
		setLayout(new BorderLayout());
		
		colorChooser = new JColorChooser();
		colorChooser.setColor(Color.RED);
		colorChooser.getSelectionModel().addChangeListener(this);
		add(colorChooser, BorderLayout.NORTH);		
		
		geneStrips = new Vector<SingleGeneStrip>();
		JPanel stripPan = new JPanel(new VerticalFlowLayout());
		for (Gene gene: geneToTxs.keySet())
		{
			Color color = originalColorMap.get(gene);
			Vector<float[]> txs = geneToTxs.get(gene);
			SingleGeneStrip strip = new SingleGeneStrip(color, gene, txs, this);
			geneStrips.add(strip);
			stripPan.add(strip);
		}
		add(stripPan, BorderLayout.CENTER);
		
		applyColorBtn = new JButton("Apply color to selected genes");
		applyColorBtn.addActionListener(this);
	}
	
	
	public JButton getApplyColorButton()
	{
		return applyColorBtn;
	}
	
	
	private class SingleGeneStrip extends JPanel
	{
		private SparklineGraph				sparkline;			// encapsulates color
		private TaggedCheckBox<Gene> 		cbox;
		
		SingleGeneStrip(Color color, Gene gene, Vector<float[]> txs, ItemListener itemListener)
		{
			setLayout(new FlowLayout(FlowLayout.LEFT));
			sparkline = new SparklineGraph(gene, txs, color, backgroundModel);
			add(sparkline);
			cbox = new TaggedCheckBox<Gene>(gene, gene.getBestAvailableName());
			cbox.setSelected(true);
			cbox.addItemListener(itemListener);
			add(cbox);
		}
		
		boolean isSelected()
		{
			return cbox.isSelected();
		}
		
		Color getColor()
		{
			return sparkline.getColor();
		}
		
		void setColor(Color color)
		{
			sparkline.setColor(color);
		}
		
		Gene getGene()
		{
			return cbox.getTag();
		}
	}  // End of inner class SingleGeneStrip
	
	
	public void actionPerformed(ActionEvent e)
	{
		for (SingleGeneStrip strip: geneStrips)
			if (strip.isSelected())
				strip.setColor(colorChooser.getColor());
	}
	
	
	public void itemStateChanged(ItemEvent e)
	{
		// "Choose color" button is enabled if any checkbox is selected.
		for (SingleGeneStrip strip: geneStrips)
		{
			if (strip.isSelected())
			{
				applyColorBtn.setEnabled(true);
				return;
			}
		}
		applyColorBtn.setEnabled(false);
	}
	
	
	public ColorMap getColorMap()
	{
		ColorMap ret = new ColorMap();
		for (SingleGeneStrip strip: geneStrips)
			ret.put(strip.getGene(), strip.getColor());
		return ret;
	}
	
	
	public void stateChanged(ChangeEvent e)
	{
		sop(colorChooser.getColor());
	}
	
	
	public OkWithContentDialog embedInDialog()
	{
		OkWithContentDialog dia = new OkWithContentDialog(this, true);
		dia.addToBottomFlowPanel(getApplyColorButton(), 0);
		return dia;
	}
	
	
	static void sop(Object x)						{ System.out.println(x); }
	
	
	private final static float[][] 					TEST_TXS =
	{
	    {  1.0f, 15.22f },
	    {  3.0f, 15.02f },
	    {  8.0f, 14.2f  },
	    { 13.0f, 13.05f },
	    { 15.0f, 12.58f },
	    { 20.0f, 13.77f },
	    { 25.0f, 14.42f },
	    { 27.0f, 14.59f }
	};
	
	
	public static void main(String[] args)
	{
		try
		{
			sop("START");
			
			int nGenes = 5;
			File serf = new File("data/ImportedStudies/Shi_Croco__imported.ser");
			Study study = Study.deserialize(serf);
			Vector<float[]> farrVec = new Vector<float[]>();
			for (float[] txs: TEST_TXS)
				farrVec.add(txs);
			ColorMap cmap = new ColorMap();
			Vector<Gene> genes = new Vector<Gene>();
			Map<Gene, Vector<float[]>> geneToTxs = new HashMap<Gene, Vector<float[]>>();
			for (int i=0; i<nGenes; i++)
			{
				Gene gene = study.get(i);
				genes.add(gene);
				cmap.put(gene, DFLT_GENE_COLORS[i%DFLT_GENE_COLORS.length]);
				geneToTxs.put(gene, farrVec);
			}
			GraphBackgroundModel bmod = new GraphBackgroundModel(28);
			GeneColorEditor ed = new GeneColorEditor(genes, geneToTxs, cmap, bmod);
			OkWithContentDialog dia = ed.embedInDialog();
			dia.setVisible(true);
		}
		catch (Exception x)
		{
			sop("STRESS: " + x.getMessage());
			x.printStackTrace(System.out);
			System.exit(1);
		}
		finally
		{
			sop("DONE");
		}
	}
}
