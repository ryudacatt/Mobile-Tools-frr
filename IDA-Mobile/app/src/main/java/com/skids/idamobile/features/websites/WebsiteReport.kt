package com.skids.idamobile.features.websites

data class WebsiteReport(
    val normalizedUrl: String,
    val statusCode: Int,
    val contentType: String,
    val sampledBytes: Int,
    val title: String?,
    val scriptTagCount: Int,
    val sha256: String
)

