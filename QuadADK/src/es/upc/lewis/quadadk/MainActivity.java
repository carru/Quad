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
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.future.usb.UsbAccessory;
import com.android.future.usb.UsbManager;

import es.upc.lewis.quadadk.comms.CommunicationsThread;
import es.upc.lewis.quadadk.comms.GroundStationClient;
import es.upc.lewis.quadadk.comms.GroundStationCommands;
import es.upc.lewis.quadadk.comms.MissionStatusPolling;
import es.upc.lewis.quadadk.mission.MissionThread;
import es.upc.lewis.quadadk.tools.MyLocation;
import es.upc.lewis.quadadk.tools.SimpleCamera;

public class MainActivity extends Activity {
	public String quadid = "001";
	
	private static final String TAG = MainActivity.class.getSimpleName();

	private PendingIntent mPermissionIntent;
	private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";
	private boolean mPermissionRequestPending;

	private UsbManager mUsbManager;
	private UsbAccessory mAccessory;
	private ParcelFileDescriptor mFileDescriptor;

	// Worker thread for ADK communications
	private CommunicationsThread comms;

	// Class to communicate with the GroundStation
	public static GroundStationClient groundStation;
	//TODO: this class will poll the server to start and abort the mission
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
	private TextView serverStatusText;
	private Button CameraButton;
	private Button connectToServerButton;
	private EditText ipEditText;
	private EditText portEditText;
	private TextView latitude;
	private TextView longitude;
	// UI states
	public static final int CONNECTED = 1;
	public static final int CONNECTING = 2;
	public static final int DISCONNECTED = 3;

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

	private OnClickListener connectToServerButtonListener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			String ip = ipEditText.getText().toString();
			int port;
			try {
				port = Integer.parseInt(portEditText.getText().toString());
			} catch (NumberFormatException e) {
				Toast.makeText(getApplicationContext(), "Invalid port",
						Toast.LENGTH_SHORT).show();
				return;
			}

			groundStation = new GroundStationClient(ip, port,
					getApplicationContext());

			// Save socket details
			sharedPreferencesEditor.putString("ip", ip);
			sharedPreferencesEditor.putInt("port", port);
			sharedPreferencesEditor.apply();
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
		connectToServerButton.setOnClickListener(connectToServerButtonListener);

		setADKStatus(DISCONNECTED);
		setServerStatus(DISCONNECTED);

		mUsbManager = UsbManager.getInstance(this);
		mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
		registerReceivers();

		// Start camera
		camera = new SimpleCamera(this, (FrameLayout) findViewById(R.id.camera_preview));

		// Start location provider
		locationProvider = new MyLocation(this);
		
		// Get last socket details
		sharedPreferences = getPreferences(Context.MODE_PRIVATE);
		sharedPreferencesEditor = sharedPreferences.edit();
		ipEditText.setText(sharedPreferences.getString("ip", ""));
		portEditText.setText(Integer.toString(sharedPreferences.getInt("port", 9090)));
		
		//TODO: implement new GroundStation system
		// Start polling server for mission start/abort
		pollingWorker = new MissionStatusPolling(this, quadid);
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
		serverStatusText = (TextView) findViewById(R.id.server_status);
		CameraButton = (Button) findViewById(R.id.button9);
		connectToServerButton = (Button) findViewById(R.id.server_connect_button);
		ipEditText = (EditText) findViewById(R.id.server_ip);
		portEditText = (EditText) findViewById(R.id.server_port);
		latitude = (TextView) findViewById(R.id.latitudeText);
		longitude = (TextView) findViewById(R.id.longitudeText);
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

	private void setServerStatus(int type) {
		switch (type) {
		case CONNECTED:
			serverStatusText.setText("Connected");
			serverStatusText.setTextColor(Color.GREEN);
			break;
		case CONNECTING:
			serverStatusText.setText("Connecting");
			serverStatusText.setTextColor(Color.YELLOW);
			break;
		case DISCONNECTED:
			serverStatusText.setText("Disconnected");
			serverStatusText.setTextColor(Color.RED);
			break;
		}
	}

	private void mission() {
		Log.i(TAG, "Starting mission");
		
		if (isMissionRunning == false && comms != null) {
		//if (isMissionRunning == false) { //DEBUG
			isMissionRunning = true;
			new MissionThread(comms, groundStation, this, locationProvider);
		}
	}
	
//	private void sendSensorData(byte sensor, int value) {
//		if (groundStation == null) { return; }
//		groundStation.send(sensor, value);
//	}

	private void displayLocation(Location location) {
		if (location == null) { return; }
		
		latitude.setText(Double.toString(location.getLatitude()));
		longitude.setText(Double.toString(location.getLongitude()));
	}
	
