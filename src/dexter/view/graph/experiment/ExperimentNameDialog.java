package dexter.view.graph.experiment;

import java.awt.BorderLayout;
import java.awt.event.*;
import javax.swing.*;
import dexter.model.Experiment;


class ExperimentNameDialog extends JDialog implements ActionListener
{
	private JTextField			tf;
	private JButton				cancelBtn;
	private boolean 			cancelled;
	
	// Presents default generated name.
	ExperimentNameDialog()
	{
		this(null);
	}
	
	
	ExperimentNameDialog(String name)
	{
		setModal(true);
		JPanel north = new JPanel();
		north.add(new JLabel("Experiment Name:"));
		tf = new JTextField(10);
		if (name == null)
			name = Experiment.generateDefaultName();
		tf.setText(name);
		tf.addActionListener(this);
		north.add(tf);
		add(north, BorderLayout.NORTH);
		JPanel south = new JPanel();
		JButton applyBtn = new JButton("Apply");
		applyBtn.addActionListener(this);
		south.add(applyBtn);
		cancelBtn = new JButton("Cancel");
		cancelBtn.addActionListener(this);
		south.add(cancelBtn);
		add(south, BorderLayout.SOUTH);
		pack();
	}
	
	public void actionPerformed(ActionEvent e)
	{
		if (e.getSource() == cancelBtn)
		{
			cancelled = true;
			setVisible(false);
		}
		
		else
		{
			String newName = getExperimentName();
			if (!Experiment.nameIsAvailable(newName))
				JOptionPane.showMessageDialog(this, "Name is in use: " + newName);
			else
				setVisible(false);
		}
	}
	
	boolean wasCancelled()		
	{
		return cancelled;
	}
	
	
	String getExperimentName()	
	{ 
		return tf.getText().trim(); 
	}
}
