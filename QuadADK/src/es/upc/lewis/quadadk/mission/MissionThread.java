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
 * Useful methods:
 * 
 * locationProvider.getLastLocation()		Returns your GPS position (updated roughly once per second)
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
 * 
 * Channel 3 is the throttle; leave it at neutral (MissionUtils.THROTTLE_NEUTRAL) to maintain
 * altitude and use 'utils.goUp()' to increase it
 * WARNING!: Never try to descend. Also, throttle channel has a different range than the others
 * but you don't need to directly modify it.
 * 
 * Use only these two methods to change throttle:
 * utils.send(ArduinoCommands.SET_CH3, MissionUtils.THROTTLE_NEUTRAL)
 * utils.goUp()
 * 
 * 
 * Channel 4 controls yaw (rotation)
 * The quadcopter will maintain the orientation it started with and we always start with it pointing north
 * Because of this, you can assume the quadcopter is always pointing north and thus don't need to 
 * manually change yaw
 * 
 * When pointing north, moving to the right increases longitude and moving forward increases latitude
 * 
 * 
 * Channel 5 is the flight mode
 * You don't have to worry about it. Your flight mode during the mission will be Loitter
 * Don't change it
 * 
 * 
 * Channel 7 controls who has control over the quadcopter, Android or the RC
 * Don't change it
 * 
 *  
 * Channels 6 and 8 are unused
 * Don't change them
 *
 */
public class MissionThread extends Thread {
	
	/*
	 * TODO:
	 *  IMPORTANT!
	 *  Set your ID
	 */
	public static final String QUAD_ID = "";
	
	// Sleep time, in milliseconds, at the end of the navigation loop
	private int NAVIGATION_LOOP_PERIOD = 250;
	
	// Maximum error to consider we reached a waypoint. You may want to tweak it
	private double WAYPOINT_LATITUDE_ERROR  = 0.00005;
	private double WAYPOINT_LONGITUDE_ERROR = 0.00007;
	
	private Location currentLocation, startLocation, targetLocation;
	private int cyclesInThisWaypoint = 0;
	private boolean isLastWaypoint = false;
	private double latitudeDelta, longitudeDelta;
	boolean latitudeMovement;
	boolean longitudeMovement;
	
	// Slider movement from neutral (MissionUtils.CH_NEUTRAL)
	// Set to a higher value to move faster. Recommended values: [200, 300]
	private final int HORIZONTAL_MOVEMENT_SLIDER = 200;
	
	// Time to go up (gain altitude), in milliseconds
	// This is used in the last waypoint. You may want to use the altitude (barometer)
	// sensor instead of going up for given amount of time
	private final int TIME_TO_GO_UP = 3000;
	
	// Store mission waypoints here. See loadWapoints()
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
	private MainActivity activity;
	
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

	/**
	 * Add your waypoints here
	 */
	private void loadWaypoints() {
		waypoints = new ArrayList<Waypoint>();
		// Starting location is 41.38812815, 2.1133061
		
		// Example:
		// waypoints.add(new Waypoint(41.38825229, 2.11327331));
		
		
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
	}
	
	/**
	 * Sets sticks to move to the waypoint (independent of previous movement direction) and throttle to neutral
	 * @throws AbortException
	 */
	private void performMovement() throws AbortException {
		// This method sets channels 1 and 2 to move to the current waypoint target
		// It always moves at the same speed (HORIZONTAL_MOVEMENT_SLIDER)
		// Feel free to modify it
		
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
		
		// Get next waypoint from the list
		Waypoint waypoint = waypoints.remove(0);
		
		// Create a Location object with this waypoint
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
		try {
			// Get starting location
			startLocation = locationProvider.getLastLocation();
			while (startLocation == null) {
				// GPS not ready, wait and try again
				utils.wait(1000);
							
				startLocation = locationProvider.getLastLocation();
			}
			// At this point we have GPS signal

			
			// Set first target
			currentWaypoint = 0;
			targetLocation = getNextWaypoint();
			if (targetLocation == null) {
				// There are no waypoints defined! End mission without taking off
				endMission();
				return;
			}

			
			// Go up, to the starting position
			utils.takeoff();

			
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

					if (isLastWaypoint) {
						// Take a picture of the last waypoint before going up
						utils.takePicture("local_" + Integer.toString(currentWaypoint));
						utils.wait(NAVIGATION_LOOP_PERIOD);
						
						// Go up for a given ammount of time
						// You can use the altitude sensor (barometer) instead
						utils.goUp();
						utils.wait(TIME_TO_GO_UP);
						
						// Stop going up
						utils.hover();
						
						// Take another picture at the new height
						utils.takePicture("global");
						
						// Stop the loop, we are done
						navigating = false;
					}
					else {
						cyclesInThisWaypoint++;

						// This makes all the measurements on each waypoint (one per loop cycle)
						// Feel free to change what to do when we are in the target waypoint
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
								// Update: if we have a special case for the last waypoint
								// ('if (isLastWaypoint)') we'll never get here
								
								// All waypoints have been reached, mission has
								// to end
								navigating = false;
							}
							break;
						}
					}
				}

				utils.wait(NAVIGATION_LOOP_PERIOD);
			}

			// Go back to the starting position and land
			utils.returnToLaunch();

		} catch (AbortException e) {
			// Mission has been aborted
			
			// If you have not changed it, the broadcastReceiver will issue a Return To Launch
			// command so you don't need to do anything here.
			// The try/catch makes sure your algorithm is stopped when we abort the mission
		}

		// Call this at the end of your mission
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
				//utils.showToast("Mission aborted!");
			
			// From Arduino
			// All it does now is send data to the server. You'll probably want to change this
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
