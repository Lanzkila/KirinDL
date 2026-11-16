package com.junkfood.seal.util.telegram

import com.junkfood.seal.util.telegram.models.TelegramMedia
import com.junkfood.seal.util.telegram.models.TelegramPostPreview
import com.junkfood.seal.util.telegram.selection.TelegramMediaSelection
import org.junit.Assert.assertEquals
import org.junit.Test

class TelegramMediaSelectionTest {
    private fun post(id: Long, vararg mediaIds: String) =
        TelegramPostPreview(
            id = id,
            url = "https://t.me/telegram/$id",
            title = "Post $id",
            text = "",
            media =
                mediaIds.map { mediaId ->
                    TelegramMedia(
                        id = mediaId,
                        kind = "photo",
                        downloadUrl = "https://cdn1.telesco.pe/file/$mediaId.jpg",
                    )
                },
        )

    @Test
    fun selectsAllMediaAcrossPreviewedPosts() {
        val posts = listOf(post(1, "a", "b"), post(2, "c"))
        assertEquals(setOf("a", "b", "c"), TelegramMediaSelection.selectAll(posts))
    }

    @Test
    fun selectedMediaRetainsPostAndAlbumOrder() {
        val posts = listOf(post(1, "a", "b"), post(2, "c"))
        val selected = TelegramMediaSelection.selectedMedia(posts, setOf("c", "a"))
        assertEquals(listOf("a", "c"), selected.map(TelegramMedia::id))
    }

    @Test
    fun togglesAndClearsSelection() {
        val item = TelegramMedia(
            id = "post-1",
            kind = "photo",
            downloadUrl = "https://cdn1.telesco.pe/file/post-1.jpg",
        )
        val selected = TelegramMediaSelection.toggle(emptySet(), item)
        assertEquals(setOf("post-1"), selected)
        assertEquals(emptySet<String>(), TelegramMediaSelection.toggle(selected, item))
        assertEquals(emptySet<String>(), TelegramMediaSelection.clear())
    }
}
