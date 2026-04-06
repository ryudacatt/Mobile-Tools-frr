package com.skids.idamobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.skids.idamobile.ui.IdaMobileApp
import com.skids.idamobile.viewmodels.MainViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            IdaMobileApp(
                uiState = viewModel.uiState,
                onRefreshNativeBridge = viewModel::refreshNativeBridge,
                onSelectTool = viewModel::selectTool,
                onPickApk = viewModel::openApkWorkspace,
                onDebuggerQueryChanged = viewModel::updateDebuggerQuery,
                onAssemblyQueryChanged = viewModel::updateAssemblyQuery,
                onAssemblyMethodSelected = viewModel::selectAssemblyMethod,
                onHexOffsetChanged = viewModel::updateHexOffsetInput,
                onHexLoadWindow = viewModel::loadHexWindow,
                onHexPatchOffsetChanged = viewModel::updateHexPatchOffsetInput,
                onHexPatchBytesChanged = viewModel::updateHexPatchBytesInput,
                onApplyHexPatch = viewModel::applyHexPatch,
                onExportSelectedMethod = viewModel::exportSelectedAssemblyMethod,
                onStringsQueryChanged = viewModel::updateStringsQuery,
                onStringSelected = viewModel::selectStringValue,
                onDecompilerQueryChanged = viewModel::updateDecompilerQuery,
                onRunDecompiler = viewModel::runDecompiler,
                onWebsiteUrlChanged = viewModel::updateWebsiteUrl,
                onInspectWebsite = viewModel::inspectWebsite,
                onRefreshTermuxStatus = viewModel::refreshTermuxStatus,
                onTermuxCommandChanged = viewModel::updateTermuxCommand,
                onLaunchTermux = viewModel::launchTermux,
                onRunTermuxCommand = viewModel::runTermuxCommand
            )
        }
    }
}
