package es.upc.lewis.navigationtest;

import android.location.Location;

public class MissionThread extends Thread {
	private double WAYPOINT_RADIUS = 0.00005;
	
	private volatile boolean enabled = true;
	
	private MyLocation locationProvider;
	private MainActivity activity;
	
	private double latitudeDelta, longitudeDelta;
	
	// Movements. Can be combined, i.e.: RIGHT + FORWARD
	private final int LEFT     = 1;
	private final int RIGHT    = 2;
	private final int FORWARD  = 4;
	private final int BACKWARD = 8;
	
	public MissionThread(MyLocation locationProvider, MainActivity activity) {
		this.locationProvider = locationProvider;
		this.activity = activity;
		
		start();
		activity.missionIsRunning(true);
	}

	private void doAction(String direction) {
		activity.displayAction(direction);
	}
	
	public void finnish() {
		enabled = false;
	}
	
	@Override
	public void run() {
		Location currentLocation, startWP, targetWP;

		// Enter mission waypoints here (latitude, longitude)
		// Not used yet
		double[] waypoints = new double[]{
				41.0000, 2.0000,
				41.0002, 2.0002,
				41.0004, 2.0004
		};
		
		// utils.takeoff();

		
		// Get starting location
		do {
			startWP = locationProvider.getLastLocation();

			if (!enabled) {
				end();
				return;
			}

			// GPS not ready, wait and try again
			try { sleep(1000); } catch (InterruptedException e) { }
		} while (startWP == null);

		// Set target about 17 meters to the east of the starting location
		targetWP = new Location(startWP);
		targetWP.setLongitude(startWP.getLongitude() + 0.0002);

		
		// Navigation loop
		while (enabled) {
			// Get current location
			currentLocation = locationProvider.getLastLocation();

			// Calculate distance, in degrees, to the target
			latitudeDelta  = currentLocation.getLatitude()  - targetWP.getLatitude();
			longitudeDelta = currentLocation.getLongitude() - targetWP.getLongitude();

			// Have we reached the target?
			if (!waypointReached()) {
				// Not yet reached. Decide movement
				switch (decideMovement()) {
				case LEFT:
					doAction("LEFT");
					break;
				case RIGHT:
					doAction("RIGHT");
					break;
				case FORWARD:
					doAction("FORWARD");
					break;
				case BACKWARD:
					doAction("BACKWARD");
					break;
				case LEFT + FORWARD:
					doAction("LEFT + FORWARD");
					break;
				case LEFT + BACKWARD:
					doAction("LEFT + BACKWARD");
					break;
				case RIGHT + FORWARD:
					doAction("RIGHT + FORWARD");
					break;
				case RIGHT + BACKWARD:
					doAction("RIGHT + BACKWARD");
					break;
				}
			}
			else {
				// We reached the target
				doAction("HOVER");
			}

			// utils.wait(500);
			try {
				sleep(500);
			} catch (InterruptedException e) {
			}
		}

		// utils.returnToLaunch();
		end();
	}

	private int decideMovement() {
		int movement = 0;
		
		if (latitudeDelta < 0) { movement = movement + FORWARD; }
		else if (latitudeDelta > 0) { movement = movement + BACKWARD; }
		
		if (longitudeDelta < 0) { movement = movement + RIGHT; }
		else if (longitudeDelta > 0) { movement = movement + LEFT; }
		
		return movement;
	}
	
	private boolean waypointReached() {
		if (Math.abs(latitudeDelta) > WAYPOINT_RADIUS) { return false; }
		if (Math.abs(longitudeDelta) > WAYPOINT_RADIUS) { return false; }
		return true;
	}
	
	private void end() {
		activity.missionIsRunning(false);
	}
}
