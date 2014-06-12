package es.upc.lewis.navigationtest;

import java.util.ArrayList;

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
	
	private volatile boolean enabled = true;
	
	private MyLocation locationProvider;
	private MainActivity activity;
	
	private double latitudeDelta, longitudeDelta;
	
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
	
	public MissionThread(MyLocation locationProvider, MainActivity activity) {
		this.locationProvider = locationProvider;
		this.activity = activity;
		
		loadWaypoints();
		
		// Register BroadcastReceiver
		LocalBroadcastManager.getInstance(activity).registerReceiver(
				broadcastReceiver, broadcastIntentFilter());
		
		start();
		activity.missionIsRunning(true);
	}

	private void doAction(String direction) {
		if (enabled) { activity.displayAction(direction); }
	}
	
	public void finnish() {
		enabled = false;
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
	
	@Override
	public void run() {
		Location currentLocation, startLocation, targetLocation;
		
		
			// Get starting location
			startLocation = locationProvider.getLastLocation();
			while (startLocation == null && enabled) {
				// GPS not ready, wait and try again
				try { sleep(1000); } catch (InterruptedException e) { }
							
				startLocation = locationProvider.getLastLocation();
			}

			// Set first target
			targetLocation = getNextWaypoint();
			if (targetLocation == null) {
				// First waypoint must never be null
				endMission();
				return;
			}
			
			
			// Take off, utils.takeoff();
			
			
			// Navigation loop
			while (enabled) {
				// Get current location
				currentLocation = locationProvider.getLastLocation();

				// Calculate distance, in degrees, to the target
				latitudeDelta  = currentLocation.getLatitude()  - targetLocation.getLatitude();
				longitudeDelta = currentLocation.getLongitude() - targetLocation.getLongitude();

				// Have we reached the target?
				if (!waypointReached()) {
					// Not yet reached
					performMovement();
				}
				else {
					// We reached the target
					// Hover, utils.hover();
					
					doAction("");
				}

				try { sleep(NAVIGATION_LOOP_PERIOD); } catch (InterruptedException e) { }
			}
			
			
			// Finish mission, utils.returnToLaunch();
			
		
			endMission();
	}

	private Location getNextWaypoint() {
		if (waypoints.size() == 0) { return null; }
		
		Waypoint waypoint = waypoints.remove(0);
		Location location = new Location("");
		location.setLatitude(waypoint.latitude);
		location.setLongitude(waypoint.longitude);
		return location;
	}
	
	private void performMovement() {
		String action = "";
		
		if (Math.abs(latitudeDelta) > WAYPOINT_LATITUDE_ERROR) {
			if (latitudeDelta < 0) {
				action = "FORWARD";
			}
			else if (latitudeDelta > 0) {
				action = "BACKWARD";
			}
		}
		
		if (Math.abs(longitudeDelta) > WAYPOINT_LONGITUDE_ERROR) {
			if (longitudeDelta < 0) {
				action = action + " RIGHT";
			}
			else if (longitudeDelta > 0) {
				action = action + " LEFT";
			}
		}
		
		doAction(action);
	}
	
	private boolean waypointReached() {
		if (Math.abs(latitudeDelta) > WAYPOINT_LATITUDE_ERROR) { return false; }
		if (Math.abs(longitudeDelta) > WAYPOINT_LONGITUDE_ERROR) { return false; }
		return true;
	}
	
	private void endMission() {
		activity.missionIsRunning(false);
		
		// Unregister receiver
		LocalBroadcastManager.getInstance(activity).unregisterReceiver(broadcastReceiver);
	}
	
	/**
	 * Receive important events
	 */
	private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			
			if (action.equals(MyLocation.ABORT_MISSION)) {
				finnish();
		    }
		}
	};
	
	/**
	 * Intents to listen to
	 */
	private static IntentFilter broadcastIntentFilter() {
		final IntentFilter intentFilter = new IntentFilter();
		
		intentFilter.addAction(MyLocation.ABORT_MISSION);
		
		return intentFilter;
	}
}
