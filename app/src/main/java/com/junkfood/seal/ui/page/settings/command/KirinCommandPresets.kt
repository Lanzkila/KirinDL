package com.junkfood.seal.ui.page.settings.command

import com.junkfood.seal.database.objects.CommandTemplate

enum class KirinCommandCategory(val label: String) {
    Media("Media"),
    Audio("Audio"),
    Subtitle("Subtitle"),
    Metadata("Metadata"),
    Playlist("Playlist"),
    Live("Live"),
    Tools("Tools"),
}

data class KirinCommandPreset(
    val name: String,
    val description: String,
    val category: KirinCommandCategory,
    val command: String,
) {
    fun asTemplate(): CommandTemplate = CommandTemplate(id = 0, name = name, template = command)
}

object KirinCommandPresets {
    val all: List<KirinCommandPreset> =
        listOf(
            KirinCommandPreset(
                name = "Thumbnail Only",
                description = "Save the best available thumbnail without downloading the media.",
                category = KirinCommandCategory.Media,
                command = "--skip-download\n--write-thumbnail",
            ),
            KirinCommandPreset(
                name = "Split Chapters",
                description = "Download normally, then split the output into chapter files with FFmpeg.",
                category = KirinCommandCategory.Media,
                command = "--split-chapters",
            ),
            KirinCommandPreset(
                name = "Remove Sponsor Segments",
                description = "Use yt-dlp SponsorBlock support to remove sponsor segments when available.",
                category = KirinCommandCategory.Media,
                command = "--sponsorblock-remove sponsor",
            ),
            KirinCommandPreset(
                name = "Best Compatibility MP4",
                description = "Prefer H.264/AAC and MP4/M4A for devices that dislike newer codecs.",
                category = KirinCommandCategory.Media,
                command = "-S vcodec:h264,acodec:aac,ext:mp4:m4a\n--merge-output-format mp4",
            ),
            KirinCommandPreset(
                name = "Audio Original",
                description = "Download the best original audio stream without forcing audio conversion.",
                category = KirinCommandCategory.Audio,
                command = "-f bestaudio/best",
            ),
            KirinCommandPreset(
                name = "Subtitle Only",
                description = "Save manual and automatic subtitles without downloading the media.",
                category = KirinCommandCategory.Subtitle,
                command = "--skip-download\n--write-subs\n--write-auto-subs\n--sub-langs all,-live_chat",
            ),
            KirinCommandPreset(
                name = "Metadata Pack",
                description = "Save info JSON, description, and thumbnail without downloading media.",
                category = KirinCommandCategory.Metadata,
                command = "--skip-download\n--write-info-json\n--write-description\n--write-thumbnail",
            ),
            KirinCommandPreset(
                name = "Playlist First 10",
                description = "Limit playlist or collection processing to the first 10 entries.",
                category = KirinCommandCategory.Playlist,
                command = "--playlist-end 10",
            ),
            KirinCommandPreset(
                name = "Playlist First 25",
                description = "Limit playlist or collection processing to the first 25 entries.",
                category = KirinCommandCategory.Playlist,
                command = "--playlist-end 25",
            ),
            KirinCommandPreset(
                name = "Playlist First 50",
                description = "Limit playlist or collection processing to the first 50 entries.",
                category = KirinCommandCategory.Playlist,
                command = "--playlist-end 50",
            ),
            KirinCommandPreset(
                name = "Live From Start",
                description = "For supported ongoing livestreams, ask yt-dlp to download from the beginning.",
                category = KirinCommandCategory.Live,
                command = "--live-from-start",
            ),
            KirinCommandPreset(
                name = "Diagnostics / Simulate",
                description = "Resolve the URL and print detailed JSON/debug output without downloading.",
                category = KirinCommandCategory.Tools,
                command = "--simulate\n--dump-json\n--verbose",
            ),
        )

    fun isInstalled(preset: KirinCommandPreset, templates: List<CommandTemplate>): Boolean =
        templates.any {
            it.name == preset.name && it.template.trim() == preset.command.trim()
        }
}
