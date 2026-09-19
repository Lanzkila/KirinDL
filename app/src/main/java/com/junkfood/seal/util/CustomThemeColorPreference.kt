package com.junkfood.seal.util

import androidx.compose.ui.graphics.Color
import com.tencent.mmkv.MMKV
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** User-defined KirinDL colors with independent Light and Dark values. */
object CustomThemeColorPreference {
    data class Colors(
        val lightBody: Int = 0xFFF8F9FC.toInt(),
        val lightAccent: Int = 0xFF6750A4.toInt(),
        val darkBody: Int = 0xFF111318.toInt(),
        val darkAccent: Int = 0xFFD0BCFF.toInt(),
    )

    private const val KEY_LIGHT_BODY = "kirin_custom_light_body"
    private const val KEY_LIGHT_ACCENT = "kirin_custom_light_accent"
    private const val KEY_DARK_BODY = "kirin_custom_dark_body"
    private const val KEY_DARK_ACCENT = "kirin_custom_dark_accent"

    private val kv by lazy { MMKV.defaultMMKV() }

    private fun read(): Colors =
        Colors(
            lightBody = kv.decodeInt(KEY_LIGHT_BODY, 0xFFF8F9FC.toInt()),
            lightAccent = kv.decodeInt(KEY_LIGHT_ACCENT, 0xFF6750A4.toInt()),
            darkBody = kv.decodeInt(KEY_DARK_BODY, 0xFF111318.toInt()),
            darkAccent = kv.decodeInt(KEY_DARK_ACCENT, 0xFFD0BCFF.toInt()),
        )

    private val mutableColors = MutableStateFlow(read())
    val colors = mutableColors.asStateFlow()

    fun setColors(lightBody: Int, lightAccent: Int, darkBody: Int, darkAccent: Int) {
        kv.encode(KEY_LIGHT_BODY, lightBody)
        kv.encode(KEY_LIGHT_ACCENT, lightAccent)
        kv.encode(KEY_DARK_BODY, darkBody)
        kv.encode(KEY_DARK_ACCENT, darkAccent)
        mutableColors.value = Colors(lightBody, lightAccent, darkBody, darkAccent)
    }

    fun reset() {
        setColors(
            lightBody = 0xFFF8F9FC.toInt(),
            lightAccent = 0xFF6750A4.toInt(),
            darkBody = 0xFF111318.toInt(),
            darkAccent = 0xFFD0BCFF.toInt(),
        )
    }

    fun parseHex(value: String): Int? {
        val clean = value.trim().removePrefix("#").removePrefix("0x").removePrefix("0X")
        val argb =
            when (clean.length) {
                6 -> "FF$clean"
                8 -> clean
                else -> return null
            }
        return argb.toLongOrNull(16)?.toInt()
    }

    fun toHex(value: Int): String = "#%08X".format(value)

    fun lightBodyColor(colors: Colors = mutableColors.value) = Color(colors.lightBody)
    fun lightAccentColor(colors: Colors = mutableColors.value) = Color(colors.lightAccent)
    fun darkBodyColor(colors: Colors = mutableColors.value) = Color(colors.darkBody)
    fun darkAccentColor(colors: Colors = mutableColors.value) = Color(colors.darkAccent)
}
