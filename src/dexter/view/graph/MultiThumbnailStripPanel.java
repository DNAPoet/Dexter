package dexter.view.graph;

import java.io.*;
import java.util.*;
import java.awt.*;

import javax.swing.*;

import dexter.MainDexterFrame;
import dexter.cluster.*;
import dexter.event.*;
import dexter.model.*;
import dexter.util.*;
import dexter.util.gui.*;
import dexter.view.graph.*;
import dexter.view.graph.experiment.*;
import static analysis.thesis.SampleArkins10Per.*;


public class MultiThumbnailStripPanel extends JPanel implements TimeAndExpressionProvider
{
	private final static String 					TIP_TEXT = "Click to select, shift-click to expand.";
	
	private MainDexterFrame							mainFrame;
	private Map<Study, TimeAssignmentMap>  			studyToTAM;		// null value for the experiments study
	private Vector<ThumbnailStrip> 					strips;			// includes the experiment strip
	private ExperimentThumbnailStrip				experimentStrip;
	private ScrollpaneLockstepManager				lockstepManager;
	private Map<Gene, Vector<float[]>>				completeGeneToTimeAndExpression;
	private Set<ThumbnailListener>					thumbnailListeners;
	

	
					
	
	
	
	
	
					//////////////////////////////////////////////////////////////////
					//                                                              //
					//                         CONSTRUCTION                         //
					//                                                              //
					//////////////////////////////////////////////////////////////////
					


	

	public MultiThumbnailStripPanel (File serializedSessionFile, 
									 boolean includeExperiments,
									 MainDexterFrame mainFrame) throws IOException
	{
		try
		{
			SessionModel sessionModel = new SessionModel(serializedSessionFile);
			init(sessionModel, includeExperiments, mainFrame);
		}
		catch (ClassNotFoundException x)
		{
			sop("Can't deserialize " + serializedSessionFile.getAbsolutePath() + "\n" + x.getMessage());
			x.printStackTrace(System.out);
			System.exit(1);
		}
	}
	
	
	public MultiThumbnailStripPanel(SessionModel sessionModel, 
									boolean includeExperiments,
									MainDexterFrame mainFrame) throws IOException
	{
		init(sessionModel, includeExperiments, mainFrame);
	}
	
	
	private void init(SessionModel session, boolean includeExperiments, MainDexterFrame mainFrame) throws IOException
	{
		this.mainFrame = mainFrame;
		
		thumbnailListeners = new HashSet<ThumbnailListener>();
		
		setLayout(new GridLayout(1, 0));
		
		// Vertical strips of thumbnails, initially alphabetized by study, with optional experiments strip at the end.
		Map<Study, TimeAssignmentMap> rawStudyToTimeAssignmentMap = 
			session.getStudyToTimeAssignmentMap();
		Map<String, Study> studySorterByName = new TreeMap<String, Study>();
		for (Study study: rawStudyToTimeAssignmentMap.keySet())
			studySorterByName.put(study.getName(), study);
		studyToTAM = new LinkedHashMap<Study, TimeAssignmentMap>();
		for (Study study: studySorterByName.values())
			studyToTAM.put(study, rawStudyToTimeAssignmentMap.get(study));
		
		// This object maintains a map from gene to time-expression pairs. Experiment graphs
		// use the map to copy expression data from existing thumbnails.
		completeGeneToTimeAndExpression = new HashMap<Gene, Vector<float[]>>();
		for (Study study: session.getStudies())
		{
			if (study.isExperimentsStudy())
				continue;
			TimeAssignmentMap timeAssignments = session.getTimeAssignmentMapForStudy(study);
			for (Gene gene: study)
			{
				Vector<float[]> timepointsForGene = gene.getTimeAndExpressionPairs(timeAssignments);
				completeGeneToTimeAndExpression.put(gene, timepointsForGene);
			}	
		}

		// Build vertical strips.
		strips = new Vector<ThumbnailStrip>();
		lockstepManager = new ScrollpaneLockstepManager();
		for (Study study: studyToTAM.keySet())
		{
			// Build strip for study.
			assert study != null;
			ThumbnailStrip strip = new ThumbnailStrip(session, study, completeGeneToTimeAndExpression, mainFrame);
			strip.setThumbnailToolTipText(TIP_TEXT);
			// Add to gui.
			add(strip);
			strips.add(strip);
			lockstepManager.add(strip.getScrollPane());
			// Collect gene to time-expression data.
			for (ThumbnailGraph thumbnail: strip.getThumbnails())
			{
				Map<Gene, Vector<float[]>> geneTimeExMapForThumbnail = 
					thumbnail.getGeneToTimeAndExpressionMap();
				completeGeneToTimeAndExpression.putAll(geneTimeExMapForThumbnail);
			}
		}	
		
		// Extra strip for the experiments study, if requested.
		if (includeExperiments)
		{
			ExperimentsStudy experStudy =  session.getExperimentsStudy();
			assert experStudy != null  :  "null experiments study";
			experimentStrip = new ExperimentThumbnailStrip(session);
			experimentStrip.setThumbnailToolTipText(TIP_TEXT);
			add(experimentStrip);
			strips.add(experimentStrip);
			lockstepManager.add(experimentStrip.getScrollPane());
		}
	}
	

	
	
				
					
					
	
					
					//////////////////////////////////////////////////////////////////
					//                                                              //
					//                          EXPERIMENTS                         //
					//                                                              //
					//////////////////////////////////////////////////////////////////
					
	
	
	
	// Creates and returns an Experiment instance. Registers its name.
	public Experiment addExperimentFor(Collection<Gene> genes, String experimentName)
	{
		return experimentStrip.addExperimentForSourceAndRegisterName(experimentName, genes);
	}
	
	
	public ExperimentThumbnailStrip getExperimentStrip()
	{
		return experimentStrip;
	}
	
	
	
	
		
			
			
