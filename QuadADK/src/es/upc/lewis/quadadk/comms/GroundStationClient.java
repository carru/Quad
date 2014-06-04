package es.upc.lewis.quadadk.comms;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

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
	
	public static volatile boolean readyToSend = true;
	
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
			
			readyToSend = false;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void send(byte command, int value) {
		if (!readyToSend) { return; }
		
		byte[] buffer = new byte[5];
		buffer[0] = command;
		byte[] intInBytes = ByteBuffer.allocate(4).putInt(value).array();
		for(int i=1; i<5; i++) { buffer[i] = intInBytes[i-1]; }
		
		try {
			output.write(buffer);
			output.flush();
			
			readyToSend = false;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void sendPicture(byte[] data) {
		if (!readyToSend) { return; }
		
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
			
		case GroundStationCommands.ACK:
			readyToSend = true;
			break;
		}
	}
	
	private void notifyAction(String status) {
		Intent intent = new Intent(status);
		LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
	}
}
