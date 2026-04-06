package com.skids.idamobile.viewmodels

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skids.idamobile.features.websites.WebsiteInspectorRepository
import com.skids.idamobile.features.websites.WebsiteReport
import com.skids.idamobile.fileloader.ApkInspectionResult
import com.skids.idamobile.fileloader.ApkInspectorRepository
import com.skids.idamobile.nativebridge.NativeBridge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class MainUiState(
    val nativeStatus: String = "Native core not checked yet.",
    val selectedTool: ToolMode = ToolMode.APK,
    val apkState: ApkUiState = ApkUiState(),
    val websiteState: WebsiteUiState = WebsiteUiState()
)

enum class ToolMode {
    APK,
    WEBSITE
}

data class ApkUiState(
    val isLoading: Boolean = false,
    val selectedFileName: String? = null,
    val report: ApkInspectionResult? = null,
    val error: String? = null
)

data class WebsiteUiState(
    val isLoading: Boolean = false,
    val urlInput: String = "",
    val report: WebsiteReport? = null,
    val error: String? = null
)

class MainViewModel : ViewModel() {
    private val apkRepository = ApkInspectorRepository()
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

    fun inspectApk(context: Context, uri: Uri) {
        uiState = uiState.copy(apkState = uiState.apkState.copy(isLoading = true, error = null))
        viewModelScope.launch(Dispatchers.IO) {
            val result = apkRepository.inspect(context, uri)
            withContext(Dispatchers.Main) {
                uiState = if (result.isSuccess) {
                    val inspection = result.getOrThrow()
                    uiState.copy(
                        apkState = uiState.apkState.copy(
                            isLoading = false,
                            selectedFileName = inspection.displayName,
                            report = inspection,
                            error = null
                        )
                    )
                } else {
                    uiState.copy(
                        apkState = uiState.apkState.copy(
                            isLoading = false,
                            error = result.exceptionOrNull()?.message ?: "APK inspection failed."
                        )
                    )
                }
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
}

