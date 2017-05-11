package dexter.cluster;


//
// The newick parser needs to build a Node<T> from a String read from a newick file. Typically
// T isa String or Gene.
//

public interface NodePayloadBuilder<T> 
{
	public T buildPayload(String src);
}
