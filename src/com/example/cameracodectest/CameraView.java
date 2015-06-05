package com.example.cameracodectest;

import java.io.IOException;

import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class CameraView extends SurfaceView implements SurfaceHolder.Callback, Runnable {
	
	private static final String TAG = "CameraView";
	
	private SurfaceHolder mHolder;
	private Camera mCamera;
	private boolean mRunning = true;

	/** Check if this device has a camera */
	private boolean checkCameraHardware(Context context) {
	    if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)){
	    	Log.i(TAG, "Camera detected!");
	        return true;
	    } else {
	    	Log.i(TAG, "No camera detected!");
	        return false;
	    }
	}
	
	/** A safe way to get an instance of the Camera object. */
	public static Camera getCameraInstance() {
	    Camera c = null;
	    int num;
	    
	    num = c.getNumberOfCameras();
	    Log.i(TAG, "Number of cameras = " + num);
	    
	    //return null if there are no cameras
	    if (num < 1)
	    	return c;
	    
	    try {
	        c = Camera.open(num - 1); // attempt to get a Camera instance
	    }
	    catch (Exception e){
	        // Camera is not available (in use or does not exist)
	    }
	    
	    if (c == null)
	    	Log.i(TAG, "Camera not opened!");
	    else
	    	Log.i(TAG, "Camera opened!");
	    
	    return c; // returns null if camera is unavailable
	}
	
	public CameraView(Context context) {
		super(context);
		checkCameraHardware(context);
		mCamera = getCameraInstance();
        // Install a SurfaceHolder.Callback so we get notified when the
        // underlying surface is created and destroyed.
        mHolder = getHolder();
        mHolder.addCallback(this);
	}
	
	public CameraView(Context context, AttributeSet attrs, int defStyle) {
	    super(context, attrs, defStyle);
	    checkCameraHardware(context);
	    mCamera = getCameraInstance();
        // Install a SurfaceHolder.Callback so we get notified when the
        // underlying surface is created and destroyed.
        mHolder = getHolder();
        mHolder.addCallback(this);
	}

	public CameraView(Context context, AttributeSet attrs) {
	    super(context, attrs);
	    checkCameraHardware(context);
	    mCamera = getCameraInstance();
        // Install a SurfaceHolder.Callback so we get notified when the
        // underlying surface is created and destroyed.
        mHolder = getHolder();
        mHolder.addCallback(this);
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
        // The Surface has been created, now tell the camera where to draw the preview.
	    Camera.Parameters parameters = mCamera.getParameters();
	    mCamera.setParameters(parameters);
	    
        try {
            mCamera.setPreviewDisplay(holder);
            mCamera.startPreview();
        } catch (IOException e) {
            Log.d(TAG, "Error setting camera preview: " + e.getMessage());
        }
        
        mRunning = true;
		//(new Thread(this)).start();
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
        // If your preview can change or rotate, take care of those events here.
        // Make sure to stop the preview before resizing or reformatting it.

        if (mHolder.getSurface() == null){
          // preview surface does not exist
          return;
        }

        // stop preview before making changes
        try {
            mCamera.stopPreview();
        } catch (Exception e){
          // ignore: tried to stop a non-existent preview
        }

        // set preview size and make any resize, rotate or
        // reformatting changes here
        mCamera.getParameters();

        // start preview with new settings
        try {
            mCamera.setPreviewDisplay(mHolder);
            mCamera.startPreview();

        } catch (Exception e){
            Log.d(TAG, "Error starting camera preview: " + e.getMessage());
        }
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		mRunning = false;
		mCamera.stopPreview();
		mCamera.release();
		mCamera = null;
	}

	@Override
	public void run() {

		Log.d(TAG, "Started running loop!");

		while(mRunning) {

		}

		Log.d(TAG, "Exiting running loop!");

	}
}
