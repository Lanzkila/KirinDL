package com.junkfood.seal.util

import com.yausername.youtubedl_android.YoutubeDLRequest
import java.util.Locale

/** yt-dlp replaces repeated extractor bodies; compose them before passing overrides. */
internal object ExtractorArguments {
    fun merge(request: YoutubeDLRequest, overrides: String) {
        if (overrides.isBlank()) return
        val bodies = linkedMapOf<String, LinkedHashMap<String, String>>()
        val overrideLines = overrides.lineSequence().filter { it.isNotBlank() }.toList()
        val touched = overrideLines.filter { ':' in it }
            .map { it.substringBefore(':').trim().lowercase(Locale.ROOT) }.toSet()
        val existing = request.getArguments("--extractor-args").orEmpty().filterNotNull()
        for (argument in existing + overrideLines) {
            val separator = argument.indexOf(':')
            if (separator <= 0) continue
            val extractor = argument.substring(0, separator).trim().lowercase(Locale.ROOT)
            val options = bodies.getOrPut(extractor) { linkedMapOf() }
            argument.substring(separator + 1).split(';').filter { it.isNotBlank() }.forEach {
                val key = it.substringBefore('=').trim().lowercase(Locale.ROOT).replace('-', '_')
                options[key] = it.trim()
            }
        }
        bodies.forEach { (extractor, options) ->
            if (extractor in touched) {
                request.addOption("--extractor-args", "$extractor:${options.values.joinToString(";")}")
            }
        }
    }

    fun hasPlayerClient(request: YoutubeDLRequest): Boolean =
        request.getArguments("--extractor-args").orEmpty().filterNotNull().any { argument ->
            argument.substringBefore(':').equals("youtube", ignoreCase = true) &&
                argument.substringAfter(':', "").split(';').any {
                    it.substringBefore('=').trim().lowercase(Locale.ROOT).replace('-', '_') == "player_client"
                }
        }
}

internal fun isYouTubeVerificationError(url: String, error: Throwable): Boolean {
    if (!isYouTubeUrl(url)) return false
    val message = error.message.orEmpty()
    return message.contains("sign in to confirm", ignoreCase = true) &&
        message.contains("bot", ignoreCase = true)
}
