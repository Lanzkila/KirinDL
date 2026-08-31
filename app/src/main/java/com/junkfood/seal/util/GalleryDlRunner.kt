package com.junkfood.seal.util

import android.content.Context
import android.media.MediaScannerConnection
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import java.io.File
import java.io.IOException
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject

object GalleryDlRunner {
    private val runMutex = Mutex()

    data class DownloadResult(
        val version: String,
        val savedFiles: List<String>,
        val destinationDirectory: String,
    )

    suspend fun download(context: Context, url: String): Result<DownloadResult> =
        runMutex.withLock {
            withContext(Dispatchers.IO) {
                runCatching {
                    val trimmedUrl = url.trim()
                    if (!(trimmedUrl.startsWith("https://") || trimmedUrl.startsWith("http://"))) {
                        throw IllegalArgumentException("Enter a valid http(s) URL")
                    }

                    val engineDir = GalleryDlEngine.engineDirectory(context)
                    val installedVersion =
                        GalleryDlEngine.installedVersion(context)
                            ?: throw IllegalStateException("Install the gallery-dl engine first")

                    val compatibility = GalleryDlConfig.prepare(context)
                    val cookiesPath =
                        compatibility.cookiesFile
                            .takeIf { it.isFile && it.length() > 0L }
                            ?.absolutePath
                            .orEmpty()

                    val jobDir = File(context.cacheDir, "gallery-dl-jobs/${UUID.randomUUID()}")
                    val outputDir = File(jobDir, "output").apply { mkdirs() }

                    try {
                        if (!Python.isStarted()) {
                            Python.start(AndroidPlatform(context.applicationContext))
                        }
                        val bridge = Python.getInstance().getModule("gallery_bridge")
                        val raw =
                            bridge.callAttr(
                                    "download",
                                    trimmedUrl,
                                    outputDir.absolutePath,
                                    engineDir.absolutePath,
                                    compatibility.configFile.absolutePath,
                                    cookiesPath,
                                    compatibility.cacheFile.absolutePath,
                                )
                                .toString()
                        val result = JSONObject(raw)
                        if (!result.optBoolean("ok", false)) {
                            throw IOException(
                                result.optString("error").ifBlank {
                                    "gallery-dl could not download this URL"
                                }
                            )
                        }

                        val files = mutableListOf<File>()
                        val jsonFiles = result.optJSONArray("files")
                        if (jsonFiles != null) {
                            for (index in 0 until jsonFiles.length()) {
                                val file = File(jsonFiles.getString(index)).canonicalFile
                                if (
                                    file.isFile &&
                                        file.toPath().startsWith(outputDir.canonicalFile.toPath())
                                ) {
                                    files += file
                                }
                            }
                        }
                        if (files.isEmpty()) {
                            throw IOException("gallery-dl finished but no files were produced")
                        }

                        val destination =
                            File(FileUtil.getExternalDownloadDirectory(), "GalleryDL").apply {
                                mkdirs()
                            }
                        val saved = files.map { source -> exportFile(source, outputDir, destination) }
                        runCatching {
                            MediaScannerConnection.scanFile(
                                context,
                                saved.toTypedArray(),
                                null,
                                null,
                            )
                        }

                        DownloadResult(
                            version = result.optString("version").ifBlank { installedVersion },
                            savedFiles = saved,
                            destinationDirectory = destination.absolutePath,
                        )
                    } finally {
                        jobDir.deleteRecursively()
                    }
                }
            }
        }

    private fun exportFile(source: File, root: File, destinationRoot: File): String {
        val relative = source.relativeTo(root).path
        val desired = File(destinationRoot, relative).canonicalFile
        val rootPath = destinationRoot.canonicalFile.toPath()
        if (!desired.toPath().startsWith(rootPath)) {
            throw SecurityException("Unsafe gallery output path")
        }
        desired.parentFile?.mkdirs()
        val destination = uniqueFile(desired)
        source.copyTo(destination, overwrite = false)
        return destination.absolutePath
    }

    private fun uniqueFile(file: File): File {
        if (!file.exists()) return file
        val parent = file.parentFile ?: return file
        val extension = file.extension
        val base = file.nameWithoutExtension
        var index = 1
        while (true) {
            val name =
                if (extension.isBlank()) "$base ($index)" else "$base ($index).$extension"
            val candidate = File(parent, name)
            if (!candidate.exists()) return candidate
            index++
        }
    }
}
