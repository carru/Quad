package es.upc.lewis.quadadk.mission;

import es.upc.lewis.quadadk.MainActivity;
import es.upc.lewis.quadadk.comms.ArduinoCommands;
import es.upc.lewis.quadadk.comms.CommunicationsThread;
import es.upc.lewis.quadadk.comms.GroundStationClient;
import es.upc.lewis.quadadk.comms.GroundStationCommands;
import android.widget.Toast;

public class MissionUtils {
	private static final int TIME_TO_ARM     = 5000;        // Milliseconds
	private static final int TIME_TO_DISARM  = TIME_TO_ARM; // Milliseconds
	private static final int TIME_TO_TAKEOFF = 20000;       // Milliseconds
	
	// Sensors
	public static final byte TEMPERATURE = 0x01;
	public static final byte HUMIDITY    = 0x02;
	public static final byte NO2         = 0x03;
	public static final byte CO          = 0x04;
	
	// Channel values
	public static final int THROTTLE_MIN     = 1150; // Throttle has a different minimum value
	public static final int THROTTLE_NEUTRAL = 1650;
	public static final int CH_MIN       = 1000;
	public static final int CH_NEUTRAL   = 1500;
	public static final int CH_MAX       = 2000;
	
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
		
		returnToLaunch();
	}
	
	/**
	 * Arms motors. Blocks for 'timeToArm' milliseconds. Switches to Altitude Hold flight mode
	 * Leaves roll, pitch and yaw in neutral (1500) and throttle at minimum (1000).
	 */
	public void arm() throws AbortException { //TODO: change back to private
		// Set flight mode to altitude hold (can't arm in loitter)
		send(ArduinoCommands.SET_MODE_ALTHOLD);

		send(ArduinoCommands.SET_CH1, CH_NEUTRAL);
		send(ArduinoCommands.SET_CH2, CH_NEUTRAL);
		send(ArduinoCommands.SET_CH3, THROTTLE_MIN);
		send(ArduinoCommands.SET_CH4, CH_MAX);

		wait(TIME_TO_ARM);

		send(ArduinoCommands.SET_CH4, CH_NEUTRAL);
	}

	/**
	 * Disarms motors. Blocks for 'timeToDisarm' milliseconds.
	 * Leaves roll, pitch and yaw in neutral (CH_NEUTRAL) and throttle at minimum (THROTTLE_MIN).
	 */
	@SuppressWarnings("unused")
	private void disarm() throws AbortException {
		send(ArduinoCommands.SET_CH1, CH_NEUTRAL);
		send(ArduinoCommands.SET_CH2, CH_NEUTRAL);
		send(ArduinoCommands.SET_CH3, THROTTLE_MIN);
		send(ArduinoCommands.SET_CH4, CH_MIN);

		wait(TIME_TO_DISARM);

		send(ArduinoCommands.SET_CH4, CH_NEUTRAL);
	}
	
	//TODO: delete when done debugging
	public void disarm_NO_EXCEPTION_DEBUG_ONLY() {
		arduino.send(ArduinoCommands.SET_CH1, CH_NEUTRAL);
		arduino.send(ArduinoCommands.SET_CH2, CH_NEUTRAL);
		arduino.send(ArduinoCommands.SET_CH3, THROTTLE_MIN);
		arduino.send(ArduinoCommands.SET_CH4, CH_MIN);

		waitWithoutException(TIME_TO_DISARM);

		arduino.send(ArduinoCommands.SET_CH4, CH_NEUTRAL);
	}

	/**
	 * Set roll, pitch, throttle and yaw to neutral (hover)
	 */
	public void hover() throws AbortException {
		send(ArduinoCommands.SET_CH1, CH_NEUTRAL);
		send(ArduinoCommands.SET_CH2, CH_NEUTRAL);
		send(ArduinoCommands.SET_CH3, THROTTLE_NEUTRAL);
		send(ArduinoCommands.SET_CH4, CH_NEUTRAL);
	}
	
	/**
	 * Arm motors and ascend to a predefined altitude
	 * Ends after (TIME_TO_TAKEOFF + 1000) milliseconds and with Loitter flight mode
	 * @throws AbortException
	 */
	public void takeoff() throws AbortException {
		// Arm motors
		arm();
		
		// Switch to Auto mode
		send(ArduinoCommands.SET_MODE_AUTO);
		
		// Wait before starting 3DR Iris mission
		wait(1000);
		
		// Raise throttle to start mission
		send(ArduinoCommands.SET_CH3, THROTTLE_NEUTRAL);
		
		// Wait so it has time to ascend
		wait(TIME_TO_TAKEOFF);
		
		// Switch to Loitter mode
		send(ArduinoCommands.SET_MODE_LOITTER);
	}
	
	/**
	 * Return to launch position, land and disarm. Executes even if mission is aborted
	 */
	public void returnToLaunch() {
		// Hover
		arduino.send(ArduinoCommands.SET_CH1, CH_NEUTRAL);
		arduino.send(ArduinoCommands.SET_CH2, CH_NEUTRAL);
		arduino.send(ArduinoCommands.SET_CH3, THROTTLE_NEUTRAL);
		arduino.send(ArduinoCommands.SET_CH4, CH_NEUTRAL);
		
		// Set return to launch mode
		arduino.send(ArduinoCommands.SET_MODE_RTL);
		
		// Wait some time so it engages RTL
		waitWithoutException(2000);
				
		// Set throttle to low (auto disarm after landing)
		arduino.send(ArduinoCommands.SET_CH3, THROTTLE_MIN);
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
	}
	
	/**
	 * Read sensor and send result to the GroundStation
	 * @throws AbortException 
	 */
	public void readSensor(byte sensor) throws AbortException {
		switch (sensor) {
		case TEMPERATURE:
			send(ArduinoCommands.READ_SENSOR_TEMPERATURE);
			break;
		case HUMIDITY:
			send(ArduinoCommands.READ_SENSOR_HUMIDITY);
			break;
		case NO2:
			send(ArduinoCommands.READ_SENSOR_NO2);
			break;
		case CO:
			send(ArduinoCommands.READ_SENSOR_CO);
			break;
		}
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
	
	private void waitWithoutException(int time) {
		try { Thread.sleep(time); }
		// Abort mission if thread is interrupted
		catch (InterruptedException e) { return; }
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
		// (may be added in the future)
		
		// Notify mission is over
		MainActivity.isMissionRunning = false;
		server.send(GroundStationCommands.MISSION_END);
	}
}
