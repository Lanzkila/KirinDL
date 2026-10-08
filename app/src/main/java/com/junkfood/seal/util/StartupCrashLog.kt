package com.junkfood.seal.util

import android.content.Context
import android.util.Log
import java.io.File

/**
 * Best-effort, app-private diagnostics for crashes that happen before the UI is created.
 * Never clears user settings or media, and never sends reports to a server.
 */
object StartupCrashLog {
    private const val TAG = "KirinDL.Startup"
    private const val REPORT_FILE = "kirindl_last_crash.txt"
    private const val STAGE_FILE = "kirindl_startup_stage.txt"

    fun markStartupStage(context: Context, stage: String) {
        runCatching { File(context.filesDir, STAGE_FILE).writeText(stage) }
            .onFailure { Log.w(TAG, "Unable to record startup stage", it) }
    }

    fun markStartupReady(context: Context) {
        runCatching { File(context.filesDir, STAGE_FILE).delete() }
            .onFailure { Log.w(TAG, "Unable to clear startup marker", it) }
    }

    fun save(context: Context, report: String) {
        runCatching {
            val stage = File(context.filesDir, STAGE_FILE).takeIf { it.exists() }?.readText()
            File(context.filesDir, REPORT_FILE).writeText(
                buildString {
                    if (!stage.isNullOrBlank()) append("Last startup stage: $stage\n")
                    append(report.take(40_000))
                }
            )
        }.onFailure { Log.e(TAG, "Unable to save crash report", it) }
    }

    fun read(context: Context): String? =
        runCatching { File(context.filesDir, REPORT_FILE).takeIf { it.isFile }?.readText() }
            .getOrNull()
}
