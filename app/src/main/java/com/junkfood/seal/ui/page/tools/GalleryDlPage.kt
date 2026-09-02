package com.junkfood.seal.ui.page.tools

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ClearAll
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Queue
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.junkfood.seal.ui.component.BackButton
import com.junkfood.seal.util.GalleryDlStore
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
    val colors = kirinGalleryColors(themeStyle)
    val clipboard = LocalClipboardManager.current
    var tab by remember { mutableIntStateOf(0) }
    var showBatch by remember { mutableStateOf(false) }
    var pendingAction by remember { mutableStateOf<GalleryConfirmAction?>(null) }
    var pendingBatchText by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) { viewModel.refreshFromDisk() }

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
                colors = colors,
                onSelected = { tab = it },
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
                            pendingAction = GalleryConfirmAction.DOWNLOAD
                            viewModel.checkExtractor()
                        },
                        onQueue = {
                            pendingAction = GalleryConfirmAction.QUEUE
                            viewModel.checkExtractor()
                        },
                        onBatch = { showBatch = true },
                    )
                1 ->
                    QueueTab(
                        state = state,
                        colors = colors,
                        onRun = viewModel::runQueue,
                        onRemove = viewModel::removeQueueItem,
                        onClearFinished = viewModel::clearFinishedQueue,
                    )
                else ->
                    HistoryTab(
                        state = state,
                        colors = colors,
                        onReuse = {
                            viewModel.reuseHistoryUrl(it)
                            tab = 0
                        },
                        onClear = viewModel::clearHistory,
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
                pendingBatchText = it
            },
        )
    }

    pendingAction?.let { action ->
        GalleryDownloadConfirmDialog(
            state = state,
            colors = colors,
            action = action,
            onDismiss = { pendingAction = null },
            onConfirm = {
                when (action) {
                    GalleryConfirmAction.DOWNLOAD -> viewModel.download()
                    GalleryConfirmAction.QUEUE -> {
                        viewModel.addCurrentToQueue()
                        tab = 1
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
            onDismiss = { pendingBatchText = null },
            onConfirm = {
                viewModel.addBatch(batchText)
                pendingBatchText = null
                tab = 1
            },
        )
    }
}

@Composable
private fun KirinTabs(
    selected: Int,
    queueCount: Int,
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
                Triple("History", Icons.Outlined.History, ""),
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
    onClearFinished: () -> Unit,
) {
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
            IconButton(onClick = onClearFinished, enabled = !state.isQueueRunning) {
                Icon(
                    Icons.Outlined.ClearAll,
                    contentDescription = "Clear finished",
                    tint = colors.muted,
                )
            }
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

            state.queue.forEach { item ->
                QueueRow(
                    item = item,
                    colors = colors,
                    removeEnabled = !state.isQueueRunning && item.state != "running",
                    onRemove = { onRemove(item.id) },
                )
            }
        }
    }
}

@Composable
private fun QueueRow(
    item: GalleryDlStore.QueueRecord,
    colors: KirinGalleryColors,
    removeEnabled: Boolean,
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
        IconButton(onClick = onRemove, enabled = removeEnabled) {
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
) {
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

        if (state.history.isEmpty()) {
            EmptyState(
                "No history yet",
                "Finished and failed jobs will appear here.",
                colors,
            )
        } else {
            state.history.forEach { record ->
                HistoryRow(record, colors) { onReuse(record.url) }
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

@Composable
private fun GalleryDownloadConfirmDialog(
    state: GalleryDlViewModel.ViewState,
    colors: KirinGalleryColors,
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

    val preflightReady =
        !state.isCheckingExtractor && state.extractorSupported == true

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (action == GalleryConfirmAction.DOWNLOAD) {
                    "Confirm Gallery Download"
                } else {
                    "Confirm Queue Item"
                },
                color = MaterialTheme.colorScheme.onSurface,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Text(
                    state.url,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 12.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
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

                FlatInfoRow(
                    "Cookies",
                    if (state.cookiesImported) "Available" else "Not imported",
                    if (state.cookiesImported) colors.success else colors.muted,
                    colors,
                )
                FlatInfoRow("Output", "Download/GalleryDL/", colors.text, colors)

                Text(
                    if (preflightReady) {
                        "Preflight passed. Nothing has been downloaded yet."
                    } else {
                        "Continue is enabled only after the gallery-dl extractor preflight succeeds."
                    },
                    color = colors.muted,
                    fontSize = 11.sp,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = preflightReady) {
                Text(
                    if (action == GalleryConfirmAction.DOWNLOAD) {
                        "Download Now"
                    } else {
                        "Add to Queue"
                    }
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun GalleryBatchConfirmDialog(
    text: String,
    colors: KirinGalleryColors,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val urls =
        remember(text) {
            text.lines()
                .map(String::trim)
                .filter(String::isNotBlank)
                .distinct()
                .filter(com.junkfood.seal.util.GalleryDlRunner::isCandidateUrl)
        }

    val siteSummary =
        remember(urls) {
            urls.mapNotNull { url ->
                runCatching {
                    Uri.parse(url).host?.removePrefix("www.")
                }.getOrNull()
            }
                .groupingBy { it }
                .eachCount()
                .entries
                .sortedByDescending { it.value }
                .joinToString(" • ") { (site, count) -> "$site $count" }
        }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Confirm Gallery Batch",
                color = MaterialTheme.colorScheme.onSurface,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                FlatInfoRow("URLs", urls.size.toString(), colors.text, colors)
                FlatInfoRow(
                    "Sites",
                    siteSummary.ifBlank { "Unknown" },
                    colors.text,
                    colors,
                )
                FlatInfoRow("Output", "Download/GalleryDL/", colors.text, colors)
                Text(
                    "The confirmed URLs will be added to the Gallery DL queue as separate jobs, so one failed site will not discard the rest.",
                    color = colors.muted,
                    fontSize = 11.sp,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = urls.isNotEmpty()) {
                Text("Add ${urls.size} to Queue")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Review") }
        },
    )
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
