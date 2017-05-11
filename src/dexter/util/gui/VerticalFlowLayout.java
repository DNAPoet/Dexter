package dexter.util.gui;

import java.awt.*;

import dexter.util.LocalMath;


public class VerticalFlowLayout extends LayoutAdapter
{
    private final static int    DFLT_MARGIN = 10;
    private final static int    DFLT_GAP    =  5;

    private int                 vMargin;     // child to container
    private int                 hMargin;     // child to container
    private int                 gap;         // child to child
    private boolean             honorAllPreferredSizes;
    private float               hAlignment;
    private float               vAlignment;


    public VerticalFlowLayout()
    {
        this(DFLT_MARGIN, DFLT_MARGIN, DFLT_GAP);
    }


    public VerticalFlowLayout(int gap)
    {
        this(DFLT_MARGIN, DFLT_MARGIN, gap);
    }


    public VerticalFlowLayout(int hMargin, int vMargin, int gap)
    {
        this(hMargin, vMargin, gap, false);
    }


    public VerticalFlowLayout(int hMargin, int vMargin, int gap, boolean honorAllPreferredSizes)
    {
        this.hMargin = hMargin;
        this.vMargin = vMargin;
        this.gap = gap;
        this.honorAllPreferredSizes = honorAllPreferredSizes;

        hAlignment = Component.CENTER_ALIGNMENT;
    }


    public Dimension preferredLayoutSize(Container cont)
    {
        int h = vMargin;
        for (int i=0; i<cont.getComponentCount(); i++)
        {
            Component child = cont.getComponent(i);
            h += child.getPreferredSize().height;
            h += gap;
        }
        h -= gap;
        h += vMargin;

        int w = maxChildWidth(cont) + 2*hMargin;
        return new Dimension(w, h);
    }


    public void layoutContainer(Container cont)
    {
        int maxChildWidth = maxChildWidth(cont);
        int containerWidth = cont.getWidth();
        int y = -99999;
        if (vAlignment == Component.TOP_ALIGNMENT)
            y = vMargin;
        else if (vAlignment == Component.CENTER_ALIGNMENT)
            y = (cont.getSize().height - preferredLayoutSize(cont).height) / 2;
        else if (vAlignment == Component.BOTTOM_ALIGNMENT)
            y = cont.getSize().height - preferredLayoutSize(cont).height;
        for (int i=0; i<cont.getComponentCount(); i++)
        {
            Component child = cont.getComponent(i);
            Dimension childPrefSize = child.getPreferredSize();
            int childWidth = honorAllPreferredSizes ? childPrefSize.width : maxChildWidth;
            child.setSize(childWidth, childPrefSize.height);
            int x = -1;
            if (hAlignment == Component.LEFT_ALIGNMENT)
                x = 4;
            else if (hAlignment == Component.CENTER_ALIGNMENT)
                x = (containerWidth - childWidth) / 2;
            else if (hAlignment == Component.RIGHT_ALIGNMENT)
                x = containerWidth - childWidth - 4;
            else
                assert false;
            child.setLocation(x, y);
            y += childPrefSize.height + gap;
        }
    }


    private int maxChildWidth(Container cont)
    {
        int wMax = 0;
        for (int i=0; i<cont.getComponentCount(); i++)
        {
            int w = cont.getComponent(i).getPreferredSize().width;
            wMax = Math.max(w, wMax);
        }
        return wMax;
    }


    // If false, all children are widest preferred width. If true, all children
    // are preferred width.
    public void setHonorAllPreferredSizes(boolean b)
    {
        honorAllPreferredSizes = b;
    }


    public void setGap(int gap)
    {
        this.gap = gap;
    }


    public void setMargin(int margin)
    {
        this.vMargin = this.hMargin = margin;
    }
    
    
    public void setHorizontalMargin(int hMargin)
    {
    	this.hMargin = hMargin;
    }
    
    
    public void setVerticalMargin(int vMargin)
    {
    	this.vMargin = vMargin;
    }
    
    
    public void setHorizontalAndVerticalMargin(int hMargin, int vMargin)
    {
    	this.hMargin = hMargin;
    	this.vMargin = vMargin;
    }


    // Must be Component.{LEFT_ALIGNMENT, CENTER_ALIGNMENT, RIGHT_ALIGNMENT}.
    public void setHorizontalAlignment(float hAlignment)
    {
        if (hAlignment != Component.LEFT_ALIGNMENT    &&
            hAlignment != Component.CENTER_ALIGNMENT  &&
            hAlignment != Component.RIGHT_ALIGNMENT)
        {
            String s = "alignment must be one of Component.{LEFT, CENTER, RIGHT}_ALIGNMENT.";
            throw new IllegalArgumentException(s);
        }
        this.hAlignment = hAlignment;
    }


    // Must be Component.{TOP_ALIGNMENT, CENTER_ALIGNMENT, BOTTOM_ALIGNMENT}.
    public void setVerticalAlignment(float vAlignment)
    {
        if (vAlignment != Component.TOP_ALIGNMENT     &&
            vAlignment != Component.CENTER_ALIGNMENT  &&
            vAlignment != Component.BOTTOM_ALIGNMENT)
        {
            String s = "alignment must be one of Component.{TOP, CENTER, BOTTOM}_ALIGNMENT.";
            throw new IllegalArgumentException(s);
        }
        this.vAlignment = vAlignment;
    }
}