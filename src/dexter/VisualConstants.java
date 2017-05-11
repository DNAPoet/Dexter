package dexter;

import java.awt.Color;
import java.awt.Paint;

import dexter.util.gui.PaintFactory;


public interface VisualConstants 
{
	public final static char			CHECKMARK				= '\u2714';
	public final static String			S_CHECKMARK				= "" + CHECKMARK;
	public final static char			XMARK					= '\u2718';
	public final static String			S_XMARK					= "" + XMARK;
	public final static char			CLOCK					= '\u231a';
	public final static String			S_CLOCK					= "" + CLOCK;
	public final static char			LEFT_ARROW				= '\u2190';
	public final static char			RIGHT_ARROW				= '\u2192';
	public final static char			DOT_DOT_DOT				= '\u2026';
	public final static String			S_DOT_DOT_DOT			= "" + DOT_DOT_DOT;
		
	public final static Color			SELECTION_COLOR			= Color.CYAN;
	public final static Color			ARM_COLOR				= Color.YELLOW;	
	public final static Color			BRICK_RED				= new Color(142, 0, 0);	
	public final static Color			PURPLE					= new Color(150, 0, 255);
	public final static Color			DARK_GREEN				= new Color(0, 200, 0);
	public final static Color			VERY_DARK_GREEN			= new Color(0, 140, 0);
	public final static Color			NEUTRAL_BROWN			= new Color(24, 100, 80);
	public final static Color			VERY_LIGHT_GRAY			= new Color(233, 233, 233);
	public final static Color			CLEAR_COLOR				= new Color(255, 255, 255, 0);
	public final static Color			PALE_CYAN				= new Color(150, 255, 255);
	public final static Color[] 		DFLT_GENE_COLORS 		=
	{
		Color.BLUE, Color.RED, DARK_GREEN, Color.BLACK, PURPLE, Color.ORANGE, Color.MAGENTA, BRICK_RED
	};
	
	public final static Paint 			FORBIDDEN_ZONE_PAINT 	= 
		PaintFactory.makeDiagonalTexturePaint(Color.LIGHT_GRAY, Color.DARK_GRAY, 20);
}
