package com.junkfood.seal.ui.page.downloadv2

import android.net.Uri
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Replay
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import com.junkfood.seal.database.objects.DownloadedVideoInfo
import com.junkfood.seal.download.DownloaderV2
import com.junkfood.seal.download.Task
import com.junkfood.seal.ui.component.BackButton
import com.junkfood.seal.util.DatabaseUtil
import com.junkfood.seal.util.FileUtil
import com.junkfood.seal.util.GalleryDlStore
import com.junkfood.seal.util.makeToast
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

private enum class CenterEngine(val label: String) {
    All("All"),
    Media("Media"),
    Gallery("Gallery"),
}

private enum class CenterStatus(val label: String) {
    All("All"),
    Active("Active"),
    Queue("Queue"),
    Paused("Paused"),
    Failed("Failed"),
    Done("Completed"),
}

private enum class CenterSort(val label: String) {
    ActiveFirst("Active first"),
    Newest("Newest"),
    Oldest("Oldest"),
}

private enum class CenterKind {
    MediaLive,
    MediaHistory,
    GalleryQueue,
    GalleryHistory,
}

private data class CenterRecord(
    val id: String,
    val kind: CenterKind,
    val title: String,
    val subtitle: String,
    val url: String,
    val status: CenterStatus,
    val timestamp: Long,
    val badge: String,
    val task: Task? = null,
    val taskState: Task.State? = null,
    val mediaHistory: DownloadedVideoInfo? = null,
    val galleryFileCount: Int = 0,
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun UnifiedDownloadCenterPage(
    onNavigateBack: () -> Unit,
    onOpenMediaQueue: () -> Unit,
    onOpenMediaHistory: () -> Unit,
    onOpenGallery: () -> Unit,
    downloader: DownloaderV2 = koinInject(),
) {
    val context = LocalContext.current
    val taskMap = downloader.getTaskStateMap()
    val scope = rememberCoroutineScope()
    var refreshKey by remember { mutableIntStateOf(0) }
    var mediaHistory by remember { mutableStateOf(emptyList<DownloadedVideoInfo>()) }
    var engine by rememberSaveable { mutableStateOf(CenterEngine.All) }
    var status by rememberSaveable { mutableStateOf(CenterStatus.All) }
    var sort by rememberSaveable { mutableStateOf(CenterSort.ActiveFirst) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var showClearCompletedDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        DatabaseUtil.getVisibleDownloadHistoryFlow().collect { mediaHistory = it }
    }

    val galleryQueue = remember(refreshKey) { GalleryDlStore.loadQueue(context) }
    val galleryHistory = remember(refreshKey) { GalleryDlStore.loadHistory(context) }

    val liveRecords by remember {
        derivedStateOf {
            taskMap.mapNotNull { (task, state) ->
                val mappedStatus = centerStatus(state.downloadState)
                if (mappedStatus == CenterStatus.Done) return@mapNotNull null
                CenterRecord(
                    id = "media-live:${task.id}",
                    kind = CenterKind.MediaLive,
                    title = state.viewState.title.ifBlank { task.url },
                    subtitle =
                        listOf(state.viewState.uploader, state.viewState.extractorKey)
                            .filter { it.isNotBlank() }
                            .joinToString(" • "),
                    url = task.url,
                    status = mappedStatus,
                    timestamp = task.timeCreated,
                    badge = mediaBadge(state.viewState.extractorKey, task.url),
                    task = task,
                    taskState = state,
                )
            }
        }
    }

    val completedMedia =
        remember(mediaHistory) {
            mediaHistory.take(100).map { info ->
                CenterRecord(
                    id = "media-history:${info.id}",
                    kind = CenterKind.MediaHistory,
                    title = info.videoTitle,
                    subtitle =
                        listOf(info.videoAuthor, info.extractor)
                            .filter { it.isNotBlank() }
                            .joinToString(" • "),
                    url = info.videoUrl,
                    status = CenterStatus.Done,
                    timestamp = if (info.downloadTimeMillis > 0) info.downloadTimeMillis else info.id.toLong(),
                    badge = mediaBadge(info.extractor, info.videoUrl),
                    mediaHistory = info,
                )
            }
        }

    val galleryQueueRecords =
        remember(galleryQueue) {
            galleryQueue.map { record ->
                val recordStatus =
                    when (record.state.lowercase()) {
                        "running" -> CenterStatus.Active
                        "paused" -> CenterStatus.Paused
                        "failed" -> CenterStatus.Failed
                        "completed" -> CenterStatus.Done
                        else -> CenterStatus.Queue
                    }
                CenterRecord(
                    id = "gallery-queue:${record.id}",
                    kind = CenterKind.GalleryQueue,
                    title = galleryTitle(record.url),
                    subtitle = record.extractor.ifBlank { record.error },
                    url = record.url,
                    status = recordStatus,
                    timestamp = 0L,
                    badge = "Gallery",
                )
            }
        }

    val galleryHistoryRecords =
        remember(galleryHistory) {
            galleryHistory.take(100).map { record ->
                CenterRecord(
                    id = "gallery-history:${record.id}",
                    kind = CenterKind.GalleryHistory,
                    title = galleryTitle(record.url),
                    subtitle =
                        buildString {
                            append(record.extractor.ifBlank { "Gallery DL" })
                            if (record.fileCount > 0) append(" • ${record.fileCount} files")
                            if (!record.success && record.error.isNotBlank()) append(" • ${record.error}")
                        },
                    url = record.url,
                    status = if (record.success) CenterStatus.Done else CenterStatus.Failed,
                    timestamp = record.finishedAt,
                    badge = "Gallery",
                    galleryFileCount = record.fileCount,
                )
            }
        }

    val allRecords = liveRecords + completedMedia
    val filteredRecords =
        remember(allRecords, engine, status, sort, searchQuery) {
            val q = searchQuery.trim()
            allRecords
                .asSequence()
                .filter { record ->
                    when (engine) {
                        CenterEngine.All -> true
                        CenterEngine.Media -> record.kind == CenterKind.MediaLive || record.kind == CenterKind.MediaHistory
                        CenterEngine.Gallery -> record.kind == CenterKind.GalleryQueue || record.kind == CenterKind.GalleryHistory
                    }
                }
                .filter { record -> status == CenterStatus.All || record.status == status }
                .filter { record ->
                    q.isBlank() ||
                        record.title.contains(q, ignoreCase = true) ||
                        record.subtitle.contains(q, ignoreCase = true) ||
                        record.url.contains(q, ignoreCase = true) ||
                        record.badge.contains(q, ignoreCase = true)
                }
                .toList()
                .let { records ->
                    when (sort) {
                        CenterSort.Newest -> records.sortedByDescending { it.timestamp }
                        CenterSort.Oldest -> records.sortedBy { it.timestamp }
                        CenterSort.ActiveFirst ->
                            records.sortedWith(
                                compareBy<CenterRecord> { statusPriority(it.status) }
                                    .thenByDescending { it.timestamp }
                            )
                    }
                }
        }

    val activeCount = allRecords.count { it.status == CenterStatus.Active }
    val queueCount = allRecords.count { it.status == CenterStatus.Queue || it.status == CenterStatus.Paused }
    val failedCount = allRecords.count { it.status == CenterStatus.Failed }
    val doneCount = allRecords.count { it.status == CenterStatus.Done }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Download Center") },
                navigationIcon = { BackButton(onNavigateBack) },
                actions = {
                    IconButton(onClick = { refreshKey++ }) {
                        Icon(Icons.Outlined.Refresh, contentDescription = "Refresh")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentAlignment = Alignment.TopCenter,
        ) {
            LazyColumn(
                modifier = Modifier.widthIn(max = 920.dp).fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
            item {
                CenterMetrics(
                    active = activeCount,
                    queued = queueCount,
                    failed = failedCount,
                    done = doneCount,
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(onClick = onOpenMediaQueue) {
                        Icon(Icons.Outlined.FileDownload, null, Modifier.size(18.dp))
                        Text(" Media Queue")
                    }
                    OutlinedButton(onClick = onOpenMediaHistory) {
                        Icon(Icons.Outlined.History, null, Modifier.size(18.dp))
                        Text(" Media History")
                    }
                }
            }

            if (failedCount > 0 || doneCount > 0) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (failedCount > 0) {
                            OutlinedButton(
                                onClick = {
                                    val failedMedia =
                                        taskMap
                                            .filterValues { state ->
                                                state.downloadState is Task.DownloadState.Error ||
                                                    state.downloadState is Task.DownloadState.Canceled
                                            }
                                            .keys
                                            .toList()
                                    failedMedia.forEach(downloader::restart)
                                    if (failedMedia.isEmpty()) {
                                        context.makeToast("No failed Media tasks to retry")
                                    } else {
                                        context.makeToast("Retrying ${failedMedia.size} Media task${if (failedMedia.size == 1) "" else "s"}")
                                    }
                                },
                            ) {
                                Icon(Icons.Outlined.Replay, null, Modifier.size(18.dp))
                                Text(" Retry Failed")
                            }
                        }
                        if (doneCount > 0) {
                            OutlinedButton(onClick = { showClearCompletedDialog = true }) {
                                Icon(Icons.Outlined.Clear, null, Modifier.size(18.dp))
                                Text(" Clear Completed")
                            }
                        }
                    }
                }
            }

            item {
                CenterFilterPanel(
                    engine = engine,
                    onEngineChange = { engine = it },
                    status = status,
                    onStatusChange = { status = it },
                    sort = sort,
                    onSortChange = { sort = it },
                    searchQuery = searchQuery,
                    onSearchQueryChange = { searchQuery = it },
                )
            }

            if (filteredRecords.isEmpty()) {
                item {
                    Surface(shape = MaterialTheme.shapes.large, tonalElevation = 1.dp) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                if (searchQuery.isNotBlank() || status != CenterStatus.All || engine != CenterEngine.All)
                                    "No matching activity"
                                else
                                    "No download activity yet",
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                if (searchQuery.isNotBlank() || status != CenterStatus.All || engine != CenterEngine.All)
                                    "Try clearing Search or changing the current filters."
                                else
                                    "New Media activity will appear here.",
                                modifier = Modifier.padding(top = 6.dp),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            } else {
                items(filteredRecords, key = { it.id }) { record ->
                    CenterRecordCard(
                        record = record,
                        onOpenMediaQueue = onOpenMediaQueue,
                        onOpenGallery = onOpenGallery,
                        onMediaAction = { action ->
                            record.task?.let { task ->
                                when (action) {
                                    "pause" -> downloader.pause(task)
                                    "resume" -> downloader.resume(task)
                                    "retry" -> downloader.restart(task)
                                }
                            }
                        },
                        onOpenMediaFile = {
                            record.mediaHistory?.let { info ->
                                FileUtil.openFile(info.videoPath) { context.makeToast("File unavailable") }
                            }
                        },
                    )
                }
            }

                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }

    if (showClearCompletedDialog) {
        AlertDialog(
            onDismissRequest = { showClearCompletedDialog = false },
            title = { Text("Clear completed history?") },
            text = {
                Text(
                    "This removes completed Media history and Gallery DL history from the Download Center. Downloaded files are kept.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearCompletedDialog = false
                        scope.launch {
                            DatabaseUtil.deleteInfoList(mediaHistory, deleteFile = false)
                            GalleryDlStore.clearHistory(context)
                            refreshKey += 1
                            context.makeToast("Completed history cleared")
                        }
                    },
                ) {
                    Text("Clear")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearCompletedDialog = false }) { Text("Cancel") }
            },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CenterMetrics(active: Int, queued: Int, failed: Int, done: Int) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val compact = maxWidth < 600.dp
        val cardWidth = if (compact) (maxWidth - 8.dp) / 2 else (maxWidth - 24.dp) / 4

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            maxItemsInEachRow = if (compact) 2 else 4,
        ) {
            CenterMetric(
                label = "Active",
                value = active,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.widthIn(min = cardWidth, max = cardWidth),
            )
            CenterMetric(
                label = "Queue",
                value = queued,
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.widthIn(min = cardWidth, max = cardWidth),
            )
            CenterMetric(
                label = "Failed",
                value = failed,
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.widthIn(min = cardWidth, max = cardWidth),
            )
            CenterMetric(
                label = "Done",
                value = done,
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.widthIn(min = cardWidth, max = cardWidth),
            )
        }
    }
}

