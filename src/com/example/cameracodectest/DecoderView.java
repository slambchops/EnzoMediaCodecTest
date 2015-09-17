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
	private SurfaceHolder mHolder;
	private MediaCodec mDecoder;
	private ByteBuffer mEncData;
	private ByteBuffer mAvcSPS;
	private ByteBuffer mAvcPPS;
	private ByteBuffer[] inputBuffers;
	CodecOutputSurface outputSurface = null;
	private Surface mTestOutputSurface;

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
		outputSurface = new CodecOutputSurface(mInWidth, mInHeight);

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
		mDecoder.configure(format, mHolder.getSurface(), null, 0);
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
					//Log.d(TAG, "surface decoder given buffer " + decoderStatus +
					//" (size=" + info.size + ")");
					if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
						Log.d(TAG, "output EOS");
					}
					ByteBuffer outputFrame = outputBuffers[decoderStatus];
					outputFrame.position(info.offset);
					outputFrame.limit(info.offset + info.size);
					if (info.size == 0) {
						//Log.d(TAG, "got empty frame");
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

					//Set doRender to false since we aren't rendering to surface
					boolean doRender = (info.size != 0);
					// As soon as we call releaseOutputBuffer, the buffer will be forwarded
					// to SurfaceTexture to convert to a texture.  The API doesn't guarantee
					// that the texture will be available before the call returns, so we
					// need to wait for the onFrameAvailable callback to fire.
					mDecoder.releaseOutputBuffer(decoderStatus, doRender);
					if (doRender) {
						Log.d(TAG, "awaiting frame " + checkIndex);
						outputSurface.awaitNewImage();
						outputSurface.drawImage(true);
					}
				}

			} else {
				Log.e(TAG, "Bad frame from camera");
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
	
	public void close() {
		mRunning = false;
	}

}
