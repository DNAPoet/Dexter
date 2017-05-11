package dexter.event;

public interface ThumbnailListener 
{
	public void thumbnailSelectionChanged(ThumbnailEvent e);
	public void thumbnailRequestedExpansion(ThumbnailEvent e);
}
