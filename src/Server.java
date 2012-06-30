import java.sql.Timestamp;
import java.util.List;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

import javax.sql.ConnectionEvent;

import org.apache.commons.io.IOUtils;

import com.google.gson.Gson;

class TempFileInfo {
    String file;
    long offset;
    long length;
    long actualLength;
}

class Connection implements Runnable {

    Socket mySock;

    public Connection(Socket mySock) {
        this.mySock = mySock;
    }

 

    @Override
    public void run() {

        System.out.println("Socket open at " + mySock.getRemoteSocketAddress());

        try (Socket socket = mySock; 
             DataInputStream dataIn = new DataInputStream(socket.getInputStream());
             DataOutputStream dataOut = new DataOutputStream(socket.getOutputStream())) {

            StorageDatabase base = new StorageDatabase();

            Gson gson = new Gson();

            while (true) {

                String input = dataIn.readUTF();
                System.out.println("Recieved string: " + input);
                Protocol.Message mes = gson.fromJson(input, Protocol.Message.class);

                System.out.println(mes.startTime);
                System.out.println(new Timestamp(mes.startTime) + "  ;  " + new Timestamp(mes.endTime));
                List<String> startingFiles = base.findFilesWithTime(mes.startTime, mes.endTime);

                System.out.printf("I need files: %s\n", startingFiles);

                Protocol.Response res = buildResponse(startingFiles, base);
                
                if (startingFiles.size() == 0) {
                    dataOut.writeUTF(gson.toJson(res));
                    continue;
                }

                TempFileInfo[] filesToWrite = buildFileWriteInfo(startingFiles, base,mes.resolution);

               shortenFirstMessage(mes, res, filesToWrite,mes.resolution);

               shortenLastMessage(mes, res, filesToWrite,mes.resolution);
                
               fixResponse(res, mes.resolution);

                dataOut.writeUTF(gson.toJson(res));

                writeFiles(dataOut, filesToWrite,mes.resolution);

            }
        } catch (EOFException e) {
            System.out.println("Socket closed");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    
    private Protocol.Response buildResponse(List<String> filesToSend, StorageDatabase database) {
        Protocol.Response res = new Protocol.Response();
        res.sections = new Protocol.Section[filesToSend.size()];

        for (int i = 0; i < filesToSend.size(); i++) {
            String file = filesToSend.get(i);

            FileInfo info = database.getFileInfo(file);

           
            
            Protocol.Section sec = new Protocol.Section();
            sec.length = info.length;
            sec.startTime = info.startTime;
            sec.endTime = info.endTime;
            res.sections[i] = sec;

        }

        return res;

    }
    
    private void fixResponse(Protocol.Response res, int resolution)
    
    {
        for (int i = 1; i < res.sections.length-1; i++) {
           
            Protocol.Section sec =  res.sections[i];
            
            
            long actualLength = sec.length/resolution;
            if (actualLength%2 != 0)
                actualLength--;
            
           
            sec.length = actualLength;

        }
    }

    private TempFileInfo[] buildFileWriteInfo(List<String> filesToSend, StorageDatabase database, int resolution) {

        TempFileInfo[] filesToWrite = new TempFileInfo[filesToSend.size()];
        for (int i = 0; i < filesToSend.size(); i++) {

            String file = filesToSend.get(i);
            FileInfo info = database.getFileInfo(file);

            long actualLength = info.length/resolution;
            if (actualLength%2 != 0)
                actualLength--;
            
            filesToWrite[i] = new TempFileInfo();
            filesToWrite[i].file = file;
            filesToWrite[i].length = info.length;
            filesToWrite[i].offset = 0;
            filesToWrite[i].actualLength = actualLength;
        }
        return filesToWrite;
    }

    private void shortenLastMessage(Protocol.Message mes, Protocol.Response res, TempFileInfo[] filesToWrite, int resolution) {
        int index = res.sections.length -1;
        Protocol.Section info2 = res.sections[index];
        

        if (mes.endTime < info2.endTime) {

            long length = getOffset( info2, mes.endTime);

            res.sections[index].endTime = mes.endTime;
            
            long actualLength = length/resolution;
            if (actualLength%2 != 0)
                actualLength--;
            
            
            res.sections[index].length = actualLength;

            filesToWrite[index].length = length;
            filesToWrite[index].actualLength = actualLength;
        }
    }

    private void shortenFirstMessage(Protocol.Message mes, Protocol.Response res, TempFileInfo[] filesToWrite, int resolution) {
        Protocol.Section info = res.sections[0];

        if (mes.startTime > info.startTime) {

            long offset =  getOffset( info,mes.startTime);
            
            
            long newLength = info.length - offset;

            
            long actualLength = newLength/resolution;
            if (actualLength%2 != 0)
                actualLength--;
            
            
            res.sections[0].startTime = mes.startTime;
            res.sections[0].length = actualLength;

            filesToWrite[0].offset = offset;
            filesToWrite[0].length = newLength;
            filesToWrite[0].actualLength = actualLength;
        }
    }

    private long getOffset(Protocol.Section info, long until) {
        long sizeOfFirst = info.length;
        long timeDeltaFirst = info.endTime - info.startTime;
        long timeToSkipFirst = until - info.startTime;
        System.out.println(timeToSkipFirst + " " + timeDeltaFirst);

        // long exactStartingPlace = (timeToSkip * sizeOfFirst)/
        // timeDelta;

        double startingPlace = ((double) timeToSkipFirst) / ((double) timeDeltaFirst) * ((double) sizeOfFirst);
        long offset = (long) startingPlace; 
        if (offset % 2 != 0)
            offset ++;
        
        if (offset < 0)
            offset = 0;
        if (offset > info.length)
            offset = info.length;
        
        return offset;
    }

    private void writeFiles(DataOutputStream dataOut, TempFileInfo[] fileToWrite, int resolution) throws IOException, FileNotFoundException {
        final int sizeOfBuffer = 1024;
        byte[] byteTempArray = new byte[sizeOfBuffer];
        
        
        
        for (TempFileInfo info3 : fileToWrite) {
            
            
            byte[] outputBytes = new byte[(int) info3.actualLength];
            int pointInOutput = 0;
            
            File f = new File(Constants.getRoot(), info3.file + "-0");
            try (FileInputStream in = new FileInputStream(f)) {
                IOUtils.skipFully(in, info3.offset);
                int current = 0;
                int posInFile = 0;
                while (current < info3.length)
                {
                   
                    int numOfBytesRead = IOUtils.read(in, byteTempArray, 0, sizeOfBuffer);
                    current+= numOfBytesRead;
                    
                    if (current > info3.length)
                        current = (int) info3.length;
                    
                    
                    
                    
                    for (;posInFile < current/2 && pointInOutput < info3.actualLength; posInFile += resolution)
                    {
                        System.out.printf("I am adding something at %d, %d, %d, %d, %d\n",pointInOutput,posInFile,current,current/2,info3.actualLength);
                        outputBytes[pointInOutput++] = byteTempArray[posInFile*2 - (current - numOfBytesRead)];
                        outputBytes[pointInOutput++] = byteTempArray[posInFile*2+1 - (current - numOfBytesRead)];
                        
                    }
                    
                    
                }
                if (info3.actualLength != pointInOutput)
                    throw new RuntimeException("Did not actually read in all stuff");
               
                System.out.println("Writing at " + System.currentTimeMillis());
                IOUtils.write(outputBytes, dataOut);
                
                
                System.out.println(f.getAbsolutePath() + " : " + outputBytes.length);
            }
        }
    }

}

public class Server implements Runnable {

    Thread serverAcceptingThread;
    ServerSocket mySocket;

    public Server() {
        try {
            mySocket = new ServerSocket(5632);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        serverAcceptingThread = new Thread(this);
        serverAcceptingThread.start();
    }

    @Override
    public void run() {
        List<Thread> childeren = new ArrayList<>();
        try {

            while (true) {

                Socket s = mySocket.accept();
                Connection c = new Connection(s);
                Thread connectionThread = new Thread(c);
                connectionThread.start();
                childeren.add(connectionThread);

            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            for (Thread child : childeren) {
                try {
                    child.join();
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }

    }

    void close() {
        try {
            Thread.sleep(100000);
        } catch (InterruptedException e2) {
            // TODO Auto-generated catch block
            e2.printStackTrace();
        }
        try {
            mySocket.close();
        } catch (IOException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }

        try {
            serverAcceptingThread.join();
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

}
