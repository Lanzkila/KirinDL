package com.junkfood.seal.util

/** Resolve the Custom picker's actual tracks before previewing or enqueueing a task. */
internal object CustomFormatSelection {
    data class Selection(val formats: List<Format>, val missingAudio: Boolean)

    fun preferredAudio(info: VideoInfo, available: List<Format>): Format? {
        val audio = available.filter { it.isAudioOnly() && it.containsAudio() }
        // Match the audio yt-dlp selected for Preset before using the picker's
        // bitrate/size ordering. Never substitute an explicitly selected track.
        val preferred = buildList {
            info.requestedFormats?.let { addAll(it) }
            info.requestedDownloads.orEmpty().forEach { download ->
                download.requestedFormats?.let { addAll(it) }
            }
            // yt-dlp's format inventory is ordered from worst to best.
            addAll(info.formats.orEmpty().asReversed())
        }
        for (candidate in preferred) {
            if (!candidate.isAudioOnly() || !candidate.containsAudio()) continue
            val id = candidate.formatId ?: continue
            audio.firstOrNull { it.formatId == id }?.let { return it }
        }
        return audio.firstOrNull()
    }

    fun resolve(
        selected: List<Format>,
        info: VideoInfo,
        availableAudio: List<Format> = info.formats.orEmpty(),
        includeAudio: Boolean = true,
    ): Selection {
        val unique = selected.distinctBy { it.formatId }
        val video = unique.filter { it.containsVideo() }
        if (video.isEmpty()) return Selection(unique, missingAudio = false)
        if (!includeAudio) return Selection(video, missingAudio = false)

        val tracks = (video + unique.filterNot { it.containsVideo() }).toMutableList()
        if (tracks.none { it.containsAudio() }) {
            preferredAudio(info, availableAudio)?.let { tracks.add(it) }
        }
        return Selection(tracks, missingAudio = tracks.none { it.containsAudio() })
    }
}
