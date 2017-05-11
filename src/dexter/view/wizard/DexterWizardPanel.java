package dexter.view.wizard;

import static dexter.util.gui.Armable.DFLT_ARM_COLOR;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.filechooser.*;
import javax.swing.border.*;
import java.io.*;
import java.util.*;
import dexter.coreg.*;
import dexter.model.*;
import dexter.util.*;
import dexter.util.gui.*;
import dexter.view.graph.*;
import dexter.VisualConstants;


class DexterWizardPanel extends GridPanel implements ActionListener, WizardStageListener, VisualConstants
{
	// General constants.
	private final static int				PREF_W						= 1150;
	private final static int				PREF_H						=  700;
	
	// Constants for importing.
	private final static File				DATA_DIRF 					= new File("data");
	private final static File				STUDIES_DIRF 				= new File(DATA_DIRF, "Studies");
	private final static File				IMPORTED_STUDIES_DIRF 		= new File(DATA_DIRF, "ImportedStudies");	
	
	// Constants for scheduling.
	private final static int				GRID_SIZE_PIX				= 17;
	private final static int				REF_LEFT_MARGIN_GRIDS		=  2;
	private final static int				REF_TOP_MARGIN_GRIDS		=  5;
	private final static int				REF_DFLT_DURATION_HOURS		= 24;
	private final static int				REF_HEIGHT_GRIDS			=  6;		// should be even
	private final static int				MIN_DURATION				= 12;
	private final static int				MAX_DURATION				= 72;		// 3 days
	private final static Font				AXIS_FONT					= new Font("SansSerif", Font.PLAIN, 11);
	private final static int				MAX_PHASES					=  6;
	private final static Color				SECONDARY_PIN_LINE_COLOR	= BRICK_RED;
	
	// General variables.
	private DexterWizardPanel				outerThis;
	private JFileChooser					fileChooser;
	private SessionModel					sessionModel;
	private DexterWizardStage				currentStage;
	private Map<DexterWizardStage, Vector<Component>>		
											stageToComponents;
	
	// Variables for importing.
	private JButton							browseForImportBtn;
	private JButton							browseForSupplementalImportBtn;
	private JButton							applyImportBtn;
	private JButton							cancelImportBtn;
	private JPanel							importControlPanel;			// contains BROWSE/APPLY/CANCEL buttons
	private JComboBox						organismCombo;
	private JPanel							organismComboHolder;
	private SpreadsheetStructureEditor		structureEditor;	
	private ImportSelectionPanel			importSelPan;
	
	// Variables for scheduling.
	private StudyList						sessionStudies;
	private GraphBackgroundModel			backgroundModel;
	private Draggable 						currentDraggable;
	private Map<DexterWizardStage, HashSet<Draggable>>
											stageToDraggables;	
	private MappingStudyGraphPainter		mappingGraphPainter;
	private StudyToTimeAssignmentMap		studyToInferredTimeAssignmentMap;
	private StudyToTimeAssignmentMap		studyToEditedTimeAssignmentMap;
	private PolyLinePaintable				rbLine;
	private Vector<FloatRange>				illegalDropRanges;			// ALIGN stage only
	private WrapPanel						wrapPanel;
	
	// Variables for managing orthologs.
	private OrthologyWizardPanel			orthoPan;
	
	// Variables for coregulation.
	private CoregulationWizardPanel			coregPan;

	
					
	
	
	
	
	
	
	
					////////////////////////////////////////////////////////////
					//                                                        //
					//                      CONSTRUCTION                      //
					//                                                        //
					////////////////////////////////////////////////////////////
					


	
	
	DexterWizardPanel(DexterWizardDialog owner)
	{
		this(owner, null);
	}
	
	
	public DexterWizardPanel(DexterWizardDialog owner, File sessionModelFile)
	{
		super(GRID_SIZE_PIX, GRID_SIZE_PIX);
		
		// Load serialized session model if specified.
		if (sessionModelFile != null)
		{
			try
			{
				sessionModel = SessionModel.deserialize(sessionModelFile);
				backgroundModel = sessionModel.getGraphBackgroundModel();
			}
			catch (Exception x)
			{
				JOptionPane.showMessageDialog(null, x.getMessage());
				return;
			}
			String title = sessionModelFile.getAbsolutePath();
			int n = title.lastIndexOf("data");
			if (n >= 0)
				title = title.substring(n);
			owner.setTitle(title);
		}

		// Configure the superclass.
		outerThis = this;	
		setSuppressGridPainting(true);
		setPreferredSize(new Dimension(PREF_W, PREF_H));
		
		// A single mouse listener handles all mouse events in all phases. (Semantic events within
		// custom components are handled by local handlers.)
		MLis listener = new MLis();
		addMouseListener(listener);
		addMouseMotionListener(listener);
		
		// Build storage for stage-specific components and draggables.
		stageToComponents = new HashMap<DexterWizardStage, Vector<Component>>();	// must preserve order
		stageToDraggables = new HashMap<DexterWizardStage, HashSet<Draggable>>();
		for (DexterWizardStage stage: DexterWizardStage.values())
		{
			stageToComponents.put(stage, new Vector<Component>());
			stageToDraggables.put(stage, new HashSet<Draggable>());
		}
		
		// Components for IMPORT stage.
		setLayout(new Lom());
		importControlPanel = new JPanel();
		importControlPanel.setOpaque(false);
		browseForImportBtn = new JButton("Open spreadsheet...");
		browseForImportBtn.addActionListener(this);
		importControlPanel.add(browseForImportBtn);
		browseForSupplementalImportBtn = new JButton("Open supplemental spreadsheet...");
		browseForSupplementalImportBtn.setEnabled(false);
		browseForSupplementalImportBtn.addActionListener(this);
		importControlPanel.add(browseForSupplementalImportBtn);
		applyImportBtn = new JButton("Apply");
		applyImportBtn.addActionListener(this);
		applyImportBtn.setEnabled(false);
		importControlPanel.add(applyImportBtn);
		cancelImportBtn = new JButton("Cancel");
		cancelImportBtn.addActionListener(this);
		cancelImportBtn.setEnabled(false);
		importControlPanel.add(cancelImportBtn);
		borderize(importControlPanel);
		recordComponentForStage(importControlPanel, DexterWizardStage.IMPORT);
		organismComboHolder = new JPanel();
		organismComboHolder.setOpaque(false);
		organismComboHolder.add(new JLabel("Organism"));
		organismCombo = new JComboBox(Organism.PROVIDED);
		organismComboHolder.add(organismCombo);
		borderize(organismComboHolder);
		recordComponentForStage(organismComboHolder, DexterWizardStage.IMPORT);
		
		// Draggable for DURATION stage.
		int durationHrs = getReferenceDuration();
		IndexedPinDraggable durationPin = 
			new IndexedPinDraggable(-1, durationHrs+" hrs", durationHrs, false);
		durationPin.setMinMaxXGrids(REF_LEFT_MARGIN_GRIDS+MIN_DURATION, REF_LEFT_MARGIN_GRIDS+MAX_DURATION);
		durationPin.setTitleSuffix(" hrs");
		recordDraggableForStage(durationPin, DexterWizardStage.DURATION);

		// Components for PHASES stage. (Draggables are added during the stage.)
		DLPhaseUseStrip dlStrip = new DLPhaseUseStrip();
		recordComponentForStage(dlStrip, DexterWizardStage.PHASES);	
		
		// Component for WRAP stage.
		wrapPanel = new WrapPanel(this);
		recordComponentForStage(wrapPanel, DexterWizardStage.WRAP);

		currentStage = DexterWizardStage.IMPORT;
		for (Component c: stageToComponents.get(currentStage))
			add(c);
	}
	
	
	private void borderize(JComponent c)
	{
		c.setBorder(BorderFactory.createLineBorder(Color.BLACK, 3));
	}
	
	
	private void recordComponentForStage(Component c, DexterWizardStage stage)
	{
		stageToComponents.get(stage).add(c);
	}	
	
	
	private void recordDraggableForStage(Draggable draggable, DexterWizardStage stage)
	{
		assert stageToDraggables != null;
		assert stageToDraggables.containsKey(stage);
		stageToDraggables.get(stage).add(draggable);
	}
	
	
	private SessionModel loadSessionModel(File sessionModelFile) throws IOException
	{
		try
		{
			return SessionModel.deserialize(sessionModelFile);
		}
		catch (Exception x)
		{
			String err = "Couldn't load session file " + sessionModelFile;
			throw new IOException(err);
		}
	}
	
	


			
	
			
			
			
			
