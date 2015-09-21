package com.example.cameracodectest;

import java.nio.ByteBuffer;

import android.annotation.SuppressLint;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecInfo.CodecCapabilities;
import android.media.MediaCodecInfo.CodecProfileLevel;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;

@SuppressLint("NewApi")
public class Decoder implements Runnable {

    private static final String TAG = "DecoderView";

    private Surface surface;
    private final Invalidator invalidator;

    public final int[] colors = new int[this.mInHeight * this.mInWidth];
    private final int INT_ARRAY_SIZE = this.mInHeight * this.mInWidth;
    public final byte[] frameBytes1 = new byte[this.mInHeight * this.mInWidth * 4];
    //public final byte[] frameBytes2 = new byte[this.mInHeight * this.mInWidth * 4];
    //public final byte[] frameBytes3 = new byte[this.mInHeight * this.mInWidth * 4];
    public final byte[] frameBytes4 = new byte[this.mInHeight * this.mInWidth * 4];
    public byte[] frameBytes = frameBytes4;
    private final int[] quad = new int[4];
    
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

    //private final String V = "values: ";
    //private final String S = " ";    
    //{Color.RED & 0xFF000000,Color.RED & 0x00FF0000,Color.RED & 0x0000FF00,Color.RED & 0x000000FF};

    private final int[] colorFormats
            = {
                MediaCodecInfo.CodecCapabilities.COLOR_Format12bitRGB444,
                MediaCodecInfo.CodecCapabilities.COLOR_Format16bitARGB1555,
                MediaCodecInfo.CodecCapabilities.COLOR_Format16bitARGB4444,
                MediaCodecInfo.CodecCapabilities.COLOR_Format16bitBGR565,
                MediaCodecInfo.CodecCapabilities.COLOR_Format16bitRGB565,
                MediaCodecInfo.CodecCapabilities.COLOR_Format18BitBGR666,
                MediaCodecInfo.CodecCapabilities.COLOR_Format18bitARGB1665,
                MediaCodecInfo.CodecCapabilities.COLOR_Format18bitRGB666,
                MediaCodecInfo.CodecCapabilities.COLOR_Format19bitARGB1666,
                MediaCodecInfo.CodecCapabilities.COLOR_Format24BitABGR6666,
                MediaCodecInfo.CodecCapabilities.COLOR_Format24BitARGB6666,
                MediaCodecInfo.CodecCapabilities.COLOR_Format24bitARGB1887,
                MediaCodecInfo.CodecCapabilities.COLOR_Format24bitBGR888,
                MediaCodecInfo.CodecCapabilities.COLOR_Format24bitRGB888,
                MediaCodecInfo.CodecCapabilities.COLOR_Format25bitARGB1888,
                MediaCodecInfo.CodecCapabilities.COLOR_Format32bitARGB8888,
                MediaCodecInfo.CodecCapabilities.COLOR_Format8bitRGB332,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV422PackedPlanar,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV422PackedSemiPlanar,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV422Planar,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV422SemiPlanar,};

    private final String[] colorFormatStrings
            = {
                "MediaCodecInfo.CodecCapabilities.COLOR_Format12bitRGB444",
                "MediaCodecInfo.CodecCapabilities.COLOR_Format16bitARGB1555",
                "MediaCodecInfo.CodecCapabilities.COLOR_Format16bitARGB4444",
                "MediaCodecInfo.CodecCapabilities.COLOR_Format16bitBGR565",
                "MediaCodecInfo.CodecCapabilities.COLOR_Format16bitRGB565",
                "MediaCodecInfo.CodecCapabilities.COLOR_Format18BitBGR666",
                "MediaCodecInfo.CodecCapabilities.COLOR_Format18bitARGB1665",
                "MediaCodecInfo.CodecCapabilities.COLOR_Format18bitRGB666",
                "MediaCodecInfo.CodecCapabilities.COLOR_Format19bitARGB1666",
                "MediaCodecInfo.CodecCapabilities.COLOR_Format24BitABGR6666",
                "MediaCodecInfo.CodecCapabilities.COLOR_Format24BitARGB6666",
                "MediaCodecInfo.CodecCapabilities.COLOR_Format24bitARGB1887",
                "MediaCodecInfo.CodecCapabilities.COLOR_Format24bitBGR888",
                "MediaCodecInfo.CodecCapabilities.COLOR_Format24bitRGB888",
                "MediaCodecInfo.CodecCapabilities.COLOR_Format25bitARGB1888",
                "MediaCodecInfo.CodecCapabilities.COLOR_Format32bitARGB8888",
                "MediaCodecInfo.CodecCapabilities.COLOR_Format8bitRGB332",
                "MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV422PackedPlanar",
                "MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV422PackedSemiPlanar",
                "MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV422Planar",
                "MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV422SemiPlanar",};
    
