package com.geeks.idamobile.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.geeks.idamobile.nativebridge.NativeBridge

data class MainUiState(
    val nativeStatus: String = "Native core not checked yet."
)

class MainViewModel : ViewModel() {
    var uiState by mutableStateOf(MainUiState())
        private set

    init {
        refreshNativeBridge()
    }

    fun refreshNativeBridge() {
        uiState = runCatching {
            val version = NativeBridge.getCoreVersion()
            MainUiState(nativeStatus = "Native core online: $version")
        }.getOrElse { throwable ->
            MainUiState(nativeStatus = "JNI error: ${throwable.javaClass.simpleName}: ${throwable.message}")
        }
    }
}

