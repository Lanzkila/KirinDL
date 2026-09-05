package com.junkfood.seal.util

import android.net.Uri
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import java.io.IOException
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * Lightweight discovery layer over KirinDL's existing yt-dlp runtime.
 *
 * Search never downloads media. Results are handed to the normal configure/queue flow, keeping
 * downloader routing, Aria2, Bilibili fallback and format logic untouched.
 */
object KirinSearchEngine {
    enum class YoutubeContent {
        ALL,
        VIDEO,
        MUSIC,
    }

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
    private val bilibiliClient =
        OkHttpClient.Builder()
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(8, TimeUnit.SECONDS)
            .build()
    private val processLock = Any()

    @Volatile private var activeProcessId: String? = null
    @Volatile private var activeBilibiliCall: Call? = null

    suspend fun search(
        query: String,
        source: KirinSearchStore.SearchSource,
        songsOnly: Boolean = true,
        youtubeContent: YoutubeContent = YoutubeContent.ALL,
        limit: Int = DEFAULT_LIMIT,
    ): Result<List<ResultItem>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val clean = query.trim()
                require(clean.isNotBlank()) { "Enter a search query" }

                val boundedLimit = limit.coerceIn(8, 30)
                val cacheKey =
                    "${source.name}:${if (songsOnly) "songs" else "all"}:${youtubeContent.name}:" +
                        "${clean.lowercase()}:$boundedLimit"
                getCached(cacheKey)?.let { return@runCatching it }

                val finalItems =
                    when (source) {
                        KirinSearchStore.SearchSource.YOUTUBE ->
                            searchYouTube(
                                query = clean,
                                content = youtubeContent,
                                limit = boundedLimit,
                            )

                        KirinSearchStore.SearchSource.YOUTUBE_MUSIC ->
                            if (songsOnly) {
                                searchFocusedYouTubeMusicSongs(clean, boundedLimit)
                            } else {
                                searchYouTubeMusicAll(clean, boundedLimit)
                            }

                        KirinSearchStore.SearchSource.BILIBILI ->
                            runCatching { searchBilibiliRich(clean, boundedLimit) }
                                .getOrElse { error ->
                                    if (
                                        error is IOException &&
                                            error.message.orEmpty()
                                                .contains("canceled", ignoreCase = true)
                                    ) {
                                        throw error
                                    }
                                    executeDiscovery(
                                        target = "bilisearch$boundedLimit:$clean",
                                        source = KirinSearchStore.SearchSource.BILIBILI,
                                        limit = boundedLimit,
                                    )
                                }
                    }
                        .distinctBy { it.url }
                        .take(boundedLimit)

