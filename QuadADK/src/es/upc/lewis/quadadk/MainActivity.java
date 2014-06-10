package es.upc.lewis.quadadk;

import java.io.IOException;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.future.usb.UsbAccessory;
import com.android.future.usb.UsbManager;

import es.upc.lewis.quadadk.comms.CommunicationsThread;
import es.upc.lewis.quadadk.comms.MissionStatusPolling;
import es.upc.lewis.quadadk.mission.MissionThread;
import es.upc.lewis.quadadk.tools.MyLocation;
import es.upc.lewis.quadadk.tools.SimpleCamera;

public class MainActivity extends Activity {
	public String QUAD_ID = "001";
	
	private static final String TAG = MainActivity.class.getSimpleName();

	private PendingIntent mPermissionIntent;
	private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";
	private boolean mPermissionRequestPending;

	private UsbManager mUsbManager;
	private UsbAccessory mAccessory;
	private ParcelFileDescriptor mFileDescriptor;

	// Worker thread for ADK communications
	private CommunicationsThread comms;

	// Polling thread
	private MissionStatusPolling pollingWorker;

	// Camera
	public static SimpleCamera camera;
	
	// Location
	private MyLocation locationProvider;
	private Location location;

	// Is mission running? (Allow only one instance)
	public static volatile boolean isMissionRunning = false;
	
	// Preferences to store socket details
	SharedPreferences sharedPreferences;
	SharedPreferences.Editor sharedPreferencesEditor;

	// UI references
	private TextView adkStatusText;
	private TextView gpsStatusText;
	private Button CameraButton;
	private TextView latitudeText;
	private TextView longitudeText;
	private TextView altitudeText;
	private TextView accuracyText;
	// UI states
	public static final int CONNECTED = 1;
	public static final int DISCONNECTED = 2;

	private void openAccessory(UsbAccessory accessory) {
		mFileDescriptor = mUsbManager.openAccessory(accessory);
		if (mFileDescriptor != null) {
			mAccessory = accessory;

			comms = new CommunicationsThread(this,
					mFileDescriptor.getFileDescriptor());
			comms.start();

			setADKStatus(CONNECTED);
			Toast.makeText(this, "Accessory opened", Toast.LENGTH_SHORT).show();
			Log.i(TAG, "Accessory opened");
		} else {
			Toast.makeText(this, "Error opening accessory", Toast.LENGTH_SHORT)
					.show();
			Log.e(TAG, "Error opening accessory");
		}
	}

	private void closeAccessory() {
		// Stop worker thread
		if (comms != null) {
			comms.interrupt();
			comms = null;
		}

		try {
			if (mFileDescriptor != null) {
				mFileDescriptor.close();
			}
		} catch (IOException e) {
		} finally {
			mFileDescriptor = null;
			mAccessory = null;

		}

		setADKStatus(DISCONNECTED);
	}

