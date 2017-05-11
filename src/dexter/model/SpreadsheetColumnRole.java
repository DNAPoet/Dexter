package dexter.model;


public class SpreadsheetColumnRole implements java.io.Serializable, Comparable<SpreadsheetColumnRole>
{
	private static final long 						serialVersionUID = -6938398863829979211L;
	
	private PredefinedSpreadsheetColumnRole			predefinedRole;		// exactly 1 of these ...
	private String									userDefinedRole;	// ... is not null
	
	
	public SpreadsheetColumnRole(PredefinedSpreadsheetColumnRole predefinedRole)
	{
		this.predefinedRole = predefinedRole;
	}
	
	
	public SpreadsheetColumnRole(String userDefinedRole)
	{
		assert userDefinedRole != null  &&  !userDefinedRole.isEmpty();
		this.userDefinedRole = userDefinedRole;
	}
	
	
	public String toString()
	{
		return (predefinedRole != null)  ?  predefinedRole.toString()  :  userDefinedRole;
	}
	
	
	public PredefinedSpreadsheetColumnRole getPredefinedRole()
	{
		return predefinedRole;
	}
	
	
	public String getUserDefinedRole()
	{
		return userDefinedRole;
	}
	
	public boolean isUserDefined()
	{
		return userDefinedRole != null;
	}
	
	public boolean isPredefined()
	{
		return predefinedRole != null;
	}

	
	public int compareTo(SpreadsheetColumnRole that) 
	{
		if (this.isPredefined() == that.isPredefined())
		{
			// Same type. Sort by natural order.
			if (this.isPredefined())
				return this.predefinedRole.compareTo(that.predefinedRole);
			else
				return this.userDefinedRole.compareTo(that.userDefinedRole);
		}
		
		else
		{
			// Different types. Predefined comes first.
			return (this.isPredefined())  ?  1  :  -1;
		}
	}
	
	
	public boolean isKEGG()
	{
		return predefinedRole == PredefinedSpreadsheetColumnRole.KEGG_PATHWAY;
	}
	
	
	public boolean equals(Object x)
	{
		SpreadsheetColumnRole that = (SpreadsheetColumnRole)x;
		if (this.isPredefined() != that.isPredefined())
			return false;
		else if (this.isPredefined())
			return this.predefinedRole.equals(that.predefinedRole);
		else
			return this.userDefinedRole.equals(that.userDefinedRole);
	}
	
	
	public int hashCode()
	{
		return isPredefined()  ?  predefinedRole.hashCode()  :  userDefinedRole.hashCode();
	}
	
	
	public static SpreadsheetColumnRole buildKEGG()
	{
		return new SpreadsheetColumnRole(PredefinedSpreadsheetColumnRole.KEGG_PATHWAY);
	}
}
