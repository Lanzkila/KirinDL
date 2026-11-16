package com.junkfood.seal.util.telegram.download

import android.content.Context
import android.media.MediaScannerConnection
import android.os.Environment
import com.junkfood.seal.util.telegram.TelegramPythonRuntime
import com.junkfood.seal.util.telegram.models.TelegramDownloadedMedia
import com.junkfood.seal.util.telegram.models.TelegramMedia
import java.io.File
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/** Downloads media URLs exposed by the public Telegram web preview. */
object TelegramDownloadEngine {
    fun defaultDownloadDirectory(context: Context): File {
        val parent =
            context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                ?: context.filesDir
        return File(parent, "KirinDL/Telegram")
    }

    suspend fun downloadSelected(
        context: Context,
        media: List<TelegramMedia>,
        destination: File = defaultDownloadDirectory(context),
    ): Result<List<TelegramDownloadedMedia>> =
        withContext(Dispatchers.IO) {
            runCatching {
                require(media.isNotEmpty()) { "Select at least one Telegram media item" }
                if (!destination.exists() && !destination.mkdirs()) {
                    throw IOException("Could not create Telegram download folder")
                }
                if (!destination.isDirectory) {
                    throw IOException("Telegram download destination is not a directory")
                }

                val payload =
                    JSONArray().apply {
                        media.forEach { item ->
                            put(
                                JSONObject()
                                    .put("url", item.downloadUrl)
                                    .put("file_name", item.fileName)
                                    .put("kind", item.kind)
                            )
                        }
                    }
                val raw =
                    TelegramPythonRuntime.bridge(context).callAttr(
                        "download",
                        payload.toString(),
                        destination.canonicalPath,
                    ).toString()
                val result = JSONObject(raw)
                val results = result.optJSONArray("results")
                val downloaded =
                    buildList {
                        if (results != null) {
                            for (index in 0 until results.length()) {
                                val item = results.optJSONObject(index) ?: continue
                                if (!item.optBoolean("ok", false)) continue
                                val path = item.optString("path")
                                if (path.isBlank()) continue
                                val file = File(path).canonicalFile
                                if (!file.isFile || !file.toPath().startsWith(destination.canonicalFile.toPath())) {
                                    continue
                                }
                                add(
                                    TelegramDownloadedMedia(
                                        sourceUrl = item.optString("url"),
                                        filePath = file.absolutePath,
                                        fileName = item.optString("file_name").ifBlank { file.name },
                                        mimeType = item.optString("mime_type"),
                                        sizeBytes = item.optLong("size_bytes"),
                                    )
                                )
                            }
                        }
                    }

                if (downloaded.isEmpty()) {
                    throw IOException(
                        result.optString("error").ifBlank {
                            "No Telegram media files were downloaded"
                        }
                    )
                }

                runCatching {
                    MediaScannerConnection.scanFile(
                        context,
                        downloaded.map { it.filePath }.toTypedArray(),
                        null,
                        null,
                    )
                }
                downloaded
            }
        }
}
