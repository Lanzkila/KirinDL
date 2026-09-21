package com.junkfood.seal.ui.page.tools

import com.junkfood.seal.util.FileUtil
import com.junkfood.seal.util.PreferenceUtil.getString
import com.junkfood.seal.util.PreferenceUtil.updateString
import java.io.File

const val MEDIA_CONVERTER_DIRECTORY = "media_converter_directory"
const val MANGA_CONVERTER_DIRECTORY = "manga_converter_directory"

object ConverterPreferences {
    fun defaultMediaDirectory(): File =
        File(FileUtil.getExternalDownloadDirectory(), "Media Converter")

    fun defaultMangaDirectory(): File =
        File(FileUtil.getExternalDownloadDirectory(), "Manga Converter")

    fun mediaDirectory(): File =
        MEDIA_CONVERTER_DIRECTORY.getString().takeIf { it.isNotBlank() }?.let(::File)
            ?: defaultMediaDirectory()

    fun mangaDirectory(): File =
        MANGA_CONVERTER_DIRECTORY.getString().takeIf { it.isNotBlank() }?.let(::File)
            ?: defaultMangaDirectory()

    fun setMediaDirectory(path: String) {
        MEDIA_CONVERTER_DIRECTORY.updateString(path)
    }

    fun setMangaDirectory(path: String) {
        MANGA_CONVERTER_DIRECTORY.updateString(path)
    }

    fun resetMediaDirectory() {
        MEDIA_CONVERTER_DIRECTORY.updateString("")
    }

    fun resetMangaDirectory() {
        MANGA_CONVERTER_DIRECTORY.updateString("")
    }
}
