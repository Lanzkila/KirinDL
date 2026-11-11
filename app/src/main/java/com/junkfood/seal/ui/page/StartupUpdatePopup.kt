package com.junkfood.seal.ui.page

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.junkfood.seal.util.DEFAULT_INTERVAL
import com.junkfood.seal.util.PreferenceUtil
import com.junkfood.seal.util.UpdateUtil
import com.junkfood.seal.util.makeToast

private const val UPDATE_POPUP_PREFERENCES = "kirin_update_popup"
private const val KEY_DISMISSED_RELEASE = "dismissed_release"

/**
 * Checks for a KirinDL release when the main app opens and presents its update dialog once per
 * release. The About > App Update page remains an independent manual entry point.
 *
 * Dismissal is remembered per release tag, so a dismissed update does not interrupt every launch.
 * The Home update alert remains available as another way to open the release details.
 */
@Composable
fun StartupUpdatePopup() {
    val context = LocalContext.current
    val popupPreferences = remember(context) {
        context.getSharedPreferences(UPDATE_POPUP_PREFERENCES, Context.MODE_PRIVATE)
    }

    var releaseToShow by remember { mutableStateOf<UpdateUtil.Release?>(null) }
    var showPopup by remember { mutableStateOf(false) }

    LaunchedEffect(context) {
        // The existing App Update setting controls automatic checks. Notifications are separate.
        if (!PreferenceUtil.isAutoUpdateEnabled()) return@LaunchedEffect

        val cachedRelease = UpdateUtil.restoreCachedAvailableUpdate(context)
        val candidate = if (
            UpdateUtil.shouldRunAutomaticUpdateCheck(
                context = context,
                intervalMs = DEFAULT_INTERVAL,
            )
        ) {
            // Keep a previously resolved update available when the network check fails.
            UpdateUtil.checkForUpdateResult(context).getOrElse { cachedRelease }
        } else {
            cachedRelease
        }

        val releaseKey = candidate?.let(::releaseKey) ?: return@LaunchedEffect
        val dismissedRelease = popupPreferences.getString(KEY_DISMISSED_RELEASE, null)
        if (releaseKey != dismissedRelease) {
            releaseToShow = candidate
            showPopup = true
        }
    }

    val release = releaseToShow
    if (showPopup && release != null) {
        UpdateDialog(
            onDismissRequest = {
                showPopup = false
                releaseKey(release)?.let { key ->
                    popupPreferences.edit()
                        .putString(KEY_DISMISSED_RELEASE, key)
                        .apply()
                }
            },
            release = release,
            isUpdateAvailable = true,
            onBackgroundUpdate = if (UpdateUtil.hasBackgroundAppUpdate(release)) {
                {
                    UpdateUtil.enqueueBackgroundAppUpdate(context, release)
                        .onSuccess {
                            context.makeToast(
                                "KirinDL update is downloading in Android Download Manager"
                            )
                        }
                        .onFailure { error ->
                            error.printStackTrace()
                            context.makeToast("Could not start the app update download")
                        }
                }
            } else {
                null
            },
        )
    }
}

private fun releaseKey(release: UpdateUtil.Release): String? =
    release.tagName?.takeIf { it.isNotBlank() }
        ?: release.name?.takeIf { it.isNotBlank() }
