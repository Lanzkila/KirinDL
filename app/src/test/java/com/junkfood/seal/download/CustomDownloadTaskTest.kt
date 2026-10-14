package com.junkfood.seal.download

import com.junkfood.seal.util.DownloadUtil.DownloadPreferences
import com.junkfood.seal.util.Format
import com.junkfood.seal.util.VideoInfo
import org.junit.Assert.*
import org.junit.Test

class CustomDownloadTaskTest {
    private val video = Format(formatId = "100026", vcodec = "avc1", acodec = "none", fileSize = 80.0)
    private val audio = Format(formatId = "30280", vcodec = "none", acodec = "mp4a.40.2", fileSize = 20.0)
    private val info = VideoInfo(id = "test", originalUrl = "https://www.bilibili.com/video/test",
        formats = listOf(audio, video), requestedFormats = listOf(video, audio))

    private fun task(
        selected: List<Format>,
        metadata: VideoInfo = info,
        includeAudio: Boolean = true,
        preferences: DownloadPreferences = DownloadPreferences.EMPTY,
    ) = TaskFactory.createWithConfigurations(metadata, selected, emptyList(), false, "",
        emptyList(), emptyList(), preferences, includeAudio)

    @Test fun videoSelectionEnqueuesBothTracksAndReportsTheirCombinedSize() {
        val result = task(listOf(video))
        assertEquals("100026+30280", result.task.preferences.formatIdString)
        assertFalse(result.task.preferences.extractAudio)
        assertEquals(listOf(audio), result.state.viewState.audioOnlyFormats)
        assertEquals(100.0, result.state.videoInfo!!.fileSize!!, 0.0)
    }

    @Test fun explicitSilentDownloadAndAudioOnlyDownloadRemainAvailable() {
        assertEquals("100026", task(listOf(video), includeAudio = false).task.preferences.formatIdString)
        val audioTask = task(listOf(audio)).task
        assertEquals("30280", audioTask.preferences.formatIdString)
        assertTrue(audioTask.preferences.extractAudio)
    }

    @Test fun customMergeHonorsTheUsersMkvPreference() {
        val result = task(listOf(video, audio), preferences = DownloadPreferences.EMPTY.copy(mergeToMkv = true))
        assertTrue(result.task.preferences.mergeToMkv)
    }

    @Test fun missingAudioCannotSilentlyEnqueueAVideoWithSoundRequested() {
        val failure = runCatching { task(listOf(video), metadata = info.copy(formats = listOf(video))) }
            .exceptionOrNull()
        assertTrue(failure is IllegalArgumentException)
    }
}