					////////////////////////////////////////////////////////////
					//                                                        //
					//              INNER CLASSES FOR SCHEDULING              //
					//                                                        //
					////////////////////////////////////////////////////////////
					
				
			
	
	
	private class IndexedPinDraggable extends VerticalPinDraggable
	{
		private int						index;
		private String					titleSuffix = "";
		private int 					hour;
		private GraphBackgroundStyle	style;
		
		IndexedPinDraggable(int index, String title, int hour, boolean handleAtTop)
		{
			super(title, hGridsToPix(REF_LEFT_MARGIN_GRIDS+hour), 
					vGridsToPix(REF_TOP_MARGIN_GRIDS - 1),
					vGridsToPix(getRefBottomGrids() + 1),
					hGridsToPix(1),
					handleAtTop); 
			this.index = index;
			this.hour = hour;
		}
		
		public int[] drag(int xHandle, int yHandle)
		{
			int[] ret = super.drag(xHandle, yHandle);
			snap();
			hour = getXGrids() - REF_LEFT_MARGIN_GRIDS;
			setTitle(hour + titleSuffix);
			return ret;
		}
		
		public String toString()					
		{ 
			return "IndexedPinDraggable #" + index + " " + super.toString(); 
		}		
		
		int getIndex()								{ return index; }
		int getHour()								{ return hour; }
		void setTitleSuffix(String s)				{ titleSuffix = s; }
		GraphBackgroundStyle getStyle()				{ return style; }
		void setStyle(GraphBackgroundStyle style)	{ this.style = style; }
	}  // End of inner class IndexedPinDraggable
	
	
	abstract private class PhaseUseStrip extends JPanel implements ItemListener
	{
		//Vector<JRadioButton>		radios;
		JComboBox					combo;
		
		PhaseUseStrip(GraphBackgroundStyle style)
		{
			setOpaque(false);
			
			// Combo.
			Vector<String> labels = new Vector<String>();
			labels.add("1 phase");
			for (int i=2; i<=MAX_PHASES; i++)
				labels.add(i + " phases");
			combo = new JComboBox(labels);
			add(combo);
			setListener();
		}
		
		protected void setListener()
		{
			combo.addItemListener(this);
		}
		
		protected void unsetListener()
		{
			combo.removeItemListener(this);
		}
		
		// Sent by a "use" or "don't use" radio button, or by the "# of phases" combo.
		public void itemStateChanged(ItemEvent e)
		{
			int nPhases = getNPhases();
			rebuildPins();
			adjustPhasePinLengthsAndColors();
			outerThis.repaint();
		}
		
		private void rebuildPins()
		{
			backgroundModel.adjustNPhases(getStyle(), getNPhases());
			Set<Draggable> pinsToDelete = new HashSet<Draggable>();			// delete all pins for this style
			GraphBackgroundStyle style = getStyle();						// and rebuild as needed
			for (Draggable d: stageToDraggables.get(currentStage))		
				if (((IndexedPinDraggable)d).getStyle() == style)
					pinsToDelete.add(d);
			stageToDraggables.get(currentStage).removeAll(pinsToDelete);
			Vector<IndexedPinDraggable> pins = buildPhaseChangePinsForBackgroundModel(style);
			stageToDraggables.get(currentStage).addAll(pins);
		}
		
		int getNPhases()	
		{
			return combo.getSelectedIndex() + 1; 
		}
		
		// Automatic call to itemStateChanged() will take care of pins.
		void setNPhases(int nPhases)
		{
			unsetListener();
			if (nPhases > 0)
				combo.setSelectedIndex(nPhases - 1);
			rebuildPins();
			setListener();
		}

		
		public String toString()			
		{ 
			return getClass().getName() + ": combo size = " + combo.getItemCount(); 
		}
		
		abstract GraphBackgroundStyle getStyle();		
	}  // End of inner class PhaseUseStrip
	
	
	private class DLPhaseUseStrip extends PhaseUseStrip
	{
		private JRadioButton		startDarkRadio;
		private JRadioButton		startLightRadio;
		
		DLPhaseUseStrip()
		{
			super(GraphBackgroundStyle.DL);
			ButtonGroup bgrp = new ButtonGroup();
			startDarkRadio = new JRadioButton("Start dark", true);
			startDarkRadio.addItemListener(this);
			bgrp.add(startDarkRadio);
			add(startDarkRadio);
			startLightRadio = new JRadioButton("Start light", false);
			startLightRadio.addItemListener(this);
			bgrp.add(startLightRadio);
			add(startLightRadio);			
		}
		
		// Sent by "start dark" or "start light" radio.
		public void itemStateChanged(ItemEvent e)
		{
			if (e.getSource() != startDarkRadio  &&  e.getSource() != startLightRadio)
			{
				super.itemStateChanged(e);
				int nPhases = getNPhases();
			}
			else if (e.getStateChange() == ItemEvent.SELECTED)
			{
				backgroundModel.setStartsDark(e.getSource() == startDarkRadio);
				outerThis.repaint();
			}
		}

		void setStartsDark(boolean b)		
		{ 
			unsetListener();
			startDarkRadio.removeItemListener(this);
			startLightRadio.removeItemListener(this);
			startDarkRadio.setSelected(b); 
			startDarkRadio.addItemListener(this);
			startLightRadio.addItemListener(this);
			setListener();
		}
		
		GraphBackgroundStyle getStyle()		{ return GraphBackgroundStyle.DL; }
	}  // End of inner class DLPhaseUseStrip
	
	
	// 1 instance of this for each study.
	private class AlignStudyControlPanel extends JPanel implements ActionListener
	{
		private Study				study;
		private JRadioButton		snapRadio;
		private JButton				clearBtn;
		private JButton				interpolateBtn;
		private JButton				resetBtn;
		private JButton				doneBtn;
		
		AlignStudyControlPanel(Study study)
		{
			assert currentStage == DexterWizardStage.ALIGN;
			this.study = study;
			setLayout(new FlowLayout(FlowLayout.LEFT));
			add(new JLabel("Align timepoints for " + study.getName()));
			snapRadio = new JRadioButton("Snap to hours", true);
			add(snapRadio);
			clearBtn = new JButton("Clear");
			clearBtn.addActionListener(this);
			add(clearBtn);
			interpolateBtn = new JButton("Interpolate");
			interpolateBtn.setEnabled(canInterpolate(study));
			interpolateBtn.addActionListener(this);
			add(interpolateBtn);
			resetBtn = new JButton("Reset");
			resetBtn.addActionListener(this);
			add(resetBtn);
			doneBtn = new JButton("Done");
			doneBtn.addActionListener(this);
			add(doneBtn);
		}
		
		// Finish alignment for study. Remove this control panel from the main schedule panel.
		public void actionPerformed(ActionEvent e)
		{
			if (e.getSource() == clearBtn)
			{
				Study currentStudy = getCurrentAlignStudyControlPanel().getStudy();
				TimeAssignmentMap timeAssignmentMap = studyToEditedTimeAssignmentMap.get(currentStudy);
				timeAssignmentMap.clear();
				outerThis.repaint();
			}
			else if (e.getSource() == interpolateBtn)
			{
				interpolateForStudy(study);
				outerThis.repaint();
			}
			else if (e.getSource() == resetBtn)
			{
				Vector<String> colNames = study.getTimepointColumnNames();
				TimeAssignmentMap assignmentMapToBeEdited = new TimeAssignmentMap(colNames);
				assignmentMapToBeEdited.setLatestTimepoint(backgroundModel.getDuration());
				studyToEditedTimeAssignmentMap.put(study, assignmentMapToBeEdited);
				outerThis.repaint();
			}
			else if (e.getSource() == doneBtn)
			{
				// Remove this panel from the main SchedulePanel instance.
				outerThis.remove(this);
				outerThis.validate();
				assert stageToComponents.get(currentStage).contains(this);
				stageToComponents.get(currentStage).remove(this);
				mappingGraphPainter = null;
				outerThis.repaint();
				// This study is aligned, so ok to start aligning a different study.
				for (Study s: sessionStudies)
				{
					StudyPreviewStrip strip = getPreviewStripForStudy(s);
					assert strip != null;
					strip.popButtonEnableStates();
				}	
				getPreviewStripForStudy(study).setEnableShare(true);
				getPreviewStripForStudy(study).setAligned(true);
			}
		}
		
