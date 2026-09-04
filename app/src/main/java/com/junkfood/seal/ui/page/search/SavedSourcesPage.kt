package com.junkfood.seal.ui.page.search

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.PlaylistPlay
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.junkfood.seal.ui.component.BackButton
import com.junkfood.seal.ui.page.downloadv2.configure.DownloadDialogViewModel
import com.junkfood.seal.ui.page.downloadv2.configure.DownloadDialogViewModel.Action
import com.junkfood.seal.util.DownloadUtil
import com.junkfood.seal.util.SavedSourceStore
import com.junkfood.seal.util.SavedSourcesEngine
import com.junkfood.seal.util.makeToast
import java.text.DateFormat
import java.util.Date
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedSourcesPage(
    dialogViewModel: DownloadDialogViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToDownloads: () -> Unit,
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val uriHandler = LocalUriHandler.current
    val scope = rememberCoroutineScope()

    var sourceRevision by remember { mutableStateOf(0) }
    var selectedSourceId by remember { mutableStateOf<String?>(null) }
    var filterText by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<SavedSourceStore.SavedSource?>(null) }
    var deleteTarget by remember { mutableStateOf<SavedSourceStore.SavedSource?>(null) }

    var browseItems by remember { mutableStateOf(emptyList<SavedSourceStore.SourceItem>()) }
    var browseLoading by remember { mutableStateOf(false) }
    var browseError by remember { mutableStateOf("") }
    var browseTitle by remember { mutableStateOf("") }
    var browseCreator by remember { mutableStateOf("") }
    var browseFromCache by remember { mutableStateOf(false) }
    var refreshRevision by remember { mutableStateOf(0) }
    val selectedUrls = remember { mutableStateListOf<String>() }

    val sources = remember(sourceRevision) { SavedSourceStore.loadSources(context) }
    val selectedSource = sources.firstOrNull { it.id == selectedSourceId }
    val lastOpenedSource =
        remember(sourceRevision) {
            val id = SavedSourceStore.getLastOpenedSourceId(context)
            SavedSourceStore.loadSources(context).firstOrNull { it.id == id }
        }

    val visibleSources =
        remember(sources, filterText) {
            val query = filterText.trim()
            if (query.isBlank()) {
                sources
            } else {
                sources.filter { source ->
                    source.displayTitle.contains(query, ignoreCase = true) ||
                        source.url.contains(query, ignoreCase = true) ||
                        source.kind.label.contains(query, ignoreCase = true)
                }
            }
        }

    fun queueUrls(urls: List<String>) {
        val clean = urls.distinct().filter { it.isNotBlank() }
        if (clean.isEmpty()) return
        dialogViewModel.postAction(
            Action.DownloadWithPreset(
                urlList = clean,
                preferences = DownloadUtil.DownloadPreferences.createFromPreferences(),
            ),
        )
        selectedUrls.clear()
        context.makeToast(
            "Added ${clean.size} item${if (clean.size == 1) "" else "s"} to Media queue",
        )
    }

    fun configure(url: String) {
        dialogViewModel.postAction(Action.ShowSheet(listOf(url)))
    }

    fun openSource(source: SavedSourceStore.SavedSource) {
        SavedSourceStore.setLastOpenedSource(context, source.id)
        selectedUrls.clear()
        browseItems = emptyList()
        browseError = ""
        browseTitle = source.displayTitle
        browseCreator = ""
        browseFromCache = false
        refreshRevision = 0
        selectedSourceId = source.id
    }

    fun closeBrowser() {
        selectedSourceId = null
        browseItems = emptyList()
        browseError = ""
        selectedUrls.clear()
        sourceRevision += 1
    }

    BackHandler(enabled = selectedSourceId != null) { closeBrowser() }

    LaunchedEffect(selectedSourceId, refreshRevision) {
        val source = selectedSource ?: return@LaunchedEffect
        browseLoading = true
        browseError = ""
        val forceRefresh = refreshRevision > 0
        SavedSourcesEngine.browse(
                context = context,
                source = source,
                forceRefresh = forceRefresh,
            )
            .onSuccess { result ->
                browseItems = result.items
                browseTitle = result.title.ifBlank { source.displayTitle }
                browseCreator = result.creator
                browseFromCache = result.fromCache
                selectedUrls.clear()
                sourceRevision += 1
                if (result.items.isEmpty()) browseError = "No media found in this source"
            }
            .onFailure { throwable ->
                browseItems = emptyList()
                browseError = throwable.message ?: "Could not open this source"
            }
        browseLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (selectedSource != null) browseTitle.ifBlank { selectedSource.displayTitle }
                        else "Saved Sources",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    BackButton {
                        if (selectedSourceId != null) closeBrowser() else onNavigateBack()
                    }
                },
                actions = {
                    if (selectedSource != null) {
                        IconButton(
                            onClick = { refreshRevision += 1 },
                            enabled = !browseLoading,
                        ) {
                            Icon(Icons.Outlined.Refresh, contentDescription = "Refresh source")
                        }
                    } else {
                        IconButton(onClick = { showAddDialog = true }) {
                            Icon(Icons.Outlined.Add, contentDescription = "Add source")
                        }
                    }
                    IconButton(onClick = onNavigateToDownloads) {
                        Icon(Icons.Outlined.FileDownload, contentDescription = "Download Center")
                    }
                },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
            )
        },
    ) { padding ->
        if (selectedSource == null) {
            SavedSourcesList(
                modifier = Modifier.fillMaxSize().padding(padding),
                sources = visibleSources,
                totalCount = sources.size,
                filterText = filterText,
                onFilterChange = { filterText = it },
                lastOpenedSource = lastOpenedSource,
                onAdd = { showAddDialog = true },
                onOpen = ::openSource,
                onRename = { renameTarget = it },
                onTogglePinned = {
                    SavedSourceStore.togglePinned(context, it.id)
                    sourceRevision += 1
                },
                onMoveUp = {
                    SavedSourceStore.moveSource(context, it.id, -1)
                    sourceRevision += 1
                },
                onMoveDown = {
                    SavedSourceStore.moveSource(context, it.id, 1)
                    sourceRevision += 1
                },
                onDelete = { deleteTarget = it },
                onOpenExternal = { uriHandler.openUri(it.url) },
            )
        } else {
            SavedSourceBrowser(
                modifier = Modifier.fillMaxSize().padding(padding),
                source = selectedSource,
                title = browseTitle,
                creator = browseCreator,
                fromCache = browseFromCache,
                items = browseItems,
                loading = browseLoading,
                errorText = browseError,
                selectedUrls = selectedUrls,
                onSelectedChange = { url, checked ->
                    if (checked) {
                        if (url !in selectedUrls) selectedUrls.add(url)
                    } else {
                        selectedUrls.remove(url)
                    }
                },
                onConfigure = ::configure,
                onQueue = { queueUrls(listOf(it)) },
                onQueueSelected = { queueUrls(selectedUrls.toList()) },
                onClearSelection = { selectedUrls.clear() },
                onRetry = { refreshRevision += 1 },
                onOpen = { uriHandler.openUri(it) },
                onCopy = {
                    clipboard.setText(AnnotatedString(it))
                    context.makeToast("Link copied")
                },
            )
        }
    }

    if (showAddDialog) {
        AddSourceDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { url, nickname ->
                val kind = SavedSourcesEngine.classifySourceUrl(url)
                if (kind == null) {
                    SavedSourcesEngine.validationMessage(url)
                } else {
                    val source = SavedSourceStore.addSource(context, url, kind, nickname)
                    sourceRevision += 1
                    showAddDialog = false
                    openSource(source)
                    null
                }
            },
        )
    }

    renameTarget?.let { source ->
        RenameSourceDialog(
            source = source,
            onDismiss = { renameTarget = null },
            onSave = { newName ->
                SavedSourceStore.renameSource(context, source.id, newName)
                sourceRevision += 1
                renameTarget = null
            },
        )
    }

    deleteTarget?.let { source ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Delete saved source?") },
            text = { Text(source.displayTitle) },
            confirmButton = {
                TextButton(
                    onClick = {
                        SavedSourceStore.deleteSource(context, source.id)
                        sourceRevision += 1
                        deleteTarget = null
                    },
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun SavedSourcesList(
    modifier: Modifier,
    sources: List<SavedSourceStore.SavedSource>,
    totalCount: Int,
    filterText: String,
    onFilterChange: (String) -> Unit,
    lastOpenedSource: SavedSourceStore.SavedSource?,
    onAdd: () -> Unit,
    onOpen: (SavedSourceStore.SavedSource) -> Unit,
    onRename: (SavedSourceStore.SavedSource) -> Unit,
    onTogglePinned: (SavedSourceStore.SavedSource) -> Unit,
    onMoveUp: (SavedSourceStore.SavedSource) -> Unit,
    onMoveDown: (SavedSourceStore.SavedSource) -> Unit,
    onDelete: (SavedSourceStore.SavedSource) -> Unit,
    onOpenExternal: (SavedSourceStore.SavedSource) -> Unit,
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            SavedSourcesIntroCard(onAdd = onAdd)
        }

        if (lastOpenedSource != null) {
            item {
                ContinueSourceCard(source = lastOpenedSource, onOpen = { onOpen(lastOpenedSource) })
            }
        }

        if (totalCount > 0) {
            item {
                OutlinedTextField(
                    value = filterText,
                    onValueChange = onFilterChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                    trailingIcon =
                        if (filterText.isNotBlank()) {
                            {
                                IconButton(onClick = { onFilterChange("") }) {
                                    Icon(Icons.Outlined.Clear, contentDescription = "Clear filter")
                                }
                            }
                        } else {
                            null
                        },
                    placeholder = { Text("Find saved channel or playlist") },
                    shape = MaterialTheme.shapes.large,
                )
            }
        }

        if (sources.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors =
                        CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer,
                        ),
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(Icons.Outlined.FolderOpen, contentDescription = null)
                        Text(
                            if (totalCount == 0) "No saved sources yet" else "No matching sources",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            if (totalCount == 0) {
                                "Save a YouTube channel/playlist, YT Music collection or Bilibili space/collection."
                            } else {
                                "Try another filter."
                            },
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        items(sources, key = { it.id }) { source ->
            SavedSourceCard(
                source = source,
                onOpen = { onOpen(source) },
                onRename = { onRename(source) },
                onTogglePinned = { onTogglePinned(source) },
                onMoveUp = { onMoveUp(source) },
                onMoveDown = { onMoveDown(source) },
                onDelete = { onDelete(source) },
                onOpenExternal = { onOpenExternal(source) },
            )
        }

        item { Spacer(Modifier.height(20.dp)) }
    }
}

@Composable
private fun SavedSourcesIntroCard(onAdd: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
            ),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(Icons.Outlined.PlaylistPlay, contentDescription = null)
                Column {
                    Text(
                        "Saved Sources",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        "Your own channel and playlist browser — not a feed.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Text(
                "Only collection URLs can be saved. Direct video URLs stay in Home or Kirin Search.",
                style = MaterialTheme.typography.bodyMedium,
            )
            FilledTonalButton(onClick = onAdd) {
                Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Text(" Add Source")
            }
        }
    }
}

