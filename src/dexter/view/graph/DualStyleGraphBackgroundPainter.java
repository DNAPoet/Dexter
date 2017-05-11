package dexter.view.graph;

import java.awt.*;
import java.io.File;
import javax.swing.*;
import java.util.*;
import dexter.model.SessionModel;
import dexter.util.*;
import dexter.util.gui.Paintable;


class DualStyleGraphBackgroundPainter implements Paintable
{
	private Vector<GraphBackgroundPainter>		painters;
	
	
	DualStyleGraphBackgroundPainter(GraphBackgroundModel backgroundModel, Rectangle bounds, float hPixPerHour)
	{
		painters = new Vector<GraphBackgroundPainter>();
		Rectangle halfBounds = new Rectangle(bounds);
		halfBounds.height /= 2;
		for (GraphBackgroundStyle style: GraphBackgroundStyle.values())
		{
			assert backgroundModel.getUsesStyle(style);
			painters.add(new GraphBackgroundPainter(style, backgroundModel, halfBounds, hPixPerHour));
			halfBounds.y += halfBounds.height;
		}
	}
	
	
	public void paint(Graphics g)
	{
		for (GraphBackgroundPainter subPainter: painters)
			subPainter.paint(g);
	}
	
	
	static void sop(Object x)			{ System.out.println(x); }
	
	
	private static class TestPanel extends JPanel
	{
		DualStyleGraphBackgroundPainter		painter;
		Rectangle							painterBounds;
		
		TestPanel()
		{
			setOpaque(true);
			setPreferredSize(new Dimension(800, 500));			
			File sessionSerf = new File("data/Sessions/TestSession.dex");
			try
			{
				SessionModel session = new SessionModel(sessionSerf);
				GraphBackgroundModel backgroundModel = session.getGraphBackgroundModel();
				painterBounds = new Rectangle(100, 100, 600, 300);
				painter = new DualStyleGraphBackgroundPainter(backgroundModel, painterBounds, 15);
			}
			catch (Exception x) { }
		}
		
		public void paintComponent(Graphics g)
		{
			g.setColor(Color.WHITE);
			g.fillRect(0, 0, 2222, 1111);
			painter.paint(g);
			
			g.setColor(Color.RED);
			((Graphics2D)g).draw(painterBounds);
		}
	}
	
	
	public static void main(String[] args)
	{
		JFrame frame = new JFrame();
		frame.add(new TestPanel(), BorderLayout.CENTER);
		frame.pack();
		frame.setVisible(true);
	}
}
