package com.junkfood.seal.util

import com.yausername.youtubedl_android.YoutubeDLRequest
import java.util.concurrent.CancellationException
import org.junit.Assert.*
import org.junit.Test

class BilibiliRequestRecoveryTest {
    private val url = "https://www.bilibili.com/video/BV1Eepu6nEog?p=1"
    private fun eof() = IllegalStateException("Got EOF from the server. ERROR: aria2c exited with code 1")

    @Test fun mirrorRetryPreservesFormatDownloaderCookiesAndOtherExtractorArguments() {
        val request = YoutubeDLRequest(url).addOption("-f", "100026+30280")
            .addOption("--downloader", "libaria2c.so").addOption("--cookies", "/private/cookies")
            .addOption("--extractor-args", "bilibili:prefer_multi_flv=true")
        var calls = 0
        val result = BilibiliRequestRecovery.execute(url, request, "/plugins") { retry, observe ->
            observe("[BiliBili] [KirinBilibili] backups=2 attempt=0 switched=0")
            if (++calls == 1) throw eof()
            assertEquals("100026+30280", retry.getOption("-f"))
            assertEquals("libaria2c.so", retry.getOption("--downloader"))
            assertEquals("/private/cookies", retry.getOption("--cookies"))
            val args = retry.getArguments("--extractor-args").orEmpty().last().orEmpty()
            assertTrue(args.contains("prefer_multi_flv=true"))
            assertTrue(args.contains("kirin_cdn_attempt=1"))
            "complete"
        }
        assertEquals("complete", result)
        assertEquals(2, calls)
    }

    @Test fun failureIsBoundedAndClassifiedAsAlreadyRecovered() {
        var calls = 0
        val error = runCatching {
            BilibiliRequestRecovery.execute(url, YoutubeDLRequest(url), "/plugins") { _, observe ->
                calls++
                observe("[KirinBilibili] backups=2")
                throw eof()
            }
        }.exceptionOrNull()
        assertTrue(error is BilibiliRecoveryException)
        assertEquals(3, calls)
        assertTrue(error!!.message.orEmpty().contains("Got EOF"))
    }

    @Test fun absentBackupsAllowOnlyOneFreshExtraction() {
        var calls = 0
        runCatching {
            BilibiliRequestRecovery.execute(url, YoutubeDLRequest(url), "/plugins") { _, observe ->
                calls++
                observe("[KirinBilibili] backups=0")
                throw IllegalStateException("Remote end closed connection without response")
            }
        }
        assertEquals(2, calls)
    }

    @Test fun cancellationStorageFailuresAndMetadataErrorsDoNotRetry() {
        for (error in listOf(CancellationException(), IllegalStateException("Permission denied"),
            IllegalStateException("Requested format is not available"), IllegalStateException("HTTP Error 429"))) {
            var calls = 0
            val caught = runCatching {
                BilibiliRequestRecovery.execute(url, YoutubeDLRequest(url), "/plugins") { _, observe ->
                    calls++
                    observe("[KirinBilibili] backups=2")
                    throw error
                }
            }.exceptionOrNull()
            assertSame(error, caught)
            assertEquals(1, calls)
        }
        var calls = 0
        runCatching {
            BilibiliRequestRecovery.execute(url, YoutubeDLRequest(url), "/plugins") { _, _ ->
                calls++
                throw IllegalStateException("Unable to download JSON: HTTP Error 403")
            }
        }
        assertEquals(1, calls)
    }

    @Test fun ariaStdoutFailureRecoversEvenWhenExceptionOnlyReportsExitCode() {
        var calls = 0
        BilibiliRequestRecovery.execute(url, YoutubeDLRequest(url), "/plugins") { _, observe ->
            observe("[KirinBilibili] backups=2")
            if (++calls == 1) {
                observe("Exception: errorCode=1 Got EOF from the server.")
                throw IllegalStateException("ERROR: aria2c exited with code 1")
            }
        }
        assertEquals(2, calls)
    }

    @Test fun failedRefreshDoesNotRetryBasedOnPreviousTransferMetadata() {
        var calls = 0
        runCatching {
            BilibiliRequestRecovery.execute(url, YoutubeDLRequest(url), "/plugins") { _, observe ->
                if (++calls == 1) {
                    observe("[KirinBilibili] backups=2")
                    throw eof()
                }
                throw IllegalStateException("Unable to download JSON: HTTP Error 403")
            }
        }
        assertEquals(2, calls)
    }

    @Test fun unrelatedSitesKeepOriginalOptionsAndRunOnce() {
        for (other in listOf("https://youtu.be/example", "https://bilibili.tv/video/test",
            "https://bilibili.com.example.org/video/test")) {
            val request = YoutubeDLRequest(other)
            val original = request.buildCommand()
            var calls = 0
            BilibiliRequestRecovery.execute(other, request, "/plugins") { _, _ -> calls++ }
            assertEquals(original, request.buildCommand())
            assertEquals(1, calls)
        }
        assertTrue(BilibiliRequestRecovery.handles("https://b23.tv/test"))
    }
}
