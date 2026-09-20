package com.junkfood.seal.util

import android.net.Uri
import com.junkfood.seal.util.PreferenceUtil.getInt
import com.junkfood.seal.util.PreferenceUtil.getString
import com.junkfood.seal.util.PreferenceUtil.updateInt
import com.junkfood.seal.util.PreferenceUtil.updateString
import com.yausername.youtubedl_android.YoutubeDLRequest
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * KirinDL YouTube PO Token preferences.
 *
 * OFF keeps yt-dlp's normal client selection untouched.
 * PROVIDER_ASSIST adds mweb beside the default client so an installed compatible yt-dlp
 * PO Token provider can service mweb GVS requests. KirinDL does not pretend to generate a token.
 * MANUAL_GVS is an advanced fallback for a user-supplied mweb GVS token.
 *
 * The token is intentionally read at execution time and is not copied into DownloadPreferences,
 * queue snapshots, logs, or download history.
 */
enum class YouTubePoTokenMode(
    val value: Int,
    val title: String,
    val description: String,
) {
    OFF(
        0,
        "Off",
        "Use yt-dlp's normal YouTube client selection.",
    ),
    PROVIDER_ASSIST(
        1,
        "Auto provider assist",
        "Keep the default client and add mweb so a compatible installed PO Token provider can supply a token.",
    ),
    MANUAL_GVS(
        2,
        "Manual mweb GVS token",
        "Advanced fallback for a user-supplied mweb GVS PO Token.",
    );

    companion object {
        fun fromValue(value: Int): YouTubePoTokenMode = entries.firstOrNull { it.value == value } ?: OFF
    }
}

object YouTubePoTokenPreference {
    private const val MODE_KEY = "youtube_po_token_mode"
    private const val TOKEN_KEY = "youtube_po_token_gvs"

    private val mutableMode = MutableStateFlow(YouTubePoTokenMode.fromValue(MODE_KEY.getInt(0)))
    private val mutableToken = MutableStateFlow(TOKEN_KEY.getString())

    val mode: StateFlow<YouTubePoTokenMode> = mutableMode.asStateFlow()
    val token: StateFlow<String> = mutableToken.asStateFlow()

    fun set(mode: YouTubePoTokenMode, token: String = mutableToken.value) {
        val cleanToken = token.trim()
        MODE_KEY.updateInt(mode.value)
        mutableMode.value = mode

        // Keep the token only when Manual mode is selected. Switching away clears the
        // persisted value so an old token cannot be accidentally reused months later.
        val storedToken = if (mode == YouTubePoTokenMode.MANUAL_GVS) cleanToken else ""
        TOKEN_KEY.updateString(storedToken)
        mutableToken.value = storedToken
    }

    fun currentMode(): YouTubePoTokenMode = YouTubePoTokenMode.fromValue(MODE_KEY.getInt(0))

    fun currentToken(): String = TOKEN_KEY.getString().trim()
}

internal fun isYouTubeUrl(url: String): Boolean {
    val host =
        runCatching { Uri.parse(url.trim()).host.orEmpty().lowercase(Locale.US) }
            .getOrDefault("")
    return host == "youtu.be" ||
        host == "youtube.com" ||
        host.endsWith(".youtube.com") ||
        host == "youtube-nocookie.com" ||
        host.endsWith(".youtube-nocookie.com")
}

private fun String.extractorArgKey(): String =
    substringBefore('=').trim().lowercase(Locale.US).replace('-', '_')

private fun mergeSkipTranslatedSubs(parts: MutableList<String>) {
    val index = parts.indexOfFirst { it.extractorArgKey() == "skip" }
    if (index < 0) {
        parts += "skip=translated_subs"
        return
    }

    val current = parts[index].substringAfter('=', "")
        .split(',')
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .toMutableList()
    if (current.none { it.equals("translated_subs", ignoreCase = true) }) {
        current += "translated_subs"
    }
    parts[index] = "skip=${current.joinToString(",")}" 
}

private fun buildKirinYouTubeArgs(
    existingYoutubeBody: String,
    skipTranslatedSubs: Boolean,
    allowPoToken: Boolean,
): String {
    val parts =
        existingYoutubeBody
            .split(';')
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .toMutableList()

    if (skipTranslatedSubs) mergeSkipTranslatedSubs(parts)

    // Metadata probes must stay independent from PO Token availability. A provider/token can
    // expire, be missing from the Android runtime, or be bound to a different video/session.
    // KirinDL therefore opts into PO handling only for the actual media transfer.
    if (allowPoToken) {
        val existingKeys = parts.map { it.extractorArgKey() }.toMutableSet()
        when (YouTubePoTokenPreference.currentMode()) {
            YouTubePoTokenMode.OFF -> Unit
            YouTubePoTokenMode.PROVIDER_ASSIST -> {
                if ("player_client" !in existingKeys) {
                    parts += "player_client=default,mweb"
                    existingKeys += "player_client"
                }
            }
            YouTubePoTokenMode.MANUAL_GVS -> {
                val token = YouTubePoTokenPreference.currentToken()
                // Never force mweb when the saved manual token is empty/corrupt. This keeps the
                // normal yt-dlp path working instead of selecting a client which expects a token.
                if (token.isNotBlank()) {
                    if ("player_client" !in existingKeys) {
                        parts += "player_client=default,mweb"
                        existingKeys += "player_client"
                    }
                    if ("po_token" !in existingKeys) {
                        parts += "po_token=mweb.gvs+$token"
                        existingKeys += "po_token"
                    }
                }
            }
        }
    }

    return parts.joinToString(";")
}

/**
 * Adds KirinDL's YouTube-specific extractor arguments without clobbering a user's custom args.
 * A custom `youtube:` block wins for keys it already defines; KirinDL only fills missing keys.
 */
internal fun YoutubeDLRequest.applyKirinYouTubeExtractorArgs(
    url: String,
    customExtractorArgs: String = "",
    skipTranslatedSubs: Boolean = false,
    allowPoToken: Boolean = true,
): YoutubeDLRequest {
    val custom = customExtractorArgs.trim()
    if (!isYouTubeUrl(url)) {
        if (custom.isNotBlank()) addOption("--extractor-args", custom)
        return this
    }

    val isCustomYouTube = custom.startsWith("youtube:", ignoreCase = true)
    if (custom.isNotBlank() && !isCustomYouTube) {
        addOption("--extractor-args", custom)
    }

    val existingBody = if (isCustomYouTube) custom.substringAfter(':') else ""
    val youtubeBody =
        buildKirinYouTubeArgs(
            existingYoutubeBody = existingBody,
            skipTranslatedSubs = skipTranslatedSubs,
            allowPoToken = allowPoToken,
        )
    if (youtubeBody.isNotBlank()) {
        addOption("--extractor-args", "youtube:$youtubeBody")
    } else if (isCustomYouTube) {
        addOption("--extractor-args", custom)
    }

    return this
}