		void setEnableInterpolation(boolean b)	{ interpolateBtn.setEnabled(b); }
		boolean getDoesSnap()					{ return snapRadio.isSelected(); }
		Study getStudy()						{ return study; }
	}  // End of inner class AlignStudyControlPanel
	
	
	
	
	
	
	
		
					
					/////////////////////////////////////////////////////
					//                                                 //
					//                     LAYOUT                      //
					//                                                 //
					/////////////////////////////////////////////////////
					

	

	private class Lom extends LayoutAdapter
	{
		public void layoutContainer(Container parent)
		{
			// All components for current stage are their own preferred sizes (unless overridden below).
			for (Component c: stageToComponents.get(currentStage))
				c.setSize(c.getPreferredSize());
			Dimension containerSize = parent.getSize();

			int leftMargin = 5;
			int y = 5;
			int broadW = containerSize.width - 2*leftMargin;
			
			switch (currentStage)
			{
				case IMPORT:
					importControlPanel.setLocation(leftMargin, y);
					y += importControlPanel.getPreferredSize().height + 8;
					int ltfW = (int)Math.max(containerSize.width*.77, 350);	
					int holderPrefH = organismComboHolder.getPreferredSize().height;
					organismComboHolder.setBounds(leftMargin, y, ltfW, holderPrefH);
					y += holderPrefH + 8;
					if (structureEditor != null)			
					{
						Dimension pref = structureEditor.getPreferredSize();
						structureEditor.setBounds(leftMargin, y, broadW, pref.height);
						y += pref.height + 8;
					}
					break;
					
				case SELECT_IMPORTS:
					assert importSelPan != null  :  "null importSelPan";
					importSelPan.setSize(broadW, importSelPan.getPreferredSize().height);
					importSelPan.setLocation(leftMargin, y);
					break;
					
				case ORTHOLOGS:
					assert orthoPan != null  :  "null orthoPan";
					y = (getHeight() - orthoPan.getPreferredSize().height) / 2;
					orthoPan.setLocation(leftMargin, y);
					break;
					
				case PHASES:
					for (Component strip: stageToComponents.get(currentStage))
					{
						strip.setSize(strip.getPreferredSize());
						int yGrids = (strip instanceof DLPhaseUseStrip)  ?  0  :  getRefBottomGrids() + 3;
						strip.setLocation(hGridsToPix(REF_LEFT_MARGIN_GRIDS), vGridsToPix(yGrids));
					}
					break;
					
				case ALIGN:
					JPanel alignControlPan = getCurrentAlignStudyControlPanel();
					if (alignControlPan != null)
					{
						alignControlPan.setSize(alignControlPan.getPreferredSize());
						y = vGridsToPix(getRefBottomGrids() + 7) - vGridsToPix(1) / 2;
						alignControlPan.setLocation(hGridsToPix(REF_LEFT_MARGIN_GRIDS), y);
					}
					JScrollPane spane = getAssignScrollPane();
					spane.setSize(spane.getPreferredSize());
					y = vGridsToPix(getRefBottomGrids() + 9);
					y += vGridsToPix(1) / 2;		// avoid jitter against grid
					spane.setLocation(hGridsToPix(2), y);
					int spaneBottom = spane.getLocation().y + spane.getHeight();
					wrapPanel.setLocation(spane.getLocation().x, spaneBottom+8);
					break;
					
				case COREGULATION:
					int xCoregPan = (getWidth() - coregPan.getWidth()) / 2;
					int yCoregPan = (getHeight() - coregPan.getHeight()) / 2;
					coregPan.setLocation(xCoregPan, yCoregPan);
					break;
					
				default:
					break;
			}
		}
	}
	
	
	
	
	

					
					
	
	
					///////////////////////////////////////////////////////
					//                                                   //
					//                     PAINTING                      //
					//                                                   //
					///////////////////////////////////////////////////////

	
	
	
	
	
	public void paintComponent(Graphics g)
	{
		assert currentStage != null  :  "Can't determine current stage";
				
		setSuppressGridPainting(!currentStage.usesGrid());
		super.paintComponent(g);				// clears and paints the grid background
		Graphics2D g2 = (Graphics2D)g;
		
		if (currentStage.usesGrid())
		{
			// Reference. If background is just dark/light or just treatment, fill the space with the
			// appropriate background. If both background types are used, fill the upper half with 
			// dark/light and lower half with treatment.
			assert backgroundModel != null  :  "Null backgroundModel";
			GraphBackgroundPainter dlPainter = new GraphBackgroundPainter(GraphBackgroundStyle.DL, 
				backgroundModel, getRefBoundsPix(), GRID_SIZE_PIX);	
			dlPainter.paint(g2);
			Stroke entryStroke = g2.getStroke();
			g2.setStroke(new BasicStroke(2));
			g2.setColor(Color.BLACK);
			g2.draw(getRefBoundsPix());
			g2.setStroke(entryStroke);
		}
		
		// Draggables.
		for (Draggable draggable: stageToDraggables.get(currentStage))
			draggable.paint(g2);

		// Other variants.
		switch (currentStage)
		{
			case DURATION:
				paintHAxisHour(g2, 0);
				break;
			case ALIGN:
				if (mappingGraphPainter != null)
				{
					mappingGraphPainter.paint(g2);
					paintMappedTimepoints(g2);
				}
				if (illegalDropRanges != null)
				{
					g2.setPaint(FORBIDDEN_ZONE_PAINT);
					for (FloatRange range: illegalDropRanges)
					{
						int x0 = hoursToXPix((int)range.min);
						int x1 = hoursToXPix((int)range.max);
						Rectangle refPix = getRefBoundsPix();
						g.fillRect(x0, refPix.y, x1-x0, refPix.height);
					}
				}
				if (rbLine != null)
					rbLine.paint(g);
				break;
		}
	}
	
	
	private void paintHAxisHour(Graphics2D g2, int hour)
	{
		paintHAxisLabelAtHour(g2, ""+hour, hour);
	}
	
	
	private void paintHAxisLabelAtHour(Graphics2D g2, String label, int hour)
	{
		g2.setFont(AXIS_FONT);
		g2.setColor(Color.BLACK);
		int sw = g2.getFontMetrics().stringWidth(label);
		int xPix = hGridsToPix(REF_LEFT_MARGIN_GRIDS + hour) - sw/2;
		int baselinePix = getRefBottomPix() + 18;
		g2.drawString(label, xPix, baselinePix);
	}
	

	// ALIGN phase only. Connects mapped timepoints in the MappingGraphPainter to corresponding
	// points in the reference.
	private void paintMappedTimepoints(Graphics g)
	{	
		// Meez.
		assert currentStage == DexterWizardStage.ALIGN;
		assert mappingGraphPainter != null;
		assert getCurrentAlignStudyControlPanel() != null;
		Study study = getCurrentAlignStudyControlPanel().getStudy();
		assert study != null;
		TimeAssignmentMap editedAssignmentMap = studyToEditedTimeAssignmentMap.get(study);
		assert editedAssignmentMap != null;
		TimeAssignmentMap srcAssignmentMap = studyToInferredTimeAssignmentMap.get(study);
		assert srcAssignmentMap != null;
		assert editedAssignmentMap != srcAssignmentMap;
		
		// Mapping lines.
		TreeMap<Integer, String> xPixCenterToLabel = new TreeMap<Integer, String>();		// for assigning tiers
		for (String tpName: srcAssignmentMap.keySet())
		{
			if (!editedAssignmentMap.containsKey(tpName))
				continue;
			int x0 = hoursToXPix(srcAssignmentMap.get(tpName));
			int y0 = mappingGraphPainter.getOrigin().y;			
			float assignedHours = editedAssignmentMap.get(tpName);
			int x1 = hoursToXPix(assignedHours);
			int y1 = getRefBottomPix();
			PolyLinePaintable lines = new PolyLinePaintable(x0, y0);
			lines.setLineColor(Color.BLUE);
			lines.lineTo(x1, y1);
			lines.lineTo(x1, getRefBoundsPix().y);
			lines.paint(g);
			String sHours = StringUtils.hoursToHM(assignedHours);
			xPixCenterToLabel.put(x1, sHours);
		}

		// Text labels on mapping lines, assigned to tiers to avoid overlap.
		g.setFont(AXIS_FONT);
		FontMetrics fm = g.getFontMetrics();
		Map<Integer, Integer> xPixToTier = StringUtils.horizontalLabelsToVerticalTier(xPixCenterToLabel, fm);
		assert xPixToTier.size() == xPixCenterToLabel.size();
		assert xPixToTier.keySet().equals(xPixCenterToLabel.keySet());
		int tier0Baseline = getRefBoundsPix().y-3;
		int tierVertDelta = AXIS_FONT.getSize() + 2;
		for (Integer xPixCenter: xPixToTier.keySet())
		{
			String label = xPixCenterToLabel.get(xPixCenter);
			int xPixLeft = xPixCenter - fm.stringWidth(label)/2;
			int tier = xPixToTier.get(xPixCenter);
			int baseline = tier0Baseline - tier*tierVertDelta;
			g.drawString(label, xPixLeft, baseline);
		}
	}
	
	
	

							
							
							
							/////////////////////////////////////////////////////
							//                                                 //
							//                    PIXOMETRY                    //
							//                                                 //
							/////////////////////////////////////////////////////
							



