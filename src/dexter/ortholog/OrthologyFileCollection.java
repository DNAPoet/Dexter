package dexter.ortholog;

import java.util.*;
import java.io.File;


//
// File args might be inner subclasses of java.io.File, which messes up s11n when the containing class
// is also serialized. So all args are converted just to be sure.
//
// Separation by file type (list vs. blast) may no longer be necessary.
//


public class OrthologyFileCollection implements java.io.Serializable
{
	private static final long 	serialVersionUID 		= 6000007711122265705L;
	
	private Set<File> 			listFiles 				= new TreeSet<File>();
	private Set<File> 			tabularBLASTFiles 		= new TreeSet<File>();
	
	
	// General ctor.
	public OrthologyFileCollection()		{ }
	
	
	// Ctor for just 1 file.
	public OrthologyFileCollection(File file, boolean listNotTabular)
	{		
		file = new File(file.getAbsolutePath());
		
		if (listNotTabular)
			listFiles.add(file);
		else
			tabularBLASTFiles.add(file);
	}
	
	
	public String toString()
	{
		String s = "OrthologyFileCollection:\n  List files:";
		if (listFiles == null)
			s += " NONE";
		else
			for (File f: listFiles)
				s += " " + f.getName();
		s += "\n  BLAST files:";
		if (tabularBLASTFiles == null)
			s += " NONE";
		else
			for (File f: tabularBLASTFiles)
				s += " " + f.getName();
		return s;
	}
	
	
	public void addListFile(File f)
	{
		listFiles.add(new File(f.getAbsolutePath()));
	}
	
	
	public void addTabularBLASTFile(File f)
	{
		tabularBLASTFiles.add(new File(f.getAbsolutePath()));
	}
	
	
	public void removeFile(File f)
	{
		f = new File(f.getAbsolutePath());
		listFiles.remove(f);
		tabularBLASTFiles.remove(f);
	}
	
	
	public Vector<File> getListFiles()
	{
		return new Vector<File>(listFiles);
	}
	
	
	public Vector<File> getTabularBLASTFiles()
	{
		return new Vector<File>(tabularBLASTFiles);
	}
	
	
	public boolean isListFile(File f)
	{
		return listFiles.contains(new File(f.getAbsolutePath()));
	}
	
	
	public boolean isTabularBLASTFile(File f)
	{
		return listFiles.contains(new File(f.getAbsolutePath()));
	}
	
	
	public Iterator<File> iterator()
	{
		Vector<File> vec = new Vector<File>();
		vec.addAll(listFiles);
		vec.addAll(tabularBLASTFiles);
		return vec.iterator();
	}
	
	
	public int size()
	{
		return listFiles.size() + tabularBLASTFiles.size();
	}
}
