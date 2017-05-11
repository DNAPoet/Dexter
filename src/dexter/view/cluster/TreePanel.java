package dexter.view.cluster;


import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import java.util.*;

import dexter.VisualConstants;
import dexter.model.Gene;
import dexter.util.gui.*;
import dexter.cluster.*;


//
// T is the payload type of the nodes.
//


public class TreePanel<T> extends JPanel implements ItemListener, VisualConstants
{
	private final static int				PREF_W					= 550;
	private final static int				PREF_H					= 750;
	private final static Font				FONT 					= new Font("SansSerif", Font.PLAIN, 14);
	private final static int				X_LEAF_NODE_TO_TEXT		=  15;
	private final static int				Y_LEAF_NODE_TO_BASELINE	=   6;
	private final static int				ROOT_HANDLE_LEN_PIX		=  23;
	private final static int				TOP_BASELINE			=  20;
	private final static int				DELTA_BASELINE			=  33;
	private final static int				BOTTOM_MARGIN			=   4;
	private final static int				LEFT_MARGIN_INCL_HANDLE	=  18;
	private final static int				RIGHT_MARGIN_INCL_TEXT	= 154;	
	private final static Color[]			CLUSTER_COLORS			= 	
	{
		Color.BLUE, 
		BRICK_RED,
		DARK_GREEN,
		PURPLE,
		Color.RED,
		Color.ORANGE,
		Color.MAGENTA
	};
	private final static Color				HAIRLINE_COLOR			= Color.BLUE;
	private final static int				MIN_HAIRLINE_XPIX		= LEFT_MARGIN_INCL_HANDLE / 2;
	
	private TreeDialog<T>					owner;						// null is ok
	private Node<T>							root;
	private float							maxNodeDepth;
	private int								graphWidthPix;
	private int 							nLeaves;
	private Map<Node<T>, Float>				nodeToDepth;
	private Map<Node<T>, Integer>			nodeToXPix;
	private Map<Node<T>, Integer>			nodeToYPix;
	private float							graphHorizDistancePerPix;
	private Set<IntOrthoLine>				branchLines;
	private Map<Node<T>, TaggedCheckBox<Node<T>>> 
											nodeToCbox;
	private Map<Node<T>, Color>				nodeToColor;
	private boolean							useCutLine;
	private int								cutLineXPix;
	
	
					
	
	
	
	
	
	
					//////////////////////////////////////////////////////////////////
					//														    	//
					//                         CONSTRUCTION                         //
					//														    	//
					//////////////////////////////////////////////////////////////////

	
	
