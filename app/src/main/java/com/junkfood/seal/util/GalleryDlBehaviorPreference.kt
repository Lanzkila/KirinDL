package com.junkfood.seal.util

import com.junkfood.seal.App
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Behavior switches for Kirin Gallery DL that should survive app restarts. */
object GalleryDlBehaviorPreference {
    private const val PREFS_NAME = "kirin_gallery_behavior"
    private const val KEY_CONFIRM_BEFORE_DOWNLOAD = "confirm_before_download"

    private val preferences by lazy {
        App.context.getSharedPreferences(PREFS_NAME, 0)
    }

    private val mutableConfirmBeforeDownload =
        MutableStateFlow(preferences.getBoolean(KEY_CONFIRM_BEFORE_DOWNLOAD, true))

    val confirmBeforeDownload = mutableConfirmBeforeDownload.asStateFlow()

    fun setConfirmBeforeDownload(value: Boolean) {
        preferences.edit().putBoolean(KEY_CONFIRM_BEFORE_DOWNLOAD, value).apply()
        mutableConfirmBeforeDownload.value = value
    }
}
