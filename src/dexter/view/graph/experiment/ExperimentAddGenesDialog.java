package dexter.view.graph.experiment;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.*;
import dexter.model.*;
import dexter.util.gui.*;


class ExperimentAddGenesDialog extends OkWithContentDialog
{	
	private AddBy				addCriterion;
	
	
	private ExperimentAddGenesDialog(AbstractGeneSelectionPanel content)
	{
		super(content);
		setModal(true);
		setTitle("Add genes by " + content.getAddBy());
	}
}
