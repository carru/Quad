package es.upc.lewis.GroundStation;

import javax.swing.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

import es.upc.lewis.quadadk.Commands;

public class GUI {
	// UI
	private JFrame frmGroundstation;
	private static JLabel statusLabel;
	private static JButton startButton;
	private static JButton btnStartMission;
	private static JLabel sensor1Label;
	private static JLabel sensor2Label;
	private static JLabel sensor3Label;
	private JLabel lblPort;
	private JTextField portText;
	// UI states
	public static final int CONNECTED = 1;
	public static final int DISCONNECTED = 2;
	public static final int LISTENING = 3;
	
	// Server
	private Server server;
	public static volatile boolean serverIsWorking = false;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				try {
					GUI window = new GUI();
					window.frmGroundstation.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the application.
	 */
	public GUI() {
		initialize();
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		frmGroundstation = new JFrame();
		frmGroundstation.setResizable(false);
		frmGroundstation.setTitle("GroundStation");
		frmGroundstation.setBounds(100, 100, 295, 300);
		frmGroundstation.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frmGroundstation.getContentPane().setLayout(null);
		
		statusLabel = new JLabel("Status: Disconnected");
		statusLabel.setBounds(125, 40, 154, 14);
		frmGroundstation.getContentPane().add(statusLabel);
		
		startButton = new JButton("Start server");
		startButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				if (!serverIsWorking) {
					server = new Server(Integer.parseInt((portText.getText())));
					server.start();
				}
				else {
					// Stop server
					server.close();
				}
			}
		});
		startButton.setBounds(10, 36, 105, 23);
		frmGroundstation.getContentPane().add(startButton);
		
		btnStartMission = new JButton("Start mission");
		btnStartMission.setEnabled(false);
		btnStartMission.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				server.send(Commands.START_MISSION);
			}
		});
		btnStartMission.setBounds(10, 70, 269, 23);
		frmGroundstation.getContentPane().add(btnStartMission);
		
		sensor1Label = new JLabel("Sensor 1: ");
		sensor1Label.setBounds(10, 104, 269, 14);
		frmGroundstation.getContentPane().add(sensor1Label);
		
		sensor2Label = new JLabel("Sensor 2: ");
		sensor2Label.setBounds(10, 129, 269, 14);
		frmGroundstation.getContentPane().add(sensor2Label);
		
		sensor3Label = new JLabel("Sensor 3: ");
		sensor3Label.setBounds(10, 154, 269, 14);
		frmGroundstation.getContentPane().add(sensor3Label);
		
		lblPort = new JLabel("Port");
		lblPort.setBounds(10, 11, 33, 14);
		frmGroundstation.getContentPane().add(lblPort);
		
		portText = new JTextField();
		portText.setText("9090");
		portText.setToolTipText("Port number");
		portText.setBounds(47, 8, 68, 20);
		frmGroundstation.getContentPane().add(portText);
		portText.setColumns(10);
	}
	
	public static void showErrorDialog(String message, String title) {
		JOptionPane.showMessageDialog(null, message, title, JOptionPane.ERROR_MESSAGE);
	}
	
	public static void displaySensorData(final byte sensor, final int value) {
		// Make sure we are in the proper thread
		if (!SwingUtilities.isEventDispatchThread()) {
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					displaySensorData(sensor, value);
				}
			});
		}
		
		switch(sensor) {
		case Commands.SENSOR_1:
			sensor1Label.setText("Sensor 1: " + Integer.toString(value));
			break;
			
		case Commands.SENSOR_2:
			sensor2Label.setText("Sensor 2: " + Integer.toString(value));
			break;
			
		case Commands.SENSOR_3:
			sensor3Label.setText("Sensor 3: " + Integer.toString(value));
			break;
		}
		
		/*if (sensor.equals(Commands.SENSOR_1)) {
			sensor1Label.setText("Sensor 1: " + Integer.toString(value));
		}
		else if (sensor.equals(Commands.SENSOR_2)) {
			sensor2Label.setText("Sensor 2: " + Integer.toString(value));
		}
		else if (sensor.equals(Commands.SENSOR_3)) {
			sensor3Label.setText("Sensor 3: " + Integer.toString(value));
		}*/
	}

	public static void setUi(final int type) {
		// Make sure we are in the proper thread
		if (!SwingUtilities.isEventDispatchThread()) {
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					setUi(type);
				}
			});
		}

		switch (type) {
		case CONNECTED:
			statusLabel.setText("Status: Connected");
			startButton.setText("Stop server");
			btnStartMission.setEnabled(true);
			
			break;
		case DISCONNECTED:
			statusLabel.setText("Status: Disconnected");
			startButton.setText("Start server");
			btnStartMission.setEnabled(false);
			break;
		case LISTENING:
			statusLabel.setText("Status: Listening");
			startButton.setText("Stop server");
			btnStartMission.setEnabled(false);
			break;
		}
	}
}
