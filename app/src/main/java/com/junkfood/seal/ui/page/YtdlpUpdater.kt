package com.junkfood.seal.ui.page

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.junkfood.seal.App
import com.junkfood.seal.Downloader
import com.junkfood.seal.util.PreferenceUtil
import com.junkfood.seal.util.PreferenceUtil.getBoolean
import com.junkfood.seal.util.PreferenceUtil.getLong
import com.junkfood.seal.util.PreferenceUtil.getString
import com.junkfood.seal.util.PreferenceUtil.updateBoolean
import com.junkfood.seal.util.PreferenceUtil.updateLong
import com.junkfood.seal.util.PreferenceUtil.updateString
import com.junkfood.seal.util.UpdateUtil
import com.junkfood.seal.util.YT_DLP_AUTO_UPDATE
import com.junkfood.seal.util.YT_DLP_UPDATE_INTERVAL
import com.junkfood.seal.util.YT_DLP_UPDATE_TIME
import com.junkfood.seal.util.YT_DLP_VERSION
import com.yausername.youtubedl_android.YoutubeDL
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val KIRIN_YTDLP_BUNDLED_RECOVERY_V1 = "kirin_ytdlp_bundled_recovery_v1"

@Composable
fun YtdlpUpdater() {

    val downloaderState by Downloader.downloaderState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        if (downloaderState !is Downloader.State.Idle) return@LaunchedEffect

        /*
         * KirinDL yt-dlp runtime recovery.
         *
         * The downloader source in current KirinDL is already effectively the same as the
         * v3.1.5 stable baseline. yt-dlp itself, however, is stored under noBackupFilesDir and
         * can survive normal APK updates. If an updater run leaves that runtime binary in a bad
         * state, every extractor can fail together (YouTube, Instagram, etc.) even though the
         * Kotlin downloader code is unchanged.
         *
         * Run this recovery exactly once:
         *  1) disable automatic yt-dlp replacement,
         *  2) delete only the downloaded yt-dlp runtime directory,
         *  3) restore the yt-dlp binary bundled inside the APK,
         *  4) remember the restored version.
         *
         * User settings, history, queues, cookies, media and download directories are untouched.
         * Manual yt-dlp updates from Settings remain possible after this recovery.
         */
        if (!KIRIN_YTDLP_BUNDLED_RECOVERY_V1.getBoolean(false)) {
            withContext(Dispatchers.IO) {
                YT_DLP_AUTO_UPDATE.updateBoolean(false)

                val ytdlpDir =
                    File(
                        App.context.noBackupFilesDir,
                        "${YoutubeDL.baseName}/${YoutubeDL.ytdlpDirName}",
                    )

                if (ytdlpDir.exists() && !ytdlpDir.deleteRecursively()) {
                    throw IllegalStateException(
                        "Failed to reset yt-dlp runtime directory: ${ytdlpDir.absolutePath}"
                    )
                }

                // Re-copy the yt-dlp binary bundled with youtubedl-android.
                YoutubeDL.init_ytdlp(App.context, ytdlpDir)

                YoutubeDL.getInstance()
                    .version(App.context)
                    ?.takeIf { it.isNotBlank() }
                    ?.let { version -> YT_DLP_VERSION.updateString(version) }

                // Clear the previous updater timestamp so it cannot describe the removed runtime.
                YT_DLP_UPDATE_TIME.updateLong(0L)
                KIRIN_YTDLP_BUNDLED_RECOVERY_V1.updateBoolean(true)
            }
            return@LaunchedEffect
        }

        // OFF now means truly OFF. Do not update the engine automatically simply because the
        // stored version string is empty. A manual update from Settings is still available.
        if (!YT_DLP_AUTO_UPDATE.getBoolean()) return@LaunchedEffect

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
            // Guaranteed to run even on CancellationException, so the downloader
            // never gets stuck in the Updating state after the coroutine is cancelled.
            Downloader.updateState(state = Downloader.State.Idle)
        }
    }
}
