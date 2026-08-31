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
import org.json.JSONArray
import org.json.JSONObject

object GalleryDlRunner {
    private val runMutex = Mutex()
    private val prefixedHttpUrl = Regex("""^[A-Za-z0-9_.-]+:https?://.+""")

    data class ExtractorInfo(
        val supported: Boolean,
        val baseCategory: String,
        val category: String,
        val subcategory: String,
        val className: String,
    ) {
        val label: String
            get() =
                listOf(category, subcategory)
                    .filter { it.isNotBlank() }
                    .joinToString(" / ")
                    .ifBlank { className }
    }

    data class RuntimeDiagnostics(
        val engineVersion: String,
        val readyModules: List<String>,
        val missingOptionalModules: List<String>,
    )

    data class DownloadResult(
        val version: String,
        val savedFiles: List<String>,
        val destinationDirectory: String,
        val extractorLabel: String,
        val configLoaded: Boolean,
        val cookiesLoaded: Boolean,
    )

    fun isCandidateUrl(value: String): Boolean {
        val trimmed = value.trim()
        return trimmed.startsWith("https://") ||
            trimmed.startsWith("http://") ||
            prefixedHttpUrl.matches(trimmed)
    }

    suspend fun inspectUrl(context: Context, url: String): Result<ExtractorInfo> =
        runMutex.withLock {
            withContext(Dispatchers.IO) {
                runCatching {
                    val trimmedUrl = url.trim()
                    if (!isCandidateUrl(trimmedUrl)) {
                        throw IllegalArgumentException("Enter a valid gallery-dl URL")
                    }

                    val installedVersion =
                        GalleryDlEngine.installedVersion(context)
                            ?: throw IllegalStateException("Install the gallery-dl engine first")
                    if (installedVersion.isBlank()) {
                        throw IllegalStateException("Install the gallery-dl engine first")
                    }

                    val compatibility = GalleryDlConfig.prepare(context)
                    val cookiesPath =
                        compatibility.cookiesFile
                            .takeIf { it.isFile && it.length() > 0L }
                            ?.absolutePath
                            .orEmpty()

                    val bridge = getBridge(context)
                    val raw =
                        bridge.callAttr(
                                "inspect_url",
                                trimmedUrl,
                                GalleryDlEngine.engineDirectory(context).absolutePath,
                                compatibility.configFile.absolutePath,
                                cookiesPath,
                                compatibility.cacheFile.absolutePath,
                            )
                            .toString()
                    val result = JSONObject(raw)

                    if (!result.optBoolean("ok", false)) {
                        throw IOException(
                            result.optString("error").ifBlank {
                                "Could not inspect this URL"
                            }
                        )
                    }

                    ExtractorInfo(
                        supported = result.optBoolean("supported", false),
                        baseCategory = result.optString("base_category"),
                        category = result.optString("category"),
                        subcategory = result.optString("subcategory"),
                        className = result.optString("class_name"),
                    )
                }
            }
        }

    suspend fun diagnostics(context: Context): Result<RuntimeDiagnostics> =
        runMutex.withLock {
            withContext(Dispatchers.IO) {
                runCatching {
                    val installedVersion =
                        GalleryDlEngine.installedVersion(context)
                            ?: throw IllegalStateException("Install the gallery-dl engine first")

                    val bridge = getBridge(context)
                    val raw =
                        bridge.callAttr(
                                "diagnostics",
                                GalleryDlEngine.engineDirectory(context).absolutePath,
                            )
                            .toString()
                    val result = JSONObject(raw)

                    if (!result.optBoolean("ok", false)) {
                        throw IOException(
                            result.optString("error").ifBlank {
                                "Could not inspect gallery-dl runtime"
                            }
                        )
                    }

                    RuntimeDiagnostics(
                        engineVersion =
                            result.optString("version").ifBlank { installedVersion },
                        readyModules = jsonArrayToList(result.optJSONArray("ready")),
                        missingOptionalModules =
                            jsonArrayToList(result.optJSONArray("missing_optional")),
                    )
                }
            }
        }

    suspend fun download(context: Context, url: String): Result<DownloadResult> =
        runMutex.withLock {
            withContext(Dispatchers.IO) {
                runCatching {
                    val trimmedUrl = url.trim()
                    if (!isCandidateUrl(trimmedUrl)) {
                        throw IllegalArgumentException("Enter a valid gallery-dl URL")
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
                        val bridge = getBridge(context)
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
                            val extractor = result.optString("extractor")
                            val error =
                                result.optString("error").ifBlank {
                                    "gallery-dl could not download this URL"
                                }
                            throw IOException(
                                if (extractor.isBlank()) error else "$extractor: $error"
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
                            extractorLabel = result.optString("extractor"),
                            configLoaded = result.optBoolean("config_loaded", false),
                            cookiesLoaded = result.optBoolean("cookies_loaded", false),
                        )
                    } finally {
                        jobDir.deleteRecursively()
                    }
                }
            }
        }

    private fun getBridge(context: Context): com.chaquo.python.PyObject {
        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(context.applicationContext))
        }
        return Python.getInstance().getModule("gallery_bridge")
    }

    private fun jsonArrayToList(array: JSONArray?): List<String> {
        if (array == null) return emptyList()
        return buildList {
            for (index in 0 until array.length()) {
                val value = array.optString(index)
                if (value.isNotBlank()) add(value)
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
