package dexter.util.gui;


import java.awt.*;
import java.awt.event.*;

import javax.swing.*;


public class OkWithContentDialog extends JDialog implements ActionListener
{
	private JButton 			okBtn;
	private JButton				optionalCancelBtn;
	private boolean				cancelled;
	private JPanel				bottomFlowPan;
	private Component			content;
	private boolean				terminateOnAnyClick;		// for debugging
	
	
	public OkWithContentDialog(Component content)
	{
		this(content, false);
	}
	
	
	public OkWithContentDialog(Component content, boolean addCancelBtn)
	{	
		setLayout(new BorderLayout());
		
		if (content != null)
		{
			add(content, BorderLayout.CENTER);
			this.content = content;
		}
		
		bottomFlowPan = new JPanel(); 
		okBtn = new JButton("Ok");
		okBtn.addActionListener(this);
		bottomFlowPan.add(okBtn);
		if (addCancelBtn)
		{
			optionalCancelBtn = new JButton("Cancel");
			optionalCancelBtn.addActionListener(this);
			bottomFlowPan.add(optionalCancelBtn);
		}
		add(bottomFlowPan, BorderLayout.SOUTH);
		pack();
	}
	
	
	public OkWithContentDialog()
	{
		this(null);
	}
	
	
	public OkWithContentDialog(boolean addCancelBtn)
	{
		this(null, addCancelBtn);
	}
	
	
	public String toString()
	{
		String s = "OkWithContentDialog:\n  content isa " + content.getClass().getName() + 
			",\n    pref=" + content.getPreferredSize() + ", actual=" + content.getSize() + 
			"\n   Control panel pref = " + bottomFlowPan.getPreferredSize() + ", actual = " + bottomFlowPan.getSize();
		return s;
	}
	
	
	public void actionPerformed(ActionEvent e)
	{
		if (terminateOnAnyClick)
			System.exit(0);
		
		if (e.getSource() == okBtn)
		{
				setVisible(false);
		}
		
		else if (e.getSource() == optionalCancelBtn)
		{
			cancelled = true;
			setVisible(false);
		}
	}
	

	public void setContent(Component content)
	{
		assert content != null  :  "Null content";
		add(content, BorderLayout.CENTER);
		this.content = content;
		pack();
	}
	
	
	public void addToBottomFlowPanel(Component addMe)
	{
		bottomFlowPan.add(addMe);
	}
	
	
	public void addToBottomFlowPanel(Component addMe, int position)
	{
		bottomFlowPan.add(addMe, position);
	}
	
	
	public void setOkButtonText(String s)
	{
		okBtn.setText(s);
		pack();
	}
	
	
	public boolean wasCancelled()			{ return cancelled;    		  }
	public boolean wasCanceled()			{ return cancelled;   		  }
	public void enableOkBtn(boolean b)		{ okBtn.setEnabled(b); 		  }
	public JPanel getBottomFlowPanel()		{ return bottomFlowPan;		  }
	public Component getContent()			{ return content;			  }
	public void setTerminateOnAnyClick()	{ terminateOnAnyClick = true; }
	public static void sop(Object x)		{ System.out.println(x); 	  }
}