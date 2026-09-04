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
 * Lightweight discovery layer over KirinDL's existing yt-dlp runtime.
 *
 * Search never downloads media. Results are handed to the normal configure/queue flow, keeping
 * downloader routing, Aria2, Bilibili fallback and format logic untouched.
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
        val musicSongHint: Boolean = false,
        val viewCount: Long? = null,
        val uploadTimestamp: Long? = null,
    )

    private data class CacheEntry(
        val createdAt: Long,
        val items: List<ResultItem>,
    )

    private const val DEFAULT_LIMIT = 18
    private const val CACHE_TTL_MS = 5 * 60 * 1000L
    private const val MAX_CACHE_ENTRIES = 8
    private val cache = LinkedHashMap<String, CacheEntry>(MAX_CACHE_ENTRIES, 0.75f, true)
    private val processLock = Any()

    @Volatile private var activeProcessId: String? = null

    suspend fun search(
        query: String,
        source: KirinSearchStore.SearchSource,
        songsOnly: Boolean = true,
        limit: Int = DEFAULT_LIMIT,
    ): Result<List<ResultItem>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val clean = query.trim()
                require(clean.isNotBlank()) { "Enter a search query" }

                val boundedLimit = limit.coerceIn(8, 30)
                val cacheKey =
                    "${source.name}:${if (songsOnly) "songs" else "all"}:${clean.lowercase()}:$boundedLimit"
                getCached(cacheKey)?.let { return@runCatching it }

                // Song filtering needs a slightly wider lightweight result pool because obvious
                // lyric/live/video entries are discarded after flat metadata parsing.
                val discoveryLimit =
                    if (source == KirinSearchStore.SearchSource.YOUTUBE_MUSIC && songsOnly) {
                        (boundedLimit * 2).coerceAtMost(36)
                    } else {
                        boundedLimit
                    }

                val target =
                    when (source) {
                        KirinSearchStore.SearchSource.YOUTUBE -> "ytsearch$discoveryLimit:$clean"
                        KirinSearchStore.SearchSource.YOUTUBE_MUSIC ->
                            "https://music.youtube.com/search?q=" +
                                URLEncoder.encode(clean, StandardCharsets.UTF_8.toString())
                        KirinSearchStore.SearchSource.BILIBILI -> "bilisearch$discoveryLimit:$clean"
                    }

                val discovered =
                    executeDiscovery(
                        target = target,
                        source = source,
                        limit = discoveryLimit,
                    )
                val finalItems =
                    if (source == KirinSearchStore.SearchSource.YOUTUBE_MUSIC && songsOnly) {
                        discovered
                            .filter(::isFocusedSong)
                            .sortedByDescending(::musicPriority)
                            .take(boundedLimit)
                    } else {
                        discovered.take(boundedLimit)
                    }

                putCached(cacheKey, finalItems)
                finalItems
            }
        }

    /** Stops only the currently running Kirin Search discovery process, never a download task. */
    fun cancelActiveSearch() {
        val processId = synchronized(processLock) {
            val current = activeProcessId
            activeProcessId = null
            current
        }
        processId?.let { runCatching { YoutubeDL.destroyProcessById(it) } }
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
        request.addOption("--socket-timeout", 10)
        // Discovery should fail quickly instead of making the Search page feel frozen.
        request.addOption("--retries", 1)
        request.addOption("--extractor-retries", 1)

        val processId = "kirin-search-${UUID.randomUUID()}"
        val previousId = synchronized(processLock) {
            val previous = activeProcessId
            activeProcessId = processId
            previous
        }
        previousId?.let { runCatching { YoutubeDL.destroyProcessById(it) } }

        val response =
            try {
                YoutubeDL.getInstance().execute(request, processId)
            } finally {
                synchronized(processLock) {
                    if (activeProcessId == processId) activeProcessId = null
                }
            }

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

        val artist = entry.optString("artist")
        val track = entry.optString("track")
        val album = entry.optString("album")
        val creator =
            sequenceOf(
                    artist,
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
        val viewCount =
            if (entry.has("view_count") && !entry.isNull("view_count")) {
                entry.optLong("view_count").takeIf { it >= 0L }
            } else {
                null
            }
        val uploadTimestamp =
            when {
                entry.has("timestamp") && !entry.isNull("timestamp") ->
                    entry.optLong("timestamp").takeIf { it > 0L }
                entry.optString("upload_date").length == 8 ->
                    runCatching {
                            val date = entry.optString("upload_date")
                            java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.US)
                                .parse(date)
                                ?.time
                                ?.div(1000L)
                        }
                        .getOrNull()
                else -> null
            }

        return ResultItem(
            id = id.ifBlank { url },
            title = title.ifBlank { url },
            url = url,
            thumbnail = thumbnail?.takeIf { it.startsWith("http") },
            creator = creator,
            durationSeconds = duration,
            source = source,
            extractor = extractor,
            musicSongHint = artist.isNotBlank() || track.isNotBlank() || album.isNotBlank(),
            viewCount = viewCount,
            uploadTimestamp = uploadTimestamp,
        )
    }

    /**
     * Strict YT Music song focus. Strong music metadata is accepted, while obvious lyric/live/
     * cover/music-video results are removed. Topic and original/official audio/song labels rank
     * highest, matching KirinDL's audio-first search preference.
     */
    fun isMusicResult(item: ResultItem): Boolean {
        val title = item.title.lowercase()
        val creator = item.creator.lowercase()
        return item.musicSongHint ||
            creator.contains("topic") ||
            title.contains("official audio") ||
            title.contains("original audio") ||
            title.contains("official song") ||
            title.contains("original song")
    }

    private fun isFocusedSong(item: ResultItem): Boolean {
        val title = item.title.lowercase()
        val creator = item.creator.lowercase()
        val strongLabel =
            creator.contains("topic") ||
                title.contains("official audio") ||
                title.contains("original audio") ||
                title.contains("official song") ||
                title.contains("original song")
        if (strongLabel) return true

        val blockers =
            listOf(
                "lyrics",
                "lyric video",
                "official video",
                "music video",
                " live",
                "live ",
                "cover",
                "reaction",
                "tutorial",
                "karaoke",
                "shorts",
            )
        if (blockers.any { token -> title.contains(token) }) return false

        return item.musicSongHint &&
            (item.durationSeconds == null || item.durationSeconds in 30..1200)
    }

    private fun musicPriority(item: ResultItem): Int {
        val title = item.title.lowercase()
        val creator = item.creator.lowercase()
        var score = 0
        if (creator.contains("topic")) score += 100
        if (title.contains("official audio") || title.contains("original audio")) score += 90
        if (title.contains("official song") || title.contains("original song")) score += 80
        if (item.musicSongHint) score += 50
        if (item.durationSeconds != null) score += 5
        return score
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

    @Synchronized
    private fun getCached(key: String): List<ResultItem>? {
        pruneCacheLocked()
        val entry = cache[key] ?: return null
        if (System.currentTimeMillis() - entry.createdAt > CACHE_TTL_MS) {
            cache.remove(key)
            return null
        }
        return entry.items
    }

    @Synchronized
    private fun putCached(key: String, items: List<ResultItem>) {
        pruneCacheLocked()
        cache[key] = CacheEntry(System.currentTimeMillis(), items)
        while (cache.size > MAX_CACHE_ENTRIES) {
            val oldestKey = cache.entries.firstOrNull()?.key ?: break
            cache.remove(oldestKey)
        }
    }

    private fun pruneCacheLocked(now: Long = System.currentTimeMillis()) {
        val expired =
            cache.filterValues { entry -> now - entry.createdAt > CACHE_TTL_MS }.keys.toList()
        expired.forEach(cache::remove)
    }
}
