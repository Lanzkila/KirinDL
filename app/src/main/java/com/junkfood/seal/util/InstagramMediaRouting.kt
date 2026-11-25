package com.junkfood.seal.util

import java.net.URI

/**
 * Chooses the engine for Instagram links submitted from Home.
 *
 * Instagram's /p/ route is ambiguous: it can contain a photo, carousel, or video. Such URLs are
 * probed with yt-dlp first and fall back to Gallery-DL when no video stream is found.
 */
object InstagramMediaRouting {
    enum class Route {
        NOT_INSTAGRAM,
        GALLERY_DL,
        YTDLP,
        PROBE_WITH_YTDLP,
    }

    private val instagramHosts = setOf("instagram.com", "www.instagram.com", "m.instagram.com")
    private val videoRoutes = setOf("reel", "reels", "tv", "live")

    fun isInstagramUrl(raw: String): Boolean {
        val uri = parse(raw) ?: return false
        return isInstagramHost(uri.host.orEmpty())
    }

    fun routeFor(raw: String): Route {
        val uri = parse(raw) ?: return Route.NOT_INSTAGRAM
        if (!isInstagramHost(uri.host.orEmpty())) return Route.NOT_INSTAGRAM

        val segments = uri.path.orEmpty()
            .split('/')
            .filter(String::isNotBlank)
            .map(String::lowercase)

        val first = segments.getOrNull(0).orEmpty()
        val second = segments.getOrNull(1).orEmpty()

        return when {
            first in videoRoutes -> Route.YTDLP
            // Instagram share URLs may wrap a Reel/Live route in /share/.
            first == "share" && second in videoRoutes -> Route.YTDLP
            // Some Live replay URLs use /<username>/live/<id>.
            second in videoRoutes -> Route.YTDLP
            first == "p" -> Route.PROBE_WITH_YTDLP
            else -> Route.GALLERY_DL
        }
    }

    fun hasVideoStream(formats: List<Format>?, directVideoCodec: String?): Boolean {
        val hasFormatVideo = formats.orEmpty().any(Format::containsVideo)
        val hasDirectVideo = !directVideoCodec.isNullOrBlank() &&
            !directVideoCodec.equals("none", ignoreCase = true)
        return hasFormatVideo || hasDirectVideo
    }

    private fun parse(raw: String): URI? =
        runCatching { URI(raw.trim()) }
            .getOrNull()
            ?.takeIf { it.scheme?.lowercase() in setOf("http", "https") }

    private fun isInstagramHost(host: String): Boolean {
        val normalized = host.lowercase()
        return normalized in instagramHosts || normalized.endsWith(".instagram.com")
    }
}
