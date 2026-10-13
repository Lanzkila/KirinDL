package com.junkfood.seal.download

import java.util.Locale

/** A Destination line is a filename, not a stream counter: retries repeat it. */
internal class TransferProgress(
    private val audioOnly: Boolean,
    private val audioFormatIds: Set<String> = emptySet(),
) {
    private var destination: String? = null
    private var lastProgress = -1f
    private var stream = if (audioOnly) "audio" else "video"
    private var stage = stream

    data class Value(val progress: Float, val cleanText: String, val stateText: String)

    fun update(rawPercentage: Float, rawText: String): Value {
        val clean = cleanText(rawText).removePrefix("[download] ").removePrefix("[download]").trim()
        val lower = clean.lowercase(Locale.ROOT)
        val nextDestination = clean.takeIf { it.startsWith("Destination:", ignoreCase = true) }
            ?.substringAfter(':')?.trim()
        val changedFile = nextDestination != null && nextDestination != destination
        if (nextDestination != null) {
            if (changedFile) lastProgress = -1f
            destination = nextDestination
            val file = nextDestination.substringAfterLast('/').lowercase(Locale.ROOT)
            val formatId = Regex("\\.f([^.]+)\\.[^.]+$").find(file)?.groupValues?.get(1)
            stream = if (audioOnly || formatId in audioFormatIds ||
                file.substringAfterLast('.') in setOf("m4a", "mp3", "aac", "opus", "ogg", "flac", "wav"))
                "audio" else "video"
            stage = stream
        } else {
            stage = when {
                "retrying" in lower || "waiting for network" in lower -> "retry"
                "[merger]" in lower || "merging format" in lower -> "merge"
                listOf("[extractaudio]", "[videoconvertor]", "[videoremuxer]", "[metadata]",
                    "[embedsubtitle]", "[embedthumbnail]", "[fixup").any(lower::contains) -> "processing"
                "[hlsnative]" in lower || "[dashsegments]" in lower || "(frag " in lower -> "fragment"
                "moving" in lower || "move file" in lower -> "moving"
                rawPercentage.isFinite() && rawPercentage >= 0f -> stream
                else -> stage
            }
        }
        val raw = if (rawPercentage.isFinite() && rawPercentage >= 0f)
            (rawPercentage / 100f).coerceIn(0f, 1f) else -1f
        val progress = when {
            stage in setOf("retry", "merge", "processing", "moving") -> -1f
            changedFile -> 0f
            nextDestination != null -> lastProgress.coerceAtLeast(0f)
            raw < 0f -> lastProgress
            lastProgress < 0f -> raw
            else -> maxOf(lastProgress, raw)
        }
        if (progress >= 0f) lastProgress = progress
        return Value(progress, clean, "$PREFIX$stage|$clean")
    }

    companion object {
        private const val PREFIX = "__kirin_stage__"

        fun cleanText(text: String): String =
            if (text.startsWith(PREFIX)) text.substringAfter('|', "") else text

        fun stageLabel(text: String, audioOnly: Boolean): String {
            val stage = if (text.startsWith(PREFIX)) text.removePrefix(PREFIX).substringBefore('|') else ""
            return when (stage) {
                "audio" -> "Audio"
                "video" -> "Video"
                "fragment" -> "Fragment"
                "retry" -> "Retrying"
                "merge" -> "Merge"
                "processing" -> "Processing"
                "moving" -> "Moving"
                else -> if (audioOnly) "Audio" else "Video"
            }
        }
    }
}
