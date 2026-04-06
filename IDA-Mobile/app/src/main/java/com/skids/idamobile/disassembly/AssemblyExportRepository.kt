package com.skids.idamobile.disassembly

import java.io.File

/**
 * Exports selected disassembly views into text files so analysis can continue off-device.
 */
class AssemblyExportRepository {
    fun exportMethodReport(
        method: AssemblyMethod,
        outputDirectory: File
    ): Result<File> = runCatching {
        outputDirectory.mkdirs()

        val safeClass = sanitize(method.className)
        val safeMethod = sanitize(method.methodName)
        val outputFile = File(
            outputDirectory,
            "${safeClass}_${safeMethod}_${System.currentTimeMillis()}.smali.txt"
        )

        outputFile.bufferedWriter().use { writer ->
            writer.appendLine("; Exported by IDA-Mobile")
            writer.appendLine("; Method ID: ${method.id}")
            writer.appendLine("; Descriptor: ${method.descriptor}")
            writer.appendLine("; Instruction count: ${method.instructionCount}")
            writer.appendLine()

            for (line in method.lines) {
                val address = "0x${line.address.toString(16).padStart(8, '0')}"
                writer.appendLine("$address  ${line.mnemonic} ${line.operands}".trimEnd())
            }
        }

        outputFile
    }

    private fun sanitize(value: String): String {
        val cleaned = value.replace(Regex("[^A-Za-z0-9._-]"), "_")
        return cleaned.trim('_').ifBlank { "method" }.take(80)
    }
}
