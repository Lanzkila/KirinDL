package com.junkfood.seal.ui.page.tools.mangaconverter

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.junkfood.seal.ui.component.BackButton
import com.junkfood.seal.ui.page.tools.ConverterPreferences

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MangaConverterPage(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val viewModel = remember { MangaConverterViewModel() }
    val state by viewModel.state.collectAsState()

    DisposableEffect(viewModel) { onDispose { viewModel.close() } }

    val picker =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
            viewModel.addUris(context, uris)
        }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manga Converter") },
                navigationIcon = { BackButton(onNavigateBack) },
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding =
                PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = padding.calculateTopPadding() + 12.dp,
                    bottom = padding.calculateBottomPadding() + 24.dp,
                ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(
                    "ZIP / CBZ manga conversion",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    "Supported: ZIP → CBZ, CBZ → ZIP, ZIP → PDF, CBZ → PDF",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(
                        onClick = {
                            picker.launch(
                                arrayOf(
                                    "application/zip",
                                    "application/x-zip-compressed",
                                    "application/x-cbz",
                                    "application/octet-stream",
                                )
                            )
                        },
                        enabled = !state.isRunning,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(if (state.inputs.isEmpty()) "Select ZIP / CBZ" else "Add files")
                    }
                    OutlinedButton(
                        onClick = viewModel::clearInputs,
                        enabled = state.inputs.isNotEmpty() && !state.isRunning,
                    ) {
                        Text("Clear")
                    }
                }
                Text(
                    "Output: ${ConverterPreferences.mangaDirectory().absolutePath}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }

            if (state.inputs.isNotEmpty()) {
                items(state.inputs, key = { it.uri.toString() }) { input ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                input.name,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            input.sizeBytes?.let {
                                Text(
                                    formatBytes(it),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        OutlinedButton(
                            onClick = { viewModel.removeInput(input.uri) },
                            enabled = !state.isRunning,
                        ) {
                            Text("Remove")
                        }
                    }
                }
            }

            item {
                HorizontalDivider()
                Spacer(Modifier.height(6.dp))
                MangaModeDropdown(
                    selected = state.settings.mode,
                    enabled = !state.isRunning,
                    onSelected = { mode -> viewModel.updateSettings { it.copy(mode = mode) } },
                )
                Spacer(Modifier.height(10.dp))
                ToggleRow(
                    label = "Natural page sorting",
                    checked = state.settings.naturalSort,
                    enabled = !state.isRunning,
                ) { checked -> viewModel.updateSettings { it.copy(naturalSort = checked) } }
                ToggleRow(
                    label = "Remove junk files",
                    checked = state.settings.removeJunkFiles,
                    enabled = !state.isRunning,
                ) { checked -> viewModel.updateSettings { it.copy(removeJunkFiles = checked) } }

                if (state.settings.mode == MangaConversionMode.ZIP_TO_CBZ) {
                    ToggleRow(
                        label = "Create ComicInfo.xml",
                        checked = state.settings.includeComicInfo,
                        enabled = !state.isRunning,
                    ) { checked -> viewModel.updateSettings { it.copy(includeComicInfo = checked) } }
                    if (state.settings.includeComicInfo) {
                        OutlinedTextField(
                            value = state.settings.title,
                            onValueChange = { value -> viewModel.updateSettings { it.copy(title = value) } },
                            label = { Text("Manga title") },
                            enabled = !state.isRunning,
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = state.settings.author,
                            onValueChange = { value -> viewModel.updateSettings { it.copy(author = value) } },
                            label = { Text("Author / writer") },
                            enabled = !state.isRunning,
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }

            item {
                HorizontalDivider()
                Spacer(Modifier.height(6.dp))
                if (state.isRunning || state.progress != null) {
                    Text(
                        state.stage,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    if (state.currentItem.isNotBlank()) {
                        Text(
                            state.currentItem,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    state.progress?.let {
                        LinearProgressIndicator(
                            progress = { it.coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                }

                state.errorMessage?.let {
                    Text(
                        it,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                }
                if (state.completed.isNotEmpty()) {
                    Text(
                        "Completed: ${state.completed.size}",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                }

                if (state.isRunning) {
                    OutlinedButton(onClick = viewModel::cancel, modifier = Modifier.fillMaxWidth()) {
                        Text("Cancel conversion")
                    }
                } else {
                    Button(
                        onClick = { viewModel.convertAll(context) },
                        enabled = state.inputs.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Convert ${state.inputs.size.coerceAtLeast(1)} file(s)")
                    }
                }
            }
        }
    }
}

@Composable
private fun MangaModeDropdown(
    selected: MangaConversionMode,
    enabled: Boolean,
    onSelected: (MangaConversionMode) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        Text("Conversion", style = MaterialTheme.typography.labelMedium)
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = { expanded = true },
                enabled = enabled,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(selected.label)
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                MangaConversionMode.entries.forEach { mode ->
                    DropdownMenuItem(
                        text = { Text(mode.label) },
                        onClick = {
                            expanded = false
                            onSelected(mode)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, enabled: Boolean, onChecked: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, modifier = Modifier.weight(1f))
        Spacer(Modifier.width(8.dp))
        Switch(checked = checked, onCheckedChange = onChecked, enabled = enabled)
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes < 1024L) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024.0) return "%.1f KB".format(kb)
    val mb = kb / 1024.0
    if (mb < 1024.0) return "%.1f MB".format(mb)
    return "%.2f GB".format(mb / 1024.0)
}
