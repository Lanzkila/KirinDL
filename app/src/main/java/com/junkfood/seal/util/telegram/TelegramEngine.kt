// DISABLED: Telegram now uses the existing yt-dlp TelegramEmbedIE path.
// Original implementation/test kept commented for reference; do not re-enable a separate engine.
// package com.junkfood.seal.util.telegram
// 
// import android.content.Context
// import com.junkfood.seal.util.telegram.download.TelegramDownloadEngine
// import com.junkfood.seal.util.telegram.models.TelegramChannelPreview
// import com.junkfood.seal.util.telegram.models.TelegramDownloadedMedia
// import com.junkfood.seal.util.telegram.models.TelegramMedia
// import com.junkfood.seal.util.telegram.preview.TelegramPreviewEngine
// import com.junkfood.seal.util.telegram.url.TelegramLink
// import com.junkfood.seal.util.telegram.url.TelegramUrlParser
// import java.io.File
// 
// /** Public entry point for Telegram URL parsing, preview pagination and selected media downloads. */
// object TelegramEngine {
//     fun parseUrl(url: String): TelegramLink? = TelegramUrlParser.parse(url)
// 
//     suspend fun preview(
//         context: Context,
//         url: String,
//         limit: Int = TelegramPreviewEngine.DEFAULT_LIMIT,
//         beforeId: Long? = null,
//     ): Result<TelegramChannelPreview> =
//         TelegramPreviewEngine.preview(context, url, limit, beforeId)
// 
//     suspend fun downloadSelected(
//         context: Context,
//         media: List<TelegramMedia>,
//         destination: File = TelegramDownloadEngine.defaultDownloadDirectory(context),
//     ): Result<List<TelegramDownloadedMedia>> =
//         TelegramDownloadEngine.downloadSelected(context, media, destination)
// }
// 