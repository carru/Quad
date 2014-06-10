package es.upc.lewis.quadadk.mission;

import java.util.ArrayList;

import es.upc.lewis.quadadk.MainActivity;
import es.upc.lewis.quadadk.comms.ArduinoCommands;
import es.upc.lewis.quadadk.comms.CommunicationsThread;
import es.upc.lewis.quadadk.comms.MissionStatusPolling;
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
	private MainActivity activity;
	
	// Slider position from neutral (MissionUtils.CH_NEUTRAL)
	private final int HORIZONTAL_MOVEMENT_SLIDER = 300;
	
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
	
	public MissionThread(CommunicationsThread comms, MainActivity activity, MyLocation locationProvider) {
		if (comms == null) { return; }
		
		this.locationProvider = locationProvider;
		this.activity = activity;
		
		// Utils class
		utils = new MissionUtils(comms, activity, this);
		
		// Register BroadcastReceiver
		LocalBroadcastManager.getInstance(activity).registerReceiver(
				broadcastReceiver, broadcastIntentFilter());

		// Notify GroundStation mission has started
		//server.send(GroundStationCommands.MISSION_START);
		
		loadWaypoints();
		
		start();
	}

	private void loadWaypoints() {
		waypoints = new ArrayList<Waypoint>();
		
		/*
		 * Starting location is 41.38798718, 2.1132787
		 */
		
		// Square
		waypoints.add(new Waypoint(41.38825229, 2.11327331));
		waypoints.add(new Waypoint(41.38809966, 2.11348435));
		waypoints.add(new Waypoint(41.38793185, 2.11331496));
		waypoints.add(new Waypoint(41.38796800, 2.11310139));
	}
	
	private void performMovement() throws AbortException {
		if (Math.abs(latitudeDelta) > WAYPOINT_LATITUDE_ERROR) {
			if (latitudeDelta < 0) {
				utils.send(ArduinoCommands.SET_CH2, MissionUtils.CH_NEUTRAL - HORIZONTAL_MOVEMENT_SLIDER);
			}
			else if (latitudeDelta > 0) {
				utils.send(ArduinoCommands.SET_CH2, MissionUtils.CH_NEUTRAL + HORIZONTAL_MOVEMENT_SLIDER);
			}
		}
		
		if (Math.abs(longitudeDelta) > WAYPOINT_LONGITUDE_ERROR) {
			if (longitudeDelta < 0) {
				utils.send(ArduinoCommands.SET_CH1, MissionUtils.CH_NEUTRAL + HORIZONTAL_MOVEMENT_SLIDER);
			}
			else if (longitudeDelta > 0) {
				utils.send(ArduinoCommands.SET_CH1, MissionUtils.CH_NEUTRAL - HORIZONTAL_MOVEMENT_SLIDER);
			}
		}
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
		// Dumb mission
		try {
			utils.arm();
			utils.send(ArduinoCommands.SET_MODE_STB);
			utils.send(ArduinoCommands.SET_CH3, 1250);
			utils.wait(200000);
		} catch (AbortException e) {  }
		
		utils.disarm_NO_EXCEPTION_DEBUG_ONLY();
		endMission();
		
		// Remember to uncomment abortmission() !!!!
		
		
//		Location currentLocation, startLocation, targetLocation;
//		boolean waypointPhotographed = false;
//		
//		
//		try {
//			// Get starting location
//			startLocation = locationProvider.getLastLocation();
//			while (startLocation == null) {
//				// GPS not ready, wait and try again
//				utils.wait(1000);
//							
//				startLocation = locationProvider.getLastLocation();
//			}
//
//			// Set first target
//			targetLocation = getNextWaypoint();
//			if (targetLocation == null) {
//				// First waypoint must never be null
//				utils.endMission();
//				return;
//			}
//			
//			
//			utils.takeoff();
//			
//			
//			// Navigation loop
//			while (true) {
//				// Get current location
//				currentLocation = locationProvider.getLastLocation();
//
//				// Calculate distance, in degrees, to the target
//				latitudeDelta  = currentLocation.getLatitude()  - targetLocation.getLatitude();
//				longitudeDelta = currentLocation.getLongitude() - targetLocation.getLongitude();
//
//				// Have we reached the target?
//				if (!waypointReached()) {
//					// Not yet reached
//					utils.hover();
//					performMovement();
//				}
//				else {
//					// We reached the target
//					utils.hover();
//					
//					// Take a picture and wait 1 cycle
//					if (waypointPhotographed) {
//						targetLocation = getNextWaypoint();
//						if (targetLocation == null) {
//							// All waypoints have been reached, mission has to end
//							break;
//						}
//						
//						waypointPhotographed = false;
//					}
//					else {
//						utils.takePicture();
//						waypointPhotographed = true;
//					}
//				}
//
//				utils.wait(NAVIGATION_LOOP_PERIOD);
//			}
//			
//			
//			utils.returnToLaunch();
//			
//			
//		} catch (AbortException e) {
//			// Mission has been aborted
//		}
//		
//		
//		endMission();
	}
	
	private void endMission() {
		// Unregister receiver
		LocalBroadcastManager.getInstance(activity).unregisterReceiver(broadcastReceiver);
		
		// Notify mission is over
		MainActivity.isMissionRunning = false;
	}
	
	/**
	 * Receive important events
	 */
	private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			
			// Get the value (4 bytes) as an int (seansor readings)
			int intBytes = intent.getIntExtra(CommunicationsThread.VALUE, 0);
			// bytes to float
			float value = Float.intBitsToFloat(intBytes);

			// From GroundStation
			if (action.equals(MissionStatusPolling.ABORT_MISSION)) {
				utils.abortMission();
				utils.showToast("Mission aborted!");
			}
			
			// From Arduino
			else if (action.equals(CommunicationsThread.ACTION_DATA_AVAILABLE_SENSOR_TEMPERATURE)) {
				// Reading stored in 'value' (float)
			} else if (action.equals(CommunicationsThread.ACTION_DATA_AVAILABLE_SENSOR_HUMIDITY)) {
				// Reading stored in 'value' (float)
			} else if (action.equals(CommunicationsThread.ACTION_DATA_AVAILABLE_SENSOR_NO2)) {
				// Reading stored in 'value' (float)
			} else if (action.equals(CommunicationsThread.ACTION_DATA_AVAILABLE_SENSOR_CO)) {
				// Reading stored in 'value' (float)
			}
		}
	};

	/**
	 * Intents to listen to
	 */
	private static IntentFilter broadcastIntentFilter() {
		final IntentFilter intentFilter = new IntentFilter();
		
		// From GroundStation
		intentFilter.addAction(MissionStatusPolling.ABORT_MISSION);
		
		// From Arduino
		intentFilter.addAction(CommunicationsThread.ACTION_DATA_AVAILABLE_SENSOR_TEMPERATURE);
		intentFilter.addAction(CommunicationsThread.ACTION_DATA_AVAILABLE_SENSOR_HUMIDITY);
		intentFilter.addAction(CommunicationsThread.ACTION_DATA_AVAILABLE_SENSOR_NO2);
		intentFilter.addAction(CommunicationsThread.ACTION_DATA_AVAILABLE_SENSOR_CO);
		
		return intentFilter;
	}
}
