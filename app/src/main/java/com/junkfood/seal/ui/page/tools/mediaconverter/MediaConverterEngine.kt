package com.junkfood.seal.ui.page.tools.mediaconverter

import android.content.Context
import android.media.MediaMetadataRetriever
import android.media.MediaScannerConnection
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.junkfood.seal.ui.page.tools.ConverterPreferences
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import java.io.File
import java.io.IOException
import java.util.Locale
import kotlin.math.max
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

object MediaConverterEngine {
    data class Progress(
        val stage: String,
        val fraction: Float?,
        val detail: String = "",
    )

    suspend fun convert(
        context: Context,
        input: MediaInput,
        settings: MediaConverterSettings,
        onProgress: suspend (Progress) -> Unit = {},
    ): MediaConversionResult = withContext(Dispatchers.IO) {
        coroutineContext.ensureActive()
        onProgress(Progress("Preparing", 0f, input.name))

        YoutubeDL.init(context.applicationContext)
        FFmpeg.init(context.applicationContext)
        val tempInput = copyToTemp(context, input)
        val durationSeconds = readDurationSeconds(context, input.uri)
        val outputDir = ConverterPreferences.mediaDirectory().apply { mkdirs() }
        if (!outputDir.exists() || !outputDir.canWrite()) {
            tempInput.delete()
            throw IOException("Output folder is not writable: ${outputDir.absolutePath}")
        }

        val outputFile = uniqueOutputFile(outputDir, input.name, settings.outputExtension)
        val command = buildCommand(tempInput, outputFile, settings)

        try {
            val exitCode = runFfmpeg(context, command) { line ->
                val fraction = parseProgressFraction(line, durationSeconds)
                if (fraction != null) {
                    onProgress(Progress("Converting", fraction, input.name))
                }
            }
            if (exitCode != 0) {
                outputFile.delete()
                throw IOException("FFmpeg conversion failed with exit code $exitCode")
            }
            if (!outputFile.exists() || outputFile.length() <= 0L) {
                outputFile.delete()
                throw IOException("FFmpeg finished without creating an output file")
            }
            MediaScannerConnection.scanFile(
                context,
                arrayOf(outputFile.absolutePath),
                null,
                null,
            )
            onProgress(Progress("Done", 1f, outputFile.name))
            MediaConversionResult(input.name, outputFile.absolutePath)
        } finally {
            tempInput.delete()
        }
    }

    private fun copyToTemp(context: Context, input: MediaInput): File {
        val extension = input.name.substringAfterLast('.', "bin").take(10).ifBlank { "bin" }
        val tempDir = File(context.cacheDir, "kirin_media_converter").apply { mkdirs() }
        val tempFile = File.createTempFile("media_", ".${extension}", tempDir)
        context.contentResolver.openInputStream(input.uri)?.use { source ->
            tempFile.outputStream().buffered().use { sink -> source.copyTo(sink) }
        } ?: throw IOException("Unable to open ${input.name}")
        return tempFile
    }

