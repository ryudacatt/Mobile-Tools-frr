package com.skids.idamobile.features.websites

import java.net.URL
import java.security.MessageDigest
import okhttp3.OkHttpClient
import okhttp3.Request

class WebsiteInspectorRepository(
    private val client: OkHttpClient = OkHttpClient()
) {
    fun inspect(urlInput: String): Result<WebsiteReport> = runCatching {
        val normalizedUrl = normalizeUrl(urlInput)
        val request = Request.Builder()
            .url(normalizedUrl)
            .header("User-Agent", "IDA-Mobile-Inspector/0.2")
            .build()

        client.newCall(request).execute().use { response ->
            val sampleBytes = response.peekBody(MAX_SAMPLE_BYTES.toLong()).bytes()
            val charset = response.body?.contentType()?.charset(Charsets.UTF_8) ?: Charsets.UTF_8
            val html = sampleBytes.toString(charset)
            val title = extractTitle(html)
            val scriptCount = SCRIPT_REGEX.findAll(html).count()

            WebsiteReport(
                normalizedUrl = normalizedUrl,
                statusCode = response.code,
                contentType = response.header("Content-Type").orEmpty(),
                sampledBytes = sampleBytes.size,
                title = title,
                scriptTagCount = scriptCount,
                sha256 = sha256(sampleBytes)
            )
        }
    }

    private fun normalizeUrl(urlInput: String): String {
        val trimmed = urlInput.trim()
        require(trimmed.isNotEmpty()) { "Website URL is required." }
        val withScheme = if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            trimmed
        } else {
            "https://$trimmed"
        }

        // URL constructor validates syntax before network request.
        URL(withScheme)
        return withScheme
    }

    private fun extractTitle(html: String): String? {
        val match = TITLE_REGEX.find(html) ?: return null
        val raw = match.groupValues[1].replace("\\s+".toRegex(), " ").trim()
        return raw.ifBlank { null }
    }

    private fun sha256(content: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(content)
        return buildString(digest.size * 2) {
            digest.forEach { byte -> append("%02x".format(byte.toInt() and 0xff)) }
        }
    }

    private companion object {
        private const val MAX_SAMPLE_BYTES = 1_048_576
        private val TITLE_REGEX = Regex("<title[^>]*>(.*?)</title>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
        private val SCRIPT_REGEX = Regex("<script\\b", RegexOption.IGNORE_CASE)
    }
}
