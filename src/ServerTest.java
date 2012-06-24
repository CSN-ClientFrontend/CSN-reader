import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

import org.apache.commons.io.IOUtils;

import com.google.gson.Gson;
import com.google.gson.JsonObject;




public class ServerTest {

	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		Socket b = new Socket();
		b.connect(new InetSocketAddress("127.0.0.1", 5632));
		DataOutputStream out = new DataOutputStream(b.getOutputStream());
		out.writeUTF("{ \"startTime\":1340549210000, \"endTime\": 1340572221000   }");
		
		DataInputStream in = new DataInputStream(b.getInputStream());
		String s = in.readUTF();
		System.out.println(s);
		
		Gson g = new Gson();
		Response res = g.fromJson(s, Response.class);
		
		for (Section sect : res.sections)
		{
			System.out.println(sect.length);
			byte[] buffer = new byte[(int) sect.length];
			IOUtils.readFully(in, buffer);
			
		}
		
		System.out.println("I have read all the fun");
		b.close();

	}

}
