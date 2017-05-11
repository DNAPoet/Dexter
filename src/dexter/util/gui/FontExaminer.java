package dexter.util.gui;

import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;


//
// Checkmark = '\u2714', xmark = '\u2718', clock = '\u231a'.
//


public class FontExaminer extends JFrame implements ActionListener
{
	private final static Font	MAIN_FONT	= new Font("Serif", Font.PLAIN, 14);
	
	private JButton				quitBtn;
	private Vector<Strip>		strips;
	private char				theChar = '\u231a';
	private String				theString;
	
	
	FontExaminer(char theChar)
	{
	    Font[] fonts = GraphicsEnvironment.getLocalGraphicsEnvironment().getAllFonts();
	    Set<String> families = new TreeSet<String>();
	    for (Font font: fonts)
	    	families.add(font.getFamily());
	    
	    this.theChar = theChar;
	    theString = "";
	    for (int i=0; i<5; i++)
	    	theString += theChar;
	    
	    JPanel controls = new JPanel();
	    quitBtn = new JButton("Quit");
	    quitBtn.addActionListener(this);
	    controls.add(quitBtn);
	    add(controls, BorderLayout.SOUTH);
	    
	    strips = new Vector<Strip>();
	    JPanel mainPan = new JPanel(new GridLayout(0, 1));
	    for (String fam: families)
	    {
	    	Font font = new Font(fam, Font.PLAIN, 14);
	    	Strip strip = new Strip(font);
	    	strips.add(strip);
	    	mainPan.add(strip);
	    }
	    JScrollPane spane = 
	    	new JScrollPane(mainPan, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
	    add(spane, BorderLayout.CENTER);
	    
	    pack();
	}
	
	
	private class Strip extends JPanel
	{
		private Font		font;
		
		Strip(Font font)
		{
			this.font = font;
			setPreferredSize(new Dimension(500, 25));
		}
		
		public void paintComponent(Graphics g)
		{
			g.setColor(Color.BLACK);
			g.setFont(MAIN_FONT);
			g.drawString(font.getName(), 3, getHeight()-3);
			
			g.setColor(Color.RED);
			g.setFont(font);
			g.drawString(theString, 100, getHeight()-3);
		}
	}
	
	
	public void actionPerformed(ActionEvent e) 
	{
		if (e.getSource() == quitBtn)
			System.exit(0);
	}
	
	
	static void sop(Object x)			{ System.out.println(x); }
	
	
	public static void main(String[] args)
	{
		(new FontExaminer('\u231a')).setVisible(true);
	}
}
