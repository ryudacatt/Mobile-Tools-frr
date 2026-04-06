#include <jni.h>
#include <android/log.h>
#include <cstdint>
#include <string>
#include <capstone/capstone.h>

#include "core/core_version.h"
#include "loader/apk_inspector.h"

namespace {
constexpr const char* kLogTag = "IDA-Mobile-JNI";
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_skids_idamobile_nativebridge_NativeBridge_getCoreVersion(JNIEnv* env, jobject /* this */) {
    const auto version = ida_mobile::core::GetCoreVersion();
    __android_log_print(ANDROID_LOG_INFO, kLogTag, "Native bridge requested core version: %s", version.data());
    return env->NewStringUTF(version.data());
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_skids_idamobile_nativebridge_NativeBridge_getCapstoneVersion(JNIEnv* env, jobject /* this */) {
    int major = 0;
    int minor = 0;
    cs_version(&major, &minor);
    const std::string version = "Capstone " + std::to_string(major) + "." + std::to_string(minor);
    __android_log_print(ANDROID_LOG_INFO, kLogTag, "Capstone version: %s", version.c_str());
    return env->NewStringUTF(version.c_str());
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_skids_idamobile_nativebridge_NativeBridge_inspectApk(
    JNIEnv* env,
    jobject /* this */,
    jint fd,
    jlong declared_size
) {
    const auto report = ida_mobile::loader::InspectApkFromFd(static_cast<int>(fd), static_cast<std::int64_t>(declared_size));
    return env->NewStringUTF(report.c_str());
}
