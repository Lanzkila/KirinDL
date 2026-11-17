// DISABLED: Telegram now uses the existing yt-dlp TelegramEmbedIE path.
// Original implementation/test kept commented for reference; do not re-enable a separate engine.
// package com.junkfood.seal.util.telegram.models
// 
// data class TelegramMedia(
//     val id: String,
//     val kind: String,
//     val downloadUrl: String,
//     val thumbnailUrl: String = "",
//     val fileName: String = "",
//     val mimeType: String = "",
// )
// 
// data class TelegramPostPreview(
//     val id: Long,
//     val url: String,
//     val title: String,
//     val text: String,
//     val date: String = "",
//     val views: String = "",
//     val media: List<TelegramMedia> = emptyList(),
// )
// 
// data class TelegramChannelPreview(
//     val username: String,
//     val title: String,
//     val description: String = "",
//     val subscribers: String = "",
//     val posts: List<TelegramPostPreview>,
//     val nextBeforeId: Long? = null,
//     val hasMore: Boolean = false,
//     val isSinglePost: Boolean = false,
// )
// 
// data class TelegramDownloadedMedia(
//     val sourceUrl: String,
//     val filePath: String,
//     val fileName: String,
//     val mimeType: String,
//     val sizeBytes: Long,
// )
// 