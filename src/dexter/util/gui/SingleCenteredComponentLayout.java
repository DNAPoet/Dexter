package dexter.util.gui;

import java.awt.*;


public class SingleCenteredComponentLayout extends LayoutAdapter
{
	private int				horizMargin;
	private int				vertMargin;
	
	
	public SingleCenteredComponentLayout(int horizMargin, int vertMargin)
	{
		this.horizMargin = horizMargin;
		this.vertMargin = vertMargin;
	}
	
	
	public Dimension preferredLayoutSize(Container parent)
	{
		assert parent.getComponentCount() == 1;
		
		Component child = parent.getComponent(0);
		Dimension childPref = child.getPreferredSize();
		return new Dimension(childPref.width + 2*horizMargin, childPref.height + 2*vertMargin);
	}

    public void layoutContainer(Container parent)                   
    {
		assert parent.getComponentCount() == 1;
		
		Component child = parent.getComponent(0);
		child.setSize(child.getPreferredSize());
		child.setLocation(horizMargin, vertMargin);
    }

}
