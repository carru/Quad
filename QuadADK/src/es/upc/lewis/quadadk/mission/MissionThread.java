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

/**
 * 
 * Implement your mission in this class (inside the run() method).
 * 
 * 
 * The MainActivity will start this thread when the server signals to start.
 * If the server aborts the mission you'll receive an ABORT_MISSION broadcast (see broadcastReceiver).
 * This BroadcastReceiver also receives data from the Arduino sensors; all it does now
 * is send it to the server.
 * 
 * 
 * The class MissionUtils has this methods you should use:
 * 
 * takeoff			Start the quadcopter and go to the starting position
 * 
 * returnToLaunch	Return to the starting position and land
 * 
 * abortMission		Interrupt and stop your mission and issue a returnToLaunch
 * 					Do not use it to end your mission when all the work is done
 * 					(see endMission for this purpose)
 * 
 * endMission		Prepare to end this thread
 * 					You should finish your mission with returnToLaunch and endMission
 * 
 * wait				Sleep for a given number of milliseconds
 * 
 * readSensor		Example: utils.readSensor(MissionUtils.TEMPERATURE);
 * 					Tell the Arduino to measure temperature and receive the result in the broadcastReceiver
 * 
 * takePicture		Take a picture with the back camera and send it to the server
 * 					String parameter is the name
 * 
 * 
 * Movement methods:
 * 
 * hover			Stay in place, don't move in any direction
 * 
 * To move your quadcopter horizontally you have to simulate movement of the right stick
 * The neutral (middle) position of the stick is 1500 and the range is [1000, 2000]
 * Channel 1: right (high) and left (low)
 * Channel 2: forward (low) and backward (high)
 * 
 * Examples
 * Forward		utils.send(ArduinoCommands.SET_CH2, MissionUtils.CH_NEUTRAL - 200);
 * 				Moves forward
 * 
 * Right		utils.send(ArduinoCommands.SET_CH1, MissionUtils.CH_NEUTRAL + 300);
 * 				Moves right faster
 * 
 * Note how the channel value (second parameter) is an offset (neutral position) with
 * the movement added. With +/- 300 we get 1200 and 1800
 * We recommend values from +/- 200 to +/- 300
 * 
 * You can combine them to move diagonally:
 * utils.send(ArduinoCommands.SET_CH2, MissionUtils.CH_NEUTRAL + 200);
 * utils.send(ArduinoCommands.SET_CH1, MissionUtils.CH_NEUTRAL + 200);
 * This will make the quadcopter move backward and right at the same time
 *
 */
public class MissionThread extends Thread {
	
	/*
	 *  IMPORTANT!
	 *  Set your ID
	 */
	public static final String QUAD_ID = "001";
	
	
	private int NAVIGATION_LOOP_PERIOD = 250; // Milliseconds
	
	private double WAYPOINT_LATITUDE_ERROR  = 0.00005;
	//private double WAYPOINT_LATITUDE_ERROR  = 0.00004;
	//private double WAYPOINT_LATITUDE_ERROR  = 0.00003;
	//private double WAYPOINT_LATITUDE_ERROR  = 0.00002;
	//private double WAYPOINT_LATITUDE_ERROR  = 0.00001;
	
	private double WAYPOINT_LONGITUDE_ERROR = 0.00007;
	//private double WAYPOINT_LONGITUDE_ERROR = 0.00006;
	//private double WAYPOINT_LONGITUDE_ERROR = 0.00005;
	//private double WAYPOINT_LONGITUDE_ERROR = 0.00004;
	//private double WAYPOINT_LONGITUDE_ERROR = 0.00003;
	//private double WAYPOINT_LONGITUDE_ERROR = 0.00002;
	//private double WAYPOINT_LONGITUDE_ERROR = 0.00001;
	
	private Location currentLocation, startLocation, targetLocation;
	private int cyclesInThisWaypoint = 0;
	private boolean isLastWaypoint = false;
	private double latitudeDelta, longitudeDelta;
	boolean latitudeMovement;
	boolean longitudeMovement;
	private MainActivity activity;
	
