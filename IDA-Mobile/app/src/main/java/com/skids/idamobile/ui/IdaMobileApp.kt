package com.skids.idamobile.ui

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.skids.idamobile.disassembly.AssemblySyntaxHighlighter
import com.skids.idamobile.viewmodels.ApkWorkspaceUiState
import com.skids.idamobile.viewmodels.MainUiState
import com.skids.idamobile.viewmodels.ToolMode
import com.skids.idamobile.viewmodels.WebsiteUiState

/**
 * Mobile-first shell for all standalone tools bundled into the app.
 */
@Composable
fun IdaMobileApp(
    uiState: MainUiState,
    onRefreshNativeBridge: () -> Unit,
    onSelectTool: (ToolMode) -> Unit,
    onPickApk: (Context, Uri) -> Unit,
    onDebuggerQueryChanged: (String) -> Unit,
    onAssemblyQueryChanged: (String) -> Unit,
    onAssemblyMethodSelected: (String) -> Unit,
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
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "IDA-Mobile Workbench",
                    style = MaterialTheme.typography.headlineSmall
                )
                Text(
                    text = uiState.nativeStatus,
                    style = MaterialTheme.typography.bodyMedium
                )
                Button(onClick = onRefreshNativeBridge, modifier = Modifier.fillMaxWidth()) {
                    Text("Recheck Native Core")
                }

                ToolSelector(
                    selectedTool = uiState.selectedTool,
                    onSelectTool = onSelectTool
                )

                if (uiState.selectedTool != ToolMode.WEBSITE) {
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
                        val buttonLabel = if (uiState.apkWorkspace.isLoading) "Loading APK Workspace..." else "Select APK"
                        Text(buttonLabel)
                    }
                    uiState.apkWorkspace.selectedFileName?.let { fileName ->
                        Text("APK: $fileName")
                    }
                    uiState.apkWorkspace.selectedFilePath?.let { path ->
                        Text("Local Cache: $path", style = MaterialTheme.typography.bodySmall)
                    }
                    uiState.apkWorkspace.error?.let { error ->
                        Text("Workspace error: $error", color = MaterialTheme.colorScheme.error)
                    }
                }

                when (uiState.selectedTool) {
                    ToolMode.APK_OVERVIEW -> ApkOverviewPanel(uiState.apkWorkspace)
                    ToolMode.APK_DEBUGGER -> ApkDebuggerPanel(uiState.apkWorkspace, onDebuggerQueryChanged)
                    ToolMode.ASSEMBLY -> AssemblyPanel(
                        state = uiState.apkWorkspace,
                        onAssemblyQueryChanged = onAssemblyQueryChanged,
                        onAssemblyMethodSelected = onAssemblyMethodSelected
                    )
                    ToolMode.WEBSITE -> WebsitePanel(
                        state = uiState.websiteState,
                        onWebsiteUrlChanged = onWebsiteUrlChanged,
                        onInspectWebsite = onInspectWebsite
                    )
                }
            }
        }
    }
}

@Composable
private fun ToolSelector(
    selectedTool: ToolMode,
    onSelectTool: (ToolMode) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ToolButton(
                label = "APK Overview",
                isSelected = selectedTool == ToolMode.APK_OVERVIEW,
                onClick = { onSelectTool(ToolMode.APK_OVERVIEW) },
                modifier = Modifier
                    .weight(1f)
                    .widthIn(min = 130.dp)
            )
            ToolButton(
                label = "APK Debugger",
                isSelected = selectedTool == ToolMode.APK_DEBUGGER,
                onClick = { onSelectTool(ToolMode.APK_DEBUGGER) },
                modifier = Modifier
                    .weight(1f)
                    .widthIn(min = 130.dp)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ToolButton(
                label = "Assembly View",
                isSelected = selectedTool == ToolMode.ASSEMBLY,
                onClick = { onSelectTool(ToolMode.ASSEMBLY) },
                modifier = Modifier
                    .weight(1f)
                    .widthIn(min = 130.dp)
            )
            ToolButton(
                label = "Website Tool",
                isSelected = selectedTool == ToolMode.WEBSITE,
                onClick = { onSelectTool(ToolMode.WEBSITE) },
                modifier = Modifier
                    .weight(1f)
                    .widthIn(min = 130.dp)
            )
        }
    }
}

@Composable
private fun ToolButton(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        modifier = modifier,
        onClick = onClick
    ) {
        if (isSelected) {
            Text("[$label]")
        } else {
            Text(label)
        }
    }
}

@Composable
private fun ApkOverviewPanel(state: ApkWorkspaceUiState) {
    val report = state.overview
    if (report == null) {
        Text("No APK loaded. Select an APK to see native ZIP + DEX summary.")
        return
    }

    Text("Native APK Summary", style = MaterialTheme.typography.titleMedium)
    Text("File size: ${report.report.fileSize} bytes")
    Text("ZIP entries: ${report.report.zipEntries}")
    Text("DEX files: ${report.report.dexFiles}")
    Text("Has AndroidManifest.xml: ${report.report.hasAndroidManifest}")
}

