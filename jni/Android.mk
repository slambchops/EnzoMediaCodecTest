LOCAL_PATH := $(call my-dir)
 
include $(CLEAR_VARS)

LOCAL_C_INCLUDES += $(LOCAL_PATH) \
	external/linux-lib/vpu \
	device/fsl-proprietary/include \
	imx6_codec_lib/lib

LOCAL_LDLIBS := -llog

LOCAL_SHARED_LIBRARIES := libvpu libg2d libenzocodec liblog libbinder

# Here we give our module name and source file(s)
LOCAL_MODULE    := libjni_cam_enc
LOCAL_SRC_FILES := cam_enc.c

include $(BUILD_SHARED_LIBRARY)