	private void registerReceivers() {
		// Sensor data receiver
//		LocalBroadcastManager.getInstance(this).registerReceiver(
//				sensorDataReceiver, sensorDataIntentFilter());

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
		// Sensor data receiver
//		LocalBroadcastManager.getInstance(this).unregisterReceiver(
//				sensorDataReceiver);

		// GroundStation receiver
		LocalBroadcastManager.getInstance(this).unregisterReceiver(
				groundStationClientReceiver);

		// Location receiver
		LocalBroadcastManager.getInstance(this).unregisterReceiver(
				locationReceiver);

		// USB events
		unregisterReceiver(usbReceiver);
	}

	// Receiver for location updates
		private BroadcastReceiver locationReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				location = locationProvider.getLastLocation();
				
				displayLocation(location);
			}
		};

		private static IntentFilter locationIntentFilter() {
			final IntentFilter intentFilter = new IntentFilter();
			intentFilter.addAction(MyLocation.GPS_UPDATE);
			return intentFilter;
		}
	
	// Receiver for Arduino application
//	private BroadcastReceiver sensorDataReceiver = new BroadcastReceiver() {
//		@Override
//		public void onReceive(Context context, Intent intent) {
//			String action = intent.getAction();
//			
//			// Get the value (4 bytes) as an int
//			int intBytes = intent.getIntExtra(CommunicationsThread.VALUE, 0);
//			// bytes to float
//			//float value = Float.intBitsToFloat(intBytes);
//			
//			if (action
//					.equals(CommunicationsThread.ACTION_DATA_AVAILABLE_SENSOR_TEMPERATURE)) {
//				sendSensorData(GroundStationCommands.SENSOR_TEMPERATURE, intBytes);
//			} else if (action
//					.equals(CommunicationsThread.ACTION_DATA_AVAILABLE_SENSOR_HUMIDITY)) {
//				sendSensorData(GroundStationCommands.SENSOR_HUMIDITY, intBytes);
//			} else if (action
//					.equals(CommunicationsThread.ACTION_DATA_AVAILABLE_SENSOR_NO2)) {
//				sendSensorData(GroundStationCommands.SENSOR_NO2, intBytes);
//			} else if (action
//					.equals(CommunicationsThread.ACTION_DATA_AVAILABLE_SENSOR_CO)) {
//				sendSensorData(GroundStationCommands.SENSOR_CO, intBytes);
//			}
//		}
//	};
//
//	private static IntentFilter sensorDataIntentFilter() {
//		final IntentFilter intentFilter = new IntentFilter();
//		intentFilter.addAction(CommunicationsThread.ACTION_DATA_AVAILABLE_SENSOR_TEMPERATURE);
//		intentFilter.addAction(CommunicationsThread.ACTION_DATA_AVAILABLE_SENSOR_HUMIDITY);
//		intentFilter.addAction(CommunicationsThread.ACTION_DATA_AVAILABLE_SENSOR_NO2);
//		intentFilter.addAction(CommunicationsThread.ACTION_DATA_AVAILABLE_SENSOR_CO);
//		return intentFilter;
//	}

	// Receiver for GroundStation related intents
	private BroadcastReceiver groundStationClientReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();

			// These are for the old server (Java GUI)
//			if (action.equals(GroundStationClient.CONNECTED)) {
//				setServerStatus(MainActivity.CONNECTED);
//			} else if (action.equals(GroundStationClient.CONNECTING)) {
//				setServerStatus(MainActivity.CONNECTING);
//			} else if (action.equals(GroundStationClient.DISCONNECTED)) {
//				setServerStatus(MainActivity.DISCONNECTED);
//			} else if (action.equals(GroundStationClient.CANT_RESOLVE_HOST)) {
//				Toast.makeText(getApplicationContext(), "Can't resolve host", Toast.LENGTH_SHORT).show();
//			} else if (action.equals(GroundStationClient.START_MISSION)) {
//				mission();
//			}
		
			if (action.equals(MissionStatusPolling.START_MISSION)) {
				//Toast.makeText(getApplicationContext(), "Start", Toast.LENGTH_SHORT).show();
				mission();
			}
//			else if (action.equals(MissionStatusPolling.ABORT_MISSION)) {
//				Toast.makeText(getApplicationContext(), "Abort", Toast.LENGTH_SHORT).show();
//			}
		}
	};

	private static IntentFilter groundStationClientIntentFilter() {
		final IntentFilter intentFilter = new IntentFilter();
		
		// These are for the old server (Java GUI)
//		intentFilter.addAction(GroundStationClient.CONNECTED);
//		intentFilter.addAction(GroundStationClient.CONNECTING);
//		intentFilter.addAction(GroundStationClient.DISCONNECTED);
//		intentFilter.addAction(GroundStationClient.CANT_RESOLVE_HOST);
//		intentFilter.addAction(GroundStationClient.START_MISSION);
		
		intentFilter.addAction(MissionStatusPolling.START_MISSION);
//		intentFilter.addAction(MissionStatusPolling.ABORT_MISSION);
		
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