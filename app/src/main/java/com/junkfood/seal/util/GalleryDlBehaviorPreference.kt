package com.junkfood.seal.util

import android.net.Uri
import com.junkfood.seal.App
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Behavior switches for Kirin Gallery DL that should survive app restarts. */
object GalleryDlBehaviorPreference {
    private const val PREFS_NAME = "kirin_gallery_behavior"
    private const val KEY_CONFIRM_BEFORE_DOWNLOAD = "confirm_before_download"
    private const val KEY_PENDING_HOME_URL = "pending_home_url"
    private const val KEY_EXPORT_FILTER = "export_filter"
    private const val KEY_SITE_EXPORT_PREFIX = "site_export_"
    private const val KEY_LAST_TAB = "last_tab"

    const val EXPORT_ALL = 0
    const val EXPORT_IMAGES = 1
    const val EXPORT_VIDEOS = 2
    const val EXPORT_MEDIA = 3

    private val preferences by lazy { App.context.getSharedPreferences(PREFS_NAME, 0) }

    private val mutableConfirmBeforeDownload =
        MutableStateFlow(preferences.getBoolean(KEY_CONFIRM_BEFORE_DOWNLOAD, true))
    private val mutableExportFilter = MutableStateFlow(EXPORT_ALL)

    val confirmBeforeDownload = mutableConfirmBeforeDownload.asStateFlow()

    /**
     * Kept only for old settings/build compatibility. DevPatch21 removes the Gallery export
     * filter from the UI and always exports every file returned by gallery-dl.
     */
    val exportFilter = mutableExportFilter.asStateFlow()

    fun setConfirmBeforeDownload(value: Boolean) {
        preferences.edit().putBoolean(KEY_CONFIRM_BEFORE_DOWNLOAD, value).apply()
        mutableConfirmBeforeDownload.value = value
    }

    fun setExportFilter(value: Int) {
        // Compatibility no-op: Gallery DL now always exports all files.
        preferences.edit().remove(KEY_EXPORT_FILTER).apply()
        mutableExportFilter.value = EXPORT_ALL
    }

    fun exportFilterLabel(value: Int = EXPORT_ALL): String = "All files"

    fun siteExportFilter(url: String): Int? = null

    /** Export filtering was removed; old stored filters cannot silently affect new downloads. */
    fun effectiveExportFilter(url: String): Int = EXPORT_ALL

    fun rememberSiteExportFilter(url: String, value: Int) {
        sitePreferenceKey(url)?.let { key -> preferences.edit().remove(key).apply() }
    }

    fun clearSiteExportFilter(url: String) {
        sitePreferenceKey(url)?.let { key -> preferences.edit().remove(key).apply() }
    }

    fun siteLabel(url: String): String? =
        runCatching {
                Uri.parse(url.trim()).host.orEmpty().lowercase().removePrefix("www.")
            }
            .getOrNull()
            ?.takeIf { it.isNotBlank() }

    private fun sitePreferenceKey(url: String): String? =
        siteLabel(url)?.let { host -> KEY_SITE_EXPORT_PREFIX + host.replace('.', '_') }

    fun setLastTab(value: Int) {
        preferences.edit().putInt(KEY_LAST_TAB, value.coerceIn(0, 2)).apply()
    }

    fun lastTab(): Int = preferences.getInt(KEY_LAST_TAB, 0).coerceIn(0, 2)

    /** One-shot handoff retained for older Home builds/backups. */
    fun setPendingHomeUrl(value: String) {
        preferences.edit().putString(KEY_PENDING_HOME_URL, value.trim()).apply()
    }

    fun consumePendingHomeUrl(): String? {
        val value = preferences.getString(KEY_PENDING_HOME_URL, null)?.trim().orEmpty()
        preferences.edit().remove(KEY_PENDING_HOME_URL).apply()
        return value.takeIf { it.isNotBlank() }
    }
}
