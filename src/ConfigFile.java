import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.commons.io.IOUtils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;




public class ConfigFile {

	class Configuration
	{
		int currentInFile;
		int currentOutFile;
		int currentFileLocation;
	}
	
	
	File configurationFile;
	Configuration config;
	Gson gson;
	public ConfigFile() {
		
		File root = Constants.getRoot();
		configurationFile = new File(root,"config.json");
	
		gson = new GsonBuilder().setPrettyPrinting().create();
				
		config = gson.fromJson(readString(), Configuration.class);
		
		
	}
	
	
	public int getCurrentInFile()
	{
		return config.currentInFile;
	}
	
	public int getCurrentOutFile()
	{
		return config.currentOutFile;
	}
	
	public int getCurrentFileLocation()
	{
		return config.currentFileLocation;
	}
	
	
	public void setCurrentFileLocation(int loc)
	{
		config.currentFileLocation = loc;
	}
	
	
	public void incrementCurrentOutFile()
	{
		config.currentOutFile++;
	}
	
	public void incrementCurrentInFile()
	{
		config.currentInFile++;
	}
	
	
	public void save()
	{
		writeString(gson.toJson(config));
	}
	
	
	void writeString(String foo)
	{
		try (FileOutputStream write = new FileOutputStream(configurationFile)){
			IOUtils.write(foo, write);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
	}
	
	String readString()
	{
		
		try (FileInputStream read = new FileInputStream(configurationFile)){
			return IOUtils.toString(read);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		return null;
		
	}
	
	
	
	
	
}
