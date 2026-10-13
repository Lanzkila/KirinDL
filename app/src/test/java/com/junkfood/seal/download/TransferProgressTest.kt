package com.junkfood.seal.download

import org.junit.Assert.*
import org.junit.Test

class TransferProgressTest {
    @Test fun repeatedVideoDestinationDoesNotBecomeAudioOrResetItsProgress() {
        val progress = TransferProgress(false)
        progress.update(-1f, "[download] Destination: /tmp/title.f100026.mp4")
        progress.update(53f, "[download] 53%")
        val retry = progress.update(-1f, "[download] Got error: Remote end closed connection. Retrying (1/2)")
        assertEquals("Retrying", TransferProgress.stageLabel(retry.stateText, false))
        assertEquals(-1f, retry.progress)
        val resumed = progress.update(-1f, "[download] Destination: /tmp/title.f100026.mp4")
        assertEquals("Video", TransferProgress.stageLabel(resumed.stateText, false))
        assertEquals(0.53f, resumed.progress, 0.0001f)
        assertEquals(0.53f, progress.update(52f, "[download] 52%").progress, 0.0001f)
    }

    @Test fun actualAudioAndMergeHaveSeparatePhases() {
        val progress = TransferProgress(false, setOf("30280"))
        progress.update(-1f, "[download] Destination: /tmp/title.f100026.mp4")
        progress.update(100f, "[download] 100%")
        val audio = progress.update(-1f, "[download] Destination: /tmp/title.f30280.webm")
        assertEquals("Audio", TransferProgress.stageLabel(audio.stateText, false))
        assertEquals(0f, audio.progress)
        val merge = progress.update(-1f, "[Merger] Merging formats")
        assertEquals("Merge", TransferProgress.stageLabel(merge.stateText, false))
        assertEquals(-1f, merge.progress)
        assertEquals("[Merger] Merging formats", TransferProgress.cleanText(merge.stateText))
    }

    @Test fun secondVideoDestinationStaysVideo() {
        val progress = TransferProgress(false)
        progress.update(-1f, "[download] Destination: /tmp/first.mp4")
        val second = progress.update(-1f, "[download] Destination: /tmp/second.mp4")
        assertEquals("Video", TransferProgress.stageLabel(second.stateText, false))
    }
}