	// Slider position from neutral (MissionUtils.CH_NEUTRAL)
	private final int HORIZONTAL_MOVEMENT_SLIDER = 200; // TODO: increase? no more than 300
	
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
		
		//MainActivity.gpsLogger.createLogFile();
		
		start();
	}

	private void loadWaypoints() {
		waypoints = new ArrayList<Waypoint>();
		
		/*
		 * Starting location is 41.38812815, 2.1133061 
		 */
		
		// Square
//		waypoints.add(new Waypoint(41.38825229, 2.11327331));
//		waypoints.add(new Waypoint(41.38809966, 2.11348435));
//		waypoints.add(new Waypoint(41.38793185, 2.11331496));
//		waypoints.add(new Waypoint(41.38796800, 2.11310139));
		
		
		// Elisenda
//		waypoints.add(new Waypoint(41.38821131, 2.11331871));
//		waypoints.add(new Waypoint(41.38818302, 2.11342866));
//		waypoints.add(new Waypoint(41.38804657, 2.11329966));
		
		
		waypoints.add(new Waypoint(41.38807804, 2.11348146));
		waypoints.add(new Waypoint(41.38799437, 2.11317220));
		// Repeat this two waypoints
		waypoints.add(new Waypoint(41.38807804, 2.11348146));
		waypoints.add(new Waypoint(41.38799437, 2.11317220));
		waypoints.add(new Waypoint(41.38807804, 2.11348146));
		waypoints.add(new Waypoint(41.38799437, 2.11317220));
		waypoints.add(new Waypoint(41.38807804, 2.11348146));
		waypoints.add(new Waypoint(41.38799437, 2.11317220));
		waypoints.add(new Waypoint(41.38807804, 2.11348146));
		waypoints.add(new Waypoint(41.38799437, 2.11317220));
	}
	
	/**
	 * Sets sticks to move to the waypoint (independent of previous movement direction) and throttle to neutral
	 * @throws AbortException
	 */
	private void performMovement() throws AbortException {
		latitudeMovement = Math.abs(latitudeDelta) > WAYPOINT_LATITUDE_ERROR;
		longitudeMovement = Math.abs(longitudeDelta) > WAYPOINT_LONGITUDE_ERROR;
		
		utils.send(ArduinoCommands.SET_CH3, MissionUtils.THROTTLE_NEUTRAL);
		
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
		if (waypoints.isEmpty()) { return null; }
		
		Waypoint waypoint = waypoints.remove(0);
		Location location = new Location("");
		location.setLatitude(waypoint.latitude);
		location.setLongitude(waypoint.longitude);
		currentWaypoint++;
		cyclesInThisWaypoint = 0;
		
		// Is this the last waypoint?
		if (waypoints.isEmpty()) { isLastWaypoint = true; }
		
		return location;
	}
	
	@Override
	public void run() {
		// TESTS
//		try {
//			
////			for(currentWaypoint=1; currentWaypoint<5; currentWaypoint++) {
////				utils.takePicture("local_" + Integer.toString(currentWaypoint));
////				utils.wait(5000);
////			}
//			
////			utils.readSensor(MissionUtils.TEMPERATURE);
////			utils.readSensor(MissionUtils.HUMIDITY);
////			utils.readSensor(MissionUtils.NO2);
////			utils.readSensor(MissionUtils.CO);
////			utils.wait(5000);
//			
////			utils.wait(100000);
//			
//			while(true) {
//				currentLocation = locationProvider.getLastLocation();
//				MainActivity.gpsLogger.write(currentLocation);
//				utils.wait(250);
//			}
//			
//		} catch (AbortException e) { }
//		endMission();
		
		
		
		
		
		// Empty mission
//		try {
//			
//			// Get starting location
//			startLocation = locationProvider.getLastLocation();
//			while (startLocation == null) {
//				// GPS not ready, wait and try again
//				utils.wait(1000);
//							
//				startLocation = locationProvider.getLastLocation();
//			}
//			// At this point we have GPS signal
//			
//
//			// Go up, to the starting position
//			utils.takeoff();
//
//			
//			// Hover for 20 seconds. Note that the time is in milliseconds
//			utils.wait(20*1000);
//
//			
//			// Go back to the starting position and land
//			utils.returnToLaunch();
//
//		} catch (AbortException e) {
//			// Mission has been aborted
//			
//			// If you have not changed it, the broadcastReceiver will issue a Return To Launch
//			// command so you don't need to do anything here.
//			// The try/catch makes sure your algorithm is stopped when we abort the mission
//		}
//
//		// Call this at the end of your mission
//		endMission();
		
		
		
		
		
		try {
			// Get starting location
			startLocation = locationProvider.getLastLocation();
			while (startLocation == null) {
				// GPS not ready, wait and try again
				utils.wait(1000);
							
				startLocation = locationProvider.getLastLocation();
			}

			// Set first target
			currentWaypoint = 0;
			targetLocation = getNextWaypoint();
			if (targetLocation == null) {
				// First waypoint must never be null
				endMission();
				return;
			}

			
			utils.takeoff();
			// utils.send(ArduinoCommands.SET_MODE_LOITTER);

			
			// Navigation loop
			boolean navigating = true;
			while (navigating) {
				// Get current location
				currentLocation = locationProvider.getLastLocation();

				// Calculate distance, in degrees, to the target
				latitudeDelta  = currentLocation.getLatitude()  - targetLocation.getLatitude();
				longitudeDelta = currentLocation.getLongitude() - targetLocation.getLongitude();

				// Have we reached the target?
				if (!waypointReached()) {
					// Not yet reached
					performMovement();
				} else {
					// We reached the target
					utils.hover();

					cyclesInThisWaypoint++;

					// Make measurements
					switch (cyclesInThisWaypoint) {
					case 1:
						utils.readSensor(MissionUtils.ALTITUDE);
						break;
					case 2:
						utils.readSensor(MissionUtils.TEMPERATURE);
						break;
					case 3:
						utils.readSensor(MissionUtils.HUMIDITY);
						break;
					case 4:
						utils.readSensor(MissionUtils.NO2);
						break;
					case 5:
						utils.readSensor(MissionUtils.CO);
						break;
					case 6:
						utils.takePicture("local_" + Integer.toString(currentWaypoint));
						break;
					case 7:
						targetLocation = getNextWaypoint();
						if (targetLocation == null) {
							// All waypoints have been reached, mission has to end
							navigating = false;
						}
						break;
					}
				}

				utils.wait(NAVIGATION_LOOP_PERIOD);
			}

			utils.returnToLaunch();

		} catch (AbortException e) {
			// Mission has been aborted
		}

		endMission();
	}
	
	/**
	 * End mission. Must be called at the end of your mission
	 */
	private void endMission() {
		// Unregister receiver
		LocalBroadcastManager.getInstance(activity).unregisterReceiver(broadcastReceiver);
		
		// Notify mission is over
		MainActivity.isMissionRunning = false;
		
		//MainActivity.gpsLogger.close();
	}
	
//	private boolean isSameLocation(Location location1, Location location2) {
//		if (location1.getLatitude()  != location2.getLatitude())  { return false; }
//		if (location1.getLongitude() != location2.getLongitude()) { return false; }
//		if (location1.getAltitude()  != location2.getAltitude())  { return false; }
//		if (location1.getAccuracy()  != location2.getAccuracy())  { return false; }
//		
//		return true;
//	}
	
	/**
	 * Receive important events
	 */
	private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			
			// Get the value (4 bytes) as an int (sensor readings)
			int intBytes = intent.getIntExtra(CommunicationsThread.VALUE, 0);
			// bytes to float
			float value = Float.intBitsToFloat(intBytes);
			String valueString = Float.toString(value);
			
			// From GroundStation
			if (action.equals(MissionStatusPolling.ABORT_MISSION)) {
				utils.abortMission();
				utils.showToast("Mission aborted!");
			
			// From Arduino
			// All it does now is send data to the server
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
