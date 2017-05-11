package dexter.view.wizard;

import java.util.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import javax.swing.*;
import dexter.util.*;
import dexter.util.gui.TaggedCheckBox;
import dexter.model.Study;


class ImportSelectionPanel extends JPanel implements ItemListener, ActionListener
{
	private DexterWizardPanel						wizardPan;
	private Vector<TaggedCheckBox<File>>			cboxes;
	private JButton									selectAllBtn;
	private JButton									deleteBtn;
	
	
	ImportSelectionPanel(File dirf, DexterWizardPanel wizardPan)
	{
		assert dirf.exists();
		
		this.wizardPan = wizardPan;
		
		setLayout(new GridLayout(0, 1));
		setOpaque(false);
		
		// Checkboxes.
		Set<String> sorter = new TreeSet<String>();
		for (String kid: dirf.list())
		{
			if (kid.endsWith("__imported.ser"))
				sorter.add(kid);
		}
		cboxes = new Vector<TaggedCheckBox<File>>();
		for (String kid: sorter)
		{
			File studyFile = new File(dirf, kid);
			String studyName = getStudyName(studyFile);
			TaggedCheckBox<File> cbox = new TaggedCheckBox<File>(studyFile, studyName);
			cbox.setSelected(true);
			cbox.addItemListener(this);
			cboxes.add(cbox);
			add(cbox);
		}
		
		// SELECT-ALL and DELETE buttons.
		JPanel pan = new JPanel();
		selectAllBtn = new JButton("Select all");
		selectAllBtn.addActionListener(this);
		pan.add(selectAllBtn);
		deleteBtn = new JButton("Delete imported data sets");
		deleteBtn.setEnabled(false);
		deleteBtn.addActionListener(this);
		pan.add(deleteBtn);
		add(pan);
	}
	
	
	public void actionPerformed(ActionEvent e)
	{
		if (e.getSource() == selectAllBtn)
		{
			for (JCheckBox cbox: cboxes)
				cbox.setSelected(true);
		}
		
		else if (e.getSource() == deleteBtn)
		{
			if (wizardPan != null)
				wizardPan.deleteImports(getSelectedSerializedImportedStudyFiles());
		}
	}
	
	
	public void itemStateChanged(ItemEvent e)
	{
		boolean somethingSelected = false;
		for (JCheckBox cbox: cboxes)
		{
			if (cbox.isSelected())
			{
				somethingSelected = true;
				break;
			}
		}
		deleteBtn.setEnabled(somethingSelected);
	}
	
	
	void selectAll(boolean b)
	{
		for (JCheckBox cbox: cboxes)
			cbox.setSelected(b);
	}
	
	
	void selectOnly(Collection<File> files)
	{
		for (TaggedCheckBox<File> cbox: cboxes)
			cbox.setSelected(files.contains(cbox.getTag()));
	}
	
	
	private String getStudyName(File importSerfile)
	{
		String s = importSerfile.getName();		// e.g. "Zinser_Pro__imported.ser"
		assert s.endsWith("__imported.ser");
		return s.substring(0, s.lastIndexOf("__imported.ser"));
	}
	
	
	Vector<File> getSelectedSerializedImportedStudyFiles()
	{
		Vector<File> ret = new Vector<File>();
		for (TaggedCheckBox<File> cbox: cboxes)
			if (cbox.isSelected())
				ret.add(cbox.getTag());
		return ret;
	}
	
	
	static void sop(Object x)						{ System.out.println(x); }
	

	public static void main(String[] args)
	{
		try
		{
			JFrame frame = new JFrame();			
			File dirf = new File("data/ImportedStudies");
			ImportSelectionPanel that = new ImportSelectionPanel(dirf, null);
			frame.add(that, BorderLayout.CENTER);
			frame.pack();
			frame.setVisible(true);
		}
		catch (Exception x)
		{
			sop("Stress: " + x.getMessage());
			x.printStackTrace();
		}
	}
}