    private final String type = "video/avc";
    private final String mYuvOutDir = "/data/local/tmp/TEST.YUV";
    public final int mInWidth = 1280;
    public final int mInHeight = 720;

    private final int ENZO_SPS_SIZE = 13;
    private final int ENZO_PPS_SIZE = 9;
    
    private SurfaceHolder mHolder;
    private MediaCodec mDecoder;
    private ByteBuffer mEncData;
    private ByteBuffer mAvcSPS;
    private ByteBuffer mAvcPPS;
    private CodecOutputSurface outputSurface = null;
    
    private int mFrameCount = 0;

    //private final Random random = new Random();
    public boolean mRunning = true;
    public boolean doRender;

    int retEncSize = 0;
    int inputBufferIndex = 0;
    int checkIndex = 0;

    MediaFormat decoderOutputFormat = null;

    public Decoder(Invalidator invalidator) {
        this.invalidator = invalidator;
        
        Log.d(TAG, this.getCodecInfo());
        
        /*
        for (int index = 0; index < INT_ARRAY_SIZE; index++) {
            //0xFFFF0000
            frameBytes[index] = -128;
            frameBytes[index + 1] = -128;
            frameBytes[index + 2] = 0;
            frameBytes[index + 3] = 0;
            //Log.d(TAG, new StringBuilder().append(V).append(quad[0]).append(S).append(quad[2]).append(S).append(quad[3]).append(S).append(quad[4]).toString());
        }
        */
    }

    /*
    @Override
    public void run() {
        try {

            MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
    
            ByteBuffer[] inputBuffers = mDecoder.getInputBuffers();
            ByteBuffer[] outputBuffers = mDecoder.getOutputBuffers();
            ByteBuffer inputBuf = null;
            ByteBuffer outputFrame;

            long currentTime;
            long elapsedTime;
            final String ELAPSED_TIME = "elapsedTime: ";

            long totalFrames = 0;
            currentTime = System.currentTimeMillis();
            
            while (mRunning) {

            }
            
            elapsedTime = System.currentTimeMillis() - currentTime;
            Log.d(TAG, ELAPSED_TIME + Long.toString(elapsedTime));
            Log.d(TAG, "Total Frames: " + Long.toString(totalFrames));
            Log.d(TAG, "Frame Rate: " + Long.toString(totalFrames / (elapsedTime / 1000)));

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
    */

