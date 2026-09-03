package com.junkfood.seal.util

import com.junkfood.seal.util.PreferenceUtil.getBoolean
import com.junkfood.seal.util.PreferenceUtil.getInt
import com.junkfood.seal.util.PreferenceUtil.getString
import com.junkfood.seal.util.PreferenceUtil.updateBoolean
import com.junkfood.seal.util.PreferenceUtil.updateInt
import com.junkfood.seal.util.PreferenceUtil.updateString

/**
 * Phase 12 UI profile macros. They only write existing KirinDL preferences and intentionally do
 * not change downloader lifecycle, queue execution, Aria2 routing, or gallery-dl execution.
 */
object SmartDownloadProfiles {
    const val MANUAL = 0
    const val VIDEO_BEST = 1
    const val VIDEO_BALANCED = 2
    const val VIDEO_SMALL = 3
    const val AUDIO_HQ = 4
    const val AUDIO_DATA_SAVER = 5
    const val BILIBILI_FAST = 6
    const val BILIBILI_STABLE = 7
    const val CUSTOM_1 = 101
    const val CUSTOM_2 = 102
    const val CUSTOM_3 = 103

    data class Definition(val id: Int, val title: String, val description: String)

    val builtIns =
        listOf(
            Definition(MANUAL, "Manual / current", "Keep your current individual format settings."),
            Definition(VIDEO_BEST, "Video Best", "Best quality sorting with automatic codec/container."),
            Definition(VIDEO_BALANCED, "Video Balanced", "1080p • H.264 • MP4 for quality and compatibility."),
            Definition(VIDEO_SMALL, "Video Small", "720p • H.264 • MP4 for smaller downloads."),
            Definition(AUDIO_HQ, "Audio HQ", "M4A/AAC • 320 Kbps • embedded cover."),
            Definition(AUDIO_DATA_SAVER, "Audio Data Saver", "Opus • 96 Kbps • no cover file."),
            Definition(BILIBILI_FAST, "Bilibili Fast", "1080p compatibility profile + Bilibili Fast mode."),
            Definition(BILIBILI_STABLE, "Bilibili Stable", "720p compatibility profile + Balanced Bilibili mode."),
        )

    fun label(id: Int = SMART_DOWNLOAD_PROFILE.getInt()): String =
        builtIns.firstOrNull { it.id == id }?.title
            ?: when (id) {
                CUSTOM_1 -> "Custom Slot 1"
                CUSTOM_2 -> "Custom Slot 2"
                CUSTOM_3 -> "Custom Slot 3"
                else -> "Manual / current"
            }

    fun customSlotSaved(slot: Int): Boolean = customKey(slot)?.getString()?.isNotBlank() == true

    fun apply(id: Int): Boolean {
        when (id) {
            MANUAL -> Unit
            VIDEO_BEST -> {
                EXTRACT_AUDIO.updateBoolean(false)
                FORMAT_SORTING.updateBoolean(true)
                SORTING_FIELDS.updateString("res,fps,vcodec,channels,acodec,br")
                VIDEO_QUALITY.updateInt(NOT_SPECIFIED)
                VIDEO_FORMAT.updateInt(FORMAT_QUALITY)
                VIDEO_CODEC.updateInt(VIDEO_CODEC_AUTO)
                VIDEO_CONTAINER.updateInt(VIDEO_CONTAINER_AUTO)
            }
            VIDEO_BALANCED -> applyVideoCompatibility(3)
            VIDEO_SMALL -> applyVideoCompatibility(4)
            AUDIO_HQ -> {
                EXTRACT_AUDIO.updateBoolean(true)
                FORMAT_SORTING.updateBoolean(false)
                AUDIO_FORMAT.updateInt(M4A)
                AUDIO_QUALITY.updateInt(AUDIO_320)
                AUDIO_CODEC.updateInt(AUDIO_CODEC_AAC)
                AUDIO_COVER_MODE.updateInt(AUDIO_COVER_EMBED)
                AUDIO_COVER_FORMAT.updateInt(AUDIO_COVER_FORMAT_JPG)
                AUDIO_CONVERT.updateBoolean(false)
            }
            AUDIO_DATA_SAVER -> {
                EXTRACT_AUDIO.updateBoolean(true)
                FORMAT_SORTING.updateBoolean(false)
                AUDIO_FORMAT.updateInt(OPUS)
                AUDIO_QUALITY.updateInt(AUDIO_96)
                AUDIO_CODEC.updateInt(AUDIO_CODEC_OPUS)
                AUDIO_COVER_MODE.updateInt(AUDIO_COVER_NONE)
                AUDIO_COVER_FORMAT.updateInt(AUDIO_COVER_FORMAT_AUTO)
                AUDIO_CONVERT.updateBoolean(false)
            }
            BILIBILI_FAST -> {
                applyVideoCompatibility(3)
                BILIBILI_SPEED_MODE.updateInt(BILIBILI_SPEED_FAST)
            }
            BILIBILI_STABLE -> {
                applyVideoCompatibility(4)
                BILIBILI_SPEED_MODE.updateInt(BILIBILI_SPEED_BALANCED)
            }
            CUSTOM_1, CUSTOM_2, CUSTOM_3 -> {
                val encoded = customKey(id)?.getString().orEmpty()
                if (encoded.isBlank() || !applyEncoded(encoded)) return false
            }
            else -> return false
        }
        SMART_DOWNLOAD_PROFILE.updateInt(id)
        return true
    }

