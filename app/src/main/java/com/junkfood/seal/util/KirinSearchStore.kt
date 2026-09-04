package com.junkfood.seal.util

import android.content.Context
import java.io.File
import java.util.UUID
import org.json.JSONArray
import org.json.JSONObject

/** Lightweight local persistence for Kirin Search history and favourite searches. */
object KirinSearchStore {
    private const val ROOT_DIR = "kirin-search"
    private const val SEARCH_FILE = "searches.json"
    private const val MAX_SEARCHES = 30

    enum class SearchSource(val label: String) {
        YOUTUBE("YouTube"),
        YOUTUBE_MUSIC("YT Music"),
        BILIBILI("Bilibili"),
    }

    data class SearchRecord(
        val id: String,
        val query: String,
        val source: SearchSource,
        val favorite: Boolean,
        val updatedAt: Long,
    )

    private fun root(context: Context): File =
        File(context.filesDir, ROOT_DIR).apply { mkdirs() }

    private fun searchFile(context: Context): File = File(root(context), SEARCH_FILE)

    fun loadSearches(context: Context): List<SearchRecord> =
        readArray(searchFile(context))
            .mapNotNull { json ->
                runCatching {
                        SearchRecord(
                            id = json.optString("id").ifBlank { UUID.randomUUID().toString() },
                            query = json.getString("query"),
                            source =
                                runCatching { SearchSource.valueOf(json.optString("source")) }
                                    .getOrDefault(SearchSource.YOUTUBE),
                            favorite = json.optBoolean("favorite", false),
                            updatedAt = json.optLong("updatedAt", 0L),
                        )
                    }
                    .getOrNull()
            }
            .sortedWith(
                compareByDescending<SearchRecord> { it.favorite }
                    .thenByDescending { it.updatedAt }
            )

    fun addSearch(context: Context, query: String, source: SearchSource) {
        val clean = query.trim()
        if (clean.isBlank()) return

        val current = loadSearches(context).toMutableList()
        val existingIndex =
            current.indexOfFirst {
                it.query.equals(clean, ignoreCase = true) && it.source == source
            }
        val favorite =
            if (existingIndex >= 0) current[existingIndex].favorite else false
        if (existingIndex >= 0) current.removeAt(existingIndex)

        current.add(
            0,
            SearchRecord(
                id = UUID.randomUUID().toString(),
                query = clean,
                source = source,
                favorite = favorite,
                updatedAt = System.currentTimeMillis(),
            ),
        )
        saveSearches(context, trimSearches(current))
    }

    fun toggleFavorite(context: Context, id: String) {
        val updated =
            loadSearches(context).map { record ->
                if (record.id == id) {
                    record.copy(
                        favorite = !record.favorite,
                        updatedAt = System.currentTimeMillis(),
                    )
                } else {
                    record
                }
            }
        saveSearches(context, trimSearches(updated))
    }

    fun deleteSearch(context: Context, id: String) {
        saveSearches(context, loadSearches(context).filterNot { it.id == id })
    }

    /** Clears normal recent searches but intentionally keeps favourited searches. */
    fun clearRecentSearches(context: Context) {
        saveSearches(context, loadSearches(context).filter { it.favorite })
    }

    private fun trimSearches(records: List<SearchRecord>): List<SearchRecord> {
        val favorites = records.filter { it.favorite }.distinctBy { it.id }
        val recent =
            records.filterNot { it.favorite }
                .distinctBy { "${it.source.name}:${it.query.lowercase()}" }
                .take(MAX_SEARCHES)
        return (favorites + recent)
            .sortedWith(
                compareByDescending<SearchRecord> { it.favorite }
                    .thenByDescending { it.updatedAt }
            )
    }

    private fun saveSearches(context: Context, records: List<SearchRecord>) {
        val array = JSONArray()
        records.forEach { record ->
            array.put(
                JSONObject()
                    .put("id", record.id)
                    .put("query", record.query)
                    .put("source", record.source.name)
                    .put("favorite", record.favorite)
                    .put("updatedAt", record.updatedAt),
            )
        }
        writeArray(searchFile(context), array)
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
        val temp = File(file.parentFile, "${file.name}.tmp")
        temp.writeText(array.toString())
        if (file.exists()) file.delete()
        if (!temp.renameTo(file)) {
            temp.copyTo(file, overwrite = true)
            temp.delete()
        }
    }
}
