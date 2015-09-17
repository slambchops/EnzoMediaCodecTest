package com.example.cameracodectest;

import java.nio.ByteBuffer;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.View;
import java.util.Random;

@SuppressLint("NewApi")
public class DecoderView3 extends View implements Runnable {

    // load the library - name matches jni/Android.mk 
    static {
        System.loadLibrary("jni_cam_enc");
    }

    private static final String TAG = "DecoderView";
    private SurfaceHolder mHolder;
    private MediaCodec mDecoder;
    private ByteBuffer mEncData;
    private ByteBuffer mAvcSPS;
    private ByteBuffer mAvcPPS;
    private ByteBuffer[] inputBuffers;
    CodecOutputSurface outputSurface = null;

    private String mYuvOutDir = "/data/local/tmp/TEST.YUV";

    private boolean mRunning = true;
    private final int mInWidth = 1280;
    private final int mInHeight = 720;
    private int mFrameCount = 0;

    private int ENZO_SPS_SIZE = 13;
    private int ENZO_PPS_SIZE = 9;

        private final String STARTED_RUNNING_LOOP = "Started running loop!";
        private final String CSD_0 = "csd-0";
        private final String CSD_1 = "csd-1";
        private final String OPEN_AVE_DECODER = "Opened AVC decoder!";
        private final String TIMESTAMP = "TIMESTAMP=";
        private final String NO_OUTPUT_FROM_DECODER_AVAILABLE = "no output from decoder available";
        private final String DECODER_OUTPUT_BUFFERS_CHANGED = "decoder output buffers changed";
        private final String DECODER_OUTPUT_FORMAT_CHANGE = "decoder output format changed: ";
        private final String UNEXPECTED_RESULT = "unexpected result from deocder.dequeueOutputBuffer: ";
        private final String SURFACE_DECODER_GIVEN_BUFFER = "surface decoder given buffer ";
        private final String SIZE = " (size=";
        private final String END = ")";
        private final String OUTPUT_EOS = "output EOS";
        private final String AWAITING_FRAME = "awaiting frame ";
        private final String GOT_EMPTY_FRAME = "got empty frame";
        private final String DECODED_CHECKING_FRAME = "decoded, checking frame ";
        private final String BAD_FRAME_FROM_CAMERA = "Bad frame from camera";

    private final Random random = new Random();
    //private final String V = "values: ";
    //private final String S = " ";
    boolean doRender;
    private final int[] colors = new int[this.mInHeight * this.mInWidth];
    private final int INT_ARRAY_SIZE = this.mInHeight * this.mInWidth;
    private byte[] frameBytes = new byte[this.mInHeight * this.mInWidth * 4];
    Paint paint = new Paint();
    private int[] quad = new int[4];
    //{Color.RED & 0xFF000000,Color.RED & 0x00FF0000,Color.RED & 0x0000FF00,Color.RED & 0x000000FF};
    
    private final String DRAWING_FRAME = "drawingFrame";

