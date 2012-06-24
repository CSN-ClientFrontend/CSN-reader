import java.io.*;
abstract public class RingBufferFixer {
	public static Times fix(RingBufferReaer input, File[] output){
		{
			@SuppressWarnings("unused")
			File check = output[2];
		}
		try {
			FileOutputStream[] out = new FileOutputStream[]{new FileOutputStream(output[0]),
					new FileOutputStream(output[2]),new FileOutputStream(output[1])};
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return null;
	}

}
