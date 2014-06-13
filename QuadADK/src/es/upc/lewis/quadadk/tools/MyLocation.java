package es.upc.lewis.quadadk.tools;

import es.upc.lewis.quadadk.comms.SendDataThread;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;

public class MyLocation implements LocationListener {
	private long MIN_TIME_BETWEEN_UPDATES = 0;   // Milliseconds, not accurate
	private long MIN_DISTANCE_BETWEEN_UPDATES = 0; // Meters
	
	private float MAX_ERROR_IN_METERS = 20;
	private int MAX_BAD_READINGS_FOR_FAILSAFE = 5;
	private int numberOfBadReadings = 0;
	
	private LocationManager locationManager;
	private Location lastLocation = null;
	
	public static final String GPS_UPDATE = "g";
	private Intent intent;
	private Context context;

	public MyLocation(Context context) {
		this.context = context;
		
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

	public void setLocation_DEBUG_ONLY(Location location) {
		lastLocation = location;
		// Notify there's an update
		intent = new Intent(GPS_UPDATE);
		LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
	}
	
	@Override
	public void onLocationChanged(Location location) {
		// Discard locations with poor accuracy
		if (location.getAccuracy() > MAX_ERROR_IN_METERS) {
			numberOfBadReadings++;
			if (numberOfBadReadings >= MAX_BAD_READINGS_FOR_FAILSAFE) { gpsFailsafe(); }
			return;
		}
		
		numberOfBadReadings = 0;
		lastLocation = location;
		
		// Notify there's an update
		intent = new Intent(GPS_UPDATE);
		LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
		
		// Send altitude
		new SendDataThread("alt_gps", Double.toString(lastLocation.getAltitude()));
	}

	private void gpsFailsafe() {
		// Send abort mission broadcast
		//TODO: we don't do it yet
//		intent = new Intent(MissionStatusPolling.ABORT_MISSION);
//		LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
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
