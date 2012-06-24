import java.io.*;
import java.nio.channels.FileChannel;
abstract public class RingBufferFixer {
	public static Times fix(RingBufferReaer input, File[] output){
		{
			@SuppressWarnings("unused")
			File check = output[2];
		}
		FileOutputStream[] out = null;
		try {
			 out = new FileOutputStream[]{new FileOutputStream(output[0]),
					new FileOutputStream(output[2]),new FileOutputStream(output[1])};
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		FileChannel[] ins = input.openChannels();
		FileChannel[] outs = new FileChannel[]{out[0].getChannel(),out[1].getChannel(),out[2].getChannel()};
		
		try {
			ins[0].transferTo(input.getRingPosition(), ins[0].size()-input.getRingPosition(), outs[0]);
			ins[1].transferTo(input.getRingPosition(), ins[1].size()-input.getRingPosition(), outs[1]);
			ins[2].transferTo(input.getRingPosition(), ins[2].size()-input.getRingPosition(), outs[2]);
			ins[0].transferTo(0, input.getRingPosition()-1, outs[0]);
			ins[1].transferTo(0, input.getRingPosition()-1, outs[1]);
			ins[2].transferTo(0, input.getRingPosition()-1, outs[2]);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return null;
	}

}