			//////////////////////////////////////////////////////////////////////////
			//                                                                      //
			//                              RESTRICTIONS                            //
			//                                                                      //
			//////////////////////////////////////////////////////////////////////////
			

	
	
	// Apply restrictions to all strips except the Experiments strip.
	public void applyExpressionRestrictions(ExpressionRestrictionModel restrictions)
	{
		for (ThumbnailStrip strip: strips)
			strip.regroupAndRestrict(restrictions);
	}
	
	
	public void unrestrict()
	{
		for (ThumbnailStrip strip: strips)
			if (!strip.isExperimentsStrip())
				strip.unrestrict();
	}
	
	
	public SelectionModel getSelections()
	{
		SelectionModel selections = new SelectionModel();
		
		for (ThumbnailStrip strip: strips)
		{
			if (strip.isSelected())
			{
				selections.addStrip(strip);
			}
			else
			{
				for (ThumbnailGraph thumbnail: strip.getThumbnails())
				{
					if (thumbnail.isSelected())
					{
						selections.addThumbnail(thumbnail);
					}
				}
			}
		}
		
		return selections;
	}
	
	
	

				
				
				////////////////////////////////////////////////////////////
				//                                                        //
				//                        CLUSTERING                      //
				//                                                        //
				////////////////////////////////////////////////////////////
				

	
	
	// Strips for clusters are added dynamically.
	public void addClusterThumbnailStrip(MainDexterFrame mainFrame, Vector<Vector<Gene>> genes, Node<Gene> root)
	{
		Point mainFrameLocation = mainFrame.getLocation();
		ClusterThumbnailStrip strip = new ClusterThumbnailStrip(mainFrame.getSessionModel(), genes, root, mainFrame);
		for (ThumbnailListener tl: thumbnailListeners)
			strip.addThumbnailListener(tl);
		add(strip);
		mainFrame.pack();
		mainFrame.setLocation(mainFrameLocation); 		// adding strip expands main frame to the left
		strips.add(strip);
		lockstepManager.add(strip.getScrollPane());
	}
	
	
	// Cluster strips may be dynamically removed.
	public void removeClusterStrip(ClusterThumbnailStrip strip)
	{
		strips.remove(strip);
		JScrollPane spane = strip.getScrollPane();
		lockstepManager.remove(spane);
		remove(strip);
		mainFrame.pack();
	}
				
			
	
	
	
	
	
				
				////////////////////////////////////////////////////////////
				//                                                        //
				//                        GROUPING                        //
				//                                                        //
				////////////////////////////////////////////////////////////
				
	
	
	
	
