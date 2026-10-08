package com.junkfood.seal.util

import java.io.File
import java.util.Locale

/** Only finished audio/video files may become a download's Play target. */
internal object CompletedMediaFiles {
    private val extensions = setOf(
        "mp4", "mkv", "webm", "mov", "m4v", "avi", "flv", "f4v", "3gp", "3g2",
        "ogv", "ogg", "mpg", "mpeg", "ts", "m2ts", "mts", "vob", "wmv", "asf",
        "mxf", "rm", "rmvb", "mp3", "m4a", "m4b", "aac", "opus", "weba", "wav",
        "flac", "mka", "alac", "aif", "aiff", "amr", "wma", "ac3", "eac3", "ape",
        "au", "mp2", "mpe", "m4p", "mpga", "oga", "divx", "dv",
    )

    fun isMediaFileName(name: String): Boolean =
        name.substringAfterLast('.', "").lowercase(Locale.ROOT) in extensions

    fun collect(directory: File, title: String? = null, includeTitleDirectory: Boolean = false): List<File> {
        val expectedName = title?.let { File(it).name }
        val expectedBase = expectedName?.let {
            if (isMediaFileName(it)) File(it).nameWithoutExtension else it
        }
        return directory.walkTopDown()
            .onEnter { it == directory || it.name != "tmp" }
            .filter { it.isFile && it.length() > 0L && isMediaFileName(it.name) }
            .filter { file ->
                expectedBase == null || matches(file.nameWithoutExtension, expectedBase) ||
                    (includeTitleDirectory && file.relativeTo(directory).parentFile?.let { parent ->
                        generateSequence(parent) { it.parentFile }.any { it.name == expectedBase }
                    } == true)
            }
            .sortedWith(compareBy<File> {
                // Prefer the merged main file over chapter files or clipped copies.
                if (expectedBase != null && it.nameWithoutExtension == expectedBase) 0 else 1
            }.thenBy { it.relativeTo(directory).path.count { char -> char == File.separatorChar } }
                .thenByDescending { it.lastModified() }
                .thenBy { it.name })
            .toList()
    }

    private fun matches(base: String, expected: String): Boolean =
        base == expected || (base.startsWith(expected) &&
            base.getOrNull(expected.length) in listOf(' ', '[', '(', '-', '_', '.'))
}
