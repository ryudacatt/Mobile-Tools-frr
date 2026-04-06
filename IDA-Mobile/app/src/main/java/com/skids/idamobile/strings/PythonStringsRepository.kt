package com.skids.idamobile.strings

import android.content.Context
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import org.json.JSONArray

/**
 * Executes bundled Python heuristics to rank extracted strings by security relevance.
 */
class PythonStringsRepository {
    fun analyze(context: Context, records: List<StringRecord>): Result<List<PythonStringInsight>> = runCatching {
        ensurePythonStarted(context)
        val python = Python.getInstance()
        val module = python.getModule("analysis_tools")

        val candidateStrings = records
            .map { it.entry.value }
            .filter { it.isNotBlank() }
            .take(MAX_INPUT_STRINGS)

        val rawJson = module.callAttr("rank_strings_json", candidateStrings, MAX_OUTPUT_INSIGHTS).toString()
        val json = JSONArray(rawJson)
        val insights = mutableListOf<PythonStringInsight>()

        for (index in 0 until json.length()) {
            val item = json.getJSONObject(index)
            val reasonsJson = item.optJSONArray("reasons") ?: JSONArray()
            val reasons = mutableListOf<String>()
            for (reasonIndex in 0 until reasonsJson.length()) {
                reasons += reasonsJson.optString(reasonIndex)
            }

            insights += PythonStringInsight(
                value = item.optString("value"),
                score = item.optDouble("score", 0.0),
                reasons = reasons
            )
        }

        insights
    }

    private fun ensurePythonStarted(context: Context) {
        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(context.applicationContext))
        }
    }

    private companion object {
        private const val MAX_INPUT_STRINGS = 5000
        private const val MAX_OUTPUT_INSIGHTS = 300
    }
}

