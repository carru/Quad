package es.upc.lewis.quadadk.mission;

import es.upc.lewis.quadadk.MainActivity;
import es.upc.lewis.quadadk.comms.ArduinoCommands;
import es.upc.lewis.quadadk.comms.CommunicationsThread;
import android.widget.Toast;

public class MissionUtils {
	private static final int timeToArm = 4000; // Milliseconds
	private static final int timeToDisarm = timeToArm; // Milliseconds
	
	// To abort the mission
	private static volatile boolean isAborted = false;
	
	// To communicate with the Arduino
	private CommunicationsThread arduino;
	
	// To show toasts (useful when testing)
	private MainActivity activity;
	
	public MissionUtils(CommunicationsThread comms, MainActivity activity) {
		arduino = comms;
		this.activity = activity;
		
		isAborted = false;
	}
	
	/**
	 * Send a command (1 byte) to the Arduino if the mission is not aborted.
	 * @param command
	 * @throws AbortException 
	 */
	public void send(byte command) throws AbortException {
		if (!isAborted) { arduino.send(command); }
		else { throw new AbortException(); }
	}
	
	/**
	 * Send a command (1 byte) and a 4 bytes int to the Arduino if the mission is not aborted.
	 * @param command
	 * @param value
	 * @throws AbortException 
	 */
	public void send(byte command, int value) throws AbortException {
		if (!isAborted) { arduino.send(command, value); }
		else { throw new AbortException(); }
	}
	
	/**
	 * Abort mission, return to launch and disarm
	 */
	public void abortMission() {
		isAborted = true;
		
		// Set all sticks to neutral (hover)
		arduino.send(ArduinoCommands.SET_CH1, 1500);
		arduino.send(ArduinoCommands.SET_CH2, 1500);
		arduino.send(ArduinoCommands.SET_CH3, 1500);
		arduino.send(ArduinoCommands.SET_CH4, 1500);
		
		// Return to launch
		arduino.send(ArduinoCommands.SET_MODE_RTL);
		
		// Set throttle to low (auto disarm)
		arduino.send(ArduinoCommands.SET_CH3, 1000);
	}
	
	/**
	 * Arms motors. Blocks for 'timeToArm' milliseconds.
	 * Leaves roll, pitch and yaw in neutral (1500) and throttle at minimum (1000).
	 */
	public void arm() throws AbortException {
		// Set flight mode to altitude hold (can't arm in loitter)
		send(ArduinoCommands.SET_MODE_ALTHOLD);

		send(ArduinoCommands.SET_CH1, 1500);
		send(ArduinoCommands.SET_CH2, 1500);
		send(ArduinoCommands.SET_CH3, 1000);
		send(ArduinoCommands.SET_CH4, 2000);

		wait(timeToArm);

		send(ArduinoCommands.SET_CH4, 1500);
	}

	/**
	 * Disarms motors. Blocks for 'timeToDisarm' milliseconds.
	 * Leaves roll, pitch and yaw in neutral (1500) and throttle at minimum (1000).
	 */
	public void disarm() throws AbortException {
		send(ArduinoCommands.SET_CH1, 1500);
		send(ArduinoCommands.SET_CH2, 1500);
		send(ArduinoCommands.SET_CH3, 1000);
		send(ArduinoCommands.SET_CH4, 1000);

		wait(timeToDisarm);

		send(ArduinoCommands.SET_CH4, 1500);
	}

	/**
	 * Set roll, pitch, throttle and yaw to neutral (hover)
	 */
	public void hover() throws AbortException {
		send(ArduinoCommands.SET_CH1, 1500);
		send(ArduinoCommands.SET_CH2, 1500);
		send(ArduinoCommands.SET_CH3, 1500);
		send(ArduinoCommands.SET_CH4, 1500);
	}
	
	/**
	 * Take a picture and send it to the GroundStation
	 */
	public void takePicture() {
		if (MainActivity.camera != null) {
			if (MainActivity.camera.isReady()) { MainActivity.camera.takePicture(); }
		}
	}
	
	/**
	 * Read sensor 1 and send value to the GroundStation
	 */
	public void readSensor1() {
		arduino.send(ArduinoCommands.READ_SENSOR_1);
	}
	
	/**
	 * Sleep
	 * @param time in milliseconds
	 * @throws AbortException 
	 */
	public void wait(int time) throws AbortException {
		try { Thread.sleep(time); }
		// Abort mission if thread is interrupted
		catch (InterruptedException e) { throw new AbortException(); }
	}
	
	/**
	 * Show a Toast
	 * @param text to show
	 */
	public void showToast(final String text) {
		activity.runOnUiThread(new Runnable() {
			public void run() {
				Toast.makeText(activity, text, Toast.LENGTH_SHORT).show();
			}
		});
	}
}
