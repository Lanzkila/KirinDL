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

// Komikku-style automatic update trigger.
// Disabled for now so About > App Update is the only active update entry point.
private val AUTO_UPDATE_CHECK_INTERVAL_MS = TimeUnit.DAYS.toMillis(1)

@Composable
fun AppUpdater() {
    // Matikan semakan automatik dan notifikasi update sementara.
    // Kod asal dikekalkan di bawah dan boleh diaktifkan semula melalui suis ini.
    if (ENABLE_AUTOMATIC_APP_UPDATE_CHECK) {
        val context = LocalContext.current

        LaunchedEffect(Unit) {
            UpdateUtil.syncInstalledVersionState(context)

            // Restore a previously detected update before honoring the network-check cooldown.
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
                    // Failed checks are not recorded as successful checks.
                    error.printStackTrace()
                }
        }
    }
}

private const val ENABLE_AUTOMATIC_APP_UPDATE_CHECK = false
