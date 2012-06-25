
public abstract class Protocol {


static public class Message
{
	long startTime;
	long endTime;
}

static public class Response
{
	Section[] sections;
}

static public class Section
{
	long length;
	long startTime;
	long endTime;
}



	
}
