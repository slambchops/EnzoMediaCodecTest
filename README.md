WHAT IT DOES:

Two things:

1. Simple camera loopback using old android camera API.
   I was going to encode this data, but haven't gotten to it yet.
   For now it just does a camera loopback.
2. In JNI, use imx6_codec_lib to encode (in an infinite loop) a raw 720p NV12
   video file, and then pass each finished frame up to Android app via JNI.
3. Use android MediaCodec to decode the encoded data created in step 2.
4. Provide the decoded data to a ByteBuffer for use. Right now I do nothing
   with it except monitor its characteristics in logcat. I verified the data
   was good by saving the decoded data to file and playing back with ffmpeg on
   my pc with the following command:

   ffplay -f rawvideo -pixel_format yuv420p -video_size 1280x736 test.yuv

   As you can see, the color space of the raw video produced my mediacodec is
   what ffmpeg calls yuv420p
	


HOW TO BUILD:

1. clone repo into freescale android source
2. setup build env for imx6 dual/quad evm
3. go into EnzoMediaCodecTest directory and build with mm.
   make sure imx6_codec_lib has been built beforehand and is included on
   filesystem. The JNI portion of the app uses imx6_codec_lib to encode
   a raw yuv file.
4. copy OUT.YUV to /data/local/tmp/OUT.YUV on Enzo
   This is the 720p NV12 raw video file that is read and encoded and then
   passed up through to app via JNI
5. touch (or somehow create) /data/local/tmp/TEST.YUV on Enzo
   this is where the raw YUV data from decoder can be stored if you uncomment
   out file saving code on line 124 of DecoderView.java
