package dexter.view.wizard;

import java.util.Vector;
import dexter.util.StringUtils;


enum DexterWizardStage 
{
	IMPORT("Import\nstudies"), 
	SELECT_IMPORTS("Select\nstudies"),
	DURATION("Duration"), 
	PHASES("Dark-light\nphases"), 
	ALIGN("Align\nschedules"),
	ORTHOLOGS("Orthologs"),
	COREGULATION("Coregulation"),
	WRAP("Wrap");
	
	
	private String			longNameWithNewlines;
	
	
	DexterWizardStage(String longNameWithNewlines)
	{
		this.longNameWithNewlines = longNameWithNewlines;
	}
	
	
	public String getLongNameWithNewlines()
	{
		return longNameWithNewlines;
	}
	
	
	public static Vector<String[]> getLongNames()
	{
		Vector<String[]> ret = new Vector<String[]>();
		for (DexterWizardStage stage: values())
			ret.add(StringUtils.splitOnLineBreaks(stage.longNameWithNewlines));
		return ret;
	}
	
	
	boolean usesGrid()
	{
		return this == DURATION  ||  this == PHASES  ||  this == ALIGN;
	}
	
	
	public static void main(String[] args)
	{
		DexterWizardDialog.main(args);
	}
}
