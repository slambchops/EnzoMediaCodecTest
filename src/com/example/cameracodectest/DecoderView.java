package com.example.cameracodectest;

import java.nio.ByteBuffer;

import android.annotation.SuppressLint;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;

@SuppressLint("NewApi")
public class DecoderView extends Thread {

	// load the library - name matches jni/Android.mk 
	static {
		System.loadLibrary("jni_cam_enc");
	}

	private native String initCamEnc();
	private native int getEncFrame(ByteBuffer byteBuf);
	private native int closeCamEnc();

	private static final String TAG = "DecoderView";
	private MediaCodec mDecoder;
	private ByteBuffer mEncData;
	private ByteBuffer mAvcSPS;
	private ByteBuffer mAvcPPS;
	private ByteBuffer[] inputBuffers;

	private int retEncSize,inputBufferIndex = 0, checkIndex = 0;
	private String type = "video/avc";
	private MediaFormat decoderOutputFormat = null;

	private String mYuvOutDir = "/data/local/tmp/TEST.YUV";

	private boolean mRunning = true;
	private int mInWidth = 1280;
	private int mInHeight = 720;
	private int mFrameCount = 0;

	private int ENZO_SPS_SIZE = 13;
	private int ENZO_PPS_SIZE = 9;

	public boolean init(Surface surface) {

		// this is where we call the native code
		String hello = initCamEnc();
		Log.i(TAG, hello);

		//alloc for now to grab init data needed to config decoder
		mEncData = ByteBuffer.allocateDirect(mInWidth * mInHeight);
		mAvcSPS = ByteBuffer.allocateDirect(ENZO_SPS_SIZE);
		mAvcPPS = ByteBuffer.allocateDirect(ENZO_PPS_SIZE);
		retEncSize = getEncFrame(mEncData);
		mEncData.clear();
		mEncData.limit(retEncSize);
		mDecoder = MediaCodec.createDecoderByType(type);
		Log.d(TAG, "Configuring codec format");
		MediaFormat format = MediaFormat.createVideoFormat(type,
				mInWidth,
				mInHeight);
		mEncData.get(mAvcSPS.array(), 0, ENZO_SPS_SIZE);
		mEncData.get(mAvcPPS.array(), 0, ENZO_PPS_SIZE);
		format.setByteBuffer("csd-0", mAvcSPS);
		format.setByteBuffer("csd-1", mAvcPPS);
		//Passing a null to argument 2 tells the decoder to send output to
		//byte buffer. Otherwise pass a valid surface.
		mDecoder.configure(format, surface, null, 0);
		mDecoder.start();
		Log.i(TAG, "Opened AVC decoder!");

		return true;
	}

	@Override
	public void run() {

		MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();

		ByteBuffer[] inputBuffers = mDecoder.getInputBuffers();
		ByteBuffer[] outputBuffers = mDecoder.getOutputBuffers();
		ByteBuffer inputBuf = null;

		boolean isInput = true;
		boolean first = false;
		long startWhen = 0;

		Log.d(TAG, "Started running loop!");

		while(mRunning) {
			retEncSize = getEncFrame(mEncData);
			if (retEncSize > 0) {
				inputBufferIndex = mDecoder.dequeueInputBuffer(-1);
				if (inputBufferIndex >= 0) {
					inputBuf = inputBuffers[inputBufferIndex];
					mEncData.clear();
					mEncData.limit(retEncSize);
					inputBuf.clear();
					inputBuf.put(mEncData);
					mDecoder.queueInputBuffer(inputBufferIndex, 0, retEncSize, info.presentationTimeUs, info.flags);
					//Log.d(TAG, "TIMESTAMP=" + info.presentationTimeUs);
				}

				int outIndex = mDecoder.dequeueOutputBuffer(info, 10000);
				switch (outIndex) {
				case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
					Log.d(TAG, "INFO_OUTPUT_BUFFERS_CHANGED");
					mDecoder.getOutputBuffers();
					break;

				case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
					Log.d(TAG, "INFO_OUTPUT_FORMAT_CHANGED format : " + mDecoder.getOutputFormat());
					break;

				case MediaCodec.INFO_TRY_AGAIN_LATER:
					//					Log.d(TAG, "INFO_TRY_AGAIN_LATER");
					break;

				default:
					if (!first) {
						startWhen = System.currentTimeMillis();
						first = true;
					}
					try {
						long sleepTime = (info.presentationTimeUs / 1000) - (System.currentTimeMillis() - startWhen);
						Log.d(TAG, "info.presentationTimeUs : " + (info.presentationTimeUs / 1000) + " playTime: " + (System.currentTimeMillis() - startWhen) + " sleepTime : " + sleepTime);

						if (sleepTime > 0)
							Thread.sleep(sleepTime);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} 

					mDecoder.releaseOutputBuffer(outIndex, true /* Surface init */);
					break;
				}

				// All decoded frames have been rendered, we can stop playing now
				if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
					Log.d(TAG, "OutputBuffer BUFFER_FLAG_END_OF_STREAM");
					break;
				}

			} else {
				Log.e(TAG, "Bad frame from camera");
			}
		}
		mDecoder.stop();
		mDecoder.release();
		mEncData = null;
		closeCamEnc();

		Log.d(TAG, "Exiting running loop!");

	}

	public void close() {
		mRunning = false;
	}

}
