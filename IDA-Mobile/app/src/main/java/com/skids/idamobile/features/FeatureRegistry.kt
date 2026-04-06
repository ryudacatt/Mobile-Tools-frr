package com.skids.idamobile.features

/**
 * Central place to register feature flags and capabilities as phases expand.
 */
object FeatureRegistry {
    val foundationReady: Boolean = true
    val apkInspectorEnabled: Boolean = true
    val websiteInspectorEnabled: Boolean = true
    val apkDebuggerEnabled: Boolean = true
    val assemblyViewerEnabled: Boolean = true
    val syntaxColorEngineEnabled: Boolean = true
    val stringsXrefsEnabled: Boolean = true
}