    fun saveCurrentToCustom(id: Int): Boolean {
        val key = customKey(id) ?: return false
        key.updateString(captureEncoded())
        SMART_DOWNLOAD_PROFILE.updateInt(id)
        return true
    }

    private fun applyVideoCompatibility(resolution: Int) {
        EXTRACT_AUDIO.updateBoolean(false)
        FORMAT_SORTING.updateBoolean(false)
        VIDEO_FORMAT.updateInt(FORMAT_QUALITY)
        VIDEO_QUALITY.updateInt(resolution)
        VIDEO_CODEC.updateInt(VIDEO_CODEC_H264)
        VIDEO_CONTAINER.updateInt(VIDEO_CONTAINER_MP4)
        AUDIO_CONVERT.updateBoolean(false)
    }

    private fun customKey(id: Int): String? =
        when (id) {
            CUSTOM_1, 1 -> SMART_PROFILE_SLOT_1
            CUSTOM_2, 2 -> SMART_PROFILE_SLOT_2
            CUSTOM_3, 3 -> SMART_PROFILE_SLOT_3
            else -> null
        }

    private fun captureEncoded(): String =
        listOf(
            EXTRACT_AUDIO.getBoolean().toString(),
            AUDIO_FORMAT.getInt().toString(),
            AUDIO_QUALITY.getInt().toString(),
            AUDIO_CODEC.getInt().toString(),
            AUDIO_COVER_MODE.getInt().toString(),
            AUDIO_COVER_FORMAT.getInt().toString(),
            VIDEO_FORMAT.getInt().toString(),
            VIDEO_QUALITY.getInt().toString(),
            VIDEO_CODEC.getInt().toString(),
            VIDEO_CONTAINER.getInt().toString(),
            FORMAT_SORTING.getBoolean().toString(),
            SORTING_FIELDS.getString(),
            BILIBILI_SPEED_MODE.getInt().toString(),
            BILIBILI_CUSTOM_FRAGMENTS.getInt().toString(),
            AUDIO_CONVERT.getBoolean().toString(),
            AUDIO_CONVERSION_FORMAT.getInt().toString(),
        ).joinToString("|")

    private fun applyEncoded(encoded: String): Boolean {
        val values = encoded.split('|')
        if (values.size < 16) return false
        return runCatching {
            EXTRACT_AUDIO.updateBoolean(values[0].toBooleanStrict())
            AUDIO_FORMAT.updateInt(values[1].toInt())
            AUDIO_QUALITY.updateInt(values[2].toInt())
            AUDIO_CODEC.updateInt(values[3].toInt())
            AUDIO_COVER_MODE.updateInt(values[4].toInt())
            AUDIO_COVER_FORMAT.updateInt(values[5].toInt())
            VIDEO_FORMAT.updateInt(values[6].toInt())
            VIDEO_QUALITY.updateInt(values[7].toInt())
            VIDEO_CODEC.updateInt(values[8].toInt())
            VIDEO_CONTAINER.updateInt(values[9].toInt())
            FORMAT_SORTING.updateBoolean(values[10].toBooleanStrict())
            SORTING_FIELDS.updateString(values[11])
            BILIBILI_SPEED_MODE.updateInt(values[12].toInt())
            BILIBILI_CUSTOM_FRAGMENTS.updateInt(values[13].toInt())
            AUDIO_CONVERT.updateBoolean(values[14].toBooleanStrict())
            AUDIO_CONVERSION_FORMAT.updateInt(values[15].toInt())
        }.isSuccess
    }
}
