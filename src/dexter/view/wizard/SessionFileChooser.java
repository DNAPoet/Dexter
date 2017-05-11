package dexter.view.wizard;

import java.io.*;
import java.awt.*;
import javax.swing.*;
import javax.swing.filechooser.*;
import dexter.model.*;


public class SessionFileChooser extends JFileChooser
{
	private static SessionFileChooser		theInstance;
	
	
	private SessionFileChooser()
	{
		File cwd = new File("data/Sessions");
		if (!cwd.exists())
			cwd = new File(".");
		setCurrentDirectory(cwd);
		FileNameExtensionFilter filter = new FileNameExtensionFilter("Serialized Dexter sessions", "dex");
		setFileFilter(filter);
	}
	
	
	public SessionModel getSessionModel(Component parent)
	{
		int ret = showOpenDialog(parent);
		if (ret != JFileChooser.APPROVE_OPTION)
			return null;
		File f = getSelectedFile();
		if (f == null)
			return null;
		try
		{
			SessionModel model = new SessionModel(f);
			return model;
		}
		catch (Exception x)
		{
			String err = "Couldn't open saved session " + f.getAbsolutePath() + ": " + x.getMessage();
			JOptionPane.showMessageDialog(parent, err);
			return null;
		}
	}
	
	
	public static SessionFileChooser getInstance()
	{
		if (theInstance == null)
			theInstance = new SessionFileChooser();
		return theInstance;
	}
}
