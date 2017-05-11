package dexter.proximity;

import java.io.*;
import dexter.model.Organism;


public class ProximityKit implements Serializable
{
	private static final long 			serialVersionUID 	= 8921453022389035736L;

	public final static ProximityKit	FOR_CROCO			= 
		new ProximityKit(ProximityFileFormat.FOR_CROCO, ProximityFileFormat.CROCO_FILE);
	public final static ProximityKit	FOR_MED4			= 
		new ProximityKit(ProximityFileFormat.FOR_MED4, ProximityFileFormat.MED4_FILE);
	public final static ProximityKit	FOR_TERY			= 
		new ProximityKit(ProximityFileFormat.FOR_TERY, ProximityFileFormat.TERY_FILE);
	
	
	public ProximityFileFormat			format;
	public File							file;
	
	
	public ProximityKit(ProximityFileFormat format, File file)
	{
		this.format = format;
		this.file = file;
	}
	
	
	public static ProximityKit forProvidedOrganism(Organism org)
	{
		if (org.equals(Organism.CROCO))
			return FOR_CROCO;
		else if (org.equals(Organism.PRO))
			return FOR_MED4;
		else if (org.equals(Organism.TERY))
			return FOR_TERY;
		assert false;
		return null;
	}
}
