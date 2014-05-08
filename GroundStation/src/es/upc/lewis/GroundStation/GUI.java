package es.upc.lewis.GroundStation;

import javax.swing.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

public class GUI {
	// UI
	private static JLabel statusLabel;
	private static JButton startButton;
	// UI states
	public static final int CONNECTED = 1;
	public static final int DISCONNECTED = 2;
	public static final int LISTENING = 3;
	
	// Server
	private Server server;
	public static volatile boolean serverIsWorking = false;

	private JFrame frmGroundstation;

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
		frmGroundstation.setBounds(100, 100, 343, 300);
		frmGroundstation.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frmGroundstation.getContentPane().setLayout(null);
		
		statusLabel = new JLabel("Status: Disconnected");
		statusLabel.setBounds(125, 15, 146, 14);
		frmGroundstation.getContentPane().add(statusLabel);
		
		startButton = new JButton("Start server");
		startButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				if (!serverIsWorking) {
					server = new Server(9090);
					server.start();
				}
				else {
					// Stop server
					server.close();
				}
			}
		});
		startButton.setBounds(10, 11, 105, 23);
		frmGroundstation.getContentPane().add(startButton);
	}
	
	public static void showErrorDialog(String message, String title) {
		JOptionPane.showMessageDialog(null, message, title, JOptionPane.ERROR_MESSAGE);
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
			break;
		case DISCONNECTED:
			statusLabel.setText("Status: Disconnected");
			startButton.setText("Start server");
			break;
		case LISTENING:
			statusLabel.setText("Status: Listening");
			startButton.setText("Stop server");
			break;
		}
	}
}
