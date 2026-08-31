package com.junkfood.seal.util

import android.content.Context
import java.io.File

/**
 * App-private compatibility files used by gallery-dl.
 *
 * The config is intentionally stored outside the downloaded engine directory so updating the
 * Codeberg engine never removes user settings, cookies, or the persistent gallery-dl cache.
 */
object GalleryDlConfig {
    private const val ROOT_DIR = "gallery-dl-config"
    private const val CONFIG_FILE = "config.json"
    private const val COOKIES_FILE = "cookies.txt"
    private const val CACHE_FILE = "cache.sqlite3"

    data class Paths(
        val configFile: File,
        val cookiesFile: File,
        val cacheFile: File,
    )

    fun prepare(context: Context): Paths {
        val root = File(context.filesDir, ROOT_DIR).apply { mkdirs() }
        val configFile = File(root, CONFIG_FILE)

        if (!configFile.exists()) {
            configFile.writeText(
                """
                {
                  "extractor": {},
                  "downloader": {},
                  "output": {},
                  "postprocessor": {}
                }
                """.trimIndent() + "\n"
            )
        }

        return Paths(
            configFile = configFile,
            cookiesFile = File(root, COOKIES_FILE),
            cacheFile = File(root, CACHE_FILE),
        )
    }
}
