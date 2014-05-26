package es.upc.lewis.quadadk.mission;

import es.upc.lewis.quadadk.comms.ArduinoCommands;

/**
 * Tries to maintain a fixed yaw value
 * It may not be necessary but I have not deleted it yet
 */
public class FixYaw extends Thread {
	private int desiredYaw;
	private int updateInterval = 2000; // Milliseconds
	private static volatile boolean enabled = true;

	// To communicate with the Arduino
	private MissionUtils utils;

	public FixYaw(MissionUtils utils, int desiredYaw) {
		this.utils = utils;
		this.desiredYaw = desiredYaw;

		start();
	}

	public void stopWorking() {
		enabled = false;
	}

	@Override
	public void run() {
		try {
			int currentYaw;
			int diff;

			while (enabled) {
				// Read orientation (yaw)
				currentYaw = 0;

				diff = currentYaw - desiredYaw;
				if (diff > 1) {
					// Rotate counterclockwise
					utils.send(ArduinoCommands.SET_CH1, 1100);
				} else if (diff < -1) {
					// Rotate clockwise
					utils.send(ArduinoCommands.SET_CH1, 1900);
				}
				
				utils.wait(updateInterval);
			}
		} catch (AbortException e) {
			// Mission has been interrupted
			stopWorking();
		}
	}
}
