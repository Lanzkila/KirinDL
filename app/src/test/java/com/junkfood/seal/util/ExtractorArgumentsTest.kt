package com.junkfood.seal.util

import com.yausername.youtubedl_android.YoutubeDLRequest
import org.junit.Assert.*
import org.junit.Test

class ExtractorArgumentsTest {
    private val url = "https://youtu.be/N5V8HEoo_10"
    private fun botError() = IllegalStateException("Sign in to confirm you're not a bot")

    @Test
    fun configuredArgumentsReachTheFirstRequestAndKeepSubtitleAndCommentOptions() {
        val request = YoutubeDLRequest(url)
            .addOption("-f", "137+140")
            .addOption("--extractor-args", "youtube:skip=translated_subs;max_comments=20")
        YouTubeRequestRecovery.execute(url, request,
            extractorArgs = "youtube:player_client=mweb;po_token=mweb.gvs+token") { actual ->
            val body = actual.getArguments("--extractor-args")!!.last()!!
            assertTrue(body.contains("player_client=mweb"))
            assertTrue(body.contains("po_token=mweb.gvs+token"))
            assertTrue(body.contains("skip=translated_subs"))
            assertTrue(body.contains("max_comments=20"))
            assertEquals("137+140", actual.getOption("-f"))
        }
    }

    @Test
    fun multipleExtractorLinesOverrideOnlyMatchingOptions() {
        val request = YoutubeDLRequest(url)
            .addOption("--extractor-args", "youtube:lang=ms;skip=translated_subs")
            .addOption("--extractor-args", "twitter:api=syndication")
        ExtractorArguments.merge(request, "youtube:lang=en\nyoutubetab:approximate_date=true")
        val args = request.getArguments("--extractor-args")!!.filterNotNull()
        assertTrue(args.contains("twitter:api=syndication"))
        assertEquals("youtube:lang=en;skip=translated_subs", args.last { it.startsWith("youtube:") })
        assertTrue(args.contains("youtubetab:approximate_date=true"))
    }

    @Test
    fun botChallengeCanTryIpv4WithoutDiscardingTheConfiguredClient() {
        val request = YoutubeDLRequest(url)
        var calls = 0
        val result = YouTubeRequestRecovery.execute(url, request,
            extractorArgs = "youtube:player_client=mweb;po_token=mweb.gvs+token") { actual ->
            if (++calls == 1) throw botError()
            assertTrue(actual.hasOption("-4"))
            assertFalse(actual.getArguments("--extractor-args")!!.any { it!!.contains("web_safari") })
            "success"
        }
        assertEquals("success", result)
        assertEquals(2, calls)
    }

    @Test
    fun explicitlyChosenAddressFamiliesStayUnchanged() {
        for (option in listOf("-4", "--force-ipv4", "-6", "--force-ipv6")) {
            val request = YoutubeDLRequest(url).addOption(option)
            var calls = 0
            runCatching {
                YouTubeRequestRecovery.execute(url, request,
                    extractorArgs = "youtube:player_client=web_safari") { calls++; throw botError() }
            }
            assertEquals(1, calls)
            assertEquals(option == "-4", request.hasOption("-4"))
        }
    }

    @Test
    fun transfer403DoesNotSwitchAddressFamilies() {
        val request = YoutubeDLRequest(url)
        var calls = 0
        YouTubeRequestRecovery.execute(url, request) {
            if (++calls == 1) throw IllegalStateException("HTTP Error 403: Forbidden")
            assertFalse(it.hasOption("-4"))
        }
        assertEquals(2, calls)
    }

    @Test
    fun unavailableIpv4RouteKeepsTheOriginalVerificationDiagnosis() {
        val request = YoutubeDLRequest(url)
        val original = botError()
        var calls = 0
        val error = runCatching {
            YouTubeRequestRecovery.execute(url, request) {
                if (++calls == 1) throw original
                throw IllegalStateException("Unable to download webpage: timed out")
            }
        }.exceptionOrNull()
        assertSame(original, error)
        assertEquals(2, calls)
        assertEquals(1, original.suppressed.size)
    }

    @Test
    fun verificationDetectionExcludesPrivateVideoErrorsAndOtherSites() {
        assertTrue(isYouTubeVerificationError(url, botError()))
        assertFalse(isYouTubeVerificationError("https://example.com", botError()))
        assertFalse(isYouTubeVerificationError(url, IllegalStateException("Sign in to watch this private video")))
    }
}
