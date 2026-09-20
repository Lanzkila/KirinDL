package com.junkfood.seal.ui.page.tools

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Queue
import androidx.compose.material.icons.outlined.Replay
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.junkfood.seal.ui.component.BackButton
import com.junkfood.seal.util.GalleryDlBehaviorPreference
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
            GalleryDlThemeStyle.SAKURA -> Color(0xFFFF78A8)
            GalleryDlThemeStyle.CRIMSON -> Color(0xFFE5485D)
            GalleryDlThemeStyle.AMBER -> Color(0xFFFFB52E)
            GalleryDlThemeStyle.TEAL -> Color(0xFF20B7A6)
            GalleryDlThemeStyle.INDIGO -> Color(0xFF6674E8)
            GalleryDlThemeStyle.LIME -> Color(0xFF91C94B)
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
        background = scheme.background,
        panel = scheme.surface,
        panelAlt = scheme.surfaceVariant,
        accent = accent,
        accentSoft =
            if (style == GalleryDlThemeStyle.APP_DEFAULT) scheme.primaryContainer
            else accent.copy(alpha = 0.14f),
        onAccent = onAccent,
        text = scheme.onSurface,
        muted = scheme.onSurfaceVariant,
        success = Color(0xFF2CA879),
        error = scheme.error,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryDlPage(
    onNavigateBack: () -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: GalleryDlViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val themeStyle by GalleryDlThemePreference.style.collectAsStateWithLifecycle()
    val confirmBeforeDownload by
        GalleryDlBehaviorPreference.confirmBeforeDownload.collectAsStateWithLifecycle()
    val exportFilter by GalleryDlBehaviorPreference.exportFilter.collectAsStateWithLifecycle()
    val colors = kirinGalleryColors(themeStyle)
    val clipboard = LocalClipboardManager.current

    var selectedTab by rememberSaveable {
        mutableIntStateOf(GalleryDlBehaviorPreference.lastTab())
    }
    var siteFilterRevision by remember { mutableIntStateOf(0) }
    var showBatchDialog by remember { mutableStateOf(false) }
    var showDownloadCenter by remember { mutableStateOf(false) }
    var confirmAction by remember { mutableStateOf<GalleryConfirmAction?>(null) }
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
                        "Gallery DL",
                        color = colors.text,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        state.installedVersion?.let { "Ready • $it" }
                            ?: "Engine setup required",
                        color = if (state.isInstalled) colors.accent else colors.muted,
                        fontSize = 11.sp,
                    )
                }
                IconButton(onClick = { showDownloadCenter = true }) {
                    Box {
                        Icon(
                            Icons.Outlined.FileDownload,
                            contentDescription = "Gallery Download Center",
                            tint = colors.accent,
                        )
                        val pendingCount =
                            state.queue.count { it.state == "pending" || it.state == "running" }
                        if (pendingCount > 0) {
                            Surface(
                                modifier = Modifier.align(Alignment.TopEnd).size(15.dp),
                                shape = CircleShape,
                                color = colors.error,
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        if (pendingCount > 9) "9+" else pendingCount.toString(),
                                        color = MaterialTheme.colorScheme.onError,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            }
                        }
                    }
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
            GalleryTabs(
                selected = selectedTab,
                queueCount = state.queue.count { it.state == "pending" || it.state == "running" },
                historyCount = state.history.size,
                colors = colors,
                onSelected = {
                    selectedTab = it
                    GalleryDlBehaviorPreference.setLastTab(it)
                },
            )

            when (selectedTab) {
                0 ->
                    GalleryDownloadTab(
                        state = state,
                        colors = colors,
                        clipboardText = { clipboard.getText()?.text.orEmpty() },
                        onUrlChanged = viewModel::updateUrl,
                        onCheck = viewModel::checkExtractor,
                        onDownload = {
                            if (confirmBeforeDownload) {
                                confirmAction = GalleryConfirmAction.DOWNLOAD
                                viewModel.checkExtractor()
                            } else {
                                viewModel.download()
                            }
                        },
                        onQueue = {
                            if (confirmBeforeDownload) {
                                confirmAction = GalleryConfirmAction.QUEUE
                                viewModel.checkExtractor()
                            } else {
                                viewModel.addCurrentToQueue()
                                selectedTab = 1
                                GalleryDlBehaviorPreference.setLastTab(1)
                            }
                        },
                        onBatch = { showBatchDialog = true },
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
                    GalleryQueueTab(
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
                    GalleryHistoryTab(
                        state = state,
                        colors = colors,
                        onReuse = {
                            viewModel.reuseHistoryUrl(it)
                            selectedTab = 0
                            GalleryDlBehaviorPreference.setLastTab(0)
                        },
                        onClearAll = viewModel::clearHistory,
                        onClearSuccessful = viewModel::clearSuccessfulHistory,
                        onClearFailed = viewModel::clearFailedHistory,
                    )
            }

            Spacer(Modifier.height(28.dp))
        }
    }

    if (showBatchDialog) {
        GalleryBatchDialog(
            colors = colors,
            onDismiss = { showBatchDialog = false },
            onAdd = { batchText ->
                showBatchDialog = false
                if (confirmBeforeDownload) {
                    pendingBatchText = batchText
                } else {
                    viewModel.addBatch(batchText)
                    selectedTab = 1
                    GalleryDlBehaviorPreference.setLastTab(1)
                }
            },
        )
    }

    pendingBatchText?.let { batchText ->
        val batchCount =
            remember(batchText) {
                batchText.lines()
                    .map(String::trim)
                    .filter(String::isNotBlank)
                    .distinct()
                    .count(GalleryDlRunner::isCandidateUrl)
            }
        AlertDialog(
            onDismissRequest = { pendingBatchText = null },
            title = { Text("Add batch to Gallery queue?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("$batchCount valid Gallery URL(s) will be added to the queue.")
                    Text(
                        "The same Gallery DL behavior and export filter will be used when each job runs.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.addBatch(batchText)
                        pendingBatchText = null
                        selectedTab = 1
                        GalleryDlBehaviorPreference.setLastTab(1)
                    },
                    enabled = batchCount > 0,
                ) {
                    Text("Add $batchCount")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingBatchText = null }) { Text("Cancel") }
            },
        )
    }

    confirmAction?.let { action ->
        AlertDialog(
            onDismissRequest = { confirmAction = null },
            title = {
                Text(
                    if (action == GalleryConfirmAction.DOWNLOAD) "Download gallery?"
                    else "Add to Gallery queue?"
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        state.preflightInfo?.title?.takeIf(String::isNotBlank)
                            ?: state.url,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (state.isCheckingExtractor) {
                        Text(
                            "Checking extractor…",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    state.extractorLabel?.let {
                        Text(
                            "Extractor: $it",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    Text(
                        "Export: ${GalleryDlBehaviorPreference.exportFilterLabel(effectiveExportFilter)}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        when (action) {
                            GalleryConfirmAction.DOWNLOAD -> viewModel.download()
                            GalleryConfirmAction.QUEUE -> {
                                viewModel.addCurrentToQueue()
                                selectedTab = 1
                                GalleryDlBehaviorPreference.setLastTab(1)
                            }
                        }
                        confirmAction = null
                    },
                    enabled = !state.isCheckingExtractor && state.extractorSupported != false,
                ) {
                    Text(if (action == GalleryConfirmAction.DOWNLOAD) "Download" else "Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmAction = null }) { Text("Cancel") }
            },
        )
    }

    if (showDownloadCenter) {
        GalleryDownloadCenterSheet(
            state = state,
            colors = colors,
            onDismiss = { showDownloadCenter = false },
            onRun = viewModel::runQueue,
            onRetryFailed = viewModel::retryFailedQueue,
            onOpenQueue = {
                showDownloadCenter = false
                selectedTab = 1
                GalleryDlBehaviorPreference.setLastTab(1)
            },
            onOpenHistory = {
                showDownloadCenter = false
                selectedTab = 2
                GalleryDlBehaviorPreference.setLastTab(2)
            },
        )
    }
}

@Composable
private fun GalleryTabs(
    selected: Int,
    queueCount: Int,
    historyCount: Int,
    colors: KirinGalleryColors,
    onSelected: (Int) -> Unit,
) {
    val entries =
        listOf(
            Triple("Download", Icons.Outlined.Download, ""),
            Triple("Queue", Icons.Outlined.Queue, if (queueCount > 0) " $queueCount" else ""),
            Triple("History", Icons.Outlined.History, if (historyCount > 0) " $historyCount" else ""),
        )
    Row(
        modifier =
            Modifier.fillMaxWidth()
                .background(colors.panel)
                .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        entries.forEachIndexed { index, entry ->
            val active = selected == index
            Row(
                modifier =
                    Modifier.weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (active) colors.accentSoft else Color.Transparent)
                        .clickable { onSelected(index) }
                        .padding(vertical = 9.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    entry.second,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = if (active) colors.accent else colors.muted,
                )
                Spacer(Modifier.width(5.dp))
                Text(
                    entry.first + entry.third,
                    color = if (active) colors.text else colors.muted,
                    fontSize = 11.sp,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun GalleryDownloadTab(
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
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Download a gallery", color = colors.text, fontSize = 23.sp, fontWeight = FontWeight.Bold)
        Text(
            "Paste a gallery, album, post, or collection URL. Check it first, then download or queue it.",
            color = colors.muted,
            fontSize = 12.sp,
        )

        Surface(
            shape = RoundedCornerShape(18.dp),
            color = colors.panel,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedTextField(
                    value = state.url,
                    onValueChange = onUrlChanged,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isBusy,
                    singleLine = true,
                    leadingIcon = {
                        Icon(Icons.Outlined.Link, contentDescription = null, tint = colors.accent)
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
                    placeholder = { Text("Gallery or collection URL", color = colors.muted) },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.text),
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
                                GalleryDlRunner.isCandidateUrl(state.url),
                        modifier = Modifier.weight(1f),
                    ) {
                        if (state.isCheckingExtractor) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Outlined.Check, contentDescription = null)
                        }
                        Spacer(Modifier.width(6.dp))
                        Text("Check")
                    }
                }
            }
        }

        GalleryStatusStrip(state = state, colors = colors)

        state.errorMessage?.let { InlineNotice(it, colors.error, colors) }
        state.statusMessage?.let { InlineNotice(it, colors.success, colors) }

        state.preflightInfo?.let { info ->
            GalleryExtractorInspector(info = info, state = state, colors = colors)
        } ?: when (state.extractorSupported) {
            false -> InlineNotice("No active gallery-dl extractor matched this URL.", colors.error, colors)
            else -> Unit
        }

        if (state.isDownloading) {
            GalleryProgress(state = state, colors = colors)
        }

        Surface(
            shape = RoundedCornerShape(14.dp),
            color = colors.panel,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Export filter", color = colors.muted, fontSize = 11.sp)
                    Text(
                        GalleryDlBehaviorPreference.exportFilterLabel(exportFilter),
                        color = colors.accent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                siteLabel?.let { label ->
                    Text(
                        "Site: $label",
                        color = colors.muted,
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutlinedButton(
                            onClick = onRememberSiteFilter,
                            enabled = state.url.isNotBlank(),
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(if (siteFilterSaved) "Update site filter" else "Remember for site")
                        }
                        if (siteFilterSaved) {
                            OutlinedButton(
                                onClick = onClearSiteFilter,
                                modifier = Modifier.weight(1f),
                            ) {
                                Text("Use global")
                            }
                        }
                    }
                }
            }
        }

        Button(
            onClick = onDownload,
            enabled = state.canDownload,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors =
                ButtonDefaults.buttonColors(
                    containerColor = colors.accent,
                    contentColor = colors.onAccent,
                ),
        ) {
            if (state.isDownloading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = colors.onAccent,
                )
                Spacer(Modifier.width(8.dp))
                Text("${state.downloadCompletedCount}/${state.downloadTotalCount ?: "?"}")
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
                        GalleryDlRunner.isCandidateUrl(state.url),
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
            InlineNotice(
                "Install or update the Gallery DL engine from Settings before downloading.",
                colors.accent,
                colors,
            )
        }
        state.destinationDirectory?.takeIf(String::isNotBlank)?.let {
            InlineNotice("Saved to $it", colors.text, colors)
        }
    }
}

@Composable
private fun GalleryStatusStrip(
    state: GalleryDlViewModel.ViewState,
    colors: KirinGalleryColors,
) {
    val pending = state.queue.count { it.state == "pending" || it.state == "running" }
    val history = state.history.size
    Row(
        modifier =
            Modifier.fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .background(colors.panel, RoundedCornerShape(14.dp))
                .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        GalleryStatusChip("Engine", state.installedVersion?.let { "v$it" } ?: "Setup", colors)
        GalleryStatusChip("Queue", pending.toString(), colors)
        GalleryStatusChip("History", history.toString(), colors)
        GalleryStatusChip("Cookies", if (state.cookiesImported) "Ready" else "Off", colors)
    }
}

@Composable
private fun GalleryStatusChip(label: String, value: String, colors: KirinGalleryColors) {
    Surface(shape = RoundedCornerShape(10.dp), color = colors.panelAlt) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Text(label, color = colors.muted, fontSize = 10.sp)
            Text(value, color = colors.text, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun GalleryExtractorInspector(
    info: GalleryDlRunner.ExtractorInfo,
    state: GalleryDlViewModel.ViewState,
    colors: KirinGalleryColors,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = colors.panel,
        tonalElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (info.thumbnailUrl.isNotBlank()) {
                    AsyncImage(
                        model = info.thumbnailUrl,
                        contentDescription = null,
                        modifier = Modifier.size(62.dp).clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop,
                    )
                    Spacer(Modifier.width(12.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        info.title.ifBlank { info.label },
                        color = colors.text,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (info.author.isNotBlank()) {
                        Text(
                            info.author,
                            color = colors.muted,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Text(
                        if (info.supported) "Supported ✓" else "Unsupported",
                        color = if (info.supported) colors.success else colors.error,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            HorizontalDivider(color = colors.panelAlt)
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                InspectorPill("Extractor", state.extractorLabel ?: info.label, colors)
                info.mediaType.takeIf(String::isNotBlank)?.let {
                    InspectorPill("Type", it, colors)
                }
                val count = info.estimatedItemCount ?: info.scannedItemCount.takeIf { it > 0 }
                count?.let { InspectorPill("Items", it.toString(), colors) }
                InspectorPill("Cookies", if (info.cookiesLoaded) "Loaded" else "Not required", colors)
            }
            if (info.preflightError.isNotBlank()) {
                Text(info.preflightError, color = colors.error, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun InspectorPill(label: String, value: String, colors: KirinGalleryColors) {
    Surface(shape = RoundedCornerShape(50), color = colors.accentSoft) {
        Text(
            "$label: $value",
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
            color = colors.text,
            fontSize = 10.sp,
            maxLines = 1,
        )
    }
}

@Composable
private fun GalleryProgress(state: GalleryDlViewModel.ViewState, colors: KirinGalleryColors) {
    val total = state.downloadTotalCount
    val completed = state.downloadCompletedCount
    val progress =
        if (total != null && total > 0) (completed.toFloat() / total.toFloat()).coerceIn(0f, 1f)
        else null
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(state.downloadStage, color = colors.text, fontWeight = FontWeight.SemiBold)
            Text("$completed / ${total ?: "?"}", color = colors.muted)
        }
        if (progress != null) {
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(7.dp),
                color = colors.accent,
                trackColor = colors.panelAlt,
            )
        } else {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth().height(7.dp),
                color = colors.accent,
                trackColor = colors.panelAlt,
            )
        }
    }
}

@Composable
private fun GalleryQueueTab(
    state: GalleryDlViewModel.ViewState,
    colors: KirinGalleryColors,
    onRun: () -> Unit,
    onRemove: (String) -> Unit,
    onRetryFailed: () -> Unit,
    onClearFailed: () -> Unit,
    onClearCompleted: () -> Unit,
    onMove: (String, Int) -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var showMenu by remember { mutableStateOf(false) }
    val visible = remember(state.queue, query) {
        val q = query.trim()
        if (q.isBlank()) state.queue
        else state.queue.filter {
            it.url.contains(q, true) || it.extractor.contains(q, true) || it.state.contains(q, true)
        }
    }
    val runnable = state.queue.count { it.state == "pending" || it.state == "failed" }

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Queue", color = colors.text, fontSize = 23.sp, fontWeight = FontWeight.Bold)
                Text("${state.queue.size} Gallery job(s)", color = colors.muted, fontSize = 11.sp)
            }
            IconButton(onClick = { showMenu = true }) {
                Icon(Icons.Outlined.MoreVert, contentDescription = "Queue menu", tint = colors.accent)
            }
            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                DropdownMenuItem(
                    text = { Text("Retry failed") },
                    onClick = { showMenu = false; onRetryFailed() },
                    leadingIcon = { Icon(Icons.Outlined.Replay, null) },
                )
                DropdownMenuItem(
                    text = { Text("Clear failed") },
                    onClick = { showMenu = false; onClearFailed() },
                )
                DropdownMenuItem(
                    text = { Text("Clear completed") },
                    onClick = { showMenu = false; onClearCompleted() },
                )
            }
        }

        GallerySearchField(query, { query = it }, "Search queue", colors)

        Button(
            onClick = onRun,
            enabled = runnable > 0 && !state.isBusy,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = colors.accent, contentColor = colors.onAccent),
        ) {
            if (state.isQueueRunning) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = colors.onAccent)
                Spacer(Modifier.width(7.dp))
                Text("Running queue")
            } else {
                Icon(Icons.Outlined.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(7.dp))
                Text("Run Queue ($runnable)")
            }
        }

        if (visible.isEmpty()) {
            GalleryEmptyState("Queue is empty", "Add a URL or use Batch URLs from Download.", colors)
        } else {
            visible.forEach { item ->
                val realIndex = state.queue.indexOfFirst { it.id == item.id }
                GalleryQueueCard(
                    item = item,
                    colors = colors,
                    canMoveUp = realIndex > 0 && !state.isQueueRunning,
                    canMoveDown = realIndex >= 0 && realIndex < state.queue.lastIndex && !state.isQueueRunning,
                    onMoveUp = { onMove(item.id, -1) },
                    onMoveDown = { onMove(item.id, 1) },
                    onRemove = { onRemove(item.id) },
                    removeEnabled = !state.isQueueRunning,
                )
            }
        }
        state.errorMessage?.let { InlineNotice(it, colors.error, colors) }
        state.statusMessage?.let { InlineNotice(it, colors.success, colors) }
    }
}

@Composable
private fun GalleryQueueCard(
    item: com.junkfood.seal.util.GalleryDlStore.QueueRecord,
    colors: KirinGalleryColors,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit,
    removeEnabled: Boolean,
) {
    val statusColor =
        when (item.state) {
            "completed" -> colors.success
            "failed" -> colors.error
            "running" -> colors.accent
            else -> colors.muted
        }
    Surface(shape = RoundedCornerShape(14.dp), color = colors.panel) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(item.url, color = colors.text, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(
                    item.state.uppercase() + item.extractor.takeIf(String::isNotBlank)?.let { " • $it" }.orEmpty(),
                    color = statusColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                )
                item.error.takeIf(String::isNotBlank)?.let {
                    Text(it, color = colors.error, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(onClick = onMoveUp, enabled = canMoveUp, modifier = Modifier.size(30.dp)) {
                    Icon(Icons.Outlined.ArrowUpward, "Move up", tint = colors.muted, modifier = Modifier.size(17.dp))
                }
                IconButton(onClick = onMoveDown, enabled = canMoveDown, modifier = Modifier.size(30.dp)) {
                    Icon(Icons.Outlined.ArrowDownward, "Move down", tint = colors.muted, modifier = Modifier.size(17.dp))
                }
            }
            IconButton(onClick = onRemove, enabled = removeEnabled, modifier = Modifier.size(38.dp)) {
                Icon(Icons.Outlined.Delete, "Remove", tint = colors.muted)
            }
        }
    }
}

@Composable
private fun GalleryHistoryTab(
    state: GalleryDlViewModel.ViewState,
    colors: KirinGalleryColors,
    onReuse: (String) -> Unit,
    onClearAll: () -> Unit,
    onClearSuccessful: () -> Unit,
    onClearFailed: () -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var showMenu by remember { mutableStateOf(false) }
    val visible = remember(state.history, query) {
        val q = query.trim()
        if (q.isBlank()) state.history
        else state.history.filter {
            it.url.contains(q, true) || it.extractor.contains(q, true) || it.error.contains(q, true)
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("History", color = colors.text, fontSize = 23.sp, fontWeight = FontWeight.Bold)
                Text("${state.history.size} recent Gallery job(s)", color = colors.muted, fontSize = 11.sp)
            }
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Outlined.MoreVert, "History menu", tint = colors.accent)
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(text = { Text("Clear completed") }, onClick = { showMenu = false; onClearSuccessful() })
                    DropdownMenuItem(text = { Text("Clear failed") }, onClick = { showMenu = false; onClearFailed() })
                    DropdownMenuItem(text = { Text("Clear all") }, onClick = { showMenu = false; onClearAll() })
                }
            }
        }

        GallerySearchField(query, { query = it }, "Search history", colors)

        if (visible.isEmpty()) {
            GalleryEmptyState("No Gallery history", "Completed and failed Gallery jobs appear here.", colors)
        } else {
            visible.forEach { record ->
                Surface(shape = RoundedCornerShape(14.dp), color = colors.panel) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(record.url, color = colors.text, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                Text(
                                    buildString {
                                        append(if (record.success) "COMPLETED" else "FAILED")
                                        record.extractor.takeIf(String::isNotBlank)?.let { append(" • $it") }
                                        if (record.success && record.fileCount > 0) append(" • ${record.fileCount} file(s)")
                                    },
                                    color = if (record.success) colors.success else colors.error,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                            TextButton(onClick = { onReuse(record.url) }) { Text("Reuse") }
                        }
                        Text(
                            DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(record.finishedAt)),
                            color = colors.muted,
                            fontSize = 10.sp,
                        )
                        record.error.takeIf(String::isNotBlank)?.let {
                            Text(it, color = colors.error, fontSize = 10.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
        state.errorMessage?.let { InlineNotice(it, colors.error, colors) }
        state.statusMessage?.let { InlineNotice(it, colors.success, colors) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GalleryDownloadCenterSheet(
    state: GalleryDlViewModel.ViewState,
    colors: KirinGalleryColors,
    onDismiss: () -> Unit,
    onRun: () -> Unit,
    onRetryFailed: () -> Unit,
    onOpenQueue: () -> Unit,
    onOpenHistory: () -> Unit,
) {
    val running = state.queue.count { it.state == "running" }
    val pending = state.queue.count { it.state == "pending" }
    val failed = state.queue.count { it.state == "failed" }
    val completed = state.history.count { it.success }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp).padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Gallery Download Center", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(
                "Gallery DL jobs only. Media/yt-dlp remains in the main KirinDL Download Center.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                CenterMetric("Running", running, colors, Modifier.weight(1f))
                CenterMetric("Queue", pending, colors, Modifier.weight(1f))
                CenterMetric("Failed", failed, colors, Modifier.weight(1f))
                CenterMetric("Done", completed, colors, Modifier.weight(1f))
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onRun,
                    enabled = !state.isBusy && (pending + failed) > 0,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Outlined.PlayArrow, null)
                    Spacer(Modifier.width(5.dp))
                    Text("Run")
                }
                OutlinedButton(onClick = onRetryFailed, enabled = !state.isBusy && failed > 0, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Outlined.Replay, null)
                    Spacer(Modifier.width(5.dp))
                    Text("Retry")
                }
            }
            OutlinedButton(onClick = onOpenQueue, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Outlined.Queue, null)
                Spacer(Modifier.width(6.dp))
                Text("Open Gallery Queue")
            }
            OutlinedButton(onClick = onOpenHistory, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Outlined.History, null)
                Spacer(Modifier.width(6.dp))
                Text("Open Gallery History")
            }
            val recentQueue = state.queue.take(3)
            if (recentQueue.isNotEmpty()) {
                HorizontalDivider()
                Text("Up next", fontWeight = FontWeight.SemiBold)
                recentQueue.forEach { item ->
                    Text(
                        "${item.state.uppercase()} • ${item.url}",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}

@Composable
private fun CenterMetric(
    label: String,
    count: Int,
    colors: KirinGalleryColors,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier, shape = RoundedCornerShape(12.dp), color = colors.panelAlt) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(count.toString(), fontWeight = FontWeight.Bold, color = if (count > 0) colors.accent else colors.text)
            Text(label, fontSize = 9.sp, color = colors.muted, maxLines = 1)
        }
    }
}

