package com.junkfood.seal.util

import com.junkfood.seal.App
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Behavior switches for Kirin Gallery DL that should survive app restarts. */
object GalleryDlBehaviorPreference {
    private const val PREFS_NAME = "kirin_gallery_behavior"
    private const val KEY_CONFIRM_BEFORE_DOWNLOAD = "confirm_before_download"
    private const val KEY_PENDING_HOME_URL = "pending_home_url"

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

    /** One-shot handoff used by the compact Gallery DL URL field on Home. */
    fun setPendingHomeUrl(value: String) {
        preferences.edit().putString(KEY_PENDING_HOME_URL, value.trim()).apply()
    }

    fun consumePendingHomeUrl(): String? {
        val value = preferences.getString(KEY_PENDING_HOME_URL, null)?.trim().orEmpty()
        preferences.edit().remove(KEY_PENDING_HOME_URL).apply()
        return value.takeIf { it.isNotBlank() }
    }
}
