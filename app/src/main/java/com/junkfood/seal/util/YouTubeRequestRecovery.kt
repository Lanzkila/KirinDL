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
        extractorArgs: String = "",
        runRequest: (YoutubeDLRequest) -> T,
    ): T {
        ExtractorArguments.merge(request, extractorArgs)
        val explicitClient = ExtractorArguments.hasPlayerClient(request)
        cancellationCheck()
        val firstError = try {
            return runRequest(request)
        } catch (error: Exception) {
            if (!isYouTubeUrl(url) || !isRecoverable(error)) throw error
            error
        }

        cancellationCheck()
        if (!explicitClient) addFallbackClients(request)
        // An IPv6 route can be challenged independently of IPv4. Respect either
        // address-family choice when the user or command already supplied one.
        val changedRoute = isBotChallenge(firstError) &&
            listOf("-4", "--force-ipv4", "-6", "--force-ipv6").none(request::hasOption)
        if (changedRoute) request.addOption("-4")
        val fallbackError = if (explicitClient && !changedRoute) {
            firstError
        } else {
            onRetry(if (changedRoute) "Retrying YouTube over IPv4..."
                else "Retrying YouTube with another playback client...")
            cancellationCheck()
            try {
                return runRequest(request)
            } catch (error: Exception) {
                if (!isRecoverable(error)) {
                    if (changedRoute && !isCancellation(error) && isNetworkFailure(error)) {
                        // Keep the verification action available if this device has no
                        // working IPv4 route; another retry on that route cannot help.
                        if (error !== firstError) firstError.addSuppressed(error)
                        throw firstError
                    }
                    throw error
                }
                error
            }
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
        if (isCancellation(error)) return false
        val message = error.message.orEmpty()
        return isBotChallenge(error) ||
            message.contains("HTTP Error 403", ignoreCase = true) ||
            message.contains("The page needs to be reloaded", ignoreCase = true) ||
            message.contains("Requested format is not available", ignoreCase = true)
    }

    private fun isCancellation(error: Exception): Boolean =
        error is YoutubeDL.CanceledException || error is CancellationException ||
            error is InterruptedException

    private fun isNetworkFailure(error: Exception): Boolean =
        listOf("timed out", "Unable to connect", "Connection reset", "Network is unreachable",
            "Failed to establish", "HTTP Error 5").any {
            error.message.orEmpty().contains(it, ignoreCase = true)
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
        ExtractorArguments.merge(request, "youtube:player_client=$FALLBACK_CLIENTS")
    }
}
