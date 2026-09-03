package com.junkfood.seal.ui.page.tools

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.ClearAll
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Queue
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.junkfood.seal.ui.component.BackButton
import com.junkfood.seal.ui.component.SealModalBottomSheet
import com.junkfood.seal.util.GalleryDlBehaviorPreference
import com.junkfood.seal.util.GalleryDlStore
import com.junkfood.seal.util.GalleryDlRunner
import com.junkfood.seal.util.GalleryDlThemePreference
import com.junkfood.seal.util.GalleryDlThemeStyle
import java.text.DateFormat
import java.util.Date
import org.koin.androidx.compose.koinViewModel

private data class KirinGalleryColors(
    val background: Color,
    val panel: Color,
    val panelAlt: Color,
    val accent: Color,
    val accentSoft: Color,
    val onAccent: Color,
    val text: Color,
    val muted: Color,
    val success: Color,
    val error: Color,
)

private enum class GalleryConfirmAction {
    DOWNLOAD,
    QUEUE,
}

@Composable
private fun kirinGalleryColors(style: GalleryDlThemeStyle): KirinGalleryColors {
    val scheme = MaterialTheme.colorScheme

    val accent =
        when (style) {
            GalleryDlThemeStyle.APP_DEFAULT -> scheme.primary
            GalleryDlThemeStyle.KIRIN_CYAN -> Color(0xFF18BFEA)
            GalleryDlThemeStyle.OCEAN -> Color(0xFF4B8DFF)
            GalleryDlThemeStyle.EMERALD -> Color(0xFF2DBF85)
            GalleryDlThemeStyle.VIOLET -> Color(0xFF8B7CFF)
        }

    val onAccent =
        if (style == GalleryDlThemeStyle.APP_DEFAULT) {
            scheme.onPrimary
        } else if (accent.luminance() > 0.179f) {
            Color(0xFF071116)
        } else {
            Color.White
        }

    return KirinGalleryColors(
        // IMPORTANT: these always follow the real app MaterialTheme, not Android's system setting.
        background = scheme.background,
        panel = scheme.surface,
        panelAlt = scheme.surfaceVariant,
        accent = accent,
        accentSoft =
            if (style == GalleryDlThemeStyle.APP_DEFAULT) {
                scheme.primaryContainer
            } else {
                accent.copy(alpha = 0.14f)
            },
        onAccent = onAccent,
        text = scheme.onSurface,
        muted = scheme.onSurfaceVariant,
        success = Color(0xFF2CA879),
        error = scheme.error,
    )
}

