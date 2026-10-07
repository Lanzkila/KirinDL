package com.junkfood.seal.ui.page

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import com.junkfood.seal.util.APP_UPDATE_NOTIFICATIONS
import com.junkfood.seal.util.NotificationUtil
import com.junkfood.seal.util.PreferenceUtil
import com.junkfood.seal.util.PreferenceUtil.getBoolean
import com.junkfood.seal.util.UpdateUtil
import java.util.concurrent.TimeUnit

// Komikku-style automatic update trigger:
// - check once when the app UI starts
// - throttle automatic checks so every app launch does not hit GitHub
// - Home owns the persistent in-app update alert
// - Android notification is optional and de-duplicated per release version
private val AUTO_UPDATE_CHECK_INTERVAL_MS = TimeUnit.DAYS.toMillis(1)

@Composable
fun AppUpdater() {
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        UpdateUtil.syncInstalledVersionState(context)

        // Restore a previously detected update before honoring the network-check cooldown.
        // This keeps the Home alert visible across launches until the newer APK is installed.
        UpdateUtil.restoreCachedAvailableUpdate(context)?.let { cached ->
            if (APP_UPDATE_NOTIFICATIONS.getBoolean()) {
                NotificationUtil.notifyAppUpdateAvailable(cached)
            }
        }

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
                if (candidate != null && APP_UPDATE_NOTIFICATIONS.getBoolean()) {
                    NotificationUtil.notifyAppUpdateAvailable(candidate)
                }
            }
            .onFailure { error ->
                // Failed network/API checks are not recorded as successful checks, so the next
                // eligible app start can retry without waiting through the normal cooldown.
                error.printStackTrace()
            }
    }
}
