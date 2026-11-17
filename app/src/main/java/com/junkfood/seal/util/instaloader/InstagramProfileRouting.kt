// DISABLED: legacy Instaloader integration; Instagram is handled by the existing Gallery-DL engine.
// package com.junkfood.seal.util.instaloader
//
// import java.net.URI
//
// /** Strictly recognizes Instagram profile URLs without intercepting posts, reels, or other routes. */
// object InstagramProfileRouting {
//     private val usernamePattern = Regex("^[A-Za-z0-9._]{1,30}$")
//     private val reservedSegments =
//         setOf(
//             "p",
//             "reel",
//             "reels",
//             "tv",
//             "stories",
//             "explore",
//             "accounts",
//             "direct",
//             "about",
//             "developer",
//             "api",
//             "graphql",
//             "challenge",
//             "web",
//             "legal",
//         )
//
//     fun isProfileUrl(raw: String): Boolean {
//         val uri = runCatching { URI(raw.trim()) }.getOrNull() ?: return false
//         if (uri.scheme?.lowercase() != "https") return false
//         if (uri.host?.lowercase() !in setOf("instagram.com", "www.instagram.com")) return false
//         if (uri.userInfo != null || uri.port != -1) return false
//
//         val segments = uri.rawPath.orEmpty().split('/').filter(String::isNotEmpty)
//         if (segments.size != 1) return false
//
//         val candidate = segments.single()
//         return usernamePattern.matches(candidate) &&
//             candidate.lowercase() !in reservedSegments
//     }
// }
//