package com.skids.idamobile.viewmodels

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skids.idamobile.decompiler.Radare2DecompileReport
import com.skids.idamobile.decompiler.Radare2DecompilerRepository
import com.skids.idamobile.disassembly.ApkAssemblyReport
import com.skids.idamobile.disassembly.AssemblyExportRepository
import com.skids.idamobile.features.termux.TermuxBridgeRepository
import com.skids.idamobile.features.termux.TermuxBridgeStatus
import com.skids.idamobile.features.websites.WebsiteInspectorRepository
import com.skids.idamobile.features.websites.WebsiteReport
import com.skids.idamobile.fileloader.ApkDebugReport
import com.skids.idamobile.fileloader.ApkDebuggerRepository
import com.skids.idamobile.fileloader.ApkInspectionResult
import com.skids.idamobile.fileloader.ApkInspectorRepository
import com.skids.idamobile.fileloader.FileLoaderRepository
import com.skids.idamobile.hexviewer.HexEditorRepository
import com.skids.idamobile.hexviewer.HexRow
import com.skids.idamobile.nativebridge.NativeBridge
import com.skids.idamobile.strings.ApkStringsRepository
import com.skids.idamobile.strings.PythonStringInsight
import com.skids.idamobile.strings.PythonStringsRepository
import com.skids.idamobile.strings.StringsReport
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class MainUiState(
    val nativeStatus: String = "Native core not checked yet.",
    val selectedTool: ToolMode = ToolMode.APK_OVERVIEW,
    val apkWorkspace: ApkWorkspaceUiState = ApkWorkspaceUiState(),
    val websiteState: WebsiteUiState = WebsiteUiState(),
    val termuxState: TermuxUiState = TermuxUiState()
)

enum class ToolMode {
    APK_OVERVIEW,
    APK_DEBUGGER,
    ASSEMBLY,
    HEX_EDITOR,
    STRINGS_XREFS,
    DECOMPILER,
    WEBSITE,
    TERMUX
}

data class HexWorkspaceUiState(
    val offsetInput: String = "0x0",
    val rows: List<HexRow> = emptyList(),
    val patchOffsetInput: String = "0x0",
    val patchBytesInput: String = "",
    val status: String? = null,
    val lastPatchedFilePath: String? = null,
    val lastExportedMethodPath: String? = null
)

data class DecompilerUiState(
    val isLoading: Boolean = false,
    val functionQuery: String = "",
    val report: Radare2DecompileReport? = null,
    val error: String? = null
)

data class ApkWorkspaceUiState(
    val isLoading: Boolean = false,
    val selectedFileName: String? = null,
    val selectedFilePath: String? = null,
    val overview: ApkInspectionResult? = null,
    val debugReport: ApkDebugReport? = null,
    val assemblyReport: ApkAssemblyReport? = null,
    val stringsReport: StringsReport? = null,
    val pythonInsights: List<PythonStringInsight> = emptyList(),
    val hexState: HexWorkspaceUiState = HexWorkspaceUiState(),
    val decompilerState: DecompilerUiState = DecompilerUiState(),
    val selectedAssemblyMethodId: String? = null,
    val selectedStringValue: String? = null,
    val assemblyQuery: String = "",
    val debuggerQuery: String = "",
    val stringsQuery: String = "",
    val error: String? = null
)

data class WebsiteUiState(
    val isLoading: Boolean = false,
    val urlInput: String = "",
    val report: WebsiteReport? = null,
    val error: String? = null
)

data class TermuxUiState(
    val commandInput: String = "r2 -v",
    val status: TermuxBridgeStatus? = null,
    val message: String? = null
)

/**
 * Single source of truth for all mobile tools bundled into the standalone APK.
 */
class MainViewModel : ViewModel() {
    private val fileLoaderRepository = FileLoaderRepository()
    private val apkInspectorRepository = ApkInspectorRepository()
    private val apkDebuggerRepository = ApkDebuggerRepository()
    private val apkDisassemblyRepository = com.skids.idamobile.disassembly.ApkDisassemblyRepository()
    private val apkStringsRepository = ApkStringsRepository()
    private val pythonStringsRepository = PythonStringsRepository()
    private val hexEditorRepository = HexEditorRepository()
    private val assemblyExportRepository = AssemblyExportRepository()
    private val radare2DecompilerRepository = Radare2DecompilerRepository()
    private val termuxBridgeRepository = TermuxBridgeRepository()
    private val websiteRepository = WebsiteInspectorRepository()

    var uiState by mutableStateOf(MainUiState())
        private set

    init {
        refreshNativeBridge()
    }

