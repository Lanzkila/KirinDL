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

    /**
     * Native process termination may bypass Java's uncaught-exception handler.
     * A leftover stage marker is only a diagnostic hint, not proof of a crash.
     */
    fun recoverInterruptedStartup(context: Context) {
        runCatching {
            val stageFile = File(context.filesDir, STAGE_FILE)
            val previousStage = stageFile.takeIf { it.isFile }?.readText()
                ?.takeIf { it.isNotBlank() } ?: return@runCatching
            val reportFile = File(context.filesDir, REPORT_FILE)
            val previousReport = reportFile.takeIf { it.isFile }?.readText().orEmpty()
            reportFile.writeText(
                buildString {
                    append("Previous launch did not finish startup.\n")
                    append("Last recorded stage: $previousStage\n")
                    append("This is a diagnostic hint; a native crash may not provide a Java stack trace.\n")
                    if (previousReport.isNotBlank()) {
                        append("\nPrevious saved crash report:\n")
                        append(previousReport.take(20_000))
                    }
                }
            )
        }.onFailure { Log.w(TAG, "Unable to recover startup breadcrumb", it) }
    }

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