                putCached(cacheKey, finalItems)
                finalItems
            }
        }

    private fun searchYouTube(
        query: String,
        content: YoutubeContent,
        limit: Int,
    ): List<ResultItem> {
        val poolLimit =
            when (content) {
                YoutubeContent.ALL -> limit
                YoutubeContent.VIDEO -> (limit * 2).coerceAtMost(30)
                YoutubeContent.MUSIC -> (limit + 10).coerceAtMost(30)
            }
        val primary =
            executeDiscovery(
                target = "ytsearch$poolLimit:$query",
                source = KirinSearchStore.SearchSource.YOUTUBE,
                limit = poolLimit,
            )

        return when (content) {
            YoutubeContent.ALL -> primary.take(limit)
            YoutubeContent.VIDEO -> primary.filterNot(::isMusicResult).take(limit)
            YoutubeContent.MUSIC -> {
                val music = primary.filter(::isMusicResult).sortedByDescending(::musicPriority)
                if (music.size >= minOf(5, limit)) {
                    music.take(limit)
                } else {
                    val extra =
                        runCatching {
                                executeDiscovery(
                                    target = "ytsearch$poolLimit:$query official audio",
                                    source = KirinSearchStore.SearchSource.YOUTUBE,
                                    limit = poolLimit,
                                )
                            }
                            .getOrDefault(emptyList())
                    (music + extra.filter(::isMusicResult))
                        .distinctBy { it.url }
                        .sortedByDescending(::musicPriority)
                        .take(limit)
                }
            }
        }
    }

    /**
     * Songs-only intentionally uses YouTube search instead of the fragile music.youtube.com
     * search endpoint. Topic / official / original audio results are then ranked locally.
     */
    private fun searchFocusedYouTubeMusicSongs(query: String, limit: Int): List<ResultItem> {
        val poolLimit = (limit + 12).coerceAtMost(30)
        val focusedQueries =
            listOf(
                "$query official audio",
                "$query original audio",
                "$query topic",
            )

        val focused =
            focusedQueries
                .flatMap { focusedQuery ->
                    runCatching {
                            executeDiscovery(
                                target = "ytsearch$poolLimit:$focusedQuery",
                                source = KirinSearchStore.SearchSource.YOUTUBE_MUSIC,
                                limit = poolLimit,
                            )
                        }
                        .getOrDefault(emptyList())
                }
                .distinctBy { it.url }
                .filter(::isFocusedSong)
                .sortedByDescending(::musicPriority)

        if (focused.size >= minOf(5, limit)) {
            return focused.take(limit)
        }

        val fallback =
            runCatching {
                    executeDiscovery(
                        target = "ytsearch$poolLimit:$query",
                        source = KirinSearchStore.SearchSource.YOUTUBE_MUSIC,
                        limit = poolLimit,
                    )
                }
                .getOrDefault(emptyList())
                .filter(::isFocusedSong)

        return (focused + fallback)
            .distinctBy { it.url }
            .sortedByDescending(::musicPriority)
            .take(limit)
    }

    private fun searchYouTubeMusicAll(query: String, limit: Int): List<ResultItem> {
        val target =
            "https://music.youtube.com/search?q=" +
                URLEncoder.encode(query, StandardCharsets.UTF_8.toString())
        return runCatching {
                executeDiscovery(
                    target = target,
                    source = KirinSearchStore.SearchSource.YOUTUBE_MUSIC,
                    limit = limit,
                )
            }
            .getOrElse {
                executeDiscovery(
                    target = "ytsearch$limit:$query",
                    source = KirinSearchStore.SearchSource.YOUTUBE_MUSIC,
                    limit = limit,
                )
            }
            .take(limit)
    }

    /**
     * Bilibili's yt-dlp flat search entries are intentionally minimal. Query the same public
     * video-search endpoint directly for rich card metadata, then fall back to bilisearch if the
     * endpoint is unavailable. This remains discovery-only and never changes download routing.
     */
    private fun searchBilibiliRich(query: String, limit: Int): List<ResultItem> {
        val encoded = URLEncoder.encode(query, StandardCharsets.UTF_8.toString())
        val url =
            "https://api.bilibili.com/x/web-interface/search/type" +
                "?Search_key=$encoded&keyword=$encoded&page=1&context=&duration=0" +
                "&tids_2=&__refresh__=true&search_type=video&tids=0&highlight=1"
        val request =
            Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 KirinDL/3.1")
                .header("Referer", "https://www.bilibili.com/")
                .header("Cookie", "buvid3=${UUID.randomUUID()}infoc")
                .build()

        val call = bilibiliClient.newCall(request)
        synchronized(processLock) {
            activeBilibiliCall?.cancel()
            activeBilibiliCall = call
        }
        val body =
            try {
                call.execute().use { response ->
                    if (!response.isSuccessful) {
                        throw IllegalStateException("Bilibili search HTTP ${response.code}")
                    }
                    response.body?.string()?.takeIf { it.isNotBlank() }
                        ?: throw IllegalStateException(
                            "Bilibili search returned an empty response"
                        )
                }
            } finally {
                synchronized(processLock) {
                    if (activeBilibiliCall === call) activeBilibiliCall = null
                }
            }
        val root = JSONObject(body)
        if (root.optInt("code", -1) != 0) {
            throw IllegalStateException(
                root.optString("message").ifBlank { "Bilibili search unavailable" }
            )
        }
        val entries = root.optJSONObject("data")?.optJSONArray("result") ?: return emptyList()

        return buildList {
                for (index in 0 until entries.length()) {
                    val entry = entries.optJSONObject(index) ?: continue
                    val bvid = entry.optString("bvid")
                    val aid = entry.optString("aid")
                    val rawUrl = entry.optString("arcurl")
                    val mediaUrl =
                        rawUrl.takeIf { it.startsWith("http") }
                            ?: bvid.takeIf { it.isNotBlank() }?.let {
                                "https://www.bilibili.com/video/$it"
                            }
                            ?: continue
                    val rawThumb = entry.optString("pic")
                    val thumbnail =
                        when {
                            rawThumb.startsWith("//") -> "https:$rawThumb"
                            rawThumb.startsWith("http") -> rawThumb
                            else -> null
                        }
                    add(
                        ResultItem(
                            id = bvid.ifBlank { aid.ifBlank { mediaUrl } },
                            title = cleanBilibiliText(entry.optString("title")).ifBlank { mediaUrl },
                            url = mediaUrl,
                            thumbnail = thumbnail,
                            creator = cleanBilibiliText(entry.optString("author")),
                            durationSeconds = parseBilibiliDuration(entry.opt("duration")),
                            source = KirinSearchStore.SearchSource.BILIBILI,
                            extractor = "BiliBiliSearch",
                            viewCount = parseLongValue(entry.opt("play")),
                            uploadTimestamp = parseLongValue(entry.opt("pubdate")),
                        )
                    )
                }
            }
            .distinctBy { it.url }
            .take(limit)
    }

    private fun cleanBilibiliText(value: String): String =
        value
            .replace(Regex("<[^>]+>"), "")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .trim()

    private fun parseLongValue(value: Any?): Long? =
        when (value) {
            is Number -> value.toLong().takeIf { it >= 0L }
            is String -> value.replace(",", "").toLongOrNull()?.takeIf { it >= 0L }
            else -> null
        }

    private fun parseBilibiliDuration(value: Any?): Int? {
        if (value is Number) return value.toInt().takeIf { it >= 0 }
        val text = value?.toString()?.trim().orEmpty()
        if (text.isBlank()) return null
        text.toIntOrNull()?.let { return it.takeIf { seconds -> seconds >= 0 } }
        val parts = text.split(':').mapNotNull { it.toIntOrNull() }
        if (parts.isEmpty()) return null
        return when (parts.size) {
            2 -> parts[0] * 60 + parts[1]
            3 -> parts[0] * 3600 + parts[1] * 60 + parts[2]
            else -> null
        }
    }

    /** Stops only the currently running Kirin Search discovery process, never a download task. */
    fun cancelActiveSearch() {
        val (processId, biliCall) = synchronized(processLock) {
            val currentProcess = activeProcessId
            val currentBiliCall = activeBilibiliCall
            activeProcessId = null
            activeBilibiliCall = null
            currentProcess to currentBiliCall
        }
        processId?.let { runCatching { YoutubeDL.destroyProcessById(it) } }
        biliCall?.cancel()
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
                "performance",
                "visualizer",
                "teaser",
                "trailer",
            )
        if (blockers.any { token -> title.contains(token) }) return false

        val topicChannel =
            creator == "topic" ||
                creator.endsWith(" - topic") ||
                creator.endsWith(" topic")
        val audioOrSongLabel =
            title.contains("official audio") ||
                title.contains("original audio") ||
                title.contains("official song") ||
                title.contains("original song")

        // Songs-only is intentionally strict: generic music metadata alone is not enough,
        // because YouTube music-video entries frequently expose artist/track metadata too.
        return topicChannel || audioOrSongLabel
    }

    private fun musicPriority(item: ResultItem): Int {
        val title = item.title.lowercase()
        val creator = item.creator.lowercase()
        var score = 0
        if (creator == "topic" || creator.endsWith(" - topic") || creator.endsWith(" topic")) {
            score += 120
        }
        if (title.contains("official audio") || title.contains("original audio")) score += 100
        if (title.contains("official song") || title.contains("original song")) score += 90
        if (item.musicSongHint) score += 20
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