    fun refreshNativeBridge() {
        val updatedStatus = runCatching {
            val coreVersion = NativeBridge.getCoreVersion()
            val capstoneVersion = NativeBridge.getCapstoneVersion()
            "Native core online: $coreVersion | $capstoneVersion"
        }.getOrElse { throwable ->
            "JNI error: ${throwable.javaClass.simpleName}: ${throwable.message}"
        }
        uiState = uiState.copy(nativeStatus = updatedStatus)
    }

    fun selectTool(mode: ToolMode) {
        uiState = uiState.copy(selectedTool = mode)
    }

    fun openApkWorkspace(context: Context, uri: Uri) {
        uiState = uiState.copy(
            apkWorkspace = uiState.apkWorkspace.copy(
                isLoading = true,
                error = null
            )
        )

        viewModelScope.launch(Dispatchers.IO) {
            val materializedResult = fileLoaderRepository.materializeApk(context, uri)
            if (materializedResult.isFailure) {
                withContext(Dispatchers.Main) {
                    uiState = uiState.copy(
                        apkWorkspace = uiState.apkWorkspace.copy(
                            isLoading = false,
                            error = materializedResult.exceptionOrNull()?.message ?: "Failed to copy APK locally."
                        )
                    )
                }
                return@launch
            }

            val materializedApk = materializedResult.getOrThrow()
            // Execute all APK analyzers on the same cached file to keep results consistent.
            val nativeResult = apkInspectorRepository.inspect(materializedApk.apkFile, materializedApk.displayName)
            val debugResult = apkDebuggerRepository.inspect(context, materializedApk.apkFile, materializedApk.displayName)
            val assemblyResult = apkDisassemblyRepository.disassemble(materializedApk.apkFile)
            val stringsResult = apkStringsRepository.analyze(materializedApk.apkFile)
            val hexResult = hexEditorRepository.readRows(materializedApk.apkFile, startOffset = 0L, rowCount = 120)
            val pythonResult = if (stringsResult.isSuccess) {
                pythonStringsRepository.analyze(context, stringsResult.getOrThrow().records)
            } else {
                Result.failure(IllegalStateException("Strings report unavailable for Python analysis."))
            }

            val failures = mutableListOf<String>()
            if (nativeResult.isFailure) {
                failures += "Native inspector: ${nativeResult.exceptionOrNull()?.message}"
            }
            if (debugResult.isFailure) {
                failures += "Debugger report: ${debugResult.exceptionOrNull()?.message}"
            }
            if (assemblyResult.isFailure) {
                failures += "Assembly view: ${assemblyResult.exceptionOrNull()?.message}"
            }
            if (stringsResult.isFailure) {
                failures += "Strings + xrefs: ${stringsResult.exceptionOrNull()?.message}"
            }
            if (pythonResult.isFailure) {
                failures += "Python heuristics: ${pythonResult.exceptionOrNull()?.message}"
            }
            if (hexResult.isFailure) {
                failures += "Hex workspace: ${hexResult.exceptionOrNull()?.message}"
            }

            withContext(Dispatchers.Main) {
                val assemblyReport = assemblyResult.getOrNull()
                val stringsReport = stringsResult.getOrNull()
                val pythonInsights = pythonResult.getOrDefault(emptyList())
                uiState = uiState.copy(
                    apkWorkspace = ApkWorkspaceUiState(
                        isLoading = false,
                        selectedFileName = materializedApk.displayName,
                        selectedFilePath = materializedApk.apkFile.absolutePath,
                        overview = nativeResult.getOrNull(),
                        debugReport = debugResult.getOrNull(),
                        assemblyReport = assemblyReport,
                        stringsReport = stringsReport,
                        pythonInsights = pythonInsights,
                        hexState = HexWorkspaceUiState(
                            rows = hexResult.getOrDefault(emptyList()),
                            status = if (hexResult.isSuccess) {
                                "Loaded first 120 rows from offset 0x0"
                            } else {
                                null
                            }
                        ),
                        decompilerState = DecompilerUiState(),
                        selectedAssemblyMethodId = assemblyReport?.methods?.firstOrNull()?.id,
                        selectedStringValue = stringsReport?.records?.firstOrNull()?.entry?.value,
                        assemblyQuery = "",
                        debuggerQuery = "",
                        stringsQuery = "",
                        error = failures.takeIf { it.isNotEmpty() }?.joinToString(separator = " | ")
                    )
                )
            }
        }
    }

    fun updateDebuggerQuery(input: String) {
        uiState = uiState.copy(
            apkWorkspace = uiState.apkWorkspace.copy(
                debuggerQuery = input
            )
        )
    }

