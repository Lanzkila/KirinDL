// DISABLED: Telegram now uses the existing yt-dlp TelegramEmbedIE path.
// Original implementation/test kept commented for reference; do not re-enable a separate engine.
// package com.junkfood.seal.util.telegram.selection
// 
// import com.junkfood.seal.util.telegram.models.TelegramMedia
// import com.junkfood.seal.util.telegram.models.TelegramPostPreview
// 
// /** Selection helpers shared by a future Telegram preview grid and the download engine. */
// object TelegramMediaSelection {
//     fun toggle(selectedIds: Set<String>, media: TelegramMedia): Set<String> =
//         if (media.id in selectedIds) selectedIds - media.id else selectedIds + media.id
// 
//     fun selectAll(posts: List<TelegramPostPreview>): Set<String> =
//         posts.flatMap(TelegramPostPreview::media)
//             .map(TelegramMedia::id)
//             .filter(String::isNotBlank)
//             .toSet()
// 
//     fun clear(): Set<String> = emptySet()
// 
//     fun selectedMedia(
//         posts: List<TelegramPostPreview>,
//         selectedIds: Set<String>,
//     ): List<TelegramMedia> =
//         posts.flatMap(TelegramPostPreview::media)
//             .filter { it.id in selectedIds && it.downloadUrl.startsWith("https://") }
//             .distinctBy(TelegramMedia::id)
// }
// 