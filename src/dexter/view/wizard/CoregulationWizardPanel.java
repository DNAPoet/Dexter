package dexter.view.wizard;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;

import dexter.coreg.*;
import dexter.model.*;
import dexter.util.gui.*;


class CoregulationWizardPanel extends JPanel
{
	private final static int						V_STRIP_PREF_W		= 350;
	private final static int						V_STRIP_PREF_H		= 450;
	private final static Color						BG					= Color.WHITE;
	private final static String[]					FILE_EXTENSIONS		=
	{
		".csv", ".tsv", ".txt", ".rkn"
	};
	
	private static JFileChooser						FILE_CHOOSER;

	
	static
	{
		FILE_CHOOSER = new JFileChooser(new File("data/Coregulation"));
		FILE_CHOOSER.setFileFilter(new Filter());
	}
	
	
	private StudyList								studies;
	private Map<String, Gene>						idToStudiedGene;
	private Map<Study, StudyPanel>					studyToPanel;
	
	
	CoregulationWizardPanel(StudyList studies, CoregulationFileCollection coregFiles)
	{
		this.studies = studies; 
		if (coregFiles == null)
			coregFiles = new CoregulationFileCollection();
		
		// Need a map from id to gene.
		idToStudiedGene = new HashMap<String, Gene>();
		for (Study study: studies)
		{
			if (study.isExperimentsStudy())
				continue;
			for (Gene gene: study)
				idToStudiedGene.put(gene.getId(), gene);
		}
		
		// Build gui.
		setLayout(new GridLayout(1, 0));
		studyToPanel = new TreeMap<Study, StudyPanel>();
		for (Study study: studies)
		{
			Organism org = study.getOrganism();
			Vector<CoregulationFile> filesForStudy = (coregFiles != null)  ?  
					coregFiles.get(org)  :  
					new Vector<CoregulationFile>();
			StudyPanel span = new StudyPanel(org, filesForStudy);
			span.setBorder(BorderFactory.createLineBorder(Color.BLACK, 2));
			add(span);
			studyToPanel.put(study, span);
		}
	}
	
	
	public void paintComponent(Graphics g)
	{
		g.setColor(Color.WHITE);
		g.fillRect(0, 0, getWidth(), getHeight());
	}
	
	
	private class StudyPanel extends JPanel implements ActionListener
	{
		private Organism									organism;
		private JButton										browseButton;
		private JPanel										cboxPan;
		private Vector<TaggedCheckBox<CoregulationFile>> 	fileCBoxes;

		
		StudyPanel(Organism organism, Vector<CoregulationFile> initialCoregFiles)
		{
			this.organism = organism;
			
			if (initialCoregFiles == null)
				initialCoregFiles = new Vector<CoregulationFile>();
			
			setLayout(new BorderLayout());
			JPanel north = new JPanel(new GridLayout(0, 1));
			JLabel label = new JLabel(organism.getShortestName(), JLabel.CENTER);
			label.setFont(new Font(label.getFont().getFamily(), Font.PLAIN, 18));
			north.add(label);
			browseButton = new JButton("Open...");
			browseButton.addActionListener(this);
			JPanel pan = new JPanel();
			pan.add(browseButton);
			north.add(pan);
			add(north, BorderLayout.NORTH);
			fileCBoxes = new Vector<TaggedCheckBox<CoregulationFile>>();
			cboxPan = new JPanel();
			VerticalFlowLayout lom = new VerticalFlowLayout();
			lom.setVerticalAlignment(Component.TOP_ALIGNMENT);
			cboxPan.setLayout(lom);
			for (CoregulationFile file: initialCoregFiles)
			{
				JPanel filePan = new JPanel(new GridLayout(2, 1));
				TaggedCheckBox<CoregulationFile> cbox = new TaggedCheckBox<CoregulationFile>(file, file.getName());
				cbox.setSelected(true);
				fileCBoxes.add(cbox);
				filePan.add(cbox);
				TaggedButton<CoregulationFile> inspectBtn = new TaggedButton<CoregulationFile>(file, "Inspect...", this);
				JPanel strip = new JPanel();
				strip.add(inspectBtn);
				filePan.add(strip);
				filePan.setBorder(BorderFactory.createLineBorder(Color.BLACK));
				cboxPan.add(filePan);
			}
			add(cboxPan, BorderLayout.CENTER);
		}
		
		public Dimension getPreferredSize()
		{
			return new Dimension(V_STRIP_PREF_W, V_STRIP_PREF_H);
		}
		
		public void paintComponent(Graphics g)
		{
			g.setColor(Color.WHITE);
			g.fillRect(0, 0, getWidth(), getHeight());
		}
		