	public TreePanel(Node<T> root, int graphWidthPix)
	{
		this.root = root;
		this.graphWidthPix = graphWidthPix;
		
		// Makes nodeToDepth map. Depth is in distance units, not pixels. Side effect: sets maxNodeDepth.
		nodeToDepth = computeNodeDepths(root);			
		
		// Pixometry.
		for (Node<T> node: nodeToDepth.keySet())
			if (node.isLeaf())
				nLeaves++;
		graphHorizDistancePerPix = maxNodeDepth / graphWidthPix;
		nodeToYPix = computeNodeYs(); 
		nodeToXPix = new HashMap<Node<T>, Integer>();
		for (Node<T> node: nodeToDepth.keySet())
		{
			int depthPix = (int)Math.round(nodeToDepth.get(node) / graphHorizDistancePerPix);
			int xPix = LEFT_MARGIN_INCL_HANDLE + depthPix;
			nodeToXPix.put(node, xPix);
		}
		
		// Checkboxes.
		setLayout(new Lom());
		nodeToCbox = new HashMap<Node<T>, TaggedCheckBox<Node<T>>>();
		for (Node<T> node: nodeToXPix.keySet())
		{
			TaggedCheckBox<Node<T>> cbox = new TaggedCheckBox<Node<T>>(node, "");
			cbox.addItemListener(this);
			add(cbox);
			nodeToCbox.put(node, cbox);
		}
		
		// Colors.
		buildNodeToColorMap();
		
		// Cut line.
		cutLineXPix = LEFT_MARGIN_INCL_HANDLE - ROOT_HANDLE_LEN_PIX/2;
		MLis mlis = new MLis();
		addMouseListener(mlis);
		addMouseMotionListener(mlis);
	}
	
	
	// Checkbox selection notifies the owner. When exactly 1 checkbox is selected, 
	// owner can support rerooting.
	void setOwner(TreeDialog<T> owner)
	{
		this.owner = owner;
	}
	
	
	public Dimension getPreferredSize()
	{

		int prefW = LEFT_MARGIN_INCL_HANDLE + graphWidthPix + RIGHT_MARGIN_INCL_TEXT;
		int prefH = TOP_BASELINE + (nLeaves-1)*DELTA_BASELINE + BOTTOM_MARGIN;
		prefH = Math.max(prefH, 400);
		return new Dimension(prefW, prefH);
	}
	
	
	private Map<Node<T>, Integer> computeNodeYs()
	{
		Map<Node<T>, Integer> ret = new HashMap<Node<T>, Integer>();
		
		// Leaf nodes are evenly spaced, in traversal order of appearance in the tree.
		int y = TOP_BASELINE - Y_LEAF_NODE_TO_BASELINE;
		Vector<Node<T>> leafNodes = root.collectLeafNodes();
		for (Node<T> leafNode: leafNodes)
		{
			ret.put(leafNode, y);
			y += DELTA_BASELINE;
		}
		
		// Recursively compute y of non-leaf nodes. Completes the map.
		recurseComputeNodeY(root, ret);		
		
		return ret;
	}
	
	
	// Node y is mean y of all its immediate children. Returns node's y coordinate and enters it in the map.
	private int recurseComputeNodeY(Node<T> node, Map<Node<T>, Integer> nodeToY)
	{
		int sumOfImmediateKidYs = 0;
		for (Node<T> kid: node.getKids())
		{
			int kidY = (nodeToY.containsKey(kid))  ?  nodeToY.get(kid)  :  recurseComputeNodeY(kid, nodeToY);
			sumOfImmediateKidYs += kidY;
  		}
		int y = (int)Math.round(sumOfImmediateKidYs / (float)node.getKids().size());
		nodeToY.put(node, y);
		return y;
	}

	
	// Assumes all leaves are at height = 0.
	private Map<Node<T>, Float> computeNodeDepths(Node<T> tree)
	{
		Map<Node<T>, Float> ret = new HashMap<Node<T>, Float>();
		ret.put(tree, 0f);
		recurseComputeNodeDepth(tree, ret);
		return ret;
	}
	
	
	private void recurseComputeNodeDepth(Node<T> node, Map<Node<T>, Float> nodeToDepth) throws IllegalArgumentException
	{
		float dParent = nodeToDepth.get(node);
		for (Node<T> kid: node.getKids())
		{
			float dKid = dParent + kid.getDistanceToParent();
			nodeToDepth.put(kid, dKid);
			maxNodeDepth = Math.max(maxNodeDepth, dKid);
			if (!kid.isLeaf())
				recurseComputeNodeDepth(kid, nodeToDepth);
		}
	}
	

	
	
	
	
	
					
					//////////////////////////////////////////////////////////////////
					//														    	//
					//                           PAINTING                           //
					//														    	//
					//////////////////////////////////////////////////////////////////

	

	public void paintComponent(Graphics g)
	{
		if (branchLines == null)
			branchLines = collectBranchLines();
		
		g.setColor(Color.WHITE);
		g.fillRect(0, 0, getWidth(), getHeight());
		
		g.setColor(Color.BLACK);
		paintLeafNodes(g);
		paintBranches(g);
		paintRootHandle(g);
		
		if (useCutLine)
		{
			g.setColor(HAIRLINE_COLOR);
			g.drawLine(cutLineXPix, 0, cutLineXPix, getHeight());
		}
	}
	
	
	private void paintLeafNodes(Graphics g)
	{
		g.setFont(FONT);
		
		for (Node<T> node: nodeToYPix.keySet())
		{
			if (node.isLeaf())
			{
				g.setColor(getColorForNode(node));
				int baseline = nodeToYPix.get(node) + Y_LEAF_NODE_TO_BASELINE;
				int xPix = nodeToXPix.get(node) + X_LEAF_NODE_TO_TEXT;
				g.drawString(getVisibleName(node), xPix, baseline);
			}
		}
	}
	
	
	protected String getVisibleName(Node<T> node)
	{
		// General case: display node's name.
		if (!(node.getPayload() instanceof Gene))
			return node.getName();
		
		// Special case for genes.
		Gene gene = (Gene)node.getPayload();
		return gene.getBestAvailableName();
	}
	
	
	protected void paintBranches(Graphics g)
	{
		for (IntOrthoLine line: branchLines) 
			line.paint(g);
	}
	
	
	protected class IntOrthoLine
	{
		Node<T>		parentNode;
		Node<T>		kidNode;			// null for vertical is ok
		int 		x0;
		int 		y0;
		int 		x1;
		int 		y1;
		
		protected IntOrthoLine(Node<T> parentNode, Node<T> kidNode, int x0, int y0, int x1, int y1)
		{
			assert x0 == x1  ||  y0 == y1;
			
			this.parentNode = parentNode;
			this.kidNode = kidNode;
			this.x0 = x0;
			this.y0 = y0;
			this.x1 = x1;
			this.y1 = y1;
			
		}
		
		public String toString()
		{
			return "IntOrthoLine (" + x0 + "," + y0 + ")---(" + x1 + "," + y1 + ")";
		}
		
