package es.upc.lewis.GroundStation;

import javax.imageio.ImageIO;
import javax.swing.*;

import java.awt.Image;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import es.upc.lewis.quadadk.comms.GroundStationCommands;
import java.awt.Font;
import java.awt.Color;

public class GUI {
	// UI
	private static JFrame frmGroundstation;
	private static JLabel statusLabel;
	private static JButton startButton;
	private static JButton btnStartMission;
	private static JButton btnAbort;
	private static JLabel sensorTempLabel;
	private static JLabel sensorHumLabel;
	private static JLabel sensorNO2Label;
	private static JLabel sensorCOLabel;
	private JLabel lblPort;
	private JTextField portText;
	private static JLabel pictureLabel;
	private static JLabel lblMissionIsRunning;
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
					//GUI window = new GUI();
					//window.frmGroundstation.setVisible(true);
					new GUI();
					GUI.frmGroundstation.setVisible(true);
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
		frmGroundstation.setBounds(100, 100, 618, 363);
		frmGroundstation.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frmGroundstation.getContentPane().setLayout(null);
		
		statusLabel = new JLabel("Status: Disconnected");
		statusLabel.setBounds(125, 40, 125, 14);
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
				server.send(GroundStationCommands.START_MISSION);
			}
		});
		btnStartMission.setBounds(10, 115, 240, 40);
		frmGroundstation.getContentPane().add(btnStartMission);
		
		sensorTempLabel = new JLabel("Temperature [\u00BAC]:");
		sensorTempLabel.setBounds(20, 234, 230, 14);
		frmGroundstation.getContentPane().add(sensorTempLabel);
		
		sensorHumLabel = new JLabel("Humidity [%]:");
		sensorHumLabel.setBounds(20, 259, 230, 14);
		frmGroundstation.getContentPane().add(sensorHumLabel);
		
		sensorNO2Label = new JLabel("NO2 [ppb]:");
		sensorNO2Label.setBounds(20, 284, 230, 14);
		frmGroundstation.getContentPane().add(sensorNO2Label);
		
		sensorCOLabel = new JLabel("CO [ppb]:");
		sensorCOLabel.setBounds(20, 309, 230, 14);
		frmGroundstation.getContentPane().add(sensorCOLabel);
		
		lblPort = new JLabel("Port");
		lblPort.setBounds(10, 11, 33, 14);
		frmGroundstation.getContentPane().add(lblPort);
		
		portText = new JTextField();
		portText.setText("9090");
		portText.setToolTipText("Port number");
		portText.setBounds(47, 8, 68, 20);
		frmGroundstation.getContentPane().add(portText);
		portText.setColumns(10);
		
		btnAbort = new JButton("Abort (return to launch)");
		btnAbort.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				server.send(GroundStationCommands.ABORT_MISSION);
			}
		});
		btnAbort.setEnabled(false);
		btnAbort.setBounds(10, 166, 240, 40);
		frmGroundstation.getContentPane().add(btnAbort);
		
		pictureLabel = new JLabel("Last taken picture will appear here", SwingConstants.CENTER);
		pictureLabel.setBounds(260, 11, 342, 312);
		frmGroundstation.getContentPane().add(pictureLabel);
		
		lblMissionIsRunning = new JLabel("Mission is RUNNING");
		lblMissionIsRunning.setForeground(Color.GREEN);
		lblMissionIsRunning.setHorizontalAlignment(SwingConstants.CENTER);
		lblMissionIsRunning.setFont(new Font("Tahoma", Font.PLAIN, 18));
		lblMissionIsRunning.setBounds(10, 90, 240, 14);
		frmGroundstation.getContentPane().add(lblMissionIsRunning);
		lblMissionIsRunning.setVisible(false);
	}
	
	public static void displayMissionIsRunning(final boolean running) {
		// Make sure we are in the proper thread
				if (!SwingUtilities.isEventDispatchThread()) {
					SwingUtilities.invokeLater(new Runnable() {
						@Override
						public void run() {
							displayMissionIsRunning(running);
						}
					});
				}
		
		lblMissionIsRunning.setVisible(running);
	}
	
	public static void showErrorDialog(String message, String title) {
		JOptionPane.showMessageDialog(null, message, title, JOptionPane.ERROR_MESSAGE);
	}
	
	public static void displaySensorData(final byte sensor, final float value) {
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
		case GroundStationCommands.SENSOR_TEMPERATURE:
			sensorTempLabel.setText("Temperature [ºC]: " + Float.toString(value));
			break;
			
		case GroundStationCommands.SENSOR_HUMIDITY:
			sensorHumLabel.setText("Humidity [%]: " + Float.toString(value));
			break;
			
		case GroundStationCommands.SENSOR_NO2:
			sensorNO2Label.setText("NO2 [ppb]: " + Float.toString(value));
			break;
		case GroundStationCommands.SENSOR_CO:
			sensorCOLabel.setText("CO [ppb]: " + Float.toString(value));
			break;
		}
	}
	
	public static void displayPicture(final byte[] picture) {
		// Make sure we are in the proper thread
		if (!SwingUtilities.isEventDispatchThread()) {
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					displayPicture(picture);
				}
			});
		}
		
		// Image needs to be scaled
		InputStream in = new ByteArrayInputStream(picture);
		BufferedImage image;
		try {
			image = ImageIO.read(in);
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
		Image scaledImage = image.getScaledInstance(pictureLabel.getWidth(), pictureLabel.getHeight(), Image.SCALE_SMOOTH);
		pictureLabel.setIcon(new ImageIcon(scaledImage));
		pictureLabel.setText("");
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
			btnAbort.setEnabled(true);
			break;
			
		case DISCONNECTED:
			statusLabel.setText("Status: Disconnected");
			startButton.setText("Start server");
			btnStartMission.setEnabled(false);
			btnAbort.setEnabled(false);
			break;
			
		case LISTENING:
			statusLabel.setText("Status: Listening");
			startButton.setText("Stop server");
			btnStartMission.setEnabled(false);
			btnAbort.setEnabled(false);
			break;
		}
	}
}
