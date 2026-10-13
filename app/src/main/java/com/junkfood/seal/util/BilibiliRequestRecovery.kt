package com.junkfood.seal.util

import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import java.net.URI
import java.util.Locale
import java.util.concurrent.CancellationException

/** Signals that the bounded site recovery has already run; do not restart its loop. */
internal class BilibiliRecoveryException(attempts: Int, cause: Exception) :
    Exception("Bilibili transfer failed after $attempts attempts.\n${cause.message.orEmpty()}", cause)

internal object BilibiliRequestRecovery {
    private val mirrorMarker = Regex("\\[KirinBilibili] backups=(\\d+)")

    fun handles(url: String): Boolean {
        val host = runCatching { URI(url.trim()).host.orEmpty().lowercase(Locale.ROOT) }
            .getOrDefault("")
        // The international bilibili.tv extractor has a different API.
        return host == "b23.tv" || host.endsWith(".b23.tv") ||
            host == "bilibili.com" || host.endsWith(".bilibili.com")
    }

    fun <T> execute(
        url: String,
        request: YoutubeDLRequest,
        pluginDirectory: String,
        cancellationCheck: () -> Unit = {},
        onRetry: (String) -> Unit = {},
        runRequest: (YoutubeDLRequest, (String) -> Unit) -> T,
    ): T {
        if (!handles(url)) return runRequest(request) {}
        request.addOption("--plugin-dirs", pluginDirectory)
        // Short, bounded retries on one URL before refreshing extraction/mirrors.
        // Keep downloader, concurrency, cookies, format and output choices intact.
        request.addOption("--retries", "2")
        request.addOption("--fragment-retries", "2")
        request.addOption("--retry-sleep", "http:exp=1:4")
        request.addOption("--retry-sleep", "fragment:exp=1:4")
        request.addOption("--socket-timeout", "20")
        var availableMirrors = -1
        for (attempt in 0..2) {
            cancellationCheck()
            var extractedThisAttempt = false
            var observedTransferFailure = false
            var observedNonRetryableFailure = false
            ExtractorArguments.merge(request, "bilibili:kirin_cdn_attempt=$attempt")
            if (attempt > 0) {
                onRetry(if (availableMirrors >= attempt)
                    "Retrying Bilibili with backup server $attempt/2..."
                else "Retrying Bilibili with a refreshed video URL...")
            }
            try {
                return runRequest(request) { line ->
                    mirrorMarker.find(line)?.groupValues?.get(1)?.toIntOrNull()?.let {
                        extractedThisAttempt = true
                        availableMirrors = maxOf(availableMirrors, it.coerceIn(0, 2))
                    }
                    // aria2 writes its transport diagnosis to stdout. The Android
                    // wrapper's exception may contain only stderr's exit-code summary.
                    observedTransferFailure = observedTransferFailure || isTransferFailure(line)
                    observedNonRetryableFailure = observedNonRetryableFailure || isNonRetryableFailure(line)
                }
            } catch (error: Exception) {
                cancellationCheck()
                val message = error.message.orEmpty()
                if (isCancellation(error) || isNonRetryableFailure(message) ||
                    observedNonRetryableFailure || !extractedThisAttempt ||
                    (!isTransferFailure(message) && !observedTransferFailure)) {
                    throw error
                }
                // One refreshed extraction even when this API response has no backups.
                // A second mirror is tried only if Bilibili actually supplied one.
                if (attempt == 2 || (attempt == 1 && availableMirrors < 2)) {
                    throw BilibiliRecoveryException(attempt + 1, error)
                }
            }
        }
        error("Unreachable Bilibili recovery state")
    }

    private fun isCancellation(error: Exception): Boolean =
        error is YoutubeDL.CanceledException || error is CancellationException ||
            error is InterruptedException

    private fun isNonRetryableFailure(message: String): Boolean {
        val text = message.lowercase(Locale.ROOT)
        return listOf("no space left", "permission denied", "http error 429", "http error 451",
            "requested format is not available", "postprocessing:").any(text::contains)
    }

    private fun isTransferFailure(message: String): Boolean {
        val text = message.lowercase(Locale.ROOT)
        return listOf("got eof from the server", "remote end closed connection",
            "remotedisconnected", "incompleteread", "more expected", "connection reset",
            "connection aborted", "timed out", "http error 403", "http error 410",
            "http error 5").any(text::contains)
    }
}
