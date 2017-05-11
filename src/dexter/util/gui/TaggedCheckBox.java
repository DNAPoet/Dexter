package dexter.util.gui;

import javax.swing.*;


public class TaggedCheckBox<T> extends JCheckBox
{
    private T       tag;


    public TaggedCheckBox(T tag)
    {
        super(tag.toString());
        this.tag = tag;
    }
    
    
    public TaggedCheckBox(T tag, String label)
    {
    	super(label);
    	this.tag = tag;
    }


    public T getTag()
    {
        return tag;
    }
}
