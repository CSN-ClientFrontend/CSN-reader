import java.io.File;
import java.net.URISyntaxException;


public class Constants {
	private static File root;
	private static File sourceRoot;
	
	static {
		try {
			File actualRoot = new File(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getParentFile();
			
			root = new File(actualRoot,"data");
			if (!root.isDirectory())
			{
				root.mkdir();
			}
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	static {
	    if(System.getProperty("os.name").contains("Windows"))
	        sourceRoot = new File("C:\\WINDOWS\\Temp\\CSNService");
	     else
	        sourceRoot = new File("/var/tmp/CSNDaemon");
	}
	
	public static File getRoot()
	{
		return root;
	}
	
	
	
	
	
	public static File getRootOfSource()
	{
	    return sourceRoot;
	}
	

}
