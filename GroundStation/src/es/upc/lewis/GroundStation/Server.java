package es.upc.lewis.GroundStation;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;

import es.upc.lewis.quadadk.Commands;

public class Server extends Thread {
	private ServerSocket serverSocket;
	int port;

	Socket socket;
	private InputStream input;
	private OutputStream output;
	// Buffer for read operations (bytes)
	private final int READ_BUFFER_SIZE = 1024;

	public Server(int port) {
		this.port = port;
		GUI.serverIsWorking = true;
	}

	public void close() {
		try {
			if(serverSocket != null) { serverSocket.close(); }
			if(      socket != null) {       socket.close(); }
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		GUI.setUi(GUI.DISCONNECTED);
		GUI.serverIsWorking = false;
	}
	
	public void send(byte command) {
		try {
			output.write(new byte[]{command});
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@Override
	public void run() {
		GUI.setUi(GUI.LISTENING);

		try {
			serverSocket = new ServerSocket(port);
		} catch (IOException e) {
			// Port already open
			GUI.showErrorDialog("Port is already open.", "Error");
			GUI.setUi(GUI.DISCONNECTED);
			GUI.serverIsWorking = false;
			return;
		}	
			
		try {	
			socket = serverSocket.accept();

			input = socket.getInputStream();
			output = socket.getOutputStream();
		} catch (IOException e) {
			// ServerSocket closed
			GUI.setUi(GUI.DISCONNECTED);
			GUI.serverIsWorking = false;
			return;
		}

		GUI.setUi(GUI.CONNECTED);
		readLoop();
	}

	private void readLoop() {
		//String string;
		byte[] buffer = new byte[READ_BUFFER_SIZE];
		int bytes;

		while (true) {
			try {
				bytes = input.read(buffer);
				parse(buffer, bytes);
			} catch (IOException e) {
				GUI.setUi(GUI.DISCONNECTED);
				return;
			}
		}
	}
	
	private void parse(byte[] data, int bytes) {
		switch(data[0]) {
		case Commands.SENSOR_1:
		case Commands.SENSOR_2:
		case Commands.SENSOR_3:
			if (bytes != 5) { break; }
			
			// Get integer (4 bytes)
			ByteBuffer bBuffer = ByteBuffer.wrap(data, 1, 4);
			int value = bBuffer.getInt();
			
			GUI.displaySensorData(data[0], value);
			
			break;
		}
	}
}
