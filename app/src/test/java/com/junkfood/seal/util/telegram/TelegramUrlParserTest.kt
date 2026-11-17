// DISABLED: Telegram now uses the existing yt-dlp TelegramEmbedIE path.
// Original implementation/test kept commented for reference; do not re-enable a separate engine.
// package com.junkfood.seal.util.telegram
// 
// import com.junkfood.seal.util.telegram.url.TelegramUrlParser
// import org.junit.Assert.assertEquals
// import org.junit.Assert.assertNull
// import org.junit.Assert.assertTrue
// import org.junit.Test
// 
// class TelegramUrlParserTest {
//     @Test
//     fun parsesPublicChannelAndWebPreviewLinks() {
//         assertEquals("telegram", TelegramUrlParser.parse("https://t.me/telegram")?.username)
//         assertEquals("telegram", TelegramUrlParser.parse("https://t.me/s/telegram")?.username)
//         assertTrue(TelegramUrlParser.isPublicChannelUrl("https://t.me/s/telegram"))
//     }
// 
//     @Test
//     fun parsesSinglePublicPostLinks() {
//         val link = TelegramUrlParser.parse("https://t.me/telegram/12345")
//         assertEquals("telegram", link?.username)
//         assertEquals(12345L, link?.messageId)
//     }
// 
//     @Test
//     fun rejectsInviteLinksAndUntrustedHosts() {
//         assertNull(TelegramUrlParser.parse("https://t.me/+invitehash"))
//         assertNull(TelegramUrlParser.parse("https://example.com/telegram/123"))
//         assertNull(TelegramUrlParser.parse("http://t.me/telegram"))
//     }
// 
//     @Test
//     fun rejectsInvalidPostIdsAndExtraPathSegments() {
//         assertNull(TelegramUrlParser.parse("https://t.me/telegram/not-a-number"))
//         assertNull(TelegramUrlParser.parse("https://t.me/telegram/123/extra"))
//     }
// }
// 