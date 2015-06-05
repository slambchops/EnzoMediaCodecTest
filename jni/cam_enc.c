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

#define WIDTH			1280
#define HEIGHT			720
#define FPS			15

struct encoderInstance *avcEnc;
struct mediaBuffer *avcData, *fileSrc;

static char *avcHeaderBuf;
static int avcHeaderSize;

static int readFrameCount = 0;

jstring Java_com_example_cameracodectest_DecoderView_initCamEnc(JNIEnv* env, jobject javaThis)
{
	int ret = 0;

	/* Initialize all the structures we will be using */
	avcEnc = (struct encoderInstance *)calloc(1, sizeof(struct encoderInstance));

	avcData = (struct mediaBuffer *)calloc(1, sizeof(struct mediaBuffer));
	fileSrc = (struct mediaBuffer *)calloc(1, sizeof(struct mediaBuffer));

	/* Set properties for H264 AVC encoder */
	avcEnc->type = H264AVC;
	avcEnc->width = WIDTH;
	avcEnc->height = HEIGHT;
	avcEnc->fps = FPS;
	avcEnc->colorSpace = NV12;

	/* Initialize the media buffers with proper parameters */
	/* This is for reading avc data from a file */
	fileSrc->dataType = RAW_VIDEO;
	fileSrc->dataSource = FILE_SRC;
	fileSrc->colorSpace = NV12;

	/* Init the VPU. This must be done before a codec can be used.
	   If this fails, we need to bail. */
	ret = vpuInit();
	if (ret < 0)
		return -1;

	/* Open up the files that will be used to read and store data */
	fileSrc->fd = open("/data/local/tmp/OUT.YUV", O_RDONLY, 0);
	LOGI("fileSrc %d\n", fileSrc->fd);

	/* Use this variable to determine if all the components were
	   initialized correctly so that transcode loop can be started */
	ret = 0;

	if (encoderInit(avcEnc, avcData) < 0)
		ret = -1;
	avcHeaderSize = avcData->bufOutSize;
	avcHeaderBuf = malloc(avcHeaderSize);
	memcpy(avcHeaderBuf, avcData->vBufOut, avcData->bufOutSize);

	return (*env)->NewStringUTF(env, "THIS IS THE STUFF!");
}

jint Java_com_example_cameracodectest_DecoderView_closeCamEnc(JNIEnv* env, jobject javaThis)
{
	encoderDeinit(avcEnc);
	close(fileSrc->fd);
	free(avcEnc);
	free(avcData);
	free(fileSrc);
	free(avcHeaderBuf);
	vpuDeinit();
	return 0;
}

jint Java_com_example_cameracodectest_DecoderView_getEncFrame(JNIEnv* env, jobject javaThis, jobject buf)
{
	int ret;

	ret = encoderEncodeFrame(avcEnc, fileSrc, avcData);
	if (ret < 0) {
		LOGE("Frame could not be encoded\n");
		return -1;
	}

	readFrameCount++;
	if (readFrameCount == 30) {
		readFrameCount = 0;
		close(fileSrc->fd);
		fileSrc->fd = open("/data/local/tmp/OUT.YUV", O_RDONLY, 0);
	}

	jbyte *BUFF = (*env)->GetDirectBufferAddress(env, buf);
	memcpy(BUFF, avcHeaderBuf, avcHeaderSize);
	memcpy(BUFF + avcHeaderSize, avcData->vBufOut, avcData->bufOutSize);
	//LOGI("Encoded data size %d\n", avcData->bufOutSize + avcHeaderSize);

	return avcData->bufOutSize + avcHeaderSize;
}
