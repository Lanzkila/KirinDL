package com.junkfood.seal.ui.page.tools.mediaconverter

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
fun MediaConverterPage(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val viewModel = remember { MediaConverterViewModel() }
    val state by viewModel.state.collectAsState()

    DisposableEffect(viewModel) { onDispose { viewModel.close() } }

    val picker =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
            viewModel.addUris(context, uris)
        }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Media Converter") },
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
                SectionTitle("Input")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(
                        onClick = { picker.launch(arrayOf("video/*", "audio/*")) },
                        enabled = !state.isRunning,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(if (state.inputs.isEmpty()) "Select media" else "Add media")
                    }
                    OutlinedButton(
                        onClick = viewModel::clearInputs,
                        enabled = state.inputs.isNotEmpty() && !state.isRunning,
                    ) {
                        Text("Clear")
                    }
                }
                Text(
                    text = "Output: ${ConverterPreferences.mediaDirectory().absolutePath}",
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
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontWeight = FontWeight.Medium,
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
                SectionTitle("Conversion")
                EnumDropdown(
                    label = "Preset",
                    value = state.preset.label,
                    values = MediaConverterPreset.entries,
                    text = { it.label },
                    enabled = !state.isRunning,
                    onSelected = viewModel::setPreset,
                )
                Spacer(Modifier.height(8.dp))
                EnumDropdown(
                    label = "Output type",
                    value = state.settings.outputType.label,
                    values = MediaOutputType.entries,
                    text = { it.label },
                    enabled = !state.isRunning,
                    onSelected = { type ->
                        viewModel.updateSettings { it.copy(outputType = type, fastRemux = false) }
                    },
                )
                Spacer(Modifier.height(8.dp))
                if (state.settings.outputType == MediaOutputType.VIDEO) {
                    EnumDropdown(
                        label = "Container",
                        value = state.settings.videoContainer.label,
                        values = VideoContainer.entries,
                        text = { it.label },
                        enabled = !state.isRunning,
                        onSelected = { container ->
                            viewModel.updateSettings { current ->
                                when (container) {
                                    VideoContainer.WEBM ->
                                        current.copy(
                                            videoContainer = container,
                                            videoCodec = VideoCodec.VP9,
                                            audioCodec = AudioCodec.OPUS,
                                            fastRemux = false,
                                        )
                                    else -> current.copy(videoContainer = container)
                                }
                            }
                        },
                    )
                } else {
                    EnumDropdown(
                        label = "Audio format",
                        value = state.settings.audioContainer.label,
                        values = AudioContainer.entries,
                        text = { it.label },
                        enabled = !state.isRunning,
                        onSelected = { container ->
                            viewModel.updateSettings { current ->
                                val codec =
                                    when (container) {
                                        AudioContainer.MP3 -> AudioCodec.MP3
                                        AudioContainer.M4A -> AudioCodec.AAC
                                        AudioContainer.OPUS -> AudioCodec.OPUS
                                        AudioContainer.OGG -> AudioCodec.VORBIS
                                        AudioContainer.FLAC -> AudioCodec.FLAC
                                        AudioContainer.WAV -> AudioCodec.PCM
                                    }
                                current.copy(audioContainer = container, audioCodec = codec)
                            }
                        },
                    )
                }

                Spacer(Modifier.height(10.dp))
                OutlinedButton(
                    onClick = viewModel::toggleAdvanced,
                    enabled = !state.isRunning,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(if (state.showAdvanced) "Hide advanced settings" else "Advanced settings")
                }
            }

            if (state.showAdvanced) {
                item {
                    AdvancedSettings(
                        settings = state.settings,
                        enabled = !state.isRunning,
                        onUpdate = viewModel::updateSettings,
                    )
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
                    state.progress?.let { progress ->
                        LinearProgressIndicator(
                            progress = { progress.coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                }

                state.errorMessage?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
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
private fun AdvancedSettings(
    settings: MediaConverterSettings,
    enabled: Boolean,
    onUpdate: ((MediaConverterSettings) -> MediaConverterSettings) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionTitle("Advanced")
        if (settings.outputType == MediaOutputType.VIDEO) {
            EnumDropdown(
                label = "Video codec",
                value = settings.videoCodec.label,
                values = VideoCodec.entries,
                text = { it.label },
                enabled = enabled,
                onSelected = { codec -> onUpdate { it.copy(videoCodec = codec, fastRemux = codec == VideoCodec.COPY) } },
            )
            NumberField(
                label = "Video bitrate (kbps, blank = quality mode)",
                value = settings.videoBitrateKbps,
                enabled = enabled && !settings.fastRemux,
                onValue = { value -> onUpdate { it.copy(videoBitrateKbps = value) } },
            )
            NumberField(
                label = "Resolution height (e.g. 1080)",
                value = settings.resolutionHeight,
                enabled = enabled && !settings.fastRemux,
                onValue = { value -> onUpdate { it.copy(resolutionHeight = value) } },
            )
            NumberField(
                label = "FPS",
                value = settings.fps,
                enabled = enabled && !settings.fastRemux,
                onValue = { value -> onUpdate { it.copy(fps = value) } },
            )
            NumberField(
                label = "CRF / quality",
                value = settings.crf,
                enabled = enabled && !settings.fastRemux,
                onValue = { value -> onUpdate { it.copy(crf = value?.coerceIn(0, 51)) } },
            )
        }

        EnumDropdown(
            label = "Audio codec",
            value = settings.audioCodec.label,
            values = AudioCodec.entries,
            text = { it.label },
            enabled = enabled && !(settings.outputType == MediaOutputType.VIDEO && settings.removeAudio),
            onSelected = { codec -> onUpdate { it.copy(audioCodec = codec) } },
        )
        NumberField(
            label = "Audio bitrate (kbps)",
            value = settings.audioBitrateKbps,
            enabled = enabled && settings.audioCodec != AudioCodec.COPY,
            onValue = { value -> onUpdate { it.copy(audioBitrateKbps = value) } },
        )
        NumberField(
            label = "Sample rate (Hz)",
            value = settings.sampleRate,
            enabled = enabled,
            onValue = { value -> onUpdate { it.copy(sampleRate = value) } },
        )
        NumberField(
            label = "Audio channels (1 mono / 2 stereo)",
            value = settings.audioChannels,
            enabled = enabled,
            onValue = { value -> onUpdate { it.copy(audioChannels = value?.coerceIn(1, 8)) } },
        )

        DecimalField(
            label = "Trim start (seconds)",
            value = settings.trimStartSeconds,
            enabled = enabled,
            onValue = { value -> onUpdate { it.copy(trimStartSeconds = value) } },
        )
        DecimalField(
            label = "Trim end (seconds)",
            value = settings.trimEndSeconds,
            enabled = enabled,
            onValue = { value -> onUpdate { it.copy(trimEndSeconds = value) } },
        )

        ToggleRow("Keep metadata", settings.keepMetadata, enabled) {
            onUpdate { current -> current.copy(keepMetadata = it) }
        }
        if (settings.outputType == MediaOutputType.VIDEO) {
            ToggleRow("Keep subtitles", settings.keepSubtitles, enabled) {
                onUpdate { current -> current.copy(keepSubtitles = it) }
            }
            ToggleRow("Remove audio", settings.removeAudio, enabled) {
                onUpdate { current -> current.copy(removeAudio = it) }
            }
            ToggleRow("Fast remux / copy streams", settings.fastRemux, enabled) {
                onUpdate { current ->
                    if (it) {
                        current.copy(
                            fastRemux = true,
                            videoCodec = VideoCodec.COPY,
                            audioCodec = AudioCodec.COPY,
                            videoBitrateKbps = null,
                            crf = null,
                            resolutionHeight = null,
                            fps = null,
                        )
                    } else {
                        current.copy(fastRemux = false, videoCodec = VideoCodec.H264, audioCodec = AudioCodec.AAC)
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
}

@Composable
private fun <T> EnumDropdown(
    label: String,
    value: String,
    values: List<T>,
    text: (T) -> String,
    enabled: Boolean,
    onSelected: (T) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium)
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = { expanded = true },
                enabled = enabled,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(value, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                values.forEach { item ->
                    DropdownMenuItem(
                        text = { Text(text(item)) },
                        onClick = {
                            expanded = false
                            onSelected(item)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun NumberField(
    label: String,
    value: Int?,
    enabled: Boolean,
    onValue: (Int?) -> Unit,
) {
    OutlinedTextField(
        value = value?.toString().orEmpty(),
        onValueChange = { raw -> onValue(raw.filter(Char::isDigit).toIntOrNull()) },
        label = { Text(label) },
        singleLine = true,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun DecimalField(
    label: String,
    value: Double?,
    enabled: Boolean,
    onValue: (Double?) -> Unit,
) {
    OutlinedTextField(
        value = value?.toString().orEmpty(),
        onValueChange = { raw ->
            val clean = raw.filter { it.isDigit() || it == '.' }
            onValue(clean.toDoubleOrNull())
        },
        label = { Text(label) },
        singleLine = true,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
    )
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
