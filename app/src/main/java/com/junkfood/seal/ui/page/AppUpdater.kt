package com.junkfood.seal.ui.page

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.junkfood.seal.util.PreferenceUtil
import com.junkfood.seal.util.UpdateUtil
import com.junkfood.seal.util.makeToast
import java.util.concurrent.TimeUnit

// Komikku-style automatic update trigger:
// - check once when the app UI starts
// - throttle automatic checks so every app launch does not hit GitHub
// - About/manual checks are intentionally NOT throttled
private val AUTO_UPDATE_CHECK_INTERVAL_MS = TimeUnit.DAYS.toMillis(1)

/**
 * KirinDL app update trigger.
 *
 * This follows Komikku's update flow: a lightweight check runs when the app enters its main UI,
 * but repeated automatic checks are rate-limited. If the user dismisses or misses the popup, the
 * About page can always perform a fresh manual check and reopen the same update dialog.
 *
 * KirinDL never silently confirms an install. The official release APK is downloaded by Android
 * DownloadManager and then handed to Android's package installer; the user still confirms the
 * final in-place update/overwrite.
 */
@Composable
fun AppUpdater() {
    val context = LocalContext.current
    var showUpdateDialog by rememberSaveable { mutableStateOf(false) }
    var release by remember { mutableStateOf(UpdateUtil.Release()) }

    LaunchedEffect(Unit) {
        // If Android has just replaced KirinDL with a newer APK, clear the previous build's
        // updater cooldown before deciding whether this startup should check GitHub.
        UpdateUtil.syncInstalledVersionState(context)

        if (
            !PreferenceUtil.isNetworkAvailableForDownload() ||
                !PreferenceUtil.isAutoUpdateEnabled()
        ) {
            return@LaunchedEffect
        }

        if (
            !UpdateUtil.shouldRunAutomaticUpdateCheck(
                context = context,
                intervalMs = AUTO_UPDATE_CHECK_INTERVAL_MS,
            )
        ) {
            return@LaunchedEffect
        }

        UpdateUtil.checkForUpdateResult(context)
            .onSuccess { candidate ->
                if (candidate != null) {
                    release = candidate
                    showUpdateDialog = true
                }
            }
            .onFailure { error ->
                // Failed network/API checks are not recorded as successful checks, so the next
                // eligible app start can retry without waiting through the normal cooldown.
                error.printStackTrace()
            }
    }

    if (showUpdateDialog) {
        UpdateDialog(
            onDismissRequest = { showUpdateDialog = false },
            release = release,
            isUpdateAvailable = true,
            onBackgroundUpdate =
                if (UpdateUtil.hasBackgroundAppUpdate(release)) {
                    {
                        UpdateUtil.enqueueBackgroundAppUpdate(context, release)
                            .onSuccess {
                                context.makeToast(
                                    "KirinDL update is downloading in the background"
                                )
                            }
                            .onFailure { error ->
                                error.printStackTrace()
                                context.makeToast("Could not start the background update")
                            }
                    }
                } else {
                    null
                },
        )
    }
}
