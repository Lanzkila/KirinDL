package com.junkfood.seal.util

import android.content.Context
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.security.MessageDigest
import java.util.UUID
import java.util.concurrent.TimeUnit
import java.util.zip.ZipInputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

/**
 * Installs gallery-dl on demand from the official PyPI package index.
 *
 * gallery-dl itself is not packaged inside the APK. This keeps the engine independently
 * updateable and avoids merging its GPL-2.0-only distribution into the GPL-3.0 APK. The wheel
 * is accepted only from pypi.org metadata and only after its published SHA-256 digest matches.
 */
object GalleryDlEngine {
    private const val PYPI_METADATA_URL = "https://pypi.org/pypi/gallery-dl/json"
    private const val ENGINE_ROOT = "gallery-dl-engine"
    private const val CURRENT_DIR = "current"
    private const val VERSION_FILE = "VERSION"

    private val httpClient =
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(45, TimeUnit.SECONDS)
            .followRedirects(true)
            .build()

    data class InstallInfo(val version: String)

    fun engineDirectory(context: Context): File = File(File(context.filesDir, ENGINE_ROOT), CURRENT_DIR)

    fun installedVersion(context: Context): String? {
        val root = File(context.filesDir, ENGINE_ROOT)
        val engine = File(root, CURRENT_DIR)
        val marker = File(root, VERSION_FILE)
        if (!engine.isDirectory || !File(engine, "gallery_dl/__init__.py").isFile) return null
        return marker.takeIf { it.isFile }?.readText()?.trim()?.takeIf { it.isNotBlank() }
    }

    fun isInstalled(context: Context): Boolean = installedVersion(context) != null

    suspend fun installLatest(context: Context): Result<InstallInfo> =
        withContext(Dispatchers.IO) {
            runCatching {
                val metadataRequest = Request.Builder().url(PYPI_METADATA_URL).get().build()
                val metadata =
                    httpClient.newCall(metadataRequest).execute().use { response ->
                        if (!response.isSuccessful) {
                            throw IOException("PyPI metadata request failed (HTTP ${response.code})")
                        }
                        response.body?.string()?.takeIf { it.isNotBlank() }
                            ?: throw IOException("PyPI returned an empty response")
                    }

                val rootJson = JSONObject(metadata)
                val version = rootJson.getJSONObject("info").getString("version")
                val urls = rootJson.getJSONArray("urls")

                var wheelUrl: String? = null
                var wheelName: String? = null
                var expectedSha256: String? = null

                // gallery-dl publishes a pure-Python wheel. Prefer the universal py3 wheel.
                for (index in 0 until urls.length()) {
                    val item = urls.getJSONObject(index)
                    val filename = item.optString("filename")
                    if (item.optString("packagetype") == "bdist_wheel" &&
                        filename.endsWith("py3-none-any.whl")
                    ) {
                        wheelUrl = item.getString("url")
                        wheelName = filename
                        expectedSha256 = item.getJSONObject("digests").getString("sha256")
                        break
                    }
                }

                if (wheelUrl == null || wheelName == null || expectedSha256 == null) {
                    throw IOException("No compatible pure-Python gallery-dl wheel was found")
                }

                val engineRoot = File(context.filesDir, ENGINE_ROOT).apply { mkdirs() }
                val wheelFile = File(context.cacheDir, wheelName)
                val stageDir = File(engineRoot, "stage-${UUID.randomUUID()}")

                try {
                    val wheelRequest = Request.Builder().url(wheelUrl).get().build()
                    httpClient.newCall(wheelRequest).execute().use { response ->
                        if (!response.isSuccessful) {
                            throw IOException("gallery-dl wheel download failed (HTTP ${response.code})")
                        }
                        val body = response.body ?: throw IOException("gallery-dl wheel was empty")
                        FileOutputStream(wheelFile).use { output -> body.byteStream().copyTo(output) }
                    }

                    val actualSha256 = sha256(wheelFile)
                    if (!actualSha256.equals(expectedSha256, ignoreCase = true)) {
                        throw SecurityException("gallery-dl wheel checksum did not match PyPI metadata")
                    }

                    extractWheelSafely(wheelFile, stageDir)
                    if (!File(stageDir, "gallery_dl/__init__.py").isFile) {
                        throw IOException("Downloaded wheel did not contain the gallery_dl package")
                    }

                    val currentDir = File(engineRoot, CURRENT_DIR)
                    val backupDir = File(engineRoot, "previous")
                    backupDir.deleteRecursively()
                    if (currentDir.exists() && !currentDir.renameTo(backupDir)) {
                        currentDir.deleteRecursively()
                    }

                    if (!stageDir.renameTo(currentDir)) {
                        currentDir.deleteRecursively()
                        if (!stageDir.copyRecursively(currentDir, overwrite = true)) {
                            throw IOException("Could not install gallery-dl engine files")
                        }
                        stageDir.deleteRecursively()
                    }

                    File(engineRoot, VERSION_FILE).writeText(version)
                    backupDir.deleteRecursively()
                    InstallInfo(version)
                } finally {
                    wheelFile.delete()
                    stageDir.deleteRecursively()
                }
            }
        }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().buffered().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val count = input.read(buffer)
                if (count <= 0) break
                digest.update(buffer, 0, count)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun extractWheelSafely(wheel: File, destination: File) {
        destination.deleteRecursively()
        destination.mkdirs()
        val destinationPath = destination.canonicalFile.toPath()

        ZipInputStream(wheel.inputStream().buffered()).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                val out = File(destination, entry.name).canonicalFile
                if (!out.toPath().startsWith(destinationPath)) {
                    throw SecurityException("Unsafe path in gallery-dl wheel")
                }
                if (entry.isDirectory) {
                    out.mkdirs()
                } else {
                    out.parentFile?.mkdirs()
                    FileOutputStream(out).use { output -> zip.copyTo(output) }
                }
                zip.closeEntry()
            }
        }
    }
}
