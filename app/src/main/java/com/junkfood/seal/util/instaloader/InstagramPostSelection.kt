// DISABLED: legacy Instaloader integration; Instagram is handled by the existing Gallery-DL engine.
// package com.junkfood.seal.util.instaloader
//
// import java.net.URI
//
// /** URL validation and selection helpers for public Instagram post previews. */
// object InstagramPostSelection {
//     private val shortcodePattern = Regex("^[A-Za-z0-9_-]+$")
//
//     fun canonicalPostUrl(raw: String): String? {
//         val uri = runCatching { URI(raw.trim()) }.getOrNull() ?: return null
//         if (uri.scheme?.lowercase() != "https") return null
//         if (uri.host?.lowercase() !in setOf("instagram.com", "www.instagram.com")) return null
//         if (uri.userInfo != null || uri.port != -1) return null
//
//         val segments = uri.rawPath.orEmpty().split('/').filter(String::isNotEmpty)
//         if (segments.size != 2) return null
//
//         val route = segments[0].lowercase()
//         val shortcode = segments[1]
//         if (route !in setOf("p", "reel", "tv") || !shortcodePattern.matches(shortcode)) {
//             return null
//         }
//
//         return "https://www.instagram.com/$route/$shortcode/"
//     }
//
//     fun toggle(selected: Set<String>, rawUrl: String): Set<String> {
//         val canonical = canonicalPostUrl(rawUrl) ?: return selected
//         return if (canonical in selected) selected - canonical else selected + canonical
//     }
//
//     fun selectAll(rawUrls: Iterable<String>): Set<String> =
//         rawUrls.mapNotNull(::canonicalPostUrl).toSet()
//
//     /** Keeps the profile's preview order when turning selected cards into queue URLs. */
//     fun orderedSelectedUrls(rawUrls: Iterable<String>, selected: Set<String>): List<String> =
//         rawUrls
//             .mapNotNull(::canonicalPostUrl)
//             .filter(selected::contains)
//             .distinct()
// }
//