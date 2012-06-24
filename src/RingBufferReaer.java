import java.io.*;
import java.nio.channels.FileChannel;

import javax.imageio.stream.FileImageInputStream;
public class RingBufferReaer {
	File[] files;
	FileInputStream[] input;
	public final static int metadata = 3;
	public final static int timecode = 4;
	boolean filesOpened=false;
	
	public RingBufferReaer(int n,File directory) throws FileNotFoundException{
		files = new File[] {new File(directory,n+"-0"),new File(directory,n+"-1"),
					new File(directory,n+"-2"),new File(directory,n+"-metadata"),
					new File(directory,n+"-timecode")};
		input = new FileInputStream[files.length];
		input[metadata] = new FileInputStream(files[metadata]);
		
		
	}
	public FileChannel[] openChannels(){
		return new FileChannel[]{input[0].getChannel(),input[1].getChannel(),input[2].getChannel()};
	}
	private void openFiles(){
		filesOpened=true;
		for(int i=0;i<files.length-2;i++)
			try {
				input[i] = new FileInputStream(files[i]);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		if(files[timecode].exists())
			try {
				input[timecode]=new FileInputStream(files[timecode]);
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	}
	public boolean currentlyActive(){
		return files[timecode].exists();
	}
	
	public void read(byte[]a,byte[]b,byte[]c, int off, int len){
		if(!filesOpened)
			openFiles();
		
		try {
			input[0].read(a, off, len);
			input[1].read(b, off, len);
			input[2].read(c, off, len);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	public long getRingPosition(){
		return 0;
	}

}
