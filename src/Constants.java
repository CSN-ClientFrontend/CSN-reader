import java.io.File;
import java.net.URISyntaxException;


public class Constants {
	private static File root;
	
	static {
		try {
			root = new File(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getParentFile();
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
