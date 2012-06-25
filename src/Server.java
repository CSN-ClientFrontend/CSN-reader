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
				DataInputStream dataIn = new DataInputStream(
						socket.getInputStream());
				DataOutputStream dataOut = new DataOutputStream(
						socket.getOutputStream())) {

			StorageDatabase base = new StorageDatabase();

			Gson gson = new Gson();

			while (true) {

				String input = dataIn.readUTF();
				System.out.println("Recieved string: " + input);
				Protocol.Message mes = gson.fromJson(input,
						Protocol.Message.class);

				System.out.println(mes.startTime);
				System.out.println(new Timestamp(mes.startTime) + "  ;  "
						+ new Timestamp(mes.endTime));
				List<String> startingFiles = base.findFilesWithTime(
						mes.startTime, mes.endTime);

				System.out.printf("I need files: %s\n", startingFiles);

				TempFileInfo[] fileToWrite = new TempFileInfo[startingFiles
						.size()];

				Protocol.Response res = new Protocol.Response();
				res.sections = new Protocol.Section[startingFiles.size()];
				if (startingFiles.size() == 0) {
					dataOut.writeUTF(gson.toJson(res));
					continue;
				}

				for (int i = 0; i < startingFiles.size(); i++) {
					String file = startingFiles.get(i);

					Protocol.Section sec = new Protocol.Section();
					FileInfo info = base.getFileInfo(file);

					sec.length = info.length;
					sec.startTime = info.startTime;
					sec.endTime = info.endTime;
					res.sections[i] = sec;

					fileToWrite[i] = new TempFileInfo();
					fileToWrite[i].file = file;
					fileToWrite[i].length = info.length;
					fileToWrite[i].offset = 0;

				}

				Protocol.Section info = res.sections[0];

				if (mes.startTime > info.startTime) {

					long sizeOfFirst = info.length;
					long timeDeltaFirst = info.endTime - info.startTime;
					long timeToSkipFirst = mes.startTime - info.startTime;
					System.out.println(timeToSkipFirst + " " + timeDeltaFirst);

					// long exactStartingPlace = (timeToSkip * sizeOfFirst)/
					// timeDelta;

					double startingPlace = ((double) timeToSkipFirst)
							/ ((double) timeDeltaFirst)
							* ((double) sizeOfFirst);
					long exactStartingPlace = (long) startingPlace;
					if (exactStartingPlace % 2 != 0)
						exactStartingPlace--;

					System.out.println(exactStartingPlace);
					long sizeOfFirstMessage = sizeOfFirst - exactStartingPlace;

					res.sections[0].startTime = mes.startTime;
					res.sections[0].length = sizeOfFirstMessage;

					fileToWrite[0].offset = exactStartingPlace;
					fileToWrite[0].length = sizeOfFirstMessage;
				}

				Protocol.Section info2 = res.sections[res.sections.length -1];;

				if (mes.endTime < info2.endTime) {

					long sizeOfSecond = info2.length;
					long timeDeltaLast = info2.endTime - info2.startTime;
					long timeNeededLast = mes.endTime - info2.startTime;

					double endingPlace = (double) timeNeededLast
							/ (double) timeDeltaLast * (double) sizeOfSecond;
					long exactEndingPlace = (long) (endingPlace + .5);
					if (exactEndingPlace % 2 != 0)
						exactEndingPlace++;

					if (exactEndingPlace > sizeOfSecond)
						exactEndingPlace = sizeOfSecond; // Missing bytes at end

					res.sections[startingFiles.size() - 1].endTime = mes.endTime;
					res.sections[startingFiles.size() - 1].length = exactEndingPlace;

					fileToWrite[startingFiles.size() - 1].length = exactEndingPlace;
				}

				dataOut.writeUTF(gson.toJson(res));

				for (TempFileInfo info3 : fileToWrite) {
					File f = new File(Constants.getRoot(),info3.file + "-0");
					try (FileInputStream in = new FileInputStream(f)) {
						long numOfBytes = IOUtils.copyLarge(in, dataOut,
								info3.offset, info3.length);
						System.out.println(f.getAbsolutePath() + " : "
								+ numOfBytes);
					}
				}

			}
		} catch (EOFException e) {
			System.out.println("Socket closed");
		} catch (Exception e) {
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
