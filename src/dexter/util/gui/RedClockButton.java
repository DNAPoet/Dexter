package dexter.util.gui;

import java.awt.*;
import javax.swing.*;


//
// Not all platforms support a font containing the clock glyph.
//


public class RedClockButton extends JButton
{
	public RedClockButton(String title)
	{
		super(title);
		
		if (!RedClockIcon.supportedOnThisPlatform())
			return;
		
		setIcon(new RedClockIcon());
		Dimension pref = getPreferredSize();
		setIconTextGap(-10);	// ?WTF? Probably very plaf-dependent
		setPreferredSize(new Dimension(pref.width+22, pref.height));
	}
	
	
	public static void main(String[] args)
	{
		JPanel pan = new JPanel();
		pan.add(new RedClockButton("ABC"));
		JFrame frame = new JFrame();
		frame.add(pan, BorderLayout.NORTH);
		frame.pack();
		frame.setVisible(true);
	}
}
