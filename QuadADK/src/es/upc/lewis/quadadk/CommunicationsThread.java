package es.upc.lewis.quadadk;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

public class CommunicationsThread extends Thread {
	Context context;
	// Broadcast types
    public static final String ACTION_DATA_AVAILABLE_SENSOR_1 = "1";
    public static final String ACTION_DATA_AVAILABLE_SENSOR_2 = "2";
    public static final String ACTION_DATA_AVAILABLE_SENSOR_3 = "3";
    // Fields
    public static final String VALUE = "4";
    
	// Buffer for read operations (bytes)
	private final int READ_BUFFER_SIZE = 1024;
	
	private FileInputStream mInputStream;
	private FileOutputStream mOutputStream;
	
	public CommunicationsThread(Context context, FileDescriptor fd) {
		this.context = context;
		
		mInputStream = new FileInputStream(fd);
		mOutputStream = new FileOutputStream(fd);
	}
	
	@Override
	public void run() {
		readLoop();
	}
	
	public void send(byte command) {
		try {
			mOutputStream.write(new byte[]{command});
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void send(byte command, int value) {
		byte[] buffer = new byte[3];
		buffer[0] = command;
		buffer[1] = (byte) (value >> 8);
		buffer[2] = (byte) (value & 0xFF);
		
		try {
			mOutputStream.write(buffer);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void readLoop() {
		int bytes = 0;
		byte[] buffer = new byte[READ_BUFFER_SIZE];
		
		try {
			while (true) {
				if (interrupted()) { return; }
				
				// Read (blocking)
				bytes = mInputStream.read(buffer);
				
				parse(buffer, bytes);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void parse(byte[] buffer, int bytes) {
		Intent intent;
		
		switch (buffer[0]) {
		case Commands.DATA_SENSOR_1:
			intent = new Intent(ACTION_DATA_AVAILABLE_SENSOR_1);
			break;
			
		case Commands.DATA_SENSOR_2:
			intent = new Intent(ACTION_DATA_AVAILABLE_SENSOR_2);
			break;
			
		case Commands.DATA_SENSOR_3:
			intent = new Intent(ACTION_DATA_AVAILABLE_SENSOR_3);
			break;
			
		default:
			return; // Do nothing
		}
		
		// Get integer (4 bytes)
		ByteBuffer bBuffer = ByteBuffer.wrap(buffer, 1, 4);
		intent.putExtra(VALUE, bBuffer.getInt());
		
    	LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
	}
}
