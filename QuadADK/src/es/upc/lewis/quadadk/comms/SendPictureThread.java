package es.upc.lewis.quadadk.comms;

import java.io.File;

import org.apache.http.entity.ByteArrayEntity;

import android.util.Log;

public class SendPictureThread extends Thread {
	String quadid;
	byte[] data;
	File file;
	String name;
	
	public SendPictureThread(String quadid, byte[] data, File file, String name) {
		this.quadid = quadid;
		this.data = data;
		this.file = file;
		this.name = name;
		start();
	}
	
	@Override
	public void run() {
		Boolean res = HTTPCalls.send_picture(new ByteArrayEntity(data), quadid, file, name);
		Log.d("TESTS", "Picture sent: " + res.toString());
	}
}
