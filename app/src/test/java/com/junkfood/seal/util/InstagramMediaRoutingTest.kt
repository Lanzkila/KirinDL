package com.junkfood.seal.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InstagramMediaRoutingTest {
    @Test
    fun reelAndIgtvRoutesUseYtDlp() {
        assertEquals(InstagramMediaRouting.Route.YTDLP, InstagramMediaRouting.routeFor("https://www.instagram.com/reel/ABC123/"))
        assertEquals(InstagramMediaRouting.Route.YTDLP, InstagramMediaRouting.routeFor("https://instagram.com/reels/ABC123/"))
        assertEquals(InstagramMediaRouting.Route.YTDLP, InstagramMediaRouting.routeFor("https://www.instagram.com/tv/ABC123/"))
    }

    @Test
    fun liveReplayRoutesUseYtDlp() {
        assertEquals(InstagramMediaRouting.Route.YTDLP, InstagramMediaRouting.routeFor("https://www.instagram.com/live/ABC123/"))
        assertEquals(InstagramMediaRouting.Route.YTDLP, InstagramMediaRouting.routeFor("https://www.instagram.com/creator/live/ABC123/"))
        assertEquals(InstagramMediaRouting.Route.YTDLP, InstagramMediaRouting.routeFor("https://www.instagram.com/share/reel/ABC123/"))
    }

    @Test
    fun ambiguousPostIsProbedBeforeChoosingAnEngine() {
        assertEquals(InstagramMediaRouting.Route.PROBE_WITH_YTDLP, InstagramMediaRouting.routeFor("https://www.instagram.com/p/ABC123/?img_index=1"))
    }

    @Test
    fun profilesAndStoriesStayInGalleryDl() {
        assertEquals(InstagramMediaRouting.Route.GALLERY_DL, InstagramMediaRouting.routeFor("https://www.instagram.com/example_user/"))
        assertEquals(InstagramMediaRouting.Route.GALLERY_DL, InstagramMediaRouting.routeFor("https://www.instagram.com/stories/example_user/123/"))
    }

    @Test
    fun nonInstagramHostsAreNotMisclassified() {
        assertEquals(InstagramMediaRouting.Route.NOT_INSTAGRAM, InstagramMediaRouting.routeFor("https://example.com/reel/ABC123/"))
        assertEquals(InstagramMediaRouting.Route.NOT_INSTAGRAM, InstagramMediaRouting.routeFor("https://evilinstagram.com/reel/ABC123/"))
        assertFalse(InstagramMediaRouting.isInstagramUrl("https://example.com/p/ABC123/"))
    }

    @Test
    fun videoProbeRequiresActualVideoCodec() {
        assertTrue(InstagramMediaRouting.hasVideoStream(listOf(Format(vcodec = "h264", acodec = "aac")), null))
        assertTrue(InstagramMediaRouting.hasVideoStream(null, "avc1"))
        assertFalse(InstagramMediaRouting.hasVideoStream(listOf(Format(vcodec = "none", acodec = "aac")), null))
        assertFalse(InstagramMediaRouting.hasVideoStream(null, "none"))
    }
}