	private OnClickListener cameraButtonListener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			if (camera.isReady()) {
				camera.takePicture();
			}
		}
	};

	private void connect() {
		UsbAccessory[] accessories = mUsbManager.getAccessoryList();
		UsbAccessory accessory = (accessories == null ? null : accessories[0]);

		if (accessory != null) {
			if (mUsbManager.hasPermission(accessory)) {
				openAccessory(accessory);
			} else {
				synchronized (usbReceiver) {
					if (!mPermissionRequestPending) {
						mUsbManager.requestPermission(accessory,
								mPermissionIntent);
						mPermissionRequestPending = true;
					}
				}
			}
		} else {
			Toast.makeText(this, "Error connecting to Arduino",
					Toast.LENGTH_SHORT).show();
			Log.e(TAG, "Error connecting to Arduino");
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.main_activity);
		getUiReferences();
		CameraButton.setOnClickListener(cameraButtonListener);

		setADKStatus(DISCONNECTED);
		setGPSStatus(DISCONNECTED);

		mUsbManager = UsbManager.getInstance(this);
		mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
		registerReceivers();

		// Start camera
		camera = new SimpleCamera(this, (FrameLayout) findViewById(R.id.camera_preview), QUAD_ID);

		// Start location provider
		locationProvider = new MyLocation(this);
		
		// Start polling server for mission start/abort
		//pollingWorker = new MissionStatusPolling(this, QUAD_ID); //TODO: uncomment
		
		
		// Debug buttons
		Button b1 = (Button) findViewById(R.id.button1);
		b1.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(new Intent("start"));
			}
		});
		Button b2 = (Button) findViewById(R.id.button2);
		b2.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(new Intent("abort"));
			}
		});
	}

	@Override
	protected void onResume() {
		super.onResume();

		if (mAccessory == null) { connect(); }
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		closeAccessory();
		unregisterReceivers();

		if (camera != null) { camera.close(); }
		
		if (locationProvider != null) { locationProvider.stop(); locationProvider = null; }
		
		// Stop polling server
		if (pollingWorker != null) { pollingWorker.finnish(); }
	}

	private void getUiReferences() {
		adkStatusText = (TextView) findViewById(R.id.adk_status);
		gpsStatusText = (TextView) findViewById(R.id.gps_status);
		CameraButton = (Button) findViewById(R.id.button9);
		latitudeText = (TextView) findViewById(R.id.latitudeText);
		longitudeText = (TextView) findViewById(R.id.longitudeText);
		altitudeText = (TextView) findViewById(R.id.altitudeText);
		accuracyText = (TextView) findViewById(R.id.accuracyText);
	}

	private void setADKStatus(int type) {
		switch (type) {
		case CONNECTED:
			adkStatusText.setText("Connected");
			adkStatusText.setTextColor(Color.GREEN);
			break;
		case DISCONNECTED:
			adkStatusText.setText("Disconnected");
			adkStatusText.setTextColor(Color.RED);
			break;
		}
	}

	private void setGPSStatus(int type) {
		switch (type) {
		case CONNECTED:
			gpsStatusText.setText("Ready");
			gpsStatusText.setTextColor(Color.GREEN);
			break;
		case DISCONNECTED:
			gpsStatusText.setText("Not ready");
			gpsStatusText.setTextColor(Color.RED);
			break;
		}
	}

	private void mission() {
		Log.i(TAG, "Starting mission");
		
		if (isMissionRunning == false && comms != null) {
		//if (isMissionRunning == false) { //DEBUG
			isMissionRunning = true;
			new MissionThread(comms, this, locationProvider);
		}
	}

	private void displayLocation(Location location) {
		if (location == null) { return; }
		
		latitudeText.setText(Double.toString(location.getLatitude()));
		longitudeText.setText(Double.toString(location.getLongitude()));
		altitudeText.setText(Double.toString(location.getAltitude()));
		accuracyText.setText(Double.toString(location.getAccuracy()));
	}
	
	private void registerReceivers() {
		// GroundStation receiver
		LocalBroadcastManager.getInstance(this).registerReceiver(
				groundStationClientReceiver, groundStationClientIntentFilter());
		
		// Location receiver
		LocalBroadcastManager.getInstance(this).registerReceiver(
				locationReceiver, locationIntentFilter());

		// USB events
		IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
		filter.addAction(UsbManager.ACTION_USB_ACCESSORY_DETACHED);
		registerReceiver(usbReceiver, filter);
	}

	private void unregisterReceivers() {
		// GroundStation receiver
		LocalBroadcastManager.getInstance(this).unregisterReceiver(groundStationClientReceiver);

		// Location receiver
		LocalBroadcastManager.getInstance(this).unregisterReceiver(locationReceiver);

		// USB events
		unregisterReceiver(usbReceiver);
	}

	// Receiver for location updates
	private BroadcastReceiver locationReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			location = locationProvider.getLastLocation();

			displayLocation(location);
			setGPSStatus(MainActivity.CONNECTED);
		}
	};

	private static IntentFilter locationIntentFilter() {
		final IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(MyLocation.GPS_UPDATE);
		return intentFilter;
	}

	// Receiver for GroundStation related intents
	private BroadcastReceiver groundStationClientReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
		
			if (action.equals(MissionStatusPolling.START_MISSION)) {
				mission();
			}
		}
	};

	private static IntentFilter groundStationClientIntentFilter() {
		final IntentFilter intentFilter = new IntentFilter();
		
		intentFilter.addAction(MissionStatusPolling.START_MISSION);
		
		return intentFilter;
	}

	// Receiver for Arduino USB communication
	private final BroadcastReceiver usbReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();

			// Granted USB permission
			if (ACTION_USB_PERMISSION.equals(action)) {
				synchronized (this) {
					UsbAccessory accessory = UsbManager.getAccessory(intent);
					if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
						openAccessory(accessory);
					} else {
						Log.e(TAG, "Permission denied for accessory " + accessory);
					}
					mPermissionRequestPending = false;
				}

				// USB disconnected
			} else if (UsbManager.ACTION_USB_ACCESSORY_DETACHED.equals(action)) {
				UsbAccessory accessory = UsbManager.getAccessory(intent);
				if (accessory != null && accessory.equals(mAccessory)) {
					closeAccessory();
				}
			}
		}
	};
}