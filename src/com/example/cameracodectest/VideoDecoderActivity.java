package com.example.cameracodectest;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class VideoDecoderActivity extends Activity implements SurfaceHolder.Callback {

	private static final String TAG = "VideoDecoderActivity";
	private DecoderView mAvcDecoder;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		SurfaceView surfaceView = new SurfaceView(this);
		surfaceView.getHolder().addCallback(this);
		setContentView(surfaceView);

		mAvcDecoder = new DecoderView();
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {

	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,	int height) {
		if (mAvcDecoder != null || holder.getSurface() != null) {
			Log.i(TAG, "About to init decoder...");
			if (mAvcDecoder.init(holder.getSurface())) {
				mAvcDecoder.start();

			} else {
				Log.e(TAG, "Could not init decoder because decoder or surface was null!");
				mAvcDecoder = null;
			}

		}
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		if (mAvcDecoder != null) {
			mAvcDecoder.close();
		}
	}
}