    fun updateAssemblyQuery(input: String) {
        uiState = uiState.copy(
            apkWorkspace = uiState.apkWorkspace.copy(
                assemblyQuery = input
            )
        )
    }

    fun selectAssemblyMethod(methodId: String) {
        uiState = uiState.copy(
            apkWorkspace = uiState.apkWorkspace.copy(
                selectedAssemblyMethodId = methodId
            )
        )
    }

    fun updateStringsQuery(input: String) {
        uiState = uiState.copy(
            apkWorkspace = uiState.apkWorkspace.copy(
                stringsQuery = input
            )
        )
    }

    fun selectStringValue(stringValue: String) {
        uiState = uiState.copy(
            apkWorkspace = uiState.apkWorkspace.copy(
                selectedStringValue = stringValue
            )
        )
    }

    fun updateHexOffsetInput(input: String) {
        uiState = uiState.copy(
            apkWorkspace = uiState.apkWorkspace.copy(
                hexState = uiState.apkWorkspace.hexState.copy(
                    offsetInput = input
                )
            )
        )
    }

    fun updateHexPatchOffsetInput(input: String) {
        uiState = uiState.copy(
            apkWorkspace = uiState.apkWorkspace.copy(
                hexState = uiState.apkWorkspace.hexState.copy(
                    patchOffsetInput = input
                )
            )
        )
    }

    fun updateHexPatchBytesInput(input: String) {
        uiState = uiState.copy(
            apkWorkspace = uiState.apkWorkspace.copy(
                hexState = uiState.apkWorkspace.hexState.copy(
                    patchBytesInput = input
                )
            )
        )
    }

    fun loadHexWindow() {
        val file = currentApkFile() ?: return
        val offsetInput = uiState.apkWorkspace.hexState.offsetInput

        viewModelScope.launch(Dispatchers.IO) {
            val result = runCatching {
                val offset = hexEditorRepository.parseOffset(offsetInput)
                val rows = hexEditorRepository.readRows(file, startOffset = offset, rowCount = 160).getOrThrow()
                offset to rows
            }

            withContext(Dispatchers.Main) {
                uiState = uiState.copy(
                    apkWorkspace = uiState.apkWorkspace.copy(
                        hexState = if (result.isSuccess) {
                            val (offset, rows) = result.getOrThrow()
                            uiState.apkWorkspace.hexState.copy(
                                rows = rows,
                                status = "Loaded ${rows.size} rows at 0x${offset.toString(16)}"
                            )
                        } else {
                            uiState.apkWorkspace.hexState.copy(
                                status = "Hex load failed: ${result.exceptionOrNull()?.message}"
                            )
                        }
                    )
                )
            }
        }
    }

    fun applyHexPatch(context: Context) {
        val file = currentApkFile() ?: return
        val patchOffsetInput = uiState.apkWorkspace.hexState.patchOffsetInput
        val patchBytesInput = uiState.apkWorkspace.hexState.patchBytesInput

        viewModelScope.launch(Dispatchers.IO) {
            val result = runCatching {
                val patchOffset = hexEditorRepository.parseOffset(patchOffsetInput)
                val patchBytes = hexEditorRepository.parsePatchBytes(patchBytesInput)
                val exportsDir = File(context.getExternalFilesDir(null) ?: context.filesDir, "exports")
                hexEditorRepository.exportPatchedCopy(
                    sourceFile = file,
                    patchOffset = patchOffset,
                    patchBytes = patchBytes,
                    exportDirectory = exportsDir
                ).getOrThrow()
            }

            withContext(Dispatchers.Main) {
                uiState = uiState.copy(
                    apkWorkspace = uiState.apkWorkspace.copy(
                        hexState = if (result.isSuccess) {
                            val exported = result.getOrThrow()
                            uiState.apkWorkspace.hexState.copy(
                                status = "Patch exported: ${exported.absolutePath}",
                                lastPatchedFilePath = exported.absolutePath
                            )
                        } else {
                            uiState.apkWorkspace.hexState.copy(
                                status = "Patch failed: ${result.exceptionOrNull()?.message}"
                            )
                        }
                    )
                )
            }
        }
    }

