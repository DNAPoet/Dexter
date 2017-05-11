package dexter.util.gui;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;


//
// Has to extend JPanel rather than JCheckBox because some looks-n-feels don't handle borders. Uses
// delegation to look as much as possible like a JCheckBox.
//
// When using as one of a group, event manager can miss mouse-entered and mouse-exited events if the
// instances are close together. Use a VariableBorderCheckboxManager or, if doing the same sort of thing
// yourself, call setExternallyManaged() to impose your own bordering externally.
//

public class VariableBorderColorTaggedCheckBox<T> extends JPanel
{
	private static int				nextSN;
	
	private int						sn = nextSN++;
	private TaggedCheckBox<T>		cbox;
	private Border					mouseInBorder;
	private Border					mouseOutBorder;
	private boolean					verbose;
	private boolean					externallyManaged;
	
	
	
    public VariableBorderColorTaggedCheckBox(T tag, Color mouseInBorderColor, Color mouseOutBorderColor)
    {
    	this(tag, null, mouseInBorderColor, mouseOutBorderColor);
    }
    
    
    public VariableBorderColorTaggedCheckBox(T tag, 
    									     String label, 
    									     Color mouseInBorderColor, 
    									     Color mouseOutBorderColor)
    {
    	setLayout(new SingleCenteredComponentLayout(4, 2));
        cbox = (label == null)  ?  new TaggedCheckBox<T>(tag)  :  new TaggedCheckBox<T>(tag, label);
        cbox.addMouseListener(new CheckboxMouseListener());
    	add(cbox);
    	
    	mouseInBorder = BorderFactory.createLineBorder(mouseInBorderColor, 2);
    	mouseOutBorder = BorderFactory.createLineBorder(mouseOutBorderColor, 1);
        setBorder(mouseOutBorder);  
    	
        addMouseListener(new OuterMouseListener());      
    }


    // For this component.
	private class OuterMouseListener extends MouseAdapter
	{
		public void mouseEntered(MouseEvent e)
		{
			if (verbose)
				sop("mouse entered " + sn);
			if (externallyManaged)
				return;
			setBorder(mouseInBorder);
		}
		
		public void mouseExited(MouseEvent e)
		{
			if (verbose)
				sop("mouse exited " + sn);
			if (externallyManaged)
				return;
			setBorder(mouseOutBorder);
		}
	}


    // For the checkbox, because when the mouse enters the checkbox it exits this component.
	private class CheckboxMouseListener extends MouseAdapter
	{
		public void mouseEntered(MouseEvent e)
		{
			if (verbose)
				sop("  mouse entered cbox" + sn);
			if (externallyManaged)
				return;
			setBorder(mouseInBorder);
		}
		
		public void mouseExited(MouseEvent e)
		{
			if (verbose)
				sop("mouse exited cbox" + sn);
		}
	}
	
	
	public T getTag()
	{
		return cbox.getTag(); 
	}
	
	
	public void setVerbose(boolean b)
	{
		verbose = b;
	}
	
	
	public void setExternallyManaged(MouseListener manager)
	{
		externallyManaged = true;
		addMouseListener(manager);
		cbox.addMouseListener(manager);
	}
	
	
	// Sent by a manager.
	public void setBorderForMouseState(boolean mouseIn)
	{
		setBorder(mouseIn ? mouseInBorder : mouseOutBorder);
	}
	
	
	public TaggedCheckBox<T> getCheckBox()
	{
		return cbox;
	}
	
	
	public boolean isSelected()
	{
		return cbox.isSelected();
	}
	
	
	public void addItemListener(ItemListener il)
	{
		cbox.addItemListener(il);
	}
	
	
	public void removeItemListener(ItemListener il)
	{
		cbox.removeItemListener(il);
	}
	
	
	public void setCheckboxForeground(Color fg)
	{
		assert cbox != null;
		cbox.setForeground(fg);
	}
	
	
	public void setEnabled(boolean b)
	{
		cbox.setEnabled(b);
	}
	
	
	public void setSelected(boolean b)
	{
		cbox.setSelected(b);
	}
	
	
	public void paintComponent(Graphics g)
	{
		g.setColor(getBackground());
		g.fillRect(0, 0, getWidth(), getHeight());
	}
    
    
    static void sop(Object x)
    {
    	System.out.println(x);
    }
	
	
	public static void main(String[] args)
	{
		sop("START");
		VariableBorderColorTaggedCheckBox<String> cbox = 
			new VariableBorderColorTaggedCheckBox<String>("Xxxxxxxx", Color.RED, Color.BLUE);
		JPanel pan = new JPanel();
		pan.add(cbox);
		JFrame frame = new JFrame();
		frame.add(pan, BorderLayout.NORTH);
		frame.pack();
		frame.setVisible(true);
	}
}