@Composable
private fun ApkDebuggerPanel(
    state: ApkWorkspaceUiState,
    onDebuggerQueryChanged: (String) -> Unit
) {
    val report = state.debugReport
    if (report == null) {
        Text("No debugger metadata loaded yet.")
        return
    }

    val query = state.debuggerQuery.trim()

    OutlinedTextField(
        value = state.debuggerQuery,
        onValueChange = onDebuggerQueryChanged,
        modifier = Modifier.fillMaxWidth(),
        label = { Text("Find in debugger data") },
        placeholder = { Text("permission, activity, service, package...") },
        singleLine = true
    )

    Text("APK Debugger Workspace", style = MaterialTheme.typography.titleMedium)
    Text("Package: ${report.packageName}")
    Text("Version: ${report.versionName ?: "unknown"} (${report.versionCode})")
    Text("Min SDK: ${report.minSdkVersion ?: "unknown"}")
    Text("Target SDK: ${report.targetSdkVersion ?: "unknown"}")
    Text("Debuggable: ${report.isDebuggable}")

    DebugListSection("Permissions", report.permissions, query)
    DebugListSection("Activities", report.activities, query)
    DebugListSection("Services", report.services, query)
    DebugListSection("Receivers", report.receivers, query)
    DebugListSection("Providers", report.providers, query)
    DebugListSection("Native Libraries", report.nativeLibraries, query)
    DebugListSection("Signing Cert SHA-256", report.certificateSha256, query)
}

@Composable
private fun DebugListSection(
    title: String,
    values: List<String>,
    query: String
) {
    val filtered = if (query.isBlank()) {
        values
    } else {
        values.filter { value -> value.contains(query, ignoreCase = true) }
    }

    Text("$title (${filtered.size})", style = MaterialTheme.typography.titleSmall)
    if (filtered.isEmpty()) {
        Text("(no matches)")
        return
    }

    // Keep rendering bounded on mobile while still exposing meaningful output
    filtered.take(120).forEach { value ->
        Text("- $value", style = MaterialTheme.typography.bodySmall)
    }
    if (filtered.size > 120) {
        Text("... ${filtered.size - 120} more items")
    }
}

@Composable
private fun AssemblyPanel(
    state: ApkWorkspaceUiState,
    onAssemblyQueryChanged: (String) -> Unit,
    onAssemblyMethodSelected: (String) -> Unit
) {
    val report = state.assemblyReport
    if (report == null) {
        Text("No classes.dex assembly loaded yet.")
        return
    }

    OutlinedTextField(
        value = state.assemblyQuery,
        onValueChange = onAssemblyQueryChanged,
        modifier = Modifier.fillMaxWidth(),
        label = { Text("Find method/class") },
        placeholder = { Text("Lcom/example/MainActivity; or onCreate") },
        singleLine = true
    )

    val query = state.assemblyQuery.trim()
    val filteredMethods = if (query.isBlank()) {
        report.methods
    } else {
        report.methods.filter { method ->
            method.className.contains(query, ignoreCase = true) ||
                method.methodName.contains(query, ignoreCase = true) ||
                method.descriptor.contains(query, ignoreCase = true)
        }
    }

    Text("DEX Entry: ${report.dexEntryName}")
    Text("Decoded methods: ${report.methods.size} ${if (report.wasTruncated) "(truncated for mobile safety)" else ""}")
    Text("Matches: ${filteredMethods.size}")

    if (filteredMethods.isEmpty()) {
        Text("No methods matched your query.")
        return
    }

    val selected = filteredMethods.firstOrNull { it.id == state.selectedAssemblyMethodId } ?: filteredMethods.first()

    Text("Methods", style = MaterialTheme.typography.titleMedium)
    filteredMethods.take(60).forEach { method ->
        Button(
            onClick = { onAssemblyMethodSelected(method.id) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("${method.className} -> ${method.methodName}")
        }
    }
    if (filteredMethods.size > 60) {
        Text("... ${filteredMethods.size - 60} more methods")
    }

    Text("Assembly: ${selected.methodName}${selected.descriptor}", style = MaterialTheme.typography.titleMedium)
    SelectionContainer {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            selected.lines.take(360).forEach { line ->
                Text(
                    text = AssemblySyntaxHighlighter.highlight(line),
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace
                )
            }
            if (selected.lines.size > 360) {
                Text("... ${selected.lines.size - 360} more lines")
            }
        }
    }
}

@Composable
private fun WebsitePanel(
    state: WebsiteUiState,
    onWebsiteUrlChanged: (String) -> Unit,
    onInspectWebsite: () -> Unit
) {
    OutlinedTextField(
        value = state.urlInput,
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
        Text(if (state.isLoading) "Inspecting website..." else "Inspect Website")
    }

    state.report?.let { report ->
        Text("Website Report", style = MaterialTheme.typography.titleMedium)
        Text("Normalized URL: ${report.normalizedUrl}")
        Text("HTTP status: ${report.statusCode}")
        Text("Content-Type: ${report.contentType}")
        Text("Sampled bytes: ${report.sampledBytes}")
        Text("Title: ${report.title ?: "(none)"}")
        Text("Script tags: ${report.scriptTagCount}")
        Text("SHA-256(sample): ${report.sha256}")
    }
    state.error?.let { error ->
        Text("Website error: $error", color = MaterialTheme.colorScheme.error)
    }
}
