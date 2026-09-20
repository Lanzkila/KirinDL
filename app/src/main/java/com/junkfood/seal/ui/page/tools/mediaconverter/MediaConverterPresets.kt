package com.junkfood.seal.ui.page.tools.mediaconverter

enum class MediaConverterPreset(val label: String, val description: String) {
    ORIGINAL("Original", "Keep source dimensions and use a normal quality encode."),
    HIGH_QUALITY("High Quality", "Higher visual quality with a larger output size."),
    BALANCED("Balanced", "Good quality and size for everyday use."),
    SMALL_SIZE("Small Size", "Lower bitrate and resolution to reduce file size."),
    AUDIO_ONLY("Audio Only", "Extract or convert the audio track only."),
    FAST_REMUX("Fast Remux", "Copy compatible streams without re-encoding."),
}

fun MediaConverterPreset.applyTo(current: MediaConverterSettings): MediaConverterSettings =
    when (this) {
        MediaConverterPreset.ORIGINAL ->
            current.copy(
                outputType = MediaOutputType.VIDEO,
                videoCodec = VideoCodec.H264,
                audioCodec = AudioCodec.AAC,
                videoBitrateKbps = null,
                resolutionHeight = null,
                fps = null,
                crf = 20,
                fastRemux = false,
            )
        MediaConverterPreset.HIGH_QUALITY ->
            current.copy(
                outputType = MediaOutputType.VIDEO,
                videoCodec = VideoCodec.H264,
                audioCodec = AudioCodec.AAC,
                videoBitrateKbps = null,
                resolutionHeight = null,
                crf = 18,
                audioBitrateKbps = 256,
                fastRemux = false,
            )
        MediaConverterPreset.BALANCED ->
            current.copy(
                outputType = MediaOutputType.VIDEO,
                videoCodec = VideoCodec.H264,
                audioCodec = AudioCodec.AAC,
                videoBitrateKbps = null,
                resolutionHeight = null,
                crf = 23,
                audioBitrateKbps = 192,
                fastRemux = false,
            )
        MediaConverterPreset.SMALL_SIZE ->
            current.copy(
                outputType = MediaOutputType.VIDEO,
                videoCodec = VideoCodec.H264,
                audioCodec = AudioCodec.AAC,
                videoBitrateKbps = null,
                resolutionHeight = 720,
                crf = 28,
                audioBitrateKbps = 128,
                fastRemux = false,
            )
        MediaConverterPreset.AUDIO_ONLY ->
            current.copy(
                outputType = MediaOutputType.AUDIO,
                audioContainer = AudioContainer.MP3,
                audioCodec = AudioCodec.MP3,
                audioBitrateKbps = 192,
                fastRemux = false,
            )
        MediaConverterPreset.FAST_REMUX ->
            current.copy(
                outputType = MediaOutputType.VIDEO,
                videoCodec = VideoCodec.COPY,
                audioCodec = AudioCodec.COPY,
                videoBitrateKbps = null,
                audioBitrateKbps = null,
                resolutionHeight = null,
                fps = null,
                crf = null,
                fastRemux = true,
            )
    }