@Composable
private fun ContinueSourceCard(
    source: SavedSourceStore.SavedSource,
    onOpen: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth().clip(MaterialTheme.shapes.large).clickable(onClick = onOpen),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(Icons.Outlined.FolderOpen, contentDescription = null)
            Column(modifier = Modifier.weight(1f)) {
                Text("Continue last opened", style = MaterialTheme.typography.labelMedium)
                Text(
                    source.displayTitle,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun SavedSourceCard(
    source: SavedSourceStore.SavedSource,
    onOpen: () -> Unit,
    onRename: () -> Unit,
    onTogglePinned: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDelete: () -> Unit,
    onOpenExternal: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
            ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AsyncImage(
                model = source.thumbnail.takeIf { it.isNotBlank() },
                contentDescription = null,
                modifier = Modifier.size(72.dp).clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop,
            )

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    if (source.pinned) {
                        Icon(
                            Icons.Outlined.Star,
                            contentDescription = "Pinned",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                    Text(
                        source.displayTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    source.kind.label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    source.url,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (source.lastFetchedAt > 0L) {
                    Text(
                        "${source.itemCount} cached items • ${formatTimestamp(source.lastFetchedAt)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Box {
                IconButton(
                    onClick = {
                        menuExpanded = true
                    },
                ) {
                    Icon(Icons.Outlined.MoreVert, contentDescription = "Source actions")
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                ) {
                    DropdownMenuItem(
                        text = { Text("Open source") },
                        leadingIcon = { Icon(Icons.Outlined.FolderOpen, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onOpen()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Open original") },
                        leadingIcon = { Icon(Icons.Outlined.OpenInNew, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onOpenExternal()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Rename") },
                        leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onRename()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(if (source.pinned) "Unpin" else "Pin") },
                        leadingIcon = {
                            Icon(
                                if (source.pinned) Icons.Outlined.Star else Icons.Outlined.StarBorder,
                                contentDescription = null,
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            onTogglePinned()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Move up") },
                        leadingIcon = { Icon(Icons.Outlined.ArrowUpward, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onMoveUp()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Move down") },
                        leadingIcon = { Icon(Icons.Outlined.ArrowDownward, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onMoveDown()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Delete") },
                        leadingIcon = { Icon(Icons.Outlined.DeleteSweep, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onDelete()
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun SavedSourceBrowser(
    modifier: Modifier,
    source: SavedSourceStore.SavedSource,
    title: String,
    creator: String,
    fromCache: Boolean,
    items: List<SavedSourceStore.SourceItem>,
    loading: Boolean,
    errorText: String,
    selectedUrls: List<String>,
    onSelectedChange: (String, Boolean) -> Unit,
    onConfigure: (String) -> Unit,
    onQueue: (String) -> Unit,
    onQueueSelected: () -> Unit,
    onClearSelection: () -> Unit,
    onRetry: () -> Unit,
    onOpen: (String) -> Unit,
    onCopy: (String) -> Unit,
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            BrowserHeaderCard(
                source = source,
                title = title,
                creator = creator,
                fromCache = fromCache,
                itemCount = items.size,
            )
        }

        if (loading) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                ) {
                    Row(
                        modifier = Modifier.padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(28.dp))
                        Column {
                            Text("Opening source…", fontWeight = FontWeight.SemiBold)
                            Text(
                                "Reading ${source.kind.label} with yt-dlp",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }

        if (errorText.isNotBlank() && !loading) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(errorText, color = MaterialTheme.colorScheme.onErrorContainer)
                        OutlinedButton(onClick = onRetry) {
                            Icon(Icons.Outlined.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text(" Retry")
                        }
                    }
                }
            }
        }

        if (items.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column {
                        Text(
                            "${items.size} media items",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                        if (selectedUrls.isNotEmpty()) {
                            Text(
                                "${selectedUrls.size} selected",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        if (selectedUrls.isNotEmpty()) {
                            TextButton(onClick = onClearSelection) { Text("Clear") }
                            FilledTonalButton(onClick = onQueueSelected) {
                                Text("Queue ${selectedUrls.size}")
                            }
                        }
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
            }

            items(items, key = { "${it.id}:${it.url}" }) { item ->
                SavedSourceMediaCard(
                    item = item,
                    selected = item.url in selectedUrls,
                    onSelectedChange = { onSelectedChange(item.url, it) },
                    onConfigure = { onConfigure(item.url) },
                    onQueue = { onQueue(item.url) },
                    onOpen = { onOpen(item.url) },
                    onCopy = { onCopy(item.url) },
                )
            }
        }

        item { Spacer(Modifier.height(22.dp)) }
    }
}

@Composable
private fun BrowserHeaderCard(
    source: SavedSourceStore.SavedSource,
    title: String,
    creator: String,
    fromCache: Boolean,
    itemCount: Int,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f),
            ),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AsyncImage(
                model = source.thumbnail.takeIf { it.isNotBlank() },
                contentDescription = null,
                modifier = Modifier.size(76.dp).clip(RoundedCornerShape(14.dp)),
                contentScale = ContentScale.Crop,
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(
                    title.ifBlank { source.displayTitle },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    creator.ifBlank { source.kind.label },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "$itemCount items • ${if (fromCache) "Cached" else "Fresh"}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun SavedSourceMediaCard(
    item: SavedSourceStore.SourceItem,
    selected: Boolean,
    onSelectedChange: (Boolean) -> Unit,
    onConfigure: () -> Unit,
    onQueue: () -> Unit,
    onOpen: () -> Unit,
    onCopy: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column {
            Box(modifier = Modifier.fillMaxWidth()) {
                AsyncImage(
                    model = item.thumbnail,
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
                    contentScale = ContentScale.Crop,
                )
                Surface(
                    modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                ) {
                    Checkbox(
                        checked = selected,
                        onCheckedChange = onSelectedChange,
                        modifier = Modifier.size(38.dp),
                    )
                }
                item.durationSeconds?.let { duration ->
                    Surface(
                        modifier = Modifier.align(Alignment.BottomEnd).padding(10.dp),
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.inverseSurface.copy(alpha = 0.9f),
                    ) {
                        Text(
                            formatDuration(duration),
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.inverseOnSurface,
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    item.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    item.creator.ifBlank { item.extractor },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(onClick = onConfigure) {
                        Icon(Icons.Outlined.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text(" Configure")
                    }
                    FilledTonalButton(onClick = onQueue) { Text("Queue") }
                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(Icons.Outlined.MoreVert, contentDescription = "More actions")
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text("Open source") },
                                leadingIcon = { Icon(Icons.Outlined.OpenInNew, contentDescription = null) },
                                onClick = {
                                    menuExpanded = false
                                    onOpen()
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("Copy link") },
                                leadingIcon = { Icon(Icons.Outlined.ContentCopy, contentDescription = null) },
                                onClick = {
                                    menuExpanded = false
                                    onCopy()
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AddSourceDialog(
    onDismiss: () -> Unit,
    onAdd: (url: String, nickname: String) -> String?,
) {
    var url by remember { mutableStateOf("") }
    var nickname by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Saved Source") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Channel / playlist / collection URLs only. Direct video URLs are intentionally rejected.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = url,
                    onValueChange = {
                        url = it
                        errorText = ""
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Source URL") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = nickname,
                    onValueChange = { nickname = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Nickname (optional)") },
                    singleLine = true,
                )
                if (errorText.isNotBlank()) {
                    Text(
                        errorText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val error = onAdd(url.trim(), nickname.trim())
                    if (error != null) errorText = error
                },
                enabled = url.isNotBlank(),
            ) {
                Text("Add")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun RenameSourceDialog(
    source: SavedSourceStore.SavedSource,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var name by remember(source.id) { mutableStateOf(source.customName.ifBlank { source.displayTitle }) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename Source") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Display name") },
            )
        },
        confirmButton = {
            TextButton(onClick = { onSave(name.trim()) }, enabled = name.isNotBlank()) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

private fun formatTimestamp(timestamp: Long): String =
    if (timestamp <= 0L) {
        "Never refreshed"
    } else {
        DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(timestamp))
    }

private fun formatDuration(seconds: Int): String {
    val safe = seconds.coerceAtLeast(0)
    val hours = safe / 3600
    val minutes = (safe % 3600) / 60
    val secs = safe % 60
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, secs)
    } else {
        "%d:%02d".format(minutes, secs)
    }
}
