package es.upc.lewis.navigationtest;

import android.location.Location;
import android.os.Bundle;
import android.app.Activity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends Activity {
	MainActivity activity = this;
	
	// UI
	private TextView latText;
	private TextView longText;
	private TextView accText;
	private TextView altText;
	private Button startBtn;
	private Button abortBtn;
	private static TextView statusText;
	private static TextView actionText;

	private UpdateThread updateWorker;
	
	private MyLocation locationProvider;
	
	private MissionThread missionThread;

	public void displayAction(final String action) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				actionText.setText(action);
			}
		});
	}
	
	public void missionIsRunning(final boolean running) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				if (running) {
					statusText.setText("Running");
				} else {
					statusText.setText("Not running");
					actionText.setText("");
				}
			}
		});
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		// Get UI references
		latText = (TextView) findViewById(R.id.latText);
		longText = (TextView) findViewById(R.id.longText);
		accText = (TextView) findViewById(R.id.accText);
		altText = (TextView) findViewById(R.id.altText);
		startBtn = (Button) findViewById(R.id.startBtn);
		abortBtn = (Button) findViewById(R.id.abortBtn);
		statusText = (TextView) findViewById(R.id.statusText);
		actionText = (TextView) findViewById(R.id.actionText);

		startBtn.setOnClickListener(startBtnListener);
		abortBtn.setOnClickListener(abortBtnListener);
		
		// Start location provider
		locationProvider = new MyLocation(this);
	}

	@Override
	protected void onResume() {
		super.onResume();

		updateWorker = new UpdateThread();
		updateWorker.start();
	}

	@Override
	protected void onStop() {
		updateWorker.finnish();

		if (missionThread != null) { missionThread.finnish(); }
		
		// Stop location provider
		if (locationProvider != null) { locationProvider.stop(); locationProvider = null; }
		
		super.onStop();
	}

	private OnClickListener startBtnListener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			if (missionThread == null) {
				missionThread = new MissionThread(locationProvider, activity);
			}
		}
	};
	
	private OnClickListener abortBtnListener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			if (missionThread != null) {
				missionThread.finnish();
				missionThread = null;
			}
		}
	};
	
	class UpdateThread extends Thread {
		private volatile boolean enabled = true;
		private int UPDATE_PERIOD = 250;

		public void finnish() { enabled = false; }

		@Override
		public void run() {
			while (enabled) {
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						Location location = locationProvider.getLastLocation();
						if (location != null) {
							latText.setText ("Lat: "      + Double.toString(location.getLatitude() ));
							longText.setText("Long: "     + Double.toString(location.getLongitude()));
							accText.setText ("Accuracy: " + Double.toString(location.getAccuracy() ));
							altText.setText ("Altitude: " + Double.toString(location.getAltitude() ));
						}
					}
				});
				try { sleep(UPDATE_PERIOD); } catch (InterruptedException e) { }
			}
		}
	}
}