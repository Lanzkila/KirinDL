// DISABLED: Telegram now uses the existing yt-dlp TelegramEmbedIE path.
// Original implementation/test kept commented for reference; do not re-enable a separate engine.
// package com.junkfood.seal.util.telegram.preview
// 
// import android.content.Context
// import com.junkfood.seal.util.telegram.TelegramPythonRuntime
// import com.junkfood.seal.util.telegram.models.TelegramChannelPreview
// import com.junkfood.seal.util.telegram.models.TelegramMedia
// import com.junkfood.seal.util.telegram.models.TelegramPostPreview
// import com.junkfood.seal.util.telegram.url.TelegramUrlParser
// import java.io.IOException
// import kotlinx.coroutines.Dispatchers
// import kotlinx.coroutines.withContext
// import org.json.JSONObject
// 
// /** Read-only preview engine for public Telegram channels and public posts. */
// object TelegramPreviewEngine {
//     const val DEFAULT_LIMIT = 20
//     const val MAX_LIMIT = 40
// 
//     suspend fun preview(
//         context: Context,
//         url: String,
//         limit: Int = DEFAULT_LIMIT,
//         beforeId: Long? = null,
//     ): Result<TelegramChannelPreview> =
//         withContext(Dispatchers.IO) {
//             runCatching {
//                 requireNotNull(TelegramUrlParser.parse(url)) {
//                     "Enter a public Telegram channel or post URL"
//                 }
//                 val raw =
//                     TelegramPythonRuntime.bridge(context).callAttr(
//                         "preview",
//                         url.trim(),
//                         limit.coerceIn(1, MAX_LIMIT),
//                         beforeId?.toString().orEmpty(),
//                     ).toString()
//                 val result = JSONObject(raw)
//                 if (!result.optBoolean("ok", false)) {
//                     throw IOException(
//                         result.optString("error").ifBlank {
//                             "Telegram could not load this public channel"
//                         }
//                     )
//                 }
// 
//                 val postsArray = result.optJSONArray("posts")
//                 val posts =
//                     buildList {
//                         if (postsArray != null) {
//                             for (index in 0 until postsArray.length()) {
//                                 val post = postsArray.optJSONObject(index) ?: continue
//                                 val mediaArray = post.optJSONArray("media")
//                                 val media =
//                                     buildList {
//                                         if (mediaArray != null) {
//                                             for (mediaIndex in 0 until mediaArray.length()) {
//                                                 val item =
//                                                     mediaArray.optJSONObject(mediaIndex) ?: continue
//                                                 val downloadUrl = item.optString("url")
//                                                 if (!downloadUrl.startsWith("https://")) continue
//                                                 add(
//                                                     TelegramMedia(
//                                                         id = item.optString("id"),
//                                                         kind = item.optString("kind").ifBlank { "file" },
//                                                         downloadUrl = downloadUrl,
//                                                         thumbnailUrl = item.optString("thumbnail_url"),
//                                                         fileName = item.optString("file_name"),
//                                                         mimeType = item.optString("mime_type"),
//                                                     )
//                                                 )
//                                             }
//                                         }
//                                     }
// 
//                                 val id = post.optLong("id").takeIf { it > 0L } ?: continue
//                                 add(
//                                     TelegramPostPreview(
//                                         id = id,
//                                         url = post.optString("url"),
//                                         title = post.optString("title"),
//                                         text = post.optString("text"),
//                                         date = post.optString("date"),
//                                         views = post.optString("views"),
//                                         media = media,
//                                     )
//                                 )
//                             }
//                         }
//                     }
// 
//                 TelegramChannelPreview(
//                     username = result.optString("username"),
//                     title = result.optString("title"),
//                     description = result.optString("description"),
//                     subscribers = result.optString("subscribers"),
//                     posts = posts,
//                     nextBeforeId =
//                         if (result.has("next_before_id") && !result.isNull("next_before_id")) {
//                             result.optLong("next_before_id").takeIf { it > 0L }
//                         } else {
//                             null
//                         },
//                     hasMore = result.optBoolean("has_more", false),
//                     isSinglePost = result.optBoolean("is_single_post", false),
//                 )
//             }
//         }
// }
// 