@Composable
fun GalleryDlPage(
    onNavigateBack: () -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: GalleryDlViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val themeStyle by GalleryDlThemePreference.style.collectAsStateWithLifecycle()
    val confirmBeforeDownload by GalleryDlBehaviorPreference.confirmBeforeDownload.collectAsStateWithLifecycle()
    val exportFilter by GalleryDlBehaviorPreference.exportFilter.collectAsStateWithLifecycle()
    val colors = kirinGalleryColors(themeStyle)
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    val outputRoot = remember {
        runCatching { GalleryDlRunner.galleryRootDirectory(context).absolutePath }
            .getOrDefault("Download/GalleryDL/")
    }
    var tab by remember { mutableIntStateOf(GalleryDlBehaviorPreference.lastTab()) }
    var siteFilterRevision by remember { mutableIntStateOf(0) }
    var showBatch by remember { mutableStateOf(false) }
    var pendingAction by remember { mutableStateOf<GalleryConfirmAction?>(null) }
    var pendingBatchText by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        viewModel.refreshFromDisk()
        GalleryDlBehaviorPreference.consumePendingHomeUrl()?.let { pendingUrl ->
            viewModel.updateUrl(pendingUrl)
            viewModel.checkExtractor()
        }
    }

    val siteExportFilter =
        remember(state.url, exportFilter, siteFilterRevision) {
            GalleryDlBehaviorPreference.siteExportFilter(state.url)
        }
    val effectiveExportFilter = siteExportFilter ?: exportFilter
    val gallerySiteLabel =
        remember(state.url, siteFilterRevision) {
            GalleryDlBehaviorPreference.siteLabel(state.url)
        }

    Scaffold(
        containerColor = colors.background,
        topBar = {
            Row(
                modifier =
                    Modifier.fillMaxWidth()
                        .background(colors.background)
                        .statusBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BackButton(onNavigateBack)
                Spacer(Modifier.width(4.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "KIRIN GALLERY",
                        color = colors.text,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.2.sp,
                    )
                    Text(
                        state.installedVersion?.let { "Codeberg • $it" }
                            ?: "Engine setup required in Settings",
                        color = if (state.isInstalled) colors.accent else colors.muted,
                        fontSize = 11.sp,
                    )
                }
                IconButton(onClick = onOpenSettings) {
                    Icon(
                        Icons.Outlined.Settings,
                        contentDescription = "Gallery DL Settings",
                        tint = colors.accent,
                    )
                }
            }
        },
    ) { padding ->
        Column(
            modifier =
                Modifier.fillMaxSize()
                    .padding(padding)
                    .imePadding()
                    .verticalScroll(rememberScrollState()),
        ) {
            KirinTabs(
                selected = tab,
                queueCount = state.queue.count { it.state == "pending" || it.state == "running" },
                historyCount = state.history.size,
                colors = colors,
                onSelected = {
                    tab = it
                    GalleryDlBehaviorPreference.setLastTab(it)
                },
            )

            when (tab) {
                0 ->
                    DownloadTab(
                        state = state,
                        colors = colors,
                        clipboardText = { clipboard.getText()?.text.orEmpty() },
                        onUrlChanged = viewModel::updateUrl,
                        onCheck = viewModel::checkExtractor,
                        onDownload = {
                            if (confirmBeforeDownload) {
                                pendingAction = GalleryConfirmAction.DOWNLOAD
                                viewModel.checkExtractor()
                            } else {
                                viewModel.download()
                            }
                        },
                        onQueue = {
                            if (confirmBeforeDownload) {
                                pendingAction = GalleryConfirmAction.QUEUE
                                viewModel.checkExtractor()
                            } else {
                                viewModel.addCurrentToQueue()
                                tab = 1
                                GalleryDlBehaviorPreference.setLastTab(1)
                            }
                        },
                        onBatch = { showBatch = true },
                        exportFilter = effectiveExportFilter,
                        siteLabel = gallerySiteLabel,
                        siteFilterSaved = siteExportFilter != null,
                        onRememberSiteFilter = {
                            GalleryDlBehaviorPreference.rememberSiteExportFilter(
                                state.url,
                                exportFilter,
                            )
                            siteFilterRevision++
                        },
                        onClearSiteFilter = {
                            GalleryDlBehaviorPreference.clearSiteExportFilter(state.url)
                            siteFilterRevision++
                        },
                    )
                1 ->
                    QueueTab(
                        state = state,
                        colors = colors,
                        onRun = viewModel::runQueue,
                        onRemove = viewModel::removeQueueItem,
                        onRetryFailed = viewModel::retryFailedQueue,
                        onClearFailed = viewModel::clearFailedQueue,
                        onClearCompleted = viewModel::clearCompletedQueue,
                        onMove = viewModel::moveQueueItem,
                    )
                else ->
                    HistoryTab(
                        state = state,
                        colors = colors,
                        onReuse = {
                            viewModel.reuseHistoryUrl(it)
                            tab = 0
                            GalleryDlBehaviorPreference.setLastTab(0)
                        },
                        onClear = viewModel::clearHistory,
                        onClearSuccessful = viewModel::clearSuccessfulHistory,
                        onClearFailed = viewModel::clearFailedHistory,
                    )
            }

            state.statusMessage?.let { Notice(it, colors.success, colors) }
            state.errorMessage?.let { Notice(it, colors.error, colors) }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (showBatch) {
        BatchDialog(
            colors = colors,
            onDismiss = { showBatch = false },
            onAdd = {
                showBatch = false
                if (confirmBeforeDownload) {
                    pendingBatchText = it
                } else {
                    viewModel.addBatch(it)
                    tab = 1
                    GalleryDlBehaviorPreference.setLastTab(1)
                }
            },
        )
    }

    pendingAction?.let { action ->
        GalleryDownloadConfirmDialog(
            state = state,
            colors = colors,
            outputRoot = outputRoot,
            action = action,
            onDismiss = { pendingAction = null },
            onConfirm = {
                when (action) {
                    GalleryConfirmAction.DOWNLOAD -> viewModel.download()
                    GalleryConfirmAction.QUEUE -> {
                        viewModel.addCurrentToQueue()
                        tab = 1
                        GalleryDlBehaviorPreference.setLastTab(1)
                    }
                }
                pendingAction = null
            },
        )
    }

    pendingBatchText?.let { batchText ->
        GalleryBatchConfirmDialog(
            text = batchText,
            colors = colors,
            outputRoot = outputRoot,
            onDismiss = { pendingBatchText = null },
            onConfirm = {
                viewModel.addBatch(batchText)
                pendingBatchText = null
                tab = 1
                GalleryDlBehaviorPreference.setLastTab(1)
            },
        )
    }
}

@Composable
private fun KirinTabs(
    selected: Int,
    queueCount: Int,
    historyCount: Int,
    colors: KirinGalleryColors,
    onSelected: (Int) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().background(colors.panel).padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        val tabs =
            listOf(
                Triple("Download", Icons.Outlined.Download, ""),
                Triple("Queue", Icons.Outlined.Queue, if (queueCount > 0) " $queueCount" else ""),
                Triple("History", Icons.Outlined.History, if (historyCount > 0) " $historyCount" else ""),
            )
        tabs.forEachIndexed { index, item ->
            val active = selected == index
            Row(
                modifier =
                    Modifier.weight(1f)
                        .background(
                            if (active) colors.accentSoft else Color.Transparent,
                            RoundedCornerShape(10.dp),
                        )
                        .clickable { onSelected(index) }
                        .padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    item.second,
                    contentDescription = null,
                    tint = if (active) colors.accent else colors.muted,
                    modifier = Modifier.size(17.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    item.first + item.third,
                    color = if (active) colors.text else colors.muted,
                    fontSize = 12.sp,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                )
            }
        }
    }
}

