package com.skids.idamobile.fileloader

import java.io.File

/**
 * Canonical local representation of a selected APK for repeatable analysis steps.
 */
data class MaterializedApk(
    val displayName: String,
    val apkFile: File
)

