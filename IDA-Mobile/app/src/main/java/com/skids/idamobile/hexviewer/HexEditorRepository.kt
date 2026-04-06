package com.skids.idamobile.hexviewer

import java.io.File
import java.io.RandomAccessFile

/**
 * Production hex workspace operations:
 * - Windowed file reads for mobile rendering.
 * - Strict patch parsing/validation.
 * - Safe patched-copy export without mutating original input.
 */
class HexEditorRepository {
    fun readRows(
        file: File,
        startOffset: Long,
        rowCount: Int,
        bytesPerRow: Int = DEFAULT_BYTES_PER_ROW
    ): Result<List<HexRow>> = runCatching {
        require(file.exists()) { "Target file does not exist: ${file.absolutePath}" }
        require(bytesPerRow in 8..32) { "bytesPerRow must be within 8..32." }
        require(rowCount in 1..MAX_ROW_COUNT) { "rowCount must be within 1..$MAX_ROW_COUNT." }

        val fileSize = file.length()
        val start = startOffset.coerceAtLeast(0L).coerceAtMost(fileSize)
        if (start >= fileSize) {
            return@runCatching emptyList()
        }

        val rows = mutableListOf<HexRow>()
        RandomAccessFile(file, "r").use { raf ->
            raf.seek(start)
            var currentOffset = start

            repeat(rowCount) {
                if (currentOffset >= fileSize) {
                    return@repeat
                }

                val buffer = ByteArray(bytesPerRow)
                val read = raf.read(buffer)
                if (read <= 0) {
                    return@repeat
                }

                rows += HexRow(
                    offset = currentOffset,
                    hexBytes = buffer.toHexString(read),
                    ascii = buffer.toAscii(read)
                )
                currentOffset += read.toLong()
            }
        }

        rows
    }

    fun parseOffset(input: String): Long {
        val normalized = input.trim().replace("_", "")
        require(normalized.isNotBlank()) { "Offset is required." }
        return if (normalized.startsWith("0x", ignoreCase = true)) {
            normalized.substring(2).toLong(16)
        } else {
            normalized.toLong(10)
        }
    }

    fun parsePatchBytes(input: String): ByteArray {
        val normalized = input
            .trim()
            .replace("0x", "", ignoreCase = true)
            .replace(Regex("[\\s,;:-]+"), "")

        require(normalized.isNotBlank()) { "Patch bytes are required." }
        require(normalized.length % 2 == 0) { "Patch bytes must contain full hex pairs." }

        val out = ByteArray(normalized.length / 2)
        var cursor = 0
        var byteIndex = 0
        while (cursor < normalized.length) {
            val pair = normalized.substring(cursor, cursor + 2)
            out[byteIndex] = pair.toInt(16).toByte()
            cursor += 2
            byteIndex += 1
        }
        return out
    }

    fun exportPatchedCopy(
        sourceFile: File,
        patchOffset: Long,
        patchBytes: ByteArray,
        exportDirectory: File
    ): Result<File> = runCatching {
        require(sourceFile.exists()) { "Source file does not exist." }
        require(patchOffset >= 0L) { "Patch offset cannot be negative." }
        require(patchBytes.isNotEmpty()) { "Patch payload cannot be empty." }

        val fileLength = sourceFile.length()
        val patchEnd = patchOffset + patchBytes.size.toLong()
        require(patchEnd <= fileLength) {
            "Patch range 0x${patchOffset.toString(16)}..0x${patchEnd.toString(16)} exceeds file size 0x${fileLength.toString(16)}."
        }

        exportDirectory.mkdirs()
        val outputFile = File(
            exportDirectory,
            "${sourceFile.nameWithoutExtension}_patched_${System.currentTimeMillis()}.apk"
        )

        sourceFile.inputStream().use { input ->
            outputFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }

        RandomAccessFile(outputFile, "rw").use { raf ->
            raf.seek(patchOffset)
            raf.write(patchBytes)
        }

        outputFile
    }

    private fun ByteArray.toHexString(read: Int): String {
        val parts = ArrayList<String>(read)
        for (index in 0 until read) {
            parts += String.format("%02X", this[index].toInt() and 0xFF)
        }
        return parts.joinToString(separator = " ")
    }

    private fun ByteArray.toAscii(read: Int): String {
        val sb = StringBuilder(read)
        for (index in 0 until read) {
            val value = this[index].toInt() and 0xFF
            sb.append(if (value in 32..126) value.toChar() else '.')
        }
        return sb.toString()
    }

    private companion object {
        private const val DEFAULT_BYTES_PER_ROW = 16
        private const val MAX_ROW_COUNT = 400
    }
}
