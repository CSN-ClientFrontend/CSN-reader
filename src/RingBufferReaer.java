import java.io.*;

import javax.imageio.stream.FileImageInputStream;
public class RingBufferReaer {
	File[] files;
	FileInputStream[] input;
	public final static int metadata = 3;
	public final static int timecode = 4;
	
	public RingBufferReaer(int n,File directory) throws FileNotFoundException{
		files = new File[] {new File(directory,n+"-0"),new File(directory,n+"-1"),
					new File(directory,n+"-2"),new File(directory,n+"-metadata"),
					new File(directory,n+"-timecode")};
		input = new FileInputStream[files.length];
		input[metadata] = new FileInputStream(files[metadata]);
//		for(int i=0;i<files.length-1;i++)
//			try {
//				input[i] = new FileImageInputStream(files[i]);
//			} catch (IOException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
		
		
	}
	private void openFiles(){
		for(int i=0;i<files.length-2;i++)
			try {
				input[i] = new FileInputStream(files[i]);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	}

}