    fun exportSelectedAssemblyMethod(context: Context) {
        val report = uiState.apkWorkspace.assemblyReport ?: return
        val selectedId = uiState.apkWorkspace.selectedAssemblyMethodId ?: return
        val method = report.methods.firstOrNull { it.id == selectedId } ?: return

        viewModelScope.launch(Dispatchers.IO) {
            val result = runCatching {
                val outputDir = File(context.getExternalFilesDir(null) ?: context.filesDir, "exports/code")
                assemblyExportRepository.exportMethodReport(method, outputDir).getOrThrow()
            }

            withContext(Dispatchers.Main) {
                uiState = uiState.copy(
                    apkWorkspace = uiState.apkWorkspace.copy(
                        hexState = if (result.isSuccess) {
                            val output = result.getOrThrow()
                            uiState.apkWorkspace.hexState.copy(
                                status = "Assembly export saved: ${output.absolutePath}",
                                lastExportedMethodPath = output.absolutePath
                            )
                        } else {
                            uiState.apkWorkspace.hexState.copy(
                                status = "Assembly export failed: ${result.exceptionOrNull()?.message}"
                            )
                        }
                    )
                )
            }
        }
    }

    fun updateDecompilerQuery(input: String) {
        uiState = uiState.copy(
            apkWorkspace = uiState.apkWorkspace.copy(
                decompilerState = uiState.apkWorkspace.decompilerState.copy(
                    functionQuery = input
                )
            )
        )
    }

    fun runDecompiler(context: Context) {
        val apkFile = currentApkFile() ?: return
        val query = uiState.apkWorkspace.decompilerState.functionQuery

        uiState = uiState.copy(
            apkWorkspace = uiState.apkWorkspace.copy(
                decompilerState = uiState.apkWorkspace.decompilerState.copy(
                    isLoading = true,
                    error = null
                )
            )
        )

        viewModelScope.launch(Dispatchers.IO) {
            val result = radare2DecompilerRepository.decompileApkPrimaryDex(context, apkFile, query)
            withContext(Dispatchers.Main) {
                uiState = uiState.copy(
                    apkWorkspace = uiState.apkWorkspace.copy(
                        decompilerState = if (result.isSuccess) {
                            uiState.apkWorkspace.decompilerState.copy(
                                isLoading = false,
                                report = result.getOrThrow(),
                                error = null
                            )
                        } else {
                            uiState.apkWorkspace.decompilerState.copy(
                                isLoading = false,
                                report = null,
                                error = result.exceptionOrNull()?.message ?: "Radare2 decompilation failed."
                            )
                        }
                    )
                )
            }
        }
    }

    fun updateWebsiteUrl(input: String) {
        uiState = uiState.copy(
            websiteState = uiState.websiteState.copy(
                urlInput = input,
                error = null
            )
        )
    }

    fun inspectWebsite() {
        val currentUrl = uiState.websiteState.urlInput
        uiState = uiState.copy(websiteState = uiState.websiteState.copy(isLoading = true, error = null))
        viewModelScope.launch(Dispatchers.IO) {
            val result = websiteRepository.inspect(currentUrl)
            withContext(Dispatchers.Main) {
                uiState = if (result.isSuccess) {
                    uiState.copy(
                        websiteState = uiState.websiteState.copy(
                            isLoading = false,
                            report = result.getOrThrow(),
                            error = null
                        )
                    )
                } else {
                    uiState.copy(
                        websiteState = uiState.websiteState.copy(
                            isLoading = false,
                            error = result.exceptionOrNull()?.message ?: "Website inspection failed."
                        )
                    )
                }
            }
        }
    }

    fun refreshTermuxStatus(context: Context) {
        val status = termuxBridgeRepository.status(context)
        uiState = uiState.copy(
            termuxState = uiState.termuxState.copy(
                status = status,
                message = status.detail
            )
        )
    }

    fun updateTermuxCommand(input: String) {
        uiState = uiState.copy(
            termuxState = uiState.termuxState.copy(
                commandInput = input
            )
        )
    }

    fun launchTermux(context: Context) {
        val result = termuxBridgeRepository.launch(context)
        uiState = uiState.copy(
            termuxState = uiState.termuxState.copy(
                message = if (result.isSuccess) {
                    "Termux launch requested."
                } else {
                    "Termux launch failed: ${result.exceptionOrNull()?.message}"
                }
            )
        )
    }

    fun runTermuxCommand(context: Context) {
        val command = uiState.termuxState.commandInput
        val result = termuxBridgeRepository.runCommand(context, command)
        uiState = uiState.copy(
            termuxState = uiState.termuxState.copy(
                message = if (result.isSuccess) {
                    "Command sent to Termux service."
                } else {
                    "Termux command failed: ${result.exceptionOrNull()?.message}"
                }
            )
        )
    }

    private fun currentApkFile(): File? {
        val filePath = uiState.apkWorkspace.selectedFilePath ?: return null
        return File(filePath).takeIf { it.exists() }
    }
}
