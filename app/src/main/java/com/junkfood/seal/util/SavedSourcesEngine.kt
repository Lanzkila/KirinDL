package com.junkfood.seal.util

import android.content.Context
import android.net.Uri
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * Discovery-only browser for Saved Sources.
 *
 * It uses the existing yt-dlp runtime only to list media. Actual downloading still goes through
 * KirinDL's normal configure / queue path.
 */
object SavedSourcesEngine {
    data class BrowseResult(
        val title: String,
        val thumbnail: String?,
        val creator: String,
        val fetchedAt: Long,
        val fromCache: Boolean,
        val items: List<SavedSourceStore.SourceItem>,
    )

    suspend fun browse(
        context: Context,
        source: SavedSourceStore.SavedSource,
        forceRefresh: Boolean = false,
        limit: Int = 50,
    ): Result<BrowseResult> =
        withContext(Dispatchers.IO) {
            runCatching {
                if (!forceRefresh) {
                    SavedSourceStore.loadFreshCache(context, source.id)?.let { cache ->
                        return@runCatching BrowseResult(
                            title = cache.title.ifBlank { source.displayTitle },
                            thumbnail = cache.thumbnail,
                            creator = cache.creator,
                            fetchedAt = cache.fetchedAt,
                            fromCache = true,
                            items = cache.items,
                        )
                    }
                }

                val result = executeBrowse(source, limit)
                SavedSourceStore.saveCache(
                    context,
                    SavedSourceStore.CacheRecord(
                        sourceId = source.id,
                        title = result.title,
                        thumbnail = result.thumbnail,
                        creator = result.creator,
                        fetchedAt = result.fetchedAt,
                        items = result.items,
                    ),
                )
                SavedSourceStore.updateMetadata(
                    context = context,
                    id = source.id,
                    title = result.title,
                    thumbnail = result.thumbnail,
                    itemCount = result.items.size,
                    fetchedAt = result.fetchedAt,
                )
                result
            }
        }

    fun classifySourceUrl(text: String): SavedSourceStore.SourceKind? {
        val raw = text.trim()
        if (raw.isBlank()) return null
        val uri = runCatching { Uri.parse(raw) }.getOrNull() ?: return null
        val scheme = uri.scheme?.lowercase()
        if (scheme != "http" && scheme != "https") return null
        val host = uri.host?.lowercase()?.removePrefix("www.") ?: return null
        val path = uri.path.orEmpty()
        val lowerPath = path.lowercase()

        if (KirinSearchEngine.looksLikeDirectVideoUrl(raw)) return null

        return when {
            host == "music.youtube.com" -> {
                when {
                    lowerPath == "/playlist" && !uri.getQueryParameter("list").isNullOrBlank() ->
                        SavedSourceStore.SourceKind.YOUTUBE_MUSIC_COLLECTION
                    lowerPath.startsWith("/browse/") ->
                        SavedSourceStore.SourceKind.YOUTUBE_MUSIC_COLLECTION
                    else -> null
                }
            }

            host.endsWith("youtube.com") -> {
                when {
                    lowerPath == "/playlist" && !uri.getQueryParameter("list").isNullOrBlank() ->
                        SavedSourceStore.SourceKind.YOUTUBE_PLAYLIST
                    lowerPath.startsWith("/channel/") ||
                        lowerPath.startsWith("/@") ||
                        lowerPath.startsWith("/c/") ||
                        lowerPath.startsWith("/user/") ->
                        SavedSourceStore.SourceKind.YOUTUBE_CHANNEL
                    else -> null
                }
            }

            host == "space.bilibili.com" -> {
                val firstSegment = path.trim('/').substringBefore('/').trim()
                when {
                    firstSegment.toLongOrNull() == null -> null
                    lowerPath.contains("/lists/") || lowerPath.contains("/favlist") ->
                        SavedSourceStore.SourceKind.BILIBILI_COLLECTION
                    else -> SavedSourceStore.SourceKind.BILIBILI_SPACE
                }
            }

            host.endsWith("bilibili.com") -> {
                when {
                    lowerPath.startsWith("/medialist/") ||
                        lowerPath.startsWith("/list/") ||
                        lowerPath.startsWith("/favlist") ->
                        SavedSourceStore.SourceKind.BILIBILI_COLLECTION
                    else -> null
                }
            }

            else -> null
        }
    }

    fun validationMessage(text: String): String {
        val raw = text.trim()
        if (raw.isBlank()) return "Paste a channel, playlist or collection URL"
        if (KirinSearchEngine.looksLikeDirectVideoUrl(raw)) {
            return "Direct video URLs belong in Kirin Search or Home, not Saved Sources"
        }
        return "Supported: YouTube channel/playlist, YT Music playlist/album, Bilibili space/collection"
    }

