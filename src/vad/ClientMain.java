package vad;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

import com.nwgjb.xrmi.RMIConnection;

public class ClientMain
{
	public static void main(String[] args) throws UnknownHostException, IOException{
		new RMIConnection(new Socket("localhost", 12345), new ClientPlayerFactory()
		{
			@Override
			public Player create(int color)
			{
				return new UserPlayer(color);
			}
		});
	}
}
