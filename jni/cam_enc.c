#include <jni.h>

#include <android/log.h>
#include <string.h>
#include <stddef.h>

#include <fcntl.h>
#include <stdio.h>
#include <sys/stat.h>
#include <sys/time.h>
#include <sys/types.h>
#include <time.h>
#include <unistd.h>

#include "enzo_codec.h"
#include "enzo_utils.h"

#include "g2d.h"

#define LOG_TAG "CameraCodecTestJNI"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)

#define FPS					15
#define BITRATE				0 //kbps
#define GOPSIZE				20
#define WIDTH				320
#define HEIGHT				240

/* These are the control structures for the encoder and camera */
struct decoderInstance *mjpgDec;
struct encoderInstance *avcEnc;
struct cameraInstance *usbCam;

/* Control structures for the media buffers, which are used to
   pass data between sources (like a camera or file), and pass them
   to other components (like the VPU, a file, or a buffer) */
struct mediaBuffer *avcData, *camData, *yuvData;

static char *avcHeaderBuf;
static int avcHeaderSize;

jstring Java_com_example_cameracodectest_DecoderView_initCamEnc(JNIEnv* env, jobject javaThis)
{
	int ret = 0;

	/* Initialize all the structures we will be using */
	mjpgDec = (struct decoderInstance *)calloc(1, sizeof(struct decoderInstance));
	avcEnc = (struct encoderInstance *)calloc(1, sizeof(struct encoderInstance));
	usbCam = (struct cameraInstance  *)calloc(1, sizeof(struct cameraInstance));

	avcData = (struct mediaBuffer *)calloc(1, sizeof(struct mediaBuffer));
	camData = (struct mediaBuffer *)calloc(1, sizeof(struct mediaBuffer));
	yuvData = (struct mediaBuffer *)calloc(1, sizeof(struct mediaBuffer));

	/* Set properties for MJPG decoder */
	mjpgDec->type = MJPEG;
	strcpy(mjpgDec->decoderName,"mjpg dec #1");

	/* Set properties for H264 AVC encoder */
	avcEnc->type = H264AVC;
	avcEnc->width = WIDTH;
	avcEnc->height = HEIGHT;
	avcEnc->fps = FPS;
	avcEnc->bitRate = BITRATE;
	avcEnc->gopSize = GOPSIZE;
	avcEnc->colorSpace = YUV422P;
	strcpy(avcEnc->encoderName,"h264 enc #1");

	/* Set properties for USB camera */
	usbCam->type = MJPEG;
	usbCam->width = WIDTH;
	usbCam->height = HEIGHT;
	usbCam->fps = FPS;
	strcpy(usbCam->deviceName,"/dev/video0");

	/* Init the VPU. This must be done before a codec can be used.
	   If this fails, we need to bail. */
	ret = vpuInit();
	if (ret < 0)
		return -1;

	/* Use this variable to determine if all the components were
	   initialized correctly so that transcode loop can be started */
	ret = 0;

	if (cameraInit(usbCam) < 0)
		ret = -1;
	/* In order to init mjpg decoder, it must be supplied with bitstream
	   parse */
	ret = cameraGetFrame(usbCam, camData);
	if (ret < 0) {
		err_msg("Could not get camera frame\n");
		ret = -1;
	}

	if (decoderInit(mjpgDec, camData) < 0) {
		err_msg("Could not init MJPG decoder\n");
		ret = -1;
	}

	avcEnc->width = mjpgDec->width;
	avcEnc->height = mjpgDec->height;

	if (encoderInit(avcEnc, avcData) < 0)
		ret = -1;
	avcHeaderSize = avcData->bufOutSize;
	avcHeaderBuf = malloc(avcHeaderSize);
	memcpy(avcHeaderBuf, avcData->vBufOut, avcData->bufOutSize);

	return (*env)->NewStringUTF(env, "THIS IS THE STUFF!");
}

jint Java_com_example_cameracodectest_DecoderView_closeCamEnc(JNIEnv* env, jobject javaThis)
{
	/* Clean up all the stuff we initialized */
	cameraDeinit(usbCam);
	decoderDeinit(mjpgDec);
	encoderDeinit(avcEnc);

	if (usbCam)
		free(usbCam);
	if (mjpgDec)
		free(mjpgDec);
	if (avcEnc)
		free(avcEnc);

	if (avcData) {
		free(avcData);
	}
	if (camData) {
		free(camData);
	}
	if (yuvData) {
		free(yuvData);
	}
	vpuDeinit();
	return 0;
}

jint Java_com_example_cameracodectest_DecoderView_getEncFrame(JNIEnv* env, jobject javaThis, jobject buf)
{
	int ret;

	ret = cameraGetFrame(usbCam, camData);
	if (ret < 0) {
		warn_msg("Could not get camera frame\n");
		return -1;
	}

	ret = decoderDecodeFrame(mjpgDec, camData, yuvData);
	if (ret < 0) {
		err_msg("Could not decode MJPG frame\n");
		return -1;
	}

	ret = encoderEncodeFrame(avcEnc, yuvData, avcData);
	if (ret < 0) {
		LOGE("Frame could not be encoded\n");
		return -1;
	}

	jbyte *BUFF = (*env)->GetDirectBufferAddress(env, buf);
	memcpy(BUFF, avcHeaderBuf, avcHeaderSize);
	memcpy(BUFF + avcHeaderSize, avcData->vBufOut, avcData->bufOutSize);
	//LOGI("Encoded data size %d\n", avcData->bufOutSize + avcHeaderSize);

	return avcData->bufOutSize + avcHeaderSize;
}
