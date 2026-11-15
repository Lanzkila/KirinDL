package com.junkfood.seal.ui.page

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.junkfood.seal.util.DEFAULT_INTERVAL
import com.junkfood.seal.util.PreferenceUtil
import com.junkfood.seal.util.UpdateUtil

private const val UPDATE_POPUP_PREFERENCES = "kirin_update_popup"
private const val KEY_DISMISSED_RELEASE = "dismissed_release"

/*
 * Kode popup startup ini mengikuti aliran dialog update YTDLnis oleh Denis Çerri.
 * Sumber asal: UiUtil.showNewAppUpdateDialog()
 * https://github.com/deniscerri/ytdlnis/blob/00f3b1b95378adb169314b61f13c1e2a641ef9ee/app/src/main/java/com/deniscerri/ytdl/util/UiUtil.kt#L2901-L2997
 * KirinDL menggunakan dialog Compose dan fungsi muat turun/installer sendiri.
 */

/**
 * Checks for updates on startup and opens the release dialog directly, following the
 * single-dialog flow used by YTDLnis. About -> App Update remains a separate manual entry point.
 *
 * Cancel closes the current prompt without ignoring the release. Ignore remembers that release.
 * A forced Stable migration cannot be permanently ignored while a Pre-release build is installed.
 */
@Composable
fun StartupUpdatePopup() {
    val context = LocalContext.current
    val popupPreferences = remember(context) {
        context.getSharedPreferences(UPDATE_POPUP_PREFERENCES, Context.MODE_PRIVATE)
    }

    var releaseToShow by remember { mutableStateOf<UpdateUtil.Release?>(null) }
    val sharedAvailableRelease by UpdateUtil.availableAppUpdate.collectAsState()
    var showUpdateDialog by remember { mutableStateOf(false) }

    LaunchedEffect(context) {
        // The existing App Update setting controls automatic checks.
        if (!PreferenceUtil.isAutoUpdateEnabled()) return@LaunchedEffect

        val cachedRelease = UpdateUtil.restoreCachedAvailableUpdate(context)
        val candidate =
            if (
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
        val ignoredRelease = popupPreferences.getString(KEY_DISMISSED_RELEASE, null)
        val forcedStableMigration = UpdateUtil.isForcedStableMigration(context, candidate)
        if (forcedStableMigration || releaseKey != ignoredRelease) {
            releaseToShow = candidate
            showUpdateDialog = true
        }
    }

    // Observe a check completed by AppUpdater so its result is not lost to the startup cooldown.
    LaunchedEffect(sharedAvailableRelease) {
        val candidate = sharedAvailableRelease ?: return@LaunchedEffect
        if (!PreferenceUtil.isAutoUpdateEnabled()) return@LaunchedEffect

        val key = releaseKey(candidate) ?: return@LaunchedEffect
        val ignoredRelease = popupPreferences.getString(KEY_DISMISSED_RELEASE, null)
        val forcedStableMigration = UpdateUtil.isForcedStableMigration(context, candidate)
        if (forcedStableMigration || key != ignoredRelease) {
            releaseToShow = candidate
            showUpdateDialog = true
        }
    }

    val release = releaseToShow
    val ignoreRelease: () -> Unit = {
        showUpdateDialog = false
        release?.let { target ->
            // Keep forced Stable migration prompts active until the installed tag is promoted.
            if (!UpdateUtil.isForcedStableMigration(context, target)) {
                releaseKey(target)?.let { key ->
                    popupPreferences.edit()
                        .putString(KEY_DISMISSED_RELEASE, key)
                        .apply()
                }
            }
        }
    }

    if (showUpdateDialog && release != null) {
        UpdateDialog(
            onDismissRequest = { showUpdateDialog = false },
            release = release,
            isUpdateAvailable = true,
            useInAppUpdate = true,
            onIgnore = ignoreRelease,
            onUpdateLaunched = {
                // Hide the prompt while Android's installer is in front.
                // If installation is cancelled, the release can be offered again next launch.
                showUpdateDialog = false
            },
        )
    }
}

private fun releaseKey(release: UpdateUtil.Release): String? =
    release.tagName?.takeIf { it.isNotBlank() }
        ?: release.name?.takeIf { it.isNotBlank() }
