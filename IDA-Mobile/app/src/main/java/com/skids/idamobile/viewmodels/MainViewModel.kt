package com.skids.idamobile.viewmodels

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skids.idamobile.disassembly.ApkAssemblyReport
import com.skids.idamobile.disassembly.ApkDisassemblyRepository
import com.skids.idamobile.features.websites.WebsiteInspectorRepository
import com.skids.idamobile.features.websites.WebsiteReport
import com.skids.idamobile.fileloader.ApkDebugReport
import com.skids.idamobile.fileloader.ApkDebuggerRepository
import com.skids.idamobile.fileloader.ApkInspectionResult
import com.skids.idamobile.fileloader.ApkInspectorRepository
import com.skids.idamobile.fileloader.FileLoaderRepository
import com.skids.idamobile.nativebridge.NativeBridge
import com.skids.idamobile.strings.ApkStringsRepository
import com.skids.idamobile.strings.StringsReport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class MainUiState(
    val nativeStatus: String = "Native core not checked yet.",
    val selectedTool: ToolMode = ToolMode.APK_OVERVIEW,
    val apkWorkspace: ApkWorkspaceUiState = ApkWorkspaceUiState(),
    val websiteState: WebsiteUiState = WebsiteUiState()
)

enum class ToolMode {
    APK_OVERVIEW,
    APK_DEBUGGER,
    ASSEMBLY,
    STRINGS_XREFS,
    WEBSITE
}

data class ApkWorkspaceUiState(
    val isLoading: Boolean = false,
    val selectedFileName: String? = null,
    val selectedFilePath: String? = null,
    val overview: ApkInspectionResult? = null,
    val debugReport: ApkDebugReport? = null,
    val assemblyReport: ApkAssemblyReport? = null,
    val stringsReport: StringsReport? = null,
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

/**
 * Single source of truth for all mobile tools bundled into the standalone APK.
 */
class MainViewModel : ViewModel() {
    private val fileLoaderRepository = FileLoaderRepository()
    private val apkInspectorRepository = ApkInspectorRepository()
    private val apkDebuggerRepository = ApkDebuggerRepository()
    private val apkDisassemblyRepository = ApkDisassemblyRepository()
    private val apkStringsRepository = ApkStringsRepository()
    private val websiteRepository = WebsiteInspectorRepository()

    var uiState by mutableStateOf(MainUiState())
        private set

    init {
        refreshNativeBridge()
    }

    fun refreshNativeBridge() {
        val updatedStatus = runCatching {
            val version = NativeBridge.getCoreVersion()
            "Native core online: $version"
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

            withContext(Dispatchers.Main) {
                val assemblyReport = assemblyResult.getOrNull()
                val stringsReport = stringsResult.getOrNull()
                uiState = uiState.copy(
                    apkWorkspace = ApkWorkspaceUiState(
                        isLoading = false,
                        selectedFileName = materializedApk.displayName,
                        selectedFilePath = materializedApk.apkFile.absolutePath,
                        overview = nativeResult.getOrNull(),
                        debugReport = debugResult.getOrNull(),
                        assemblyReport = assemblyReport,
                        stringsReport = stringsReport,
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
}