    private fun executeBrowse(
        source: SavedSourceStore.SavedSource,
        limit: Int,
    ): BrowseResult {
        val request = YoutubeDLRequest(source.url)
        request.addOption("--flat-playlist")
        request.addOption("--dump-single-json")
        request.addOption("--playlist-end", limit)
        request.addOption("--ignore-errors")
        request.addOption("--no-warnings")
        request.addOption("--socket-timeout", 15)
        request.addOption("--retries", 3)
        request.addOption("--extractor-retries", 3)

        val processId = "kirin-source-${UUID.randomUUID()}"
        val response = YoutubeDL.getInstance().execute(request, processId)
        val raw = response.out.trim()
        if (raw.isBlank()) {
            return BrowseResult(
                title = source.displayTitle,
                thumbnail = source.thumbnail.takeIf { it.startsWith("http") },
                creator = "",
                fetchedAt = System.currentTimeMillis(),
                fromCache = false,
                items = emptyList(),
            )
        }

        val root = JSONObject(raw)
        val entries = root.optJSONArray("entries") ?: JSONArray().put(root)
        val items = buildList {
                for (index in 0 until entries.length()) {
                    val entry = entries.optJSONObject(index) ?: continue
                    parseItem(entry, source.kind)?.let(::add)
                }
            }
            .distinctBy { it.url }
            .take(limit)

        val title =
            sequenceOf(
                    root.optString("title"),
                    root.optString("playlist_title"),
                    root.optString("channel"),
                    root.optString("uploader"),
                )
                .firstOrNull { it.isNotBlank() }
                ?: source.displayTitle

        val creator =
            sequenceOf(
                    root.optString("channel"),
                    root.optString("uploader"),
                    root.optString("artist"),
                )
                .firstOrNull { it.isNotBlank() }
                .orEmpty()

        val thumbnail = extractThumbnail(root) ?: source.thumbnail.takeIf { it.startsWith("http") }
        val fetchedAt = System.currentTimeMillis()

        return BrowseResult(
            title = title,
            thumbnail = thumbnail,
            creator = creator,
            fetchedAt = fetchedAt,
            fromCache = false,
            items = items,
        )
    }

    private fun parseItem(
        entry: JSONObject,
        kind: SavedSourceStore.SourceKind,
    ): SavedSourceStore.SourceItem? {
        val id = entry.optString("id")
        val title = entry.optString("title").ifBlank { entry.optString("fulltitle") }
        val rawUrl =
            entry.optString("webpage_url").ifBlank {
                entry.optString("original_url").ifBlank { entry.optString("url") }
            }
        val url = normalizeMediaUrl(rawUrl, id, kind) ?: return null
        val creator =
            sequenceOf(
                    entry.optString("artist"),
                    entry.optString("channel"),
                    entry.optString("uploader"),
                    entry.optString("uploader_id"),
                )
                .firstOrNull { it.isNotBlank() }
                .orEmpty()
        val duration =
            if (entry.has("duration") && !entry.isNull("duration")) {
                entry.optDouble("duration").takeIf { !it.isNaN() && it >= 0.0 }?.toInt()
            } else {
                null
            }
        val extractor =
            entry.optString("extractor_key").ifBlank { entry.optString("extractor") }
        val thumbnail =
            extractThumbnail(entry)
                ?: youtubeThumbnailFallback(id, kind)

        return SavedSourceStore.SourceItem(
            id = id.ifBlank { url },
            title = title.ifBlank { url },
            url = url,
            thumbnail = thumbnail,
            creator = creator,
            durationSeconds = duration,
            extractor = extractor,
        )
    }

    private fun normalizeMediaUrl(
        rawUrl: String,
        id: String,
        kind: SavedSourceStore.SourceKind,
    ): String? {
        if (rawUrl.startsWith("http://") || rawUrl.startsWith("https://")) {
            if (KirinSearchEngine.looksLikeDirectVideoUrl(rawUrl)) return rawUrl
            if (
                (kind == SavedSourceStore.SourceKind.YOUTUBE_CHANNEL ||
                    kind == SavedSourceStore.SourceKind.YOUTUBE_PLAYLIST ||
                    kind == SavedSourceStore.SourceKind.YOUTUBE_MUSIC_COLLECTION) &&
                    id.isNotBlank() && !id.startsWith("UC")
            ) {
                return "https://www.youtube.com/watch?v=$id"
            }
            return null
        }

        return when (kind) {
            SavedSourceStore.SourceKind.YOUTUBE_CHANNEL,
            SavedSourceStore.SourceKind.YOUTUBE_PLAYLIST,
            SavedSourceStore.SourceKind.YOUTUBE_MUSIC_COLLECTION ->
                id.takeIf { it.isNotBlank() && !it.startsWith("UC") }
                    ?.let { "https://www.youtube.com/watch?v=$it" }

            SavedSourceStore.SourceKind.BILIBILI_SPACE,
            SavedSourceStore.SourceKind.BILIBILI_COLLECTION -> {
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

    private fun extractThumbnail(json: JSONObject): String? {
        json.optString("thumbnail").takeIf { it.startsWith("http") }?.let { return it }
        val thumbnails = json.optJSONArray("thumbnails") ?: return null
        for (index in thumbnails.length() - 1 downTo 0) {
            val url = thumbnails.optJSONObject(index)?.optString("url")
            if (!url.isNullOrBlank() && url.startsWith("http")) return url
        }
        return null
    }

    private fun youtubeThumbnailFallback(
        id: String,
        kind: SavedSourceStore.SourceKind,
    ): String? =
        if (
            id.isNotBlank() &&
                (kind == SavedSourceStore.SourceKind.YOUTUBE_CHANNEL ||
                    kind == SavedSourceStore.SourceKind.YOUTUBE_PLAYLIST ||
                    kind == SavedSourceStore.SourceKind.YOUTUBE_MUSIC_COLLECTION)
        ) {
            "https://i.ytimg.com/vi/$id/hqdefault.jpg"
        } else {
            null
        }
}
