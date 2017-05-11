package dexter.view.wizard;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.io.*;
import dexter.model.*;


class WrapPanel extends JPanel implements ActionListener
{
	private DexterWizardPanel			wizardPan;
	private JButton						proceedBtn;
	private JButton						saveBtn;
	private JButton						quitBtn;
	
	
	WrapPanel(DexterWizardPanel wizardPan)
	{
		this.wizardPan = wizardPan;
		setOpaque(true);
		setBackground(Color.WHITE);
		
		proceedBtn = new JButton("Proceed to analysis...");
		proceedBtn.addActionListener(this);
		add(proceedBtn);
		
		saveBtn = new JButton("Save session configuration...");
		saveBtn.addActionListener(this);
		add(saveBtn);
		
		quitBtn = new JButton("Quit");
		quitBtn.addActionListener(this);
		add(quitBtn);
		
		setBorder(BorderFactory.createLineBorder(Color.BLACK));
	}


	public void actionPerformed(ActionEvent e) 
	{
		if (e.getSource() == proceedBtn)
		{
			wizardPan.setWindowInvisible();
			SessionModel sessionModel = wizardPan.buildSessionModel();
			try
			{
				sessionModel.open();
			}
			catch (IOException x)
			{
				String err = "Couldn't open session.";
				JOptionPane.showMessageDialog(this, err);
			}
		}
		
		else if (e.getSource() == saveBtn)
		{
			JFileChooser fileChooser = SessionFileChooser.getInstance();
			int ret = fileChooser.showSaveDialog(this);
			if (ret != JFileChooser.APPROVE_OPTION)
				return;
			SessionModel sessionModel = wizardPan.buildSessionModel();
			assert sessionModel.getOrthologyFiles() != null  :  "null orthofiles";	// empty is ok, null isn't
			for (Study study: sessionModel.getStudies())
				study.validateGenes();
			try
			{
				File file = fileChooser.getSelectedFile();
				String fname = file.getName();
				if (!fname.endsWith(".dex"))
					file = new File(file.getParent(), fname+".dex");
				sessionModel.serialize(file);
			}
			catch (IOException x)
			{
				String err = "Couldn't save session: " + x.getMessage();
				JOptionPane.showMessageDialog(this, err);
				x.printStackTrace();
			}
		}
		
		else if (e.getSource() == quitBtn)
		{
			System.exit(0);
		}
	}
	
	
	static void sop(Object x)			{ System.out.println(x); }
	
	
	public static void main(String[] args)
	{
		DexterWizardPanel.main(args);
	}
}