@Composable
private fun GalleryBatchDialog(
    colors: KirinGalleryColors,
    onDismiss: () -> Unit,
    onAdd: (String) -> Unit,
) {
    var text by remember { mutableStateOf("") }
    val validCount = remember(text) {
        text.lines().map(String::trim).filter(String::isNotBlank).distinct().count(GalleryDlRunner::isCandidateUrl)
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Batch Gallery URLs") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("One URL per line. Valid links are added as separate Gallery jobs.")
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 6,
                    maxLines = 10,
                    placeholder = { Text("https://…\nhttps://…") },
                )
                Text("$validCount valid URL(s)", color = colors.accent, fontWeight = FontWeight.SemiBold)
            }
        },
        confirmButton = {
            TextButton(onClick = { onAdd(text) }, enabled = validCount > 0) { Text("Add $validCount") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
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
        leadingIcon = { Icon(Icons.Outlined.Search, null, tint = colors.accent) },
        trailingIcon =
            if (value.isNotBlank()) {
                {
                    IconButton(onClick = { onValueChange("") }) {
                        Icon(Icons.Outlined.Clear, "Clear search", tint = colors.muted)
                    }
                }
            } else {
                null
            },
        placeholder = { Text(placeholder, color = colors.muted) },
    )
}

@Composable
private fun InlineNotice(text: String, color: Color, colors: KirinGalleryColors) {
    Text(
        text,
        modifier =
            Modifier.fillMaxWidth()
                .background(colors.panelAlt, RoundedCornerShape(10.dp))
                .padding(11.dp),
        color = color,
        fontSize = 11.sp,
    )
}

@Composable
private fun GalleryEmptyState(title: String, description: String, colors: KirinGalleryColors) {
    Column(
        modifier =
            Modifier.fillMaxWidth()
                .background(colors.panel, RoundedCornerShape(16.dp))
                .padding(22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Text(title, color = colors.text, fontWeight = FontWeight.Bold)
        Text(description, color = colors.muted, fontSize = 11.sp)
    }
}
