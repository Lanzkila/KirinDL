package com.junkfood.seal.ui.page.tools

import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaScannerConnection
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.junkfood.seal.util.DownloadUtil
import com.junkfood.seal.util.FileUtil
import com.junkfood.seal.util.PlaylistEntry
import com.junkfood.seal.util.VideoInfo
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import java.io.File
import java.io.IOException
import java.util.Locale
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

data class MediaTrackInfo(
    val index: Int,
    val type: String,
    val codec: String,
    val detail: String,
)

data class MediaInspection(
    val name: String,
    val sizeBytes: Long?,
    val durationMs: Long?,
    val width: Int?,
    val height: Int?,
    val bitrate: Int?,
    val tracks: List<MediaTrackInfo>,
)

data class MetadataEdits(
    val title: String = "",
    val artist: String = "",
    val album: String = "",
    val year: String = "",
    val genre: String = "",
    val removeExisting: Boolean = false,
)

data class ChapterMark(
    val title: String,
    val startSeconds: Double,
    val endSeconds: Double,
)

object KirinUtilityEngine {
    suspend fun fetchVideo(url: String): VideoInfo =
        withContext(Dispatchers.IO) {
            DownloadUtil.fetchVideoInfoFromUrl(url).getOrThrow()
        }

    suspend fun downloadSubtitleOnly(
        context: Context,
        url: String,
        language: String,
        includeManual: Boolean,
        includeAuto: Boolean,
        outputFormat: String,
    ): String = withContext(Dispatchers.IO) {
        require(includeManual || includeAuto) { "Choose manual/original subtitles, auto subtitles, or both." }
        require(language.isNotBlank()) { "Choose a subtitle language." }

        YoutubeDL.init(context.applicationContext)
        val outputDir =
            File(FileUtil.getExternalDownloadDirectory(), "Subtitles").apply { mkdirs() }
        if (!outputDir.exists() || !outputDir.canWrite()) {
            throw IOException("Subtitle output folder is not writable: ${outputDir.absolutePath}")
        }

        val request =
            YoutubeDLRequest(url).apply {
                addOption("--skip-download")
                if (includeManual) addOption("--write-subs")
                if (includeAuto) addOption("--write-auto-subs")
                addOption("--sub-langs", language)
                if (outputFormat.lowercase() != "original") {
                    addOption("--convert-subs", outputFormat.lowercase())
                }
                addOption("-P", outputDir.absolutePath)
                addOption("-o", "%(title).180B [%(id)s].%(ext)s")
                addOption("--no-playlist")
                addOption("-R", "3")
                addOption("--socket-timeout", "20")
            }

        YoutubeDL.getInstance().execute(request, "subtitle_tool", null)
        outputDir.absolutePath
    }

    suspend fun extractPlaylist(url: String) =
        withContext(Dispatchers.IO) {
            DownloadUtil.getPlaylistOrVideoInfo(url).getOrThrow()
        }

    fun playlistEntryUrl(entry: PlaylistEntry): String? {
        val raw = entry.url?.trim().orEmpty()
        if (raw.startsWith("http://") || raw.startsWith("https://")) return raw
        val id = entry.id?.trim().orEmpty()
        if (id.isBlank()) return raw.takeIf { it.isNotBlank() }
        val key = entry.ieKey.orEmpty().lowercase(Locale.US)
        return when {
            "youtube" in key -> "https://www.youtube.com/watch?v=$id"
            raw.isNotBlank() -> raw
            else -> id
        }
    }