	private int getRefBottomGrids()
	{
		return REF_TOP_MARGIN_GRIDS + REF_HEIGHT_GRIDS;
	}


	private int getRefBottomPix()
	{
		return hGridsToPix(getRefBottomGrids());
	}


	private int getRefRightGrids()
	{
		return REF_LEFT_MARGIN_GRIDS + getReferenceDuration();
	}


	private int getRefRightPix()
	{
		return hGridsToPix(getRefRightGrids());
	}


	private GridRectangle getRefBoundsGrids()
	{
		return new GridRectangle(REF_LEFT_MARGIN_GRIDS, REF_TOP_MARGIN_GRIDS, 
				     			 getReferenceDuration(), REF_HEIGHT_GRIDS);
	}


	private Rectangle getRefBoundsPix()
	{
		return gridRectToPix(getRefBoundsGrids());
	}


	private GraphBackgroundStyle xyToBackgroundStyle(int x, int y)
	{
		if (currentStage != DexterWizardStage.PHASES)
			return null;

		Rectangle refBoundsPix = getRefBoundsPix();
		if (!refBoundsPix.contains(x, y))
			return null;

		// Graph contains the mouse and stage is correct.
		return GraphBackgroundStyle.DL;
	}


	// Returns -1 if not in a phase.
	private int xyToPhaseIndex(MouseEvent e)
	{
		return xyToPhaseIndex(e.getX(), e.getY());
	}


	// Returns -1 if not in phase stage.
	private int xyToPhaseIndex(int x, int y)
	{
		if (currentStage != DexterWizardStage.PHASES)
			return -1;

		Rectangle refBoundsPix = getRefBoundsPix();
		if (!refBoundsPix.contains(x, y))
			return -1;

		GraphBackgroundStyle style = xyToBackgroundStyle(x, y);

		Rectangle r = new Rectangle(refBoundsPix.x, refBoundsPix.y, 0, refBoundsPix.height);
		Vector<Integer> changes = backgroundModel.getPhaseChanges(style);
		if (changes.isEmpty())
			return 0;
		for (int i=0; i<changes.size(); i++)
		{
			r.width = hGridsToPix(changes.get(i));
			if (r.contains(x, y))
				return i;
		}
		return changes.size();
	}


	private float xPixToHours(int xPix)
	{
		return (xPix - hGridsToPix(REF_LEFT_MARGIN_GRIDS)) / (float)getGridWidth();
	}


	private int hoursToXPix(float hours)
	{
		return (int)Math.round((hours + REF_LEFT_MARGIN_GRIDS) * getGridWidth());
	}


	private int xGridsToHours(int xGrids)
	{
		return xGrids - REF_LEFT_MARGIN_GRIDS;
	}








					
					
					////////////////////////////////////////////////////////////
					//                                                        //
					//                          EVENTS                        //
					//                                                        //
					////////////////////////////////////////////////////////////
		
	
	

	public void actionPerformed(ActionEvent e)
	{
		if (e.getSource() == browseForImportBtn)
			browseForSpeadsheetFile();	
		
		else if (e.getSource() == browseForSupplementalImportBtn)
		{
			assert structureEditor != null;
			structureEditor.browseForSupplementalSpreadsheetModal();
		}
		
		else if (e.getSource() == applyImportBtn)
			applySpreadsheetStructureEdits();
		
		else if (e.getSource() == cancelImportBtn)
			terminateImporting();
	}
	
	
	public class MLis extends MouseAdapter
	{
		private String				timepointAtDragAnchor;			// for PHASES stage
		private String				timepointForClickDeletion;		// for ALIGN stage
		
		public void mouseMoved(MouseEvent me)
		{
			if (currentStage != DexterWizardStage.ALIGN)
			{
				// Stages before ALIGN have draggables. ALIGN doens't.
				if (currentDraggable != null)
					currentDraggable.disarm();
				currentDraggable = null;
				Set<Draggable> draggablesForStage = stageToDraggables.get(currentStage);
				for (Draggable d: draggablesForStage)
				{
					if (d.contains(me))
					{
						currentDraggable = d;
						break;
					}
				}
				if (currentDraggable != null)
					currentDraggable.arm();
				outerThis.repaint();
			}
			else
			{
				if (mappingGraphPainter == null)
					return;
				String tptName = mappingGraphPainter.xyToTimepointName(me);
				mappingGraphPainter.setArmedTimepoint(tptName);
				outerThis.repaint();
			}
		}
		
		public void mousePressed(MouseEvent me)
		{
			mouseMoved(me);				// catch up

			if (currentStage != DexterWizardStage.ALIGN)
			{
				// Stages before ALIGN have draggables. ALIGN doens't.
				if (currentDraggable == null  ||  !currentDraggable.isArmed())
					return;
				currentDraggable.startDrag(me);
				outerThis.repaint();
			}
			
			else
			{
				// Align stage: initiate a rubber-band line from top of selected timepoint in study.
				if (mappingGraphPainter == null)
					return;
				timepointAtDragAnchor = mappingGraphPainter.getArmedTimepoint();
				if (timepointAtDragAnchor == null)
					return;
				Study study = getCurrentAlignStudyControlPanel().getStudy();
				assert study != null;
				TimeAssignmentMap assignmentMap = studyToEditedTimeAssignmentMap.get(study);
				assert assignmentMap != null;
				if (assignmentMap.containsKey(timepointAtDragAnchor))
				{
					// This timepoint is already mapped. Mark it for deletion in mouseClicked().
					timepointForClickDeletion = timepointAtDragAnchor;
				}
				else
				{
					// Timepoint isn't mapped. Initiate a drag for mapping.
					mappingGraphPainter.selectArmedTimepoint();
					int xAnchor = mappingGraphPainter.getXPixOfArmedTimepoint();
					int yAnchor = mappingGraphPainter.getOrigin().y;
					rbLine = new PolyLinePaintable(xAnchor, yAnchor);
					FloatRange referenceRange = new FloatRange(0, true, getReferenceDuration(), true);
					FloatRange legalRange = getLegalMappedRangeForTimepoint(timepointAtDragAnchor);
					illegalDropRanges = referenceRange.minus(legalRange);
				}
				outerThis.repaint();
			}
		}
		
		public void mouseDragged(MouseEvent me)
		{
			switch (currentStage)
			{
				case DURATION:
				case PHASES:
					dragForDurationOrPhasesStage(me);
					break;
				case ALIGN:
					dragForAlignStage(me);
					break;
			}
			outerThis.repaint();
		}
		
