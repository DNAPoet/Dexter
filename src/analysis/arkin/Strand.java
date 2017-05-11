package analysis.arkin;

public enum Strand 
{
	PLUS, MINUS;
	
	
	public String toString()
	{
		return (this == PLUS)  ?  "+"  :  "-";
	}
	
	
	static Strand valueOf(char ch)
	{
		assert ch == '+'  ||  ch == '-'  :  "Saw " + ch + ", must be + or -";
		return (ch == '+')  ?  PLUS  :  MINUS;
	}
}
