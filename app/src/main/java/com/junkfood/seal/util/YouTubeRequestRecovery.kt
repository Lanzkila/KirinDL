package com.junkfood.seal.util

import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import java.net.URI
import java.util.Locale
import java.util.concurrent.CancellationException

internal fun isYouTubeUrl(url: String): Boolean {
    val host = runCatching { URI(url.trim()).host.orEmpty().lowercase(Locale.ROOT) }
        .getOrDefault("")
    return host == "youtu.be" || host == "youtube.com" || host.endsWith(".youtube.com") ||
        host == "youtube-nocookie.com" || host.endsWith(".youtube-nocookie.com")
}

/** Short links and embedded-player profiles share YouTube's browser cookie store. */
internal fun cookieProfileUrl(url: String): String {
    val httpsUrl = when {
        url.startsWith("https://", ignoreCase = true) -> url
        url.startsWith("http://", ignoreCase = true) -> "https://${url.substring(7)}"
        else -> "https://$url"
    }
    return if (isYouTubeUrl(httpsUrl)) "https://www.youtube.com/" else httpsUrl
}

/** Bounded recovery for ordinary probes/transfers; custom commands keep their own behavior. */
internal object YouTubeRequestRecovery {
    // Keep the engine's defaults, adding clients with browser-session/HLS support.
    // Do not force android_vr, missing-PO-token formats, or a lower-quality selector.
    private const val FALLBACK_CLIENTS = "default,web_safari,web_embedded"

    fun <T> execute(
        url: String,
        request: YoutubeDLRequest,
        cancellationCheck: () -> Unit = {},
        onRetry: (String) -> Unit = {},
        runRequest: (YoutubeDLRequest) -> T,
    ): T {
        cancellationCheck()
        val firstError = try {
            return runRequest(request)
        } catch (error: Exception) {
            if (!isYouTubeUrl(url) || !isRecoverable(error)) throw error
            error
        }

        cancellationCheck()
        addFallbackClients(request)
        onRetry("Retrying YouTube with another playback client...")
        cancellationCheck()
        val fallbackError = try {
            return runRequest(request)
        } catch (error: Exception) {
            if (!isRecoverable(error)) throw error
            error
        }

        // A stale logged-in session can itself trigger the bot wall. Try only a direct
        // video as a guest, without changing the cookie preference or deleting profiles.
        // Private playlists, age restrictions and membership errors never enter this path.
        if (!request.hasOption("--cookies") ||
            !isBotChallenge(fallbackError) || !isDirectVideo(url, request.hasOption("--no-playlist"))) {
            throw fallbackError
        }
        cancellationCheck()
        request.addOption("--no-cookies")
        onRetry("Retrying this YouTube video without the saved cookie session...")
        cancellationCheck()
        return try {
            runRequest(request)
        } catch (error: Exception) {
            // Preserve the last engine error and the original verification diagnosis.
            if (error !== firstError && isRecoverable(error)) error.addSuppressed(firstError)
            throw error
        }
    }

    private fun isBotChallenge(error: Throwable): Boolean {
        val message = error.message.orEmpty()
        return message.contains("sign in to confirm", ignoreCase = true) &&
            message.contains("bot", ignoreCase = true)
    }

    private fun isRecoverable(error: Exception): Boolean {
        if (error is YoutubeDL.CanceledException || error is CancellationException ||
            error is InterruptedException) return false
        val message = error.message.orEmpty()
        return isBotChallenge(error) ||
            message.contains("HTTP Error 403", ignoreCase = true) ||
            message.contains("The page needs to be reloaded", ignoreCase = true) ||
            message.contains("Requested format is not available", ignoreCase = true)
    }

    private fun isDirectVideo(url: String, noPlaylist: Boolean): Boolean = runCatching {
        val uri = URI(url.trim())
        val query = uri.rawQuery.orEmpty().split('&')
        if (!noPlaylist && query.any { it.substringBefore('=') == "list" }) return@runCatching false
        val host = uri.host.orEmpty().lowercase(Locale.ROOT)
        val path = uri.path.orEmpty().trim('/').split('/')
        val id = when {
            host == "youtu.be" && path.size == 1 -> path[0]
            path == listOf("watch") -> query.firstOrNull { it.startsWith("v=") }?.substring(2)
            path.size == 2 && path[0] in listOf("shorts", "embed", "live", "v") -> path[1]
            else -> null
        }
        id?.matches(Regex("[A-Za-z0-9_-]{11}")) == true
    }.getOrDefault(false)

    private fun addFallbackClients(request: YoutubeDLRequest) {
        val parts = request.getArguments("--extractor-args").orEmpty()
            .filterNotNull()
            .filter { it.substringBefore(':').equals("youtube", ignoreCase = true) }
            .flatMap { it.substringAfter(':', "").split(';') }
            .filter { it.isNotBlank() }
            .filterNot {
                it.substringBefore('=').trim().lowercase(Locale.ROOT).replace('-', '_') ==
                    "player_client"
            }
        // yt-dlp replaces a repeated extractor's whole argument body. Merge first so
        // subtitle/comment/PO-token settings survive this temporary client override.
        request.addOption(
            "--extractor-args",
            "youtube:${(parts + "player_client=$FALLBACK_CLIENTS").joinToString(";")}",
        )
    }
}
