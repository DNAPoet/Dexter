package dexter.util;

import java.io.*;
import java.util.Stack;


public class PushbackLineReader extends BufferedReader 
{
	private Stack<String>		pushbackStack;
	
	
	public PushbackLineReader(Reader src) throws IOException
	{
		super(src);
		pushbackStack = new Stack<String>();
	}
	
	
	public String readLine() throws IOException
	{
		return  pushbackStack.isEmpty()  ?  super.readLine()  :  pushbackStack.pop();
	}
	
	
	public String peek() throws IOException
	{
		if (pushbackStack.isEmpty())
		{
			String s = readLine();
			if (s != null)
				pushbackStack.push(s);
			return s;
		}
		
		else
			return pushbackStack.peek();
	}
	
	
	public void push(String s)			{ pushbackStack.push(s); }	
}