		protected void paint(Graphics g)	
		{ 
			g.setColor(getColorForNode(parentNode));
			g.drawLine(x0, y0, x1, y1); 
		}
		
		protected boolean crosses(int x)
		{
			assert isHorizontal();
			return x0 <= x  &&  x <= x1;
		}
		
		protected boolean isLeftOf(int x)		{ return x0 <= x  &&  x1 <= x; }
		protected boolean isRightOf(int x)		{ return x0 >= x  &&  x1 >= x; }
		protected boolean isHorizontal()		{ return y0 == y1; }
	}  // End of inner class IntOrthoLine
	
	
	private Set<IntOrthoLine> collectBranchLines()
	{
		Set<IntOrthoLine> ret = new HashSet<IntOrthoLine>();
		
		for (Node<T> node: nodeToYPix.keySet())
		{
			if (node.isLeaf())
				continue;
			int xPix = nodeToXPix.get(node);
			for (Node<T> kid: node.getKids())
			{
				// Horizontal.
				int xKid = nodeToXPix.get(kid);
				int yKid = nodeToYPix.get(kid);
				ret.add(new IntOrthoLine(node, kid, xPix, yKid, xKid, yKid));
				// Vertical.
				int topKidY = nodeToYPix.get(node.getKids().firstElement());
				int bottomKidY = nodeToYPix.get(node.getKids().lastElement());
				ret.add(new IntOrthoLine(node, kid, xPix, topKidY, xPix, bottomKidY));
			}
		}
		
		return ret;
	}
	
	
	private void paintRootHandle(Graphics g)
	{
		int y = nodeToYPix.get(root);
		g.setColor(getColorForNode(root));
		g.drawLine(LEFT_MARGIN_INCL_HANDLE, y, LEFT_MARGIN_INCL_HANDLE-ROOT_HANDLE_LEN_PIX, y);
	}
	
	

	
	
	
	
	
				
				//////////////////////////////////////////////////////////////////
				//														    	//
				//                          CHECKBOXES                          //
				//														    	//
				//////////////////////////////////////////////////////////////////

	

	
	// Assumes the only contained components are the checkboxes.
	private class Lom extends LayoutAdapter
	{
	    public void layoutContainer(Container parent)                   
	    {
	    	Dimension pref = null;
	    	for (TaggedCheckBox<Node<T>> cbox: nodeToCbox.values())
	    	{
	    		Node<T> node = cbox.getTag();
	    		int xPixCenter = nodeToXPix.get(node);
	    		int yPixCenter = nodeToYPix.get(node);
	    		if (pref == null)
	    			pref = cbox.getPreferredSize();
	    		cbox.setSize(pref);
	    		int x = xPixCenter - pref.width/2;
	    		int y = yPixCenter - pref.height/2;
	    		cbox.setLocation(x, y);
	    	}
	    }
	    
	    public Dimension preferredLayoutSize(Container parent)         
	    {
	    	return new Dimension(PREF_W, PREF_H); 
	    }
	}  // End of inner class Lom
	
	
	// BEFORE: When a node is selected, deselect all its ancestors and descendants.
	// Now: better to let user choose overlapping trees if that's what they want.
	// TODO: delete commented-out code when new way is stable.
	public void itemStateChanged(ItemEvent e)
	{
		/***************
		// Deselect ancestors and descendants.
		if (e.getStateChange() == ItemEvent.SELECTED)
		{
			TaggedCheckBox<Node<T>> cbox = (TaggedCheckBox<Node<T>>)e.getSource();
			Node<T> node = cbox.getTag();
			Node<T> ancestor = node.getParent();
			while (ancestor != null)
			{
				nodeToCbox.get(ancestor).setSelected(false);
				ancestor = ancestor.getParent();
			}
			recurseDeselectdescendants(node);
		}
		*********/
		
		// Re-color subtrees.
		buildNodeToColorMap();
		repaint();
		
		// Notify owner.
		if (owner != null)
			owner.selectionChanged();
	}
	
	
	boolean rootIsSelected()
	{
		return nodeToCbox.get(root).isSelected();
	}
	
	
	private void recurseDeselectdescendants(Node<T> node)
	{
		for (Node<T> kid: node.getKids())
		{
			nodeToCbox.get(kid).setSelected(false);
			if (!kid.isLeaf())
				recurseDeselectdescendants(kid);
		}
	}

	
	
	
	
	
	
