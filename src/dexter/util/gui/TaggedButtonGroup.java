package dexter.util.gui;

import javax.swing.*;
import javax.swing.border.*;
import java.util.*;
import java.awt.LayoutManager;
import java.awt.event.ItemListener;


public class TaggedButtonGroup<T> extends ButtonGroup
{
	private Vector<TaggedRadio<T>>			radios = new Vector<TaggedRadio<T>>();
	private Set<ItemListener>				listeners = new HashSet<ItemListener>();
	
	
	public TaggedButtonGroup()	{ }
	
	
	public TaggedButtonGroup(T[] tags)
	{
		Vector<T> tagsVec = new Vector<T>(tags.length);
		for (T tag: tags)
			tagsVec.add(tag);
		initFromCollection(tagsVec);
	}
	
	
	public TaggedButtonGroup(Collection<T> tags)
	{
		initFromCollection(tags);
	}
	
	
	private void initFromCollection(Collection<T> tags)
	{
		for (T tag: tags)
			add(new TaggedRadio<T>(tag));
	}
	
	
	public void add(TaggedRadio<T> addMe)
	{
		addMe.setSelected(radios.isEmpty());
		super.add(addMe);
		if (!radios.contains(addMe))	
			radios.add(addMe);
		for (ItemListener il: listeners)
			addMe.addItemListener(il);
	}
	
	
	public void remove(TaggedRadio<T> removeMe)
	{
		super.remove(removeMe);
		radios.remove(removeMe);
	}
	
	
	public JPanel buildPanel()
	{
		return buildPanel(null, null);
	}
	
	
	public JPanel buildPanel(ItemListener il)
	{
		return buildPanel(il, null);
	}
	
	
	public JPanel buildPanel(LayoutManager lom)
	{
		return buildPanel(null, lom);
	}
	
	
	public JPanel buildPanel(ItemListener il, LayoutManager lom)
	{
		JPanel pan = new JPanel();
		if (lom != null)
			pan.setLayout(lom);
		for (TaggedRadio<T> radio: radios)
		{
			if (il != null)
				radio.addItemListener(il);
			pan.add(radio);
		}
		return pan;
	}
	
	
	public TaggedRadio<T> getSelectedRadio()
	{
		for (TaggedRadio<T> radio: radios)
			if (radio.isSelected())
				return radio;
		return null;
	}
	
	
	public T getSelectedTag()
	{
		TaggedRadio<T> radio = getSelectedRadio();
		return (radio != null)  ?  radio.getTag()  :  null;
	}
	
	
	public void addItemListener(ItemListener il)
	{
		listeners.add(il);
		for (TaggedRadio<T> radio: radios)
			radio.addItemListener(il);
	}
	
	
	public void setSelectedTag(T tag)
	{
		TaggedRadio<T> wanted = null;
		for (TaggedRadio<T> radio: radios)
		{
			if (radio.getTag() == tag)
			{
				wanted = radio;
				break;
			}
		}
		
		assert wanted != null;
		wanted.setSelected(true);
	}
	
	
	public void setEnabled(boolean b)
	{
		for (TaggedRadio<T> radio: radios)
			radio.setEnabled(b);
	}
	
	
	public void setEnabled(T tag, boolean b)
	{
		for (TaggedRadio<T> radio: radios)
			if (radio.getTag() == tag)
				radio.setEnabled(b);
	}
	
	
	public boolean contains(TaggedRadio<?> radio)		{ return radios.contains(radio); }
	public Vector<TaggedRadio<T>> getRadios()			{ return new Vector<TaggedRadio<T>>(radios); }
}
