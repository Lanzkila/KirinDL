package com.junkfood.seal.util

import android.content.Context
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.UUID
import java.util.concurrent.TimeUnit
import java.util.zip.ZipInputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

/**
 * Installs gallery-dl on demand from the official Codeberg source repository.
 *
 * KirinDownloader follows the current Codeberg master branch instead of relying on a package
 * mirror. The updater first resolves the exact master commit from Codeberg's Forgejo API, then
 * downloads the archive for that immutable commit and extracts only the gallery_dl Python package
 * into app-private storage.
 */
object GalleryDlEngine {
    private const val CODEBERG_BRANCH_METADATA_URL =
        "https://codeberg.org/api/v1/repos/mikf/gallery-dl/branches/master"
    private const val CODEBERG_ARCHIVE_BASE_URL =
        "https://codeberg.org/api/v1/repos/mikf/gallery-dl/archive/"
    private const val USER_AGENT = "KirinDownloader gallery-dl updater"

    private const val ENGINE_ROOT = "gallery-dl-engine"
    private const val CURRENT_DIR = "current"
    private const val VERSION_FILE = "VERSION"
    private const val SOURCE_FILE = "SOURCE"

    private val versionPattern = Regex("""__version__\s*=\s*[\"']([^\"']+)[\"']""")
    private val commitPattern = Regex("^[0-9a-fA-F]{7,64}$")

    private val httpClient =
        OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(90, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
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
                val commit = resolveCodebergMasterCommit()
                val shortCommit = commit.take(12)
                val archiveUrl = "$CODEBERG_ARCHIVE_BASE_URL$commit.zip"

                val engineRoot = File(context.filesDir, ENGINE_ROOT).apply { mkdirs() }
                val archiveFile = File(context.cacheDir, "gallery-dl-codeberg-$shortCommit.zip")
                val stageDir = File(engineRoot, "stage-${UUID.randomUUID()}")

                try {
                    downloadArchive(archiveUrl, archiveFile)
                    extractGalleryDlPackageSafely(archiveFile, stageDir)

                    if (!File(stageDir, "gallery_dl/__init__.py").isFile) {
                        throw IOException("Codeberg archive did not contain the gallery_dl package")
                    }

                    val version = detectPackageVersion(stageDir) ?: "codeberg-$shortCommit"
                    installStagedEngine(engineRoot, stageDir)

                    File(engineRoot, VERSION_FILE).writeText(version)
                    File(engineRoot, SOURCE_FILE).writeText("codeberg:$commit")
                    InstallInfo(version)
                } finally {
                    archiveFile.delete()
                    stageDir.deleteRecursively()
                }
            }
        }

    private fun resolveCodebergMasterCommit(): String {
        val request =
            Request.Builder()
                .url(CODEBERG_BRANCH_METADATA_URL)
                .header("Accept", "application/json")
                .header("User-Agent", USER_AGENT)
                .get()
                .build()

        val metadata =
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IOException("Codeberg branch request failed (HTTP ${response.code})")
                }
                response.body?.string()?.takeIf { it.isNotBlank() }
                    ?: throw IOException("Codeberg returned an empty branch response")
            }

        val commitJson =
            JSONObject(metadata).optJSONObject("commit")
                ?: throw IOException("Codeberg branch response did not include commit metadata")

        val commit =
            commitJson.optString("id").ifBlank {
                commitJson.optString("sha")
            }

        if (!commitPattern.matches(commit)) {
            throw IOException("Codeberg returned an invalid gallery-dl commit id")
        }
        return commit.lowercase()
    }

    private fun downloadArchive(url: String, destination: File) {
        val request =
            Request.Builder()
                .url(url)
                .header("Accept", "application/zip, application/octet-stream")
                .header("User-Agent", USER_AGENT)
                .get()
                .build()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Codeberg gallery-dl archive download failed (HTTP ${response.code})")
            }
            val body = response.body ?: throw IOException("Codeberg gallery-dl archive was empty")
            FileOutputStream(destination).use { output -> body.byteStream().copyTo(output) }
        }

        if (!destination.isFile || destination.length() <= 0L) {
            throw IOException("Codeberg gallery-dl archive download produced no data")
        }
    }

    /** Extract only the gallery_dl package, ignoring docs/tests/build metadata from the source ZIP. */
    private fun extractGalleryDlPackageSafely(archive: File, destination: File) {
        destination.deleteRecursively()
        destination.mkdirs()
        val destinationPath = destination.canonicalFile.toPath()
        var extractedPackageFile = false

        ZipInputStream(archive.inputStream().buffered()).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                val normalizedName = entry.name.replace('\\', '/')
                val marker = "gallery_dl/"
                val markerIndex = normalizedName.indexOf(marker)

                if (markerIndex >= 0) {
                    val relativeName = normalizedName.substring(markerIndex)
                    val out = File(destination, relativeName).canonicalFile
                    if (!out.toPath().startsWith(destinationPath)) {
                        throw SecurityException("Unsafe path in Codeberg gallery-dl archive")
                    }

                    if (entry.isDirectory) {
                        out.mkdirs()
                    } else {
                        out.parentFile?.mkdirs()
                        FileOutputStream(out).use { output -> zip.copyTo(output) }
                        extractedPackageFile = true
                    }
                }
                zip.closeEntry()
            }
        }

        if (!extractedPackageFile) {
            throw IOException("No gallery_dl package files were found in the Codeberg archive")
        }
    }

    private fun detectPackageVersion(stageDir: File): String? {
        val candidates =
            listOf(
                File(stageDir, "gallery_dl/version.py"),
                File(stageDir, "gallery_dl/__init__.py"),
            )

        for (file in candidates) {
            if (!file.isFile) continue
            val match = runCatching { versionPattern.find(file.readText()) }.getOrNull() ?: continue
            return match.groupValues.getOrNull(1)?.takeIf { it.isNotBlank() }
        }
        return null
    }

    private fun installStagedEngine(engineRoot: File, stageDir: File) {
        val currentDir = File(engineRoot, CURRENT_DIR)
        val backupDir = File(engineRoot, "previous")
        backupDir.deleteRecursively()

        if (currentDir.exists() && !currentDir.renameTo(backupDir)) {
            if (!currentDir.copyRecursively(backupDir, overwrite = true)) {
                throw IOException("Could not back up the current gallery-dl engine")
            }
            currentDir.deleteRecursively()
        }

        try {
            if (!stageDir.renameTo(currentDir)) {
                currentDir.deleteRecursively()
                if (!stageDir.copyRecursively(currentDir, overwrite = true)) {
                    throw IOException("Could not install gallery-dl engine files")
                }
                stageDir.deleteRecursively()
            }
            backupDir.deleteRecursively()
        } catch (error: Throwable) {
            currentDir.deleteRecursively()
            if (backupDir.exists()) {
                backupDir.renameTo(currentDir)
            }
            throw error
        }
    }
}
