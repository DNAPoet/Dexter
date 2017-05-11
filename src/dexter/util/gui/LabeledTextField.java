package dexter.util.gui;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;


public class LabeledTextField extends JPanel
{
	private JLabel				label;
	private JTextField			tf;
	
	
	public LabeledTextField(String labelValue)
	{
		this(labelValue, "");
	}
	
	
	public LabeledTextField(String labelValue, String tfValue)
	{
		setLayout(new BorderLayout());
		label = new JLabel(labelValue);
		add(label, BorderLayout.WEST);
		tf = new JTextField(tfValue);
		add(tf, BorderLayout.CENTER);
	}
	
	
	// Useful if tf is a custom subclass of JTextField.
	public LabeledTextField(String labelValue, JTextField tf)
	{
		setLayout(new BorderLayout());
		label = new JLabel(labelValue);
		add(label, BorderLayout.WEST);
		this.tf = tf;
		add(tf, BorderLayout.CENTER);
	}
	
	
	public void addActionListener(ActionListener al)
	{
		tf.addActionListener(al);
	}
	
	
	public String getText()
	{
		return tf.getText();
	}
	
	
	public void setText(String s)
	{
		tf.setText(s);
	}
	
	
	public void setEnabled(boolean b)
	{
		tf.setEnabled(b);
		label.setForeground(b ? Color.BLACK : Color.LIGHT_GRAY);
	}
}
