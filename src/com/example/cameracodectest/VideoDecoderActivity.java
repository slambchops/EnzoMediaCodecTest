package com.example.cameracodectest;

import android.app.Activity;
import android.os.Bundle;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class VideoDecoderActivity extends Activity implements SurfaceHolder.Callback {

	private DecoderView mAvcDecoder;
	private SurfaceView surfaceView;
	private SurfaceHolder surfaceHolder;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		surfaceView = (SurfaceView) findViewById(R.id.surfaceView);
		surfaceHolder = surfaceView.getHolder();
		surfaceHolder.addCallback(this);
		setContentView(surfaceView);

		mAvcDecoder = new DecoderView();
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {

	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,	int height) {
		if (mAvcDecoder != null) {
			if (mAvcDecoder.init(holder.getSurface())) {
				mAvcDecoder.start();

			} else {
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
