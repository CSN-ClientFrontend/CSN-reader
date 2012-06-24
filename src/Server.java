import java.sql.Timestamp;
import java.util.List;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

import javax.sql.ConnectionEvent;

import org.apache.commons.io.IOUtils;

import com.google.gson.Gson;

class Message
{
	long startTime;
	long endTime;
}

class Response
{
	Section[] sections;
}

class Section
{
	long length;
	long startTime;
	long endTime;
}




class Connection implements Runnable {

	Socket mySock;
	

	public Connection(Socket mySock) {
		this.mySock = mySock;
	}

	@Override
	public void run() {

		System.out.println("Socket open at " + mySock.getRemoteSocketAddress());

		try (	Socket socket = mySock;
				DataInputStream dataIn = new DataInputStream(socket.getInputStream());
				DataOutputStream dataOut = new DataOutputStream(socket.getOutputStream()) 
				){
			
			
			StorageDatabase base = new StorageDatabase();
			
			Gson gson = new Gson();
			;

			while (true) {
				
				String input = dataIn.readUTF();
				System.out.println("Recieved string: " + input );
				Message mes = gson.fromJson(input, Message.class);
				
				
				System.out.println(mes.startTime);
				System.out.println(new Timestamp(mes.startTime) + "  ;  " + new Timestamp(mes.endTime));
				List<String> startingFiles = base.findFilesWithTime(mes.startTime,mes.endTime);
				
				
				System.out.printf("I need files: %s\n",startingFiles);
				
				
				
				Response res = new Response();
				res.sections = new Section[startingFiles.size()];
				
				for (int i = 0 ; i <startingFiles.size(); i++)
				{
					String file = startingFiles.get(i);
					File f = new File(Constants.getRoot(),file+ "-0");
					Section sec = new Section();
					FileInfo info = base.getFileInfo(file);
					sec.length = f.length();
					sec.startTime = info.startTime;
					sec.endTime = info.endTime;
					res.sections[i] = sec;
					
				}
				
				dataOut.writeUTF(gson.toJson(res));
				
				
				for (String file : startingFiles)
				{
					try(FileInputStream in = new FileInputStream(new File(Constants.getRoot(),file + "-0")))
					{
						int numOfBytes = IOUtils.copy(in,dataOut);
						System.out.println(file + " : " + numOfBytes);
					}
				}
				
				
				
				
//				File firstFile = new File(Constants.getRoot(),startingFiles.get(0)+ "-0");
//				FileInfo info = base.getFileInfo(startingFiles.get(0));
//				
//				
//				
//				long sizeOfFirst = firstFile.length();
//				long timeDeltaFirst = info.endTime - info.startTime;
//				long timeToSkipFirst = mes.startTime - info.startTime ;
//				System.out.println(timeToSkipFirst + " " + timeDeltaFirst);
//				
//				//long exactStartingPlace = (timeToSkip * sizeOfFirst)/ timeDelta;
//				
//				double startingPlace = ((double) timeToSkipFirst) /( (double) timeDeltaFirst) * ((double) sizeOfFirst);
//				long exactStartingPlace = (long) startingPlace;
//				if (exactStartingPlace%2 != 0)
//					exactStartingPlace--;
//				
//				
//				if (exactStartingPlace <0)
//					exactStartingPlace = 0; // Missing bytes at front
//			
//				System.out.println(exactStartingPlace);
//				long sizeOfFirstMessage = sizeOfFirst-exactStartingPlace;
//				
//				
//				File lastFile = new File(Constants.getRoot(),startingFiles.get(startingFiles.size()-1)+ "-0");
//				FileInfo info2 = base.getFileInfo(startingFiles.get(startingFiles.size()-1));
//				
//				long sizeOfSecond = lastFile.length();
//				long timeDeltaLast = info2.endTime - info2.startTime;
//				long timeNeededLast = mes.endTime - info2.startTime;
//				
//				double endingPlace = (double) timeNeededLast/ (double) timeDeltaLast * (double)sizeOfFirst;
//				long exactEndingPlace = (long) (endingPlace+.5);
//				if (exactEndingPlace % 2 != 0)
//					exactEndingPlace++;
//				
//				if (exactStartingPlace >sizeOfSecond)
//					exactStartingPlace = sizeOfSecond; // Missing bytes at end
				
				
				
				
				
			}
		}
		catch (EOFException e) {
			System.out.println("Socket closed");
		}
		catch (Exception e) {
			e.printStackTrace();
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
		}
		finally {
			for (Thread child : childeren)
			{
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
