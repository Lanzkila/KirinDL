package com.junkfood.seal.util

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.IOException
import org.json.JSONObject

/**
 * App-private compatibility files used by gallery-dl.
 *
 * These files live outside the downloaded Codeberg engine directory so engine updates never remove
 * user configuration, cookies, or the persistent gallery-dl cache.
 */
object GalleryDlConfig {
    private const val ROOT_DIR = "gallery-dl-config"
    private const val CONFIG_FILE = "config.json"
    private const val COOKIES_FILE = "cookies.txt"
    private const val CACHE_FILE = "cache.sqlite3"
    private const val MAX_CONFIG_BYTES = 1024 * 1024

    private val defaultConfig =
        """
        {
          "extractor": {},
          "downloader": {},
          "output": {},
          "postprocessor": {}
        }
        """.trimIndent() + "\n"

    data class Paths(
        val configFile: File,
        val cookiesFile: File,
        val cacheFile: File,
    )

    data class Snapshot(
        val configText: String,
        val configValid: Boolean,
        val cookiesImported: Boolean,
        val cookiesSize: Long,
        val cacheSize: Long,
    )

    fun defaultConfigText(): String = defaultConfig

    fun prepare(context: Context): Paths {
        val root = File(context.filesDir, ROOT_DIR).apply { mkdirs() }
        val configFile = File(root, CONFIG_FILE)

        if (!configFile.exists()) {
            configFile.writeText(defaultConfig)
        }

        return Paths(
            configFile = configFile,
            cookiesFile = File(root, COOKIES_FILE),
            cacheFile = File(root, CACHE_FILE),
        )
    }

    fun snapshot(context: Context): Snapshot {
        val paths = prepare(context)
        val configText = paths.configFile.readText()
        val cookiesSize = paths.cookiesFile.takeIf { it.isFile }?.length() ?: 0L
        val cacheSize = paths.cacheFile.takeIf { it.isFile }?.length() ?: 0L
        return Snapshot(
            configText = configText,
            configValid = validateConfig(configText).isSuccess,
            cookiesImported = cookiesSize > 0L,
            cookiesSize = cookiesSize,
            cacheSize = cacheSize,
        )
    }

    fun validateConfig(text: String): Result<Unit> =
        runCatching {
            if (text.isBlank()) {
                throw IllegalArgumentException("Configuration cannot be empty")
            }
            if (text.toByteArray(Charsets.UTF_8).size > MAX_CONFIG_BYTES) {
                throw IllegalArgumentException("Configuration is too large")
            }
            JSONObject(text)
            Unit
        }

    fun saveConfig(context: Context, text: String): Result<Unit> =
        runCatching {
            validateConfig(text).getOrThrow()
            prepare(context).configFile.writeText(text.trimEnd() + "\n")
        }

    fun resetConfig(context: Context): Result<Unit> =
        runCatching {
            prepare(context).configFile.writeText(defaultConfig)
        }

    fun importConfig(context: Context, uri: Uri): Result<Snapshot> =
        runCatching {
            val text =
                context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use {
                    it.readText()
                } ?: throw IOException("Could not open the selected configuration file")

            validateConfig(text).getOrThrow()
            prepare(context).configFile.writeText(text.trimEnd() + "\n")
            snapshot(context)
        }

    fun exportConfig(context: Context, uri: Uri): Result<Unit> =
        runCatching {
            val text = prepare(context).configFile.readText()
            validateConfig(text).getOrThrow()
            context.contentResolver.openOutputStream(uri, "w")?.bufferedWriter(Charsets.UTF_8)?.use {
                it.write(text)
            } ?: throw IOException("Could not create the configuration file")
        }

    fun importCookies(context: Context, uri: Uri): Result<Long> =
        runCatching {
            val target = prepare(context).cookiesFile
            val temporary = File(target.parentFile, "$COOKIES_FILE.importing")
            temporary.delete()

            try {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    temporary.outputStream().buffered().use { output -> input.copyTo(output) }
                } ?: throw IOException("Could not open the selected cookies file")

                if (!temporary.isFile || temporary.length() <= 0L) {
                    throw IOException("The selected cookies file is empty")
                }

                if (target.exists() && !target.delete()) {
                    throw IOException("Could not replace the existing cookies file")
                }

                if (!temporary.renameTo(target)) {
                    temporary.copyTo(target, overwrite = true)
                    temporary.delete()
                }

                target.length()
            } finally {
                temporary.delete()
            }
        }

    fun clearCookies(context: Context): Result<Unit> =
        runCatching {
            val cookies = prepare(context).cookiesFile
            if (cookies.exists() && !cookies.delete()) {
                throw IOException("Could not remove the imported cookies file")
            }
        }

    fun clearCache(context: Context): Result<Unit> =
        runCatching {
            val cache = prepare(context).cacheFile
            if (cache.exists() && !cache.delete()) {
                throw IOException("Could not clear the gallery-dl cache")
            }
        }
}
