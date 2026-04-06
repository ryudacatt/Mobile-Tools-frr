package com.skids.idamobile.fileloader

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.skids.idamobile.nativebridge.NativeBridge
import org.json.JSONObject

data class ApkInspectionResult(
    val displayName: String,
    val report: ApkNativeReport
)

class ApkInspectorRepository {
    fun inspect(context: Context, uri: Uri): Result<ApkInspectionResult> = runCatching {
        val displayName = resolveDisplayName(context, uri)
        val rawReport = context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
            NativeBridge.inspectApk(pfd.fd, pfd.statSize)
        } ?: error("Could not open selected APK.")

        val report = parseNativeReport(rawReport)
        ApkInspectionResult(displayName = displayName, report = report)
    }

    private fun resolveDisplayName(context: Context, uri: Uri): String {
        val fallback = uri.lastPathSegment ?: "selected.apk"
        context.contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null
        )?.use { cursor ->
            val column = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (column >= 0 && cursor.moveToFirst()) {
                val value = cursor.getString(column)
                if (!value.isNullOrBlank()) {
                    return value
                }
            }
        }
        return fallback
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

