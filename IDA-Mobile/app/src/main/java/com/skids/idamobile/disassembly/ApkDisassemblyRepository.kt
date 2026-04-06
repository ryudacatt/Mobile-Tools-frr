package com.skids.idamobile.disassembly

import java.io.File
import java.util.zip.ZipFile
import org.jf.dexlib2.Opcodes
import org.jf.dexlib2.dexbacked.DexBackedDexFile
import org.jf.dexlib2.iface.Method
import org.jf.dexlib2.iface.instruction.FiveRegisterInstruction
import org.jf.dexlib2.iface.instruction.Instruction
import org.jf.dexlib2.iface.instruction.NarrowLiteralInstruction
import org.jf.dexlib2.iface.instruction.OffsetInstruction
import org.jf.dexlib2.iface.instruction.OneRegisterInstruction
import org.jf.dexlib2.iface.instruction.ReferenceInstruction
import org.jf.dexlib2.iface.instruction.RegisterRangeInstruction
import org.jf.dexlib2.iface.instruction.ThreeRegisterInstruction
import org.jf.dexlib2.iface.instruction.TwoRegisterInstruction
import org.jf.dexlib2.iface.instruction.WideLiteralInstruction

/**
 * Decodes Dalvik bytecode from classes.dex and prepares it for the mobile assembly viewer.
 */
class ApkDisassemblyRepository {
    fun disassemble(apkFile: File): Result<ApkAssemblyReport> = runCatching {
        ZipFile(apkFile).use { zip ->
            val dexEntry = zip.getEntry("classes.dex")
                ?: error("classes.dex not found in selected APK.")

            zip.getInputStream(dexEntry).use { dexStream ->
                val dexFile = DexBackedDexFile.fromInputStream(Opcodes.getDefault(), dexStream)
                buildReport(dexEntry.name, dexFile)
            }
        }
    }

    private fun buildReport(dexEntryName: String, dexFile: DexBackedDexFile): ApkAssemblyReport {
        val methods = mutableListOf<AssemblyMethod>()
        var wasTruncated = false

        classLoop@ for (classDef in dexFile.classes.sortedBy { it.type }) {
            val allMethods = classDef.directMethods + classDef.virtualMethods
            for (method in allMethods) {
                if (methods.size >= MAX_METHODS) {
                    wasTruncated = true
                    break@classLoop
                }

                val implementation = method.implementation ?: continue
                val lines = mutableListOf<InstructionLine>()
                var address = 0L

                for (instruction in implementation.instructions) {
                    if (lines.size >= MAX_INSTRUCTIONS_PER_METHOD) {
                        lines += InstructionLine(
                            address = address,
                            mnemonic = "truncated",
                            operands = "; method output truncated for UI safety"
                        )
                        wasTruncated = true
                        break
                    }

                    lines += InstructionLine(
                        address = address,
                        mnemonic = instruction.opcode.name,
                        operands = buildOperands(instruction)
                    )
                    address += instruction.codeUnits * 2L
                }

                methods += AssemblyMethod(
                    id = buildMethodId(method),
                    className = classDef.type,
                    methodName = method.name,
                    descriptor = buildDescriptor(method),
                    instructionCount = lines.size,
                    lines = lines
                )
            }
        }

        return ApkAssemblyReport(
            dexEntryName = dexEntryName,
            methods = methods,
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

    private fun buildOperands(instruction: Instruction): String {
        val parts = mutableListOf<String>()
        appendRegisters(instruction, parts)

        if (instruction is ReferenceInstruction) {
            parts += instruction.reference.toString()
        }
        if (instruction is NarrowLiteralInstruction) {
            parts += "#${instruction.narrowLiteral}"
        } else if (instruction is WideLiteralInstruction) {
            parts += "#${instruction.wideLiteral}"
        }
        if (instruction is OffsetInstruction) {
            parts += "-> ${instruction.codeOffset}"
        }

        return parts.joinToString(", ")
    }

    private fun appendRegisters(instruction: Instruction, parts: MutableList<String>) {
        when (instruction) {
            is FiveRegisterInstruction -> {
                val regs = mutableListOf<Int>()
                if (instruction.registerCount >= 1) regs += instruction.registerC
                if (instruction.registerCount >= 2) regs += instruction.registerD
                if (instruction.registerCount >= 3) regs += instruction.registerE
                if (instruction.registerCount >= 4) regs += instruction.registerF
                if (instruction.registerCount >= 5) regs += instruction.registerG
                if (regs.isNotEmpty()) {
                    parts += regs.joinToString(", ") { "v$it" }
                }
            }

            is ThreeRegisterInstruction -> {
                parts += "v${instruction.registerA}, v${instruction.registerB}, v${instruction.registerC}"
            }

            is TwoRegisterInstruction -> {
                parts += "v${instruction.registerA}, v${instruction.registerB}"
            }

            is OneRegisterInstruction -> {
                parts += "v${instruction.registerA}"
            }

            is RegisterRangeInstruction -> {
                val start = instruction.startRegister
                val end = start + instruction.registerCount - 1
                if (instruction.registerCount > 0) {
                    parts += if (start == end) "v$start" else "v$start..v$end"
                }
            }
        }
    }

    private companion object {
        private const val MAX_METHODS = 400
        private const val MAX_INSTRUCTIONS_PER_METHOD = 500
    }
}

