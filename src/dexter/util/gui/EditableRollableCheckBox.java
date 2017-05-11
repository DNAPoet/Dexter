package dexter.util.gui;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;

import javax.swing.*;


public class EditableRollableCheckBox<T> extends RollableCheckBox<T>
{
	public EditableRollableCheckBox(T tag, Color lineColor, boolean selected)
	{
		this(tag, tag.toString(), lineColor, selected);
	}

	
	public EditableRollableCheckBox(T tag, String text, Color lineColor, boolean selected)
	{
		this(tag, text, lineColor, 1, selected);
	}
	
	
	public EditableRollableCheckBox(T tag, String text, Color lineColor, int lineThickness, boolean selected)
	{
		super(tag, text, lineColor, lineThickness, selected);
		setToolTipText("Left-click to sel/unsel, right-click to edit");
	}
	
	
	// Superclass.mouseClicked() calls this. Superclass version does nothing.
	public void handleMiddleOrRightMouseClick(MouseEvent e)
	{
		Editor ed = new Editor();
		ed.setVisible(true); 		// modal
		if (ed.wasCancelled())
			return;
		setText(ed.getNewText());
	}
	
	
	private class Editor extends OkWithContentDialog
	{
		private JTextField			tf;
		
		Editor()
		{
			super(null, true);		// no content yet, add CANCEL button
			setTitle("Edit displayed name for " + getTag());
			setModal(true);
			tf = new JTextField(getText(), 25);
			tf.addActionListener(this);
			JPanel pan = new JPanel();
			pan.add(tf);
			setContent(pan);
		}
		
		String getNewText()
		{
			return tf.getText().trim();
		}
		
		public void actionPerformed(ActionEvent e)
		{
			if (e.getSource() == tf)
				setVisible(false);
			else
				super.actionPerformed(e);
		}
	}
}
