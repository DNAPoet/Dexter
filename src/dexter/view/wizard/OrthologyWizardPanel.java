package dexter.view.wizard;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;
import java.io.*;
import java.util.*;
import dexter.VisualConstants;
import dexter.model.*;
import dexter.ortholog.*;
import dexter.util.*;
import dexter.util.gui.*;


class OrthologyWizardPanel extends JPanel implements ActionListener, ItemListener, VisualConstants
{
	private final static Dimension					PREF_SIZE			= new Dimension(1050, 678);  // empirical
	private final static File						ORTHO_DIRF			= new File("data/Orthologs");
	private final static Font						INSTRUX_FONT		= 
		new Font("Serif", Font.BOLD+Font.ITALIC, 32);
	private final static String						INSTRUX_LINES		=
		"Load\northologs\nfrom\nlist file(s)\nor tabular\nBLAST\nfile(s)";
	
	private GeneIdToOrganismMap						idToOrganism;
	private MultilineLabel							instructionsPan;	// until an orthology file is loaded
	private OrthologySummaryPanel					summaryPan;
	private Component								centerComponent;	// instructionsPan or summaryPanSpane
	private CheckoffTable<Organism>					checkoff;
	private JFileChooser							fileChooser;
	private JButton									openBtn;
	private JButton									unloadBtn;
	private JButton									exportBtn;
	private DynamicVerticalCheckboxHolder<File> 	cboxHolder;
	private OrthologyFileCollection					fileCollection;
	
	
	OrthologyWizardPanel(GeneIdToOrganismMap idToOrganism)
	{
		this.idToOrganism = idToOrganism;
		
		fileCollection = new OrthologyFileCollection();
		
		setOpaque(true);
		setBackground(Color.WHITE);
		setLayout(new BorderLayout());
		
		// Panel for 4 buttons and list of orthologies files.
		VerticalFlowLayout vflom = new VerticalFlowLayout();
		vflom.setVerticalAlignment(CENTER_ALIGNMENT);
		JPanel west = new JPanel(vflom);
		west.setOpaque(false);
		JPanel orthoFilesPan = new JPanel(new BorderLayout());			
		orthoFilesPan.setOpaque(false);
		JPanel btnPan = new JPanel(new VerticalFlowLayout());
		btnPan.setOpaque(false);
		openBtn = new JButton("Open...");
		openBtn.addActionListener(this);
		btnPan.add(openBtn);
		unloadBtn = new JButton("Unload selected orthology files");
		unloadBtn.addActionListener(this);
		unloadBtn.setEnabled(false);
		btnPan.add(unloadBtn);
		exportBtn = new JButton("Export to merged orthology file...");
		exportBtn.addActionListener(this);
		exportBtn.setEnabled(false);
		btnPan.add(exportBtn);
		orthoFilesPan.add(btnPan, BorderLayout.NORTH);
		cboxHolder = new DynamicVerticalCheckboxHolder<File>();
		cboxHolder.setOpaque(false);
		cboxHolder.addItemListener(this);
		orthoFilesPan.add(cboxHolder, BorderLayout.CENTER);
		orthoFilesPan.setBorder(createSubpanelBorder());
		enableExportButton();
		west.add(orthoFilesPan);
		
		// Checkoff panel that shows which organisms have loaded orthologs.
		checkoff = new CheckoffTable<Organism>(collectAllOrganismsInSession());
		checkoff.setMarkUnchecked(true);
		Border b = createSubpanelBorder();
		String s = "Organisms with imported orthologies";
		b = BorderFactory.createTitledBorder(b, s);
		checkoff.setBorder(b);
		west.add(checkoff);
		add(west, BorderLayout.WEST);
		
		// Initially, and whenever zero orthology files are selected, an instructions panel is
		// displayed in place of the summary panel.
		instructionsPan = new MultilineLabel(INSTRUX_LINES, INSTRUX_FONT);
		instructionsPan.setTextColor(Color.BLUE);
		instructionsPan.setOpaque(false);
		int containerPrefH = DexterWizardPanel.getPreferredHeight();
		int prefW = instructionsPan.getPreferredSize().width;
		instructionsPan.setPreferredSize(new Dimension(prefW, containerPrefH-20));
		installAtCenter(instructionsPan);
		add(instructionsPan, BorderLayout.CENTER);
		
		// A single file chooser is used for both kinds of orthology file and for exporting.
		fileChooser = new JFileChooser(ORTHO_DIRF);
		fileChooser.setFileFilter(new NoRepeatsFilter());
	}
	
	
	public Dimension getPreferredSize()
	{
		return PREF_SIZE;
	}
	
	
	private static Border createSubpanelBorder()
	{
		return BorderFactory.createLineBorder(Color.BLACK, 3);
	}
	
	
	private void installAtCenter(Component c)
	{
		assert c != null;
		
		if (centerComponent != null)
			remove(centerComponent);
		
		add(c, BorderLayout.CENTER);
		centerComponent = c;
		
		validate();
		repaint();
	}
	
	
	// Need some kind of implementation to cover up ghost components after they're removed.
	public void repaint(Graphics g)
	{
		g.setColor(Color.WHITE); 
		g.fillRect(0, 0, 2222, 2222);
	}
	
	
	// Won't accept a file that's already loaded.
	private class NoRepeatsFilter extends javax.swing.filechooser.FileFilter
	{
		public boolean accept(File f) 
		{
			return !(getListFiles().contains(f) || getBLASTFiles().contains(f));
		}

