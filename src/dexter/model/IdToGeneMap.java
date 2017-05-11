package dexter.model;

import java.util.HashMap;
import dexter.cluster.NewickNodeNameProvider;


public class IdToGeneMap extends HashMap<String, Gene>
{
	public IdToGeneMap(SessionModel session)
	{
		for (Study study: session.getStudiesOmitExperiments())
			for (Gene gene: study)
				put(gene.getId(), gene);
	}
}
