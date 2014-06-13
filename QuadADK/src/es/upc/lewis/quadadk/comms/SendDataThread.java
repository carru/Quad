package es.upc.lewis.quadadk.comms;

import es.upc.lewis.quadadk.MainActivity;

public class SendDataThread extends Thread {
	String varName;
	String value;
	
	public SendDataThread(String varName, String value) {
		this.varName = varName;
		this.value = value;
		start();
	}
	
	@Override
	public void run() {
		HTTPCalls.send_data(MainActivity.QUAD_ID, varName, value);
	}
}
