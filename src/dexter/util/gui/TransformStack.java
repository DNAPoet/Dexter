package dexter.util.gui;

import java.util.Stack;
import java.awt.*;
import java.awt.geom.*;


public class TransformStack extends Stack<AffineTransform>
{
	public void push(Graphics g)
	{
		Graphics2D g2 = (Graphics2D)g;
		push(g2.getTransform());
	}
	
	
	public void pop(Graphics g)
	{
		Graphics2D g2 = (Graphics2D)g;
		g2.setTransform(pop());
	}
}
