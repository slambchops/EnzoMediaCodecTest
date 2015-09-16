LOCAL_PATH := $(call my-dir)
 
include $(CLEAR_VARS)
LOCAL_MODULE    := libg2d
LOCAL_SRC_FILES := $(LOCAL_PATH)/enzo-libs/libg2d.so
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/enzo-libs/g2d
include $(PREBUILT_SHARED_LIBRARY)

LOCAL_MODULE    := libvpu
LOCAL_SRC_FILES := enzo-libs/libvpu.so
LOCAL_EXPORT_C_INCLUDES := enzo-libs/vpu
include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE    := libenzocodec
LOCAL_SRC_FILES := $(LOCAL_PATH)/enzo-libs/libenzocodec.so
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/enzo-libs/enzo_codec

include $(PREBUILT_SHARED_LIBRARY)
include $(CLEAR_VARS)


LOCAL_C_INCLUDES += $(LOCAL_PATH) \
	$(LOCAL_PATH)/enzo-libs/g2d \
	$(LOCAL_PATH)/enzo-libs/vpu \
	$(LOCAL_PATH)/enzo-libs/enzo_codec

LOCAL_LDLIBS := -llog

LOCAL_SHARED_LIBRARIES := libvpu libg2d libenzocodec liblog libbinder libjnigraphics

# Here we give our module name and source file(s)
LOCAL_MODULE    := libjni_cam_enc
LOCAL_SRC_FILES := cam_enc.c

include $(BUILD_SHARED_LIBRARY)
