package com.junkfood.seal.ui.page.tools.mangaconverter

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.pdf.PdfDocument
import android.media.MediaScannerConnection
import androidx.documentfile.provider.DocumentFile
import com.junkfood.seal.ui.page.tools.ConverterPreferences
import java.io.File
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import kotlin.math.max
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

object MangaConverterEngine {
    data class Progress(
        val stage: String,
        val completed: Int,
        val total: Int,
        val current: String = "",
    ) {
        val fraction: Float
            get() = if (total <= 0) 0f else (completed.toFloat() / total.toFloat()).coerceIn(0f, 1f)
    }

    suspend fun convert(
        context: Context,
        input: MangaInput,
        settings: MangaConverterSettings,
        onProgress: suspend (Progress) -> Unit = {},
    ): MangaConversionResult = withContext(Dispatchers.IO) {
        coroutineContext.ensureActive()
        val inputExtension = input.name.substringAfterLast('.', "").lowercase()
        if (inputExtension !in setOf("zip", "cbz")) {
            throw IOException("Only ZIP and CBZ input files are supported")
        }
        if (inputExtension != settings.mode.sourceExtension) {
            throw IOException(
                "${settings.mode.label} expects a .${settings.mode.sourceExtension.uppercase()} input: ${input.name}"
            )
        }

        val tempInput = copyToTemp(context, input, inputExtension)
        val outputDir = ConverterPreferences.mangaDirectory().apply { mkdirs() }
        if (!outputDir.exists() || !outputDir.canWrite()) {
            tempInput.delete()
            throw IOException("Output folder is not writable: ${outputDir.absolutePath}")
        }
        val outputFile =
            MangaConverterUtils.uniqueOutputFile(
                outputDir,
                input.name,
                settings.mode.targetExtension,
            )

        try {
            val pages =
                when (settings.mode) {
                    MangaConversionMode.ZIP_TO_CBZ,
                    MangaConversionMode.CBZ_TO_ZIP ->
                        repackArchive(tempInput, outputFile, settings, onProgress)
                    MangaConversionMode.ZIP_TO_PDF,
                    MangaConversionMode.CBZ_TO_PDF ->
                        archiveToPdf(tempInput, outputFile, settings, onProgress)
                }
            if (!outputFile.exists() || outputFile.length() <= 0L) {
                outputFile.delete()
                throw IOException("Conversion finished without creating an output file")
            }
            MediaScannerConnection.scanFile(context, arrayOf(outputFile.absolutePath), null, null)
            MangaConversionResult(input.name, outputFile.absolutePath, pages)
        } catch (t: Throwable) {
            outputFile.delete()
            throw t
        } finally {
            tempInput.delete()
        }
    }

    private fun copyToTemp(context: Context, input: MangaInput, extension: String): File {
        val tempDir = File(context.cacheDir, "kirin_manga_converter").apply { mkdirs() }
        val tempFile = File.createTempFile("manga_", ".${extension.ifBlank { "zip" }}", tempDir)
        context.contentResolver.openInputStream(input.uri)?.use { source ->
            tempFile.outputStream().buffered().use { sink -> source.copyTo(sink) }
        } ?: throw IOException("Unable to open ${input.name}")
        return tempFile
    }

    private suspend fun repackArchive(
        input: File,
        output: File,
        settings: MangaConverterSettings,
        onProgress: suspend (Progress) -> Unit,
    ): Int {
        ZipFile(input).use { zip ->
            val entries =
                zip.entries().asSequence()
                    .filterNot { it.isDirectory }
                    .filter { !settings.removeJunkFiles || !MangaConverterUtils.isJunkEntry(it.name) }
                    .toMutableList()
            if (settings.naturalSort) {
                val comparator = MangaConverterUtils.naturalComparator()
                entries.sortWith { a, b -> comparator.compare(a.name, b.name) }
            }
            val imageCount = entries.count { MangaConverterUtils.isImageEntry(it.name) }
            onProgress(Progress("Packing", 0, entries.size.coerceAtLeast(1)))
            ZipOutputStream(output.outputStream().buffered()).use { out ->
                entries.forEachIndexed { index, entry ->
                    coroutineContext.ensureActive()
                    val target = ZipEntry(entry.name)
                    target.time = entry.time
                    out.putNextEntry(target)
                    zip.getInputStream(entry).use { it.copyTo(out) }
                    out.closeEntry()
                    onProgress(Progress("Packing", index + 1, entries.size, entry.name))
                }
                if (
                    settings.mode == MangaConversionMode.ZIP_TO_CBZ &&
                        settings.includeComicInfo &&
                        entries.none { it.name.equals("ComicInfo.xml", ignoreCase = true) }
                ) {
                    val xml = comicInfoXml(settings)
                    out.putNextEntry(ZipEntry("ComicInfo.xml"))
                    out.write(xml.toByteArray(Charsets.UTF_8))
                    out.closeEntry()
                }
            }
            return imageCount
        }
    }

