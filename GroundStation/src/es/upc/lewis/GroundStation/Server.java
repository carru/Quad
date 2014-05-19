package es.upc.lewis.GroundStation;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import es.upc.lewis.quadadk.GroundStationCommands;

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
		//byte[] buffer = new byte[READ_BUFFER_SIZE];
		//int bytes;

		while (true) {
			try {
				//bytes = input.read(buffer);
				//parse(buffer, bytes);
				
				attendCommand((byte) input.read());
			} catch (IOException e) {
				GUI.setUi(GUI.DISCONNECTED);
				return;
			}
		}
	}
	
	private void attendCommand(byte command) throws IOException {
		byte[] buffer = new byte[READ_BUFFER_SIZE];
		int bytes;
		
		ByteBuffer bBuffer;
		
		switch(command) {
		case GroundStationCommands.SENSOR_1:
		case GroundStationCommands.SENSOR_2:
		case GroundStationCommands.SENSOR_3:
			bytes = input.read(buffer);
			if (bytes < 4) { break; }
			
			// Get integer (4 bytes)
			bBuffer = ByteBuffer.wrap(buffer, 0, 4);
			int value = bBuffer.getInt();
			
			SaveData.saveSensor1(value); // This is just a placeholder
			GUI.displaySensorData(command, value);
			
			break;
			
		case GroundStationCommands.PICTURE_START:
			// Get data length (4 bytes)
			for (int i=0; i<4; i++) { buffer[i] = (byte) input.read(); }
			bBuffer = ByteBuffer.wrap(buffer, 0, 4);
			int length = bBuffer.getInt();
			
			// Read picture
			byte[] picture = new byte[length];
			for (int i=0; i<length; i++) { picture[i] = (byte) input.read(); }

			SaveData.savePicture(picture);
			GUI.displayPicture(picture);
			
			break;
		}
	}
}
