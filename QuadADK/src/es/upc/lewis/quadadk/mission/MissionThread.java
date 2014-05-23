package es.upc.lewis.quadadk.mission;

import es.upc.lewis.quadadk.MainActivity;
import es.upc.lewis.quadadk.comms.ArduinoCommands;
import es.upc.lewis.quadadk.comms.CommunicationsThread;
import es.upc.lewis.quadadk.comms.GroundStationClient;
import es.upc.lewis.quadadk.comms.GroundStationCommands;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;

public class MissionThread extends Thread {
	private MissionUtils utils;
	
	public MissionThread(CommunicationsThread comms, GroundStationClient server, MainActivity activity) {
		if (comms == null) { return; }
		
		// Utils class
		utils = new MissionUtils(comms, server, activity);
		
		// Register BroadcastReceiver
		LocalBroadcastManager.getInstance(activity).registerReceiver(
				broadcastReceiver, broadcastIntentFilter());

		// Notify GroundStation mission has started
		server.send(GroundStationCommands.MISSION_START);
		
		start();
	}

	@Override
	public void run() {
		try {
			utils.wait(2000);
			
			
			//utils.takeoff();
			//utils.returnToLaunch();
			
			
			/*
			// Arm motors
			utils.showToast("Arming...");
			utils.arm();

			// Set loitter flight mode
			utils.send(ArduinoCommands.SET_MODE_LOITTER);

			// Go up for 2 seconds
			utils.send(ArduinoCommands.SET_CH3, 1750);
			utils.wait(2000);

			utils.takePicture();
			
			// Hover for 5 seconds
			utils.hover();
			utils.wait(5000);

			// Disarm motors
			// NOTE: this is just a test. Don't disarm while flying!!
			utils.showToast("Disarming...");
			utils.disarm();*/
		} catch (AbortException e) {
			// Mission has been aborted
		}
		
		utils.endMission();
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
