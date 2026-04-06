package com.skids.idamobile.decompiler

import android.content.Context
import android.os.Build
import java.io.File
import java.util.concurrent.TimeUnit
import java.util.zip.ZipFile
import org.json.JSONArray

/**
 * Real decompiler backend integration:
 * - Uses local radare2 binary bundled in app assets by ABI.
 * - Executes analysis/decompilation commands fully offline on-device.
 */
class Radare2DecompilerRepository {
    fun decompileApkPrimaryDex(
        context: Context,
        apkFile: File,
        functionQuery: String
    ): Result<Radare2DecompileReport> = runCatching {
        require(apkFile.exists()) { "APK file does not exist." }

        val dexFile = extractPrimaryDex(apkFile, context.cacheDir)
        val abi = Build.SUPPORTED_ABIS.firstOrNull() ?: "unknown"
        val binary = ensureRadare2Binary(context, abi)

        val functionsRaw = runRadare2(
            binary = binary,
            targetFile = dexFile,
            command = "aaa;aflj"
        )
        val allFunctions = parseFunctions(functionsRaw)

        val filteredFunctions = if (functionQuery.isBlank()) {
            allFunctions
        } else {
            allFunctions.filter { function ->
                function.name.contains(functionQuery, ignoreCase = true)
            }
        }

        val selected = filteredFunctions.firstOrNull()
        val pseudoCode = if (selected != null) {
            runRadare2(
                binary = binary,
                targetFile = dexFile,
                command = "aaa;s 0x${selected.offset.toString(16)};pdc"
            ).trim()
        } else {
            "No function matched query."
        }

        Radare2DecompileReport(
            dexPath = dexFile.absolutePath,
            abi = abi,
            functionCount = allFunctions.size,
            wasTruncated = filteredFunctions.size > MAX_FUNCTIONS_TO_RETURN,
            functions = filteredFunctions.take(MAX_FUNCTIONS_TO_RETURN),
            selectedFunction = selected,
            pseudoCode = pseudoCode
        )
    }

    private fun extractPrimaryDex(apkFile: File, cacheDir: File): File {
        val outDir = File(cacheDir, "radare2_dex").apply { mkdirs() }
        val output = File(outDir, "${apkFile.nameWithoutExtension}_classes.dex")

        ZipFile(apkFile).use { zip ->
            val dexEntry = zip.getEntry("classes.dex")
                ?: error("classes.dex not found in selected APK.")
            zip.getInputStream(dexEntry).use { input ->
                output.outputStream().use { out ->
                    input.copyTo(out)
                }
            }
        }
        return output
    }

    private fun ensureRadare2Binary(context: Context, abi: String): File {
        val targetDir = File(context.filesDir, "tools/radare2/$abi").apply { mkdirs() }
        val binary = File(targetDir, "r2")
        if (binary.exists() && binary.canExecute()) {
            return binary
        }

        val assetPath = "tools/radare2/$abi/r2"
        runCatching {
            context.assets.open(assetPath).use { input ->
                binary.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        }.getOrElse { throwable ->
            throw IllegalStateException(
                "Radare2 binary missing for ABI '$abi'. Place it at app/src/main/assets/$assetPath. ${throwable.message}"
            )
        }

        check(binary.setExecutable(true)) {
            "Failed to mark radare2 binary executable: ${binary.absolutePath}"
        }
        return binary
    }

    private fun runRadare2(binary: File, targetFile: File, command: String): String {
        val process = ProcessBuilder(
            listOf(
                binary.absolutePath,
                "-2",
                "-q",
                "-c",
                command,
                targetFile.absolutePath
            )
        )
            .redirectErrorStream(true)
            .start()

        val completed = process.waitFor(COMMAND_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        if (!completed) {
            process.destroyForcibly()
            error("Radare2 timed out after $COMMAND_TIMEOUT_SECONDS seconds.")
        }

        val output = process.inputStream.bufferedReader().readText()
        if (process.exitValue() != 0) {
            error("Radare2 exited with code ${process.exitValue()}: ${output.take(800)}")
        }
        return output
    }

    private fun parseFunctions(rawOutput: String): List<Radare2Function> {
        val jsonPayload = extractJsonArray(rawOutput)
        val array = JSONArray(jsonPayload)
        val functions = mutableListOf<Radare2Function>()

        for (index in 0 until array.length()) {
            val item = array.getJSONObject(index)
            val name = item.optString("name").ifBlank { "fcn_$index" }
            val offset = item.optLong("offset", -1L)
            val size = item.optLong("size", 0L)
            if (offset >= 0L) {
                functions += Radare2Function(
                    name = name,
                    offset = offset,
                    size = size
                )
            }
        }

        return functions.sortedBy { it.offset }
    }

    private fun extractJsonArray(rawOutput: String): String {
        val start = rawOutput.indexOf('[')
        val end = rawOutput.lastIndexOf(']')
        require(start >= 0 && end > start) {
            "Radare2 did not return function JSON. Output: ${rawOutput.take(400)}"
        }
        return rawOutput.substring(start, end + 1)
    }

    private companion object {
        private const val MAX_FUNCTIONS_TO_RETURN = 300
        private const val COMMAND_TIMEOUT_SECONDS = 90L
    }
}
