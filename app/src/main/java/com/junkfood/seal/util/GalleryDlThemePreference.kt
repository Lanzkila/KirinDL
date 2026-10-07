package com.junkfood.seal.util

import com.junkfood.seal.App
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class GalleryDlThemeStyle(
    val key: String,
    val title: String,
    val description: String,
) {
    APP_DEFAULT(
        key = "app_default",
        title = "Follow app",
        description = "Use the active KirinDL theme and accent.",
    ),
    KIRIN_CYAN(
        key = "kirin_cyan",
        title = "Kirin Cyan",
        description = "Kirin cyan accent while keeping the app background and text colors.",
    ),
    OCEAN(
        key = "ocean",
        title = "Ocean Blue",
        description = "Cool blue accent with the current app light/dark surfaces.",
    ),
    EMERALD(
        key = "emerald",
        title = "Emerald",
        description = "Green accent with the current app light/dark surfaces.",
    ),
    VIOLET(
        key = "violet",
        title = "Violet",
        description = "Violet accent with the current app light/dark surfaces.",
    ),
    SAKURA(
        key = "sakura",
        title = "Sakura Pink",
        description = "Soft pink accent while keeping the active app surfaces.",
    ),
    CRIMSON(
        key = "crimson",
        title = "Crimson",
        description = "Deep red accent with the current app light/dark surfaces.",
    ),
    AMBER(
        key = "amber",
        title = "Amber Gold",
        description = "Warm gold accent with the current app light/dark surfaces.",
    ),
    TEAL(
        key = "teal",
        title = "Teal",
        description = "Balanced teal accent with the current app light/dark surfaces.",
    ),
    INDIGO(
        key = "indigo",
        title = "Indigo",
        description = "Deep indigo accent with the current app light/dark surfaces.",
    ),
    LIME(
        key = "lime",
        title = "Lime",
        description = "Bright lime accent with the current app light/dark surfaces.",
    ),
    CUSTOM(
        key = "custom",
        title = "Custom",
        description = "Use your own Gallery DL accent color.",
    );

    companion object {
        fun fromKey(value: String?): GalleryDlThemeStyle =
            entries.firstOrNull { it.key == value } ?: APP_DEFAULT
    }
}

/**
 * Gallery Hub appearance preference.
 *
 * Only the Gallery accent can be customized. Background, surface and text colors always come from
 * KirinDL's active MaterialTheme, which keeps Gallery DL synchronized with the app's
 * Light/Dark/Dynamic/Gradient appearance and prevents light-on-light input fields.
 */
object GalleryDlThemePreference {
    private const val PREFS_NAME = "kirin_gallery_ui"
    private const val KEY_THEME = "gallery_theme"
    private const val KEY_CUSTOM_ACCENT = "gallery_custom_accent"
    private const val DEFAULT_CUSTOM_ACCENT = 0xFF18BFEA.toInt()

    private val preferences by lazy {
        App.context.getSharedPreferences(PREFS_NAME, 0)
    }

    private val mutableStyle =
        MutableStateFlow(
            GalleryDlThemeStyle.fromKey(
                preferences.getString(KEY_THEME, GalleryDlThemeStyle.APP_DEFAULT.key)
            )
        )

    val style = mutableStyle.asStateFlow()

    private val mutableCustomAccent =
        MutableStateFlow(preferences.getInt(KEY_CUSTOM_ACCENT, DEFAULT_CUSTOM_ACCENT))

    val customAccent = mutableCustomAccent.asStateFlow()

    fun setStyle(value: GalleryDlThemeStyle) {
        preferences.edit().putString(KEY_THEME, value.key).apply()
        mutableStyle.value = value
    }

    fun setCustomAccent(value: Int) {
        preferences.edit().putInt(KEY_CUSTOM_ACCENT, value).apply()
        mutableCustomAccent.value = value
        setStyle(GalleryDlThemeStyle.CUSTOM)
    }
}
