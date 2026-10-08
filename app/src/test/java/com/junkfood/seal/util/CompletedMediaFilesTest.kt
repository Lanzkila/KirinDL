package com.junkfood.seal.util

import java.io.File
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class CompletedMediaFilesTest {
    @get:Rule val directory = TemporaryFolder()

    private fun output(name: String, content: String = "media"): File =
        File(directory.root, name).apply { parentFile.mkdirs(); writeText(content) }

    @Test
    fun manifestsMetadataThumbnailsAndPartialDownloadsNeverBecomePlayTargets() {
        for (suffix in listOf("m3u8", "mpd", "info.json", "description.txt", "webp",
            "jpg", "vtt", "srt", "mp4.part", "mp4.ytdl", "mp4.aria2")) {
            output("Video.$suffix")
        }
        val media = output("Video.mp4")
        assertEquals(listOf(media), CompletedMediaFiles.collect(directory.root, "Video"))
    }

    @Test
    fun mergedFileIsSelectedBeforeClipsRegardlessOfDirectoryOrder() {
        val clip = output("Video [0-10].mp4")
        val main = output("Video.mp4")
        assertEquals(listOf(main, clip), CompletedMediaFiles.collect(directory.root, "Video"))
    }

    @Test
    fun parentDirectoryAndTitlesWithoutASuffixBoundaryCannotMatch() {
        output("Video playlist/Other.mp4")
        output("Video2.mp4")
        val media = output("Video.mp4")
        assertFalse(CompletedMediaFiles.collect(directory.root, "Video").any { it.name == "Other.mp4" })
        assertFalse(CompletedMediaFiles.collect(directory.root, "Video").any { it.name == "Video2.mp4" })
        assertEquals(media, CompletedMediaFiles.collect(directory.root, "Video").first())
    }

    @Test
    fun chaptersAreIncludedOnlyForChapterDownloadsAndMainVideoPlaysFirst() {
        val chapter = output("Video/1 - Intro.mp4")
        val main = output("Video/Video.mp4")
        assertEquals(listOf(main), CompletedMediaFiles.collect(directory.root, "Video"))
        assertEquals(listOf(main, chapter), CompletedMediaFiles.collect(directory.root, "Video", true))
    }

    @Test
    fun remuxedFilesStillMatchMetadataWithThePreviousExtension() {
        val media = output("Video.mkv")
        assertEquals(listOf(media), CompletedMediaFiles.collect(directory.root, "Video.webm"))
    }

    @Test
    fun emptyFilesAndTempDirectoriesAreExcludedAndUppercaseAudioWorks() {
        output("Video.mp4", "")
        output("tmp/Video.mp4")
        val audio = output("Video.M4A")
        assertEquals(listOf(audio), CompletedMediaFiles.collect(directory.root, "Video"))
    }

    @Test
    fun currentMediaIsSelectedBeforeAnOlderDownloadWithTheSameTitle() {
        val old = output("Video.m4a").apply { assertTrue(setLastModified(1_000L)) }
        val current = output("Video.mp3").apply { assertTrue(setLastModified(2_000L)) }
        assertEquals(listOf(current, old), CompletedMediaFiles.collect(directory.root, "Video"))
    }

    @Test
    fun titleDirectoriesSupportCustomTemplatesWithAnIdAsTheFileName() {
        val media = output("Video/N5V8HEoo_10.mp4")
        output("Video/N5V8HEoo_10.info.json")
        assertEquals(listOf(media), CompletedMediaFiles.collect(directory.root, "Video", true))
    }
}
