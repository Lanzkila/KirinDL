package com.junkfood.seal.util

import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import java.util.concurrent.CancellationException
import org.junit.Assert.*
import org.junit.Test

class YouTubeRequestRecoveryTest {
    private val url = "https://youtu.be/xcSURfABiUw"
    private fun botError() = IllegalStateException(
        "ERROR: [youtube] xcSURfABiUw: Sign in to confirm you’re not a bot. Use --cookies",
    )

    @Test
    fun successfulRequestsRunOnceWithoutChangingOptions() {
        val request = YoutubeDLRequest(url).addOption("-f", "137+140")
        val original = request.buildCommand()
        var calls = 0
        val result = YouTubeRequestRecovery.execute(url, request) { calls++; "ok" }
        assertEquals("ok", result)
        assertEquals(1, calls)
        assertEquals(original, request.buildCommand())
    }

    @Test
    fun challengeRetriesWithSubtitleAndFormatChoicesIntact() {
        val request = YoutubeDLRequest(url)
            .addOption("-f", "137+140")
            .addOption("--extractor-args", "youtube:skip=translated_subs;max_comments=10")
            .addOption("--extractor-args", "twitter:api=syndication")
        var calls = 0
        val result = YouTubeRequestRecovery.execute(url, request) { retry ->
            if (++calls == 1) throw botError()
            assertEquals("137+140", retry.getOption("-f"))
            val args = retry.getArguments("--extractor-args").orEmpty()
            assertTrue(args.contains("twitter:api=syndication"))
            assertTrue(args.last().orEmpty().contains("skip=translated_subs"))
            assertTrue(args.last().orEmpty().contains("max_comments=10"))
            assertTrue(args.last().orEmpty().contains("web_safari"))
            "formats"
        }
        assertEquals("formats", result)
        assertEquals(2, calls)
    }

    @Test
    fun staleSessionCanRetryDirectVideoAsGuestWithoutRemovingCookieFile() {
        val request = YoutubeDLRequest(url).addOption("--cookies", "/private/cookies.txt")
        var calls = 0
        val result = YouTubeRequestRecovery.execute(url, request) { retry ->
            if (++calls < 3) throw botError()
            assertTrue(retry.hasOption("--no-cookies"))
            assertEquals("/private/cookies.txt", retry.getOption("--cookies"))
            "public video"
        }
        assertEquals("public video", result)
        assertEquals(3, calls)
    }

    @Test
    fun persistentChallengeStopsAfterThreeAttemptsAndKeepsLastError() {
        val request = YoutubeDLRequest(url).addOption("--cookies", "/private/cookies.txt")
        val lastError = botError()
        var calls = 0
        val error = runCatching {
            YouTubeRequestRecovery.execute(url, request) {
                calls++
                throw lastError
            }
        }.exceptionOrNull()
        assertSame(lastError, error)
        assertEquals(3, calls)
    }

    @Test
    fun playlistNeverDropsAccountCookies() {
        val playlist = "https://www.youtube.com/playlist?list=private"
        val request = YoutubeDLRequest(playlist).addOption("--cookies", "/private/cookies.txt")
        var calls = 0
        runCatching {
            YouTubeRequestRecovery.execute(playlist, request) { calls++; throw botError() }
        }
        assertEquals(2, calls)
        assertFalse(request.hasOption("--no-cookies"))
    }

    @Test
    fun videoWithPlaylistParameterRequiresNoPlaylistBeforeGuestRetry() {
        val playlist = "$url?list=private"
        for (noPlaylist in listOf(false, true)) {
            val request = YoutubeDLRequest(playlist).addOption("--cookies", "/private/cookies.txt")
            if (noPlaylist) request.addOption("--no-playlist")
            var calls = 0
            runCatching {
                YouTubeRequestRecovery.execute(playlist, request) { calls++; throw botError() }
            }
            assertEquals(if (noPlaylist) 3 else 2, calls)
        }
    }

    @Test
    fun unrelatedSitesRestrictedVideosAndRateLimitsDoNotRetry() {
        for ((target, error) in listOf(
            "https://example.com/video" to botError(),
            "https://youtube.com.example.com/watch?v=xcSURfABiUw" to botError(),
            url to IllegalStateException("Sign in to confirm your age"),
            url to IllegalStateException("This is a private video"),
            url to IllegalStateException("HTTP Error 429: Too Many Requests"),
            url to YoutubeDL.CanceledException(),
            url to CancellationException("Sign in to confirm you're not a bot"),
        )) {
            var calls = 0
            val actual = runCatching {
                YouTubeRequestRecovery.execute(target, YoutubeDLRequest(target)) { calls++; throw error }
            }.exceptionOrNull()
            assertSame(error, actual)
            assertEquals(1, calls)
        }
    }

    @Test
    fun cancelBetweenProcessesDoesNotLaunchRetry() {
        var calls = 0
        var cancelled = false
        val error = runCatching {
            YouTubeRequestRecovery.execute(
                url,
                YoutubeDLRequest(url),
                cancellationCheck = { if (cancelled) throw CancellationException() },
            ) {
                calls++
                cancelled = true
                throw botError()
            }
        }.exceptionOrNull()
        assertTrue(error is CancellationException)
        assertEquals(1, calls)
    }

    @Test
    fun cancelFromRetryCallbackDoesNotLaunchAnotherProcess() {
        var calls = 0
        var cancelled = false
        val error = runCatching {
            YouTubeRequestRecovery.execute(
                url,
                YoutubeDLRequest(url),
                cancellationCheck = { if (cancelled) throw CancellationException() },
                onRetry = { cancelled = true },
            ) { calls++; throw botError() }
        }.exceptionOrNull()
        assertTrue(error is CancellationException)
        assertEquals(1, calls)
    }

    @Test
    fun forbiddenMediaRetriesClientsButDoesNotDropCookies() {
        val request = YoutubeDLRequest(url).addOption("--cookies", "/private/cookies.txt")
        var calls = 0
        runCatching {
            YouTubeRequestRecovery.execute(url, request) {
                calls++
                throw IllegalStateException("unable to download video data: HTTP Error 403: Forbidden")
            }
        }
        assertEquals(2, calls)
        assertFalse(request.hasOption("--no-cookies"))
    }

    @Test
    fun shortMobileAndEmbeddedProfilesUseYouTubeCookieDomain() {
        for (profile in listOf(url, "youtu.be", "http://m.youtube.com/watch?v=xcSURfABiUw",
            "https://www.youtube-nocookie.com/embed/xcSURfABiUw")) {
            assertEquals("https://www.youtube.com/", cookieProfileUrl(profile))
        }
        assertEquals("https://example.com/", cookieProfileUrl("http://example.com/"))
        assertFalse(isYouTubeUrl("https://youtube.com.example.com/"))
    }
}
