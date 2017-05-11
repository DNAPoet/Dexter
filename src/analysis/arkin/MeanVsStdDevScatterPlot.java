package analysis.arkin;

import java.awt.*;
import java.io.*;
import javax.swing.JFrame;
import java.util.*;

import org.jfree.data.statistics.HistogramDataset;
import org.jfree.data.xy.*;
import org.jfree.chart.*;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.*;
import org.jfree.chart.renderer.xy.*;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.*;

import dexter.VisualConstants;



public class MeanVsStdDevScatterPlot 
{
	private final static String[] PCT_INDEX_LINES =
	{
		"<LI><A HREF=\"gnc59919.html\">Prochlorococcus marinus sp. MED4</A></LI>",
		"<LI><A HREF=\"gnc165597.html\">Crocosphaera watsonii WH 8501</A></LI>",
		"<LI><A HREF=\"gnc203124.html\">Trichodesmium erythraeum IMS101</A></LI>"
	};

	MeanVsStdDevScatterPlot(String title, String[] indexLines) throws IOException
	{
		// Create dataset.
		XYSeriesCollection seriesCollection = new XYSeriesCollection();
	    XYSeries series = new XYSeries(title);
	    for (String indexLine: indexLines)
	    {
	    	File ifile = ArkinlabOperonLengthHisto.indexLineToFile(indexLine);
	    	ArkinlabOperonLengthHisto histo = new ArkinlabOperonLengthHisto(ifile);
	    	float[] meanAndSdev = histo.meanAndSdevBinPopulation();
	    	series.add(meanAndSdev[0], meanAndSdev[1]);
	    }
		seriesCollection.addSeries(series);
		
		// Create chart.        
		JFreeChart chart = ChartFactory.createScatterPlot(title, "Mean operon length", "Std dev of operon length",
			seriesCollection, 
			PlotOrientation.VERTICAL,
			true, 			// include legend
			true, 			// tooltips
			false); 		// urls
		
		// Frame for chart.
        ChartFrame frame = new ChartFrame(title, chart);
        frame.pack();
        frame.setVisible(true);
	}
	
	
	static XYSeries indexLinesToXYSeries(String[] indexLines, String title) throws IOException
	{
	    XYSeries series = new XYSeries(title);
	    for (String indexLine: indexLines)
	    {
	    	File ifile = ArkinlabOperonLengthHisto.indexLineToFile(indexLine);
	    	ArkinlabOperonLengthHisto histo = new ArkinlabOperonLengthHisto(ifile);
	    	float[] meanAndSdev = histo.meanAndSdevBinPopulation();
	    	series.add(meanAndSdev[0], meanAndSdev[1]);
	    }	
	    return series;
	}
	
	
	static XYSeries indexLineToXYSeries(String indexLine, String title) throws IOException
	{
	    String[] sarr = { indexLine };
	    return indexLinesToXYSeries(sarr, title);
	}
	  
	
	static void showScatterPlots() throws IOException
	{
		XYSeriesCollection seriesCollection = new XYSeriesCollection();
		seriesCollection.addSeries(indexLineToXYSeries(PCT_INDEX_LINES[0], "Pro"));
		seriesCollection.addSeries(indexLineToXYSeries(PCT_INDEX_LINES[1], "Croco"));
		seriesCollection.addSeries(indexLineToXYSeries(PCT_INDEX_LINES[2], "Tery"));
		seriesCollection.addSeries(indexLinesToXYSeries(ArkinlabSampleCyanos.CYANO_HTMLLINES, "Other Cyanos"));
		seriesCollection.addSeries(indexLinesToXYSeries(ArkinlabSampleNonCyanos.NON_CYANO_HTMLLINES, "Sample Non-cyanos"));
		JFreeChart chart = ChartFactory.createScatterPlot("Mean vs stddev of operon length", 
				"Mean operon length", 
				"Std dev of operon length",
				seriesCollection, 
				PlotOrientation.VERTICAL,
				true, 			// include legend
				true, 			// tooltips
				false); 		// urls
		DefaultXYItemRenderer renderer = new DefaultXYItemRenderer();
		Color[] colors = { Color.BLUE, 
				Color.YELLOW,
				Color.RED, 
				VisualConstants.PALE_CYAN, 
				new Color(175, 88, 88) };
		for (int i=0; i<5; i++)
		{
			renderer.setSeriesPaint(i, colors[i]);
			renderer.setSeriesLinesVisible(i, false);
			if (i <= 2)
				renderer.setSeriesOutlinePaint(i, Color.BLACK);
		}
		XYPlot plot = (XYPlot)chart.getPlot();
		plot.setRenderer(renderer);
        ChartFrame frame = new ChartFrame("Mean vs stddev of operon length", chart);	
        frame.pack();
        frame.setVisible(true);
	}
	
	
	////////////////////////////////////////////////////////////////////////////////////////////
	
	
	static class HistoRenderer extends BarRenderer
	{
		// Row = series.
		public Paint getItemPaint(final int row, final int column)
		{
			return (row == 0)  ?  Color.yellow  :  Color.black;
		}
	}


