package dexter.view.graph;

public enum GraphBackgroundStyle 
{
	DL, TREATMENT;
	
	
	public String toString()
	{
		switch (this)
		{
			case DL:
				return "dark/light";
				
			case TREATMENT:
				return "treatment";
		}
		return null;
	}
}
