package com.junkfood.seal.ui.page

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.junkfood.seal.Downloader
import com.junkfood.seal.util.PreferenceUtil
import com.junkfood.seal.util.PreferenceUtil.getBoolean
import com.junkfood.seal.util.PreferenceUtil.getLong
import com.junkfood.seal.util.PreferenceUtil.getString
import com.junkfood.seal.util.PreferenceUtil.updateBoolean
import com.junkfood.seal.util.PreferenceUtil.updateLong
import com.junkfood.seal.util.UpdateUtil
import com.junkfood.seal.util.YT_DLP_AUTO_UPDATE
import com.junkfood.seal.util.YT_DLP_UPDATE_INTERVAL
import com.junkfood.seal.util.YT_DLP_UPDATE_TIME
import com.junkfood.seal.util.YT_DLP_VERSION
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val OLD_KIRIN_YTDLP_RECOVERY_KEY = "kirin_ytdlp_bundled_recovery_v1"
private const val KIRIN_YTDLP_SEALPLUS_RESTORE_V1 = "kirin_ytdlp_sealplus_restore_v1"

@Composable
fun YtdlpUpdater() {

    val downloaderState by Downloader.downloaderState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        /*
         * Restore the original Sealplus updater behaviour.
         *
         * A previous KirinDL recovery patch deleted the downloaded yt-dlp runtime,
         * restored the APK-bundled engine and forced YT_DLP_AUTO_UPDATE to false.
         * If that recovery already ran on this installation, undo only that forced
         * updater state once so yt-dlp can update normally again.
         */
        if (!KIRIN_YTDLP_SEALPLUS_RESTORE_V1.getBoolean(false)) {
            if (OLD_KIRIN_YTDLP_RECOVERY_KEY.getBoolean(false)) {
                YT_DLP_AUTO_UPDATE.updateBoolean(true)
                YT_DLP_UPDATE_TIME.updateLong(0L)
            }
            KIRIN_YTDLP_SEALPLUS_RESTORE_V1.updateBoolean(true)
        }

        if (downloaderState !is Downloader.State.Idle) return@LaunchedEffect

        // Original Sealplus behaviour:
        // when auto update is disabled and a runtime version is already present, leave it alone.
        if (!YT_DLP_AUTO_UPDATE.getBoolean() && YT_DLP_VERSION.getString().isNotEmpty()) {
            return@LaunchedEffect
        }

        if (!PreferenceUtil.isNetworkAvailableForDownload()) {
            return@LaunchedEffect
        }

        val lastUpdateTime = YT_DLP_UPDATE_TIME.getLong()
        val currentTime = System.currentTimeMillis()

        if (currentTime < lastUpdateTime + YT_DLP_UPDATE_INTERVAL.getLong()) {
            return@LaunchedEffect
        }

        try {
            Downloader.updateState(state = Downloader.State.Updating)
            withContext(Dispatchers.IO) { UpdateUtil.updateYtDlp() }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            Downloader.updateState(state = Downloader.State.Idle)
        }
    }
}