		// Caller must repaint after return from this method.
		private void dragForDurationOrPhasesStage(MouseEvent me)
		{
			if (currentDraggable == null) 
				return;
			
			IndexedPinDraggable currentPin = (IndexedPinDraggable)currentDraggable;
			currentPin.drag(me);		// snaps
			int selectedHour = currentPin.getHour();
			
			switch (currentStage)
			{
				case DURATION:
					assert currentPin == getDurationPin();
					setReferenceDuration(selectedHour);
					backgroundModel.setDuration(selectedHour);
					currentPin.setTitle(selectedHour + " hrs");
					for (DexterWizardStage stage: DexterWizardStage.values())
					{
						if (stage == DexterWizardStage.DURATION)
							continue;
						Collection<Draggable> draggables = stageToDraggables.get(stage);
						Set<IndexedPinDraggable> pinsToDelete = new HashSet<IndexedPinDraggable>();
						for (Draggable d: draggables)
						{
							if (d instanceof IndexedPinDraggable)
							{
								IndexedPinDraggable pin = (IndexedPinDraggable)d;
								if (pin.getHour() >= selectedHour)
									pinsToDelete.add(pin);
							}
						}
						draggables.removeAll(pinsToDelete);
					}
					for (GraphBackgroundStyle style: GraphBackgroundStyle.values())
					{
						if (backgroundModel.getUsesStyle(style))
						{
							int nNewPins = collectPhasePinsForStyle(style).size();
							backgroundModel.reduceNChangesDropFromRight(style, nNewPins);
						}
					}
					break;
					
				case PHASES:
					assert stageToDraggables.get(currentStage).contains(currentDraggable);
					assert currentDraggable instanceof IndexedPinDraggable;
					IndexedPinDraggable ipin = (IndexedPinDraggable)currentDraggable;
					int hour = ipin.getHour();
					int index = ipin.getIndex();
					GraphBackgroundStyle style = ipin.getStyle();
					backgroundModel.setPhaseChangeAtIndex(style, hour, index);
					break;
					
				case ALIGN:
					assert false;
					break;
			}
			
			outerThis.repaint();
		}

		// Caller must repaint after return from this method. When drag begins, legal destinations in the
		// reference are highlighted. If drag tip is in a legal destination, rb line is 2 segments. Otherwise
		// rb is a single segment.
		private void dragForAlignStage(MouseEvent me)
		{
			assert currentStage == DexterWizardStage.ALIGN;
			if (rbLine == null)
				return;
			assert timepointForClickDeletion == null  :  "Non-null timepoint for click deletion.";
			boolean legal = xyIsLegalForTimepointMapping(me, timepointAtDragAnchor);
			if (!legal)
			{
				// RB line doesn't end at a legal point. Make sure the polyline contains only 1 segment.
				if (rbLine.size() > 1)
					rbLine.setSize(1);
				rbLine.stretchTo(me.getX(), me.getY());
			}
			else
			{
				// RB line does end at a legal point. Make sure the polyline contains 2 segments.
				if (rbLine.size() > 0)
					rbLine.setSize(1);
				rbLine.stretchTo(me.getX(), getRefBottomPix());
				int xTip = me.getX();								
				if (getCurrentAlignStudyControlPanel().getDoesSnap())	
				{
					GridPoint snappedGrids = snapPixToGrid(me);		
					xTip = hGridsToPix(snappedGrids.xGrids);
				}
				rbLine.stretchTo(xTip, getRefBottomPix());
				rbLine.lineTo(xTip, vGridsToPix(REF_TOP_MARGIN_GRIDS));  
			}
			rbLine.limn(DFLT_ARM_COLOR, 5);
		}
		
		public void mouseReleased(MouseEvent me)
		{
			switch (currentStage)
			{
				case DURATION:
				case PHASES:
					releaseForDurationOrPhasesStage(me);
					break;
				case ALIGN:
					releaseForAlignStage(me);
					break;
			}
			mouseMoved(me);
			outerThis.repaint();
		}
		
		private void releaseForDurationOrPhasesStage(MouseEvent me)
		{
			if (currentDraggable == null  ||  !currentDraggable.isArmed())
				return;
			
			mouseDragged(me);			// catch up
			currentDraggable.stopDrag();
			if (currentStage == DexterWizardStage.PHASES)
			{
				GraphBackgroundStyle style = ((IndexedPinDraggable)currentDraggable).getStyle();
				for (IndexedPinDraggable pin: collectPhasePinsForStyle(style))
					setMinAndMaxForPin(pin, style);
			}
			mouseMoved(me);				// arms if still in handle
		}
		
		private void releaseForAlignStage(MouseEvent me)
		{
			assert currentStage == DexterWizardStage.ALIGN;
			
			mouseDragged(me);			// catch up, update timepointAtDragAnchor
			if (rbLine == null)
				return;
			assert timepointForClickDeletion == null;
			Study currentStudy = getCurrentAlignStudyControlPanel().getStudy();
			TimeAssignmentMap timeAssignmentMap = studyToEditedTimeAssignmentMap.get(currentStudy);
			assert !timeAssignmentMap.containsKey(timepointAtDragAnchor);
			if (xyIsLegalForTimepointMapping(me, timepointAtDragAnchor))
			{
				int xTip = me.getX();								
				if (getCurrentAlignStudyControlPanel().getDoesSnap())	
				{
					GridPoint snappedGrids = snapPixToGrid(me);		
					xTip = hGridsToPix(snappedGrids.xGrids);
				}			
				float hours = xPixToHours(xTip);
				timeAssignmentMap.put(timepointAtDragAnchor, hours);
			}
			rbLine = null;
			timepointAtDragAnchor = null;
			illegalDropRanges = null;
			
			getCurrentAlignStudyControlPanel().setEnableInterpolation(canInterpolate());
		}
		
		public void mouseClicked(MouseEvent e)
		{				
			switch (currentStage)
			{
				case ALIGN:
					timepointAtDragAnchor = null;
					if (timepointForClickDeletion != null)
					{
						Study currentStudy = getCurrentAlignStudyControlPanel().getStudy();
						TimeAssignmentMap timeAssignmentMap = studyToEditedTimeAssignmentMap.get(currentStudy);
						assert timeAssignmentMap.containsKey(timepointForClickDeletion);
						timeAssignmentMap.remove(timepointForClickDeletion);
						timepointForClickDeletion = null;
						mappingGraphPainter.setArmedTimepoint(null);
						outerThis.repaint();
					}			
					getCurrentAlignStudyControlPanel().setEnableInterpolation(canInterpolate());
					break;
			}
		}
	}  // End of inner class MLis
	
	
	
	


	
	


				
				///////////////////////////////////////////////////////////////////////
				//                                                                   //
				//                    SPREADSHEET AND IMPORT FILES                   //
				//                                                                   //
				///////////////////////////////////////////////////////////////////////




	
	
	private void browseForSpeadsheetFile()
	{
		if (fileChooser == null)
			fileChooser = buildFileChooser();
		
		if (fileChooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION)
			return;
		
		File spreadsheetFile = fileChooser.getSelectedFile();
		assert spreadsheetFile != null;
		openSpreadsheet(spreadsheetFile);
		browseForImportBtn.setEnabled(false);
		browseForSupplementalImportBtn.setEnabled(true);
		applyImportBtn.setEnabled(true);
		cancelImportBtn.setEnabled(true);
	}
	
	
	private void openSpreadsheet(File f)
	{
		assert currentStage == DexterWizardStage.IMPORT;
		
		Study preexistingStudy = null;
		File serFile = Study.getSerFileForSpreadsheetFile(IMPORTED_STUDIES_DIRF, f);
		if (serFile.exists())
		{
			try
			{
				preexistingStudy = Study.deserialize(serFile);
				Organism org = preexistingStudy.getOrganism();
				if (!org.isProvided())
					org = org.toProvided();
				assert org != null  :
					"Unusual organism " + preexistingStudy.getOrganism() + " in study " + preexistingStudy;
				String s = "Spreadsheet is already imported. Edit imported version, or start over?";
				// Options appear in JOptionPane in reverse order.
				String[] options = { "Start over", "Edit imported version" };
				int choice = JOptionPane.showOptionDialog(this, 
					  s, 								// message
					  "", 								// title
					  JOptionPane.YES_NO_OPTION, 		// 2 options
					  JOptionPane.QUESTION_MESSAGE,		// for plaf
					  null, 							// icon
					  options,							// button labels
					  options[1]);						// initial selection
				if (choice == JOptionPane.CLOSED_OPTION)
					return;
				else if (choice == 0)
					preexistingStudy = null;
			}
			catch (Exception x)
			{
				String err = "Imported version already exists but is unreadable, will start over.";
				JOptionPane.showMessageDialog(this, err);
			}
		}
		
		CoregulationFileCollection coregFiles = null;
		if (preexistingStudy != null)
		{
			Organism org = preexistingStudy.getOrganism();
			assert org != null  :  "null organism";
			organismCombo.setSelectedItem(org);
			coregFiles = preexistingStudy.getCoregFiles();
		}
		
		// Build a structure editor for the primary spreadsheet.
		assert structureEditor == null;
		try
		{
			SpreadsheetStructure structure = (preexistingStudy != null)  ?  
				preexistingStudy.getPrimarySpreadsheetStructure()  :
				null;
			structureEditor = new SpreadsheetStructureEditor(f, structure, fileChooser, true);
		}
		catch (IOException x)
		{
			String err = "Couldn't open spreadsheet file " + f.getAbsolutePath();
			JOptionPane.showMessageDialog(this, err);
			return;
		}
		borderize(structureEditor);
		stageToComponents.get(currentStage).add(structureEditor);
		add(structureEditor);
		validate();	
	}
	

