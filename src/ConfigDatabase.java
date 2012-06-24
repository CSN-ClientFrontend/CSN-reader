import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;


public class ConfigDatabase {

	Connection databaseConn;
	public ConfigDatabase() {
		try {
			Class.forName("org.h2.Driver");
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		try {
			databaseConn = DriverManager.getConnection("jdbc:h2:~/test2;ifexists=true","","");
		} catch (SQLException e) {
			// Database does not already exist
			try {
				databaseConn = DriverManager.getConnection("jdbc:h2:~/test2","","");
				initializeDatabase();
			} catch (SQLException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			
			
			e.printStackTrace();
			
		
			
		}
		
		
		try {
			Statement stat = databaseConn.createStatement();
			ResultSet set = stat.executeQuery("SELECT * FROM Configuration");
			System.out.println(set);
			set.next();
			System.out.println(set);
			System.out.println(set.getInt("CurrentToRead") + " "+ set.getInt("MaxSizeInMegaBytes"));
		} catch (SQLException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		
	}
	private void initializeDatabase() throws SQLException {
		Statement stat =  databaseConn.createStatement();
		stat.execute("CREATE TABLE Configuration (CurrentToRead INTEGER NOT NULL, MaxSizeInMegaBytes INTEGER NOT NULL)");
		stat.execute("INSERT INTO Configuration VALUES(0,1000)");
		
	}
	
	

	
}