@Composable
private fun DownloadTab(
    state: GalleryDlViewModel.ViewState,
    colors: KirinGalleryColors,
    clipboardText: () -> String,
    onUrlChanged: (String) -> Unit,
    onCheck: () -> Unit,
    onDownload: () -> Unit,
    onQueue: () -> Unit,
    onBatch: () -> Unit,
    exportFilter: Int,
    siteLabel: String?,
    siteFilterSaved: Boolean,
    onRememberSiteFilter: () -> Unit,
    onClearSiteFilter: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            "Download a gallery",
            color = colors.text,
            fontSize = 24.sp,
            fontWeight = FontWeight.Black,
        )
        Text(
            "Paste one URL, analyze its extractor, or add several URLs into the queue.",
            color = colors.muted,
            fontSize = 13.sp,
        )

        GalleryDashboard(state = state, colors = colors)

        Box(
            modifier =
                Modifier.fillMaxWidth()
                    .background(colors.panel, RoundedCornerShape(16.dp))
                    .padding(14.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = state.url,
                    onValueChange = onUrlChanged,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isBusy,
                    singleLine = true,
                    textStyle =
                        MaterialTheme.typography.bodyLarge.copy(
                            color = colors.text,
                        ),
                    placeholder = {
                        Text(
                            "Gallery or collection URL",
                            color = colors.muted,
                        )
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Outlined.Link,
                            contentDescription = null,
                            tint = colors.accent,
                        )
                    },
                    trailingIcon =
                        if (state.url.isNotBlank() && !state.isBusy) {
                            {
                                IconButton(onClick = { onUrlChanged("") }) {
                                    Icon(
                                        Icons.Outlined.Clear,
                                        contentDescription = "Clear URL",
                                        tint = colors.muted,
                                    )
                                }
                            }
                        } else {
                            null
                        },
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(
                        onClick = {
                            clipboardText().trim().takeIf(String::isNotBlank)?.let(onUrlChanged)
                        },
                        enabled = !state.isBusy,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Paste")
                    }
                    OutlinedButton(
                        onClick = onCheck,
                        enabled =
                            state.isInstalled &&
                                !state.isBusy &&
                                com.junkfood.seal.util.GalleryDlRunner.isCandidateUrl(state.url),
                        modifier = Modifier.weight(1f),
                    ) {
                        if (state.isCheckingExtractor) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Icon(Icons.Outlined.Check, contentDescription = null)
                        }
                        Spacer(Modifier.width(6.dp))
                        Text("Analyze")
                    }
                }
            }
        }

        when (state.extractorSupported) {
            true ->
                FlatInfoRow(
                    "Extractor",
                    state.extractorLabel ?: "Ready",
                    colors.success,
                    colors,
                )
            false -> FlatInfoRow("Extractor", "No match", colors.error, colors)
            null -> Unit
        }

        FlatInfoRow(
            "Export filter",
            GalleryDlBehaviorPreference.exportFilterLabel(exportFilter),
            colors.accent,
            colors,
        )
        siteLabel?.let { label ->
            GallerySiteFilterMemory(
                siteLabel = label,
                filterLabel = GalleryDlBehaviorPreference.exportFilterLabel(exportFilter),
                saved = siteFilterSaved,
                onRemember = onRememberSiteFilter,
                onClear = onClearSiteFilter,
                colors = colors,
            )
        }
        if (state.preflightInfo != null || state.extractorSupported != null) {
            GalleryPreflightDiagnostic(state, colors)
        }

        Button(
            onClick = onDownload,
            enabled = state.canDownload,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            colors =
                ButtonDefaults.buttonColors(
                    containerColor = colors.accent,
                    contentColor = colors.onAccent,
                ),
            shape = RoundedCornerShape(12.dp),
        ) {
            if (state.isDownloading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = colors.onAccent,
                )
                Spacer(Modifier.width(8.dp))
                Text("Downloading")
            } else {
                Icon(Icons.Outlined.Download, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Download Gallery", fontWeight = FontWeight.Bold)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(
                onClick = onQueue,
                enabled =
                    state.isInstalled &&
                        !state.isBusy &&
                        com.junkfood.seal.util.GalleryDlRunner.isCandidateUrl(state.url),
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.Outlined.Add, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Add to Queue")
            }
            OutlinedButton(
                onClick = onBatch,
                enabled = !state.isBusy,
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.Outlined.Queue, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Batch URLs")
            }
        }

        if (!state.isInstalled) {
            Notice(
                "Install or update the Gallery DL engine from Settings before downloading.",
                colors.accent,
                colors,
            )
        }

        state.destinationDirectory?.let {
            FlatInfoRow("Saved to", it, colors.text, colors)
        }
    }
}