					//////////////////////////////////////////////////////////////////
					//														    	//
					//                            COLORS                            //
					//														    	//
					//////////////////////////////////////////////////////////////////

	
	
	
	private void buildNodeToColorMap()
	{
		nodeToColor = new HashMap<Node<T>, Color>();
		
		// Initially make every node black.
		for (Node<T> node: nodeToXPix.keySet())
			nodeToColor.put(node, Color.BLACK);
		
		// Any subtree whose checkbox is selected gets its own color. Note at most 1 checkbox is
		// selected in any path from root to leaf.
		int colorIndex = 0;
		for (TaggedCheckBox<Node<T>> cbox: nodeToCbox.values())
		{
			if (!cbox.isSelected())
				continue;				// this path taken for most checkboxes
			Color color = CLUSTER_COLORS[colorIndex++ % CLUSTER_COLORS.length];
			recurseSetNodeColor(cbox.getTag(), color);
		}
	}
	
	
	private void recurseSetNodeColor(Node<T> node, Color color)
	{
		nodeToColor.put(node, color);
		for (Node<T> kid: node.getKids())
			recurseSetNodeColor(kid, color);
	}
	
	
	protected Color getColorForNode(Node<T> node)
	{
		Color ret = nodeToColor.get(node);
		return (ret != null)  ?  ret  :  Color.BLACK;
	}
	
	

					
	
	
	
	
					
					//////////////////////////////////////////////////////////////////
					//														    	//
					//                            CUTLINE                           //
					//														    	//
					//////////////////////////////////////////////////////////////////


	

	void setUseCutLine(boolean useCutLine)
	{
		this.useCutLine = useCutLine;
		for (TaggedCheckBox<Node<T>> cbox: nodeToCbox.values())
			cbox.setEnabled(!useCutLine);
		repaint();
	}
	
	
	private class MLis extends MouseAdapter
	{
		public void mouseMoved(MouseEvent e)
		{
			if (!useCutLine)
				return;
			cutLineXPix = Math.max(e.getX(), MIN_HAIRLINE_XPIX);
			cutLineXPix = Math.min(cutLineXPix, getWidth()-50);
			repaint();
		}
		
		public void mouseClicked(MouseEvent e)
		{
			if (!useCutLine)
				return;
			cutLineXPix = Math.max(e.getX(), MIN_HAIRLINE_XPIX);
			cutLineXPix = Math.min(cutLineXPix, getWidth()-50);
			selectCheckboxesToRightOfHairline();
		}
	}  // end of inner class MLis
	
	
	private void selectCheckboxesToRightOfHairline()
	{
		// First deselect all.
		deselectAll();
		
		// Select any checkbox to the right of the hairline, whose parent is null or to the left of the hairline.
		for (TaggedCheckBox<Node<T>> cbox: nodeToCbox.values())
		{
			Node<T> node = cbox.getTag();
			int xBox = nodeToXPix.get(node);
			if (xBox <= cutLineXPix)
			{
				// Box is to left of hairline.
				continue;
			}
			Node<T> parent = node.getParent();
			if (parent == null  ||  nodeToXPix.get(parent) <= cutLineXPix)
			{
				// Box and parent lie to right and left of hairline.
				cbox.setSelected(true);
			}
		}
		
		// Repaint to assign colors to subtrees.
		repaint();
	}
	

	
	
					
					
					//////////////////////////////////////////////////////////////////
					//														    	//
					//                             MISC                             //
					//														    	//
					//////////////////////////////////////////////////////////////////

	
	
	void deselectAll()
	{
		for (TaggedCheckBox<Node<T>> cbox: nodeToCbox.values())
			cbox.setSelected(false);
	}
	
	
	Vector<Node<T>> getSelectedSubtrees()
	{
		Vector<Node<T>> ret = new Vector<Node<T>>();
		for (TaggedCheckBox<Node<T>> cbox: nodeToCbox.values())
			if (cbox.isSelected())
				ret.add(cbox.getTag());
		return ret;
	}
	

	public Vector<Vector<T>> collectSelectedSubtreeLeavesBySubtree()
	{
		Vector<Vector<T>> ret = new Vector<Vector<T>>();
		for (Node<T> subtreeRoot: getSelectedSubtrees())
		{
			Vector<T> leaves = subtreeRoot.collectLeafPayloads();
			ret.add(leaves);
		}
		return ret;
	}
	
	
	Node<T> getRoot()
	{
		return root;
	}
	
	
	Point getNodeLocation(Node<T> node)
	{
		return new Point(nodeToXPix.get(node), nodeToYPix.get(node));
	}
	
	
	Collection<TaggedCheckBox<Node<T>>> collectCheckBoxes()
	{
		return nodeToCbox.values();
	}

	
	static void sop(Object x)					{ System.out.println(x); }
	
	
	public static void main(String[] args)
	{
		try
		{
			String s = "(  (A:5, B:5):9,   ((C:8,D:8):2, E:10):4  );";
			NewickParser<String> parser = new NewickParser<String>(s, new NewickPayloadBuilderStringIdentity());
			Node<String> root = parser.parse();
			TreePanel<String> that = new TreePanel<String>(root, 500);
			that.setUseCutLine(true);
			TreeDialog<String> dia = new TreeDialog<String>(root);
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