    private fun readDurationSeconds(context: Context, uri: Uri): Double? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toDoubleOrNull()
                ?.div(1000.0)
        } catch (_: Throwable) {
            null
        } finally {
            runCatching { retriever.release() }
        }
    }

    private fun uniqueOutputFile(directory: File, sourceName: String, extension: String): File {
        val rawBase = sourceName.substringBeforeLast('.', sourceName)
        val safeBase =
            rawBase.replace(Regex("[\\\\/:*?\"<>|\\p{Cntrl}]"), "_")
                .trim()
                .take(120)
                .ifBlank { "KirinDL-converted" }
        var candidate = File(directory, "$safeBase.$extension")
        var index = 1
        while (candidate.exists()) {
            candidate = File(directory, "$safeBase ($index).$extension")
            index++
        }
        return candidate
    }

    private fun buildCommand(
        input: File,
        output: File,
        settings: MediaConverterSettings,
    ): List<String> = buildList {
        addAll(listOf("-y", "-hide_banner", "-nostdin"))
        settings.trimStartSeconds?.takeIf { it > 0.0 }?.let {
            addAll(listOf("-ss", formatSeconds(it)))
        }
        addAll(listOf("-i", input.absolutePath))
        settings.trimEndSeconds?.takeIf { it > 0.0 }?.let {
            addAll(listOf("-to", formatSeconds(it)))
        }

        if (settings.keepMetadata) addAll(listOf("-map_metadata", "0"))
        else addAll(listOf("-map_metadata", "-1"))

        if (settings.outputType == MediaOutputType.AUDIO) {
            addAll(listOf("-map", "0:a:0?", "-vn"))
            settings.audioCodec.ffmpegName?.let { addAll(listOf("-c:a", it)) }
            settings.audioBitrateKbps?.takeIf { it > 0 && settings.audioCodec != AudioCodec.COPY }?.let {
                addAll(listOf("-b:a", "${it}k"))
            }
            settings.sampleRate?.takeIf { it > 0 }?.let { addAll(listOf("-ar", it.toString())) }
            settings.audioChannels?.takeIf { it in 1..8 }?.let { addAll(listOf("-ac", it.toString())) }
        } else {
            addAll(listOf("-map", "0:v:0?"))
            if (!settings.removeAudio) addAll(listOf("-map", "0:a?"))
            if (settings.keepSubtitles) addAll(listOf("-map", "0:s?"))

            val videoCodec = if (settings.fastRemux) VideoCodec.COPY else settings.videoCodec
            val audioCodec = if (settings.fastRemux) AudioCodec.COPY else settings.audioCodec
            videoCodec.ffmpegName?.let { addAll(listOf("-c:v", it)) }
            if (!settings.removeAudio) {
                audioCodec.ffmpegName?.let { addAll(listOf("-c:a", it)) }
            } else {
                add("-an")
            }

            if (!settings.fastRemux && videoCodec != VideoCodec.COPY) {
                settings.videoBitrateKbps?.takeIf { it > 0 }?.let {
                    addAll(listOf("-b:v", "${it}k"))
                }
                if (settings.videoBitrateKbps == null) {
                    settings.crf?.coerceIn(0, 51)?.let { addAll(listOf("-crf", it.toString())) }
                }
                settings.resolutionHeight?.takeIf { it > 0 }?.let {
                    addAll(listOf("-vf", "scale=-2:$it"))
                }
                settings.fps?.takeIf { it > 0 }?.let { addAll(listOf("-r", it.toString())) }
                if (videoCodec == VideoCodec.H264 || videoCodec == VideoCodec.HEVC) {
                    addAll(listOf("-preset", "medium"))
                }
            }

            if (!settings.removeAudio && audioCodec != AudioCodec.COPY) {
                settings.audioBitrateKbps?.takeIf { it > 0 }?.let {
                    addAll(listOf("-b:a", "${it}k"))
                }
                settings.sampleRate?.takeIf { it > 0 }?.let { addAll(listOf("-ar", it.toString())) }
                settings.audioChannels?.takeIf { it in 1..8 }?.let { addAll(listOf("-ac", it.toString())) }
            }

            if (settings.keepSubtitles) {
                when (settings.videoContainer) {
                    VideoContainer.MP4, VideoContainer.MOV -> addAll(listOf("-c:s", "mov_text"))
                    else -> addAll(listOf("-c:s", "copy"))
                }
            } else {
                add("-sn")
            }
        }

        add(output.absolutePath)
    }

    private suspend fun runFfmpeg(
        context: Context,
        arguments: List<String>,
        onLine: suspend (String) -> Unit,
    ): Int {
        val youtubeDl = YoutubeDL.getInstance()
        val ffmpegPath = reflectedFile(youtubeDl, "ffmpegPath")
            ?: File(context.applicationInfo.nativeLibraryDir, "libffmpeg.so")
        if (!ffmpegPath.exists()) throw IOException("Bundled FFmpeg executable was not found")

        val command = mutableListOf(ffmpegPath.absolutePath).apply { addAll(arguments) }
        val builder = ProcessBuilder(command).redirectErrorStream(true)
        reflectedString(youtubeDl, "ENV_LD_LIBRARY_PATH")?.takeIf { it.isNotBlank() }?.let {
            builder.environment()["LD_LIBRARY_PATH"] = it
        }
        reflectedString(youtubeDl, "TMPDIR")?.takeIf { it.isNotBlank() }?.let {
            builder.environment()["TMPDIR"] = it
        }
        reflectedFile(youtubeDl, "binDir")?.absolutePath?.let { binDir ->
            val oldPath = builder.environment()["PATH"].orEmpty()
            builder.environment()["PATH"] = if (oldPath.isBlank()) binDir else "$oldPath:$binDir"
        }

        val process = builder.start()
        try {
            process.inputStream.bufferedReader().use { reader ->
                while (true) {
                    val line = reader.readLine() ?: break
                    coroutineContext.ensureActive()
                    onLine(line)
                }
            }
            return process.waitFor()
        } catch (t: Throwable) {
            process.destroy()
            throw t
        }
    }

    private fun reflectedFile(instance: Any, fieldName: String): File? =
        runCatching {
            instance.javaClass.getDeclaredField(fieldName).run {
                isAccessible = true
                get(instance) as? File
            }
        }.getOrNull()

    private fun reflectedString(instance: Any, fieldName: String): String? =
        runCatching {
            instance.javaClass.getDeclaredField(fieldName).run {
                isAccessible = true
                get(instance) as? String
            }
        }.getOrNull()

    private fun parseProgressFraction(line: String, durationSeconds: Double?): Float? {
        if (durationSeconds == null || durationSeconds <= 0.0) return null
        val match = Regex("time=(\\d{2}):(\\d{2}):(\\d{2}(?:\\.\\d+)?)").find(line) ?: return null
        val hours = match.groupValues[1].toDoubleOrNull() ?: return null
        val minutes = match.groupValues[2].toDoubleOrNull() ?: return null
        val seconds = match.groupValues[3].toDoubleOrNull() ?: return null
        val elapsed = hours * 3600.0 + minutes * 60.0 + seconds
        return (elapsed / max(durationSeconds, 0.001)).toFloat().coerceIn(0f, 0.99f)
    }

    private fun formatSeconds(value: Double): String = String.format(Locale.US, "%.3f", value)

    fun describeUri(context: Context, uri: Uri): MediaInput {
        val document = DocumentFile.fromSingleUri(context, uri)
        return MediaInput(
            uri = uri,
            name = document?.name ?: uri.lastPathSegment ?: "media",
            sizeBytes = document?.length()?.takeIf { it > 0L },
        )
    }
}
