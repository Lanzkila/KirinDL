package com.junkfood.seal

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.junkfood.seal.util.UpdateUtil

/**
 * Receives Android DownloadManager completion events for KirinDL app updates only.
 *
 * Media downloads are not handled here. The receiver verifies the DownloadManager id against the
 * pending KirinDL update id before opening Android's package installer.
 */
class AppUpdateDownloadReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != DownloadManager.ACTION_DOWNLOAD_COMPLETE) return

        val downloadId =
            intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
        if (downloadId < 0L || !UpdateUtil.isPendingAppUpdateDownload(context, downloadId)) {
            return
        }

        UpdateUtil.handleCompletedAppUpdateDownload(context, downloadId)
            .onFailure { error ->
                Log.w(TAG, "Could not continue KirinDL app update install", error)
            }
    }

    companion object {
        private const val TAG = "AppUpdateReceiver"
    }
}
