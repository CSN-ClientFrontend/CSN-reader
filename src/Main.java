import java.io.File;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;
import java.util.Map;

public class Main {

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		Server b = new Server();
		System.out.println(System.getProperty("user.dir"));

		System.out.println(Constants.getRoot().getAbsolutePath());

		StorageDatabase storage = new StorageDatabase();
		ConfigFile config = new ConfigFile();

		storage.printAllFiles();

		while (true) {
			while (true) {
				int startingFile = config.getCurrentInFile();
				int startingByte = config.getCurrentFileLocation();

				Map<String, String> data = MetadataParser
						.parseMetadata(new File(
								"C:\\Windows\\Temp\\CSNService", startingFile
										+ "-metadata"));
				//System.out.println(data);

				int length = Integer.parseInt(data.get("length"));
				int rate = Integer.parseInt(data.get("device.dataRate"));
				int numOfBytesInFile = length / rate * 2;

				long startTime = Long.parseLong(data.get("begin"));
				long endTime = Long.parseLong(data.get("end"));

				try {

					FileChannel file0 = FileChannel.open(new File(
							"C:\\Windows\\Temp\\CSNService", startingFile
									+ "-0").toPath(), StandardOpenOption.READ);
					FileChannel fileOut0 = FileChannel.open(
							new File(Constants.getRoot(), startingFile + "-0")
									.toPath(), StandardOpenOption.APPEND,
							StandardOpenOption.CREATE);

					file0.transferTo(startingByte, numOfBytesInFile
							- startingByte, fileOut0);

					System.out.println(numOfBytesInFile);

					if (startingByte == 0)
						storage.addFile(startingFile + "", startTime, endTime,numOfBytesInFile);
					else
						storage.updateEndTimeAndLength("" + startingFile, endTime,numOfBytesInFile);

					File nextInList = new File("C:\\Windows\\Temp\\CSNService",
							(startingFile + 1) + "-0");

					if (nextInList.exists()) {
						config.incrementCurrentInFile();
						config.setCurrentFileLocation(0);

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

//		storage.close();
//		b.close();
	}

}
