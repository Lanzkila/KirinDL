package com.junkfood.seal.ui.page

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.junkfood.seal.util.PreferenceUtil
import com.junkfood.seal.util.UpdateUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Lightweight update checker for KirinDL.
 *
 * The app intentionally does not request package-install permission and does not install APKs
 * itself. When an update is available, the user can open the official KirinDL GitHub release in
 * their browser and decide what to do there.
 */
@Composable
fun AppUpdater() {
    var showUpdateDialog by rememberSaveable { mutableStateOf(false) }
    var release by remember { mutableStateOf(UpdateUtil.Release()) }

    LaunchedEffect(Unit) {
        if (
            !PreferenceUtil.isNetworkAvailableForDownload() ||
                !PreferenceUtil.isAutoUpdateEnabled()
        ) {
            return@LaunchedEffect
        }

        withContext(Dispatchers.IO) {
            runCatching {
                    UpdateUtil.checkForUpdate()?.let {
                        release = it
                        showUpdateDialog = true
                    }
                }
                .onFailure { it.printStackTrace() }
        }
    }

    if (showUpdateDialog) {
        UpdateDialog(
            onDismissRequest = { showUpdateDialog = false },
            release = release,
        )
    }
}