@Composable
private fun QueueTab(
    state: GalleryDlViewModel.ViewState,
    colors: KirinGalleryColors,
    onRun: () -> Unit,
    onRemove: (String) -> Unit,
    onRetryFailed: () -> Unit,
    onClearFailed: () -> Unit,
    onClearCompleted: () -> Unit,
    onMove: (String, Int) -> Unit,
) {
    var searchQuery by rememberSaveable { mutableStateOf("") }
    val visibleQueue =
        remember(state.queue, searchQuery) {
            val query = searchQuery.trim()
            if (query.isBlank()) state.queue
            else
                state.queue.filter { item ->
                    item.url.contains(query, ignoreCase = true) ||
                        item.extractor.contains(query, ignoreCase = true) ||
                        item.state.contains(query, ignoreCase = true) ||
                        item.error.contains(query, ignoreCase = true)
                }
        }
    val pendingCount = state.queue.count { it.state == "pending" }
    val runningCount = state.queue.count { it.state == "running" }
    val failedCount = state.queue.count { it.state == "failed" }
    val completedCount = state.queue.count { it.state == "completed" }

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Download queue",
                    color = colors.text,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                )
                Text(
                    "${state.queue.size} item(s) • sequential jobs",
                    color = colors.muted,
                    fontSize = 12.sp,
                )
            }
        }

        GalleryStatsStrip(
            firstLabel = "Pending",
            firstValue = pendingCount,
            secondLabel = "Running",
            secondValue = runningCount,
            thirdLabel = "Failed",
            thirdValue = failedCount,
            fourthLabel = "Done",
            fourthValue = completedCount,
            colors = colors,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            TextButton(
                onClick = onRetryFailed,
                enabled = failedCount > 0 && !state.isQueueRunning,
                modifier = Modifier.weight(1f),
            ) {
                Text("Retry failed")
            }
            TextButton(
                onClick = onClearFailed,
                enabled = failedCount > 0 && !state.isQueueRunning,
                modifier = Modifier.weight(1f),
            ) {
                Text("Clear failed")
            }
            TextButton(
                onClick = onClearCompleted,
                enabled = completedCount > 0 && !state.isQueueRunning,
                modifier = Modifier.weight(1f),
            ) {
                Text("Clear done")
            }
        }
        if (state.queue.isNotEmpty()) {
            GallerySearchField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = "Search queue URL, extractor or status",
                colors = colors,
            )
        }

        if (state.queue.isEmpty()) {
            EmptyState(
                "Queue is empty",
                "Add a URL or use Batch URLs from Download.",
                colors,
            )
        } else {
            Button(
                onClick = onRun,
                enabled =
                    state.isInstalled &&
                        !state.isBusy &&
                        state.queue.any { it.state == "pending" || it.state == "failed" },
                modifier = Modifier.fillMaxWidth(),
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = colors.accent,
                        contentColor = colors.onAccent,
                    ),
            ) {
                if (state.isQueueRunning) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(17.dp),
                        strokeWidth = 2.dp,
                        color = colors.onAccent,
                    )
                } else {
                    Icon(Icons.Outlined.PlayArrow, contentDescription = null)
                }
                Spacer(Modifier.width(8.dp))
                Text(if (state.isQueueRunning) "Queue Running" else "Run Queue")
            }

            if (visibleQueue.isEmpty()) {
                EmptyState(
                    "No queue matches",
                    "Try another URL, extractor, or status.",
                    colors,
                )
            } else {
                visibleQueue.forEach { item ->
                    val actualIndex = state.queue.indexOfFirst { it.id == item.id }
                    QueueRow(
                        item = item,
                        colors = colors,
                        removeEnabled = !state.isQueueRunning && item.state != "running",
                        canMoveUp = !state.isQueueRunning && actualIndex > 0,
                        canMoveDown =
                            !state.isQueueRunning &&
                                actualIndex >= 0 &&
                                actualIndex < state.queue.lastIndex,
                        onMoveUp = { onMove(item.id, -1) },
                        onMoveDown = { onMove(item.id, 1) },
                        onRemove = { onRemove(item.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun QueueRow(
    item: GalleryDlStore.QueueRecord,
    colors: KirinGalleryColors,
    removeEnabled: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit,
) {
    val statusColor =
        when (item.state) {
            "completed" -> colors.success
            "failed" -> colors.error
            "running" -> colors.accent
            else -> colors.muted
        }

    Row(
        modifier =
            Modifier.fillMaxWidth()
                .background(colors.panel, RoundedCornerShape(12.dp))
                .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier.size(9.dp)
                    .background(statusColor, RoundedCornerShape(99.dp))
        )
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                item.url,
                color = colors.text,
                fontSize = 12.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                item.state.uppercase() +
                    item.extractor.takeIf(String::isNotBlank)?.let { " • $it" }.orEmpty(),
                color = statusColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
            )
            if (item.error.isNotBlank()) {
                Text(
                    item.error,
                    color = colors.error,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            IconButton(
                onClick = onMoveUp,
                enabled = canMoveUp,
                modifier = Modifier.size(32.dp),
            ) {
                Icon(
                    Icons.Outlined.ArrowUpward,
                    contentDescription = "Move up",
                    tint = colors.muted,
                    modifier = Modifier.size(18.dp),
                )
            }
            IconButton(
                onClick = onMoveDown,
                enabled = canMoveDown,
                modifier = Modifier.size(32.dp),
            ) {
                Icon(
                    Icons.Outlined.ArrowDownward,
                    contentDescription = "Move down",
                    tint = colors.muted,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        IconButton(onClick = onRemove, enabled = removeEnabled, modifier = Modifier.size(38.dp)) {
            Icon(
                Icons.Outlined.Delete,
                contentDescription = "Remove",
                tint = colors.muted,
            )
        }
    }
}

@Composable
private fun HistoryTab(
    state: GalleryDlViewModel.ViewState,
    colors: KirinGalleryColors,
    onReuse: (String) -> Unit,
    onClear: () -> Unit,
    onClearSuccessful: () -> Unit,
    onClearFailed: () -> Unit,
) {
    var searchQuery by rememberSaveable { mutableStateOf("") }
    val visibleHistory =
        remember(state.history, searchQuery) {
            val query = searchQuery.trim()
            if (query.isBlank()) state.history
            else
                state.history.filter { record ->
                    record.url.contains(query, ignoreCase = true) ||
                        record.extractor.contains(query, ignoreCase = true) ||
                        record.error.contains(query, ignoreCase = true) ||
                        (record.success && "completed".contains(query, ignoreCase = true)) ||
                        (!record.success && "failed".contains(query, ignoreCase = true))
                }
        }
    val successCount = state.history.count { it.success }
    val failureCount = state.history.size - successCount

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "History",
                    color = colors.text,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                )
                Text(
                    "Latest ${state.history.size} Gallery DL job(s)",
                    color = colors.muted,
                    fontSize = 12.sp,
                )
            }
            TextButton(
                onClick = onClear,
                enabled = state.history.isNotEmpty() && !state.isBusy,
            ) {
                Text("Clear", color = colors.error)
            }
        }

        GalleryStatsStrip(
            firstLabel = "Total",
            firstValue = state.history.size,
            secondLabel = "Done",
            secondValue = successCount,
            thirdLabel = "Failed",
            thirdValue = failureCount,
            fourthLabel = "Files",
            fourthValue = state.history.filter { it.success }.sumOf { it.fileCount },
            colors = colors,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(
                onClick = onClearSuccessful,
                enabled = successCount > 0 && !state.isBusy,
                modifier = Modifier.weight(1f),
            ) {
                Text("Clear done")
            }
            OutlinedButton(
                onClick = onClearFailed,
                enabled = failureCount > 0 && !state.isBusy,
                modifier = Modifier.weight(1f),
            ) {
                Text("Clear failed")
            }
        }
        if (state.history.isNotEmpty()) {
            GallerySearchField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = "Search history URL, extractor or status",
                colors = colors,
            )
        }

        if (state.history.isEmpty()) {
            EmptyState(
                "No history yet",
                "Finished and failed jobs will appear here.",
                colors,
            )
        } else {
            if (visibleHistory.isEmpty()) {
                EmptyState(
                    "No history matches",
                    "Try another URL, extractor, or status.",
                    colors,
                )
            } else {
                visibleHistory.forEach { record ->
                    HistoryRow(record, colors) { onReuse(record.url) }
                }
            }
        }
    }
}

@Composable
private fun HistoryRow(
    record: GalleryDlStore.HistoryRecord,
    colors: KirinGalleryColors,
    onReuse: () -> Unit,
) {
    val stateColor = if (record.success) colors.success else colors.error

    Column(
        modifier =
            Modifier.fillMaxWidth()
                .background(colors.panel, RoundedCornerShape(12.dp))
                .clickable(onClick = onReuse)
                .padding(13.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (record.success) "COMPLETED" else "FAILED",
                color = stateColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
            )
            Spacer(Modifier.weight(1f))
            Text(
                DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
                    .format(Date(record.finishedAt)),
                color = colors.muted,
                fontSize = 10.sp,
            )
        }

        Text(
            record.url,
            color = colors.text,
            fontSize = 12.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )

        Text(
            if (record.success) {
                listOf(
                        record.extractor.takeIf(String::isNotBlank),
                        "${record.fileCount} file(s)",
                    )
                    .filterNotNull()
                    .joinToString(" • ")
            } else {
                record.error.ifBlank { "Download failed" }
            },
            color = if (record.success) colors.muted else colors.error,
            fontSize = 10.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun GallerySiteFilterMemory(
    siteLabel: String,
    filterLabel: String,
    saved: Boolean,
    onRemember: () -> Unit,
    onClear: () -> Unit,
    colors: KirinGalleryColors,
) {
    Row(
        modifier =
            Modifier.fillMaxWidth()
                .background(colors.panelAlt, RoundedCornerShape(12.dp))
                .padding(start = 12.dp, end = 6.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "Site profile • $siteLabel",
                color = colors.text,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                if (saved) "Remembered • $filterLabel" else "Using global export filter",
                color = colors.muted,
                fontSize = 10.sp,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            if (!saved) {
                TextButton(onClick = onRemember) { Text("Remember") }
            } else {
                Text(
                    "Active",
                    color = colors.accent,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                )
                TextButton(onClick = onClear) { Text("Clear") }
            }
        }
    }
}

@Composable
private fun GalleryPreflightDiagnostic(
    state: GalleryDlViewModel.ViewState,
    colors: KirinGalleryColors,
) {
    val info = state.preflightInfo
    val errorText =
        listOfNotNull(info?.preflightError, state.errorMessage)
            .joinToString(" ")
            .lowercase()
    val authLabel =
        when {
            info?.cookiesLoaded == true -> "Cookies loaded for this extractor"
            state.cookiesImported -> "Cookies imported • available if the site needs login"
            errorText.contains("login") ||
                errorText.contains("auth") ||
                errorText.contains("unauthorized") ||
                errorText.contains("forbidden") -> "Authentication may be required"
            else -> "No authentication warning detected"
        }
    val rateWarning =
        errorText.contains("429") ||
            errorText.contains("rate limit") ||
            errorText.contains("too many requests")
    val rateLabel =
        when {
            rateWarning -> "Rate-limit warning detected"
            info?.largeGallery == true ->
                "Large gallery • conservative retry behavior recommended"
            else -> "No rate-limit warning detected"
        }

    Column(
        modifier =
            Modifier.fillMaxWidth()
                .background(colors.panelAlt, RoundedCornerShape(12.dp))
                .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            "Preflight diagnostics",
            color = colors.text,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(authLabel, color = colors.muted, fontSize = 10.sp)
        Text(
            rateLabel,
            color = if (rateWarning) colors.error else colors.muted,
            fontSize = 10.sp,
        )
        info?.mediaType?.takeIf(String::isNotBlank)?.let {
            Text("Media • $it", color = colors.muted, fontSize = 10.sp)
        }
    }
}

@Composable
private fun GalleryStatsStrip(
    firstLabel: String,
    firstValue: Int,
    secondLabel: String,
    secondValue: Int,
    thirdLabel: String,
    thirdValue: Int,
    fourthLabel: String,
    fourthValue: Int,
    colors: KirinGalleryColors,
) {
    Row(
        modifier =
            Modifier.fillMaxWidth()
                .background(colors.panelAlt, RoundedCornerShape(12.dp))
                .padding(horizontal = 10.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        GalleryMetric(firstLabel, firstValue, colors, Modifier.weight(1f))
        GalleryMetric(secondLabel, secondValue, colors, Modifier.weight(1f))
        GalleryMetric(thirdLabel, thirdValue, colors, Modifier.weight(1f))
        GalleryMetric(fourthLabel, fourthValue, colors, Modifier.weight(1f))
    }
}

@Composable
private fun GalleryMetric(
    label: String,
    value: Int,
    colors: KirinGalleryColors,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value.toString(),
            color = if (value > 0) colors.accent else colors.text,
            fontSize = 14.sp,
            fontWeight = FontWeight.Black,
        )
        Text(
            label,
            color = colors.muted,
            fontSize = 9.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun GallerySearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    colors: KirinGalleryColors,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        leadingIcon = {
            Icon(Icons.Outlined.Search, contentDescription = null, tint = colors.accent)
        },
        trailingIcon =
            if (value.isNotBlank()) {
                {
                    IconButton(onClick = { onValueChange("") }) {
                        Icon(
                            Icons.Outlined.Clear,
                            contentDescription = "Clear search",
                            tint = colors.muted,
                        )
                    }
                }
            } else {
                null
            },
        placeholder = { Text(placeholder, color = colors.muted) },
        textStyle = MaterialTheme.typography.bodyMedium.copy(color = colors.text),
    )
}

@Composable
private fun FlatInfoRow(
    label: String,
    value: String,
    valueColor: Color,
    colors: KirinGalleryColors,
) {
    Row(
        modifier =
            Modifier.fillMaxWidth()
                .background(colors.panelAlt, RoundedCornerShape(10.dp))
                .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = colors.muted, fontSize = 11.sp)
        Spacer(Modifier.weight(1f))
        Text(
            value,
            color = valueColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun Notice(
    text: String,
    color: Color,
    colors: KirinGalleryColors,
) {
    Text(
        text,
        modifier =
            Modifier.fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .background(colors.panelAlt, RoundedCornerShape(10.dp))
                .padding(12.dp),
        color = color,
        fontSize = 11.sp,
    )
}

@Composable
private fun EmptyState(
    title: String,
    description: String,
    colors: KirinGalleryColors,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 44.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(title, color = colors.text, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(5.dp))
        Text(description, color = colors.muted, fontSize = 11.sp)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GalleryDownloadConfirmDialog(
    state: GalleryDlViewModel.ViewState,
    colors: KirinGalleryColors,
    outputRoot: String,
    action: GalleryConfirmAction,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val host =
        remember(state.url) {
            runCatching {
                Uri.parse(state.url).host.orEmpty().removePrefix("www.")
            }.getOrDefault("")
        }
    val info = state.preflightInfo
    val preflightReady = !state.isCheckingExtractor && state.extractorSupported == true
    val itemCountLabel =
        info?.estimatedItemCount?.let { count ->
            when {
                info?.itemCountExact == true -> count.toString()
                count > 0 -> "$count+ / estimated"
                else -> "Unknown"
            }
        } ?: "Unknown"
    val statusLabel =
        when {
            state.isCheckingExtractor -> "Analyzing metadata…"
            state.extractorSupported == false -> "Unsupported URL"
            info?.preflightStatus == "login_required" -> "Login / cookies may be required"
            info?.preflightStatus == "rate_limited" -> "Site rate limited preflight"
            info?.preflightStatus == "extractor_error" -> "Metadata partially unavailable"
            preflightReady -> "Ready"
            else -> "Waiting for preflight"
        }
    val statusColor =
        when {
            state.extractorSupported == false -> colors.error
            info?.preflightStatus == "login_required" -> colors.error
            info?.preflightStatus == "rate_limited" -> colors.error
            info?.preflightStatus == "extractor_error" -> colors.accent
            preflightReady -> colors.success
            else -> colors.accent
        }

    SealModalBottomSheet(
        onDismissRequest = onDismiss,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier =
                        Modifier.size(48.dp)
                            .background(colors.accentSoft, RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Outlined.AutoAwesome,
                        contentDescription = null,
                        tint = colors.accent,
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        if (action == GalleryConfirmAction.DOWNLOAD) {
                            "Review Gallery Download"
                        } else {
                            "Review Queue Item"
                        },
                        color = colors.text,
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Black,
                    )
                    Text(
                        "Seal-style preflight: inspect first, then choose whether to continue.",
                        color = colors.muted,
                        fontSize = 12.sp,
                    )
                }
            }

            if (!info?.thumbnailUrl.isNullOrBlank()) {
                AsyncImage(
                    model = info?.thumbnailUrl,
                    contentDescription = "Gallery preview",
                    modifier =
                        Modifier.fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(colors.panelAlt),
                    contentScale = ContentScale.Crop,
                )
            }

            if (!info?.title.isNullOrBlank() || !info?.author.isNullOrBlank()) {
                Column(
                    modifier =
                        Modifier.fillMaxWidth()
                            .background(colors.panelAlt, RoundedCornerShape(14.dp))
                            .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    if (!info?.title.isNullOrBlank()) {
                        Text(
                            info?.title.orEmpty(),
                            color = colors.text,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    if (!info?.author.isNullOrBlank()) {
                        Text(
                            info?.author.orEmpty(),
                            color = colors.muted,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }

            Box(
                modifier =
                    Modifier.fillMaxWidth()
                        .background(colors.panelAlt, RoundedCornerShape(14.dp))
                        .padding(13.dp),
            ) {
                Text(
                    state.url,
                    color = colors.text,
                    fontSize = 12.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Text(
                "PREFLIGHT DETAILS",
                color = colors.accent,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
            )

            FlatInfoRow("Status", statusLabel, statusColor, colors)
            FlatInfoRow("Site", host.ifBlank { "Unknown" }, colors.text, colors)
            when {
                state.isCheckingExtractor ->
                    FlatInfoRow("Extractor", "Analyzing…", colors.accent, colors)
                state.extractorSupported == true ->
                    FlatInfoRow(
                        "Extractor",
                        state.extractorLabel ?: "Supported",
                        colors.success,
                        colors,
                    )
                state.extractorSupported == false ->
                    FlatInfoRow("Extractor", "Unsupported", colors.error, colors)
                else ->
                    FlatInfoRow("Extractor", "Waiting for preflight", colors.muted, colors)
            }
            if (!info?.mediaType.isNullOrBlank()) {
                FlatInfoRow("Media", info?.mediaType.orEmpty(), colors.text, colors)
            }
            FlatInfoRow("Estimated items", itemCountLabel, colors.text, colors)
            FlatInfoRow(
                "Authentication",
                when {
                    info?.preflightStatus == "login_required" -> "Login / cookies required"
                    state.cookiesImported -> "Cookies available"
                    else -> "No cookies imported"
                },
                when {
                    info?.preflightStatus == "login_required" -> colors.error
                    state.cookiesImported -> colors.success
                    else -> colors.muted
                },
                colors,
            )
            FlatInfoRow(
                "Engine",
                state.installedVersion?.let { "gallery-dl $it" } ?: "Not installed",
                if (state.isInstalled) colors.success else colors.error,
                colors,
            )
            FlatInfoRow("Output", outputRoot, colors.text, colors)

            if (info?.largeGallery == true) {
                Notice(
                    "Large gallery detected. Metadata preview is intentionally capped; the actual download can contain more items than the preflight scan.",
                    colors.accent,
                    colors,
                )
            }

            info?.preflightError?.takeIf(String::isNotBlank)?.let { warning ->
                Notice(
                    "Preflight note: $warning",
                    if (info?.preflightStatus == "extractor_error") colors.accent else colors.error,
                    colors,
                )
            }

            Notice(
                when {
                    preflightReady && info?.preflightStatus == "ready" ->
                        "Preflight passed. Only metadata was inspected; the media download has not started."
                    preflightReady ->
                        "The extractor matched this URL, but some metadata could not be verified. You can still continue or cancel."
                    state.extractorSupported == false ->
                        "This URL is not currently matched by the installed gallery-dl engine."
                    else ->
                        "KirinDL is checking the extractor before enabling Continue."
                },
                if (preflightReady) colors.success else colors.accent,
                colors,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f).height(50.dp),
                ) {
                    Text("Cancel")
                }
                Button(
                    onClick = onConfirm,
                    enabled = preflightReady,
                    modifier = Modifier.weight(1.35f).height(50.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.accent,
                        contentColor = colors.onAccent,
                    ),
                ) {
                    Icon(
                        if (action == GalleryConfirmAction.DOWNLOAD) {
                            Icons.Outlined.Download
                        } else {
                            Icons.Outlined.Add
                        },
                        contentDescription = null,
                    )
                    Spacer(Modifier.width(7.dp))
                    Text(
                        if (action == GalleryConfirmAction.DOWNLOAD) {
                            "Download Now"
                        } else {
                            "Add to Queue"
                        },
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GalleryBatchConfirmDialog(
    text: String,
    colors: KirinGalleryColors,
    outputRoot: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val allEntries =
        remember(text) {
            text.lines().map(String::trim).filter(String::isNotBlank).distinct()
        }
    val urls = remember(allEntries) { allEntries.filter(com.junkfood.seal.util.GalleryDlRunner::isCandidateUrl) }
    val invalidCount = allEntries.size - urls.size
    val siteCounts =
        remember(urls) {
            urls.mapNotNull { url ->
                runCatching { Uri.parse(url).host?.removePrefix("www.") }.getOrNull()
            }.groupingBy { it }.eachCount().entries.sortedByDescending { it.value }
        }
    val largeBatch = urls.size >= 20

    SealModalBottomSheet(
        onDismissRequest = onDismiss,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier =
                        Modifier.size(48.dp)
                            .background(colors.accentSoft, RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Outlined.Queue, contentDescription = null, tint = colors.accent)
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        "Review Gallery Batch",
                        color = colors.text,
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Black,
                    )
                    Text(
                        "One confirmation for the whole batch — no popup spam per URL.",
                        color = colors.muted,
                        fontSize = 12.sp,
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                GalleryStatCard("Valid URLs", urls.size.toString(), colors, Modifier.weight(1f))
                GalleryStatCard("Sites", siteCounts.size.toString(), colors, Modifier.weight(1f))
            }
            if (invalidCount > 0) {
                GalleryStatCard("Skipped invalid", invalidCount.toString(), colors, Modifier.fillMaxWidth())
            }

            if (siteCounts.isNotEmpty()) {
                Text(
                    "SITE SUMMARY",
                    color = colors.accent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                )
                siteCounts.take(8).forEach { (site, count) ->
                    FlatInfoRow(site, "$count URL(s)", colors.text, colors)
                }
                if (siteCounts.size > 8) {
                    FlatInfoRow(
                        "Other sites",
                        "+${siteCounts.size - 8}",
                        colors.muted,
                        colors,
                    )
                }
            }

            FlatInfoRow("Queue mode", "Separate jobs", colors.text, colors)
            FlatInfoRow("Failure handling", "Continue remaining URLs", colors.text, colors)
            FlatInfoRow("Output", outputRoot, colors.text, colors)

            if (largeBatch) {
                Notice(
                    "Large batch detected (${urls.size} URLs). KirinDL will queue them as separate jobs so one failed extractor does not cancel the rest.",
                    colors.accent,
                    colors,
                )
            } else {
                Notice(
                    "The confirmed URLs enter the Gallery DL queue as separate jobs. One failed site will not cancel the rest.",
                    colors.accent,
                    colors,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f).height(50.dp),
                ) {
                    Text("Review")
                }
                Button(
                    onClick = onConfirm,
                    enabled = urls.isNotEmpty(),
                    modifier = Modifier.weight(1.35f).height(50.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.accent,
                        contentColor = colors.onAccent,
                    ),
                ) {
                    Icon(Icons.Outlined.Queue, contentDescription = null)
                    Spacer(Modifier.width(7.dp))
                    Text("Queue ${urls.size}", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun GalleryDashboard(
    state: GalleryDlViewModel.ViewState,
    colors: KirinGalleryColors,
) {
    val pending = state.queue.count { it.state == "pending" || it.state == "failed" }
    val completed = state.history.count { it.success }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            GalleryStatCard(
                "Engine",
                state.installedVersion?.let { "v$it" } ?: "Setup",
                colors,
                Modifier.weight(1f),
            )
            GalleryStatCard("Queue", pending.toString(), colors, Modifier.weight(1f))
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            GalleryStatCard("History", completed.toString(), colors, Modifier.weight(1f))
            GalleryStatCard(
                "Cookies",
                if (state.cookiesImported) "Ready" else "Off",
                colors,
                Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun GalleryStatCard(
    label: String,
    value: String,
    colors: KirinGalleryColors,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier.background(colors.panel, RoundedCornerShape(12.dp)).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Text(label, color = colors.muted, fontSize = 10.sp)
        Text(
            value,
            color = colors.text,
            fontSize = 15.sp,
            fontWeight = FontWeight.Black,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun BatchDialog(
    colors: KirinGalleryColors,
    onDismiss: () -> Unit,
    onAdd: (String) -> Unit,
) {
    var text by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Batch Gallery URLs",
                color = MaterialTheme.colorScheme.onSurface,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "One URL per line. Valid links go into the Gallery DL queue.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                )
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 8,
                    maxLines = 14,
                    textStyle =
                        MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurface,
                        ),
                    placeholder = {
                        Text(
                            "https://...\nhttps://...\nhttps://...",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onAdd(text) },
                enabled = text.isNotBlank(),
            ) {
                Text("Add to Queue")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}
