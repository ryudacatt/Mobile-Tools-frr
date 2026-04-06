package com.skids.idamobile.disassembly

/**
 * Assembly view model containing decoded methods from classes.dex.
 */
data class ApkAssemblyReport(
    val dexEntryName: String,
    val methods: List<AssemblyMethod>,
    val wasTruncated: Boolean
)

/**
 * In-memory representation of a disassembled method.
 */
data class AssemblyMethod(
    val id: String,
    val className: String,
    val methodName: String,
    val descriptor: String,
    val instructionCount: Int,
    val lines: List<InstructionLine>
)

