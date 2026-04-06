package com.skids.idamobile.fileloader

/**
 * Static debugging context derived from APK manifest and package metadata.
 */
data class ApkDebugReport(
    val displayName: String,
    val packageName: String,
    val versionName: String?,
    val versionCode: Long,
    val minSdkVersion: Int?,
    val targetSdkVersion: Int?,
    val isDebuggable: Boolean,
    val permissions: List<String>,
    val activities: List<String>,
    val services: List<String>,
    val receivers: List<String>,
    val providers: List<String>,
    val nativeLibraries: List<String>,
    val certificateSha256: List<String>
)

