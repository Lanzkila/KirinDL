package com.junkfood.seal.ui.page.tools

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.junkfood.seal.download.DownloaderV2
import com.junkfood.seal.download.Task
import com.junkfood.seal.ui.component.BackButton
import com.junkfood.seal.util.DownloadUtil
import com.junkfood.seal.util.PlaylistResult
import com.junkfood.seal.util.VideoInfo
import java.util.Locale
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UtilityToolPage(
    title: String,
    subtitle: String,
    onNavigateBack: () -> Unit,
    content: @Composable () -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        Text(title)
                        Text(
                            subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                navigationIcon = { BackButton(onNavigateBack) },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { padding ->
        Column(
            modifier =
                Modifier.fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            content()
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun UtilityCard(content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        tonalElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            content()
        }
    }
}

@Composable
fun SubtitleDownloaderPage(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var url by remember { mutableStateOf("") }
    var info by remember { mutableStateOf<VideoInfo?>(null) }
    var language by remember { mutableStateOf("") }
    var manual by remember { mutableStateOf(true) }
    var auto by remember { mutableStateOf(false) }
    var format by remember { mutableStateOf("srt") }
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("") }

    val availableLanguages =
        remember(info, manual, auto) {
            buildList {
                if (manual) addAll(info?.subtitles?.keys.orEmpty())
                if (auto) addAll(info?.automaticCaptions?.keys.orEmpty())
            }.filterNot { it.equals("live_chat", true) }.distinct().sorted()
        }

    UtilityToolPage(
        title = "Subtitle Downloader",
        subtitle = "Download subtitle tracks without downloading the video",
        onNavigateBack = onNavigateBack,
    ) {
        if (busy) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())

        UtilityCard {
            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Video URL") },
                singleLine = true,
            )
            Button(
                onClick = {
                    scope.launch {
                        busy = true
                        status = "Fetching subtitle tracks…"
                        runCatching { KirinUtilityEngine.fetchVideo(url.trim()) }
                            .onSuccess {
                                info = it
                                val first =
                                    it.subtitles.keys.firstOrNull()
                                        ?: it.automaticCaptions.keys.firstOrNull()
                                if (language.isBlank()) language = first.orEmpty()
                                status =
                                    "Found ${it.subtitles.size} manual and ${it.automaticCaptions.size} auto languages."
                            }
                            .onFailure { status = it.message ?: "Unable to fetch subtitles." }
                        busy = false
                    }
                },
                enabled = url.isNotBlank() && !busy,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Fetch subtitle tracks")
            }
        }

        UtilityCard {
            Text("Subtitle source", fontWeight = FontWeight.SemiBold)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Original / manual")
                Switch(checked = manual, onCheckedChange = { manual = it })
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Auto-generated")
                Switch(checked = auto, onCheckedChange = { auto = it })
            }

            OutlinedTextField(
                value = language,
                onValueChange = { language = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Language / yt-dlp pattern") },
                supportingText = { Text("Example: en, en.*, ja, all") },
                singleLine = true,
            )

            if (availableLanguages.isNotEmpty()) {
                Text("Available languages", style = MaterialTheme.typography.labelLarge)
                FlowingChips(
                    labels = availableLanguages.take(24),
                    selected = language,
                    onSelect = { language = it },
                )
            }

            Text("Output format", style = MaterialTheme.typography.labelLarge)
            FlowingChips(
                labels = listOf("original", "srt", "vtt", "ass", "lrc"),
                selected = format,
                onSelect = { format = it },
            )

            Button(
                onClick = {
                    scope.launch {
                        busy = true
                        status = "Downloading subtitles…"
                        runCatching {
                            KirinUtilityEngine.downloadSubtitleOnly(
                                context = context,
                                url = url.trim(),
                                language = language.trim(),
                                includeManual = manual,
                                includeAuto = auto,
                                outputFormat = format,
                            )
                        }.onSuccess {
                            status = "Saved subtitle files to $it"
                        }.onFailure {
                            status = it.message ?: "Subtitle download failed."
                        }
                        busy = false
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled =
                    info != null &&
                        language.isNotBlank() &&
                        (manual || auto) &&
                        !busy,
            ) {
                Text("Download subtitles")
            }
        }

        if (status.isNotBlank()) Text(status, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun FlowingChips(
    labels: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        labels.chunked(3).forEach { rowItems ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                rowItems.forEach { label ->
                    FilterChip(
                        selected = selected == label,
                        onClick = { onSelect(label) },
                        label = { Text(label, maxLines = 1) },
                    )
                }
            }
        }
    }
}

@Composable
fun PlaylistExtractorPage(
    onNavigateBack: () -> Unit,
    downloader: DownloaderV2 = koinInject(),
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current
    var url by remember { mutableStateOf("") }
    var playlist by remember { mutableStateOf<PlaylistResult?>(null) }
    val selected = remember { mutableStateListOf<Int>() }
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("") }
    var exportFormat by remember { mutableStateOf("txt") }

    val entries = playlist?.entries.orEmpty()
    val selectedEntries = selected.mapNotNull { entries.getOrNull(it) }
    val selectedUrls =
        selected.mapNotNull { index ->
            entries.getOrNull(index)?.let(KirinUtilityEngine::playlistEntryUrl)
        }.distinct()

    val exportLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.CreateDocument("text/plain")
        ) { outputUri: Uri? ->
            if (outputUri != null) {
                runCatching {
                    val data = buildPlaylistExport(exportFormat, selectedEntries)
                    context.contentResolver.openOutputStream(outputUri)?.bufferedWriter()?.use {
                        it.write(data)
                    } ?: error("Unable to open export destination.")
                }.onSuccess {
                    status = "Exported ${selectedEntries.size} entries as ${exportFormat.uppercase(Locale.US)}."
                }.onFailure {
                    status = it.message ?: "Export failed."
                }
            }
        }

    UtilityToolPage(
        title = "Playlist / Channel Extractor",
        subtitle = "Extract, select and queue links from collections",
        onNavigateBack = onNavigateBack,
    ) {
        if (busy) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())

        UtilityCard {
            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Playlist / channel URL") },
                singleLine = true,
            )
            Button(
                onClick = {
                    scope.launch {
                        busy = true
                        status = "Extracting collection…"
                        runCatching { KirinUtilityEngine.extractPlaylist(url.trim()) }
                            .onSuccess { result ->
                                if (result is PlaylistResult) {
                                    playlist = result
                                    selected.clear()
                                    result.entries.orEmpty().indices.forEach { selected.add(it) }
                                    status = "Found ${result.entries.orEmpty().size} entries."
                                } else {
                                    status = "This URL resolved to a single video, not a playlist/channel."
                                }
                            }
                            .onFailure { status = it.message ?: "Unable to extract collection." }
                        busy = false
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = url.isNotBlank() && !busy,
            ) {
                Text("Extract links")
            }
        }

        if (entries.isNotEmpty()) {
            UtilityCard {
                Text(
                    playlist?.title ?: "Collection",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    "${selected.size} of ${entries.size} selected",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            selected.clear()
                            entries.indices.forEach { selected.add(it) }
                        }
                    ) {
                        Text("Select all")
                    }
                    OutlinedButton(onClick = { selected.clear() }) { Text("None") }
                }

                HorizontalDivider()

                entries.take(100).forEachIndexed { index, entry ->
                    Row(
                        modifier =
                            Modifier.fillMaxWidth()
                                .clickable {
                                    if (index in selected) selected.remove(index) else selected.add(index)
                                }
                                .padding(vertical = 4.dp),
                    ) {
                        Checkbox(
                            checked = index in selected,
                            onCheckedChange = {
                                if (it) {
                                    if (index !in selected) selected.add(index)
                                } else {
                                    selected.remove(index)
                                }
                            },
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(entry.title ?: entry.id ?: "Untitled")
                            KirinUtilityEngine.playlistEntryUrl(entry)?.let {
                                Text(
                                    it,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                )
                            }
                        }
                    }
                }

                if (entries.size > 100) {
                    Text(
                        "Showing first 100 entries. All selected items are still kept internally.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            clipboard.setText(AnnotatedString(selectedUrls.joinToString("\n")))
                            status = "Copied ${selectedUrls.size} links."
                        },
                        enabled = selectedUrls.isNotEmpty(),
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Copy selected")
                    }
                    Button(
                        onClick = {
                            val preferences = DownloadUtil.DownloadPreferences.createFromPreferences()
                            selectedUrls.forEach { itemUrl ->
                                downloader.enqueue(Task(url = itemUrl, preferences = preferences))
                            }
                            status = "Queued ${selectedUrls.size} items."
                        },
                        enabled = selectedUrls.isNotEmpty(),
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Queue selected")
                    }
                }

                Text("Export selected", style = MaterialTheme.typography.labelLarge)
                FlowingChips(
                    labels = listOf("txt", "json", "m3u"),
                    selected = exportFormat,
                    onSelect = { exportFormat = it },
                )
                OutlinedButton(
                    onClick = { exportLauncher.launch("kirindl-links.$exportFormat") },
                    enabled = selectedEntries.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Export ${exportFormat.uppercase(Locale.US)}")
                }
            }
        }

        if (status.isNotBlank()) Text(status, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun MediaInspectorPage(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var inspection by remember { mutableStateOf<MediaInspection?>(null) }
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("") }

    val picker =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            if (uri != null) {
                scope.launch {
                    busy = true
                    status = "Inspecting media…"
                    runCatching { KirinUtilityEngine.inspectMedia(context, uri) }
                        .onSuccess {
                            inspection = it
                            status = "Inspection complete."
                        }
                        .onFailure { status = it.message ?: "Unable to inspect media." }
                    busy = false
                }
            }
        }

    UtilityToolPage(
        title = "Media Inspector",
        subtitle = "Inspect local codec, bitrate, resolution and tracks",
        onNavigateBack = onNavigateBack,
    ) {
        if (busy) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())

        Button(
            onClick = { picker.launch(arrayOf("video/*", "audio/*")) },
            modifier = Modifier.fillMaxWidth(),
            enabled = !busy,
        ) {
            Text("Choose media file")
        }

        inspection?.let { item ->
            UtilityCard {
                Text(item.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                InfoLine("Size", item.sizeBytes?.let(::formatBytes) ?: "Unknown")
                InfoLine("Duration", item.durationMs?.let(::formatDuration) ?: "Unknown")
                InfoLine(
                    "Resolution",
                    if (item.width != null && item.height != null) "${item.width}x${item.height}" else "N/A",
                )
                InfoLine("Bitrate", item.bitrate?.let { "${it / 1000} kbps" } ?: "Unknown")
            }

            UtilityCard {
                Text("Tracks", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                item.tracks.forEach { track ->
                    Text("${track.index + 1}. ${track.type} • ${track.codec}", fontWeight = FontWeight.Medium)
                    if (track.detail.isNotBlank()) {
                        Text(
                            track.detail,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (track != item.tracks.last()) HorizontalDivider()
                }
            }
        }

        if (status.isNotBlank()) Text(status, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun InfoLine(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun MetadataEditorPage(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var uri by remember { mutableStateOf<Uri?>(null) }
    var fileName by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var artist by remember { mutableStateOf("") }
    var album by remember { mutableStateOf("") }
    var year by remember { mutableStateOf("") }
    var genre by remember { mutableStateOf("") }
    var artworkUri by remember { mutableStateOf<Uri?>(null) }
    var artworkName by remember { mutableStateOf("") }
    var removeExisting by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("") }

    val picker =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { picked: Uri? ->
            uri = picked
            fileName = picked?.lastPathSegment.orEmpty()
            status = if (picked != null) "Ready to edit metadata." else status
        }
    val artworkPicker =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { picked: Uri? ->
            artworkUri = picked
            artworkName = picked?.lastPathSegment.orEmpty()
        }

    UtilityToolPage(
        title = "Metadata Editor",
        subtitle = "Edit tags using stream-copy; original file stays untouched",
        onNavigateBack = onNavigateBack,
    ) {
        if (busy) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())

        Button(
            onClick = { picker.launch(arrayOf("video/*", "audio/*")) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (uri == null) "Choose media file" else "Choose another file")
        }
        if (fileName.isNotBlank()) Text(fileName, color = MaterialTheme.colorScheme.onSurfaceVariant)

        UtilityCard {
            MetadataField("Title", title) { title = it }
            MetadataField("Artist", artist) { artist = it }
            MetadataField("Album", album) { album = it }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = year,
                    onValueChange = { year = it },
                    modifier = Modifier.weight(1f),
                    label = { Text("Year") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = genre,
                    onValueChange = { genre = it },
                    modifier = Modifier.weight(1f),
                    label = { Text("Genre") },
                    singleLine = true,
                )
            }
            Text("Cover artwork", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { artworkPicker.launch(arrayOf("image/*")) },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(if (artworkUri == null) "Choose image" else "Replace image")
                }
                if (artworkUri != null) {
                    OutlinedButton(
                        onClick = {
                            artworkUri = null
                            artworkName = ""
                        },
                    ) {
                        Text("Clear")
                    }
                }
            }
            if (artworkName.isNotBlank()) {
                Text(
                    artworkName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "Artwork replacement is supported for common audio containers.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Remove existing metadata")
                    Text(
                        "Clears old tags before applying the fields above.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = removeExisting, onCheckedChange = { removeExisting = it })
            }

            Button(
                onClick = {
                    val input = uri ?: return@Button
                    scope.launch {
                        busy = true
                        status = "Writing metadata…"
                        runCatching {
                            KirinUtilityEngine.editMetadata(
                                context,
                                input,
                                MetadataEdits(title, artist, album, year, genre, removeExisting),
                                artworkUri = artworkUri,
                            )
                        }.onSuccess {
                            status = "Saved edited copy to $it"
                        }.onFailure {
                            status = it.message ?: "Metadata edit failed."
                        }
                        busy = false
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = uri != null && !busy,
            ) {
                Text("Save edited copy")
            }
        }

        if (status.isNotBlank()) Text(status, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun MetadataField(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        singleLine = true,
    )
}

@Composable
fun ChapterClipMakerPage(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var uri by remember { mutableStateOf<Uri?>(null) }
    var chapters by remember { mutableStateOf<List<ChapterMark>>(emptyList()) }
    var start by remember { mutableStateOf("0") }
    var end by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("") }

    val picker =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { picked: Uri? ->
            if (picked != null) {
                uri = picked
                chapters = emptyList()
                scope.launch {
                    busy = true
                    status = "Reading chapters…"
                    runCatching { KirinUtilityEngine.readChapters(context, picked) }
                        .onSuccess {
                            chapters = it
                            status =
                                if (it.isEmpty()) {
                                    "No embedded chapters found. You can still enter start/end manually."
                                } else {
                                    "Found ${it.size} chapters."
                                }
                        }
                        .onFailure {
                            status = "Could not read chapters. Manual clipping is still available."
                        }
                    busy = false
                }
            }
        }

    UtilityToolPage(
        title = "Chapter / Clip Maker",
        subtitle = "Split a chapter or cut a local media section quickly",
        onNavigateBack = onNavigateBack,
    ) {
        if (busy) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())

        Button(
            onClick = { picker.launch(arrayOf("video/*", "audio/*")) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (uri == null) "Choose media file" else "Choose another file")
        }

        if (chapters.isNotEmpty()) {
            UtilityCard {
                Text("Embedded chapters", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                chapters.forEach { chapter ->
                    Surface(
                        modifier =
                            Modifier.fillMaxWidth()
                                .clickable {
                                    start = secondsText(chapter.startSeconds)
                                    end = secondsText(chapter.endSeconds)
                                },
                        shape = MaterialTheme.shapes.medium,
                        tonalElevation = 2.dp,
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(chapter.title, fontWeight = FontWeight.Medium)
                            Text(
                                "${secondsText(chapter.startSeconds)}s → ${secondsText(chapter.endSeconds)}s",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                Button(
                    onClick = {
                        val input = uri ?: return@Button
                        scope.launch {
                            busy = true
                            status = "Splitting ${chapters.size} chapters…"
                            runCatching {
                                KirinUtilityEngine.splitChapters(context, input, chapters)
                            }.onSuccess {
                                status = "Created ${it.size} chapter clips in Downloads/KirinDL/Clips."
                            }.onFailure {
                                status = it.message ?: "Chapter split failed."
                            }
                            busy = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = uri != null && chapters.isNotEmpty() && !busy,
                ) {
                    Text("Split all chapters")
                }
            }
        }

        UtilityCard {
            Text("Clip range", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = start,
                    onValueChange = { start = it },
                    modifier = Modifier.weight(1f),
                    label = { Text("Start (seconds)") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = end,
                    onValueChange = { end = it },
                    modifier = Modifier.weight(1f),
                    label = { Text("End (seconds)") },
                    singleLine = true,
                )
            }
            Text(
                "Uses FFmpeg stream-copy, so clipping is fast and does not re-encode the media.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Button(
                onClick = {
                    val input = uri ?: return@Button
                    val startValue = start.toDoubleOrNull() ?: return@Button
                    val endValue = end.toDoubleOrNull() ?: return@Button
                    scope.launch {
                        busy = true
                        status = "Creating clip…"
                        runCatching {
                            KirinUtilityEngine.makeClip(context, input, startValue, endValue)
                        }.onSuccess {
                            status = "Saved clip to $it"
                        }.onFailure {
                            status = it.message ?: "Clip creation failed."
                        }
                        busy = false
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled =
                    uri != null &&
                        start.toDoubleOrNull() != null &&
                        end.toDoubleOrNull() != null &&
                        (end.toDoubleOrNull() ?: 0.0) > (start.toDoubleOrNull() ?: 0.0) &&
                        !busy,
            ) {
                Text("Create clip")
            }
        }

        if (status.isNotBlank()) Text(status, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun buildPlaylistExport(
    format: String,
    entries: List<com.junkfood.seal.util.PlaylistEntry>,
): String {
    val items =
        entries.mapNotNull { entry ->
            KirinUtilityEngine.playlistEntryUrl(entry)?.let { url ->
                (entry.title ?: entry.id ?: "Untitled") to url
            }
        }
    return when (format.lowercase(Locale.US)) {
        "json" ->
            items.joinToString(
                prefix = "[\n",
                postfix = "\n]",
                separator = ",\n",
            ) { (title, url) ->
                "  {\"title\":\"${escapeJson(title)}\",\"url\":\"${escapeJson(url)}\"}"
            }
        "m3u" ->
            buildString {
                appendLine("#EXTM3U")
                items.forEach { (title, url) ->
                    appendLine("#EXTINF:-1,$title")
                    appendLine(url)
                }
            }
        else -> items.joinToString("\n") { it.second } + "\n"
    }
}

private fun escapeJson(value: String): String =
    value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")

private fun formatBytes(value: Long): String {
    if (value < 1024) return "$value B"
    val kb = value / 1024.0
    if (kb < 1024) return String.format(Locale.US, "%.1f KB", kb)
    val mb = kb / 1024.0
    if (mb < 1024) return String.format(Locale.US, "%.1f MB", mb)
    return String.format(Locale.US, "%.2f GB", mb / 1024.0)
}

private fun formatDuration(ms: Long): String {
    val total = ms / 1000
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) String.format(Locale.US, "%d:%02d:%02d", h, m, s)
    else String.format(Locale.US, "%d:%02d", m, s)
}

private fun secondsText(value: Double): String =
    String.format(Locale.US, "%.3f", value).trimEnd('0').trimEnd('.')
