package com.junkfood.seal.ui.page.search

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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.PlaylistAdd
import androidx.compose.material.icons.outlined.PlaylistPlay
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
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.junkfood.seal.ui.component.BackButton
import com.junkfood.seal.ui.page.downloadv2.configure.DownloadDialogViewModel
import com.junkfood.seal.ui.page.downloadv2.configure.DownloadDialogViewModel.Action
import com.junkfood.seal.util.DownloadUtil
import com.junkfood.seal.util.KirinSearchEngine
import com.junkfood.seal.util.KirinSearchStore
import com.junkfood.seal.util.makeToast
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KirinSearchPage(
    dialogViewModel: DownloadDialogViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToDownloads: () -> Unit,
    onNavigateToSavedSources: () -> Unit,
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val uriHandler = LocalUriHandler.current
    val keyboard = LocalSoftwareKeyboardController.current
    val scope = rememberCoroutineScope()

    var query by remember { mutableStateOf("") }
    var source by remember { mutableStateOf(KirinSearchStore.SearchSource.YOUTUBE) }
    var results by remember { mutableStateOf(emptyList<KirinSearchEngine.ResultItem>()) }
    var loading by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf("") }
    var searchRevision by remember { mutableStateOf(0) }
    var detailsTarget by remember { mutableStateOf<KirinSearchEngine.ResultItem?>(null) }
    val selectedUrls = remember { mutableStateListOf<String>() }

    val searches = remember(searchRevision) { KirinSearchStore.loadSearches(context) }

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

    fun downloadWithConfigure(url: String) {
        dialogViewModel.postAction(Action.ShowSheet(listOf(url)))
    }

    fun runSearch() {
        val clean = query.trim()
        if (clean.isBlank() || loading) return

        keyboard?.hide()
        if (KirinSearchEngine.looksLikeDirectVideoUrl(clean)) {
            downloadWithConfigure(clean)
            return
        }

        loading = true
        errorText = ""
        results = emptyList()
        selectedUrls.clear()

        scope.launch {
            KirinSearchEngine.search(clean, source)
                .onSuccess { items ->
                    results = items
                    KirinSearchStore.addSearch(context, clean, source)
                    searchRevision += 1
                    if (items.isEmpty()) errorText = "No results found"
                }
                .onFailure { throwable ->
                    results = emptyList()
                    errorText = throwable.message ?: "Search failed"
                }
            loading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Kirin Search") },
                navigationIcon = { BackButton(onNavigateBack) },
                actions = {
                    IconButton(onClick = onNavigateToSavedSources) {
                        Icon(
                            imageVector = Icons.Outlined.PlaylistPlay,
                            contentDescription = "Saved Sources",
                        )
                    }
                    IconButton(onClick = onNavigateToDownloads) {
                        Icon(
                            imageVector = Icons.Outlined.FileDownload,
                            contentDescription = "Download Center",
                        )
                    }
                },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                SearchIntroCard()
            }

            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (query.isNotBlank()) {
                                IconButton(onClick = { query = "" }) {
                                    Icon(
                                        imageVector = Icons.Outlined.Clear,
                                        contentDescription = "Clear search",
                                    )
                                }
                            }
                            IconButton(
                                onClick = { runSearch() },
                                enabled = query.isNotBlank() && !loading,
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Search,
                                    contentDescription = "Search",
                                )
                            }
                        }
                    },
                    placeholder = { Text("Search video, song or Bilibili media") },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { runSearch() }),
                    shape = MaterialTheme.shapes.large,
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    KirinSearchStore.SearchSource.entries.forEach { searchSource ->
                        FilterChip(
                            selected = source == searchSource,
                            onClick = {
                                if (source != searchSource) {
                                    source = searchSource
                                    results = emptyList()
                                    errorText = ""
                                    selectedUrls.clear()
                                }
                            },
                            label = { Text(searchSource.label) },
                        )
                    }
                }
            }

            if (KirinSearchEngine.looksLikeDirectVideoUrl(query)) {
                item {
                    DirectUrlCard(
                        url = query.trim(),
                        onConfigure = { downloadWithConfigure(query.trim()) },
                    )
                }
            }

            if (loading) {
                item {
                    LoadingSearchCard(source = source)
                }
            }

            if (errorText.isNotBlank()) {
                item {
                    SearchErrorCard(
                        message = errorText,
                        onRetry = { runSearch() },
                    )
                }
            }

            if (results.isNotEmpty()) {
                item {
                    ResultsHeader(
                        source = source,
                        count = results.size,
                        selectedCount = selectedUrls.size,
                        onQueueSelected = { queueUrls(selectedUrls.toList()) },
                        onClearSelection = { selectedUrls.clear() },
                    )
                }

                items(
                    items = results,
                    key = { item -> "${item.source.name}:${item.id}:${item.url}" },
                ) { item ->
                    SearchResultCard(
                        item = item,
                        selected = item.url in selectedUrls,
                        onSelectedChange = { checked ->
                            if (checked) {
                                if (item.url !in selectedUrls) selectedUrls.add(item.url)
                            } else {
                                selectedUrls.remove(item.url)
                            }
                        },
                        onDownload = { downloadWithConfigure(item.url) },
                        onQueue = { queueUrls(listOf(item.url)) },
                        onOpen = { uriHandler.openUri(item.url) },
                        onCopy = {
                            clipboard.setText(AnnotatedString(item.url))
                            context.makeToast("Link copied")
                        },
                        onDetails = { detailsTarget = item },
                    )
                }
            } else if (!loading && errorText.isBlank() && searches.isNotEmpty()) {
                item {
                    RecentSearchHeader(
                        onClearRecent = {
                            KirinSearchStore.clearRecentSearches(context)
                            searchRevision += 1
                        },
                    )
                }

                items(searches, key = { it.id }) { record ->
                    RecentSearchRow(
                        record = record,
                        onRun = {
                            query = record.query
                            source = record.source
                            runSearch()
                        },
                        onFavorite = {
                            KirinSearchStore.toggleFavorite(context, record.id)
                            searchRevision += 1
                        },
                        onDelete = {
                            KirinSearchStore.deleteSearch(context, record.id)
                            searchRevision += 1
                        },
                    )
                }
            } else if (!loading && errorText.isBlank() && query.isBlank()) {
                item {
                    EmptySearchCard()
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }

    detailsTarget?.let { item ->
        SearchDetailsDialog(
            item = item,
            onDismiss = { detailsTarget = null },
            onDownload = {
                detailsTarget = null
                downloadWithConfigure(item.url)
            },
            onOpen = { uriHandler.openUri(item.url) },
        )
    }
}