	// Serializes a Study instance.
	private void applySpreadsheetStructureEdits()
	{
		assert currentStage == DexterWizardStage.IMPORT;
		
		// Build and serialize study object.
		Organism organism = (Organism)organismCombo.getSelectedItem();
		Vector<SpreadsheetStructure> structures = 
			structureEditor.getAllSpreadsheetStructures();	// primary is first
		Study study = new Study(organism, structures);
		try
		{
			study.serialize(IMPORTED_STUDIES_DIRF);
		}
		catch (IOException x)
		{
			String err = "Can't serialize study to " + IMPORTED_STUDIES_DIRF.getAbsolutePath();
			JOptionPane.showMessageDialog(this, err);
			x.printStackTrace(System.out);
			return;
		}
		
		terminateImporting();
	}
	
	
	private void terminateImporting()
	{
		remove(structureEditor);
		stageToComponents.remove(structureEditor);
		structureEditor = null;
		validate();
		repaint();
		browseForImportBtn.setEnabled(true);
		browseForSupplementalImportBtn.setEnabled(false);
		applyImportBtn.setEnabled(false);
		cancelImportBtn.setEnabled(false);
	}
	
	
	private void buildImportSelectionPanel()
	{
		assert currentStage == DexterWizardStage.SELECT_IMPORTS;
		
		// Remove old.
		if (importSelPan != null)
		{
			assert stageToComponents.get(currentStage).contains(importSelPan);
			stageToComponents.get(currentStage).remove(importSelPan);
			remove(importSelPan);
		}
		
		// Build and install new.
		importSelPan = new ImportSelectionPanel(IMPORTED_STUDIES_DIRF, this);
		Border b = BorderFactory.createLineBorder(Color.BLACK, 3);
		b = BorderFactory.createTitledBorder(b, "Select imported studies for this session");
		importSelPan.setBorder(b);
		recordComponentForStage(importSelPan, currentStage);
		add(importSelPan);
		validate();
		repaint();
	}
	
	
	void deleteImports(Collection<File> importFilesToDelete)
	{
		for (File file: importFilesToDelete)
			file.delete();
		buildImportSelectionPanel();
	}
	
	
	
	
	
	
	
	
					
					////////////////////////////////////////////////////////////////////
					//                                                                //
					//                     GENERAL SCHEDULING UTILS                   //
					//                                                                //
					////////////////////////////////////////////////////////////////////
					


	

	
	private Vector<IndexedPinDraggable> buildPhaseChangePinsForBackgroundModel(GraphBackgroundStyle style)
	{
		Vector<IndexedPinDraggable> ret = new Vector<IndexedPinDraggable>();
		Vector<Integer> changes = backgroundModel.getPhaseChanges(style);
			
		if (changes == null  ||  changes.isEmpty())
			return ret;
		
		for (int i=0; i<changes.size(); i++)
		{
			Integer change = changes.get(i);
			boolean top = style == GraphBackgroundStyle.DL;
			IndexedPinDraggable pin = new IndexedPinDraggable(i, ""+change, change, top);
			pin.setStyle(style);
			setMinAndMaxForPin(pin, style);
			ret.add(pin);
		}
		return ret;
	}
	
	
	private void setMinAndMaxForPin(IndexedPinDraggable pin, GraphBackgroundStyle style)
	{
		int[] range = backgroundModel.getToleranceForChange(style, pin.getIndex());
		pin.setMinMaxXGrids(range[0] + REF_LEFT_MARGIN_GRIDS, range[1] + REF_LEFT_MARGIN_GRIDS);
	}
	
	
	private DLPhaseUseStrip getDLPhaseUseStrip()
	{
		Iterator<Component> iter = stageToComponents.get(DexterWizardStage.PHASES).iterator();
		while (iter.hasNext())
		{
			Component c = iter.next();
			if (c instanceof DLPhaseUseStrip)
				return (DLPhaseUseStrip)c;
		}
		return null;
	}


	// Returns null if the scroll pane doesn't exist yet. Assumes its the only JScrollPane
	// instance in ALIGN phase.
	private JScrollPane getAssignScrollPane()
	{
		assert currentStage == DexterWizardStage.ALIGN;

		JScrollPane ret = null;
		int nInstances = 0;
		for (Component c: stageToComponents.get(currentStage))
		{
			if (c instanceof JScrollPane)
			{
				ret = (JScrollPane)c;
				nInstances++;
			}
		}
		assert nInstances <= 1  :  "Too many assign scroll panes: expected 1, saw " + nInstances;
		return ret;
	}


	private Set<VerticalPinDraggable> collectPinsForStage(DexterWizardStage stage)
	{
		Set<VerticalPinDraggable> ret = new HashSet<VerticalPinDraggable>();
		for (Draggable d: stageToDraggables.get(stage))
			if (d instanceof VerticalPinDraggable)
				ret.add((VerticalPinDraggable)d);
		return ret;
	}


	// Returns an empty set (not null) if style isn't displayed.
	private Set<IndexedPinDraggable> collectPhasePinsForStyle(GraphBackgroundStyle style)
	{
		Set<IndexedPinDraggable> ret = new HashSet<IndexedPinDraggable>();
		for (VerticalPinDraggable pin: collectPinsForStage(DexterWizardStage.PHASES))
		{
			IndexedPinDraggable ipin  = (IndexedPinDraggable)pin;
			if (ipin.getStyle() == style)
				ret.add(ipin);
		}
		return ret;
	}


	// When both styles are displayed, pins should be half as long as the reference graph height,
	// and lower pins should be brick red.
	private void adjustPhasePinLengthsAndColors()
	{
		int nDisplayedStyles = 0;
		for (GraphBackgroundStyle style: GraphBackgroundStyle.values())
			if (backgroundModel.getUsesStyle(style))
				nDisplayedStyles++;

		switch (nDisplayedStyles)
		{
			case 0:
				return;
			case 1:
				for (VerticalPinDraggable pin: collectPinsForStage(DexterWizardStage.PHASES))
				{
					pin.setLineLength(getRefBoundsPix().height);
					pin.setLineColor(Color.BLACK);
				}
				break;
			case 2:
				for (VerticalPinDraggable pin: collectPinsForStage(DexterWizardStage.PHASES))
				{
					IndexedPinDraggable ipin = (IndexedPinDraggable)pin;
					ipin.setLineLength(getRefBoundsPix().height/2);
					Color lineColor = (ipin.getStyle() == GraphBackgroundStyle.DL)  ?
							Color.black  :  SECONDARY_PIN_LINE_COLOR;
					ipin.setLineColor(lineColor);
				}
				break;
			}
	}


	// Works whether or not the strips are contained in a scrollpane.
	private StudyPreviewStrip getPreviewStripForStudy(Study study)
	{
		for (Component c: stageToComponents.get(DexterWizardStage.ALIGN))
		{
			if (c instanceof AlignStudySelectionPanel)
				return ((AlignStudySelectionPanel)c).getPreviewStripForStudy(study);
			else if (c instanceof JScrollPane)
			{
				JScrollPane spane = (JScrollPane)c;
				AlignStudySelectionPanel kid = (AlignStudySelectionPanel)spane.getViewport().getView();
				return kid.getPreviewStripForStudy(study);
			}
		}					
		return null;
	}


	private int getReferenceDuration()
	{
		return (backgroundModel != null)  ?  backgroundModel.getDuration()  :  REF_DFLT_DURATION_HOURS;
	}


	private void setReferenceDuration(int duration)
	{
		assert backgroundModel != null  :  "null background model";
		backgroundModel.setDuration(duration);
	}


	private IndexedPinDraggable getDurationPin()
	{
		return (IndexedPinDraggable)stageToDraggables.get(DexterWizardStage.DURATION).iterator().next();
	}


