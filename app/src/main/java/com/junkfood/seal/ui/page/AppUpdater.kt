package com.junkfood.seal.ui.page

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.junkfood.seal.util.PreferenceUtil
import com.junkfood.seal.util.UpdateUtil
import com.junkfood.seal.util.makeToast

/**
 * Lightweight update checker for KirinDL.
 *
 * The app intentionally does not request package-install permission and does not install APKs
 * itself. The automatic update popup can hand the official release APK to Android DownloadManager
 * for a background download; installation remains a normal user-controlled Android action.
 */
@Composable
fun AppUpdater() {
    val context = LocalContext.current
    var showUpdateDialog by rememberSaveable { mutableStateOf(false) }
    var release by remember { mutableStateOf(UpdateUtil.Release()) }
    var backgroundUpdateBusy by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (
            !PreferenceUtil.isNetworkAvailableForDownload() ||
                !PreferenceUtil.isAutoUpdateEnabled()
        ) {
            return@LaunchedEffect
        }

        runCatching {
                UpdateUtil.checkForUpdate()?.let {
                    release = it
                    showUpdateDialog = true
                }
            }
            .onFailure { it.printStackTrace() }
    }

    if (showUpdateDialog) {
        UpdateDialog(
            onDismissRequest = { showUpdateDialog = false },
            release = release,
            backgroundUpdateBusy = backgroundUpdateBusy,
            onBackgroundUpdate =
                if (UpdateUtil.hasBackgroundAppUpdate(release)) {
                    {
                        if (!backgroundUpdateBusy) {
                            backgroundUpdateBusy = true
                            UpdateUtil.enqueueBackgroundAppUpdate(context, release)
                                .onSuccess {
                                    backgroundUpdateBusy = false
                                    showUpdateDialog = false
                                    context.makeToast(
                                        "KirinDL update started — check your notification"
                                    )
                                }
                                .onFailure { error ->
                                    backgroundUpdateBusy = false
                                    error.printStackTrace()
                                    context.makeToast("Could not start the background update")
                                }
                        }
                    }
                } else {
                    null
                },
        )
    }
}
