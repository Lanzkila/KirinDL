package com.junkfood.seal.util

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import androidx.documentfile.provider.DocumentFile
import java.io.File
import java.util.Locale

/** Local file handoff shared by Play, history, notifications, and Share. */
internal object LocalFileIntents {
    fun open(context: Context, path: String?): Intent? =
        fileIntent(context, path)?.apply {
            action = Intent.ACTION_VIEW
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

    fun share(context: Context, path: String?): Intent? =
        fileIntent(context, path)?.apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_STREAM, data)
        }

    private fun fileIntent(context: Context, path: String?): Intent? {
        if (path.isNullOrBlank()) return null
        return runCatching {
            val suppliedUri = Uri.parse(path).normalizeScheme()
            val uri = when (suppliedUri.scheme?.lowercase(Locale.ROOT)) {
                "content" -> DocumentFile.fromSingleUri(context, suppliedUri)
                    ?.takeIf { it.exists() && !it.isDirectory }?.uri
                null, "file" -> {
                    val file = if (suppliedUri.scheme == null) File(path)
                        else suppliedUri.path?.let(::File)
                    file?.takeIf { it.isFile }?.let {
                        FileProvider.getUriForFile(context, "${context.packageName}.provider", it)
                    }
                }
                // Playback must never fall back to a remote source URL.
                else -> null
            } ?: return null

            val resolver = context.contentResolver
            val document = DocumentFile.fromSingleUri(context, uri)
            val mimeType = resolveMimeType(resolver.getType(uri), document?.name ?: path)
            Intent().apply {
                setDataAndType(uri, mimeType)
                clipData = ClipData(document?.name ?: "KirinDL file", arrayOf(mimeType), ClipData.Item(uri))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        }.getOrNull()
    }

    private fun resolveMimeType(providerType: String?, name: String): String {
        val extension = name.substringAfterLast('.', "").lowercase(Locale.ROOT)
        val fromName = when (extension) {
            "mkv" -> "video/x-matroska"
            "mka" -> "audio/x-matroska"
            "opus" -> "audio/ogg"
            "m4a", "m4b" -> "audio/mp4"
            else -> MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
        }
        val concreteProviderType = providerType?.lowercase(Locale.ROOT)?.takeUnless {
            it == "application/octet-stream" || it == "*/*" || it == "media/*"
        }
        val canonicalProviderType = when (concreteProviderType) {
            "audio/x-m4a", "audio/m4a", "audio/x-m4b" -> "audio/mp4"
            "video/mkv" -> "video/x-matroska"
            else -> concreteProviderType
        }
        // Known container extensions take precedence over inconsistent provider MIME types.
        return if (extension in setOf("m4a", "m4b", "mkv", "mka", "opus")) {
            fromName!!
        } else canonicalProviderType ?: fromName ?: "application/octet-stream"
    }
}
