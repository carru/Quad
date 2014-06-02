package es.upc.lewis.quadadk.mission;

import java.util.ArrayList;

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
	private int NAVIGATION_LOOP_PERIOD = 250; // Milliseconds
	
	private double WAYPOINT_LATITUDE_ERROR  = 0.00005; // 9.3 meters
	private double WAYPOINT_LONGITUDE_ERROR = 0.00007; // 9.7 meters
	
	private double latitudeDelta, longitudeDelta;
	
	// Movements. Can be combined, i.e.: RIGHT + FORWARD
	private final int LEFT     = 1;
	private final int RIGHT    = 2;
	private final int FORWARD  = 4;
	private final int BACKWARD = 8;
	
	// Store mission waypoints here
	ArrayList<Waypoint> waypoints;
	private class Waypoint {
		public double latitude;
		public double longitude;
		public Waypoint(double latitude, double longitude) {
			this.latitude = latitude;
			this.longitude = longitude;
		}
	}
	
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
		
		loadWaypoints();
		
		start();
	}

	private void loadWaypoints() {
		waypoints = new ArrayList<Waypoint>();
		
		// Square
		waypoints.add(new Waypoint(41.388193, 2.113258));
		waypoints.add(new Waypoint(41.388062, 2.113528));
		waypoints.add(new Waypoint(41.387869, 2.113328));
		waypoints.add(new Waypoint(41.387967, 2.113098));
	}
	
	private int decideMovement() {
		int movement = 0;
		
		if (Math.abs(latitudeDelta) > WAYPOINT_LATITUDE_ERROR) {
			if (latitudeDelta < 0) { movement = movement + FORWARD; }
			else if (latitudeDelta > 0) { movement = movement + BACKWARD; }
		}
		
		if (Math.abs(longitudeDelta) > WAYPOINT_LONGITUDE_ERROR) {
			if (longitudeDelta < 0) { movement = movement + RIGHT; }
			else if (longitudeDelta > 0) { movement = movement + LEFT; }
		}
		
		return movement;
	}
	
	private boolean waypointReached() {
		if (Math.abs(latitudeDelta) > WAYPOINT_LATITUDE_ERROR) { return false; }
		if (Math.abs(longitudeDelta) > WAYPOINT_LONGITUDE_ERROR) { return false; }
		return true;
	}
	
	private Location getNextWaypoint() {
		if (waypoints.size() == 0) { return null; }
		
		Waypoint waypoint = waypoints.remove(0);
		Location location = new Location("");
		location.setLatitude(waypoint.latitude);
		location.setLongitude(waypoint.longitude);
		return location;
	}
	
	@Override
	public void run() {
		Location currentLocation, startLocation, targetLocation;
		boolean waypointPhotographed = false;
		
		
		try {
			// Get starting location
			startLocation = locationProvider.getLastLocation();
			while (startLocation == null) {
				// GPS not ready, wait and try again
				utils.wait(1000);
							
				startLocation = locationProvider.getLastLocation();
			}

			// Set first target
			targetLocation = getNextWaypoint();
			if (targetLocation == null) {
				// First waypoint must never be null
				utils.endMission();
				return;
			}
			
			
			utils.takeoff();
			
			
			// Navigation loop
			while (true) {
				// Get current location
				currentLocation = locationProvider.getLastLocation();

				// Calculate distance, in degrees, to the target
				latitudeDelta  = currentLocation.getLatitude()  - targetLocation.getLatitude();
				longitudeDelta = currentLocation.getLongitude() - targetLocation.getLongitude();

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
						utils.send(ArduinoCommands.SET_CH2, 1200);
						break;
					case BACKWARD:
						utils.send(ArduinoCommands.SET_CH2, 1800);
						break;
					case LEFT + FORWARD:
						utils.send(ArduinoCommands.SET_CH1, 1200);
						utils.send(ArduinoCommands.SET_CH2, 1200);
						break;
					case LEFT + BACKWARD:
						utils.send(ArduinoCommands.SET_CH1, 1200);
						utils.send(ArduinoCommands.SET_CH2, 1800);
						break;
					case RIGHT + FORWARD:
						utils.send(ArduinoCommands.SET_CH1, 1800);
						utils.send(ArduinoCommands.SET_CH2, 1200);
						break;
					case RIGHT + BACKWARD:
						utils.send(ArduinoCommands.SET_CH1, 1800);
						utils.send(ArduinoCommands.SET_CH2, 1800);
						break;
					}
				}
				else {
					// We reached the target
					utils.hover();
					
					// Take a picture and wait 1 cycle
					if (waypointPhotographed) {
						targetLocation = getNextWaypoint();
						if (targetLocation == null) {
							// All waypoints have been reached, mission has to end
							break;
						}
						
						waypointPhotographed = false;
					}
					else {
						utils.takePicture();
						waypointPhotographed = true;
					}
				}

				utils.wait(NAVIGATION_LOOP_PERIOD);
			}
			
			
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
