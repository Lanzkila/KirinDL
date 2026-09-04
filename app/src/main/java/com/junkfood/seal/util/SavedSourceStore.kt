package com.junkfood.seal.util

import android.content.Context
import java.io.File
import java.util.UUID
import org.json.JSONArray
import org.json.JSONObject

/** Persistent local storage for Kirin Search Saved Sources and their lightweight browse cache. */
object SavedSourceStore {
    private const val ROOT_DIR = "kirin-search"
    private const val SOURCES_FILE = "saved-sources.json"
    private const val LAST_OPENED_FILE = "last-opened-source.txt"
    private const val CACHE_DIR = "source-cache"
    const val CACHE_MAX_AGE_MS = 30L * 60L * 1000L
    private const val CACHE_RETENTION_MS = 7L * 24L * 60L * 60L * 1000L

    enum class SourceKind(val label: String) {
        YOUTUBE_CHANNEL("YouTube Channel"),
        YOUTUBE_PLAYLIST("YouTube Playlist"),
        YOUTUBE_MUSIC_COLLECTION("YT Music Collection"),
        BILIBILI_SPACE("Bilibili Space"),
        BILIBILI_COLLECTION("Bilibili Collection"),
    }

    data class SavedSource(
        val id: String,
        val url: String,
        val kind: SourceKind,
        val customName: String,
        val resolvedTitle: String,
        val thumbnail: String,
        val itemCount: Int,
        val pinned: Boolean,
        val order: Int,
        val createdAt: Long,
        val lastFetchedAt: Long,
    ) {
        val displayTitle: String
            get() = customName.ifBlank { resolvedTitle.ifBlank { kind.label } }
    }

    data class SourceItem(
        val id: String,
        val title: String,
        val url: String,
        val thumbnail: String?,
        val creator: String,
        val durationSeconds: Int?,
        val extractor: String,
    )

    data class CacheRecord(
        val sourceId: String,
        val title: String,
        val thumbnail: String?,
        val creator: String,
        val fetchedAt: Long,
        val items: List<SourceItem>,
    )

    private fun root(context: Context): File =
        File(context.filesDir, ROOT_DIR).apply { mkdirs() }

    private fun sourcesFile(context: Context): File = File(root(context), SOURCES_FILE)

    private fun lastOpenedFile(context: Context): File = File(root(context), LAST_OPENED_FILE)

    private fun cacheDir(context: Context): File =
        File(root(context), CACHE_DIR).apply { mkdirs() }

    private fun cacheFile(context: Context, sourceId: String): File =
        File(cacheDir(context), "$sourceId.json")

    fun loadSources(context: Context): List<SavedSource> =
        readArray(sourcesFile(context))
            .mapNotNull { json ->
                runCatching {
                        SavedSource(
                            id = json.optString("id").ifBlank { UUID.randomUUID().toString() },
                            url = json.getString("url"),
                            kind =
                                runCatching { SourceKind.valueOf(json.getString("kind")) }
                                    .getOrDefault(SourceKind.YOUTUBE_PLAYLIST),
                            customName = json.optString("customName"),
                            resolvedTitle = json.optString("resolvedTitle"),
                            thumbnail = json.optString("thumbnail"),
                            itemCount = json.optInt("itemCount", 0),
                            pinned = json.optBoolean("pinned", false),
                            order = json.optInt("order", Int.MAX_VALUE),
                            createdAt = json.optLong("createdAt", 0L),
                            lastFetchedAt = json.optLong("lastFetchedAt", 0L),
                        )
                    }
                    .getOrNull()
            }
            .sortedWith(
                compareByDescending<SavedSource> { it.pinned }
                    .thenBy { it.order }
                    .thenByDescending { it.createdAt }
            )

    fun findDuplicate(context: Context, url: String): SavedSource? {
        val comparable = normalizeComparableUrl(url)
        if (comparable.isBlank()) return null
        return loadSources(context).firstOrNull {
            normalizeComparableUrl(it.url) == comparable
        }
    }

