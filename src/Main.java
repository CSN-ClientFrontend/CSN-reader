import java.io.File;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Map;

public class Main {

	/**
	 * @param args
	 */
	public static void main(String[] args) {

	    StorageDatabase storage = new StorageDatabase();
        ConfigFile config = new ConfigFile();
	    
        
        
        QueueThread g = new QueueThread();
        
		Server b = new Server(g);
		Reader r = new Reader();
		
	    
	    g.start();	
		b.start();
		r.start();
	
	
		
		

//		storage.close();
//		b.close();
	}

}
