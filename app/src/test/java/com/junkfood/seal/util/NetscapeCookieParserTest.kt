package com.junkfood.seal.util

import org.junit.Assert.*
import org.junit.Test

class NetscapeCookieParserTest {
    @Test
    fun httpOnlyOnlyExportKeepsLoginSessionAndRoundTrips() {
        val text = "# Netscape HTTP Cookie File\r\n" +
            "#HttpOnly_.youtube.com\tTRUE\t/\tTRUE\t0\tSID\tsession-token\r\n"
        assertTrue(NetscapeCookieParser.accepts(text))
        val cookie = NetscapeCookieParser.parse(text).single()
        assertEquals("SID", cookie.name)
        assertEquals("session-token", cookie.value)
        assertTrue(cookie.isHttpOnly)
        assertTrue(cookie.secure)
        assertEquals(cookie, NetscapeCookieParser.parse(cookie.toNetscapeCookieString()).single())
    }

    @Test
    fun hostOnlyAndEmptyValuesArePreservedForMozillaCookieJar() {
        val text = "#HttpOnly_www.youtube.com\tFALSE\t/\tTRUE\t0\tSID\tvalue\n" +
            ".youtube.com\tTRUE\t/\tFALSE\t0\tEMPTY\t"
        val cookies = NetscapeCookieParser.parse(text)
        assertEquals(2, cookies.size)
        assertEquals("www.youtube.com", cookies[0].domain)
        assertFalse(cookies[0].includeSubdomains)
        assertEquals(".youtube.com", cookies[1].domain)
        assertEquals("", cookies[1].value)
        assertTrue(cookies[1].toNetscapeCookieString().endsWith('\t'))
    }

    @Test
    fun expiredAndMalformedRowsCannotBecomeLoginCookies() {
        val text = ".youtube.com\tTRUE\t/\tTRUE\t10\tEXPIRED\tvalue\n" +
            ".youtube.com\tTRUE\t/\tTRUE\tinvalid\tBAD\tvalue\n" +
            ".youtube.com\tTRUE\t/\tTRUE\t0\tSESSION\tvalid"
        assertEquals(listOf("SESSION"), NetscapeCookieParser.parse(text, now = 10L).map { it.name })
    }
}
