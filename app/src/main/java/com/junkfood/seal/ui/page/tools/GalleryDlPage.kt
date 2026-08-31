package com.junkfood.seal.ui.page.tools

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.junkfood.seal.ui.component.BackButton
import com.junkfood.seal.util.GalleryDlStore
import java.text.DateFormat
import java.util.Date
import org.koin.androidx.compose.koinViewModel

private data class KirinGalleryColors(
    val background: Color,
    val panel: Color,
    val panelAlt: Color,
    val accent: Color,
    val accentSoft: Color,
    val text: Color,
    val muted: Color,
    val success: Color,
    val error: Color,
)

@Composable
private fun kirinGalleryColors(): KirinGalleryColors =
    if (isSystemInDarkTheme()) {
        KirinGalleryColors(
            background = Color(0xFF071116),
            panel = Color(0xFF0D1B22),
            panelAlt = Color(0xFF10252E),
            accent = Color(0xFF47D2FF),
            accentSoft = Color(0xFF123541),
            text = Color(0xFFF2F7F9),
            muted = Color(0xFF92AAB3),
            success = Color(0xFF6EE7B7),
            error = Color(0xFFFF7D86),
        )
    } else {
        KirinGalleryColors(
            background = Color(0xFFF2F7F9),
            panel = Color.White,
            panelAlt = Color(0xFFE8F2F5),
            accent = Color(0xFF007EA4),
            accentSoft = Color(0xFFD9F2FA),
            text = Color(0xFF102027),
            muted = Color(0xFF607780),
            success = Color(0xFF16835D),
            error = Color(0xFFB42331),
        )
    }

@Composable
fun GalleryDlPage(
    onNavigateBack: () -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: GalleryDlViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = kirinGalleryColors()
    val clipboard = LocalClipboardManager.current
    var tab by remember { mutableIntStateOf(0) }
    var showBatch by remember { mutableStateOf(false) }

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
                        onDownload = viewModel::download,
                        onQueue = viewModel::addCurrentToQueue,
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
                viewModel.addBatch(it)
                showBatch = false
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
        Text("Download a gallery", color = colors.text, fontSize = 24.sp, fontWeight = FontWeight.Black)
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
                    placeholder = { Text("Gallery or collection URL") },
                    leadingIcon = {
                        Icon(Icons.Outlined.Link, contentDescription = null, tint = colors.accent)
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
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
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
            true -> FlatInfoRow("Extractor", state.extractorLabel ?: "Ready", colors.success, colors)
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
                    contentColor = Color(0xFF00151C),
                ),
            shape = RoundedCornerShape(12.dp),
        ) {
            if (state.isDownloading) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
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
                    !state.isBusy &&
                        com.junkfood.seal.util.GalleryDlRunner.isCandidateUrl(state.url),
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.Outlined.Add, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Add to Queue")
            }
            OutlinedButton(onClick = onBatch, enabled = !state.isBusy, modifier = Modifier.weight(1f)) {
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
                Text("Download queue", color = colors.text, fontSize = 24.sp, fontWeight = FontWeight.Black)
                Text("${state.queue.size} item(s) • sequential jobs", color = colors.muted, fontSize = 12.sp)
            }
            IconButton(onClick = onClearFinished, enabled = !state.isQueueRunning) {
                Icon(Icons.Outlined.ClearAll, contentDescription = "Clear finished", tint = colors.muted)
            }
        }

        if (state.queue.isEmpty()) {
            EmptyState("Queue is empty", "Add a URL or use Batch URLs from Download.", colors)
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
                        contentColor = Color(0xFF00151C),
                    ),
            ) {
                if (state.isQueueRunning) {
                    CircularProgressIndicator(modifier = Modifier.size(17.dp), strokeWidth = 2.dp)
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
        modifier = Modifier.fillMaxWidth().background(colors.panel, RoundedCornerShape(12.dp)).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.size(9.dp).background(statusColor, RoundedCornerShape(99.dp)))
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(item.url, color = colors.text, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(
                item.state.uppercase() + item.extractor.takeIf(String::isNotBlank)?.let { " • $it" }.orEmpty(),
                color = statusColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
            )
            if (item.error.isNotBlank()) {
                Text(item.error, color = colors.error, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        IconButton(onClick = onRemove, enabled = removeEnabled) {
            Icon(Icons.Outlined.Delete, contentDescription = "Remove", tint = colors.muted)
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
                Text("History", color = colors.text, fontSize = 24.sp, fontWeight = FontWeight.Black)
                Text("Latest ${state.history.size} Gallery DL job(s)", color = colors.muted, fontSize = 12.sp)
            }
            TextButton(onClick = onClear, enabled = state.history.isNotEmpty() && !state.isBusy) {
                Text("Clear", color = colors.error)
            }
        }

        if (state.history.isEmpty()) {
            EmptyState("No history yet", "Finished and failed jobs will appear here.", colors)
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
                DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(record.finishedAt)),
                color = colors.muted,
                fontSize = 10.sp,
            )
        }
        Text(record.url, color = colors.text, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Text(
            if (record.success) {
                listOf(record.extractor.takeIf(String::isNotBlank), "${record.fileCount} file(s)")
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
private fun FlatInfoRow(label: String, value: String, valueColor: Color, colors: KirinGalleryColors) {
    Row(
        modifier = Modifier.fillMaxWidth().background(colors.panelAlt, RoundedCornerShape(10.dp)).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = colors.muted, fontSize = 11.sp)
        Spacer(Modifier.weight(1f))
        Text(value, color = valueColor, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun Notice(text: String, color: Color, colors: KirinGalleryColors) {
    Text(
        text,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp).background(colors.panelAlt, RoundedCornerShape(10.dp)).padding(12.dp),
        color = color,
        fontSize = 11.sp,
    )
}

@Composable
private fun EmptyState(title: String, description: String, colors: KirinGalleryColors) {
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
private fun BatchDialog(colors: KirinGalleryColors, onDismiss: () -> Unit, onAdd: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Batch Gallery URLs") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("One URL per line. Valid links go into the Gallery DL queue.", color = colors.muted, fontSize = 11.sp)
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 8,
                    maxLines = 14,
                    placeholder = { Text("https://...\nhttps://...\nhttps://...") },
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onAdd(text) }, enabled = text.isNotBlank()) { Text("Add to Queue") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