	@Override
	public void run() {
            try
            { 
		MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();

		ByteBuffer[] inputBuffers = mDecoder.getInputBuffers();
		ByteBuffer[] outputBuffers = mDecoder.getOutputBuffers();
		ByteBuffer inputBuf = null;
                ByteBuffer outputFrame = null;

		boolean isInput = true;
		boolean first = false;
		long startWhen = 0;
                
		Log.d(TAG, "Started running loop!");

            long currentTime;
            long elapsedTime;
            final String ELAPSED_TIME = "elapsedTime: ";

            long totalFrames = 0;
            currentTime = System.currentTimeMillis();
                
		while(mRunning) {

                    retEncSize = DecoderView.getEncFrame(mEncData);
                    if (retEncSize > 0) {
                    
                    /*
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

                    if (decoderStatus >= 0) {
                            //Log.d(TAG, new StringBuilder().append(SURFACE_DECODER_GIVEN_BUFFER).append(decoderStatus).append(SIZE).append(info.size).append(END).toString());
                        //if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        //	Log.d(TAG, OUTPUT_EOS);
                        //}
                        outputFrame = outputBuffers[decoderStatus];
                        outputFrame.position(info.offset);
                        outputFrame.limit(info.offset + info.size);

//                         if (info.size == 0) {
//                         Log.d(TAG, GOT_EMPTY_FRAME);
//                         } else {
//                         Log.d(TAG, DECODED_CHECKING_FRAME + checkIndex);
//                         }
//                         if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
//                         Log.d(TAG, OUTPUT_EOS);
//                         }

//                         if (outputFrame != null) {
//                         //write to file stuff
//                         FileChannel channel = null;
//                         int bytesWrittenToFile = 0;
//                         try {
//                         channel = new FileOutputStream(mYuvOutDir, true).getChannel();
//                         } catch (FileNotFoundException e) {
//                         e.printStackTrace();
//                         }
//                         try {
//                         bytesWrittenToFile = channel.write(outputFrame);
//                         Log.i(TAG, "Bytes written to file: " + bytesWrittenToFile);
//                         } catch (IOException e) {
//                         e.printStackTrace();
//                         }
//                         } else {
//                         Log.e(TAG, "outputFrame is null!");
//                         }


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

                            frameBytes = frameBytes1;
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
                            
                            this.invalidator.post();
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
                    
                    */

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
						//Log.d(TAG, "info.presentationTimeUs : " + (info.presentationTimeUs / 1000) + " playTime: " + (System.currentTimeMillis() - startWhen) + " sleepTime : " + sleepTime);

						if (sleepTime > 0)
							Thread.sleep(sleepTime);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} 

                        if(surface == null)
                        {
                            //09-21 16:50:34.909: E/AndroidRuntime(3835): java.lang.IllegalArgumentException: Bad limit (capacity 1382400): 1413120
                            
                        outputFrame = outputBuffers[outIndex];
                        outputFrame.position(info.offset);
                        outputFrame.limit(info.offset + info.size);
                        }                                        

    //true = Surface init
	        	mDecoder.releaseOutputBuffer(outIndex, true);
                                        
                        if(surface == null)
                        {
                        doRender = (info.size != 0);
                        
                        if (doRender) {

                            frameBytes = frameBytes1;
                            outputFrame.get(frameBytes, info.offset, info.size);

//                            for (int index = 0; index < INT_ARRAY_SIZE; index++) {
//                                //colors[index] = (int) random.nextInt(Byte.MAX_VALUE);
//
//                                quad[0] = frameBytes[index];
//                                quad[1] = frameBytes[index + 1];
//                                quad[2] = frameBytes[index + 2];
//                                quad[3] = frameBytes[index + 3];
//                                //Log.d(TAG, new StringBuilder().append(V).append(quad[0]).append(S).append(quad[2]).append(S).append(quad[3]).append(S).append(quad[4]).toString());
//                                //colors[index] = Color.RED;
//                                colors[index] = quad[0] << 12 | quad[1] << 8 | quad[2] << 4 | quad[3];
//                            }
                            this.invalidator.post();
                        }

                        //while (doRender && mRunning) {
                            //Thread.sleep(30);
                            //Log.d(TAG, AWAITING_FRAME + checkIndex);
                        //}
                        }
                                        
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
                        
                        totalFrames++;
		}
                
            elapsedTime = System.currentTimeMillis() - currentTime;
            Log.d(TAG, ELAPSED_TIME + Long.toString(elapsedTime));
            Log.d(TAG, "Total Frames: " + Long.toString(totalFrames));
            Log.d(TAG, "Frame Rate: " + Long.toString(totalFrames / (elapsedTime / 1000)));
                
		mDecoder.stop();
		mDecoder.release();
		mEncData = null;
		DecoderView.closeCamEnc();

                Log.d(TAG, "Exiting running loop!");

            } catch (Exception e) {
                Log.e(TAG, "Exception", e);
            }
 
	}
    
