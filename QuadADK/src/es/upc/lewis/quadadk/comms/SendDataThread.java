package es.upc.lewis.quadadk.comms;

import es.upc.lewis.quadadk.mission.MissionThread;

public class SendDataThread extends Thread {
	String varName;
	String value;
	
	/**
	 * 
	 * @param varName temp1, temp2, hum1, hum2, co, no2, alt_bar, alt_gps
	 * @param value
	 */
	public SendDataThread(String varName, String value) {
		this.varName = varName;
		this.value = value;
		start();
	}
	
	@Override
	public void run() {
		HTTPCalls.send_data(MissionThread.QUAD_ID, varName, value);
	}
}
