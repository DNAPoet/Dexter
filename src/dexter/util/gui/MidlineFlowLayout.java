package dexter.util.gui;

import java.awt.*;

import dexter.util.LocalMath;


public class MidlineFlowLayout extends LayoutAdapter
{
    private final static int    DFLT_MARGIN 	= 10;
    private final static int    DFLT_GAP    	=  5;
    private final static int    DFLT_ALIGN   	= FlowLayout.CENTER;

    private int                 margin;     	// child to container
    private int                 gap;         	// child to child
    private int 				align;			// one of FlowLayout.LEFT/RIGHT/CENTER


    public MidlineFlowLayout()
    {
        this(DFLT_MARGIN, DFLT_GAP);
    }


    public MidlineFlowLayout(int margin, int gap)
    {
        this.margin = (margin < 0)  ?  DFLT_MARGIN  :  margin;
        this.gap = (gap < 0)  ?  DFLT_GAP  :  gap;
        align = DFLT_ALIGN;
    }
    
    
    public void setAlign(int align)
    {
    	if (align != FlowLayout.LEFT  &&  align != FlowLayout.RIGHT  &&  align != FlowLayout.CENTER)
    		throw new IllegalArgumentException("Bad align");
    	this.align = align;
    }


    public Dimension preferredLayoutSize(Container cont)
    {
        int w = margin;
        for (int i=0; i<cont.getComponentCount(); i++)
        {
            Component child = cont.getComponent(i);
            w += child.getPreferredSize().width;
            w += gap;
        }
        w -= gap;
        w += margin;

        int h = maxChildHeight(cont) + 2*margin;
        return new Dimension(w, h);
    }


    public void layoutContainer(Container cont)
    {
        int clusterW = preferredLayoutSize(cont).width - 2*margin;
        int x = -1;
        if (align == FlowLayout.CENTER)
        	x = (cont.getWidth() - clusterW) / 2;						
        else if (align == FlowLayout.LEFT)
        	x = margin;
        else if (align == FlowLayout.RIGHT)
        	x = cont.getWidth() - margin - clusterW;
        int containerH = cont.getHeight();
        for (int i=0; i<cont.getComponentCount(); i++)
        {
            Component child = cont.getComponent(i);
            Dimension pref = child.getPreferredSize();
            child.setSize(pref);
            int y = (containerH - pref.height) / 2;
            child.setLocation(x, y);
            x += pref.width + gap;
        }
    }


    private int maxChildHeight(Container cont)
    {
        int hMax = 0;
        for (int i=0; i<cont.getComponentCount(); i++)
        {
            int h = cont.getComponent(i).getPreferredSize().height;
            hMax = Math.max(h, hMax);
        }
        return hMax;
    }
}