    @Override
    public void run() {
        try {
            int retEncSize, inputBufferIndex = 0, checkIndex = 0;
            String type = "video/avc";
            MediaFormat decoderOutputFormat = null;

            outputSurface = new CodecOutputSurface(mInWidth, mInHeight);

            // this is where we call the native code
            String hello = DecoderView.initCamEnc();
            Log.i(TAG, hello);
		Log.d(TAG, STARTED_RUNNING_LOOP);

            MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
            //alloc for now to grab init data needed to config decoder
            mEncData = ByteBuffer.allocateDirect(mInWidth * mInHeight);
            mAvcSPS = ByteBuffer.allocateDirect(ENZO_SPS_SIZE);
            mAvcPPS = ByteBuffer.allocateDirect(ENZO_PPS_SIZE);
            retEncSize = DecoderView.getEncFrame(mEncData);
            mEncData.clear();
            mEncData.limit(retEncSize);
            mDecoder = MediaCodec.createDecoderByType(type);
            //int colorFormat = random.nextInt(colorFormats.length);
            //Log.d(TAG, "Configuring codec format: " + colorFormatStrings[colorFormat]);
            Log.d(TAG, "Configuring codec format");
            MediaFormat format = MediaFormat.createVideoFormat(type,
                    mInWidth,
                    mInHeight);
            
            //format.setInteger(MediaFormat.KEY_COLOR_FORMAT, colorFormats[colorFormat]);

            mEncData.get(mAvcSPS.array(), 0, ENZO_SPS_SIZE);
            mEncData.get(mAvcPPS.array(), 0, ENZO_PPS_SIZE);
            format.setByteBuffer(CSD_0, mEncData);
            format.setByteBuffer(CSD_1, mEncData);
		//Passing a null to argument 2 tells the decoder to send output to
            //byte buffer. Otherwise pass a valid surface.
            //MediaCodec.CONFIGURE_FLAG_ENCODE
            mDecoder.configure(format, /*outputSurface.getSurface()*/ null, null, 0);
            mDecoder.start();
		Log.i(TAG, OPEN_AVE_DECODER);

            ByteBuffer[] inputBuffers = mDecoder.getInputBuffers();
            ByteBuffer[] outputBuffers = mDecoder.getOutputBuffers();
            ByteBuffer inputBuf = null;
            ByteBuffer outputFrame;

                long currentTime;
                long elapsedTime;
                final String ELAPSED_TIME = "elapsedTime: ";
            
            while (mRunning) {
                
                currentTime = System.currentTimeMillis();
                
                retEncSize = DecoderView.getEncFrame(mEncData);
                if (retEncSize > 0) {
                    inputBufferIndex = mDecoder.dequeueInputBuffer(-1);
                    if (inputBufferIndex >= 0) {
                        inputBuf = inputBuffers[inputBufferIndex];
                        mEncData.clear();
                        mEncData.limit(retEncSize);
                        inputBuf.clear();
                        inputBuf.put(mEncData);
                        mDecoder.queueInputBuffer(inputBufferIndex, 0, retEncSize, info.presentationTimeUs, info.flags);
                        //Log.d(TAG, TIMESTAMP + info.presentationTimeUs);
                    }

                    int decoderStatus = mDecoder.dequeueOutputBuffer(info, 10000);
                    
                        if(decoderStatus >= 0)
                        {                            
                            //Log.d(TAG, new StringBuilder().append(SURFACE_DECODER_GIVEN_BUFFER).append(decoderStatus).append(SIZE).append(info.size).append(END).toString());
                            //if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                            //	Log.d(TAG, OUTPUT_EOS);
                            //}
                            outputFrame = outputBuffers[decoderStatus];
                            outputFrame.position(info.offset);
                            outputFrame.limit(info.offset + info.size);
                            /*
                             if (info.size == 0) {
                             Log.d(TAG, GOT_EMPTY_FRAME);
                             } else {
                             Log.d(TAG, DECODED_CHECKING_FRAME + checkIndex);
                             }
                             if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                             Log.d(TAG, OUTPUT_EOS);
                             }
                             */
                
                        /*
                         if (outputFrame != null) {
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
                        doRender = (info.size != 0);
                    //boolean doRender = true;
                        // As soon as we call releaseOutputBuffer, the buffer will be forwarded
                        // to SurfaceTexture to convert to a texture.  The API doesn't guarantee
                        // that the texture will be available before the call returns, so we
                        // need to wait for the onFrameAvailable callback to fire.
                        mDecoder.releaseOutputBuffer(decoderStatus, doRender);
//					if (doRender) {
//						Log.d(TAG, "awaiting frame " + checkIndex);
//						outputSurface.awaitNewImage();
//						outputSurface.drawImage(false);
//					}

                        if (doRender) {

                            outputFrame.get(frameBytes, info.offset, info.size);
                            for (int index = 0; index < INT_ARRAY_SIZE; index++) {
                                //colors[index] = (int) random.nextInt(Byte.MAX_VALUE);

                                quad[0] = frameBytes[index];
                                quad[1] = frameBytes[index + 1];
                                quad[2] = frameBytes[index + 2];
                                quad[3] = frameBytes[index + 3];
                                //Log.d(TAG, new StringBuilder().append(V).append(quad[0]).append(S).append(quad[2]).append(S).append(quad[3]).append(S).append(quad[4]).toString());
                                //colors[index] = Color.RED;
                                colors[index] = quad[0] << 12 | quad[1] << 8 | quad[2] << 4 | quad[3];
                            }

                            postInvalidate();
                        }

                        while (doRender && mRunning) {
                            Thread.sleep(30);
                            //Log.d(TAG, AWAITING_FRAME + checkIndex);
                        }
                                
			} else if (decoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
				// no output available yet
				Log.d(TAG, NO_OUTPUT_FROM_DECODER_AVAILABLE);
			} else if (decoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
				// The storage associated with the direct ByteBuffer may already be unmapped,
				// so attempting to access data through the old output buffer array could
				// lead to a native crash.
				Log.d(TAG, DECODER_OUTPUT_BUFFERS_CHANGED);
				outputBuffers = mDecoder.getOutputBuffers();
			} else if (decoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
				// this happens before the first frame is returned
				decoderOutputFormat = mDecoder.getOutputFormat();
				Log.d(TAG, DECODER_OUTPUT_FORMAT_CHANGE + decoderOutputFormat);
			} else {
				Log.e(TAG, UNEXPECTED_RESULT + decoderStatus);
			}

                        elapsedTime = System.currentTimeMillis() - currentTime;
                        Log.d(TAG, ELAPSED_TIME + Long.toString(elapsedTime));
                        
                } else {
                    Log.e(TAG, BAD_FRAME_FROM_CAMERA);
                }
            }
            mDecoder.stop();
            mDecoder.release();
            mEncData = null;
            if (outputSurface != null) {
                outputSurface.release();
                outputSurface = null;
            }
            DecoderView.closeCamEnc();

            Log.d(TAG, "Exiting running loop!");
        } catch (Exception e) {
            Log.e(TAG, "Exception", e);
        }
    }

    public DecoderView3(Context context) {
        super(context);
        this.init();
    }

    public DecoderView3(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.init();
    }

    public DecoderView3(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.init();
    }

    private void init() {
        //Log.d(TAG, GET_ENC_FRAME);

        (new Thread(this)).start();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        Log.d(TAG, "onDetachedFromWindow");
        mRunning = false;
    }

    @Override
    public void draw(Canvas canvas) {

        try {
            //Log.d(TAG, DRAWING_FRAME);

            super.draw(canvas);

            if (this.frameBytes == null) {
                return;
            }

//            paint.setTextSize(40);
//            paint.setColor(Color.WHITE);
//            canvas.drawText(this.AWAITING_FRAME, 0, 0, paint);
//            paint.setColor(Color.BLACK);
//            canvas.drawText(this.AWAITING_FRAME, 0, 10, paint);

            canvas.drawBitmap(this.colors, 0, this.mInWidth, 0, 0, mInWidth, mInHeight, false, paint);

            //Log.d(TAG, "end");
            doRender = false;

        } catch (Exception e) {
            Log.e(TAG, "Exception", e);
        }
    }

}
