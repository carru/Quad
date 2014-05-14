package es.upc.lewis.quadadk;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;
import android.widget.Toast;

public class MissionThread extends Thread {
	private static final int timeToArm = 4000; // Milliseconds
	private static final int timeToDisarm = timeToArm; // Milliseconds
	
	// To abort the mission
	private static volatile boolean isAborted = false;
	
	// To communicate with the Arduino
	private CommunicationsThread arduino;
	
	// To show toasts (useful when testing)
	private MainActivity activity;

	public MissionThread(CommunicationsThread comms, MainActivity activity) {
		arduino = comms;
		this.activity = activity;

		if (comms == null) { return; }

		// Register BroadcastReceiver
		LocalBroadcastManager.getInstance(activity).registerReceiver(
				broadcastReceiver, broadcastIntentFilter());

		start();
	}

	@Override
	public void run() {
		// Arm motors
		showToast("Arming...");
		arm();

		// Set loitter flight mode
		send(ArduinoCommands.SET_MODE_LOITTER);
		
		// Go up for 2 seconds
		send(ArduinoCommands.SET_CH3, 1750);
		wait(2000);
		
		// Hover for 5 seconds
		hover();
		wait(5000);		

		// Disarm motors
		// NOTE: this is just a test. Don't disarm while flying!!
		showToast("Disarming...");
		disarm();
	}

	/**
	 * Send a command (1 byte) to the Arduino if the mission is not aborted.
	 * @param command
	 */
	private void send(byte command) {
		if (!isAborted) { arduino.send(command); }
	}
	
	/**
	 * Send a command (1 byte) and a 4 bytes int to the Arduino if the mission is not aborted.
	 * @param command
	 * @param value
	 */
	private void send(byte command, int value) {
		if (!isAborted) { arduino.send(command, value); }
	}
	
	/**
	 * Return to launch
	 */
	private void abortMission() {
		// Set all sticks to neutral (hover)
		arduino.send(ArduinoCommands.SET_CH1, 1500);
		arduino.send(ArduinoCommands.SET_CH2, 1500);
		arduino.send(ArduinoCommands.SET_CH3, 1500);
		arduino.send(ArduinoCommands.SET_CH4, 1500);
		
		// Return to launch
		arduino.send(ArduinoCommands.SET_MODE_RTL);
	}
	
	/**
	 * Arms motors. Blocks for 'timeToArm' milliseconds.
	 * Leaves roll, pitch and yaw in neutral (1500) and throttle at minimum (1000).
	 */
	private void arm() {
		// Set flight mode to altitude hold (can't arm in loitter)
		send(ArduinoCommands.SET_MODE_ALTHOLD);

		send(ArduinoCommands.SET_CH1, 1500);
		send(ArduinoCommands.SET_CH2, 1500);
		send(ArduinoCommands.SET_CH3, 1000);
		send(ArduinoCommands.SET_CH4, 2000);

		wait(timeToArm);

		//send(ArduinoCommands.SET_CH1, 1500);
		//send(ArduinoCommands.SET_CH2, 1500);
		//send(ArduinoCommands.SET_CH3, 1000);
		send(ArduinoCommands.SET_CH4, 1500);
	}

	/**
	 * Disarms motors. Blocks for 'timeToDisarm' milliseconds.
	 * Leaves roll, pitch and yaw in neutral (1500) and throttle at minimum (1000).
	 */
	private void disarm() {
		send(ArduinoCommands.SET_CH1, 1500);
		send(ArduinoCommands.SET_CH2, 1500);
		send(ArduinoCommands.SET_CH3, 1000);
		send(ArduinoCommands.SET_CH4, 1000);

		wait(timeToDisarm);

		//arduino.send(ArduinoCommands.SET_CH1, 1500);
		//arduino.send(ArduinoCommands.SET_CH2, 1500);
		//arduino.send(ArduinoCommands.SET_CH3, 1000);
		send(ArduinoCommands.SET_CH4, 1500);
	}

	/**
	 * Set roll, pitch, throttle and yaw to neutral (hover)
	 */
	private void hover() {
		send(ArduinoCommands.SET_CH1, 1500);
		send(ArduinoCommands.SET_CH2, 1500);
		send(ArduinoCommands.SET_CH3, 1500);
		send(ArduinoCommands.SET_CH4, 1500);
	}
	
	/**
	 * Sleep
	 * @param time in milliseconds
	 */
	private void wait(int time) {
		try {
			sleep(time);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Show a Toast
	 * @param text to show
	 */
	private void showToast(final String text) {
		activity.runOnUiThread(new Runnable() {
			public void run() {
				Toast.makeText(activity, text, Toast.LENGTH_SHORT).show();
			}
		});
	}

	/**
	 * Receive important events
	 */
	private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();

			if (action.equals(GroundStationClient.ABORT_MISSION)) {
				isAborted = true;
				showToast("Mission aborted!");
				abortMission();
			}
		}
	};

	/**
	 * Intents to listen to
	 */
	private static IntentFilter broadcastIntentFilter() {
		final IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(GroundStationClient.ABORT_MISSION);
		return intentFilter;
	}
}
