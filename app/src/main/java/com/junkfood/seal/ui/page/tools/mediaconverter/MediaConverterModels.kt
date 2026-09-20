package com.junkfood.seal.ui.page.tools.mediaconverter

import android.net.Uri

enum class MediaOutputType(val label: String) {
    VIDEO("Video"),
    AUDIO("Audio"),
}

enum class VideoContainer(val label: String, val extension: String) {
    MP4("MP4", "mp4"),
    MKV("MKV", "mkv"),
    WEBM("WebM", "webm"),
    MOV("MOV", "mov"),
    AVI("AVI", "avi"),
    FLV("FLV", "flv"),
}

enum class AudioContainer(val label: String, val extension: String) {
    MP3("MP3", "mp3"),
    M4A("M4A / AAC", "m4a"),
    OPUS("Opus", "opus"),
    OGG("Ogg Vorbis", "ogg"),
    FLAC("FLAC", "flac"),
    WAV("WAV", "wav"),
}

enum class VideoCodec(val label: String, val ffmpegName: String?) {
    AUTO("Auto", null),
    COPY("Copy / Remux", "copy"),
    H264("H.264 / AVC", "libx264"),
    HEVC("H.265 / HEVC", "libx265"),
    VP9("VP9", "libvpx-vp9"),
    AV1("AV1", "libaom-av1"),
}

enum class AudioCodec(val label: String, val ffmpegName: String?) {
    AUTO("Auto", null),
    COPY("Copy", "copy"),
    AAC("AAC", "aac"),
    MP3("MP3", "libmp3lame"),
    OPUS("Opus", "libopus"),
    VORBIS("Vorbis", "libvorbis"),
    FLAC("FLAC", "flac"),
    PCM("PCM", "pcm_s16le"),
}

data class MediaInput(
    val uri: Uri,
    val name: String,
    val sizeBytes: Long? = null,
)

data class MediaConverterSettings(
    val outputType: MediaOutputType = MediaOutputType.VIDEO,
    val videoContainer: VideoContainer = VideoContainer.MP4,
    val audioContainer: AudioContainer = AudioContainer.MP3,
    val videoCodec: VideoCodec = VideoCodec.H264,
    val audioCodec: AudioCodec = AudioCodec.AAC,
    val videoBitrateKbps: Int? = null,
    val audioBitrateKbps: Int? = 192,
    val resolutionHeight: Int? = null,
    val fps: Int? = null,
    val crf: Int? = 23,
    val sampleRate: Int? = null,
    val audioChannels: Int? = null,
    val trimStartSeconds: Double? = null,
    val trimEndSeconds: Double? = null,
    val keepMetadata: Boolean = true,
    val keepSubtitles: Boolean = true,
    val removeAudio: Boolean = false,
    val fastRemux: Boolean = false,
) {
    val outputExtension: String
        get() = if (outputType == MediaOutputType.VIDEO) videoContainer.extension else audioContainer.extension
}

data class MediaConversionResult(
    val sourceName: String,
    val outputPath: String,
)
