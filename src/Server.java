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
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
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
    
    QueueThread queueThread;

    public Connection(Socket mySock, QueueThread queueThread2) {
        this.mySock = mySock;
        this.queueThread = queueThread2;
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

                String input2 = dataIn.readUTF();
                System.out.println("Recieved string: " + input2);
                
                 Protocol.Request req = gson.fromJson(input2, Protocol.Request.class);
                 
                 switch(req.type)
                 {
                 case RequestData:
                     handleRequestData(dataIn, dataOut, gson, base);
                     break;
                     
                 case RequestSerials:
                     handleRequestSerials(dataIn,dataOut,gson,base);
                     break;
                     
                 case PushQueueAddRequest:
                     handlePushQueueAddRequest(dataIn, dataOut, gson, base);
                     break;
                     
                 case PushQueueRemoveRequest:
                     handlePushQueueRemoveRequest(dataIn, dataOut, gson, base);
                     break;
                     
                 case PushQueueDisplayRequest:
                     handlePushQueueDisplayRequest(dataIn,dataOut, gson, base);
                     break;
                 
                 }
                
            }
        } catch (EOFException e) {
            System.out.println("Socket closed");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    
    
    private void handlePushQueueDisplayRequest(DataInputStream dataIn, DataOutputStream dataOut, Gson gson, StorageDatabase base) throws IOException {
        QueueObject[] objects = queueThread.getAllQueueObjects();
        
        Protocol.PushQueue.PushQueueDisplayResponse response = new Protocol.PushQueue.PushQueueDisplayResponse();
        response.items = new Protocol.PushQueue.PushQueueItem[objects.length];
        for (int i = 0; i < objects.length; i++)
        {
            QueueObject obj = objects[i];
            
            Protocol.PushQueue.PushQueueItem item = new Protocol.PushQueue.PushQueueItem();
            item.id = obj.id;
            item.lastTime = obj.lastTime;
            item.port = obj.port;
            item.url = obj.url;
            item.timeBetween = obj.timeBetween;
            
            response.items[i] = item;
        }
        
        String output = gson.toJson(response);
        System.out.println(output);
        dataOut.writeUTF(output);
        
    }

    private void handlePushQueueRemoveRequest(DataInputStream dataIn, DataOutputStream dataOut, Gson gson, StorageDatabase base) throws IOException {
        String input = dataIn.readUTF();
        
        Protocol.PushQueue.PushQueueRemoveRequest req = gson.fromJson(input, Protocol.PushQueue.PushQueueRemoveRequest.class);
        
        boolean success = queueThread.removeQueueObject(req.id);
        
        Protocol.PushQueue.PushQueueRemoveResponse response =  new Protocol.PushQueue.PushQueueRemoveResponse();
        response.success = success;
        
        dataOut.writeUTF(gson.toJson(response));
            
    }

    private void handlePushQueueAddRequest(DataInputStream dataIn, DataOutputStream dataOut, Gson gson, StorageDatabase base) throws IOException {
        String input = dataIn.readUTF();
        
        Protocol.PushQueue.PushQueueAddRequest req = gson.fromJson(input, Protocol.PushQueue.PushQueueAddRequest.class);
        
        QueueObject obj = new QueueObject();
        obj.lastTime = 0;
        obj.port = req.port;
        obj.url = req.url;
        obj.timeBetween = req.timeBetween;
        
        
        long id = queueThread.addQueueObject(obj);
        
        Protocol.PushQueue.PushQueueAddResponse response =  new Protocol.PushQueue.PushQueueAddResponse();
        
        response.id = id;
        
        dataOut.writeUTF(gson.toJson(response));
      
        
    }

    private void handleRequestSerials(DataInputStream dataIn, DataOutputStream dataOut, Gson gson, StorageDatabase base) throws IOException {
        List<Integer> serials = base.getAllSerials();
        
        Protocol.RequestSerials.SerialsResponse s = new Protocol.RequestSerials.SerialsResponse();
        int[] arr = new int[serials.size()];
        for (int i = 0; i < arr.length;i ++)
            arr[i] = serials.get(i);
        
        s.serialNumbers = arr;
        
        dataOut.writeUTF(gson.toJson(s));
        
    }





    private void handleRequestData(DataInputStream dataIn, DataOutputStream dataOut, Gson gson, StorageDatabase base) throws IOException
    {
        
        String input = dataIn.readUTF();
        Protocol.RequestData.RequestMessageParameters mes = gson.fromJson(input, Protocol.RequestData.RequestMessageParameters.class);

        System.out.println(mes.startTime);
        System.out.println(new Timestamp(mes.startTime) + "  ;  " + new Timestamp(mes.endTime));
        List<FileInfo> startingFiles = base.findFilesWithTime(mes.startTime, mes.endTime,mes.serialNumber);

        System.out.printf("I need files: %s\n", startingFiles);

        Protocol.RequestData.ResponseMetadata res = buildResponse(startingFiles, base);
        
        if (startingFiles.size() == 0) {
            dataOut.writeUTF(gson.toJson(res));
            return;
        }

        TempFileInfo[] filesToWrite = buildFileWriteInfo(startingFiles, base);

        shortenFirstMessage(mes, res, filesToWrite);

        shortenLastMessage(mes, res, filesToWrite);
        
        fixResponse(res, filesToWrite, mes.resolution);

        dataOut.writeUTF(gson.toJson(res));

        writeFiles(dataOut, filesToWrite,mes.resolution);
    }
    
    private Protocol.RequestData.ResponseMetadata buildResponse(List<FileInfo> startingFiles, StorageDatabase database) {
        Protocol.RequestData.ResponseMetadata res = new Protocol.RequestData.ResponseMetadata();
        res.sections = new  Protocol.RequestData.SectionMetada[startingFiles.size()];

       
        
        for (int i = 0; i < startingFiles.size(); i++) {
            FileInfo info = startingFiles.get(i);


           
            
            Protocol.RequestData.SectionMetada sec = new Protocol.RequestData.SectionMetada();
            sec.length = info.length;
            sec.startTime = info.startTime;
            sec.endTime = info.endTime;
            res.sections[i] = sec;

        }

        return res;

    }
    
    private void fixResponse(Protocol.RequestData.ResponseMetadata res, TempFileInfo[] fileDatas, int resolution)
    
    {
        for (int i = 0; i < res.sections.length; i++) {
           
            Protocol.RequestData.SectionMetada sec =  res.sections[i];
            TempFileInfo data = fileDatas[i];
            
            
             shortenFile(sec, data , resolution);

        }
    }

    private TempFileInfo[] buildFileWriteInfo(List<FileInfo> startingFiles, StorageDatabase database) {

        TempFileInfo[] filesToWrite = new TempFileInfo[startingFiles.size()];
        for (int i = 0; i < startingFiles.size(); i++) {

            
            FileInfo info =  startingFiles.get(i);

        
            
            filesToWrite[i] = new TempFileInfo();
            filesToWrite[i].file = info.fileName;
            filesToWrite[i].length = info.length;
            filesToWrite[i].offset = 0;
            
        }
        return filesToWrite;
    }

    private void shortenFile(Protocol.RequestData.SectionMetada sec, TempFileInfo fileData, int resolution)
    {
        long actualLength = sec.length/resolution;
        if (actualLength%2 != 0)
            actualLength--;
        
        sec.length = actualLength;
        fileData.actualLength = actualLength;
    }
    
    private void shortenLastMessage(Protocol.RequestData.RequestMessageParameters mes, Protocol.RequestData.ResponseMetadata res, TempFileInfo[] filesToWrite) {
        int index = res.sections.length -1;
        Protocol.RequestData.SectionMetada info2 = res.sections[index];
        

        if (mes.endTime < info2.endTime) {

            long length = getOffset( info2, mes.endTime);

            res.sections[index].endTime = mes.endTime;  
            
            
            res.sections[index].length = length;
            filesToWrite[index].length = length;
         
        }
    }

    
    private void shortenFirstMessage(Protocol.RequestData.RequestMessageParameters mes,Protocol.RequestData.ResponseMetadata res, TempFileInfo[] filesToWrite) {
        Protocol.RequestData.SectionMetada info = res.sections[0];

        if (mes.startTime > info.startTime) {

            long offset =  getOffset( info,mes.startTime);
            
            
            long newLength = info.length - offset;
            
            res.sections[0].startTime = mes.startTime;
            res.sections[0].length = newLength;

            filesToWrite[0].offset = offset;
            filesToWrite[0].length = newLength;
     
        }
    }

    private long getOffset(Protocol.RequestData.SectionMetada info, long until) {
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

    
    short squashValues(short[] pointsToSquash, int numberOfPoints)
    {
        int sum = 0; 
        for (int i = 0; i < numberOfPoints;i++)
            sum+= (0xffff & pointsToSquash[i]);
        
        sum/= numberOfPoints;
        
        return (short) sum;
    }
    
    private void writeFiles(DataOutputStream dataOut, TempFileInfo[] fileToWrite, int resolution) throws IOException, FileNotFoundException {
        final int sizeOfBuffer = resolution * 100*2;
        byte[] byteTempArray = new byte[sizeOfBuffer];
        
        
        
        for (TempFileInfo info3 : fileToWrite) {
            
            ByteBuffer actualOutput = ByteBuffer.allocate((int) info3.actualLength);
            ShortBuffer outputArray = actualOutput.asShortBuffer();
          
          int pointInOutput = 0;
            
            File f = new File(Constants.getRoot(), info3.file + "-0");
            try (FileInputStream in = new FileInputStream(f)) {
                IOUtils.skipFully(in, info3.offset);
                int current = 0;
      
                while (current < info3.length)
                {
                   
                    int numOfBytesRead = IOUtils.read(in, byteTempArray, 0, sizeOfBuffer);
                    current+= numOfBytesRead;
                    
                    if (current > info3.length){
                        current = (int) info3.length;
                        throw new RuntimeException("How is this possible?");
                        
                    }
                       
                    
                    
                    ByteBuffer buffer = ByteBuffer.wrap(byteTempArray);
                    ShortBuffer view = buffer.asShortBuffer();
                    
                    int tempPosition = 0;
                    for (int f1 = 0; f1 < 100 && pointInOutput < info3.actualLength/2; f1++)
                    {
                        int amountToSquash =0;
                        short[] valuesToSquash = new short[resolution];
                        
                      
                        
                        System.out.println(pointInOutput);
                        
                        for (int i = 0; i < resolution && tempPosition < numOfBytesRead/2; i++, tempPosition++)
                        {
                            valuesToSquash[i] = view.get(tempPosition);
                            amountToSquash++;
                            
                        }
                    
                        
                        short value = squashValues(valuesToSquash, amountToSquash);
                        outputArray.put(pointInOutput++,value);
                    }
                    
                    
                    
                }
                if (info3.actualLength != pointInOutput*2)
                    throw new RuntimeException("Did not actually read in all stuff");
               
                System.out.println("Writing at " + System.currentTimeMillis());
                IOUtils.write(actualOutput.array(), dataOut);
                
                
                System.out.println(f.getAbsolutePath() + " : " + actualOutput.array().length);
            }
        }
    }

}

public class Server implements Runnable {

    Thread serverAcceptingThread;
    ServerSocket mySocket;
    
    QueueThread queueThread;

    public Server(QueueThread g) {
        try {
            mySocket = new ServerSocket(5632);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        queueThread = g;
        serverAcceptingThread = new Thread(this);
        serverAcceptingThread.start();
    }

    @Override
    public void run() {
        List<Thread> childeren = new ArrayList<>();
        try {

            while (true) {

                Socket s = mySocket.accept();
                
                System.out.println("Accepted");
                Connection c = new Connection(s,queueThread);
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

    public void start() {
       
        
    }

}