@Composable
private fun SearchIntroCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = "Search → Configure → Download",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text =
                    "Discovery stays separate from the downloader. Choose YouTube, YT Music or Bilibili, then send any result into KirinDL's normal configure or queue flow.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun DirectUrlCard(
    url: String,
    onConfigure: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
            ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(Icons.Outlined.FileDownload, contentDescription = null)
            Column(Modifier.weight(1f)) {
                Text(
                    text = "Direct video URL detected",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = url,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            TextButton(onClick = onConfigure) { Text("Configure") }
        }
    }
}

@Composable
private fun LoadingSearchCard(source: KirinSearchStore.SearchSource) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 3.dp)
            Column {
                Text("Searching ${source.label}…", fontWeight = FontWeight.SemiBold)
                Text(
                    text = "yt-dlp is fetching lightweight discovery metadata.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SearchErrorCard(
    message: String,
    onRetry: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.errorContainer,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Text("Search unavailable", fontWeight = FontWeight.SemiBold)
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            OutlinedButton(onClick = onRetry) { Text("Retry") }
        }
    }
}

@Composable
private fun ResultsHeader(
    source: KirinSearchStore.SearchSource,
    count: Int,
    selectedCount: Int,
    onQueueSelected: () -> Unit,
    onClearSelection: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = "${source.label} results",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "$count result${if (count == 1) "" else "s"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (selectedCount > 0) {
                TextButton(onClick = onClearSelection) { Text("Clear") }
                FilledTonalButton(onClick = onQueueSelected) {
                    Icon(
                        imageVector = Icons.Outlined.PlaylistAdd,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(" Queue $selectedCount")
                }
            }
        }
        HorizontalDivider()
    }
}

