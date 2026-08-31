package com.junkfood.seal.util

import android.content.Context
import java.io.File
import java.util.UUID
import org.json.JSONArray
import org.json.JSONObject

/** Persistent lightweight queue/history storage for Kirin Gallery Hub. */
object GalleryDlStore {
    private const val ROOT_DIR = "gallery-dl-state"
    private const val QUEUE_FILE = "queue.json"
    private const val HISTORY_FILE = "history.json"
    private const val MAX_HISTORY = 100

    data class QueueRecord(
        val id: String,
        val url: String,
        val state: String,
        val extractor: String = "",
        val error: String = "",
    )

    data class HistoryRecord(
        val id: String,
        val url: String,
        val extractor: String,
        val destination: String,
        val fileCount: Int,
        val success: Boolean,
        val error: String,
        val finishedAt: Long,
    )

    private fun root(context: Context): File =
        File(context.filesDir, ROOT_DIR).apply { mkdirs() }

    private fun queueFile(context: Context): File = File(root(context), QUEUE_FILE)

    private fun historyFile(context: Context): File = File(root(context), HISTORY_FILE)

    fun loadQueue(context: Context): List<QueueRecord> =
        readArray(queueFile(context)).mapNotNull { json ->
            runCatching {
                    QueueRecord(
                        id = json.optString("id").ifBlank { UUID.randomUUID().toString() },
                        url = json.getString("url"),
                        state = normalizeQueueState(json.optString("state")),
                        extractor = json.optString("extractor"),
                        error = json.optString("error"),
                    )
                }
                .getOrNull()
        }

    fun saveQueue(context: Context, records: List<QueueRecord>) {
        val array = JSONArray()
        records.forEach { record ->
            array.put(
                JSONObject()
                    .put("id", record.id)
                    .put("url", record.url)
                    .put("state", record.state)
                    .put("extractor", record.extractor)
                    .put("error", record.error)
            )
        }
        writeArray(queueFile(context), array)
    }

    fun loadHistory(context: Context): List<HistoryRecord> =
        readArray(historyFile(context)).mapNotNull { json ->
            runCatching {
                    HistoryRecord(
                        id = json.optString("id").ifBlank { UUID.randomUUID().toString() },
                        url = json.getString("url"),
                        extractor = json.optString("extractor"),
                        destination = json.optString("destination"),
                        fileCount = json.optInt("fileCount", 0),
                        success = json.optBoolean("success", false),
                        error = json.optString("error"),
                        finishedAt = json.optLong("finishedAt", 0L),
                    )
                }
                .getOrNull()
        }

    fun saveHistory(context: Context, records: List<HistoryRecord>) {
        val array = JSONArray()
        records.sortedByDescending { it.finishedAt }.take(MAX_HISTORY).forEach { record ->
            array.put(
                JSONObject()
                    .put("id", record.id)
                    .put("url", record.url)
                    .put("extractor", record.extractor)
                    .put("destination", record.destination)
                    .put("fileCount", record.fileCount)
                    .put("success", record.success)
                    .put("error", record.error)
                    .put("finishedAt", record.finishedAt)
            )
        }
        writeArray(historyFile(context), array)
    }

    fun clearHistory(context: Context) {
        historyFile(context).delete()
    }

    private fun normalizeQueueState(value: String): String =
        when (value.lowercase()) {
            "completed" -> "completed"
            "failed" -> "failed"
            else -> "pending"
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
