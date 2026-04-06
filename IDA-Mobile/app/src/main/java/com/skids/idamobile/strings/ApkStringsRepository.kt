package com.skids.idamobile.strings

import java.io.File
import java.util.zip.ZipFile
import org.jf.dexlib2.Opcodes
import org.jf.dexlib2.dexbacked.DexBackedDexFile
import org.jf.dexlib2.iface.Method
import org.jf.dexlib2.iface.instruction.ReferenceInstruction
import org.jf.dexlib2.iface.reference.StringReference

/**
 * Extracts DEX string constants that are referenced by instructions and builds xref links.
 */
class ApkStringsRepository {
    fun analyze(apkFile: File): Result<StringsReport> = runCatching {
        ZipFile(apkFile).use { zip ->
            val dexEntry = zip.getEntry("classes.dex")
                ?: error("classes.dex not found in selected APK.")

            zip.getInputStream(dexEntry).use { dexStream ->
                val dexFile = DexBackedDexFile.fromInputStream(Opcodes.getDefault(), dexStream)
                buildReport(dexEntry.name, dexFile)
            }
        }
    }

    private fun buildReport(dexEntryName: String, dexFile: DexBackedDexFile): StringsReport {
        val xrefsByString = LinkedHashMap<String, MutableList<StringXref>>()
        var wasTruncated = false

        for (classDef in dexFile.classes.sortedBy { it.type }) {
            val methods = classDef.directMethods + classDef.virtualMethods
            for (method in methods) {
                val implementation = method.implementation ?: continue
                val methodId = buildMethodId(method)
                val descriptor = buildDescriptor(method)
                var address = 0L

                for (instruction in implementation.instructions) {
                    val refInstruction = instruction as? ReferenceInstruction
                    val stringRef = refInstruction?.reference as? StringReference
                    if (stringRef != null) {
                        val value = stringRef.string
                        var xrefList = xrefsByString[value]
                        if (xrefList == null) {
                            if (xrefsByString.size >= MAX_TRACKED_STRINGS) {
                                wasTruncated = true
                                address += instruction.codeUnits * 2L
                                continue
                            }
                            xrefList = mutableListOf()
                            xrefsByString[value] = xrefList
                        }

                        if (xrefList.size < MAX_XREFS_PER_STRING) {
                            xrefList += StringXref(
                                methodId = methodId,
                                className = method.definingClass,
                                methodName = method.name,
                                descriptor = descriptor,
                                instructionAddress = address,
                                opcode = instruction.opcode.name
                            )
                        } else {
                            wasTruncated = true
                        }
                    }

                    address += instruction.codeUnits * 2L
                }
            }
        }

        val records = xrefsByString.entries
            .map { (value, xrefs) ->
                StringRecord(
                    entry = StringEntry(
                        value = value,
                        utf8Length = value.toByteArray(Charsets.UTF_8).size,
                        xrefCount = xrefs.size
                    ),
                    xrefs = xrefs.toList()
                )
            }
            .sortedWith(
                compareByDescending<StringRecord> { it.entry.xrefCount }
                    .thenBy { it.entry.value }
            )

        return StringsReport(
            dexEntryName = dexEntryName,
            totalStrings = records.size,
            records = records,
            wasTruncated = wasTruncated
        )
    }

    private fun buildMethodId(method: Method): String {
        return "${method.definingClass}->${method.name}${buildDescriptor(method)}"
    }

    private fun buildDescriptor(method: Method): String {
        val params = method.parameterTypes.joinToString(separator = "")
        return "($params)${method.returnType}"
    }

    private companion object {
        private const val MAX_TRACKED_STRINGS = 5000
        private const val MAX_XREFS_PER_STRING = 300
    }
}

