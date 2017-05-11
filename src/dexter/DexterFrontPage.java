package dexter;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.filechooser.*;

import java.io.*;

import dexter.model.*;
import dexter.util.gui.VerticalFlowLayout;
import dexter.view.wizard.*;


public class DexterFrontPage extends JDialog implements ActionListener
{
	private final static int			FONT_SIZE	= 36;
	
	private JButton						wizardBtn;
	private JButton						analysisBtn;
	private JButton						quitBtn;
	
	
	public DexterFrontPage()
	{
		VerticalFlowLayout lom = new VerticalFlowLayout();
		lom.setHonorAllPreferredSizes(true);
		lom.setHorizontalAlignment(Component.CENTER_ALIGNMENT);
		setLayout(lom);
		
		wizardBtn = new JButton("Wizard");
		wizardBtn.addActionListener(this);
		Font stdFont = wizardBtn.getFont();
		Font font = new Font(stdFont.getFamily(), Font.PLAIN, FONT_SIZE);
		wizardBtn.setFont(font);
		add(wizardBtn);
		
		analysisBtn = new JButton("Analysis");
		analysisBtn.addActionListener(this);
		analysisBtn.setFont(font);
		add(analysisBtn);
		
		quitBtn = new JButton("Quit");
		quitBtn.addActionListener(this);
		quitBtn.setFont(font);
		add(quitBtn);
		
		pack();
	}


	public void actionPerformed(ActionEvent e)
	{
		if (e.getSource() == wizardBtn)
		{
			(new DexterWizardDialog()).setVisible(true);
			this.setVisible(false);
		}
		
		else if (e.getSource() == analysisBtn)
		{
			JFileChooser chooser = SessionFileChooser.getInstance();
		    FileNameExtensionFilter filter = new FileNameExtensionFilter("Dexter session files", "dex");
		    chooser.setFileFilter(filter);
		    int approved = chooser.showOpenDialog(this);
		    if (approved != JFileChooser.APPROVE_OPTION)
		    	return;
		    File sessionFile = chooser.getSelectedFile();
		    try
		    {
		    	MainDexterFrame mainFrame = new MainDexterFrame(sessionFile);
		    	mainFrame.setVisible(true);
		    }
		    catch (IOException x)
		    {
		    	String err = "Couldn't open Dexter session file: " + x.getMessage();
		    	JOptionPane.showMessageDialog(this, err);
		    }
			this.setVisible(false);
		}
		
		else if (e.getSource() == quitBtn)
			System.exit(0);
	}
	
	
	public static void main(String[] args)
	{
		DexterFrontPage that = new DexterFrontPage();
		that.setVisible(true);		// modal
	}
}
