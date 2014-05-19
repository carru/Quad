package es.upc.lewis.quadadk.mission;

import es.upc.lewis.quadadk.MainActivity;
import es.upc.lewis.quadadk.comms.ArduinoCommands;
import es.upc.lewis.quadadk.comms.CommunicationsThread;
import es.upc.lewis.quadadk.comms.GroundStationClient;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;

public class MissionThread extends Thread {
	private MissionUtils utils;
	
	public MissionThread(CommunicationsThread comms, MainActivity activity) {
		if (comms == null) { return; }

		// Utils class
		utils = new MissionUtils(comms, activity);
		
		// Register BroadcastReceiver
		LocalBroadcastManager.getInstance(activity).registerReceiver(
				broadcastReceiver, broadcastIntentFilter());

		start();
	}

	@Override
	public void run() {
		try {
			// Arm motors
			utils.showToast("Arming...");
			utils.arm();

			// Set loitter flight mode
			utils.send(ArduinoCommands.SET_MODE_LOITTER);

			// Go up for 2 seconds
			utils.send(ArduinoCommands.SET_CH3, 1750);
			utils.wait(2000);

			// Hover for 5 seconds
			utils.hover();
			utils.wait(5000);

			// Disarm motors
			// NOTE: this is just a test. Don't disarm while flying!!
			utils.showToast("Disarming...");
			utils.disarm();
		} catch (AbortException e) {
			// Mission has been aborted
			return;
		}
	}

	/**
	 * Receive important events
	 */
	private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();

			if (action.equals(GroundStationClient.ABORT_MISSION)) {
				utils.abortMission();
				utils.showToast("Mission aborted!");
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
