package com.junkfood.seal.util

import org.junit.Assert.*
import org.junit.Test

class CustomFormatSelectionTest {
    private val video = Format(formatId = "100026", vcodec = "avc1", acodec = "none", ext = "mp4")
    private val audio = Format(formatId = "30280", vcodec = "none", acodec = "mp4a.40.2", ext = "m4a")
    private val otherAudio = audio.copy(formatId = "30216", abr = 64.0)
    private val lossless = audio.copy(formatId = "30251", acodec = "flac", abr = 1500.0)
    private val info = VideoInfo(formats = listOf(otherAudio, audio, lossless, video),
        requestedFormats = listOf(video, audio))

    @Test fun customVideoGetsTheSameAudioAsPresetEvenIfPickerRanksLosslessFirst() {
        val result = CustomFormatSelection.resolve(listOf(video), info, listOf(lossless, audio, otherAudio))
        assertEquals(listOf("100026", "30280"), result.formats.map { it.formatId })
        assertFalse(result.missingAudio)
    }

    @Test fun explicitlyChosenAudioIsPreservedAndVideoGoesFirst() {
        val result = CustomFormatSelection.resolve(listOf(otherAudio, video), info)
        assertEquals(listOf(video, otherAudio), result.formats)
    }

    @Test fun autoMergedChoiceDoesNotReceiveADuplicateAudioTrack() {
        assertEquals(listOf(video, audio),
            CustomFormatSelection.resolve(listOf(video, audio, audio), info).formats)
    }

    @Test fun unknownAudioCodecDoesNotMakeAVideoLookLikeItAlreadyHasSound() {
        val result = CustomFormatSelection.resolve(listOf(video.copy(acodec = null)), info)
        assertTrue(result.formats.contains(audio))
        assertFalse(result.missingAudio)
    }

    @Test fun combinedVideoKeepsItsExistingAudio() {
        val combined = video.copy(formatId = "18", acodec = "mp4a.40.2")
        assertEquals(listOf(combined), CustomFormatSelection.resolve(listOf(combined), info).formats)
    }

    @Test fun audioOnlyAndExplicitMultipleAudioSelectionsStayIntact() {
        assertEquals(listOf(audio, otherAudio),
            CustomFormatSelection.resolve(listOf(audio, otherAudio), info, includeAudio = false).formats)
        assertEquals(listOf(video, audio, otherAudio),
            CustomFormatSelection.resolve(listOf(video, audio, otherAudio), info).formats)
    }

    @Test fun silentVideoRequiresAnExplicitAudioOptOut() {
        assertEquals(listOf(video),
            CustomFormatSelection.resolve(listOf(video, audio), info, includeAudio = false).formats)
        val silentInfo = VideoInfo(formats = listOf(video))
        assertTrue(CustomFormatSelection.resolve(listOf(video), silentInfo).missingAudio)
        assertFalse(CustomFormatSelection.resolve(listOf(video), silentInfo, includeAudio = false).missingAudio)
    }

    @Test fun preferredTrackMustExistInTheFilteredInventory() {
        assertEquals(otherAudio, CustomFormatSelection.preferredAudio(info, listOf(otherAudio)))
        assertNull(CustomFormatSelection.preferredAudio(info, emptyList()))
    }

    @Test fun extractorOrderingIsUsedWhenRequestedTracksAreUnavailable() {
        val fallback = info.copy(requestedFormats = null, formats = listOf(otherAudio, audio, video))
        assertEquals(audio, CustomFormatSelection.preferredAudio(fallback, listOf(otherAudio, audio)))
    }
}
