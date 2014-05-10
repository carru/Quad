package es.upc.lewis.GroundStation;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

import es.upc.lewis.quadadk.Commands;

public class Server extends Thread {
	private ServerSocket serverSocket;
	int port;

	Socket socket;
	BufferedReader input;
	PrintWriter output;

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

			input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			//output = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
			output = new PrintWriter(socket.getOutputStream(), true);
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
		String string;

		while (true) {
			try {
				string = input.readLine();
				attendCommand(string);
			} catch (IOException e) {
				return;
			}
		}
	}
	
	public void write(String string) {
		output.print(string);
		output.println();
	}
	
	private void attendCommand(String string) {
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
	}
}
