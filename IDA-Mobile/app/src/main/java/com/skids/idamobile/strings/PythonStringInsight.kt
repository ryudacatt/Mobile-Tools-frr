package com.skids.idamobile.strings

/**
 * Ranked Python-side heuristic insight for an extracted string.
 */
data class PythonStringInsight(
    val value: String,
    val score: Double,
    val reasons: List<String>
)