	private AlignStudyControlPanel getCurrentAlignStudyControlPanel()
	{
		assert currentStage == DexterWizardStage.ALIGN;

		for (Component c: stageToComponents.get(currentStage))
			if (c instanceof AlignStudyControlPanel)
				return (AlignStudyControlPanel)c;
		return null;
	}


	// Return value isn't guaranteed if DL isn't used.
	boolean getStartsDark()
	{
		return backgroundModel.getStartsDark();
	}


	boolean getAlignDoesSnap()
	{
		return getCurrentAlignStudyControlPanel().getDoesSnap();
	}


	// To be called after all editing. The whole point of this class.
	public Map<Study, TimeAssignmentMap> getAllTimeAssignments()
	{
		return studyToEditedTimeAssignmentMap;
	}
	
	
	static int getMaxPhases()
	{
		return MAX_PHASES;
	}
	

				
	
	
	
	
	
				//////////////////////////////////////////////////////////////////////
				//                                                                  //
				//                     SCHEDULING UTILS: ALIGNING                   //
				//                                                                  //
				//////////////////////////////////////////////////////////////////////
				
	
	
	
	//
	// Sent by an instance of StudyPreviewStrip in response to a click on the ALIGN button.
	//
	void startAlign(Study study)
	{
		assert currentStage == DexterWizardStage.ALIGN;
		assert getCurrentAlignStudyControlPanel() == null;
		assert mappingGraphPainter == null;
				
		// Make sure there's an assignment map to be edited.
		TimeAssignmentMap assignmentMapToBeEdited = studyToEditedTimeAssignmentMap.get(study);
		if (assignmentMapToBeEdited == null)
		{
			Vector<String> colNames = study.getTimepointColumnNames();
			assignmentMapToBeEdited = new TimeAssignmentMap(colNames);
			assignmentMapToBeEdited.setLatestTimepoint(backgroundModel.getDuration());
			studyToEditedTimeAssignmentMap.put(study, assignmentMapToBeEdited);
		} 
		
		// Create a painter for the study.
		TimeAssignmentMap inferredTimeAssignments = studyToInferredTimeAssignmentMap.get(study);
		GridPoint painterOriginGrids = new GridPoint(REF_LEFT_MARGIN_GRIDS, getRefBottomGrids()+2);
		Point painterOriginPix = gridPointToPix(painterOriginGrids);
		int duration = (int)Math.round(Math.min(inferredTimeAssignments.getLatestTimepoint()+1, 50));
		mappingGraphPainter = new MappingStudyGraphPainter(painterOriginPix, duration, 
														   getGridWidth(), 3*getGridHeight(), 
														   inferredTimeAssignments);
		
		// Create a control panel.
		AlignStudyControlPanel pan = new AlignStudyControlPanel(study);
		pan.setBorder(BorderFactory.createLineBorder(Color.BLACK));
		recordComponentForStage(pan, currentStage);
		assert getCurrentAlignStudyControlPanel() != null;
		add(pan);
		validate();
		repaint();
		
		// Can't start aligning another study until this one is completed.
		for (Study s: sessionStudies)
		{
			StudyPreviewStrip strip = getPreviewStripForStudy(s);
			assert strip != null  :  "startAlign for " + study.getName() + ": no preview strip";
			strip.disableButtonsAndPushEnableStates();
		}
	}
	
	
	void shareAlignment(Study study, StudyList cohort)
	{
		assert cohort.contains(study);
		assert cohort.size() > 1;
		
		TimeAssignmentMap shareMe = studyToEditedTimeAssignmentMap.get(study);
		for (Study receivingStudy: cohort)
			if (receivingStudy != study)
				studyToEditedTimeAssignmentMap.put(receivingStudy, shareMe);
		
		for (Study s: cohort)
		{
			StudyPreviewStrip strip = getPreviewStripForStudy(s);
			assert strip != null;
			strip.setEnableShare(false);
		}
	}
	
	
	private boolean canInterpolate()
	{
		assert getCurrentAlignStudyControlPanel() != null;
		assert getCurrentAlignStudyControlPanel().getStudy() != null; 
		return canInterpolate(getCurrentAlignStudyControlPanel().getStudy());
	}
	
	
	private boolean canInterpolate(Study study)
	{
		assert study != null;
		return !collectInterpolationRuns(study).isEmpty();		
	}
	
	
	// Each member of the returned object is a vector of >= 3 timepoint names. The first and last vector
	// members are mapped timepoints. The other members are not mapped, hence eligible for interpolation.
	private Vector<Vector<String>> collectInterpolationRuns(Study study)
	{
		// Meez.
		TimeAssignmentMap inferredAssignments = studyToInferredTimeAssignmentMap.get(study);
		Vector<String> allTimepoints = new Vector<String>(inferredAssignments.keySet());
		TimeAssignmentMap editedAssignments = studyToEditedTimeAssignmentMap.get(study);
		Vector<String> mappedTimepoints = new Vector<String>(editedAssignments.keySet());
		Vector<Vector<String>> ret = new Vector<Vector<String>>();
		
		// Check each pair of adjacent mapped timepoints.
		for (int n=0; n<mappedTimepoints.size()-1; n++)
		{
			String tp0 = mappedTimepoints.get(n);
			String tp1 = mappedTimepoints.get(n+1);
			int i0 = allTimepoints.indexOf(tp0);
			int i1 = allTimepoints.indexOf(tp1);
			assert i1 > i0  :  
				tp0 +"@" + i0 + ", " + tp1 + "@" + i1 + 
				"\nINFERRED: " + inferredAssignments + "\nEDITED: " + editedAssignments;
			if (i1-i0 == 1)
				continue;		// timepoints are adjacent in original dataset, so nothing is between them
			Vector<String> vec = new Vector<String>();
			for (int i=i0; i<=i1; i++)
				vec.add(allTimepoints.get(i));
			ret.add(vec);
		}

		return ret;
	}

	
	// Updates the edited time assignment map for the study.
	private void interpolateForStudy(Study study)
	{
		Vector<Vector<String>> runs = collectInterpolationRuns(study);
		TimeAssignmentMap rawTam = studyToInferredTimeAssignmentMap.get(study);
		TimeAssignmentMap editedTam = studyToEditedTimeAssignmentMap.get(study);
		for (Vector<String> run: runs)
		{
			assert run.size() >= 3;
			Vector<Float> fvec = new Vector<Float>();
			for (String timept: run)
			{
				fvec.add(new Float(rawTam.get(timept)));
				if (fvec.size() > 1)
					assert fvec.lastElement() > fvec.get(fvec.size()-2)  :  
						"Not monotonic: " + fvec.lastElement() + " !< " + fvec.get(fvec.size()-2);
			}
			float editedStart = editedTam.get(run.firstElement());
			float editedEnd = editedTam.get(run.lastElement());
			BatchInterpolator interpolated = new BatchInterpolator(fvec, editedStart, editedEnd);
			assert interpolated.size() == run.size();
			for (int i=1; i<run.size()-1; i++)
			{
				String timept = run.get(i);
				Float hours = interpolated.get(i);
				assert hours > interpolated.get(i-1);
				editedTam.put(timept, hours);
			}
		}
	}
	
	
	private FloatRange getLegalMappedRangeForTimepoint(String timepoint)
	{
		FloatRange ret = new FloatRange(0, true, getReferenceDuration(), true);
		
		// Fetch inferred and edited assignment maps.
		Study study = getCurrentAlignStudyControlPanel().getStudy();
		TimeAssignmentMap inferredAssignments = studyToInferredTimeAssignmentMap.get(study);
		assert inferredAssignments.containsKey(timepoint);
		if (!inferredAssignments.containsKey(timepoint))
			return null;
		TimeAssignmentMap editedAssignments = studyToEditedTimeAssignmentMap.get(study);
		
		// Find next-earlier mapped timepoint.
		Vector<String> allTPsChronological = new Vector<String>(inferredAssignments.keySet());
		int indexOfTP = allTPsChronological.indexOf(timepoint);
		assert indexOfTP >= 0;
		String nextEarlierMappedTP = null;
		if (indexOfTP > 0)
		{
			for (int i=indexOfTP-1; i>=0; i--)
			{
				String earlierTP = allTPsChronological.get(i);
				if (editedAssignments.containsKey(earlierTP))
				{
					nextEarlierMappedTP = earlierTP;
					break;
				}
			}
		}
		if (nextEarlierMappedTP != null)
		{
			ret.min = editedAssignments.get(nextEarlierMappedTP);
			ret.minIsInclusive = false;
		}
		
		// Find next-later mapped timepoint.
		String nextLaterMappedTP = null;
		if (indexOfTP < allTPsChronological.size()-1)
		{
			for (int i=indexOfTP+1; i<allTPsChronological.size(); i++)
			{
				String laterTP = allTPsChronological.get(i);
				if (editedAssignments.containsKey(laterTP))
				{
					nextLaterMappedTP = laterTP;
					break;
				}
			}
		}
		if (nextLaterMappedTP != null)
		{
			ret.max = editedAssignments.get(nextLaterMappedTP);
			ret.maxIsInclusive = false;
		}
		
		return ret;
	}
	
	
	private boolean xyIsLegalForTimepointMapping(MouseEvent me, String timepoint)
	{
		if (!getRefBoundsPix().contains(me.getX(), me.getY()))
			return false;
		
		float hours = getAlignDoesSnap()  ?  xGridsToHours(snapPixToGrid(me).xGrids)  :  xPixToHours(me.getX());
		FloatRange legalRange = getLegalMappedRangeForTimepoint(timepoint);
		return legalRange.contains(hours);
	}
	
	
	private void dumpEditedTimeAssignments(String prefix)
	{
		sop("--------\n" + prefix);
		if (studyToEditedTimeAssignmentMap == null)
			sop("studyToEditedTimeAssignmentMap is null");
		else
		{
			sop("studyToEditedTimeAssignmentMap contains " + studyToEditedTimeAssignmentMap.size() + " mappings");
			for (Study study: studyToEditedTimeAssignmentMap.keySet())
				sop("  " + study.getName() + ": " + studyToEditedTimeAssignmentMap.get(study));
		}
		sop("==============");
	}
					
	
	
	
	
	
	
					
					
