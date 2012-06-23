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
		
		try {
			Class.forName("org.h2.Driver");
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		try {
			Connection c = DriverManager.getConnection("jdbc:h2:~/test2","","");
			
			Statement stat = c.createStatement();
			ResultSet res = stat.executeQuery("SELECT * from foo");
			res.next();
			System.out.println(res.getString("day"));
			c.close();
		} catch (SQLException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	

	}

}
