import java.sql.Timestamp;
import java.util.List;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

import javax.sql.ConnectionEvent;

import com.google.gson.Gson;

class Message
{
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

		try {
			Gson gson = new Gson();
			DataInputStream dataIn = new DataInputStream(
					mySock.getInputStream());
			StorageDatabase base = new StorageDatabase();

			while (true) {
				
				String input = dataIn.readUTF();
				System.out.println("Recieved string: " + input );
				Message mes = gson.fromJson(input, Message.class);
				
				
				System.out.println(mes.startTime);
				System.out.println(new Timestamp(mes.startTime) + "  ;  " + new Timestamp(mes.endTime));
				List<String> startingFiles = base.findFilesWithTime(mes.startTime,mes.endTime);
				
				
				System.out.printf("I need files: %s\n",startingFiles);
				File firstFile = new File(Constants.getRoot(),startingFiles.get(0)+ "-0");
				FileInfo info = base.getFileInfo(startingFiles.get(0));
				
				
				
				long sizeOfFirst = firstFile.length();
				long timeDelta = info.endTime - info.startTime;
				long timeToSkip = mes.startTime - info.startTime ;
				System.out.println(timeToSkip + " " + timeDelta);
				
				//long exactStartingPlace = (timeToSkip * sizeOfFirst)/ timeDelta;
				
				double startingPlace = ((double) timeToSkip) /( (double) timeDelta) * ((double) sizeOfFirst);
				long exactStartingPlace = (long) startingPlace;
				if (exactStartingPlace%2 != 0)
					exactStartingPlace--;
				
				
			
				System.out.println(exactStartingPlace);
				
				
				
			}
		}
		catch (EOFException e) {
			System.out.println("Socket closed");
		}
		catch (Exception e) {
			e.printStackTrace();
		} finally {
			
			try {
				mySock.close();
			} catch (IOException e) {
				e.printStackTrace();
				
			}
			
			System.out.println("Socket dead");
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
