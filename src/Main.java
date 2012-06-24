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
		
		
		
		
		ConfigDatabase b = new ConfigDatabase();
		
		StorageDatabase c = new StorageDatabase();
		
		long time = System.currentTimeMillis();
		c.addFile("foo", time - 2000,time - 1001);
		c.addFile("blah", time - 1000,time);
		
		c.clearAll();
		c.printAllFiles();
		
		System.out.println(c.findFileWithTime(time-500));
		c.close();
	

	}

}
