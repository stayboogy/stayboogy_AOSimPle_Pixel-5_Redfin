LOCAL_PATH := $(call my-dir)

ifneq ($(filter redfin, $(TARGET_DEVICE)),)
include $(CLEAR_VARS)
LOCAL_MODULE_SUFFIX := $(COMMON_ANDROID_PACKAGE_SUFFIX)
LOCAL_MODULE := Etar
LOCAL_MODULE_TAGS := optional
LOCAL_BUILT_MODULE_STEM := package.apk
LOCAL_SYSTEM_EXT_MODULE := true
LOCAL_MODULE_CLASS := APPS
LOCAL_SRC_FILES := $(LOCAL_MODULE).apk
LOCAL_CERTIFICATE := platform
# Disable dexpreopt and <uses-library> check because the APK depends on
# libraries that are not present as modules in the build system.
LOCAL_ENFORCE_USES_LIBRARIES := false
LOCAL_DEX_PREOPT := false
include $(BUILD_PREBUILT)
endif
