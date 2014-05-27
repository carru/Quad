package es.upc.lewis.navigationtest;

import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

public class MyLocation implements LocationListener {
	private long MIN_TIME_BETWEEN_UPDATES = 100;   // Milliseconds, not accurate
	private long MIN_DISTANCE_BETWEEN_UPDATES = 0; // Meters
	
	public static final String GPS_UPDATE = "g";
	Intent intent;
	
	private LocationManager locationManager;
	private Location lastLocation = null;

	public MyLocation(Context context) {
		locationManager = (LocationManager) context
				.getSystemService(Context.LOCATION_SERVICE);
		locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
				MIN_TIME_BETWEEN_UPDATES, MIN_DISTANCE_BETWEEN_UPDATES, this);
	}

	public Location getLastLocation() {
		return lastLocation;
	}
	
	public void stop() {
		locationManager.removeUpdates(this);
	}
	
	@Override
	public void onLocationChanged(Location location) {
		lastLocation = location;
	}

	@Override
	public void onProviderDisabled(String provider) {

	}

	@Override
	public void onProviderEnabled(String provider) {

	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {

	}
}
