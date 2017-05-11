package dexter.model;

import java.util.*;

import dexter.view.graph.*;


public class SelectionModel 
{
	private Collection<ThumbnailStrip>			strips;
	private Collection<ThumbnailGraph>			thumbnails;
	
	
	public SelectionModel()
	{
		strips =  new HashSet<ThumbnailStrip>();
		thumbnails = new HashSet<ThumbnailGraph>();
	}
	
	
	public SelectionModel(Collection<ThumbnailStrip> strips, Collection<ThumbnailGraph> thumbnails)
	{
		this.strips = strips;
		this.thumbnails = thumbnails;
	}
	
	
	public Collection<ThumbnailStrip> getStrips()
	{
		return strips;
	}
	
	
	public Collection<ThumbnailGraph> getThumbnails()
	{
		return thumbnails;
	}
	
	
	public void addStrip(ThumbnailStrip strip)
	{
		strips.add(strip);
	}
	
	
	public void addThumbnail(ThumbnailGraph thumbnail)
	{
		thumbnails.add(thumbnail);
	}
}
