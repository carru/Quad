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
	//BufferedReader input;
	//PrintWriter output;
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
	
	/*public void send(byte command, int value) {
		byte[] buffer = new byte[3];
		buffer[0] = command;
		buffer[1] = (byte) (value >> 8);
		buffer[2] = (byte) (value & 0xFF);
		
		try {
			output.write(buffer);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}*/
	
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

			//input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			//output = new PrintWriter(socket.getOutputStream(), true);
			input = socket.getInputStream();
			output = socket.getOutputStream();
		} catch (IOException e) {
			// Server closed
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
				//string = input.readLine();
				//attendCommand(string);
				
				bytes = input.read(buffer);
				parse(buffer, bytes);
			} catch (IOException e) {
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
	
	/*private void attendCommand(String string) {
		if (string == null) { return; }
		if (string.startsWith(Commands.SENSOR_1)) {
			String data = string.substring(Commands.SENSOR_1.length());
			int value = Integer.parseInt(data);
			
			GUI.displaySensorData(Commands.SENSOR_1, value);
		}
		else if (string.startsWith(Commands.SENSOR_2)) {
			String data = string.substring(Commands.SENSOR_2.length());
			int value = Integer.parseInt(data);
			
			GUI.displaySensorData(Commands.SENSOR_2, value);
		}
		else if (string.startsWith(Commands.SENSOR_3)) {
			String data = string.substring(Commands.SENSOR_1.length());
			int value = Integer.parseInt(data);
			
			GUI.displaySensorData(Commands.SENSOR_3, value);
		}
	}*/
}