@Composable
private fun CenterMetric(
    label: String,
    value: Int,
    containerColor: androidx.compose.ui.graphics.Color,
    contentColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = containerColor,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                value.toString(),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = contentColor,
            )
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = contentColor.copy(alpha = 0.85f),
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun CenterFilterPanel(
    engine: CenterEngine,
    onEngineChange: (CenterEngine) -> Unit,
    status: CenterStatus,
    onStatusChange: (CenterStatus) -> Unit,
    sort: CenterSort,
    onSortChange: (CenterSort) -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
) {
    val selectedChipColors =
        FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primary,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
            selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary,
        )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "Options",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
            )
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CenterEngine.entries.filter { it != CenterEngine.Gallery }.forEach { tab ->
                    FilterChip(
                        selected = engine == tab,
                        onClick = { onEngineChange(tab) },
                        label = { Text(tab.label) },
                        colors = selectedChipColors,
                    )
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f))
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Outlined.Search, null, tint = MaterialTheme.colorScheme.primary) },
                trailingIcon =
                    if (searchQuery.isNotBlank()) {
                        {
                            IconButton(onClick = { onSearchQueryChange("") }) {
                                Icon(Icons.Outlined.Clear, contentDescription = "Clear search")
                            }
                        }
                    } else null,
                placeholder = { Text("Search title, site, creator or URL") },
                shape = MaterialTheme.shapes.large,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    focusedLeadingIconColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                ),
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f))
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CenterStatus.entries.forEach { filter ->
                    FilterChip(
                        selected = status == filter,
                        onClick = { onStatusChange(filter) },
                        label = { Text(filter.label) },
                        colors = selectedChipColors,
                    )
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Sort", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CenterSort.entries.forEach { option ->
                        FilterChip(
                            selected = sort == option,
                            onClick = { onSortChange(option) },
                            label = { Text(option.label) },
                            colors = selectedChipColors,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CenterRecordCard(
    record: CenterRecord,
    onOpenMediaQueue: () -> Unit,
    onOpenGallery: () -> Unit,
    onMediaAction: (String) -> Unit,
    onOpenMediaFile: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        onClick = {
            when (record.kind) {
                CenterKind.MediaLive -> onOpenMediaQueue()
                CenterKind.MediaHistory -> onOpenMediaFile()
                CenterKind.GalleryQueue,
                CenterKind.GalleryHistory -> onOpenGallery()
            }
        },
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CenterBadge(record.badge)
                CenterStatusBadge(record.status, Modifier.padding(start = 8.dp))
                Spacer(Modifier.weight(1f))
                if (record.kind == CenterKind.GalleryHistory && record.galleryFileCount > 0) {
                    Text("${record.galleryFileCount} files", style = MaterialTheme.typography.labelSmall)
                }
            }
            Text(
                record.title,
                modifier = Modifier.padding(top = 8.dp),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.SemiBold,
            )
            if (record.subtitle.isNotBlank()) {
                Text(
                    record.subtitle,
                    modifier = Modifier.padding(top = 3.dp),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                record.url,
                modifier = Modifier.padding(top = 3.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (record.kind == CenterKind.MediaLive) {
                val downloadState = record.taskState?.downloadState
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                    horizontalArrangement = Arrangement.End,
                ) {
                    when (downloadState) {
                        is Task.DownloadState.Running ->
                            TextButton(onClick = { onMediaAction("pause") }) {
                                Icon(Icons.Outlined.Pause, null, Modifier.size(18.dp))
                                Text(" Pause")
                            }
                        is Task.DownloadState.Paused ->
                            TextButton(onClick = { onMediaAction("resume") }) {
                                Icon(Icons.Outlined.PlayArrow, null, Modifier.size(18.dp))
                                Text(" Resume")
                            }
                        is Task.DownloadState.Error,
                        is Task.DownloadState.Canceled ->
                            TextButton(onClick = { onMediaAction("retry") }) {
                                Icon(Icons.Outlined.Replay, null, Modifier.size(18.dp))
                                Text(" Retry")
                            }
                        else -> Unit
                    }
                    TextButton(onClick = onOpenMediaQueue) { Text("Open Queue") }
                }
            }
        }
    }
}

@Composable
private fun CenterBadge(text: String) {
    Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.primaryContainer) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}

