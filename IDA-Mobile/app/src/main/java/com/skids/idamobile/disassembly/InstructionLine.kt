package com.skids.idamobile.disassembly

/**
 * One decoded instruction line for UI rendering.
 */
data class InstructionLine(
    val address: Long,
    val mnemonic: String,
    val operands: String
)
