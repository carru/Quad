package es.upc.lewis.quadadk.mission;

import es.upc.lewis.quadadk.MainActivity;
import es.upc.lewis.quadadk.comms.ArduinoCommands;
import es.upc.lewis.quadadk.comms.CommunicationsThread;
import es.upc.lewis.quadadk.comms.GroundStationClient;
import es.upc.lewis.quadadk.comms.GroundStationCommands;
import es.upc.lewis.quadadk.tools.MyLocation;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.support.v4.content.LocalBroadcastManager;

public class MissionThread extends Thread {
	private double WAYPOINT_RADIUS = 0.00007; // 9.7 meters
	private double latitudeDelta, longitudeDelta;
	
	private int NAVIGATION_LOOP_PERIOD = 250; // Milliseconds
	
	// Movements. Can be combined, i.e.: RIGHT + FORWARD
	private final int LEFT     = 1;
	private final int RIGHT    = 2;
	private final int FORWARD  = 4;
	private final int BACKWARD = 8;
	
	private MissionUtils utils;
	private MyLocation locationProvider;
	
	public MissionThread(CommunicationsThread comms, GroundStationClient server, MainActivity activity, MyLocation locationProvider) {
		if (comms == null) { return; }
		
		this.locationProvider = locationProvider;
		
		// Utils class
		utils = new MissionUtils(comms, server, activity);
		
		// Register BroadcastReceiver
		LocalBroadcastManager.getInstance(activity).registerReceiver(
				broadcastReceiver, broadcastIntentFilter());

		// Notify GroundStation mission has started
		server.send(GroundStationCommands.MISSION_START);
		
		start();
	}

	private int decideMovement() {
		int movement = 0;
		
		if (Math.abs(latitudeDelta) > WAYPOINT_RADIUS) {
			if (latitudeDelta < 0) { movement = movement + FORWARD; }
			else if (latitudeDelta > 0) { movement = movement + BACKWARD; }
		}
		
		if (Math.abs(longitudeDelta) > WAYPOINT_RADIUS) {
			if (longitudeDelta < 0) { movement = movement + RIGHT; }
			else if (longitudeDelta > 0) { movement = movement + LEFT; }
		}
		
		return movement;
	}
	
	private boolean waypointReached() {
		if (Math.abs(latitudeDelta) > WAYPOINT_RADIUS) { return false; }
		if (Math.abs(longitudeDelta) > WAYPOINT_RADIUS) { return false; }
		return true;
	}
	
	@Override
	public void run() {
		Location currentLocation, startWP, targetWP;
		
		// Enter mission waypoints here (latitude, longitude)
		// Not used yet
		@SuppressWarnings("unused")
		double[] waypoints = new double[]{
				41.38802, 2.11325,
				41.00000, 2.00000,
				41.00000, 2.00000
		};
		
		
		try {
			// Get starting location
			startWP = locationProvider.getLastLocation();
			while (startWP == null) {
				// GPS not ready, wait and try again
				try { sleep(1000); } catch (InterruptedException e) { }
							
				startWP = locationProvider.getLastLocation();
			}

			// Set target, about 27 meters to the east
			targetWP = new Location(startWP);
			targetWP.setLongitude(startWP.getLongitude() + 0.0002);
			
			
			utils.takeoff();
			
			
			
			// Navigation loop
			while (true) {
				// Get current location
				currentLocation = locationProvider.getLastLocation();

				// Calculate distance, in degrees, to the target
				latitudeDelta  = currentLocation.getLatitude()  - targetWP.getLatitude();
				longitudeDelta = currentLocation.getLongitude() - targetWP.getLongitude();

				// Have we reached the target?
				if (!waypointReached()) {
					// Not yet reached. Decide movement
					utils.hover();
					switch (decideMovement()) {
					case LEFT:
						utils.send(ArduinoCommands.SET_CH1, 1200);
						break;
					case RIGHT:
						utils.send(ArduinoCommands.SET_CH1, 1800);
						break;
					case FORWARD:
						utils.send(ArduinoCommands.SET_CH2, 1800);
						break;
					case BACKWARD:
						utils.send(ArduinoCommands.SET_CH2, 1200);
						break;
					case LEFT + FORWARD:
						utils.send(ArduinoCommands.SET_CH1, 1200);
						utils.send(ArduinoCommands.SET_CH2, 1800);
						break;
					case LEFT + BACKWARD:
						utils.send(ArduinoCommands.SET_CH1, 1200);
						utils.send(ArduinoCommands.SET_CH2, 1200);
						break;
					case RIGHT + FORWARD:
						utils.send(ArduinoCommands.SET_CH1, 1800);
						utils.send(ArduinoCommands.SET_CH2, 1800);
						break;
					case RIGHT + BACKWARD:
						utils.send(ArduinoCommands.SET_CH1, 1800);
						utils.send(ArduinoCommands.SET_CH2, 1200);
						break;
					}
				}
				else {
					// We reached the target
					utils.hover();
				}

				// utils.wait(500);
				try {
					sleep(NAVIGATION_LOOP_PERIOD);
				} catch (InterruptedException e) {
				}
			}
			
			
			//utils.returnToLaunch();
			
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
