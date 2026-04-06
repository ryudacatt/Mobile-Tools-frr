package com.skids.idamobile.fileloader

data class ApkNativeReport(
    val fileSize: Long,
    val zipEntries: Int,
    val dexFiles: Int,
    val hasAndroidManifest: Boolean
)

