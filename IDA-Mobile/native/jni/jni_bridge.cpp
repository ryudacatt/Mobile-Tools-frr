#include <jni.h>
#include <android/log.h>

#include "core/core_version.h"

namespace {
constexpr const char* kLogTag = "IDA-Mobile-JNI";
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_geeks_idamobile_nativebridge_NativeBridge_getCoreVersion(JNIEnv* env, jobject /* this */) {
    const auto version = ida_mobile::core::GetCoreVersion();
    __android_log_print(ANDROID_LOG_INFO, kLogTag, "Native bridge requested core version: %s", version.data());
    return env->NewStringUTF(version.data());
}

