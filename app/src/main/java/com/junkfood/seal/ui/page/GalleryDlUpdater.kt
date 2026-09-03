package com.junkfood.seal.ui.page

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import com.junkfood.seal.util.GALLERY_DL_AUTO_UPDATE
import com.junkfood.seal.util.GALLERY_DL_UPDATE_INTERVAL
import com.junkfood.seal.util.GALLERY_DL_UPDATE_TIME
import com.junkfood.seal.util.GalleryDlEngine
import com.junkfood.seal.util.PreferenceUtil
import com.junkfood.seal.util.PreferenceUtil.getBoolean
import com.junkfood.seal.util.PreferenceUtil.getLong
import com.junkfood.seal.util.PreferenceUtil.updateLong
import kotlinx.coroutines.delay

/**
 * Lightweight Gallery DL updater that mirrors the yt-dlp updater cadence.
 * It only updates an already-installed engine; initial setup remains an explicit manual action.
 */
@Composable
fun GalleryDlUpdater() {
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        if (!GALLERY_DL_AUTO_UPDATE.getBoolean()) return@LaunchedEffect
        if (!GalleryDlEngine.isInstalled(context)) return@LaunchedEffect
        if (!PreferenceUtil.isNetworkAvailableForDownload()) return@LaunchedEffect

        val now = System.currentTimeMillis()
        if (now < GALLERY_DL_UPDATE_TIME.getLong() + GALLERY_DL_UPDATE_INTERVAL.getLong()) {
            return@LaunchedEffect
        }

        // Give the app shell a moment to settle before making the background Codeberg check.
        delay(1200L)

        val latestCommit = GalleryDlEngine.latestMasterCommit().getOrNull() ?: return@LaunchedEffect
        val installedSource = GalleryDlEngine.installedSource(context)
        if (installedSource == "codeberg:$latestCommit") {
            GALLERY_DL_UPDATE_TIME.updateLong(System.currentTimeMillis())
            return@LaunchedEffect
        }

        GalleryDlEngine.installLatest(context)
            .onSuccess { GALLERY_DL_UPDATE_TIME.updateLong(System.currentTimeMillis()) }
            .onFailure { it.printStackTrace() }
    }
}
