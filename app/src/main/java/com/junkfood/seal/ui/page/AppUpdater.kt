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


private const val UPDATE_POPUP_PREFS = "kirindl_update_popup"
private const val LAST_AUTO_POPUP_RELEASE_STABLE = "last_auto_popup_release_stable"
private const val LAST_AUTO_POPUP_RELEASE_PRERELEASE = "last_auto_popup_release_prerelease"

private fun UpdateUtil.Release.autoPopupKey(): String =
    listOfNotNull(tagName, name, publishedAt, htmlUrl)
        .firstOrNull { it.isNotBlank() }
        .orEmpty()

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
                UpdateUtil.checkForUpdate()?.let { candidate ->
                    val popupKey = candidate.autoPopupKey()
                    val popupPrefs =
                        context.getSharedPreferences(UPDATE_POPUP_PREFS, android.content.Context.MODE_PRIVATE)
                    val storageKey =
                        if (candidate.preRelease == true) LAST_AUTO_POPUP_RELEASE_PRERELEASE
                        else LAST_AUTO_POPUP_RELEASE_STABLE
                    val alreadyShown =
                        popupKey.isNotBlank() &&
                            popupPrefs.getString(storageKey, null) == popupKey

                    if (!alreadyShown) {
                        release = candidate
                        showUpdateDialog = true

                        // Persist as soon as the automatic popup is presented. Re-entering Home,
                        // reopening the app, or using the same pre-release tag will not spam the
                        // same dialog again. A different release/tag gets one fresh popup.
                        if (popupKey.isNotBlank()) {
                            popupPrefs.edit().putString(storageKey, popupKey).apply()
                        }
                    }
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
