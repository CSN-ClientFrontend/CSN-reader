import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;


public class Database {

	Connection databaseConn;
	public Database() {
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
	}
	private void initializeDatabase() {
		// TODO Auto-generated method stub
		
	}

	
}
