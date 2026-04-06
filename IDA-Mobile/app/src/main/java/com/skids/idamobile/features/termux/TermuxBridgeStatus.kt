package com.skids.idamobile.features.termux

/**
 * Runtime status of the Termux bridge integration.
 */
data class TermuxBridgeStatus(
    val installed: Boolean,
    val packageName: String = "com.termux",
    val detail: String
)
