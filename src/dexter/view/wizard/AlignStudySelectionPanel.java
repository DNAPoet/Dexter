package dexter.view.wizard;

import java.awt.*;
import java.util.*;
import javax.swing.*;
import javax.swing.border.*;
import dexter.model.*;
import dexter.util.*;
import dexter.util.gui.*;


class AlignStudySelectionPanel extends JPanel
{
	private DexterWizardPanel					owner;
	private Map<Study, StudyPreviewStrip>		studyToPreviewStrip;
	

	AlignStudySelectionPanel(DexterWizardPanel owner, StudyList studies)
	{
		assert studies != null  :  "null study list";
		this.owner = owner;
		
		studyToPreviewStrip = new HashMap<Study, StudyPreviewStrip>();
		setOpaque(false);
		setLayout(new VerticalFlowLayout());
		AlignmentCohortStudyOrganizer organizer = new AlignmentCohortStudyOrganizer(studies);
		for (StudyList cohort: organizer)
		{
			StripCohortPan subpan = new StripCohortPan(cohort);
			Border b = BorderFactory.createLineBorder(Color.BLACK, 2);
			if (cohort.size() > 1)
				b = BorderFactory.createTitledBorder(b, "Compatible timepoints");
			subpan.setBorder(b);
			add(subpan);
		}
	}
	
	
	private class AlignmentCohortStudyOrganizer extends Vector<StudyList>
	{
		AlignmentCohortStudyOrganizer(StudyList raw)
		{
			// For now, group by first element of name. Later group by timepoint compatibility.
			Map<String, StudyList> cohortNameToStudies = new LinkedHashMap<String, StudyList>();
			for (Study study: raw)
			{
				String key = study.getName().split("\\s")[0];
				StudyList cohort = cohortNameToStudies.get(key);
				if (cohort == null)
				{
					cohort = new StudyList();
					cohortNameToStudies.put(key, cohort);
				}
				cohort.add(study);
			}
			addAll(cohortNameToStudies.values());
		}
	}  // End of inner class AlignmentCohortStudyOrganizer
	
	
	private class StripCohortPan extends JPanel
	{
		StripCohortPan(StudyList cohort)
		{
			setLayout(new GridLayout(0, 1));
			setOpaque(false);
			for (Study study: cohort)
			{
				StudyPreviewStrip strip = new StudyPreviewStrip(study, cohort, owner);
				studyToPreviewStrip.put(study, strip);
				add(strip);
			}
		}
		
		public void paintComponent(Graphics g)
		{
			g.setColor(Color.BLACK);
			for (Component c: getComponents())
			{
				if (!(c instanceof StudyPreviewStrip))
					continue;
				StudyPreviewStrip strip = (StudyPreviewStrip)c;
				int top = strip.getLocation().y;
				if (top > 25)
					g.drawLine(3, top, getWidth()-3, top);
			}
		}
	}  // End of inner inner class StripCohortPan
	
	
	StudyPreviewStrip getPreviewStripForStudy(Study study)
	{
		return studyToPreviewStrip.get(study);
	}
	
	
	void setStudyIsAligned(Study study, boolean aligned)
	{
		assert study != null  :  "Null dataset";
		if (getPreviewStripForStudy(study) == null)
		{
			String err = "No preview strip for dataset " + study.getName() + ", have these datasets:";
			for (Study s: studyToPreviewStrip.keySet())
				err += "\n  " + s.getName();
			assert false : err;
		}
		assert getPreviewStripForStudy(study) != null  :  "No preview strip for dataset " + study.getName();
		getPreviewStripForStudy(study).setAligned(aligned);
	}
	
	
	static void sop(Object x)		{ System.out.println(x); }
}

