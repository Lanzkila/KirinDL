package com.junkfood.seal.util

import com.junkfood.seal.util.PreferenceUtil.getBoolean
import com.junkfood.seal.util.PreferenceUtil.updateBoolean
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Controls whether legacy / SealPlus-styled Material surfaces inherit KirinDL's optional
 * body and accent presets. The bridge is deliberately OFF by default so the original
 * SealPlus appearance remains unchanged until the user explicitly enables it.
 */
object SealPlusThemePreference {
    private const val KEY = "kirin_sealplus_follow_theme"

    private val mutableFollowTheme = MutableStateFlow(KEY.getBoolean(false))
    val followTheme: StateFlow<Boolean> = mutableFollowTheme.asStateFlow()

    fun setFollowTheme(enabled: Boolean) {
        KEY.updateBoolean(enabled)
        mutableFollowTheme.value = enabled
    }

    fun toggle() = setFollowTheme(!mutableFollowTheme.value)
}
