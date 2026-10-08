package com.junkfood.seal.util

import com.junkfood.seal.ui.page.settings.network.Cookie

internal object NetscapeCookieParser {
    private const val HTTP_ONLY_PREFIX = "#HttpOnly_"

    fun accepts(content: String): Boolean = content.lineSequence().any { line ->
        val row = line.trimStart()
        '\t' in row && (!row.startsWith('#') || row.startsWith(HTTP_ONLY_PREFIX))
    }

    fun parse(content: String, now: Long = System.currentTimeMillis() / 1000L): List<Cookie> =
        content.lineSequence().mapNotNull { line ->
            val row = line.trimEnd('\r').trimStart()
            val httpOnly = row.startsWith(HTTP_ONLY_PREFIX)
            if (row.isBlank() || (row.startsWith('#') && !httpOnly)) return@mapNotNull null
            val parts = row.removePrefix(HTTP_ONLY_PREFIX).split('\t', limit = 7)
            if (parts.size != 7) return@mapNotNull null
            val expiry = parts[4].toLongOrNull() ?: return@mapNotNull null
            if (parts[0].isBlank() || parts[5].isBlank() ||
                (expiry > 0L && expiry <= now)) return@mapNotNull null
            val includeSubdomains = parts[1].equals("TRUE", ignoreCase = true)
            // MozillaCookieJar requires the domain's dot to agree with this flag.
            val domain = if (includeSubdomains) ".${parts[0].trimStart('.')}"
                else parts[0].trimStart('.')
            Cookie(
                domain = domain,
                name = parts[5],
                value = parts[6],
                includeSubdomains = includeSubdomains,
                path = parts[2].ifEmpty { "/" },
                secure = parts[3].equals("TRUE", ignoreCase = true),
                expiry = expiry,
                isHttpOnly = httpOnly,
            )
        }.toList()
}
