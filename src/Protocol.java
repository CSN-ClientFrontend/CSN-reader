
public abstract class Protocol {


static public class Message
{
	long startTime;
	long endTime;
	int resolution;
	int serialNumber;
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


static public class Serials
{
    int[] serialNumbers;
}

static public class Request
{
    enum TypeOfRequest
    {
        RequestData,
        RequestSerials
    }
    
    TypeOfRequest type;
}

	
}
