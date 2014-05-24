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
			utils.takeoff();
			
			utils.wait(5000);
			
			utils.returnToLaunch();
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
			else if (action.equals(GroundStationClient.ACK)) {
				MissionUtils.readyToSend = true;
			}
		}
	};

	/**
	 * Intents to listen to
	 */
	private static IntentFilter broadcastIntentFilter() {
		final IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(GroundStationClient.ABORT_MISSION);
		intentFilter.addAction(GroundStationClient.ACK);
		return intentFilter;
	}
}
