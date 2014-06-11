package es.upc.lewis.quadadk.comms;

import org.apache.http.entity.ByteArrayEntity;

import android.util.Log;

public class SendPictureThread extends Thread {
	String quadid;
	byte[] data;
	
	public SendPictureThread(String quadid, byte[] data) {
		this.quadid = quadid;
		this.data = data;
		start();
	}
	
	@Override
	public void run() {
		Boolean res = HTTPCalls.send_picture(new ByteArrayEntity(data), quadid);
		Log.d("TESTS", "Picture sent: " + res.toString());
	}
}
