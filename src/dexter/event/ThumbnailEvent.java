package dexter.event;

import dexter.util.gui.ArmState;
import dexter.view.graph.*;


public class ThumbnailEvent 
{
	private ThumbnailGraph			thumbnail;
	private ArmState				armState;
	private boolean					requestExpansion;
	
	
	public ThumbnailEvent(ThumbnailGraph thumbnail, ArmState armState, boolean requestExpansion)
	{
		this.thumbnail = thumbnail;
		this.armState = armState;
		this.requestExpansion = requestExpansion;
	}
	
	
	public String toString()
	{
		return "ThumbnailEvent selected=" + armState + "  graph=" + thumbnail.getTitle();
	}
	
	
	public ThumbnailGraph getThumbnail()		{ return thumbnail;	       }
	public ArmState getArmState()				{ return armState;         }
	public boolean getDidRequestExpansion()		{ return requestExpansion; }
}