					////////////////////////////////////////////////////////////
					//                                                        //
					//                    PHASE TRANSITION                    //
					//                                                        //
					////////////////////////////////////////////////////////////
					

	
	

	// Returns null if approved, error message if denied. Doesn't do anything yet.
	public String isTransitionApproved(int oldStageIndex, int newStageIndex)
	{
		DexterWizardStage oldStage = DexterWizardStage.values()[oldStageIndex];
		assert oldStage == currentStage;
		DexterWizardStage newStage = DexterWizardStage.values()[newStageIndex];
		return null;
	}
	
	
	public void wizardStageChanged(WizardStageEvent e) 
	{		
		// Remove all components for previous stage.
		removeAll();
		
		// Leave previous stage. Harvest data for the session model that was generated during the stage.
		DexterWizardStage prevStage = DexterWizardStage.values()[e.getOldAndNewIndices()[0]];
		switch (prevStage)
		{
			case SELECT_IMPORTS:
				assert importSelPan != null;
				Vector<File> importFiles = importSelPan.getSelectedSerializedImportedStudyFiles();
				try
				{
					sessionStudies = new StudyList(importFiles);
				}
				catch (Exception x)
				{
					String err = "Couldn't read imported studies: " + x.getMessage();
					JOptionPane.showMessageDialog(this, err);
				}
				break;
		}
		
		// Prepare to enter next stage.
		currentStage = DexterWizardStage.values()[e.getOldAndNewIndices()[1]];
		switch (currentStage)
		{
			case SELECT_IMPORTS:
				buildImportSelectionPanel();
				break;
				
			case DURATION:
				if (backgroundModel == null)
					backgroundModel = new GraphBackgroundModel(REF_DFLT_DURATION_HOURS);
				break;
				
			case PHASES:
				int nDLPhases = backgroundModel.getNPhases(GraphBackgroundStyle.DL);
				getDLPhaseUseStrip().setNPhases(nDLPhases);
				getDLPhaseUseStrip().setStartsDark(backgroundModel.getStartsDark());
				break;
				
			case ALIGN:	
				// The inferred time assignments are derived from the spreadsheet column names for
				// each dataset. In the ALIGN phase they are used as a default before the user begins
				// editing, and for drawing the images of the dataset sin the preview panels. The edited 
				// assignments are driven by the user, either by input activity in the current session
				// or from a previously saved session.
				if (studyToInferredTimeAssignmentMap == null)
					studyToInferredTimeAssignmentMap = new StudyToTimeAssignmentMap();
				for (Study study: sessionStudies)
				{
					Vector<String> colNames = study.getTimepointColumnNames();
					TimeAssignmentMap inferredTimes = new TimeAssignmentMap(colNames);
					studyToInferredTimeAssignmentMap.put(study, inferredTimes);
				}
				if (studyToEditedTimeAssignmentMap == null)
				{
					studyToEditedTimeAssignmentMap = (sessionModel != null)  ?
						new StudyToTimeAssignmentMap(sessionModel.getStudyToTimeAssignmentMap())  :
						new StudyToTimeAssignmentMap();
				}
				if (getAssignScrollPane() == null)
				{
					AlignStudySelectionPanel studySelPan = new AlignStudySelectionPanel(this, sessionStudies);
					if (sessionModel != null)
					{
						// Mark studies that have already been aligned.
						Collection<Study> alignedStudies = sessionModel.getStudies();
						assert alignedStudies != null;
						assert !alignedStudies.isEmpty()  :  "No studies";
						for (Study study: alignedStudies)
							studySelPan.setStudyIsAligned(study, true);
					}
					JScrollPane spane = new JScrollPane(studySelPan, 
							ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS, 
							ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
					int spanePrefW = spane.getPreferredSize().width;
					spane.setPreferredSize(new Dimension(spanePrefW, 165));
					recordComponentForStage(spane, DexterWizardStage.ALIGN);
				}
				break;
				
			case ORTHOLOGS:
				// Orthology code needs to associate an organism with every orthologous gene.
				// Cleanest way is to read every spreadsheet, since user has associated an
				// organism with every spreadsheet and the spreadsheets contains every gene.				 
				GeneIdToOrganismMap geneIdToOrganism = new GeneIdToOrganismMap(sessionStudies);
				orthoPan = new OrthologyWizardPanel(geneIdToOrganism);
				if (sessionModel != null)
				{
					orthoPan.loadOrthologyFiles(sessionModel.getOrthologyFiles());
				}
				recordComponentForStage(orthoPan, DexterWizardStage.ORTHOLOGS);
				break;
				
			case COREGULATION:
				CoregulationFileCollection coregFiles = (sessionModel != null)  ?  
					sessionModel.getCoregulationFiles()  :  
					null;
				coregPan = new CoregulationWizardPanel(sessionStudies, coregFiles);
				recordComponentForStage(coregPan, DexterWizardStage.COREGULATION);
			
			case WRAP:
				break;
		}

		// Add stage-specific components.
		for (Component c: stageToComponents.get(currentStage))
			add(c);
		validate();
		repaint(); 
	}
	
	
	
	
	
	
					
					
					///////////////////////////////////////////////////////////////////////
					//                                                                   //
					//                      MISC, TOP LEVEL AND MAIN                     //
					//                                                                   //
					///////////////////////////////////////////////////////////////////////
					
	

	
	static JFileChooser buildFileChooser()
	{
		JFileChooser fileChooser = new JFileChooser(STUDIES_DIRF);
	    FileNameExtensionFilter filter = 
	    	new FileNameExtensionFilter("csv and tsv spreadsheets", "csv", "tsv");
	    fileChooser.setFileFilter(filter);
	    return fileChooser;
	}
	

	static int getPreferredWidth()
	{
		return PREF_W;
	}
	

	static int getPreferredHeight()
	{
		return PREF_H;
	}
	
	
	SessionModel buildSessionModel()
	{
		SessionModel session =  new SessionModel(studyToEditedTimeAssignmentMap, 
							    				 backgroundModel, 
							    				 (orthoPan != null)  ?  orthoPan.getOrthologyFiles()  :  null,
							    				 null);
		session.setCoregulationFiles(coregPan.getCoregulationFiles());
		return session;
	}
	
	
	void setWindowInvisible()
	{
		Component outermost = this;
		while (outermost.getParent() != null)
			outermost = outermost.getParent();
		outermost.setVisible(false);
	}
	
	
	public static File getDataDirf()
	{
		return DATA_DIRF;
	}
	
	
	public static void main(String[] args) 
	{
		DexterWizardDialog.main(args);
	}
}
