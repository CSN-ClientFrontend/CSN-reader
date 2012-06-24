import java.util.List;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.channels.FileLock;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.IOUtils;

public class MetadataParser {

	public static Map<String, String> parseMetadata(File metadataFile) {
		HashMap<String, String> result = new HashMap<>();

		try (FileInputStream in = new FileInputStream(metadataFile)) {

			List<String> lines = IOUtils.readLines(in);
			for (String line : lines) {
				String[] arr = line.split(":");
				result.put(arr[0].trim(), arr[1].trim());
			}

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return result;

	}

}
