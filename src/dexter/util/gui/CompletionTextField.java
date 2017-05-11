package dexter.util.gui;

import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import dexter.util.StringProductionGraph;


//
// Offers unique partial completion as a highlighted extension to the textfield's contents. User can accept
// by typing ENTER, or reject by typing other text (which will overwrite the selection). If multiple 
// extensions are possible and the number of those extensions is reasonable small, pops up a menu..
//


public class CompletionTextField extends JTextField implements ActionListener
{
    private final static String 		COMMIT_ACTION_NAME 			= "commit"; 
    private final static int			MAX_ORGANISMS_IN_POPUP		= 20;
    
    private StringProductionGraph		graph;
    private CompletionTextField			outerThis;
    private boolean						nextInsertionIsSelfGenerated;
    private Mode						mode;		// INSERT or COMPLETION
    private JPopupMenu					popup;
    
      

    private static enum Mode { INSERTION, COMPLETION };
    
    
    public CompletionTextField(int nCols, StringProductionGraph graph)
    {
    	super(nCols);
        
    	this.graph = graph;
    	outerThis = this;  
    	mode = Mode.INSERTION;

        getDocument().addDocumentListener(new DocLis());
        InputMap im = getInputMap();
        ActionMap am = getActionMap();
        im.put(KeyStroke.getKeyStroke("ENTER"), COMMIT_ACTION_NAME);
        am.put(COMMIT_ACTION_NAME, new CommitAction());
    }
    
    
    private class DocLis extends DocumentAdapter
    {
        public void insertUpdate(DocumentEvent ev) 
        {        	
        	// This method is called when the user types and when the completion task inserts
        	// text programattically. Only do completion in response to user typing.
        	if (nextInsertionIsSelfGenerated)
        	{
        		nextInsertionIsSelfGenerated = false;
        		return;
        	}

    		// If only 1 extension is possible, append and highlight it.
        	String prefix = outerThis.getText();
        	String extension = graph.produceWhileNoDecisions(prefix);
        	if (extension != null  &&  extension.length() > 0)
        	{
        		String replacement = prefix + extension;
        		SwingUtilities.invokeLater(new CompletionTask(prefix.length(), replacement));
        		return;
        	}
        	
        	// If reasonable few multiple extensions are possible, present them in a popup menu.
        	Vector<String> possibilities = graph.produce(prefix);
        	if (possibilities == null)
        		return;			// graph doesn't produce prefix
        	if (possibilities.size() > MAX_ORGANISMS_IN_POPUP)
        		return;			// too many extensions
        	popup = new JPopupMenu();
        	for (String s: possibilities)
        	{
        		TaggedMenuItem<String> mi = new TaggedMenuItem<String>(s);
        		mi.addActionListener(outerThis);
        		popup.add(mi);
        	}
        	popup.show(outerThis, 30, 10);
        }
    }
    
    
    public void actionPerformed(ActionEvent e)
    {
    	if (e.getSource() instanceof JMenuItem)
    	{
    		String replacement = ((TaggedMenuItem<String>)e.getSource()).getTag();
    		popup.setVisible(false);
    		String prefix = getText();
    		SwingUtilities.invokeLater(new CompletionTask(prefix.length(), replacement));
    	}
    }
    
    
    private class CompletionTask implements Runnable 
    {
    	private int 	prevLength;
    	private String 	replacement;
         
        CompletionTask(int prevLength, String replacement) 
        {
        	this.prevLength = prevLength;
            this.replacement = replacement;
        } 
         
        public void run() 
        {
        	nextInsertionIsSelfGenerated = true;
        	outerThis.setText(replacement);
        	outerThis.setCaretPosition(prevLength);
        	outerThis.moveCaretPosition(replacement.length());
            mode = Mode.COMPLETION;
        }
    }
     
    
    private class CommitAction extends AbstractAction 
    {
        public void actionPerformed(ActionEvent ev) 
        {
            if (mode == Mode.COMPLETION) 
            {
                int pos = outerThis.getSelectionEnd();
                outerThis.setCaretPosition(pos);
                mode = Mode.INSERTION;
            }
        }
    }
    
    
    public StringProductionGraph getGraph()
    {
    	return graph;
    }
    
    
	static void sop(Object x)			{ System.out.println(x); }
	
	
	public static void main(String[] args)
	{				
		try
		{
			StringProductionGraph graph = StringProductionGraph.forKEGGOrganisms();
			CompletionTextField tf = new CompletionTextField(25, graph);
			JPanel pan = new JPanel();
			pan.add(tf);
			JFrame frame = new JFrame();
			frame.add(pan, BorderLayout.NORTH);
			frame.pack();
			frame.setVisible(true);
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
