package dexter.view.wizard;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.io.*;
import dexter.model.*;


public class DexterWizardDialog extends JDialog
{
	private WizardStagesPanel					stagePan;
	private DexterWizardPanel					wizardPan;
	
	
	public DexterWizardDialog()
	{
		this(null);
	}
	
	
	public DexterWizardDialog(File sessionFile)
	{
		stagePan = new WizardStagesPanel(DexterWizardStage.getLongNames());
		add(stagePan, BorderLayout.NORTH);
		
		wizardPan = new DexterWizardPanel(this, sessionFile);
		add(wizardPan, BorderLayout.CENTER);
		stagePan.addWizardStageListener(wizardPan);
		
		pack();
	}

	
	public SessionModel getSessionModel()
	{
		return wizardPan.buildSessionModel();
	}
	
	
	static void sop(Object x)				{ System.out.println(x); }
	
	
	public static void main(String[] args)
	{
		try
		{
			File ifile = new File("data/sessions/CPT.dex");
			if (ifile != null  &&  !ifile.exists())
				ifile = null;
			DexterWizardDialog dia = new DexterWizardDialog(ifile);
			dia.setVisible(true);
		}
		catch (Exception x)
		{
			sop("Stress: " + x.getMessage());
			x.printStackTrace();
		}
	}
}
