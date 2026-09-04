package com.junkfood.seal.util

import android.net.Uri
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * Discovery-only layer over the app's existing yt-dlp runtime.
 *
 * This never downloads media itself. Search results are handed back to the normal KirinDL
 * configure/queue flow so downloader routing, Aria2, Bilibili fallback and format logic stay
 * untouched.
 */
object KirinSearchEngine {
    data class ResultItem(
        val id: String,
        val title: String,
        val url: String,
        val thumbnail: String?,
        val creator: String,
        val durationSeconds: Int?,
        val source: KirinSearchStore.SearchSource,
        val extractor: String,
    )

    suspend fun search(
        query: String,
        source: KirinSearchStore.SearchSource,
        limit: Int = 24,
    ): Result<List<ResultItem>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val clean = query.trim()
                require(clean.isNotBlank()) { "Enter a search query" }

                val target =
                    when (source) {
                        KirinSearchStore.SearchSource.YOUTUBE -> "ytsearch$limit:$clean"
                        KirinSearchStore.SearchSource.YOUTUBE_MUSIC ->
                            "https://music.youtube.com/search?q=" +
                                URLEncoder.encode(clean, StandardCharsets.UTF_8.toString())
                        KirinSearchStore.SearchSource.BILIBILI -> "bilisearch$limit:$clean"
                    }

                executeDiscovery(target = target, source = source, limit = limit)
            }
        }

    private fun executeDiscovery(
        target: String,
        source: KirinSearchStore.SearchSource,
        limit: Int,
    ): List<ResultItem> {
        val request = YoutubeDLRequest(target)
        request.addOption("--flat-playlist")
        request.addOption("--dump-single-json")
        request.addOption("--playlist-end", limit)
        request.addOption("--ignore-errors")
        request.addOption("--no-warnings")
        request.addOption("--socket-timeout", 15)
        request.addOption("--retries", 3)
        request.addOption("--extractor-retries", 3)

        val processId = "kirin-search-${UUID.randomUUID()}"
        val response = YoutubeDL.getInstance().execute(request, processId)
        val raw = response.out.trim()
        if (raw.isBlank()) return emptyList()

        val root = JSONObject(raw)
        val entries = root.optJSONArray("entries") ?: JSONArray().put(root)
        return buildList {
                for (index in 0 until entries.length()) {
                    val entry = entries.optJSONObject(index) ?: continue
                    parseEntry(entry, source)?.let(::add)
                }
            }
            .distinctBy { it.url }
            .take(limit)
    }

    private fun parseEntry(
        entry: JSONObject,
        source: KirinSearchStore.SearchSource,
    ): ResultItem? {
        val id = entry.optString("id")
        val title = entry.optString("title").ifBlank { entry.optString("fulltitle") }
        val rawUrl =
            entry.optString("webpage_url").ifBlank {
                entry.optString("original_url").ifBlank { entry.optString("url") }
            }
        val url = normalizeUrl(rawUrl, id, source) ?: return null

        val creator =
            sequenceOf(
                    entry.optString("artist"),
                    entry.optString("channel"),
                    entry.optString("uploader"),
                    entry.optString("uploader_id"),
                )
                .firstOrNull { it.isNotBlank() }
                .orEmpty()

        val thumbnail =
            entry.optString("thumbnail").takeIf { it.startsWith("http") }
                ?: entry.optJSONArray("thumbnails")?.let { thumbnails ->
                    if (thumbnails.length() > 0) {
                        thumbnails.optJSONObject(thumbnails.length() - 1)?.optString("url")
                    } else {
                        null
                    }
                }
                ?: youtubeThumbnailFallback(id, source)

        val duration =
            if (entry.has("duration") && !entry.isNull("duration")) {
                entry.optDouble("duration").takeIf { !it.isNaN() && it >= 0.0 }?.toInt()
            } else {
                null
            }

        val extractor =
            entry.optString("extractor_key").ifBlank { entry.optString("extractor") }

        return ResultItem(
            id = id.ifBlank { url },
            title = title.ifBlank { url },
            url = url,
            thumbnail = thumbnail?.takeIf { it.startsWith("http") },
            creator = creator,
            durationSeconds = duration,
            source = source,
            extractor = extractor,
        )
    }

    private fun youtubeThumbnailFallback(
        id: String,
        source: KirinSearchStore.SearchSource,
    ): String? =
        if (
            id.isNotBlank() &&
                (source == KirinSearchStore.SearchSource.YOUTUBE ||
                    source == KirinSearchStore.SearchSource.YOUTUBE_MUSIC)
        ) {
            "https://i.ytimg.com/vi/$id/hqdefault.jpg"
        } else {
            null
        }

    private fun normalizeUrl(
        rawUrl: String,
        id: String,
        source: KirinSearchStore.SearchSource,
    ): String? {
        if (rawUrl.startsWith("http://") || rawUrl.startsWith("https://")) {
            if (looksLikeDirectVideoUrl(rawUrl)) return rawUrl
            // Search pages can expose channels/playlists alongside media. Part 2 is intentionally
            // video/song-first, so non-video navigation entries are discarded here.
            if (
                (source == KirinSearchStore.SearchSource.YOUTUBE ||
                    source == KirinSearchStore.SearchSource.YOUTUBE_MUSIC) &&
                    id.length in 8..15 && !id.startsWith("UC")
            ) {
                return "https://www.youtube.com/watch?v=$id"
            }
            return null
        }

        return when (source) {
            KirinSearchStore.SearchSource.YOUTUBE,
            KirinSearchStore.SearchSource.YOUTUBE_MUSIC ->
                id.takeIf { it.isNotBlank() }?.let { "https://www.youtube.com/watch?v=$it" }
            KirinSearchStore.SearchSource.BILIBILI -> {
                val candidate = rawUrl.ifBlank { id }
                when {
                    candidate.startsWith("BV", ignoreCase = true) ->
                        "https://www.bilibili.com/video/$candidate"
                    candidate.startsWith("av", ignoreCase = true) ->
                        "https://www.bilibili.com/video/$candidate"
                    else -> null
                }
            }
        }
    }

    fun looksLikeDirectVideoUrl(text: String): Boolean {
        val uri = runCatching { Uri.parse(text.trim()) }.getOrNull() ?: return false
        val host = uri.host?.lowercase()?.removePrefix("www.") ?: return false
        val path = uri.path.orEmpty().lowercase()

        return when {
            host == "youtu.be" -> true
            host.endsWith("youtube.com") &&
                (path == "/watch" || path.startsWith("/shorts/") || path.startsWith("/live/")) ->
                true
            host.endsWith("bilibili.com") && path.startsWith("/video/") -> true
            else -> false
        }
    }
}
