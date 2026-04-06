package com.skids.idamobile.ui

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.skids.idamobile.viewmodels.MainUiState
import com.skids.idamobile.viewmodels.ToolMode

@Composable
fun IdaMobileApp(
    uiState: MainUiState,
    onRefreshNativeBridge: () -> Unit,
    onSelectTool: (ToolMode) -> Unit,
    onPickApk: (Context, Uri) -> Unit,
    onWebsiteUrlChanged: (String) -> Unit,
    onInspectWebsite: () -> Unit
) {
    val context = LocalContext.current
    val apkPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            if (uri != null) {
                onPickApk(context, uri)
            }
        }
    )

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "IDA-Mobile Inspector",
                    style = MaterialTheme.typography.headlineSmall
                )
                Text(
                    text = uiState.nativeStatus,
                    style = MaterialTheme.typography.bodyMedium
                )
                Button(onClick = onRefreshNativeBridge, modifier = Modifier.fillMaxWidth()) {
                    Text("Recheck JNI")
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        modifier = Modifier.weight(1f),
                        onClick = { onSelectTool(ToolMode.APK) }
                    ) { Text("APK Tool") }
                    Button(
                        modifier = Modifier.weight(1f),
                        onClick = { onSelectTool(ToolMode.WEBSITE) }
                    ) { Text("Website Tool") }
                }

                if (uiState.selectedTool == ToolMode.APK) {
                    Button(
                        onClick = {
                            apkPicker.launch(
                                arrayOf(
                                    "application/vnd.android.package-archive",
                                    "application/zip",
                                    "*/*"
                                )
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (uiState.apkState.isLoading) "Inspecting APK..." else "Pick APK")
                    }

                    val selectedName = uiState.apkState.selectedFileName
                    if (!selectedName.isNullOrBlank()) {
                        Text("Selected: $selectedName")
                    }
                    uiState.apkState.report?.let { report ->
                        Text("File size: ${report.report.fileSize} bytes")
                        Text("ZIP entries: ${report.report.zipEntries}")
                        Text("DEX files: ${report.report.dexFiles}")
                        Text("Has AndroidManifest.xml: ${report.report.hasAndroidManifest}")
                    }
                    uiState.apkState.error?.let { error ->
                        Text("APK error: $error", color = MaterialTheme.colorScheme.error)
                    }
                } else {
                    OutlinedTextField(
                        value = uiState.websiteState.urlInput,
                        onValueChange = onWebsiteUrlChanged,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Website URL") },
                        placeholder = { Text("example.com") },
                        singleLine = true
                    )
                    Button(
                        onClick = onInspectWebsite,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (uiState.websiteState.isLoading) "Inspecting website..." else "Inspect Website")
                    }
                    uiState.websiteState.report?.let { report ->
                        Text("Normalized URL: ${report.normalizedUrl}")
                        Text("HTTP status: ${report.statusCode}")
                        Text("Content-Type: ${report.contentType}")
                        Text("Sampled bytes: ${report.sampledBytes}")
                        Text("Title: ${report.title ?: "(none)"}")
                        Text("Script tags: ${report.scriptTagCount}")
                        Text("SHA-256(sample): ${report.sha256}")
                    }
                    uiState.websiteState.error?.let { error ->
                        Text("Website error: $error", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}
