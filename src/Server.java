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

                TempFileInfo[] filesToWrite = buildFileWriteInfo(startingFiles, base);

               shortenFirstMessage(mes, res, filesToWrite);

               shortenLastMessage(mes, res, filesToWrite);
                

                dataOut.writeUTF(gson.toJson(res));

                writeFiles(dataOut, filesToWrite);

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

    private TempFileInfo[] buildFileWriteInfo(List<String> filesToSend, StorageDatabase database) {

        TempFileInfo[] filesToWrite = new TempFileInfo[filesToSend.size()];
        for (int i = 0; i < filesToSend.size(); i++) {

            String file = filesToSend.get(i);
            FileInfo info = database.getFileInfo(file);

            filesToWrite[i] = new TempFileInfo();
            filesToWrite[i].file = file;
            filesToWrite[i].length = info.length;
            filesToWrite[i].offset = 0;
        }
        return filesToWrite;
    }

    private void shortenLastMessage(Protocol.Message mes, Protocol.Response res, TempFileInfo[] filesToWrite) {
        int index = res.sections.length -1;
        Protocol.Section info2 = res.sections[index];
        

        if (mes.endTime < info2.endTime) {

            long length = getOffset( info2, mes.endTime);

            res.sections[index].endTime = mes.endTime;
            res.sections[index].length = length;

            filesToWrite[index].length = length;
        }
    }

    private void shortenFirstMessage(Protocol.Message mes, Protocol.Response res, TempFileInfo[] filesToWrite) {
        Protocol.Section info = res.sections[0];

        if (mes.startTime > info.startTime) {

            long offset =  getOffset( info,mes.startTime);
            
            
            long newLength = info.length - offset;

            res.sections[0].startTime = mes.startTime;
            res.sections[0].length = newLength;

            filesToWrite[0].offset = offset;
            filesToWrite[0].length = newLength;
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

    private void writeFiles(DataOutputStream dataOut, TempFileInfo[] fileToWrite) throws IOException, FileNotFoundException {
        for (TempFileInfo info3 : fileToWrite) {
            File f = new File(Constants.getRoot(), info3.file + "-0");
            try (FileInputStream in = new FileInputStream(f)) {
                long numOfBytes = IOUtils.copyLarge(in, dataOut, info3.offset, info3.length);
                System.out.println(f.getAbsolutePath() + " : " + numOfBytes);
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