		public String getDescription() 
		{
			return "Orthology files (lists of IDs or tabular BLAST results";
		}		
	}
	
	
	public void itemStateChanged(ItemEvent e)
	{
		int nSel = cboxHolder.getSelectedTags().size();
		unloadBtn.setEnabled(nSel > 0);
	}

	
	void enableExportButton()
	{
		boolean enabled = getListFiles().size()  +  getBLASTFiles().size() > 0;
		exportBtn.setEnabled(enabled);
	}
	
	
	private Vector<File> getListFiles()		
	{ 
		assert fileCollection != null;
		return fileCollection.getListFiles();
	}
	
	
	private Vector<File> getBLASTFiles()		
	{ 
		assert fileCollection != null;
		return fileCollection.getTabularBLASTFiles();
	}
	
	
	// Called externally to initialize, e.g. from a saved session, or internally to load
	// an individual file (encapsulated in a size-1 collection) specified by the user.
	public void loadOrthologyFiles(OrthologyFileCollection files)
	{
		if (files != null)
		{
			Iterator<File> iter = files.iterator();
			while (iter.hasNext())
			{
				File file = iter.next();
				cboxHolder.addTag(file, file.getName());
				cboxHolder.setToolTipText(file, file.getAbsolutePath());
			}
		}
		
		enableExportButton();
		
		loadOrthologies();
	}
	
	
	public void actionPerformed(ActionEvent e)
	{	
		if (e.getSource() == openBtn)
		{			
			if (fileChooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION)
				return;
			File file = fileChooser.getSelectedFile();
			boolean listNotBLAST = false;
			try
			{
				listNotBLAST = OrthologyGraph.isCommaDelimitedListFile(file);
				if (!listNotBLAST)
				{
					listNotBLAST = !OrthologyGraph.isTabularBlastFile(file);
					if (listNotBLAST)
						throw new IOException();
				}
			}
			catch (IOException x)
			{
				String err = "Illegal file format in " + file.getPath();
				JOptionPane.showMessageDialog(this, err);
				return;
			}
			if (listNotBLAST)
				fileCollection.addListFile(file);
			else
				fileCollection.addTabularBLASTFile(file);
			loadOrthologyFiles(new OrthologyFileCollection(file, listNotBLAST));
		}
		
		else if (e.getSource() == exportBtn)
		{
			if (fileChooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION)
				return;
			File ofile = fileChooser.getSelectedFile();
			try
			{
				FileWriter fw = new FileWriter(ofile);
				Vector<OrthologyGroup> orthoGroups = summaryPan.getOrthologyGroups();
				for (OrthologyGroup og: orthoGroups)
					fw.write(og + "\n");
				fw.flush();
				fw.close();
			}
			catch (IOException x)
			{
				String err = "Can't write orthology file " + ofile + ": " + x.getMessage();
				JOptionPane.showMessageDialog(this, err);
			}
		}
		
		else if (e.getSource() == unloadBtn)
		{
			Collection<File> selectedFiles = cboxHolder.getSelectedTags();
			cboxHolder.removeTags(selectedFiles);
			for (File removeMe: selectedFiles)
				fileCollection.removeFile(removeMe);
			loadOrthologies();
			int nSel = cboxHolder.getSelectedTags().size();
			unloadBtn.setEnabled(nSel > 0);	
			enableExportButton();
		}
	}
	
	
	// Ordinarily installs a new summary panel and returns true (caller needs to validate layout). If no
	// orthology files are selected, installs the instructions panel. If orthology files are selected but
	// they contain no orthologies for the current set of organisms, leaves the layout alone and displays
	// a dialog.
	private void loadOrthologies()
	{
		// No orthology files are selected. Install the instructions panel.
		if (fileCollection.size() == 0)
		{
			remove(centerComponent);
			installAtCenter(instructionsPan);
			checkoff.setAllChecked(false);
			return;
		}
		
		try
		{			
			// Build new summary panel unless zero orthologies.
			OrthologyGraph orthoGraph = new OrthologyGraph(idToOrganism);
			orthoGraph.loadCommaDelimitedListFiles(getListFiles());
			orthoGraph.loadTabularBlastFiles(getBLASTFiles());
			if (orthoGraph.isEmpty())
			{
				// No orthologies.
				checkoff.setAllChecked(false);
				String msg = "No orthologies found for organisms of selected datasets.";
				JOptionPane.showMessageDialog(this, msg);
				return;
			}
			
			// Found orthologies. Build a summary panel: a vertical stack of strips, 1 per
			// orthology group. If >= 5 strips, install summary in a scrollpane.
			Component nextCenter = null;
			summaryPan = new OrthologySummaryPanel(orthoGraph);
			int nThumbnails = summaryPan.getNThumbnailStrips();
			assert nThumbnails > 0;
			if (nThumbnails <= 4)
			{
				JPanel pan = new JPanel(new MidlineFlowLayout());
				pan.setOpaque(false);
				pan.add(summaryPan);
				nextCenter = pan;
			}
			else
			{
				JScrollPane summaryPanSpane = new JScrollPane(summaryPan,
					    JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, 
	                    JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
				Dimension spanePref = summaryPanSpane.getPreferredSize();
				int prefW = spanePref.width + 20;
				int prefH = DexterWizardPanel.getPreferredHeight() - 34;
				summaryPanSpane.setPreferredSize(new Dimension(prefW, prefH));
				nextCenter = summaryPanSpane;
			}
			
			// Install next panel.
			installAtCenter(nextCenter);	
			
			// Update checks and xs in checkoff table.
			updateCheckoffs(orthoGraph);		
		}
		catch (IOException x)
		{
			String err = "Trouble loading orthology file: " + x.getMessage();
			JOptionPane.showMessageDialog(this, err);
		}
	}
	
	
	private void updateCheckoffs(OrthologyGraph graph)
	{
		checkoff.setAllChecked(false);
		Collection<Organism> organismsRepresentedInGraph = graph.collectOrganisms();
		for (Organism org: organismsRepresentedInGraph)
			checkoff.setChecked(org, true);
	}
	
	
	Vector<OrthologyGroup> getOrthologyGroups()
	{
		return summaryPan.getOrthologyGroups();
	}
	
	
	OrthologyFileCollection getOrthologyFiles()
	{
		OrthologyFileCollection ret = new OrthologyFileCollection();
		
		for (File file: getListFiles())
			ret.addListFile(file);
		
		for (File file: getBLASTFiles())
			ret.addTabularBLASTFile(file);
		
		return ret;
	}
	
	
	// The gene ID to organism map contains all organisms represented in all imported datasets.
	private Collection<Organism> collectAllOrganismsInSession()
	{
		return new TreeSet<Organism>(idToOrganism.values());
	}	

	
	static void sop(Object x)	{ System.out.println(x); }
	
	
	public static void main(String[] args)
	{				
		try
		{
			DexterWizardPanel.main(args);
		}
		catch (Exception x)
		{
			sop("Stress: " + x.getMessage());
			x.printStackTrace();
		}
		finally
		{
			sop("DONE");
		}
	}
}
