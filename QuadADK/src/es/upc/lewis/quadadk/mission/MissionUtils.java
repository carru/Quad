package es.upc.lewis.quadadk.mission;

import es.upc.lewis.quadadk.MainActivity;
import es.upc.lewis.quadadk.comms.ArduinoCommands;
import es.upc.lewis.quadadk.comms.CommunicationsThread;
import es.upc.lewis.quadadk.comms.GroundStationClient;
import es.upc.lewis.quadadk.comms.GroundStationCommands;
import android.widget.Toast;

public class MissionUtils {
	private static final int timeToArm            = 4000;      // Milliseconds
	private static final int timeToDisarm         = timeToArm; // Milliseconds
	private static final int TIME_TO_SEND_PICTURE = 2000;      // Milliseconds
	private static final int TIME_TO_READ_SENSOR  = 100;       // Milliseconds
	private static final int TIME_TO_TAKEOFF      = 10000;       // Milliseconds
	
	// To abort the mission
	private static volatile boolean isAborted = false;
	
	// To communicate with the Arduino
	private CommunicationsThread arduino;
	
	// To communicate with the GroundStation
	private GroundStationClient server;
	
	// To show toasts (useful when testing)
	private MainActivity activity;
	
	public MissionUtils(CommunicationsThread comms, GroundStationClient server, MainActivity activity) {
		arduino = comms;
		this.activity = activity;
		this.server = server;
		
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
	 * Arms motors. Blocks for 'timeToArm' milliseconds. Switches to Altitude Hold flight mode
	 * Leaves roll, pitch and yaw in neutral (1500) and throttle at minimum (1000).
	 */
	private void arm() throws AbortException {
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
	@SuppressWarnings("unused")
	private void disarm() throws AbortException {
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
	 * Arm motors and ascend to a predefined altitude
	 * Ends after X milliseconds and with Loitter flight mode
	 * @throws AbortException
	 */
	public void takeoff() throws AbortException {
		// Arm motors
		arm();
		
		// Switch to Auto mode
		send(ArduinoCommands.SET_MODE_AUTO);
		
		// Raise throttle to start mission
		send(ArduinoCommands.SET_CH3, 1500);
		
		// Wait so it has time to ascend
		wait(TIME_TO_TAKEOFF);
		
		// Switch to Loitter mode
		send(ArduinoCommands.SET_MODE_LOITTER);
	}
	
	/**
	 * Return to launch position, land and disarm
	 */
	public void returnToLaunch() {
		// Set return to launch mode
		arduino.send(ArduinoCommands.SET_MODE_RTL);
				
		// Set throttle to low (auto disarm)
		arduino.send(ArduinoCommands.SET_CH3, 1000);
	}
	
	/**
	 * Take a picture and send it to the GroundStation. Blocks for TIME_TO_SEND_PICTURE milliseconds
	 * @throws AbortException 
	 */
	public void takePicture() throws AbortException {
		if (isAborted) { throw new AbortException(); }

		if (MainActivity.camera != null) {
			if (MainActivity.camera.isReady()) { MainActivity.camera.takePicture(); }
		}
		
		// Give some time for the picture to be transmitted
		wait(TIME_TO_SEND_PICTURE);
	}
	
	/**
	 * Read temperature sensor and send result to the GroundStation
	 * @throws AbortException 
	 */
	public void readSensorTemperature() throws AbortException {
		send(ArduinoCommands.READ_SENSOR_TEMPERATURE);
		
		wait(TIME_TO_READ_SENSOR);
	}
	
	/**
	 * Read humidity sensor and send result to the GroundStation
	 * @throws AbortException 
	 */
	public void readSensorHumidity() throws AbortException {
		send(ArduinoCommands.READ_SENSOR_HUMIDITY);
		
		wait(TIME_TO_READ_SENSOR);
	}
	
	/**
	 * Read NO2 sensor and send result to the GroundStation
	 * @throws AbortException 
	 */
	public void readSensorNO2() throws AbortException {
		send(ArduinoCommands.READ_SENSOR_NO2);
		
		wait(TIME_TO_READ_SENSOR);
	}
	
	/**
	 * Read CO sensor and send result to the GroundStation
	 * @throws AbortException 
	 */
	public void readSensorCO() throws AbortException {
		send(ArduinoCommands.READ_SENSOR_CO);
		
		wait(TIME_TO_READ_SENSOR);
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
	
	/**
	 * End mission. Must be called at the end of your mission
	 */
	public void endMission() {
		// Clear / stop additional threads that were working
		// (maybe added in the future)
		
		// Notify mission is over
		MainActivity.isMissionRunning = false;
		server.send(GroundStationCommands.MISSION_END);
	}
}
