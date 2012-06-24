import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;


public class Main {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		
		System.out.println(System.getProperty("user.dir"));
		
		System.out.println(Constants.getRoot().getAbsolutePath());
		
		StorageDatabase storage = new StorageDatabase();
		ConfigFile config = new ConfigFile();

		
		
		
		config.close();
		storage.close();
	}

}