    private static double[] createHistoDataArray(String[] indexLines) throws IOException 
    {
        java.util.Vector<Double> dvec = new java.util.Vector<Double>();
        for (int i=0; i<indexLines.length; i++)
        {
	    	File ifile = ArkinlabOperonLengthHisto.indexLineToFile(indexLines[i]);
	    	ArkinlabOperonLengthHisto histo = new ArkinlabOperonLengthHisto(ifile);
	    	double nLength2s = histo.getCountForBinZeroDefault(2);
	    	int nOperons = histo.getSumOfAllCounts();
	    	if (nOperons < 25)
	    		continue;
	    	if (nLength2s == 0  ||  nOperons == 0)
	    	{
	    		sop(i + "   ?? WTFO " + nLength2s + " :: " + nOperons);
	    		continue;
	    	}
	    	double fracLength2Operons = nLength2s / nOperons;
	    	dvec.add(fracLength2Operons);
        }
        double[] darr = new double[dvec.size()];
        int n = 0;
        for (Double d: dvec)
        	darr[n++] = d;
        assert n == dvec.size();
        return darr;
    }
	
    
	static void showShortFracHisto() throws IOException
	{
		HistogramDataset dataset = new HistogramDataset();
        double[] darr = createHistoDataArray(ArkinlabSampleCyanos.CYANO_HTMLLINES);
		dataset.addSeries("Cyanos", darr, 100);
        darr = createHistoDataArray(ArkinlabSampleNonCyanos.NON_CYANO_HTMLLINES);
		dataset.addSeries("Non-cyanos", darr, 100);
        JFreeChart chart = ChartFactory.createHistogram(
        		"Fraction of predicted operons with length = 2",
                null,
                null,
                dataset,
                PlotOrientation.VERTICAL,
                true,
                true,
                false);
        XYPlot plot = (XYPlot) chart.getPlot();
        plot.setDomainPannable(true);
        plot.setRangePannable(true);
        plot.setForegroundAlpha(0.85f);
        NumberAxis yAxis = (NumberAxis) plot.getRangeAxis();
        yAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
        XYBarRenderer renderer = (XYBarRenderer) plot.getRenderer();
        renderer.setDrawBarOutline(false);
        // flat bars look best...
        renderer.setBarPainter(new StandardXYBarPainter());
        renderer.setShadowVisible(false);        
        ChartPanel panel = new ChartPanel(chart);
        JFrame frame = new JFrame();
        frame.add(panel, BorderLayout.CENTER);
        frame.pack();
        frame.setVisible(true);
	}
	
	
	static void showMeanOperonLengthByOrganismHisto() throws IOException
	{
		// All organisms.
        java.util.Vector<Double> dvec = new java.util.Vector<Double>();
        for (int i=0; i<ArkinlabIndexByOrganism.HTMLLINES.length; i++)
        {
	    	File ifile = ArkinlabOperonLengthHisto.indexLineToFile(ArkinlabIndexByOrganism.HTMLLINES[i]);
	    	ArkinlabOperonLengthHisto histo = new ArkinlabOperonLengthHisto(ifile);
	    	double meanOperonLength = histo.meanAndSdevBinPopulation()[0];
	    	dvec.add(meanOperonLength);
        }
        double[] darrAll = new double[dvec.size()];
        int n = 0;
        for (Double d: dvec)
        	darrAll[n++] = d;
        assert n == dvec.size();
        
        // Cyanos except PCT.
        dvec = new java.util.Vector<Double>();
        for (int i=0; i<ArkinlabSampleCyanos.CYANO_HTMLLINES.length; i++)
        {
	    	File ifile = ArkinlabOperonLengthHisto.indexLineToFile(ArkinlabSampleCyanos.CYANO_HTMLLINES[i]);
	    	ArkinlabOperonLengthHisto histo = new ArkinlabOperonLengthHisto(ifile);
	    	double meanOperonLength = histo.meanAndSdevBinPopulation()[0];
	    	dvec.add(meanOperonLength);
        }
        double[] darrCyanos = new double[dvec.size()];
        n = 0;
        for (Double d: dvec)
        	darrCyanos[n++] = d;
        assert n == dvec.size();  
        
        // PCT (stdout only).
        for (int i=0; i<PCT_INDEX_LINES.length; i++)
        {
	    	File ifile = ArkinlabOperonLengthHisto.indexLineToFile(PCT_INDEX_LINES[i]);
	    	ArkinlabOperonLengthHisto histo = new ArkinlabOperonLengthHisto(ifile);
	    	double meanOperonLength = histo.meanAndSdevBinPopulation()[0];
	    	sop(histo.getOrganismName() + ": mean predicted operon len = " + meanOperonLength);
        }
        
		HistogramDataset dataset = new HistogramDataset();
		dataset.addSeries("Cyano operon length", darrCyanos, 100);
		dataset.addSeries("Mean operon length, all Organisms", darrAll, 100);
        JFreeChart chart = ChartFactory.createHistogram(
        		"Mean operon length",
                null,
                null,
                dataset,
                PlotOrientation.VERTICAL,
                true,
                true,
                false);
        XYPlot plot = (XYPlot) chart.getPlot();
        plot.setDomainPannable(true);
        plot.setRangePannable(true);
        plot.setForegroundAlpha(0.85f);
        NumberAxis yAxis = (NumberAxis) plot.getRangeAxis();
        yAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
        XYBarRenderer renderer = (XYBarRenderer) plot.getRenderer();
        renderer.setDrawBarOutline(false);
        // flat bars look best...
        renderer.setBarPainter(new StandardXYBarPainter());
        renderer.setShadowVisible(false);        
        ChartPanel panel = new ChartPanel(chart);
        JFrame frame = new JFrame();
        frame.add(panel, BorderLayout.CENTER);
        frame.pack();
        frame.setVisible(true);
	}
	
	
	
	static void sop(Object x)	{ System.out.println(x); }
	
	
	public static void main(String[] args)
	{
		try
		{
			sop("START");
			showShortFracHisto();					// not as good as showMeanOperonLengthByOrganismHisto()
			showScatterPlots();
			showMeanOperonLengthByOrganismHisto();
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
