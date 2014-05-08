package es.upc.lewis.quadadk;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

public class GroundStationClient extends Thread {
	private String TAG = "GroundStationClient";
	
	private Context context;
	
	// Intents
	public static final String CONNECTED = "connected";
	public static final String CONNECTING = "connecting";
	public static final String DISCONNECTED = "disconnected";
	public static final String CANT_RESOLVE_HOST = "host_error";
	
	private String ip;
	private int port;
	
	private Socket socket;
	BufferedReader input;
	PrintWriter output;
	
	public GroundStationClient(String ip, int port, Context context) {
		this.context = context;
		
		this.ip = ip;
		this.port = port;
		
		start();
	}
	
	@Override
	public void run() {
		InetAddress addr;
		
		// Resolve host
		try {
			addr = InetAddress.getByName(ip);
		} catch (UnknownHostException e) {
			Log.e(TAG, "Can't resolve host " + ip);
			notifyStatus(CANT_RESOLVE_HOST);
			return;
		}
		
		// Connect
		try {
			Log.i(TAG, "Connecting to " + addr.toString() + ":" + String.valueOf(port));
			notifyStatus(CONNECTING);
			socket = new Socket(addr, port);
			Log.i(TAG, "Connected");
			notifyStatus(CONNECTED);
		} catch (IOException e) {
			Log.e(TAG, "Connection failed");
			notifyStatus(DISCONNECTED);
			e.printStackTrace();
		}
		
		// Get streams
		try {
			input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			output = new PrintWriter(socket.getOutputStream(), true);
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
		
		// Read commands from the server
		readLoop();
	}
	
	private void readLoop() {
		String string;
		
		try {
			while (true) {
				if (interrupted()) { return; }
				
				string = input.readLine();
			}
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
	}
	
	private void notifyStatus(String status) {
		Intent intent = new Intent(status);
		LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
	}
}