    public boolean init(Surface surface) {
        this.surface = surface;
        outputSurface = new CodecOutputSurface(mInWidth, mInHeight);
        
        // this is where we call the native code
        String hello = DecoderView.initCamEnc();
        Log.i(TAG, hello);
        Log.d(TAG, STARTED_RUNNING_LOOP);

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
        
        format.setByteBuffer(CSD_0, mAvcSPS);
        format.setByteBuffer(CSD_1, mAvcPPS);
        
        //Passing a null to argument 2 tells the decoder to send output to
        //byte buffer. Otherwise pass a valid surface.
        //MediaCodec.CONFIGURE_FLAG_ENCODE
        mDecoder.configure(format, surface, null, 0);
        mDecoder.start();
        Log.i(TAG, OPEN_AVE_DECODER);
        
        return true;
    }
    
    public void start()
    {
        (new Thread(this)).start();
    }

    public void close()
    {
        this.mRunning = false;
    }

    /*
    private static MediaCodecInfo selectCodec(String mimeType) {

        int numCodecs = MediaCodecList.getCodecCount();
        for (int i = 0; i < numCodecs; i++) {
            MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(i);

            if (!codecInfo.isEncoder()) {
                continue;
            }

            String[] types = codecInfo.getSupportedTypes();
            for (int j = 0; j < types.length; j++) {
                if (types[j].equalsIgnoreCase(mimeType)) {
                    return codecInfo;
                }
            }
        }
        return null;
    }
    */

    private String getCodecInfo() {

        final String CODEC_INFO = "CodecInfo: ";
        final String NAME = "Name: ";
        final String IS_ENCODER = " isEncoder: ";
        final String COMMA_SPACE = ", ";
        final String TYPES = " Types: ";
        final String COLOR_FORMATS = " Color Formats: ";
        final String DASH = "-";
        
        final StringBuilder stringBuilder = new StringBuilder();
        
        stringBuilder.append(CODEC_INFO);
        
        int numCodecs = MediaCodecList.getCodecCount();
        String[] types = null;
        CodecCapabilities codecCapabilities = null;
        CodecProfileLevel[] codecProfileLevelArray;
        String type;
        int[] colorFormats;

        for (int i = 0; i < numCodecs; i++) {
            MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(i);

            stringBuilder.append(NAME);
            stringBuilder.append(codecInfo.getName());

            stringBuilder.append(IS_ENCODER);
            stringBuilder.append(codecInfo.isEncoder());

            stringBuilder.append(TYPES);
            types = codecInfo.getSupportedTypes();
            for (int j = 0; j < types.length; j++) {
                type = types[j];
                stringBuilder.append(type);

                codecCapabilities = codecInfo.getCapabilitiesForType(type);
                colorFormats = codecCapabilities.colorFormats;
                stringBuilder.append(COLOR_FORMATS);
                for(int index = 0; index < colorFormats.length; index++)
                {
                    stringBuilder.append(colorFormats[index]);
                    stringBuilder.append(COMMA_SPACE);
                }
                codecProfileLevelArray = codecCapabilities.profileLevels;
                for(int index = 0; index < codecProfileLevelArray.length; index++)
                {
                    stringBuilder.append(codecProfileLevelArray[index].level);
                    stringBuilder.append(DASH);
                    stringBuilder.append(codecProfileLevelArray[index].profile);
                    stringBuilder.append(COMMA_SPACE);
                }
                
                stringBuilder.append(COMMA_SPACE);
            }
        }
        return stringBuilder.toString();
    }
}
