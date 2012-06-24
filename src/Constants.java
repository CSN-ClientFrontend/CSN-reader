import java.io.File;
import java.net.URISyntaxException;


public class Constants {
	private static File root;
	
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
	
	public static File getRoot()
	{
		return root;
	}
	

}
