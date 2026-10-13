package com.junkfood.seal.util

import org.junit.Assert.*
import org.junit.Test

class DownloadLogStoreTest {
    @Test fun logsKeepCdnHostnameAndErrorWithoutSignedQueryOrCookie() {
        val value = DownloadLogStore.sanitize("Got EOF: https://cdn.example/video.m4s?sig=secret&mid=123")
        assertTrue(value.contains("cdn.example/video.m4s"))
        assertFalse(value.contains("secret"))
        assertFalse(DownloadLogStore.sanitize("Cookie: SESSDATA=secret").contains("secret"))
    }

    @Test fun recentLogIsBoundedAndAvailableBeforeFailure() {
        repeat(300) { DownloadLogStore.append("test-active", "line $it") }
        val log = DownloadLogStore.read("test-active").lines()
        assertEquals(250, log.size)
        assertEquals("line 50", log.first())
        assertEquals("line 299", log.last())
    }
}
