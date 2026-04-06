package com.skids.idamobile.fileloader

import android.os.ParcelFileDescriptor
import com.skids.idamobile.nativebridge.NativeBridge
import java.io.File
import org.json.JSONObject

data class ApkInspectionResult(
    val displayName: String,
    val report: ApkNativeReport
)

/**
 * Bridges cached APK files into the native mmap-based inspector through JNI.
 */
class ApkInspectorRepository {
    fun inspect(apkFile: File, displayName: String): Result<ApkInspectionResult> = runCatching {
        val rawReport = ParcelFileDescriptor.open(apkFile, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
            NativeBridge.inspectApk(pfd.fd, apkFile.length())
        }

        val report = parseNativeReport(rawReport)
        ApkInspectionResult(displayName = displayName, report = report)
    }

    private fun parseNativeReport(rawJson: String): ApkNativeReport {
        val json = JSONObject(rawJson)
        if (!json.optBoolean("ok", false)) {
            throw IllegalStateException(json.optString("error", "Native APK inspection failed."))
        }

        return ApkNativeReport(
            fileSize = json.optLong("fileSize", 0L),
            zipEntries = json.optInt("entries", 0),
            dexFiles = json.optInt("dexFiles", 0),
            hasAndroidManifest = json.optBoolean("hasManifest", false)
        )
    }
}
