package dexter.util.gui;

import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;


//
// T is the type of the checkbox tags.
//


public class DynamicVerticalCheckboxHolder<T> extends JPanel
{
	private Vector<TaggedCheckBox<T>>		cboxes;
	private Set<ItemListener>				itemListeners;
	
	
	public DynamicVerticalCheckboxHolder(Collection<T> tags)
	{
		this(tags, null);
	}
	
	
	public DynamicVerticalCheckboxHolder(Collection<T> tags, Collection<String> labels)
	{
		assert labels == null  ||  tags.size() == labels.size();
		
		VerticalFlowLayout lom = new VerticalFlowLayout();
		lom.setVerticalAlignment(Component.TOP_ALIGNMENT);
		setLayout(lom);
		cboxes = new Vector<TaggedCheckBox<T>>();
		Vector<String> labelsVec = null;
		if (labels != null)
			labelsVec = new Vector<String>(labels);	
		int n = 0;
		for (T tag: tags)
		{
			TaggedCheckBox<T> cbox = (labels == null)  ? 
					new TaggedCheckBox<T>(tag)  :
					new TaggedCheckBox<T>(tag, labelsVec.get(n++))	;
			cboxes.add(cbox);
			add(cbox);
		}
		
		itemListeners = new HashSet<ItemListener>();
	}
	
	
	public DynamicVerticalCheckboxHolder()
	{
		this(new Vector<T>());
	}
	
	
	public void setToolTipText(T tag, String tttext)
	{
		for (TaggedCheckBox<T> cbox: cboxes)
		{
			if (cbox.getTag() == tag)
			{
				cbox.setToolTipText(tttext);
				return;
			}
		}
		
		assert false : "No such tag: " + tag;
	}
	
	
	public void addItemListener(ItemListener il)
	{
		itemListeners.add(il);
		for (TaggedCheckBox<T> cbox: cboxes)
			cbox.addItemListener(il);
	}
	
	
	public void removeItemListener(ItemListener il)
	{
		itemListeners.remove(il);
		for (TaggedCheckBox<T> cbox: cboxes)
			cbox.removeItemListener(il);
	}
	
	
	public Vector<T> getSelectedTags()
	{
		Vector<T> ret = new Vector<T>();
		for (TaggedCheckBox<T> cbox: cboxes)
			if (cbox.isSelected())
				ret.add(cbox.getTag());
		return ret;
	}
	
	
	public TaggedCheckBox<T> getCheckBox(T tag)
	{
		for (TaggedCheckBox<T> cbox: cboxes)
			if (cbox.getTag().equals(tag))
				return cbox;
		return null;
	}
	
	
	public void addTag(T tag)
	{
		Set<T> tags = new HashSet<T>();
		tags.add(tag);
		addTags(tags);
	}
	
	
	public void addTags(Collection<T> tags)
	{
		for (T tag: tags)
		{
			TaggedCheckBox<T> cbox = new TaggedCheckBox<T>(tag);
			for (ItemListener il: itemListeners)
				cbox.addItemListener(il);
			add(cbox);
			cboxes.add(cbox);
		}
		validate();
	}
	
	
	public void addTag(T tag, String label)
	{
		LinkedHashMap<T, String> tagToLabel = new LinkedHashMap<T, String>();
		tagToLabel.put(tag, label);
		addTags(tagToLabel);
	}
	
	
	public void addTags(Map<T, String> tagToLabel)
	{
		for (T tag: tagToLabel.keySet())
		{
			TaggedCheckBox<T> cbox = new TaggedCheckBox<T>(tag, tagToLabel.get(tag));
			for (ItemListener il: itemListeners)
				cbox.addItemListener(il);
			add(cbox);
			cboxes.add(cbox);
		}
		validate();
	}
	
	
	public void removeTag(T tag)
	{
		Set<T> tags = new HashSet<T>();
		tags.add(tag);
		removeTags(tags);
	}
	
	
	public void removeTags(Collection<T> tags)
	{
		for (T tag: tags)
		{
			TaggedCheckBox<T> cbox = getCheckBox(tag);
			cboxes.remove(cbox);
			remove(cbox);
		}
		validate();
	}
	
	
	public Vector<T> getTags()
	{
		Vector<T> tags = new Vector<T>();
		for (TaggedCheckBox<T> cbox: cboxes)
			tags.add(cbox.getTag());
		return tags;
	}
	
	
	public void setEnabled(boolean b)
	{
		for (TaggedCheckBox<T> cbox: cboxes)
			cbox.setEnabled(b);
	}
	
	
	public void setSelected(T tag, boolean selected)
	{
		TaggedCheckBox<T> cbox = getCheckBox(tag);
		for (ItemListener il: itemListeners)
			cbox.removeItemListener(il);
		cbox.setSelected(selected);
		for (ItemListener il: itemListeners)
			cbox.addItemListener(il);
	}
	
	
	public void setForegroundForTag(Color fg, T tag)
	{
		getCheckBox(tag).setForeground(fg);
	}
	
	
	public Color getForegroundForTag(T tag)
	{
		TaggedCheckBox<T> cbox = getCheckBox(tag);
		return cbox.getForeground();
	}
	
	
	private class TesterFrame extends JFrame implements ActionListener, ItemListener
	{
		private DynamicVerticalCheckboxHolder<T> 		holder;
		private JButton									moreBtn;
		private JButton									fewerBtn;
		private int										nextIndex;
		
		TesterFrame(DynamicVerticalCheckboxHolder<T> holder)
		{
			this.holder = holder;
			holder.addItemListener(this);
			add(holder, BorderLayout.CENTER);
			JPanel pan = new JPanel();
			fewerBtn = new JButton("-");
			fewerBtn.addActionListener(this);
			pan.add(fewerBtn);
			moreBtn = new JButton("+");
			moreBtn.addActionListener(this);
			pan.add(moreBtn);
			add(pan, BorderLayout.NORTH);
			pack();
			nextIndex = 100;
		}
		
		public void itemStateChanged(ItemEvent e) 
		{
			sop("ISC -----------");
			for (T tag: holder.getSelectedTags())
				sop(tag);
		}

		public void actionPerformed(ActionEvent e) 
		{
			if (e.getSource() == fewerBtn)
			{
				holder.removeTag(holder.getTags().firstElement());
			}
			else
			{
				String s = "ADDED_" + nextIndex++;
				holder.addTag((T)s);
			}
		}		
	}  // End of inner class TesterFrame
	
	
	public JFrame buildTesterFrame()
	{
		return new TesterFrame(this);
	}
	
	
	static void sop(Object x)		{ System.out.println(x); }
	
	
	public static void main(String[] args)
	{				
		try
		{
			Vector<String> tags = new Vector<String>();
			for (int i=1; i<5; i++)
				tags.add("TAGTAGTAGTAGTAGTAGTAGTAGTAGTAGTAGTAGTAGTAGTAGTAGTAG" + i);
			DynamicVerticalCheckboxHolder<String> holder = new DynamicVerticalCheckboxHolder<String>(tags);
			holder.buildTesterFrame().setVisible(true);
		}
		catch (Exception x)
		{
			sop("Stress: " + x.getMessage());
			x.printStackTrace();
		}
		finally
		{
			sop("DONE");
		}
	}
}
