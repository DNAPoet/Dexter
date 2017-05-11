package dexter.util;

import java.util.*;


public class FloatBlender extends Stack<Float>
{
	public FloatBlender(float start, float end, int len)
	{
		push(start);
		float delta = (end - start) / (len - 1);
		while (size() < len)
			push(peek() + delta);
		pop();
		push(end);			// avoid rounding in last element
	}
}
