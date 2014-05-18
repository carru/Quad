package es.upc.lewis.quadadk;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.ArrayList;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

public class GroundStationClient extends Thread {
	private String TAG = "GroundStationClient";
	
	private Context context;
	
	// Intents
	public static final String CONNECTED = "connected";
	public static final String CONNECTING = "connecting";
	public static final String DISCONNECTED = "disconnected";
	public static final String CANT_RESOLVE_HOST = "host_error";
	public static final String START_MISSION = "start";
	public static final String ABORT_MISSION = "abort";
	
	private String ip;
	private int port;
	
	private Socket socket;
	private InputStream input;
	private OutputStream output;
	// Buffer for read operations (bytes)
	private final int READ_BUFFER_SIZE = 1024;
	
	public GroundStationClient(String ip, int port, Context context) {
		this.context = context;
		
		this.ip = ip;
		this.port = port;
		
		start();
	}
	
	public void send(byte command) {
		try {
			output.write(new byte[]{command});
			output.flush();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void send(byte command, int value) {
		//Log.d(TAG, "Sending command: " + Byte.toString(command) + ", value = " + value);
		
		byte[] buffer = new byte[5];
		buffer[0] = command;
		byte[] intInBytes = ByteBuffer.allocate(4).putInt(value).array();
		for(int i=1; i<5; i++) { buffer[i] = intInBytes[i-1]; }
		
		try {
			output.write(buffer);
			output.flush();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/*public void sendPicture(byte[] data) {
		int PACKET_SIZE = 1024;

		Log.d(TAG, "Picture size: " + Integer.toString(data.length) + " bytes");
		
		ArrayList<Byte> picture = new ArrayList<Byte>(data.length);
		for (int i=0; i<data.length; i++) { picture.add(data[i]); }
		
		byte[] buffer = new byte[PACKET_SIZE];
		// Send picture as blocks of PACKET_SIZE bytes (1 of them being a header)
		while(picture.size() > (PACKET_SIZE - 1)) {
			Log.d(TAG, "Sending picture block");
			
			buffer[0] = GroundStationCommands.PICTURE_START;
			for (int i=1; i<PACKET_SIZE; i++) { buffer[i] = picture.remove(0); }
			
			try {
				output.write(buffer);
				output.flush();
			} catch (IOException e) {
				e.printStackTrace();
				return;
			}
		}
		
		// Send the last block
		if (picture.size() > 0) {
			buffer = new byte[picture.size() + 1];
			
			buffer[0] = GroundStationCommands.PICTURE_END;
			for (int i=0; i<picture.size(); i++) { buffer[i+1] = picture.remove(0); }
			
			try {
				output.write(buffer);
				output.flush();
			} catch (IOException e) {
				e.printStackTrace();
				return;
			}
		}
	}*/
	
	public void sendPicture(byte[] data) {
		Log.d(TAG, "Picture size: " + Integer.toString(data.length) + " bytes");
		
		send(GroundStationCommands.PICTURE_START, data.length);
		
		try {
			output.write(data);
			output.flush();
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
		
		//send(GroundStationCommands.PICTURE_END);
	}
	
	@Override
	public void run() {
		InetAddress addr;
		
		// Resolve host
		try {
			addr = InetAddress.getByName(ip);
		} catch (UnknownHostException e) {
			Log.e(TAG, "Can't resolve host " + ip);
			notifyAction(CANT_RESOLVE_HOST);
			return;
		}
		
		// Connect
		try {
			Log.i(TAG, "Connecting to " + addr.toString() + ":" + String.valueOf(port));
			notifyAction(CONNECTING);
			socket = new Socket(addr, port);
			Log.i(TAG, "Connected");
			notifyAction(CONNECTED);
		} catch (IOException e) {
			Log.e(TAG, "Connection failed");
			notifyAction(DISCONNECTED);
			e.printStackTrace();
			return;
		}
		
		// Get streams
		try {
			input = socket.getInputStream();
			output = socket.getOutputStream();
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
		
		// Read commands from the server
		readLoop();
	}
	
	private void readLoop() {
		byte[] buffer = new byte[READ_BUFFER_SIZE];
		int bytes;
		
		try {
			while (true) {
				if (interrupted()) { return; }
				
				bytes = input.read(buffer);
				parse(buffer, bytes);
			}
		} catch (IOException e) {
			e.printStackTrace();
			disconnect();
			return;
		}
	}
	
	private void disconnect() {
		notifyAction(DISCONNECTED);
	}
	
	private void parse(byte[] data, int bytes) {
		switch(data[0]) {
		case GroundStationCommands.START_MISSION:
			notifyAction(START_MISSION);
			break;
			
		case GroundStationCommands.ABORT_MISSION:
			notifyAction(ABORT_MISSION);
			break;
		}
	}
	
	private void notifyAction(String status) {
		Intent intent = new Intent(status);
		LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
	}
}
