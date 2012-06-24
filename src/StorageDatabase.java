import java.io.File;
import java.io.IOException;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Timestamp;


public class StorageDatabase {

	private Connection databaseConn;
	private PreparedStatement addFile;
	private PreparedStatement printAllFiles;
	private PreparedStatement findFileWithTime;
	private PreparedStatement clearAll;
	
	private File homeDirectory = new File(System.getProperty("user.home"));
	private File rootDirectory = new File(homeDirectory,"CSNStuff");
	
	public StorageDatabase() {
		if (!rootDirectory.isDirectory())
			System.out.println("Directory made = " + rootDirectory.mkdir());
		
		try {
			Class.forName("org.h2.Driver");
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		try {
			databaseConn = DriverManager.getConnection("jdbc:h2:~/test3;ifexists=true","","");
		} catch (SQLException e) {
			// Database does not already exist
			try {
				databaseConn = DriverManager.getConnection("jdbc:h2:~/test3","","");
				initializeDatabase();
			} catch (SQLException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			
			
			e.printStackTrace();
			
		
			
		}
		
		
		try {
			addFile = databaseConn.prepareStatement("INSERT INTO FileList VALUES ( ?, ? ,?)");
			printAllFiles = databaseConn.prepareStatement("SELECT * FROM FileList");
			findFileWithTime = databaseConn.prepareStatement("SELECT * FROM FileList WHERE ? BETWEEN StartTime AND EndTime");
			clearAll = databaseConn.prepareStatement("DELETE FROM FileList");
		} catch (SQLException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		
		
		
	}
	
	public void addFile(String fileName, long startTime, long endTime) {
		try {
			addFile.setString(1, fileName);
			addFile.setTimestamp(2, new Timestamp(startTime));
			addFile.setTimestamp(3, new Timestamp(endTime));
			boolean finished = addFile.execute();
			System.out.println("Adding the file: " + finished);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
	}
	
	public void printAllFiles()
	{
		
		System.out.println("Printing out the database");
		try {
			ResultSet result = printAllFiles.executeQuery();
			while (result.next())
			{
				String fileName = result.getString("FileName");
				Timestamp start = result.getTimestamp("StartTime");
				Timestamp end = result.getTimestamp("EndTime");
				int row = result.getRow();
				
				System.out.printf("Line %d: FileName=%s, StartTime=%s, EndTime=%s\n", row,fileName,start,end);
			
			}
			result.close();
			
			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
	}
	
	public String findFileWithTime(long timestamp)
	{
		try {
			findFileWithTime.setTimestamp(1, new Timestamp(timestamp));
			ResultSet result = findFileWithTime.executeQuery();
			if (!result.next()) // No results
				return null;
			String answer = result.getString("FileName");
			if (result.next()) // Too many results
				throw new RuntimeException("More than one file with that timestamp");
			return answer;
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;	
		
		
	}
	
	private void initializeDatabase() throws SQLException {
		Statement stat =  databaseConn.createStatement();
		stat.execute("CREATE TABLE FileList (FileName VARCHAR(255) NOT NULL, StartTime TIMESTAMP NOT NULL, EndTime TIMESTAMP NOT NULL)");
	}
		
	
	public void clearAll()
	{
		try {
			clearAll.execute();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void close()
	{
		
		try {
			addFile.close();
			printAllFiles.close();
			findFileWithTime.close();
			clearAll.close();
			
			databaseConn.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
	}
	

	
}
