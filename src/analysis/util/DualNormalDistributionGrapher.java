package analysis.util;

import java.awt.Color;
import org.jfree.chart.*;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.*;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.*;
import org.jfree.ui.*;
import dexter.util.LocalNormalDistribution;




public class DualNormalDistributionGrapher extends ApplicationFrame {

	private LocalNormalDistribution				dist1;
	private LocalNormalDistribution				dist2;
	private double							maxX;
	
	
    public DualNormalDistributionGrapher(String title, double maxX, LocalNormalDistribution dist1, LocalNormalDistribution dist2) 
    {
        super(title);
        
        this.dist1 = dist1;
        this.dist2 = dist2;
        this.maxX = maxX;

        XYDataset dataset = createDataset();
        JFreeChart chart = createChart(dataset);
        ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setPreferredSize(new java.awt.Dimension(500, 270));
        setContentPane(chartPanel);

    }
    
    
    void setMaxX(float maxX)
    {
    	this.maxX = maxX;
    }
    
    /**
     * Creates a sample dataset.
     * 
     * @return a sample dataset.
     */
    private XYDataset createDataset() {
        
        XYSeries series1 = new XYSeries(dist1.getName());
        XYSeries series2 = new XYSeries(dist2.getName());
        for (double x=-20; x<50; x+=0.1)
        {
        	series1.add(x, dist1.fOfX(x));
        	series2.add(x, dist2.fOfX(x));
        }
        XYSeriesCollection dataset = new XYSeriesCollection();
        dataset.addSeries(series1);
        dataset.addSeries(series2);
                
        return dataset;
        
    }
    
    /**
     * Creates a chart.
     * 
     * @param dataset  the data for the chart.
     * 
     * @return a chart.
     */
    private JFreeChart createChart(final XYDataset dataset) {
        
        // create the chart...
        final JFreeChart chart = ChartFactory.createXYLineChart(
            "Expression Distance Distribution",      // chart title
            "Expression Distance",                      // x axis label
            "P(distance)",                      // y axis label
            dataset,                  // data
            PlotOrientation.VERTICAL,
            true,                     // include legend
            true,                     // tooltips
            false                     // urls
        );

        // NOW DO SOME OPTIONAL CUSTOMISATION OF THE CHART...
        chart.setBackgroundPaint(Color.white);
        
        // get a reference to the plot for further customisation...
        final XYPlot plot = chart.getXYPlot();
        plot.setBackgroundPaint(Color.lightGray);
    //    plot.setAxisOffset(new Spacer(Spacer.ABSOLUTE, 5.0, 5.0, 5.0, 5.0));
        plot.setDomainGridlinePaint(Color.white);
        plot.setRangeGridlinePaint(Color.white);

        NumberAxis domainAxis = (NumberAxis) plot.getDomainAxis();            
        domainAxis.setRange(-20, 50);
        NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();            
        rangeAxis.setRange(0, .1);
        

        // change the auto tick unit selection to integer units only...
        //final NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
        //rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
        // OPTIONAL CUSTOMISATION COMPLETED.
                
        return chart;
    }
    
    
    static void sop(Object x)			{ System.out.println(x); }

    
    public static void main(final String[] args) {
    	LocalNormalDistribution tery1 = new LocalNormalDistribution(6.9054623, 4.615163);
    	tery1.setName("Same operon: 1333 pairs");
    	LocalNormalDistribution tery2 = new LocalNormalDistribution(10.509467, 6.537533);
    	tery2.setName("Not-same operon: 1739 pairs");
        DualNormalDistributionGrapher teryGrapher = 
        	new DualNormalDistributionGrapher("Trichodesmium erythraeum", 30, tery1, tery2);
        teryGrapher.pack();
        teryGrapher.setVisible(true);

    	LocalNormalDistribution pro1 = new LocalNormalDistribution(4.567571,  4.379395);
    	pro1.setName("Same operon: 429 pairs");
    	LocalNormalDistribution pro2 = new LocalNormalDistribution(9.306983,  8.071912);
    	pro2.setName("Not-same operon: 637 pairs");
        DualNormalDistributionGrapher proGrapher = 
        	new DualNormalDistributionGrapher("Prochlorococcus marinus MED4", 30, pro1, pro2);
        proGrapher.pack();
        proGrapher.setVisible(true);

    	LocalNormalDistribution croco1 = new LocalNormalDistribution(10.97963,  8.0738945);
    	croco1.setName("Same operon: 1246 pairs");
    	LocalNormalDistribution croco2 = new LocalNormalDistribution(17.830431,  11.690574);
    	croco2.setName("Not-same operon: 1171 pairs");
        DualNormalDistributionGrapher crocoGrapher = 
        	new DualNormalDistributionGrapher("Crocosphaera watsonii WH 8501", 30, croco1, croco2);
        crocoGrapher.pack();
        crocoGrapher.setVisible(true);

    }
    

/********************
 
Trichodesmium erythraeum: 
	  Same operon: 1333 pairs.
	  m/sd = 6.9054623  4.615163
	  Not-same operon: 1739 pairs.
	  m/sd = 10.509467  6.537533
	Prochlorococcus marinus: 
	  Same operon: 429 pairs.
	  m/sd = 4.567571  4.379395
	  Not-same operon: 637 pairs.
	  m/sd = 9.306983  8.071912
	Crocosphaera watsonii: 
	  Same operon: 1246 pairs.
	  m/sd = 10.97963  8.0738945
	  Not-same operon: 1171 pairs.
	  m/sd = 17.830431  11.690574
	  
********************/

}