package es.upc.lewis.navigationtest;

import android.location.Location;

public class MissionThread extends Thread {
	private volatile boolean enabled = true;
	
	private MyLocation locationProvider;
	
	private MainActivity activity;
	
	public MissionThread(MyLocation locationProvider, MainActivity activity) {
		this.locationProvider = locationProvider;
		this.activity = activity;
		
		start();
		activity.missionIsRunning(true);
	}

	private void move(String direction) {
		activity.displayAction(direction);
	}
	
	public void finnish() {
		enabled = false;
	}
	
	@Override
	public void run() {
			Location myloc, inici, target;
			Double difflat, difflong;
			
			//utils.takeoff();
			
			// Get starting location
			do {
				inici = locationProvider.getLastLocation();
				if (!enabled) {
					end();
					return;
				}
			} while (inici == null);
			
			// Set target about 17 meters to the east
			target = new Location(inici);
			target.setLongitude(inici.getLongitude()+0.0002);
			
			while (enabled) {
				myloc = locationProvider.getLastLocation();
				
				difflat = myloc.getLatitude() - target.getLatitude();
				difflong = myloc.getLongitude() - target.getLongitude();
				
				if (Math.abs(difflong) > (double) 0.00005) {
					// tira cap a la dreta
					if(difflong<(double) 0)
						move("RIGHT");
					else
						move("LEFT");
				} else {
					move("HOVER");
					
					// Don't end mission (to see if it tries to go back)
					//break;
				}
				
				//utils.wait(500);
				try { sleep(500); } catch (InterruptedException e) { }
			}
			
			//utils.returnToLaunch();
			end();
	}
	
	private void end() {
		activity.missionIsRunning(false);
	}
}
