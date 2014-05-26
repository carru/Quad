package es.upc.lewis.quadadk.mission;

import es.upc.lewis.quadadk.GetLocation;
import es.upc.lewis.quadadk.MainActivity;
import es.upc.lewis.quadadk.comms.ArduinoCommands;
import es.upc.lewis.quadadk.comms.CommunicationsThread;
import es.upc.lewis.quadadk.comms.GroundStationClient;
import es.upc.lewis.quadadk.comms.GroundStationCommands;
import es.upc.lewis.quadadk.tools.MyLocation;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.support.v4.content.LocalBroadcastManager;

public class MissionThread extends Thread {
	private MissionUtils utils;
	
	private static volatile boolean state = false;
	private static volatile boolean canToggle = false;
	
	public MissionThread(CommunicationsThread comms, GroundStationClient server, MainActivity activity) {
		if (comms == null) { return; }
		
		// Utils class
		utils = new MissionUtils(comms, server, activity);
		
		// Register BroadcastReceiver
		LocalBroadcastManager.getInstance(activity).registerReceiver(
				broadcastReceiver, broadcastIntentFilter());

		// Notify GroundStation mission has started
		server.send(GroundStationCommands.MISSION_START);
		
		start();
	}

	@Override
	public void run() {
		try {
			Location myloc;
			Double difflat;
			Double difflong;
			
//			utils.arm();
//			canToggle = true;
//			
//			utils.send(ArduinoCommands.SET_MODE_STB);
//			utils.send(ArduinoCommands.SET_CH3, 1250);
//			
//			new FixYaw(utils, 0);
//			
//			Location init_loc = GetLocation.currentLocation();
//			Location waypoint= new Location(init_loc);
//			waypoint.setLongitude(10);
//			
//			int s=0;
//			while (true) {
//				Location myloc = GetLocation.currentLocation();
//				Double difflat = myloc.getLatitude() - waypoint.getLatitude();
//				Double difflong = myloc.getLongitude() - waypoint.getLongitude();
//				
//				if (Math.abs(difflong) > (double) 1) {
//					// toggle dreta esquerra
//					toggle();
//				} else {
//					break;
//				}
//				
//				s++;
//				if(s==5) waypoint = init_loc;
//				
//				utils.wait(2000);
//			}
//			
//			canToggle = false;
//			utils.disarm();
			
			
			
			
			utils.takeoff();
			
			Location inici = GetLocation.currentLocation();
			Location target = new Location(inici);
			target.setLongitude(inici.getLongitude()+0.0002);
			
			while (true) {
				myloc = GetLocation.currentLocation();
				
				difflat = myloc.getLatitude() - target.getLatitude();
				difflong = myloc.getLongitude() - target.getLongitude();
				
				if (Math.abs(difflong) > (double) 0.0001) {
					// tira cap a la dreta
					if(difflong<(double) 0)
						utils.send(ArduinoCommands.SET_CH1, 1800);
					else
						utils.send(ArduinoCommands.SET_CH1, 1200);
				} else {
					utils.takePicture();
					break;
				}
				
				utils.wait(500);
			}
			
			utils.returnToLaunch();
			
			
			
			
			
		} catch (AbortException e) {
			canToggle = false;
			// Mission has been aborted
		}
		
		utils.endMission();
	}

	private void toggle() throws AbortException {
		if (!canToggle) {return;}
		if (state) {
			state = false;
			utils.send(ArduinoCommands.SET_CH1, 1900);
		} else {
			state = true;
			utils.send(ArduinoCommands.SET_CH1, 1100);
		}
	}
	
	/**
	 * Receive important events
	 */
	private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
//			try {
			String action = intent.getAction();

			if (action.equals(GroundStationClient.ABORT_MISSION)) {
				utils.abortMission();
				utils.showToast("Mission aborted!");
			}
			else if (action.equals(GroundStationClient.ACK)) {
				MissionUtils.readyToSend = true;
			}
//			else if (action.equals(MyLocation.GPS_UPDATE)) {
//				toggle();
//				utils.showToast("gps");
//			}
//			} catch (AbortException e) {}
		}
	};

	/**
	 * Intents to listen to
	 */
	private static IntentFilter broadcastIntentFilter() {
		final IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(GroundStationClient.ABORT_MISSION);
		intentFilter.addAction(GroundStationClient.ACK);
		intentFilter.addAction(MyLocation.GPS_UPDATE);
		return intentFilter;
	}
}
