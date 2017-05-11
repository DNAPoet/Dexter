package dexter.util.gui;

import java.awt.*;
import javax.swing.JPanel;


public class SolidBGPanel extends JPanel
{
	public SolidBGPanel(Color bg)
	{
		setBackground(bg);
		setOpaque(true);
	}
	
	
	public SolidBGPanel(Color bg, LayoutManager lom)
	{
		super(lom);
		setBackground(bg);
		setOpaque(true);
	}
	
	
	public SolidBGPanel(LayoutManager lom)
	{
		setLayout(lom);
		setOpaque(true);
	}
	
	
	public void paintComponent(Graphics g)
	{
		g.setColor(getBackground());
		g.fillRect(0, 0, getWidth(), getHeight());
	}
}
