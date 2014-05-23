package es.upc.lewis.quadadk.tools;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import es.upc.lewis.quadadk.MainActivity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.os.Environment;
import android.text.format.Time;
import android.util.Log;
import android.widget.FrameLayout;

public class SimpleCamera {
	private String TAG = "SimpleCamera";
	
	private final int MEDIA_TYPE_IMAGE = 1;
	private final int MEDIA_TYPE_VIDEO = 2;
	
	private String FOLDER_NAME = "QuadADK";

	private Camera mCamera;
	private CameraPreview mPreview;
	private boolean isReady = false;
	
	public SimpleCamera(Context context, FrameLayout preview) {
		if (!checkCameraHardware(context)) { return; }
		
		// Create an instance of Camera
        mCamera = getCameraInstance();
        if (mCamera == null) {
        	Log.e(TAG, "Error opening camera");
        	return;
        }
        
        // Create our Preview view and set it as the content of our activity.
        mPreview = new CameraPreview(context, mCamera);
        preview.addView(mPreview);

        isReady = true;
	}
	
	public boolean isReady() { return isReady; }
	
	public void close() {
		isReady = false;
		if (mCamera != null) { mCamera.release(); }
	}
	
	public void takePicture() {
        mCamera.takePicture(null, null, mPicture);
	}
	
	private boolean checkCameraHardware(Context context) {
	    if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)){
	        // This device has a camera
	        return true;
	    } else {
	        // No camera on this device
	        return false;
	    }
	}
	
	private Camera getCameraInstance(){
	    Camera c = null;
	    try {
	        c = Camera.open(); // Attempt to get a Camera instance
	    }
	    catch (Exception e){
	        // Camera is not available (in use or does not exist)
	    }
	    return c; // Returns null if camera is unavailable
	}
	
	private PictureCallback mPicture = new PictureCallback() {

	    @Override
	    public void onPictureTaken(byte[] data, Camera camera) {
	    	Log.i(TAG, "Picture taken");
	    	
	    	// Restart preview
	    	try {
	        	mCamera.startPreview();
	        } catch(Exception e) {
	        	Log.e(TAG, "Error restarting preview. Closing camera");
	        	close();
	        }

	    	// Save picture
	        File pictureFile = getOutputMediaFile(MEDIA_TYPE_IMAGE);
	        if (pictureFile == null){
	            Log.d(TAG, "Error creating media file.");
	            return;
	        }

	        try {
	            FileOutputStream fos = new FileOutputStream(pictureFile);
	            fos.write(data);
	            fos.close();
	        } catch (FileNotFoundException e) {
	            Log.d(TAG, "File not found: " + e.getMessage());
	        } catch (IOException e) {
	            Log.d(TAG, "Error accessing file: " + e.getMessage());
	        }
	        
	        // Send picture to GroundStation
	        sendPicture(data);
	    }
	};
	
	private void sendPicture(byte[] data) {
		if (MainActivity.groundStation != null) { MainActivity.groundStation.sendPicture(data); }
	}
	
	/*private Uri getOutputMediaFileUri(int type){
	      return Uri.fromFile(getOutputMediaFile(type));
	}*/

	private File getOutputMediaFile(int type){
	    // To be safe, you should check that the SDCard is mounted
	    // using Environment.getExternalStorageState() before doing this.

	    File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
	              Environment.DIRECTORY_PICTURES), FOLDER_NAME);
	    // This location works best if you want the created images to be shared
	    // between applications and persist after your app has been uninstalled.

	    // Create the storage directory if it does not exist
	    if (! mediaStorageDir.exists()){
	        if (! mediaStorageDir.mkdirs()){
	            Log.d(TAG, "Failed to create directory");
	            return null;
	        }
	    }

	    // Create a media file name
		Time now = new Time();
		now.setToNow();
	    String timeStamp = now.format2445();
	    File mediaFile;
	    if (type == MEDIA_TYPE_IMAGE){
	        mediaFile = new File(mediaStorageDir.getPath() + File.separator + "IMG_"+ timeStamp + ".jpg");
	    } else if(type == MEDIA_TYPE_VIDEO) {
	        mediaFile = new File(mediaStorageDir.getPath() + File.separator + "VID_"+ timeStamp + ".mp4");
	    } else {
	        return null;
	    }

	    return mediaFile;
	}
}
