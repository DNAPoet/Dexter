package dexter.util.gui;

import java.awt.*;


// Implements all methods.
public class LayoutAdapter implements LayoutManager
{
    public void addLayoutComponent(String name, Component comp)     { }
    public void removeLayoutComponent(Component comp)               { }
    public void layoutContainer(Container parent)                   { }
    public Dimension minimumLayoutSize(Container c)                 { return preferredLayoutSize(c); }
    public Dimension preferredLayoutSize(Container parent)          { return null; }
    static void sop(Object x)										{ System.out.println(x); }
}
