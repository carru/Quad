package es.upc.lewis.quadadk.tools;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import android.content.Context;
import android.location.Location;
import android.os.Environment;
import android.text.format.Time;

public class GPSLogger {
	private Context context;
	private BufferedWriter bufferedWriter;

	// To check if it is ready and working (can write)
	private boolean isReady;

	public GPSLogger(Context context) {
		this.context = context;

		isReady = false;
	}
	
	public synchronized void write(Location location) {
		if (!isReady) { return; }
		if (location == null) { return; }
		
		try {
			bufferedWriter.write(Double.toString(location.getLatitude()) + ";" 
					+ Double.toString(location.getLongitude()) + ";"
					+ Double.toString(location.getAltitude()) + ";"
					+ Double.toString(location.getAccuracy()) + ";\n");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void close() {
		try {
			bufferedWriter.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * Checks if external storage is available for read and write
	 * 
	 * @return
	 */
	private boolean isExternalStorageWritable() {
		String state = Environment.getExternalStorageState();
		if (Environment.MEDIA_MOUNTED.equals(state)) {
			return true;
		}
		return false;
	}

	public void createLogFile() {
		// Check if filesystem is writable
		if (!isExternalStorageWritable()) { return; }
				
		String fileName = getDefaultFileName();

		File file = new File(context.getExternalFilesDir(null), fileName + ".log");

		// BufferedWriter for performance
		try {
			bufferedWriter = new BufferedWriter(new FileWriter(file));
			isReady = true;
		} catch (IOException e) {
			isReady = false;
			e.printStackTrace();
		}
	}

	/**
	 * Return timestamp filename in readable format
	 * 
	 * @return file name
	 */
	private String getDefaultFileName() {
		Time now = new Time();
		now.setToNow();
		return now.format2445();
	}
}
