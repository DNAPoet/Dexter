package dexter.view.graph.experiment;

import java.util.Map;
import java.util.Vector;
import dexter.model.*;
import dexter.view.graph.GraphBackgroundModel;
import dexter.view.graph.ThumbnailGraph;


public class ExperimentThumbnailGraph extends ThumbnailGraph
{
	public ExperimentThumbnailGraph(Experiment experiment, 
									SessionModel session,
			                        GraphBackgroundModel backgroundModel, 
			                        Map<Gene, Vector<float[]>> geneToTimeAndExpression)
	{
		super(experiment.getName(), session, backgroundModel, geneToTimeAndExpression);
		setExperiment(experiment);
	}
}