@Composable
private fun SearchResultCard(
    item: KirinSearchEngine.ResultItem,
    selected: Boolean,
    onSelectedChange: (Boolean) -> Unit,
    onDownload: () -> Unit,
    onQueue: () -> Unit,
    onOpen: () -> Unit,
    onCopy: () -> Unit,
    onDetails: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
            ),
    ) {
        Column {
            Box(modifier = Modifier.fillMaxWidth()) {
                AsyncImage(
                    model = item.thumbnail,
                    contentDescription = null,
                    modifier =
                        Modifier.fillMaxWidth()
                            .aspectRatio(16f / 9f)
                            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
                    contentScale = ContentScale.Crop,
                )

                Surface(
                    modifier = Modifier.align(Alignment.TopStart).padding(10.dp),
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                ) {
                    Text(
                        text = item.source.label,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                }

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
                            text = formatDuration(duration),
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
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = item.creator.ifBlank { item.extractor.ifBlank { item.source.label } },
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
                    Button(onClick = onDownload) {
                        Icon(
                            imageVector = Icons.Outlined.FileDownload,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(" Configure")
                    }
                    OutlinedButton(onClick = onQueue) {
                        Icon(
                            imageVector = Icons.Outlined.PlaylistAdd,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(" Queue")
                    }
                    Spacer(Modifier.weight(1f))
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
                                leadingIcon = { Icon(Icons.Outlined.OpenInNew, null) },
                                onClick = {
                                    menuExpanded = false
                                    onOpen()
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("Copy link") },
                                leadingIcon = { Icon(Icons.Outlined.ContentCopy, null) },
                                onClick = {
                                    menuExpanded = false
                                    onCopy()
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("Details") },
                                leadingIcon = { Icon(Icons.Outlined.Info, null) },
                                onClick = {
                                    menuExpanded = false
                                    onDetails()
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
private fun RecentSearchHeader(onClearRecent: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = "Recent & favourite searches",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "Favourites stay when normal recent searches are cleared.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        TextButton(onClick = onClearRecent) { Text("Clear recent") }
    }
}

@Composable
private fun RecentSearchRow(
    record: KirinSearchStore.SearchRecord,
    onRun: () -> Unit,
    onFavorite: () -> Unit,
    onDelete: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Row(
            modifier =
                Modifier.fillMaxWidth()
                    .clickable(onClick = onRun)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Column(
                modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = record.query,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = record.source.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onFavorite) {
                Icon(
                    imageVector =
                        if (record.favorite) Icons.Outlined.Star else Icons.Outlined.StarBorder,
                    contentDescription = if (record.favorite) "Unfavorite" else "Favorite",
                    tint = MaterialTheme.colorScheme.secondary,
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Outlined.Clear,
                    contentDescription = "Delete search",
                )
            }
        }
    }
}

@Composable
private fun EmptySearchCard() {
    Surface(
        modifier = Modifier.fillMaxWidth().widthIn(max = 720.dp),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = "Find media without leaving KirinDL",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "YouTube is the default. Switch to YT Music or Bilibili when needed.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SearchDetailsDialog(
    item: KirinSearchEngine.ResultItem,
    onDismiss: () -> Unit,
    onDownload: () -> Unit,
    onOpen: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(item.title, maxLines = 2, overflow = TextOverflow.Ellipsis) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DetailLine("Source", item.source.label)
                if (item.creator.isNotBlank()) DetailLine("Creator", item.creator)
                item.durationSeconds?.let { DetailLine("Duration", formatDuration(it)) }
                if (item.extractor.isNotBlank()) DetailLine("Extractor", item.extractor)
                DetailLine("URL", item.url)
            }
        },
        confirmButton = {
            TextButton(onClick = onDownload) { Text("Configure") }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onOpen) { Text("Open") }
                TextButton(onClick = onDismiss) { Text("Close") }
            }
        },
    )
}

@Composable
private fun DetailLine(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun formatDuration(totalSeconds: Int): String {
    val safe = totalSeconds.coerceAtLeast(0)
    val hours = safe / 3600
    val minutes = (safe % 3600) / 60
    val seconds = safe % 60
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%d:%02d".format(minutes, seconds)
    }
}
