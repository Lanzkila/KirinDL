// DISABLED: Telegram now uses the existing yt-dlp TelegramEmbedIE path.
// Original implementation/test kept commented for reference; do not re-enable a separate engine.
// package com.junkfood.seal.util.telegram.url
// 
// import java.net.URI
// 
// data class TelegramLink(
//     val username: String,
//     val messageId: Long? = null,
// ) {
//     val isSinglePost: Boolean
//         get() = messageId != null
// 
//     val publicFeedUrl: String
//         get() = "https://t.me/s/$username"
// }
// 
// /** Accepts public t.me channel links and individual public post links only. */
// object TelegramUrlParser {
//     private val usernamePattern = Regex("^[A-Za-z0-9_]{5,32}$")
//     private val reserved =
//         setOf(
//             "addstickers",
//             "confirmphone",
//             "joinchat",
//             "login",
//             "proxy",
//             "setlanguage",
//             "share",
//             "socks",
//             "iv",
//             "c",
//         )
// 
//     fun parse(raw: String): TelegramLink? {
//         val value =
//             raw.trim().let {
//                 when {
//                     it.isBlank() -> return null
//                     "://" in it -> it
//                     else -> "https://$it"
//                 }
//             }
//         val uri = runCatching { URI(value) }.getOrNull() ?: return null
//         if (uri.scheme?.lowercase() != "https") return null
//         if (
//             uri.host?.lowercase() !in
//                 setOf("t.me", "www.t.me", "telegram.me", "www.telegram.me")
//         ) {
//             return null
//         }
//         if (uri.userInfo != null || uri.port != -1) return null
// 
//         var segments = uri.path.orEmpty().split('/').filter(String::isNotBlank)
//         if (segments.firstOrNull()?.equals("s", ignoreCase = true) == true) {
//             segments = segments.drop(1)
//         }
//         if (segments.isEmpty() || segments.size > 2) return null
// 
//         val username = segments[0]
//         if (!usernamePattern.matches(username) || username.lowercase() in reserved) return null
// 
//         val messageId =
//             if (segments.size == 2) {
//                 segments[1].toLongOrNull()?.takeIf { it > 0L } ?: return null
//             } else {
//                 null
//             }
// 
//         return TelegramLink(username = username, messageId = messageId)
//     }
// 
//     fun isPublicChannelUrl(raw: String): Boolean =
//         parse(raw)?.messageId == null && parse(raw) != null
// }
// 