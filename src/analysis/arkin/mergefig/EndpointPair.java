package analysis.arkin.mergefig;

class EndpointPair 
{
	int x0;
	int y0;
	int x1;
	int y1;
	
	// E.g. "48,2,120,6"
	EndpointPair(String csv)
	{
		String[] pieces = csv.split(",");
		assert pieces.length == 4;
		x0 = Integer.parseInt(pieces[0]);
		y0 = Integer.parseInt(pieces[1]);
		x1 = Integer.parseInt(pieces[2]);
		y1 = Integer.parseInt(pieces[3]);
	}
	
	
	public String toString()
	{
		return x0 + "," + y0 + "," + x1 + "," + y1;
	}
	
	
	public static void main(String[] args)
	{
		String s = "12,34,56,789";
		EndpointPair ep = new EndpointPair(s);
		System.out.println(s + "\n" + ep);
	}
}
