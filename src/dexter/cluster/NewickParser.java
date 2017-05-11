package dexter.cluster;

import java.util.*;
import java.io.*;


//
// T is the payload type of the nodes. Typically the payloads will be genes, but could be another
// type, e.g. when debugging. This class doesn't use payloads.
//
// The payload builder provides conversion from strings read from the newick file to nodes of type T. 
//


public class NewickParser<T> 
{
	NodePayloadBuilder<T> 		payloadBuilder;
	private String				newickNoWhitespace;
	private TokenChain			tokens;
	
	
	public NewickParser(String src, NodePayloadBuilder<T> payloadBuilder)
	{
		this.payloadBuilder = payloadBuilder;
		init(src);
	}
	
	
	public NewickParser(File ifile, NodePayloadBuilder<T> payloadBuilder) throws IOException
	{
		this.payloadBuilder = payloadBuilder;
		
		FileReader fr = new FileReader(ifile);
		BufferedReader br = new BufferedReader(fr);
		String newick = "";
		String line = null;
		while ((line = br.readLine()) != null)
			newick += line;
		br.close();
		fr.close();
		init(newick);
	}
	
		
	private void init(String src)
	{
		// Remove whitespace.
		StringBuilder sb = new StringBuilder();
		for (int i=0; i<src.length(); i++)
		{
			char ch = src.charAt(i);
			if (!Character.isWhitespace(ch))
				sb.append(src.charAt(i));
		}
		newickNoWhitespace = sb.toString();
	}
	
	
	private enum TokenType	
	{
		OP, CP, NAME, COMMA, COLON, DIST, SEMI, SUBTREE;
	
		static TokenType valueOf(char ch)
		{
			switch (ch)
			{
				case '(':		return OP;
				case ')':		return CP;
				case ':':		return COLON;
				case ';':		return SEMI;
				case ',':		return COMMA;
			}
			if (ch == '.'  ||  (ch >= '0'  &&  ch <= '9')  ||  ch == '-')
				return DIST;
			if (Character.isJavaIdentifierStart(ch))
				return NAME;
			throw new IllegalArgumentException("Illegal char '" + ch + "'");
		}
		
		boolean isValidSubsequentChar(char ch)
		{
			switch (this)
			{
				case DIST:
					return Character.isDigit(ch)  ||  ch == '.'  ||  ch == 'e'  ||  ch == 'E'  ||  ch == '+'  ||  ch == '-';
				case NAME:		
					return Character.isDigit(ch)  ||  Character.isJavaIdentifierStart(ch)  ||  ch == '|'  ||  ch == '-';
				default:
					return false;
			}
		}
		
		boolean isMultiChar()	{ return this == NAME  ||  this == DIST; }
	}
	
	
	private class Token
	{
		TokenType		type;
		String			sval;		// name or distance
		Token 			prev;
		Token			next;
		
		Token(TokenType type)		{ this(type, null); }
		
		Token(TokenType type, String sval)
		{
			this.type = type;
			this.sval = sval;
		}
		
		public String toString()
		{
			switch (type)
			{
				case OP:		return "(";
				case CP:		return ")";
				case COLON:		return ":";
				case SEMI:		return ";";
				case COMMA:		return ",";
				case SUBTREE:	return "Subtree";
				default:		return sval;
			}
		}
	}  // End of inner class Token
	
	
	private class TokenChain
	{
		Token			head;
		Token			tail;
		
		public String toString()
		{
			String s = "";
			Token t = head;
			while (t != null)
			{
				s += t + " ";
				t = t.next;
			}
			return s.trim();
		}
		
		void append(Token t)
		{
			t.next = t.prev = null;
			if (head == null)
				head = tail = t;
			else
			{
				t.prev = tail;
				tail.next = t;
				tail = t;
			}
		}
		