    suspend fun inspectMedia(context: Context, uri: Uri): MediaInspection =
        withContext(Dispatchers.IO) {
            val doc = DocumentFile.fromSingleUri(context, uri)
            val name = doc?.name ?: uri.lastPathSegment ?: "media"
            val size = doc?.length()?.takeIf { it > 0L }

            var durationMs: Long? = null
            var width: Int? = null
            var height: Int? = null
            var bitrate: Int? = null

            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, uri)
                durationMs =
                    retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                        ?.toLongOrNull()
                width =
                    retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
                        ?.toIntOrNull()
                height =
                    retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
                        ?.toIntOrNull()
                bitrate =
                    retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)
                        ?.toIntOrNull()
            } finally {
                runCatching { retriever.release() }
            }

            val tracks = mutableListOf<MediaTrackInfo>()
            val extractor = MediaExtractor()
            try {
                extractor.setDataSource(context, uri, null)
                for (index in 0 until extractor.trackCount) {
                    val format = extractor.getTrackFormat(index)
                    val mime = format.stringOrNull(MediaFormat.KEY_MIME).orEmpty()
                    val type =
                        when {
                            mime.startsWith("video/") -> "Video"
                            mime.startsWith("audio/") -> "Audio"
                            mime.startsWith("text/") ||
                                mime.contains("subtitle", true) ||
                                mime.contains("subrip", true) -> "Subtitle"
                            else -> "Other"
                        }
                    val details = buildList {
                        format.intOrNull(MediaFormat.KEY_WIDTH)?.let { w ->
                            format.intOrNull(MediaFormat.KEY_HEIGHT)?.let { h -> add("${w}x$h") }
                        }
                        format.intOrNull(MediaFormat.KEY_FRAME_RATE)?.let { add("$it fps") }
                        format.intOrNull(MediaFormat.KEY_BIT_RATE)?.let { add("${it / 1000} kbps") }
                        format.intOrNull(MediaFormat.KEY_SAMPLE_RATE)?.let { add("$it Hz") }
                        format.intOrNull(MediaFormat.KEY_CHANNEL_COUNT)?.let { add("$it ch") }
                        format.stringOrNull(MediaFormat.KEY_LANGUAGE)
                            ?.takeIf { it.isNotBlank() }
                            ?.let { add(it) }
                    }.joinToString(" • ")

                    tracks +=
                        MediaTrackInfo(
                            index = index,
                            type = type,
                            codec = mime.ifBlank { "unknown" },
                            detail = details,
                        )
                }
            } finally {
                extractor.release()
            }

            MediaInspection(
                name = name,
                sizeBytes = size,
                durationMs = durationMs,
                width = width,
                height = height,
                bitrate = bitrate,
                tracks = tracks,
            )
        }

    suspend fun editMetadata(
        context: Context,
        uri: Uri,
        edits: MetadataEdits,
        artworkUri: Uri? = null,
    ): String = withContext(Dispatchers.IO) {
        YoutubeDL.init(context.applicationContext)
        val input = copyUriToTemp(context, uri, "kirin_metadata")
        val sourceName = DocumentFile.fromSingleUri(context, uri)?.name ?: input.name
        val extension = sourceName.substringAfterLast('.', "mkv").take(10).ifBlank { "mkv" }
        val outputDir =
            File(FileUtil.getExternalDownloadDirectory(), "Metadata Editor").apply { mkdirs() }
        val output = uniqueOutput(outputDir, sourceName.substringBeforeLast('.', sourceName), extension)

        val artwork =
            artworkUri?.let { copyUriToTemp(context, it, "kirin_artwork") }
        val audioArtworkExtensions = setOf("mp3", "m4a", "aac", "flac", "ogg", "opus")
        if (artwork != null && extension.lowercase(Locale.US) !in audioArtworkExtensions) {
            input.delete()
            artwork.delete()
            throw IOException("Cover artwork replacement is supported for audio files only.")
        }

        val args = buildList {
            addAll(listOf("-y", "-hide_banner", "-nostdin", "-i", input.absolutePath))
            if (artwork != null) {
                addAll(listOf("-i", artwork.absolutePath))
                addAll(listOf("-map", "0:a?", "-map", "1:v:0"))
                addAll(listOf("-c:a", "copy", "-c:v", "mjpeg"))
                addAll(listOf("-disposition:v:0", "attached_pic"))
            } else {
                addAll(listOf("-map", "0", "-c", "copy"))
            }
            if (edits.removeExisting) addAll(listOf("-map_metadata", "-1"))
            else addAll(listOf("-map_metadata", "0"))
            edits.title.takeIf { it.isNotBlank() }?.let { addAll(listOf("-metadata", "title=$it")) }
            edits.artist.takeIf { it.isNotBlank() }?.let { addAll(listOf("-metadata", "artist=$it")) }
            edits.album.takeIf { it.isNotBlank() }?.let { addAll(listOf("-metadata", "album=$it")) }
            edits.year.takeIf { it.isNotBlank() }?.let { addAll(listOf("-metadata", "date=$it")) }
            edits.genre.takeIf { it.isNotBlank() }?.let { addAll(listOf("-metadata", "genre=$it")) }
            add(output.absolutePath)
        }

        try {
            val result = runFfmpeg(context, args)
            if (result.first != 0 || !output.exists() || output.length() == 0L) {
                output.delete()
                throw IOException("FFmpeg metadata edit failed with exit code ${result.first}")
            }
            scan(context, output)
            output.absolutePath
        } finally {
            input.delete()
            artwork?.delete()
        }
    }

    suspend fun readChapters(context: Context, uri: Uri): List<ChapterMark> =
        withContext(Dispatchers.IO) {
            YoutubeDL.init(context.applicationContext)
            val input = copyUriToTemp(context, uri, "kirin_chapters")
            try {
                val result =
                    runFfmpeg(
                        context,
                        listOf(
                            "-hide_banner",
                            "-nostdin",
                            "-i",
                            input.absolutePath,
                            "-f",
                            "ffmetadata",
                            "-",
                        ),
                    )
                parseFfmetadataChapters(result.second)
            } finally {
                input.delete()
            }
        }

    suspend fun makeClip(
        context: Context,
        uri: Uri,
        startSeconds: Double,
        endSeconds: Double,
        label: String? = null,
    ): String = withContext(Dispatchers.IO) {
        require(startSeconds >= 0.0) { "Start time must be 0 or greater." }
        require(endSeconds > startSeconds) { "End time must be greater than start time." }

        YoutubeDL.init(context.applicationContext)
        val input = copyUriToTemp(context, uri, "kirin_clip")
        val sourceName = DocumentFile.fromSingleUri(context, uri)?.name ?: input.name
        val extension = sourceName.substringAfterLast('.', "mp4").take(10).ifBlank { "mp4" }
        val outputDir = File(FileUtil.getExternalDownloadDirectory(), "Clips").apply { mkdirs() }
        val output =
            uniqueOutput(
                outputDir,
                sourceName.substringBeforeLast('.', sourceName) +
                    " - " +
                    label?.takeIf { it.isNotBlank() }.orEmpty().ifBlank { "clip" },
                extension,
            )

        val duration = endSeconds - startSeconds
        val args =
            listOf(
                "-y",
                "-hide_banner",
                "-nostdin",
                "-ss",
                seconds(startSeconds),
                "-i",
                input.absolutePath,
                "-t",
                seconds(duration),
                "-map",
                "0",
                "-c",
                "copy",
                "-avoid_negative_ts",
                "make_zero",
                output.absolutePath,
            )

        try {
            val result = runFfmpeg(context, args)
            if (result.first != 0 || !output.exists() || output.length() == 0L) {
                output.delete()
                throw IOException("FFmpeg clip failed with exit code ${result.first}")
            }
            scan(context, output)
            output.absolutePath
        } finally {
            input.delete()
        }
    }

    private fun MediaFormat.intOrNull(key: String): Int? =
        runCatching { if (containsKey(key)) getInteger(key) else null }.getOrNull()

    private fun MediaFormat.stringOrNull(key: String): String? =
        runCatching { if (containsKey(key)) getString(key) else null }.getOrNull()

    private fun copyUriToTemp(context: Context, uri: Uri, prefix: String): File {
        val name = DocumentFile.fromSingleUri(context, uri)?.name ?: "media.bin"
        val extension = name.substringAfterLast('.', "bin").take(10).ifBlank { "bin" }
        val dir = File(context.cacheDir, "kirin_utility_tools").apply { mkdirs() }
        val target = File.createTempFile("${prefix}_", ".$extension", dir)
        context.contentResolver.openInputStream(uri)?.use { source ->
            target.outputStream().buffered().use { sink -> source.copyTo(sink) }
        } ?: throw IOException("Unable to open $name")
        return target
    }

    private fun uniqueOutput(directory: File, baseName: String, extension: String): File {
        val safe =
            baseName
                .replace(Regex("[\\\\/:*?\"<>|\\p{Cntrl}]"), "_")
                .trim()
                .take(120)
                .ifBlank { "KirinDL" }
        var file = File(directory, "$safe.$extension")
        var index = 1
        while (file.exists()) {
            file = File(directory, "$safe ($index).$extension")
            index++
        }
        return file
    }

    private suspend fun runFfmpeg(
        context: Context,
        arguments: List<String>,
    ): Pair<Int, List<String>> {
        FFmpeg.init(context.applicationContext)
        val instance = YoutubeDL.getInstance()
        val ffmpegPath =
            reflectedFile(instance, "ffmpegPath")
                ?: File(context.applicationInfo.nativeLibraryDir, "libffmpeg.so")
        if (!ffmpegPath.exists()) throw IOException("Bundled FFmpeg executable was not found")

        val command = mutableListOf(ffmpegPath.absolutePath).apply { addAll(arguments) }
        val builder = ProcessBuilder(command).redirectErrorStream(true)
        reflectedString(instance, "ENV_LD_LIBRARY_PATH")?.takeIf { it.isNotBlank() }?.let {
            builder.environment()["LD_LIBRARY_PATH"] = it
        }
        reflectedString(instance, "TMPDIR")?.takeIf { it.isNotBlank() }?.let {
            builder.environment()["TMPDIR"] = it
        }
        reflectedFile(instance, "binDir")?.absolutePath?.let { binDir ->
            val oldPath = builder.environment()["PATH"].orEmpty()
            builder.environment()["PATH"] = if (oldPath.isBlank()) binDir else "$oldPath:$binDir"
        }

        val output = mutableListOf<String>()
        val process = builder.start()
        try {
            process.inputStream.bufferedReader().useLines { lines ->
                lines.forEach {
                    coroutineContext.ensureActive()
                    output += it
                }
            }
            return process.waitFor() to output
        } catch (t: Throwable) {
            process.destroy()
            throw t
        }
    }

    private fun parseFfmetadataChapters(lines: List<String>): List<ChapterMark> {
        val chapters = mutableListOf<ChapterMark>()
        var inChapter = false
        var timeBaseNum = 1.0
        var timeBaseDen = 1000.0
        var start: Long? = null
        var end: Long? = null
        var title = ""

        fun flush() {
            val s = start
            val e = end
            if (inChapter && s != null && e != null && e > s) {
                chapters +=
                    ChapterMark(
                        title = title.ifBlank { "Chapter ${chapters.size + 1}" },
                        startSeconds = s * timeBaseNum / timeBaseDen,
                        endSeconds = e * timeBaseNum / timeBaseDen,
                    )
            }
            start = null
            end = null
            title = ""
            timeBaseNum = 1.0
            timeBaseDen = 1000.0
        }

        lines.forEach { raw ->
            val line = raw.trim()
            when {
                line == "[CHAPTER]" -> {
                    if (inChapter) flush()
                    inChapter = true
                }
                inChapter && line.startsWith("TIMEBASE=") -> {
                    val parts = line.substringAfter('=').split('/')
                    timeBaseNum = parts.getOrNull(0)?.toDoubleOrNull() ?: 1.0
                    timeBaseDen = parts.getOrNull(1)?.toDoubleOrNull() ?: 1000.0
                }
                inChapter && line.startsWith("START=") -> start = line.substringAfter('=').toLongOrNull()
                inChapter && line.startsWith("END=") -> end = line.substringAfter('=').toLongOrNull()
                inChapter && line.startsWith("title=") -> title = line.substringAfter('=')
                line.startsWith("[") && line != "[CHAPTER]" && inChapter -> {
                    flush()
                    inChapter = false
                }
            }
        }
        if (inChapter) flush()
        return chapters
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

    private fun scan(context: Context, file: File) {
        MediaScannerConnection.scanFile(context, arrayOf(file.absolutePath), null, null)
    }

    private fun seconds(value: Double): String = String.format(Locale.US, "%.3f", value)
}
