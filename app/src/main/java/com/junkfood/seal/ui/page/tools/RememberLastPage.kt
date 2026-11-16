package com.junkfood.seal.ui.page.tools

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

/**
 * The last expanded category in More Tools.
 *
 * Persisting this separately from MoreToolsPage keeps the selected dropdown when a child tool
 * temporarily replaces the More Tools content, and also restores it when the page is reopened.
 */
internal enum class MoreToolsDropdownSection(val storageValue: String) {
    NONE("none"),
    VIDEO("video"),
    GALLERY("gallery");

    companion object {
        fun fromStorage(value: String?): MoreToolsDropdownSection =
            values().firstOrNull { it.storageValue == value } ?: NONE
    }
}

internal class RememberLastPage(context: Context) {
    private val preferences =
        context.applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    var expandedSection by mutableStateOf(
        MoreToolsDropdownSection.fromStorage(
            preferences.getString(KEY_EXPANDED_SECTION, null)
        )
    )
        private set

    /** Expanding one category closes the other; tapping the open category collapses it. */
    fun toggle(section: MoreToolsDropdownSection) {
        expandedSection =
            if (expandedSection == section) {
                MoreToolsDropdownSection.NONE
            } else {
                section
            }

        preferences.edit()
            .putString(KEY_EXPANDED_SECTION, expandedSection.storageValue)
            .apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "more_tools_last_page"
        const val KEY_EXPANDED_SECTION = "expanded_section"
    }
}

@Composable
internal fun rememberLastPage(): RememberLastPage {
    val context = LocalContext.current.applicationContext
    return remember(context) { RememberLastPage(context) }
}
