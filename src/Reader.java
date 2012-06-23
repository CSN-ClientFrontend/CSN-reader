import java.io.*;

public class Reader {
	File directory;
	File[] manafests;
	
	public Reader(){
		if(System.getProperty("os.name").contains("Windows"))
			directory = new File("C:\\WINDOWS\\Temp\\CSNService");
		else
			directory = new File("/var/tmp/CSNDaemon");
	}
	

}
