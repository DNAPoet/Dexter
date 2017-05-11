package dexter.util.gui;

import java.awt.*;
import javax.swing.*;

import dexter.util.LocalMath;

import java.util.*;


//
// JTextField(String, nCols) ctor does a terrible job of estimating width. This class uses
// the metrics of the underlying font.
//

public class CorrectTextFieldWidthLayout extends FlowLayout

{
	private final static float				DFLT_EXPANSION	= 1.2f;
	
	private Map<JTextField, String>			tfToLongestAnticipatedText;
	private float							expansion;
	
	
	public CorrectTextFieldWidthLayout(Map<JTextField, String> tfToLongestAnticipatedText)
	{
		this(tfToLongestAnticipatedText, DFLT_EXPANSION);
	}	
	
	
	public CorrectTextFieldWidthLayout(Map<JTextField, String> tfToLongestAnticipatedText, float expansion)
	{
		this.tfToLongestAnticipatedText = tfToLongestAnticipatedText;
		if (expansion < 1f)
			expansion++;
		this.expansion = expansion;
	}
	

	private Dimension preferredTFSize(JTextField tf)
	{
		Dimension originalPref = tf.getPreferredSize();
		if (!tfToLongestAnticipatedText.containsKey(tf))
			return originalPref;
		
		FontMetrics fm = tf.getFontMetrics(tf.getFont());
		int w = fm.stringWidth(tfToLongestAnticipatedText.get(tf));
		w = Math.round(w * expansion);
		
		return new Dimension(w, originalPref.height);
	}
	
	
	// Call this before superclass does anything involving preferred child sizes.
	private void setTFPreferredSizes(Container cont)
	{
		for (Component child: cont.getComponents())
		{
			if (tfToLongestAnticipatedText.containsKey(child))
			{
				Dimension pref = preferredTFSize((JTextField)child);
				child.setPreferredSize(pref);
			}
		}
	}
	
	
	public Dimension preferredLayoutSize(Container c)
	{
		setTFPreferredSizes(c);
		return super.preferredLayoutSize(c);
	}
	
	
	public void layoutContainer(Container c)
	{
		setTFPreferredSizes(c);
		super.layoutContainer(c);
	}
}
