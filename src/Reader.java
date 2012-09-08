import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Map;

public class Reader implements Runnable {
	Thread readerThread = new Thread(this);
	
	
	public void start()
	{
	    readerThread.run();
	    
	}

	
	private void copyFile(File fromFile, File toFile, int startingByte, int numOfBytesInFile) throws IOException
	{
	    
	    Path fromPath =fromFile.toPath();
	    
        FileChannel fromChannel = FileChannel.open(fromPath, StandardOpenOption.READ);
	    
	    
        Path toPath = toFile
                .toPath();
        
        FileChannel toChannel = FileChannel.open(toPath, StandardOpenOption.APPEND, StandardOpenOption.CREATE);

        fromChannel.transferTo(startingByte, numOfBytesInFile
                - startingByte, toChannel);
	}
	

    @Override
    public void run() {
        StorageDatabase storage = new StorageDatabase();
        ConfigFile config = new ConfigFile();

        storage.printAllFiles();
        
        List<Integer> serials = storage.getAllSerials();
        for (int serial : serials)
            System.out.println(serial);

        while (true) {
            while (true) {
                int startingFile = config.getCurrentInFile();
                int startingByte = config.getCurrentFileLocation();

                File metadataFile = new File(Constants.getRootOfSource(), startingFile + "-metadata");
                Map<String, String> data = MetadataParser.parseMetadata(metadataFile);

                int length = Integer.parseInt(data.get("length"));
                int rate = Integer.parseInt(data.get("device.dataRate"));
                int numOfBytesInFile = length / rate * 2;

                long startTime = Long.parseLong(data.get("begin"));
                long endTime = Long.parseLong(data.get("end"));
                
                int serialNumber = Integer.parseInt(data.get("device.serial"));
                
               
                String storageName = config.getCurrentFileName();
                if (storageName == null)
                {
                  storageName = storage.getUniqueString("" + startingFile);
                  config.setCurrentFileName(storageName);
                }

                try {

                    for (int i = 0; i < 3; i++)
                    {
                    
                        File fromFile =  new File( Constants.getRootOfSource(), startingFile + "-" + i);
                    
                        File toFile = new File(Constants.getRoot(), storageName + "-" + i);
                    
             
                        copyFile(fromFile,toFile,startingByte,numOfBytesInFile);

                    }
                    
                    //System.out.println(numOfBytesInFile+ " , " + endTime);

                    if (startingByte == 0)
                        storage.addFile(storageName, startTime, endTime,numOfBytesInFile,serialNumber);
                    else
                        storage.updateEndTimeAndLength(storageName, endTime,numOfBytesInFile);
                     
                    
                    File nextInList = new File(Constants.getRootOfSource(),
                            (startingFile + 1) + "-metadata");

                    if (nextInList.exists()) {
                        config.incrementCurrentInFile();
                        config.setCurrentFileLocation(0);
                        config.setCurrentFileName(null);
                        config.save();

                    } else {
                        config.setCurrentFileLocation(numOfBytesInFile);
                        config.save();
                        break;
                    }

                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

            }

            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        
    }
	

}
