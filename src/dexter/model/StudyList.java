package dexter.model;

import java.util.*;
import java.io.*;
import dexter.util.HashMapIgnoreKeyCase;


public class StudyList extends Vector<Study>
{
	private String 				name;
	
	
	public StudyList()		{ }
	
	
	public StudyList(String name)
	{
		this.name = name;
	}
	
	
	public StudyList(Collection<File> importSerFiles) throws Exception
	{
		for (File file: importSerFiles)
			add(Study.deserialize(file));
	}
	
	
	public String getName()
	{
		return name;
	}
	
	
	public void generateName()
	{
		name = firstElement().getName();
		if (size() > 1)
			name += "...";
	}
	
	
	public Map<Study, HashMapIgnoreKeyCase<Gene>> getNameToGeneMapsByStudyIgnoreNameCase()
	{
		Map<Study, HashMapIgnoreKeyCase<Gene>> ret = new HashMap<Study, HashMapIgnoreKeyCase<Gene>>();
		for (Study study: this)
			ret.put(study, study.getNameToGeneMap());		
		return ret;
	}
	
	
	public Map<Study, Map<String, Gene>> getIdToGeneMapsByStudy()
	{
		Map<Study, Map<String, Gene>> ret = new HashMap<Study, Map<String,Gene>>();
		for (Study study: this)
			ret.put(study, study.getIdToGeneMap());		
		return ret;
	}
}
