package com.junkfood.seal.ui.page.tools.mangaconverter

import java.io.File

object MangaConverterUtils {
    private val imageExtensions = setOf("jpg", "jpeg", "png", "webp", "bmp", "gif")
    private val junkNames =
        setOf(
            ".ds_store",
            "thumbs.db",
            "desktop.ini",
            "__macosx",
        )

    fun isImageEntry(name: String): Boolean =
        name.substringAfterLast('.', "").lowercase() in imageExtensions

    fun isJunkEntry(name: String): Boolean {
        val lower = name.lowercase()
        val parts = lower.split('/')
        return parts.any { it in junkNames } || lower.startsWith("__macosx/")
    }

    fun naturalComparator(): Comparator<String> = Comparator { a, b -> naturalCompare(a, b) }

    private fun naturalCompare(a: String, b: String): Int {
        val left = tokenize(a.lowercase())
        val right = tokenize(b.lowercase())
        val limit = minOf(left.size, right.size)
        for (i in 0 until limit) {
            val x = left[i]
            val y = right[i]
            val xNum = x.toLongOrNull()
            val yNum = y.toLongOrNull()
            val result =
                if (xNum != null && yNum != null) xNum.compareTo(yNum)
                else x.compareTo(y)
            if (result != 0) return result
        }
        return left.size.compareTo(right.size)
    }

    private fun tokenize(value: String): List<String> =
        Regex("(\\d+|\\D+)").findAll(value).map { it.value }.toList()

    fun uniqueOutputFile(directory: File, sourceName: String, extension: String): File {
        val rawBase = sourceName.substringBeforeLast('.', sourceName)
        val safeBase =
            rawBase.replace(Regex("[\\\\/:*?\"<>|\\p{Cntrl}]"), "_")
                .trim()
                .take(120)
                .ifBlank { "KirinDL-manga" }
        var candidate = File(directory, "$safeBase.$extension")
        var index = 1
        while (candidate.exists()) {
            candidate = File(directory, "$safeBase ($index).$extension")
            index++
        }
        return candidate
    }

    fun xmlEscape(value: String): String =
        value.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
}
