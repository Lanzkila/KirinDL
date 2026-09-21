package com.junkfood.seal.ui.page.tools.mangaconverter

import android.net.Uri

enum class MangaConversionMode(
    val label: String,
    val sourceExtension: String,
    val targetExtension: String,
) {
    ZIP_TO_CBZ("ZIP → CBZ", "zip", "cbz"),
    CBZ_TO_ZIP("CBZ → ZIP", "cbz", "zip"),
    ZIP_TO_PDF("ZIP → PDF", "zip", "pdf"),
    CBZ_TO_PDF("CBZ → PDF", "cbz", "pdf"),
}

data class MangaInput(
    val uri: Uri,
    val name: String,
    val sizeBytes: Long? = null,
)

data class MangaConverterSettings(
    val mode: MangaConversionMode = MangaConversionMode.ZIP_TO_CBZ,
    val naturalSort: Boolean = true,
    val removeJunkFiles: Boolean = true,
    val includeComicInfo: Boolean = false,
    val title: String = "",
    val author: String = "",
)

data class MangaConversionResult(
    val sourceName: String,
    val outputPath: String,
    val pageCount: Int,
)