		void removeTail()
		{
			if (tail == null)
				toss("Can't remove tail from empty chain");
			tail = tail.prev;
			if (tail == null)
				head = null;
			else
				tail.next = null;
		}
	}  // End of inner class TokenChain

	
	private TokenChain tokenize(String newick)
	{
		TokenChain ret = new TokenChain();
		for (int i=0; i<newick.length(); i++)
		{
			char ch = newick.charAt(i);
			if (Character.isWhitespace(ch))
				continue;
			TokenType type = TokenType.valueOf(ch);
			Token tok = new Token(type);
			ret.append(tok);
			if (type.isMultiChar())
			{
				// Multi-char token: name or distance.
				tok.sval = "" + ch;
				while (type.isValidSubsequentChar(newick.charAt(i+1)))
					tok.sval += newick.charAt(++i);
			}
		}
		
		return ret;
	}
	
	
	public Node<T> parse()
	{
		// Tokenize.
		tokens = tokenize(newickNoWhitespace);
		
		// Degenerate case.
		if (tokens.head == null)
			return null;
		
		// Strip terminal semicolon.
		if (tokens.tail.type != TokenType.SEMI)
			toss("Must end with semicolon");
		tokens.removeTail();	
		
		// All trees consist of { spec:distance } except the outermost tree, which usually has no distance.
		// If there's no distance for the outermost tree, append one for consistency (it will be ignored).
		if (tokens.tail.type != TokenType.DIST)
		{
			if (tokens.tail.prev.type == TokenType.COLON)
				toss("Illegal end");
			tokens.append(new Token(TokenType.COLON));
			tokens.append(new Token(TokenType.DIST, "1234567"));
		}
		
		// Token chain is non-trivial and at least partly valid.
		Node<T> tree = parseSubtree(tokens.head, tokens.tail);
		assert tree.distToParent == 1234567f;
		tree.distToParent = 0f;
		return tree;
	}
	
	
	// Subtree format is (xx,xx,xx,xx):dist or name:dist
	// Distance is present even in top-level tree because it was artificially appended after tokenizing.
	private Node<T> parseSubtree(Token tStart, Token tEnd)
	{		
		if (tEnd.type != TokenType.DIST  ||  tEnd.prev.type != TokenType.COLON)
			toss("No colon-distance in " + toString(tStart, tEnd));
		float distanceToParent = Float.parseFloat(tEnd.sval);
		tEnd = tEnd.prev.prev;
		
		// The returned node will get populated with children unless it represents a leaf.
		Node<T> ret = new Node<T>(tStart.sval, distanceToParent);
		ret.setPayload(payloadBuilder.buildPayload(tStart.sval));
		if (tStart == tEnd  &&  tStart.type == TokenType.NAME)
			return ret;		// leaf
			
		// Interior is a comma-separated list of leaf or composite (subtree) nodes.
		if (tStart.type != TokenType.OP  ||  tEnd.type != TokenType.CP)
			toss("Expected parens around " + toString(tStart, tEnd));
		Vector<TokenChain> pieces = splitAtCommas(tStart.next, tEnd.prev);
		
		// Evaluate comma-delimited pieces.
		for (TokenChain piece: pieces)
		{
			Node<T> subtree = parseSubtree(piece.head, piece.tail);
			ret.addKid(subtree);
		}
		
		return ret;
	}
	
	
	private void toss(String err) throws IllegalArgumentException
	{
		throw new IllegalArgumentException(err);
	}
	
	
	private String toString(Token tStart, Token tEnd)
	{
		String s = "";
		Token t = tStart;
		while (t != tEnd)
		{
			s += t + "/";
			t = t.next;
		}
		s += tEnd;
		return s;
	}
	
	
	// Only split at commas at level 0 (i.e. ignore commas within parens).
	private Vector<TokenChain> splitAtCommas(Token tStart, Token tEnd)
	{
		Vector<TokenChain> ret = new Vector<TokenChain>();
		ret.add(new TokenChain());
				
		int level = 0;
		Token afterEnd = tEnd.next;
		Token t = tStart;
		do
		{
			assert t != null  :  "Null token";
			assert t.type != null  :  "Null type in token";
			if (t.type == TokenType.COMMA  &&  level == 0)
			{
				ret.add(new TokenChain());
				t = t.next;
			}
			else
			{
				if (t.type == TokenType.OP)
					level++;
				else if (t.type == TokenType.CP)
					level--;
				Token nextToken = t.next;		// appending clobbers "next" field
				ret.lastElement().append(t);
				t = nextToken;
			}
		}
		while (t != afterEnd);
		
		return ret;
	}

	
	static void sop(Object x)					{ System.out.println(x); }
	
	
	public static void main(String[] args)
	{
		try
		{
			//String s = "(A:1.234E-5,B:0.2,(X:0.3,Y:0.4):0.5);";		// from wikipedia example, no intermediate names
			String s = "(A:1,B:0.2,(X:0.3,Y:0.4):0.5);";		// from wikipedia example, no intermediate names
			//String s = "(  (AB-CD:-5, B:5):9,   ((C:8,D:8):2, E:10):4  );";			
			//File f = new File("data/Clusters/All_Croco_Pro_Tery_7457_gt_delta1.tre");
			//String s = "(NFIX|nifU:4.628191,FAKE:6);";
			//NewickParser<String> parser = new NewickParser<String>(f, new NewickPayloadBuilderStringIdentity());
			NewickParser<String> parser = new NewickParser<String>(s, new NewickPayloadBuilderStringIdentity());
			Node<String> tree = parser.parse();
			sop(tree.toStringWithIndent());
		}
		catch (Exception x)
		{
			sop("Stress: " + x.getMessage());
			x.printStackTrace();
		}
		sop("DONE");
	}
}
