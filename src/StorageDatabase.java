import java.io.File;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

class FileInfo {
    String fileName;
    long startTime;
    long endTime;
    long length;
    int serialNumber;
}

public class StorageDatabase {

    private Connection databaseConn;
    private PreparedStatement addFile;
    private PreparedStatement printAllFiles;
    private PreparedStatement findFileWithTime;
    private PreparedStatement clearAll;
    private PreparedStatement updateEndTime;
    private PreparedStatement getFileInfo;
    private PreparedStatement getAllSerials;

    public StorageDatabase() {

        try {
            Class.forName("org.h2.Driver");
        } catch (ClassNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        File root = Constants.getRoot();
        File databaseLocation = new File(root, "storageDatabase");
        String path = null;
        try {
            path = databaseLocation.getCanonicalPath();
        } catch (IOException e2) {
            // TODO Auto-generated catch block
            e2.printStackTrace();
            throw new RuntimeException(e2);
        }

        try {
            databaseConn = DriverManager.getConnection("jdbc:h2:" + path + ";ifexists=true", "", "");
        } catch (SQLException e) {
            // Database does not already exist
            try {
                databaseConn = DriverManager.getConnection("jdbc:h2:" + path, "", "");
                initializeDatabase();
            } catch (SQLException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }

            e.printStackTrace();

        }

        try {
            addFile = databaseConn.prepareStatement("INSERT INTO FileList VALUES ( ?, ? ,?, ?,?)");
            printAllFiles = databaseConn.prepareStatement("SELECT * FROM FileList");
            findFileWithTime = databaseConn.prepareStatement("SELECT * FROM FileList WHERE SerialNumber = ? AND (? BETWEEN StartTime AND EndTime) OR (? BETWEEN StartTime AND EndTime) OR (StartTime BETWEEN ? AND ?) ORDER BY StartTime ASC");
            clearAll = databaseConn.prepareStatement("DELETE FROM FileList");
            updateEndTime = databaseConn.prepareStatement("UPDATE FileList SET EndTime = ?, Length = ? WHERE FileName =?");
            getFileInfo = databaseConn.prepareStatement("SELECT * FROM FileList WHERE FileName = ?");
            getAllSerials = databaseConn.prepareStatement("SELECT DISTINCT SerialNumber FROM FileList");
        } catch (SQLException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }

    }

    public List<Integer> getAllSerials() {

        ArrayList<Integer> serials = new ArrayList<>();
        try (ResultSet result = getAllSerials.executeQuery()) {

            while (result.next()) {
                serials.add(result.getInt("SerialNumber"));
            }

        } catch (SQLException e) {

        }

        return serials;

    }

    public void addFile(String fileName, long startTime, long endTime, long length, int serialNumber) {
        try {
            addFile.setString(1, fileName);
            addFile.setTimestamp(2, new Timestamp(startTime));
            addFile.setTimestamp(3, new Timestamp(endTime));
            addFile.setLong(4, length);
            addFile.setInt(5, serialNumber);
            addFile.execute();
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    public void printAllFiles() {

        System.out.println("Printing out the database");
        try {
            ResultSet result = printAllFiles.executeQuery();
            while (result.next()) {
                String fileName = result.getString("FileName");
                Timestamp start = result.getTimestamp("StartTime");
                Timestamp end = result.getTimestamp("EndTime");
                long length = result.getLong("Length");
                int serialNumber = result.getInt("SerialNumber");
                int row = result.getRow();

                System.out.printf("Line %d: FileName=%s, StartTime=%s, EndTime=%s, Length=%s, SerialNumber=%s\n", row, fileName, start, end, length, serialNumber);

            }
            result.close();

        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public List<FileInfo> findFilesWithTime(long startTime, long endTime, int serialNumber) {
        try {
            List<FileInfo> results = new ArrayList<>();
            findFileWithTime.setInt(1, serialNumber);
            findFileWithTime.setTimestamp(2, new Timestamp(startTime));
            findFileWithTime.setTimestamp(3, new Timestamp(endTime));
            findFileWithTime.setTimestamp(4, new Timestamp(startTime));
            findFileWithTime.setTimestamp(5, new Timestamp(endTime));
            ResultSet result = findFileWithTime.executeQuery();
            while (result.next()) {
               
                results.add(getFileFromResult(result));
            }
            result.close();
            return results;
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;

    }

    
    private FileInfo getFileFromResult(ResultSet res) throws SQLException
    {
        FileInfo answer = new FileInfo();
        answer.fileName = res.getString("FileName");
        answer.startTime = res.getTimestamp("StartTime").getTime();
        answer.endTime = res.getTimestamp("EndTime").getTime();
        answer.length = res.getLong("Length");
        answer.serialNumber = res.getInt("SerialNumber");
        
        return answer;
    }
    
    public FileInfo getFileInfo(String filename) {
        try {
            getFileInfo.setString(1, filename);
            ResultSet res = getFileInfo.executeQuery();
            res.next();

           FileInfo answer = getFileFromResult(res);

            res.close();

            return answer;
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;

    }

    private void initializeDatabase() throws SQLException {
        Statement stat = databaseConn.createStatement();
        stat.execute("CREATE TABLE FileList (FileName VARCHAR(255) NOT NULL UNIQUE, StartTime TIMESTAMP NOT NULL, EndTime TIMESTAMP NOT NULL, Length INTEGER NOT NULL, SerialNumber INTEGER NOT NULL)");
    }

    public void clearAll() {
        try {
            clearAll.execute();
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void updateEndTimeAndLength(String file, long endTime, long length) {
        try {
            updateEndTime.setTimestamp(1, new Timestamp(endTime));
            updateEndTime.setLong(2, length);
            updateEndTime.setString(3, file);
            updateEndTime.execute();
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    public void close() {

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