		public void actionPerformed(ActionEvent e)
		{
			if (e.getSource() == browseButton)
			{
				// Browse for a coregulation file.
				if (FILE_CHOOSER.showOpenDialog(this) != JFileChooser.APPROVE_OPTION)
					return;
				CoregulationFile file = new CoregulationFile(FILE_CHOOSER.getSelectedFile(), organism);
				if (!file.isValid())
				{
					JOptionPane.showMessageDialog(this, "Invalid coregulation file: " + file.getName());
					return;
				}
				for (TaggedCheckBox<CoregulationFile> cbox: fileCBoxes)
				{
					if (cbox.getTag().equals(file))
					{
						JOptionPane.showMessageDialog(this, "Duplicate file.");
						return;
					}
				}
				// Add checkbox and INSPECT button for new file.
				JPanel filePan = new JPanel(new GridLayout(2, 1));
				TaggedCheckBox<CoregulationFile> cbox = new TaggedCheckBox<CoregulationFile>(file, file.getName());
				cbox.setSelected(true);
				fileCBoxes.add(cbox);
				filePan.add(cbox);
				TaggedButton<CoregulationFile> inspectBtn = new TaggedButton<CoregulationFile>(file, "Inspect...", this);
				JPanel strip = new JPanel();
				strip.add(inspectBtn);
				filePan.add(strip);
				filePan.setBorder(BorderFactory.createLineBorder(Color.BLACK));
				cboxPan.add(filePan);
				validate();
			}
			
			else
			{
				// Inspect a file.
				CoregulationFile file = ((TaggedButton<CoregulationFile>)e.getSource()).getTag();
				try
				{
					inspectCoregulationFile(file);
				}
				catch (IOException x)
				{
					String err = "Couldn't inspect file " + file.getName() + ": " + x.getMessage();
					JOptionPane.showMessageDialog(this, err);
				}
			}
		}
		
		Vector<CoregulationFile> getSelectedFiles()
		{
			Vector<CoregulationFile> ret = new Vector<CoregulationFile>();
			for (TaggedCheckBox<CoregulationFile> cbox: fileCBoxes)
				if (cbox.isSelected())
					ret.add(cbox.getTag());
			return ret;
		}
	}  // End of inner class StudyPanel
	
	
	private static class Filter extends FileFilter
	{
		public boolean accept(File f) 
		{
			for (String extension: FILE_EXTENSIONS)
				if (f.getName().endsWith(extension))
					return true;
			return false;
		}

		public String getDescription()
		{
			return "Coregulation files";
		}	
	}  // End of inner class FileFilter
	
	
	private void inspectCoregulationFile(CoregulationFile coregFile) throws IOException
	{
		// Determine study. For now assume 1-1 organism-to-study.
		Study study = null;
		for (Study s: studies)
		{
			if (s.getOrganism().equals(coregFile.getOrganism()))
			{
				study = s;
				break;
			}
		}
		assert study != null;
		
		// Load the file into coregulation groups.
		Vector<CoregulationGroup> coregGroups = coregFile.getCoregulationGroups();
		
		// For each group in the file, build a group inspector.		
		JPanel inspectorsPan = new JPanel(new VerticalFlowLayout(10));		// arg = child-to-child gap
		for (CoregulationGroup coregGroup: coregGroups)
		{
			assert coregGroup.size() >= 2;
			CoregulationGroupInspector inspector = new CoregulationGroupInspector(coregGroup, idToStudiedGene);
			inspectorsPan.add(inspector);
		}
		
		// Inspect.
		JScrollPane spane = new JScrollPane(inspectorsPan,
											JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
											JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		JPanel legendAndSpane = new JPanel(new BorderLayout());
		JPanel legend = CoregulationGroupInspector.getLegend();
		legend.setBorder(BorderFactory.createLineBorder(Color.BLACK));
		legendAndSpane.add(legend, BorderLayout.NORTH);
		spane.setPreferredSize(new Dimension(spane.getPreferredSize().width+14, 450));
		legendAndSpane.add(spane, BorderLayout.SOUTH);
		OkWithContentDialog dia = new OkWithContentDialog(legendAndSpane);
		dia.setModal(true);
		dia.pack();
		dia.setVisible(true);
	}
	
	
	CoregulationFileCollection getCoregulationFiles()
	{
		CoregulationFileCollection ret = new CoregulationFileCollection();
		for (Study study: studyToPanel.keySet())
		{
			Vector<CoregulationFile> filesForStudy = studyToPanel.get(study).getSelectedFiles();
			if (!filesForStudy.isEmpty())
				ret.put(study.getOrganism(), filesForStudy);
		}
		return ret;
	}
	
	
	static void sop(Object x)				{ System.out.println(x); }
	
	
	public static void main(String[] args)
	{
		try
		{
			File serfile = new File("data/Sessions/CPT.dex");
			SessionModel session = SessionModel.deserialize(serfile);
			StudyList studies = session.getStudies();
			assert studies.size() == 3;
			
			CoregulationFileCollection files = new CoregulationFileCollection();
			File teryFile = new File("data/Coregulation/Tery_OperonDB.csv");
			assert teryFile.exists();
			files.add(new CoregulationFile(teryFile, Organism.TERY));
			File crocoFile = new File("data/Coregulation/CrocoOperons.txt");
			assert crocoFile.exists();
			//files.add(new CoregulationFile(crocoFile, Organism.CROCO));
			
			CoregulationWizardPanel pan = new CoregulationWizardPanel(studies, files);
			OkWithContentDialog dia = new OkWithContentDialog(pan);		// packs
			dia.setTerminateOnAnyClick();
			dia.setVisible(true);
		}
		catch (Exception x) 
		{ 
			sop("STRESS: " + x.getMessage());
			x.printStackTrace(System.out);
		}
		finally
		{
			sop("DONE");
		}
	}
}
