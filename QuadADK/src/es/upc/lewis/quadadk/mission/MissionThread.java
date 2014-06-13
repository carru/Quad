package es.upc.lewis.quadadk.mission;

import java.util.ArrayList;

import es.upc.lewis.quadadk.MainActivity;
import es.upc.lewis.quadadk.comms.ArduinoCommands;
import es.upc.lewis.quadadk.comms.CommunicationsThread;
import es.upc.lewis.quadadk.comms.MissionStatusPolling;
import es.upc.lewis.quadadk.comms.SendDataThread;
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
	//private double WAYPOINT_LATITUDE_ERROR  = 0.00004;
	//private double WAYPOINT_LONGITUDE_ERROR = 0.00006;
	
	private Location currentLocation, startLocation, targetLocation;
	private int cyclesInThisWaypoint = 0;
	private double latitudeDelta, longitudeDelta;
	boolean latitudeMovement;
	boolean longitudeMovement;
	private MainActivity activity;
	
	// Slider position from neutral (MissionUtils.CH_NEUTRAL)
	private final int HORIZONTAL_MOVEMENT_SLIDER = 300;
	
	// Store mission waypoints here
	ArrayList<Waypoint> waypoints;
	private int currentWaypoint;
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
		this.locationProvider = locationProvider;
		this.activity = activity;
		
		// Utils class
		utils = new MissionUtils(comms, activity, this);
		
		// Register BroadcastReceiver
		LocalBroadcastManager.getInstance(activity).registerReceiver(
				broadcastReceiver, broadcastIntentFilter());
		
		loadWaypoints();
		
		MainActivity.isMissionRunning = true;
		
		MainActivity.gpsLogger.createLogFile();
		
		start();
	}

	private void loadWaypoints() {
		waypoints = new ArrayList<Waypoint>();
		
		/*
		 * Starting location is 41.38798718, 2.1132787
		 */
		
		// Square
//		waypoints.add(new Waypoint(41.38825229, 2.11327331));
//		waypoints.add(new Waypoint(41.38809966, 2.11348435));
//		waypoints.add(new Waypoint(41.38793185, 2.11331496));
//		waypoints.add(new Waypoint(41.38796800, 2.11310139));
		
		// Elisenda
		waypoints.add(new Waypoint(41.38821131, 2.11331871));
		waypoints.add(new Waypoint(41.38818302, 2.11342866));
		waypoints.add(new Waypoint(41.38804657, 2.11329966));
	}
	
	/**
	 * Sets sticks to move to the waypoint (independent of previous movement direction)
	 * @throws AbortException
	 */
	private void performMovement() throws AbortException {
		latitudeMovement = Math.abs(latitudeDelta) > WAYPOINT_LATITUDE_ERROR;
		longitudeMovement = Math.abs(longitudeDelta) > WAYPOINT_LONGITUDE_ERROR;
		
		if (latitudeMovement) {
			// Set latitude movement
			if (latitudeDelta < 0) {
				utils.send(ArduinoCommands.SET_CH2, MissionUtils.CH_NEUTRAL - HORIZONTAL_MOVEMENT_SLIDER);
			}
			else if (latitudeDelta > 0) {
				utils.send(ArduinoCommands.SET_CH2, MissionUtils.CH_NEUTRAL + HORIZONTAL_MOVEMENT_SLIDER);
			}
			
			// Change longitude to neutral if needed
			if (!longitudeMovement) {
				utils.send(ArduinoCommands.SET_CH1, MissionUtils.CH_NEUTRAL);
			}
		}
		
		if (longitudeMovement) {
			// Set longitude movement
			if (longitudeDelta < 0) {
				utils.send(ArduinoCommands.SET_CH1, MissionUtils.CH_NEUTRAL + HORIZONTAL_MOVEMENT_SLIDER);
			}
			else if (longitudeDelta > 0) {
				utils.send(ArduinoCommands.SET_CH1, MissionUtils.CH_NEUTRAL - HORIZONTAL_MOVEMENT_SLIDER);
			}
			
			// Change latitude to neutral if needed
			if (!latitudeMovement) {
				utils.send(ArduinoCommands.SET_CH2, MissionUtils.CH_NEUTRAL);
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
		currentWaypoint++;
		return location;
	}
	
	@Override
	public void run() {
		
		try {
			
//			for(currentWaypoint=1; currentWaypoint<5; currentWaypoint++) {
//				utils.takePicture("local_" + Integer.toString(currentWaypoint));
//				utils.wait(5000);
//			}
			
//			utils.readSensor(MissionUtils.TEMPERATURE);
//			utils.readSensor(MissionUtils.HUMIDITY);
//			utils.readSensor(MissionUtils.NO2);
//			utils.readSensor(MissionUtils.CO);
//			utils.wait(5000);
			
//			utils.wait(100000);
			
			while(true) {
				currentLocation = locationProvider.getLastLocation();
				MainActivity.gpsLogger.write(currentLocation);
				utils.wait(250);
			}
			
		} catch (AbortException e) { }
		endMission();
		
		
		
		
		
		
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
//			currentWaypoint = 0;
//			targetLocation = getNextWaypoint();
//			if (targetLocation == null) {
//				// First waypoint must never be null
//				endMission();
//				return;
//			}
//			
//			
//			utils.takeoff();
//			//utils.send(ArduinoCommands.SET_MODE_LOITTER);
//			
//			
//			// Navigation loop
//			boolean navigating = true;
//			while (navigating) {
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
//					performMovement();
//					cyclesInThisWaypoint = 0;
//				}
//				else {
//					// We reached the target
//					utils.hover();
//					
//					cyclesInThisWaypoint++;
//					switch (cyclesInThisWaypoint) {
//					case 1:
//						utils.readSensor(MissionUtils.ALTITUDE);
//						break;
//					case 2:
//						utils.readSensor(MissionUtils.TEMPERATURE);
//						break;
//					case 3:
//						utils.readSensor(MissionUtils.HUMIDITY);
//						break;
//					case 4:
//						utils.readSensor(MissionUtils.NO2);
//						break;
//					case 5:
//						utils.readSensor(MissionUtils.CO);
//						break;
//					case 6:
//						utils.takePicture("local_" + Integer.toString(currentWaypoint));
//						break;
//					case 7:
//						targetLocation = getNextWaypoint();
//						if (targetLocation == null) {
//							// All waypoints have been reached, mission has to end
//							navigating = false;
//						}
//						break;
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
	
	/**
	 * End mission. Must be called at the end of your mission
	 */
	private void endMission() {
		// Unregister receiver
		LocalBroadcastManager.getInstance(activity).unregisterReceiver(broadcastReceiver);
		
		// Notify mission is over
		MainActivity.isMissionRunning = false;
		
		MainActivity.gpsLogger.close();
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
			String valueString = Float.toString(value);
			
			// From GroundStation
			if (action.equals(MissionStatusPolling.ABORT_MISSION)) {
				//utils.abortMission(); //TODO: UNCOMMENT!!!!!!!!!!!!!!!!
				utils.showToast("Mission aborted!");
			
			// From Arduino
		    } else if (action.equals(CommunicationsThread.ACTION_DATA_AVAILABLE_SENSOR_TEMPERATURE)) {
		    	new SendDataThread("temp1", valueString);
			} else if (action.equals(CommunicationsThread.ACTION_DATA_AVAILABLE_SENSOR_HUMIDITY)) {
				new SendDataThread("hum1", valueString);
			} else if (action.equals(CommunicationsThread.ACTION_DATA_AVAILABLE_SENSOR_NO2)) {
				new SendDataThread("no2", valueString);
			} else if (action.equals(CommunicationsThread.ACTION_DATA_AVAILABLE_SENSOR_CO)) {
				new SendDataThread("co", valueString);
			} else if (action.equals(CommunicationsThread.ACTION_DATA_AVAILABLE_SENSOR_ALTITUDE)) {
				new SendDataThread("alt_bar", valueString);
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
		intentFilter.addAction(CommunicationsThread.ACTION_DATA_AVAILABLE_SENSOR_ALTITUDE);
		
		return intentFilter;
	}
}