	// Returns a vector containing new # of thumbnails for all regrouped strips.
	public void regroup(GroupGenesBy groupBy, 
						OrderGeneGroupsBy orderBy, 
						Metric metric, 
						ExpressionRestrictionModel restrictions)
	{
		
		for (ThumbnailStrip strip: strips)
		{
			if (!strip.isExperimentsStudy())
			{
				strip.regroupAndRestrict(groupBy, orderBy, metric, restrictions);
				sop(strip.getTitle() + ": " + strip.getThumbnails().size() + " groups, " + strip.countGenes() + " genes");			
			}
		}
	}
	
	
	
	
	
	
	

					
					//////////////////////////////////////////////////////////////////
					//                                                              //
					//                              MISC                            //
					//                                                              //
					//////////////////////////////////////////////////////////////////
					

			

	
	public void setScrollLockstep(boolean b)
	{
		lockstepManager.setEnabled(b);
	}
	
	
	// Delegates to all the thumbnail graphs.
	public void addThumbnailListener(ThumbnailListener tl)
	{
		thumbnailListeners.add(tl);
		for (ThumbnailStrip strip: strips)
			strip.addThumbnailListener(tl);
	}
	
	
	// Delegates to all the thumbnail graphs.
	public void removeThumbnailListener(ThumbnailListener tl)
	{
		thumbnailListeners.remove(tl);
		for (ThumbnailStrip strip: strips)
			strip.removeThumbnailListener(tl);
	}
	
	
	public void deselectAll()
	{
		for (ThumbnailStrip strip: strips)
			strip.deselectAll();
		experimentStrip.enableOrDisableRemoveBtn();
	}
	
	
	public Vector<ThumbnailStrip> getThumbnailStrips()
	{
		return strips;
	}
	
	
	public Vector<ThumbnailStrip> getSelectedThumbnailStrips()
	{
		Vector<ThumbnailStrip> ret = new Vector<ThumbnailStrip>();
		for (ThumbnailStrip strip: strips)
			if (strip.isSelected())
				ret.add(strip);
		return ret;
	}
	
	
	public Vector<float[]> getTimeAndExpressionPairsForGene(Gene gene)
	{
		return completeGeneToTimeAndExpression.get(gene);
	}
	
	
	public Map<Gene, Vector<float[]>> getCompleteGeneToTimeAndExpressionMap()
	{
		return completeGeneToTimeAndExpression;
	}
	
	
	public Vector<Gene> collectGenesBySelectionLevel(GeneSelectionLevel level)
	{
		Vector<Gene> ret = new Vector<Gene>();
		for (ThumbnailStrip strip: strips)
			ret.addAll(strip.collectGenesBySelectionLevel(level));		
		return ret;
	}
	
	
	public Map<Gene, Vector<float[]>> collectGenesToTxsBySelectionLevel(GeneSelectionLevel level)
	{
		Map<Gene, Vector<float[]>> ret = new HashMap<Gene, Vector<float[]>>();
		for (ThumbnailStrip strip: strips)
		{
			Map<Gene, Vector<float[]>> submap = strip.collectGenesToTxsBySelectionLevel(level);
			for (Gene gene: submap.keySet())
				ret.put(gene, submap.get(gene));
		}
		return ret;
	}
	
	
	public Vector<ThumbnailGraph> collectSelectedThumbnails()
	{
		Vector<ThumbnailGraph> ret = new Vector<ThumbnailGraph>();
		for (ThumbnailStrip strip: strips)
			ret.addAll(strip.collectSelectedThumbnails());
		return ret;
	}
	
	
	public MainDexterFrame getMainFrame()
	{
		return mainFrame;
	}
	
	
	static void sop(Object x)			{ System.out.println(x); }
	
	
	public static void main(String[] args)
	{
		try
		{
			MainDexterFrame.main(args);
		}
		catch (Exception x)
		{
			sop("Stress: " + x.getMessage());
			x.printStackTrace();
		}
	}
}
