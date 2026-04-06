package com.skids.idamobile.strings

/**
 * One cross-reference from a method instruction to a string constant
 */
data class StringXref(
    val methodId: String,
    val className: String,
    val methodName: String,
    val descriptor: String,
    val instructionAddress: Long,
    val opcode: String
)

