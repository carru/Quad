package es.upc.lewis.quadadk;

import android.widget.Toast;

public class MissionThread extends Thread {
	private static final int timeToArm = 4000; // Milliseconds
	private static final int timeToDisarm = timeToArm; // Milliseconds
	
	private CommunicationsThread arduino;
	
	// To show toasts (useful when testing)
	private MainActivity activity;

	public MissionThread(CommunicationsThread comms, MainActivity activity) {
		arduino = comms;
		this.activity = activity;

		if (comms == null) { return; }
		start();
	}

	@Override
	public void run() {
		showToast("Arming...");
		arm();

		wait(5000);

		showToast("Disarming...");
		disarm();
	}

	/**
	 * Arms motors. Blocks for 'timeToArm' milliseconds.
	 */
	private void arm() {
		// Set flight mode to altitude hold (can't arm in loitter)
		arduino.send(ArduinoCommands.SET_MODE_ALTHOLD);

		arduino.send(ArduinoCommands.SET_CH1, 1500);
		arduino.send(ArduinoCommands.SET_CH2, 1500);
		arduino.send(ArduinoCommands.SET_CH3, 1000);
		arduino.send(ArduinoCommands.SET_CH4, 2000);

		wait(timeToArm);

		arduino.send(ArduinoCommands.SET_CH1, 1500);
		arduino.send(ArduinoCommands.SET_CH2, 1500);
		arduino.send(ArduinoCommands.SET_CH3, 1000);
		arduino.send(ArduinoCommands.SET_CH4, 1500);
	}

	/**
	 * Disarms motors. Blocks for 'timeToDisarm' milliseconds.
	 */
	private void disarm() {
		arduino.send(ArduinoCommands.SET_CH1, 1500);
		arduino.send(ArduinoCommands.SET_CH2, 1500);
		arduino.send(ArduinoCommands.SET_CH3, 1000);
		arduino.send(ArduinoCommands.SET_CH4, 1000);

		wait(timeToDisarm);

		arduino.send(ArduinoCommands.SET_CH1, 1500);
		arduino.send(ArduinoCommands.SET_CH2, 1500);
		arduino.send(ArduinoCommands.SET_CH3, 1000);
		arduino.send(ArduinoCommands.SET_CH4, 1500);
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
}
