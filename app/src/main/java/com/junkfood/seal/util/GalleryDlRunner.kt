package com.junkfood.seal.util

import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
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
    private val unsafeFileChars = Regex("""[\u0000-\u001F\\/:*?"<>|]""")
    private val repeatedWhitespace = Regex("""\s+""")

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
                        val outputRoot = outputDir.canonicalFile
                        if (jsonFiles != null) {
                            for (index in 0 until jsonFiles.length()) {
                                val file = File(jsonFiles.getString(index)).canonicalFile
                                if (
                                    file.isFile &&
                                        file.toPath().startsWith(outputRoot.toPath())
                                ) {
                                    files += file
                                }
                            }
                        }
                        if (files.isEmpty()) {
                            throw IOException("gallery-dl finished but no files were produced")
                        }

                        val galleryRoot =
                            File(FileUtil.getExternalDownloadDirectory(), "GalleryDL").apply {
                                mkdirs()
                            }
                        val destination =
                            buildGalleryDestination(
                                galleryRoot = galleryRoot,
                                inputUrl = trimmedUrl,
                                extractorLabel = result.optString("extractor"),
                            )

                        // Do not export gallery-dl's raw nested path directly. Every result is
                        // flattened into the app-created [Site]/[Gallery] folder using a sanitized
                        // filename. Files are still accepted only when their canonical source path
                        // is inside the private temporary job sandbox.
                        val saved = files.map { source -> exportFileSafely(source, destination) }

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

    /** Build Download/GalleryDL/[Site]/[Gallery]/ from the real HTTP(S) URL. */
    private fun buildGalleryDestination(
        galleryRoot: File,
        inputUrl: String,
        extractorLabel: String,
    ): File {
        val root = galleryRoot.canonicalFile
        val realUrl = extractHttpUrl(inputUrl)
        val uri = Uri.parse(realUrl)

        val siteName =
            sanitizePathSegment(
                uri.host
                    ?.removePrefix("www.")
                    ?.substringBefore(':')
                    .orEmpty(),
                fallback = "Unknown Site",
            )

        val pathSegments =
            uri.pathSegments
                .map { it.trim() }
                .filter { it.isNotBlank() }

        val lastSegment = pathSegments.lastOrNull().orEmpty()
        val galleryCandidate =
            when {
                lastSegment.isNotBlank() && !looksLikeSingleMediaFile(lastSegment) ->
                    lastSegment
                pathSegments.size >= 2 ->
                    pathSegments[pathSegments.lastIndex - 1]
                else ->
                    firstUsefulQueryId(uri)
                        ?: extractorLabel.takeIf { it.isNotBlank() }
                        ?: "Gallery"
            }

        val galleryName = sanitizePathSegment(galleryCandidate, fallback = "Gallery")

        val siteDir = File(root, siteName).canonicalFile
        val galleryDir = File(siteDir, galleryName).canonicalFile
        val rootPath = root.toPath()

        if (
            !siteDir.toPath().startsWith(rootPath) ||
                !galleryDir.toPath().startsWith(rootPath)
        ) {
            throw SecurityException("Could not create a safe GalleryDL destination")
        }

        if (!galleryDir.exists() && !galleryDir.mkdirs() && !galleryDir.isDirectory) {
            throw IOException("Could not create GalleryDL destination folder")
        }

        return galleryDir
    }

    private fun extractHttpUrl(value: String): String {
        val httpsIndex = value.indexOf("https://")
        val httpIndex = value.indexOf("http://")
        val start =
            listOf(httpsIndex, httpIndex)
                .filter { it >= 0 }
                .minOrNull()
                ?: throw IllegalArgumentException("Enter a valid gallery-dl URL")
        return value.substring(start)
    }

    private fun firstUsefulQueryId(uri: Uri): String? =
        listOf("id", "gid", "gallery", "album", "post")
            .firstNotNullOfOrNull { key ->
                uri.getQueryParameter(key)?.trim()?.takeIf { it.isNotBlank() }
            }

    private fun looksLikeSingleMediaFile(value: String): Boolean {
        val lower = value.lowercase()
        return listOf(
                ".jpg",
                ".jpeg",
                ".png",
                ".webp",
                ".gif",
                ".bmp",
                ".avif",
                ".mp4",
                ".webm",
                ".mov",
            )
            .any(lower::endsWith)
    }

    private fun sanitizePathSegment(
        value: String,
        fallback: String,
        maxLength: Int = 96,
    ): String {
        val cleaned =
            value
                .replace(unsafeFileChars, "_")
                .replace(repeatedWhitespace, " ")
                .trim()
                .trim('.')
                .take(maxLength)
                .trim()
                .trim('.')

        return cleaned
            .takeIf { it.isNotBlank() && it != "." && it != ".." }
            ?: fallback
    }

    private fun safeFileName(originalName: String): String {
        val dot = originalName.lastIndexOf('.')
        val hasExtension = dot > 0 && dot < originalName.lastIndex

        val rawBase =
            if (hasExtension) {
                originalName.substring(0, dot)
            } else {
                originalName
            }
        val rawExtension =
            if (hasExtension) {
                originalName.substring(dot + 1)
            } else {
                ""
            }

        val base = sanitizePathSegment(rawBase, fallback = "file", maxLength = 120)
        val extension =
            sanitizePathSegment(rawExtension, fallback = "", maxLength = 20)
                .replace(".", "")
                .trim()

        return if (extension.isBlank()) base else "$base.$extension"
    }

    private fun exportFileSafely(source: File, destinationRoot: File): String {
        val root = destinationRoot.canonicalFile
        val safeName = safeFileName(source.name)
        val desired = File(root, safeName).canonicalFile

        if (!desired.toPath().startsWith(root.toPath())) {
            throw SecurityException("Could not create a safe GalleryDL filename")
        }

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