    private suspend fun archiveToPdf(
        input: File,
        output: File,
        settings: MangaConverterSettings,
        onProgress: suspend (Progress) -> Unit,
    ): Int {
        ZipFile(input).use { zip ->
            val images =
                zip.entries().asSequence()
                    .filterNot { it.isDirectory }
                    .filter { MangaConverterUtils.isImageEntry(it.name) }
                    .filter { !settings.removeJunkFiles || !MangaConverterUtils.isJunkEntry(it.name) }
                    .toMutableList()
            if (settings.naturalSort) {
                val comparator = MangaConverterUtils.naturalComparator()
                images.sortWith { a, b -> comparator.compare(a.name, b.name) }
            }
            if (images.isEmpty()) throw IOException("No supported image pages were found in the archive")

            val pdf = PdfDocument()
            try {
                images.forEachIndexed { index, entry ->
                    coroutineContext.ensureActive()
                    val bitmap = decodeSampled(zip.getInputStream(entry).readBytes(), 2600)
                        ?: throw IOException("Unable to decode page: ${entry.name}")
                    try {
                        val pageWidth = 1240
                        val pageHeight = 1754
                        val pageInfo =
                            PdfDocument.PageInfo.Builder(pageWidth, pageHeight, index + 1).create()
                        val page = pdf.startPage(pageInfo)
                        page.canvas.drawColor(Color.WHITE)
                        val destination = fitRect(bitmap.width, bitmap.height, pageWidth, pageHeight)
                        page.canvas.drawBitmap(bitmap, null, destination, Paint(Paint.FILTER_BITMAP_FLAG))
                        pdf.finishPage(page)
                    } finally {
                        bitmap.recycle()
                    }
                    onProgress(Progress("Creating PDF", index + 1, images.size, entry.name))
                }
                output.outputStream().buffered().use { stream -> pdf.writeTo(stream) }
            } finally {
                pdf.close()
            }
            return images.size
        }
    }

    private fun decodeSampled(bytes: ByteArray, maxDimension: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / sample > maxDimension) sample *= 2
        return BitmapFactory.decodeByteArray(
            bytes,
            0,
            bytes.size,
            BitmapFactory.Options().apply { inSampleSize = sample },
        )
    }

    private fun fitRect(sourceWidth: Int, sourceHeight: Int, pageWidth: Int, pageHeight: Int): Rect {
        val scale = minOf(pageWidth.toFloat() / sourceWidth, pageHeight.toFloat() / sourceHeight)
        val width = (sourceWidth * scale).toInt().coerceAtLeast(1)
        val height = (sourceHeight * scale).toInt().coerceAtLeast(1)
        val left = (pageWidth - width) / 2
        val top = (pageHeight - height) / 2
        return Rect(left, top, left + width, top + height)
    }

    private fun comicInfoXml(settings: MangaConverterSettings): String {
        val title = MangaConverterUtils.xmlEscape(settings.title.ifBlank { "KirinDL Manga" })
        val author = MangaConverterUtils.xmlEscape(settings.author)
        return buildString {
            append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n")
            append("<ComicInfo>\n")
            append("  <Title>$title</Title>\n")
            if (author.isNotBlank()) append("  <Writer>$author</Writer>\n")
            append("</ComicInfo>\n")
        }
    }

    fun describeUri(context: Context, uri: android.net.Uri): MangaInput {
        val document = DocumentFile.fromSingleUri(context, uri)
        return MangaInput(
            uri = uri,
            name = document?.name ?: uri.lastPathSegment ?: "manga.zip",
            sizeBytes = document?.length()?.takeIf { it > 0L },
        )
    }
}
