package com.skids.idamobile.decompiler

/**
 * Function metadata emitted by `aflj` in radare2.
 */
data class Radare2Function(
    val name: String,
    val offset: Long,
    val size: Long
)