    fun addSource(
        context: Context,
        url: String,
        kind: SourceKind,
        customName: String = "",
    ): SavedSource {
        val cleanUrl = url.trim()
        val current = loadSources(context).toMutableList()
        val comparable = normalizeComparableUrl(cleanUrl)
        current.firstOrNull { normalizeComparableUrl(it.url) == comparable }?.let { existing ->
            if (customName.isNotBlank() && customName != existing.customName) {
                renameSource(context, existing.id, customName)
                return loadSources(context).first { it.id == existing.id }
            }
            return existing
        }

        val nextOrder = (current.maxOfOrNull { it.order.takeIf { value -> value < Int.MAX_VALUE } ?: 0 } ?: 0) + 1
        val source =
            SavedSource(
                id = UUID.randomUUID().toString(),
                url = cleanUrl,
                kind = kind,
                customName = customName.trim(),
                resolvedTitle = "",
                thumbnail = "",
                itemCount = 0,
                pinned = false,
                order = nextOrder,
                createdAt = System.currentTimeMillis(),
                lastFetchedAt = 0L,
            )
        current.add(source)
        saveSources(context, current)
        return source
    }

    fun renameSource(context: Context, id: String, name: String) {
        updateSource(context, id) { it.copy(customName = name.trim()) }
    }

    fun togglePinned(context: Context, id: String) {
        val current = loadSources(context)
        val target = current.firstOrNull { it.id == id } ?: return
        val nextPinned = !target.pinned
        val nextOrder =
            current.filter { it.pinned == nextPinned && it.id != id }.maxOfOrNull { it.order }?.plus(1)
                ?: 0
        saveSources(
            context,
            current.map { source ->
                if (source.id == id) source.copy(pinned = nextPinned, order = nextOrder) else source
            },
        )
    }

    fun moveSource(context: Context, id: String, direction: Int) {
        if (direction == 0) return
        val current = loadSources(context).toMutableList()
        val target = current.firstOrNull { it.id == id } ?: return
        val group = current.filter { it.pinned == target.pinned }.sortedBy { it.order }.toMutableList()
        val index = group.indexOfFirst { it.id == id }
        if (index < 0) return
        val nextIndex = (index + direction).coerceIn(0, group.lastIndex)
        if (nextIndex == index) return
        val other = group[nextIndex]
        val updated =
            current.map { source ->
                when (source.id) {
                    target.id -> source.copy(order = other.order)
                    other.id -> source.copy(order = target.order)
                    else -> source
                }
            }
        saveSources(context, updated)
    }

    fun deleteSource(context: Context, id: String) {
        saveSources(context, loadSources(context).filterNot { it.id == id })
        cacheFile(context, id).delete()
        if (getLastOpenedSourceId(context) == id) lastOpenedFile(context).delete()
    }

    fun updateMetadata(
        context: Context,
        id: String,
        title: String,
        thumbnail: String?,
        itemCount: Int,
        fetchedAt: Long,
    ) {
        updateSource(context, id) { source ->
            source.copy(
                resolvedTitle = title.ifBlank { source.resolvedTitle },
                thumbnail = thumbnail?.takeIf { it.startsWith("http") } ?: source.thumbnail,
                itemCount = itemCount,
                lastFetchedAt = fetchedAt,
            )
        }
    }

    fun setLastOpenedSource(context: Context, id: String) {
        lastOpenedFile(context).writeText(id)
    }

    fun getLastOpenedSourceId(context: Context): String? =
        lastOpenedFile(context).takeIf { it.isFile }?.readText()?.trim()?.takeIf { it.isNotBlank() }

    fun cacheAgeMs(source: SavedSource, now: Long = System.currentTimeMillis()): Long? =
        source.lastFetchedAt.takeIf { it > 0L }?.let { (now - it).coerceAtLeast(0L) }

    fun isCacheFresh(source: SavedSource, now: Long = System.currentTimeMillis()): Boolean =
        cacheAgeMs(source, now)?.let { it <= CACHE_MAX_AGE_MS } == true

    fun cleanupCache(context: Context) {
        val validSourceIds = loadSources(context).mapTo(mutableSetOf()) { it.id }
        val now = System.currentTimeMillis()
        cacheDir(context).listFiles()?.forEach { file ->
            val sourceId = file.name.removeSuffix(".json")
            val tooOld = now - file.lastModified() > CACHE_RETENTION_MS
            if (sourceId !in validSourceIds || tooOld) file.delete()
        }
    }

