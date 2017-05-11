package dexter.cluster;


public class NewickPayloadBuilderStringIdentity implements NodePayloadBuilder<String>
{
	public String buildPayload(String src) 
	{
		return src;
	}
}
