package es.upc.lewis.quadadk.comms;

import java.io.File;

import android.util.Log;

public class SendPictureThread extends Thread {
	File file;
	String name;
	
	public SendPictureThread(File file, String name) {
		this.file = file;
		this.name = name;
		start();
	}
	
	@Override
	public void run() {
		Boolean res = HTTPCalls.send_picture(file, name);
		Log.d("TESTS", "Picture sent: " + res.toString());
	}
}
