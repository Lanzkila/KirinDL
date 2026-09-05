package com.junkfood.seal.util

import android.content.Context
import android.net.Uri

/**
 * Remembers the last extractor that successfully handled a host for each runtime.
 *
 * This lets KirinDL distinguish a generic unsupported URL from a site which used to have a
 * working extractor but no longer matches after an engine update. The latter is reported as a
 * likely upstream extractor removal/disable instead of being retried forever.
 */
object ExtractorHealthUtil {
    enum class Engine(val key: String, val label: String) {
        YT_DLP("yt_dlp", "yt-dlp"),
        GALLERY_DL("gallery_dl", "gallery-dl"),
    }

    private const val PREFS_NAME = "kirindl_extractor_health"

    private val unavailableTokens =
        listOf(
            "unsupported url",
            "no suitable extractor",
            "no extractor",
            "extractor not found",
            "extractor is disabled",
            "extractor has been disabled",
            "no gallery-dl extractor matched",
            "no active extractor",
        )

    fun remember(
        context: Context,
        engine: Engine,
        url: String,
        extractor: String,
    ) {
        val host = hostKey(url) ?: return
        val cleanExtractor = extractor.trim()
        if (cleanExtractor.isBlank()) return
        context
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString("${engine.key}:$host", cleanExtractor)
            .apply()
    }

    fun previousExtractor(
        context: Context,
        engine: Engine,
        url: String,
    ): String? {
        val host = hostKey(url) ?: return null
        return context
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString("${engine.key}:$host", null)
            ?.takeIf { it.isNotBlank() }
    }

    fun looksUnavailable(message: String?): Boolean {
        val lower = message.orEmpty().lowercase()
        return unavailableTokens.any(lower::contains)
    }

    fun unavailableMessage(
        context: Context,
        engine: Engine,
        url: String,
        originalMessage: String? = null,
    ): String {
        val host = hostKey(url) ?: "this site"
        val previous = previousExtractor(context, engine, url)
        val headline =
            if (previous != null) {
                "${engine.label} extractor '$previous' previously handled $host but is no longer active."
            } else {
                "${engine.label} has no active extractor for $host."
            }
        val reason =
            if (previous != null) {
                "It may have been removed or disabled upstream in the current engine."
            } else {
                "The site may no longer be supported, or its extractor may have been removed upstream."
            }
        val detail =
            originalMessage
                ?.trim()
                ?.takeIf { it.isNotBlank() && !it.equals(headline, ignoreCase = true) }
                ?.let { " Engine detail: $it" }
                .orEmpty()
        return "$headline $reason$detail"
    }

    fun decorateFailure(
        context: Context,
        engine: Engine,
        url: String,
        error: Throwable,
    ): Throwable {
        if (!looksUnavailable(error.message)) return error
        return IllegalStateException(
            unavailableMessage(context, engine, url, error.message),
            error,
        )
    }

    private fun hostKey(url: String): String? {
        val normalized =
            if (url.startsWith("http://", true) || url.startsWith("https://", true)) {
                url
            } else {
                url.substringAfter(':', missingDelimiterValue = url)
            }
        return runCatching { Uri.parse(normalized).host }
            .getOrNull()
            ?.lowercase()
            ?.removePrefix("www.")
            ?.takeIf { it.isNotBlank() }
    }
}
