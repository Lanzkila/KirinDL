package com.junkfood.seal.ui.page

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.junkfood.seal.util.APP_UPDATE_CHECK_TIME
import com.junkfood.seal.util.PreferenceUtil
import com.junkfood.seal.util.PreferenceUtil.getLong
import com.junkfood.seal.util.PreferenceUtil.updateLong
import com.junkfood.seal.util.UpdateUtil
import com.junkfood.seal.util.makeToast
import java.util.concurrent.TimeUnit

// Komikku-style automatic update trigger:
// - check once when the app UI starts
// - throttle automatic checks so every app launch does not hit GitHub
// - About/manual checks are intentionally NOT throttled
private val AUTO_UPDATE_CHECK_INTERVAL_MS = TimeUnit.DAYS.toMillis(2)

/**
 * KirinDL app update trigger.
 *
 * This follows Komikku's update flow: a lightweight check runs when the app enters its main UI,
 * but repeated automatic checks are rate-limited. If the user dismisses or misses the popup, the
 * About page can always perform a fresh manual check and reopen the same update dialog.
 *
 * KirinDL does not silently install APKs. The update action only hands the official release APK
 * to Android DownloadManager; installation remains user-controlled.
 */
@Composable
fun AppUpdater() {
    val context = LocalContext.current
    var showUpdateDialog by rememberSaveable { mutableStateOf(false) }
    var release by remember { mutableStateOf(UpdateUtil.Release()) }

    LaunchedEffect(Unit) {
        if (
            !PreferenceUtil.isNetworkAvailableForDownload() ||
                !PreferenceUtil.isAutoUpdateEnabled()
        ) {
            return@LaunchedEffect
        }

        val now = System.currentTimeMillis()
        val lastChecked = APP_UPDATE_CHECK_TIME.getLong()
        val checkedRecently =
            lastChecked > 0L &&
                now >= lastChecked &&
                now - lastChecked < AUTO_UPDATE_CHECK_INTERVAL_MS

        if (checkedRecently) {
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
                // UpdateUtil records the attempt time. A failed network/API request should not
                // silence automatic checks for the whole cooldown window, so clear it again.
                APP_UPDATE_CHECK_TIME.updateLong(0L)
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