    fun saveCache(context: Context, cache: CacheRecord) {
        val root =
            JSONObject()
                .put("sourceId", cache.sourceId)
                .put("title", cache.title)
                .put("thumbnail", cache.thumbnail.orEmpty())
                .put("creator", cache.creator)
                .put("fetchedAt", cache.fetchedAt)
        val items = JSONArray()
        cache.items.take(50).forEach { item ->
            items.put(
                JSONObject()
                    .put("id", item.id)
                    .put("title", item.title)
                    .put("url", item.url)
                    .put("thumbnail", item.thumbnail.orEmpty())
                    .put("creator", item.creator)
                    .put("durationSeconds", item.durationSeconds ?: -1)
                    .put("extractor", item.extractor),
            )
        }
        root.put("items", items)
        atomicWrite(cacheFile(context, cache.sourceId), root.toString())
    }

    fun loadFreshCache(
        context: Context,
        sourceId: String,
        maxAgeMs: Long = CACHE_MAX_AGE_MS,
    ): CacheRecord? {
        val cache = loadCache(context, sourceId) ?: return null
        val age = System.currentTimeMillis() - cache.fetchedAt
        return cache.takeIf { age in 0..maxAgeMs }
    }

    fun loadCache(context: Context, sourceId: String): CacheRecord? {
        val file = cacheFile(context, sourceId)
        if (!file.isFile || file.length() <= 0L) return null
        return runCatching {
                val json = JSONObject(file.readText())
                val itemsJson = json.optJSONArray("items") ?: JSONArray()
                val items = buildList {
                    for (index in 0 until itemsJson.length()) {
                        val item = itemsJson.optJSONObject(index) ?: continue
                        val duration = item.optInt("durationSeconds", -1).takeIf { it >= 0 }
                        add(
                            SourceItem(
                                id = item.optString("id").ifBlank { item.optString("url") },
                                title = item.optString("title").ifBlank { item.optString("url") },
                                url = item.getString("url"),
                                thumbnail = item.optString("thumbnail").takeIf { it.startsWith("http") },
                                creator = item.optString("creator"),
                                durationSeconds = duration,
                                extractor = item.optString("extractor"),
                            ),
                        )
                    }
                }
                CacheRecord(
                    sourceId = sourceId,
                    title = json.optString("title"),
                    thumbnail = json.optString("thumbnail").takeIf { it.startsWith("http") },
                    creator = json.optString("creator"),
                    fetchedAt = json.optLong("fetchedAt", 0L),
                    items = items,
                )
            }
            .getOrNull()
    }

    private fun updateSource(context: Context, id: String, update: (SavedSource) -> SavedSource) {
        saveSources(
            context,
            loadSources(context).map { source -> if (source.id == id) update(source) else source },
        )
    }

    private fun saveSources(context: Context, records: List<SavedSource>) {
        val array = JSONArray()
        records.forEach { source ->
            array.put(
                JSONObject()
                    .put("id", source.id)
                    .put("url", source.url)
                    .put("kind", source.kind.name)
                    .put("customName", source.customName)
                    .put("resolvedTitle", source.resolvedTitle)
                    .put("thumbnail", source.thumbnail)
                    .put("itemCount", source.itemCount)
                    .put("pinned", source.pinned)
                    .put("order", source.order)
                    .put("createdAt", source.createdAt)
                    .put("lastFetchedAt", source.lastFetchedAt),
            )
        }
        writeArray(sourcesFile(context), array)
    }

    private fun readArray(file: File): List<JSONObject> {
        if (!file.isFile || file.length() <= 0L) return emptyList()
        return runCatching {
                val array = JSONArray(file.readText())
                buildList {
                    for (index in 0 until array.length()) {
                        array.optJSONObject(index)?.let(::add)
                    }
                }
            }
            .getOrElse { emptyList() }
    }

    private fun writeArray(file: File, array: JSONArray) {
        atomicWrite(file, array.toString())
    }

    private fun atomicWrite(file: File, text: String) {
        file.parentFile?.mkdirs()
        val temp = File(file.parentFile, "${file.name}.tmp")
        temp.writeText(text)
        if (file.exists()) file.delete()
        if (!temp.renameTo(file)) {
            temp.copyTo(file, overwrite = true)
            temp.delete()
        }
    }

    private fun normalizeComparableUrl(url: String): String =
        url.trim().removeSuffix("/").lowercase()
}
