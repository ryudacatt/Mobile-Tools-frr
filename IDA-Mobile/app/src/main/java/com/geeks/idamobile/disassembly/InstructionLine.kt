package com.geeks.idamobile.disassembly

data class InstructionLine(
    val address: Long,
    val mnemonic: String,
    val operands: String
)

