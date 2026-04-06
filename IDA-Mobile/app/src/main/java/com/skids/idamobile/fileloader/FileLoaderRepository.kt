package com.skids.idamobile.fileloader

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File

/**
 * Materializes a SAF-selected APK into local cache so all analyzers can read it via file APIs.
 */
class FileLoaderRepository {
    fun materializeApk(context: Context, uri: Uri): Result<MaterializedApk> = runCatching {
        val displayName = resolveDisplayName(context, uri)
        val workspaceDir = File(context.cacheDir, "apk_workspace").apply { mkdirs() }
        val safeName = sanitizeFileName(displayName)
        val outputFile = File(workspaceDir, "${System.currentTimeMillis()}_$safeName")

        context.contentResolver.openInputStream(uri)?.use { input ->
            outputFile.outputStream().use { output ->
                input.copyTo(output)
            }
        } ?: error("Could not open selected APK stream.")

        MaterializedApk(
            displayName = displayName,
            apkFile = outputFile
        )
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

    private fun sanitizeFileName(name: String): String {
        val candidate = name.replace(Regex("[^A-Za-z0-9._-]"), "_")
        return if (candidate.lowercase().endsWith(".apk")) candidate else "$candidate.apk"
    }
}