@Composable
private fun CenterStatusBadge(status: CenterStatus, modifier: Modifier = Modifier) {
    Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.secondaryContainer, modifier = modifier) {
        Text(
            status.label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
        )
    }
}

private fun centerStatus(state: Task.DownloadState): CenterStatus =
    when (state) {
        is Task.DownloadState.FetchingInfo,
        is Task.DownloadState.Running -> CenterStatus.Active
        Task.DownloadState.Idle,
        Task.DownloadState.ReadyWithInfo -> CenterStatus.Queue
        is Task.DownloadState.Paused -> CenterStatus.Paused
        is Task.DownloadState.Error,
        is Task.DownloadState.Canceled -> CenterStatus.Failed
        is Task.DownloadState.Completed -> CenterStatus.Done
    }

private fun statusPriority(status: CenterStatus): Int =
    when (status) {
        CenterStatus.Active -> 0
        CenterStatus.Queue -> 1
        CenterStatus.Paused -> 2
        CenterStatus.Failed -> 3
        CenterStatus.Done -> 4
        CenterStatus.All -> 5
    }

private fun galleryTitle(url: String): String {
    val host = runCatching { Uri.parse(url).host }.getOrNull()?.removePrefix("www.")
    return host?.let { "Gallery • $it" } ?: "Gallery download"
}

private fun mediaBadge(extractor: String, url: String): String {
    val lower = (extractor + " " + url).lowercase()
    return when {
        "bilibili" in lower || "b23.tv" in lower -> "Bilibili"
        "music.youtube" in lower -> "YT Music"
        "youtube" in lower || "youtu.be" in lower -> "YT"
        else -> "Media"
    }
}
