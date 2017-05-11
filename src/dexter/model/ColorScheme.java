package dexter.model;

public enum ColorScheme 
{
	Gene, Study, Organism, Pathway, Addition_order, All_blue, All_red, BlueRed;
	
	
	public String toString()
	{
		if (this == BlueRed)
			return "Blue/Red";
		else if (name().contains("_"))
		{
			String[] pieces = name().split("_");
			return pieces[0] + " " + pieces[1];
		}
		else
			return super.toString();
	}
}
