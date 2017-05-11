package dexter.view.cluster;


import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.Vector;

import javax.swing.*;

import dexter.util.gui.OkWithContentDialog;
import dexter.cluster.*;
import dexter.view.cluster.*;		// later won't need
import dexter.util.gui.*;


//
// T is the type of the node payloads: String for a graph that was parsed from a newick file, Gene
// for a tree that was constructed during the current session.
//
// For deployment, use subclass GeneTreeDialog.
//


public class TreeDialog<T> extends OkWithContentDialog implements ItemListener
{
	protected final static int						TREE_WIDTH_PIX		= 900;
	protected final static int						SPANE_HEIGHT_PIX	= 600;
	
	protected Node<T> 								root;
	protected TreePanel<T> 							treePan;
	protected JScrollPane							spane;
	protected JButton								saveBtn;
	protected JButton								deselectAllBtn;
	protected JButton								rerootBtn;
	protected JToggleButton							cutLineToggle;
	
	
	public TreeDialog(Node<T> root)
	{
		this(root, null);
	}
	
	
	public TreeDialog(Node<T> root, TreePanel<T> treePan)
	{
		this.root = root;
		this.treePan = treePan;
		
		// Tree panel in a vertical scrollpane.
		if (treePan == null)
			treePan = new TreePanel<T>(root, TREE_WIDTH_PIX);
		treePan.setOwner(this);
		spane = new JScrollPane(treePan, 
								JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, 
								JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		spane.setPreferredSize(new Dimension(spane.getPreferredSize().width, SPANE_HEIGHT_PIX));
		setContent(spane);
		
		// Custom controls. Reverse order is easiest.
		deselectAllBtn = new JButton("Deselect all");
		deselectAllBtn.addActionListener(this);
		addToBottomFlowPanel(deselectAllBtn, 0);
		saveBtn = new JButton("Save tree...");
		saveBtn.addActionListener(this);
		addToBottomFlowPanel(saveBtn, 0);
		rerootBtn = new JButton("Reroot");
		rerootBtn.setToolTipText("When 1 non-leaf node is selected, reroots at that node.");
		rerootBtn.addActionListener(this);
		rerootBtn.setEnabled(false);
		addToBottomFlowPanel(rerootBtn, 0);
		cutLineToggle = new JToggleButton("Cut line");
		cutLineToggle.addItemListener(this);
		addToBottomFlowPanel(cutLineToggle, 0);
		pack();
	}
	

	public Vector<Vector<T>> collectSelectedSubtreeLeavesBySubtree()
	{
		return treePan.collectSelectedSubtreeLeavesBySubtree();
	}
	
	
	public void actionPerformed(ActionEvent e)
	{
		if (e.getSource() == deselectAllBtn)
			treePan.deselectAll();
		else if (e.getSource() == saveBtn)
			save();
		else if (e.getSource() == rerootBtn)
			reroot();
		else
			super.actionPerformed(e);
	}
	
	
	private void save()
	{
		JFileChooser chooser = ClusterDialog.getFileChooser();
		if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION)
			return;
		File newickFile = chooser.getSelectedFile();
		if (!newickFile.getName().endsWith(".tre"))
			newickFile = new File(newickFile.getAbsolutePath() + ".tre");
		String newickString = root.toNewickStringForRoot();
		try
		{
			FileWriter fw = new FileWriter(newickFile);
			fw.write(newickString);
			fw.flush();
			fw.close();
		}
		catch (IOException x)
		{
			String err = "Couldn't save tree to file " + newickFile.getName() +": " + x.getMessage();
			JOptionPane.showMessageDialog(this, err);
		}
	}
	
	
	public void itemStateChanged(ItemEvent e)
	{
		treePan.setUseCutLine(cutLineToggle.isSelected());
	}
	
	
	TreePanel<T> getTreePanel()
	{
		return treePan;
	}
	
	
	// Called by the tree panel. Rerooting is allowed if exactly 1 node is selected, and that
	// node is intermediate (not root or leaf).
	void selectionChanged()
	{
		Vector<Node<T>> selNodes = treePan.getSelectedSubtrees();
		boolean canReroot = (selNodes.size() == 1);
		if (canReroot)
		{
			Node<T> selNode = selNodes.firstElement();
			if (selNode.isLeaf()  ||  selNode.isRoot())
				canReroot = false;
		}
		rerootBtn.setEnabled(canReroot);
	}
	
	
	// Reroots the tree, then installs a new tree panel.
	private void reroot()
	{
		// Reroot the tree.
		Vector<Node<T>> subtrees = treePan.getSelectedSubtrees();
		assert subtrees.size() == 1;
		Node<T> oldRoot = treePan.getRoot();
		Node<T> newRoot = subtrees.firstElement();
		assert newRoot != oldRoot;
		oldRoot.reroot(newRoot);
		assert newRoot.isRoot();
		assert !oldRoot.isRoot();
		
		// Replace the tree panel.
		TreePanel<T> newTreePan = buildNewTreePanelForRerooting(newRoot);
		newTreePan.setOwner(this);		
		JScrollPane newSpane = new JScrollPane(newTreePan, 
											   JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, 
											   JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		newSpane.setPreferredSize(spane.getPreferredSize());
		remove(spane);
		spane = newSpane;
		setContent(newSpane);		// packs
		
		// New tree has nothing selected.
		rerootBtn.setEnabled(false);
	}
	
	
	protected TreePanel buildNewTreePanelForRerooting(Node<T> newRoot)
	{
		return new TreePanel<T>(newRoot, TREE_WIDTH_PIX);
	}
	

	public static void main(String[] args)
	{
		try
		{
			File f = new File("data/Clusters/Croco_all_KEGGED.tre");
			NewickParser<String> parser = new NewickParser<String>(f, new NewickPayloadBuilderStringIdentity());
			Node<String> tree = parser.parse();
			TreeDialog<String> dia = new TreeDialog<String>(tree);
			dia.setVisible(true);
		}
		catch (Exception x)
		{
			sop("Stress: " + x.getMessage());
			x.printStackTrace();
		}
		sop("DONE");
	}
}
