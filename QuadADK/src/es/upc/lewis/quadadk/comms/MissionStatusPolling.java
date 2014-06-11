package es.upc.lewis.quadadk.comms;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

public class MissionStatusPolling extends Thread {
	// Parameters
	private int POLLING_PERIOD = 1000; // Milliseconds
	
	// Intents
	public static final String START_MISSION = "start";
	public static final String ABORT_MISSION = "abort";
	
	private String TAG = "Polling Thread";
	
	private volatile boolean enabled = true;
	private String quadid;
	private Context context;
	
	public MissionStatusPolling(Context context, String quadid) {
		this.context = context;
		this.quadid = quadid;
		start();
	}
	
	public void finnish() {
		enabled = false;
	}
	
	@Override
	public void run() {
		boolean log = true;
		
		while(enabled) {
			if (HTTPCalls.get_startmission(quadid)) {
				notifyAction(START_MISSION);
				if (log) { Log.i(TAG, "start"); }
			} else if (HTTPCalls.get_abortmission(quadid)) {
				notifyAction(ABORT_MISSION);
				if (log) { Log.i(TAG, "abort"); }
			} else {
				if (log) { Log.i(TAG, "no flags"); }
			}

			try {
				sleep(POLLING_PERIOD);
			} catch (InterruptedException e) {
			}
		}
	}
	
	private void notifyAction(String status) {
		Intent intent = new Intent(status);
		LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
	}
}
