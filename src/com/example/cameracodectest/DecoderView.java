package com.example.cameracodectest;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import android.annotation.SuppressLint;
import android.content.Context;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

@SuppressLint("NewApi")
public class DecoderView extends SurfaceView implements SurfaceHolder.Callback, Runnable {

	// load the library - name matches jni/Android.mk 
	static {
		System.loadLibrary("jni_cam_enc");
	}

	private native String initCamEnc();
	private native int getEncFrame(ByteBuffer byteBuf);
	private native int closeCamEnc();

	private static final String TAG = "DecoderView";
	private SurfaceHolder mHolder;
	private MediaCodec mDecoder;
	private ByteBuffer mEncData;
	private ByteBuffer[] inputBuffers;
	CodecOutputSurface outputSurface = null;

	private String mEncFileDir = "/data/local/tmp/OUT.264";
	private String mYuvOutDir = "/data/local/tmp/TEST.YUV";

	private boolean mRunning = true;
	private int mInWidth = 1280;
	private int mInHeight = 720;
	private int mFrameCount = 0;

	@Override
	public void run() {
		int retEncSize,inputBufferIndex = 0, checkIndex = 0;
		String type = "video/avc";
		MediaFormat decoderOutputFormat = null;

		outputSurface = new CodecOutputSurface(mInWidth, mInHeight);

		// this is where we call the native code
		String hello = initCamEnc();
		Log.i(TAG, hello);
		Log.d(TAG, "Started running loop!");

		MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
		//alloc for now to grab init data needed to config decoder
		mEncData = ByteBuffer.allocateDirect(mInWidth * mInHeight);
		retEncSize = getEncFrame(mEncData);
		mEncData.clear();
		mEncData.limit(retEncSize);
		mDecoder = MediaCodec.createDecoderByType(type);
		MediaFormat format = MediaFormat.createVideoFormat(type,
				mInWidth,
				mInHeight);
		format.setByteBuffer("csd-0", mEncData);
		mDecoder.configure(format, null, null, 0);
		mDecoder.start();
		Log.i(TAG, "Opened AVC decoder!");

		ByteBuffer[] inputBuffers = mDecoder.getInputBuffers();
		ByteBuffer[] outputBuffers = mDecoder.getOutputBuffers();
		ByteBuffer inputBuf = null;

		while(mRunning) {
			inputBufferIndex = mDecoder.dequeueInputBuffer(-1);
			if (inputBufferIndex >= 0) {
				inputBuf = inputBuffers[inputBufferIndex];
				retEncSize = getEncFrame(mEncData);
				mEncData.clear();
				mEncData.limit(retEncSize);
				inputBuf.clear();
				inputBuf.put(mEncData);
				mDecoder.queueInputBuffer(inputBufferIndex, 0, retEncSize, info.presentationTimeUs, info.flags);
				Log.d(TAG, "TIMESTAMP=" + info.presentationTimeUs);
			}

			int decoderStatus = mDecoder.dequeueOutputBuffer(info, 10000);
			if (decoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
				// no output available yet
				Log.d(TAG, "no output from decoder available");
			} else if (decoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
				// The storage associated with the direct ByteBuffer may already be unmapped,
				// so attempting to access data through the old output buffer array could
				// lead to a native crash.
				Log.d(TAG, "decoder output buffers changed");
				outputBuffers = mDecoder.getOutputBuffers();
			} else if (decoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
				// this happens before the first frame is returned
				decoderOutputFormat = mDecoder.getOutputFormat();
				Log.d(TAG, "decoder output format changed: " +
						decoderOutputFormat);
			} else if (decoderStatus < 0) {
				Log.e(TAG, "unexpected result from deocder.dequeueOutputBuffer: " + decoderStatus);
			} else {  // decoderStatus >= 0
				Log.d(TAG, "surface decoder given buffer " + decoderStatus +
						" (size=" + info.size + ")");
				if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
					Log.d(TAG, "output EOS");
				}
				ByteBuffer outputFrame = outputBuffers[decoderStatus];
				outputFrame.position(info.offset);
				outputFrame.limit(info.offset + info.size);
				if (info.size == 0) {
					Log.d(TAG, "got empty frame");
				} else {
					Log.d(TAG, "decoded, checking frame " + checkIndex);
				}
				if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
					Log.d(TAG, "output EOS");
				}

				/*if (outputFrame != null) {
					//write to file stuff
					FileChannel channel = null;
					int bytesWrittenToFile = 0;
					try {
						channel = new FileOutputStream(mYuvOutDir, true).getChannel();
					} catch (FileNotFoundException e) {
						e.printStackTrace();
					}
					try {
						bytesWrittenToFile = channel.write(outputFrame);
						Log.i(TAG, "Bytes written to file: " + bytesWrittenToFile);
					} catch (IOException e) {
						e.printStackTrace();
					}
				} else {
					Log.e(TAG, "outputFrame is null!");
				}*/


				boolean doRender = (info.size != 0);
				// As soon as we call releaseOutputBuffer, the buffer will be forwarded
				// to SurfaceTexture to convert to a texture.  The API doesn't guarantee
				// that the texture will be available before the call returns, so we
				// need to wait for the onFrameAvailable callback to fire.
				mDecoder.releaseOutputBuffer(decoderStatus, doRender);
				/*if (doRender) {
					Log.d(TAG, "awaiting frame " + checkIndex);
					outputSurface.awaitNewImage();
					outputSurface.drawImage(false);
					if (!checkSurfaceFrame(checkIndex++)) {
					    badFrames++;
					}
				}*/
			}
		}
		mDecoder.stop();
		mDecoder.release();
		mEncData = null;
		if (outputSurface != null) {
			outputSurface.release();
			outputSurface = null;
		}
		closeCamEnc();

		Log.d(TAG, "Exiting running loop!");

	}

	public DecoderView(Context context) {
		super(context);
		// Install a SurfaceHolder.Callback so we get notified when the
		// underlying surface is created and destroyed.
		mHolder = getHolder();
		mHolder.addCallback(this);
	}

	public DecoderView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		// Install a SurfaceHolder.Callback so we get notified when the
		// underlying surface is created and destroyed.
		mHolder = getHolder();
		mHolder.addCallback(this);
	}

	public DecoderView(Context context, AttributeSet attrs) {
		super(context, attrs);
		// Install a SurfaceHolder.Callback so we get notified when the
		// underlying surface is created and destroyed.
		mHolder = getHolder();
		mHolder.addCallback(this);
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		mHolder = holder;
		mRunning = true;
		(new Thread(this)).start();
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		// TODO Auto-generated method stub

	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		mRunning = false;
	}

	private static long computePresentationTime(int frameIndex) {
		return 132 + frameIndex * 1000000 / 30;
	}

}
