package com.junkfood.seal.ui.page.tools

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Cookie
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SystemUpdateAlt
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.junkfood.seal.R
import com.junkfood.seal.ui.component.BackButton
import org.koin.androidx.compose.koinViewModel

@Composable
fun GalleryDlPage(
    onNavigateBack: () -> Unit,
    viewModel: GalleryDlViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val palette = rememberToolPalette()
    val clipboard = LocalClipboardManager.current

    val cookiesLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri?.let(viewModel::importCookies)
        }
    val configImportLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri?.let(viewModel::importConfig)
        }
    val configExportLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.CreateDocument("application/json")
        ) { uri ->
            uri?.let(viewModel::exportConfig)
        }

    Scaffold(
        containerColor = palette.background,
        topBar = {
            Column(modifier = Modifier.statusBarsPadding()) {
                Row(
                    modifier =
                        Modifier.fillMaxWidth()
                            .padding(start = 4.dp, end = 16.dp, top = 4.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    BackButton(onNavigateBack)
                    Spacer(Modifier.width(4.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.gallery_dl),
                            style =
                                MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                            color = palette.textPrimary,
                        )
                        Text(
                            text = stringResource(R.string.gallery_dl_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                HorizontalDivider(color = palette.border.copy(alpha = 0.4f))
            }
        },
    ) { padding ->
        Column(
            modifier =
                Modifier.fillMaxSize()
                    .padding(padding)
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            EngineCard(
                state = state,
                palette = palette,
                onInstall = viewModel::installOrUpdateEngine,
            )

            CompatibilityCard(
                state = state,
                palette = palette,
                onDiagnostics = viewModel::runDiagnostics,
                onClearCache = viewModel::clearCache,
            )

            ConfigCard(
                state = state,
                palette = palette,
                onConfigChanged = viewModel::updateConfigText,
                onSave = viewModel::saveConfig,
                onReset = viewModel::resetConfig,
                onImport = {
                    configImportLauncher.launch(
                        arrayOf("application/json", "text/plain", "application/octet-stream")
                    )
                },
                onExport = { configExportLauncher.launch("gallery-dl-config.json") },
            )

            CookiesCard(
                state = state,
                palette = palette,
                onImport = {
                    cookiesLauncher.launch(arrayOf("text/plain", "application/octet-stream"))
                },
                onClear = viewModel::clearCookies,
            )

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
                color = palette.surface,
                border = BorderStroke(1.dp, palette.border.copy(alpha = 0.5f)),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Outlined.Link,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = palette.primary,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.gallery_dl_url),
                            style =
                                MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.SemiBold
                                ),
                            color = palette.textPrimary,
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = state.url,
                        onValueChange = viewModel::updateUrl,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        enabled = !state.isBusy,
                        placeholder = { Text(stringResource(R.string.gallery_dl_url_hint)) },
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutlinedButton(
                            onClick = {
                                clipboard
                                    .getText()
                                    ?.text
                                    ?.trim()
                                    ?.takeIf { it.isNotEmpty() }
                                    ?.let(viewModel::updateUrl)
                            },
                            enabled = !state.isBusy,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(stringResource(R.string.paste))
                        }
                        OutlinedButton(
                            onClick = viewModel::checkExtractor,
                            enabled =
                                state.isInstalled &&
                                    !state.isBusy &&
                                    com.junkfood.seal.util.GalleryDlRunner.isCandidateUrl(state.url),
                            modifier = Modifier.weight(1f),
                        ) {
                            if (state.isCheckingExtractor) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                )
                                Spacer(Modifier.width(8.dp))
                            }
                            Text("Check")
                        }
                    }

                    when (state.extractorSupported) {
                        true -> {
                            Spacer(Modifier.height(10.dp))
                            Text(
                                text = "Extractor: ${state.extractorLabel ?: "Ready"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.success,
                            )
                        }
                        false -> {
                            Spacer(Modifier.height(10.dp))
                            Text(
                                text = "No matching extractor",
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.error,
                            )
                        }
                        null -> Unit
                    }
                }
            }

            Button(
                onClick = viewModel::download,
                enabled = state.canDownload,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (state.isDownloading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                    )
                    Spacer(Modifier.width(10.dp))
                } else {
                    Icon(Icons.Outlined.Download, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                }
                Text(
                    if (state.isDownloading) {
                        stringResource(R.string.gallery_dl_downloading)
                    } else {
                        stringResource(R.string.gallery_dl_download)
                    }
                )
            }

            state.statusMessage?.let { message ->
                MessageCard(
                    text = message,
                    icon = Icons.Outlined.CheckCircle,
                    tint = palette.success,
                    palette = palette,
                )
            }
            state.errorMessage?.let { message ->
                MessageCard(
                    text = message,
                    icon = Icons.Outlined.ErrorOutline,
                    tint = palette.error,
                    palette = palette,
                )
            }

            state.destinationDirectory?.let { directory ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                    color = palette.surfaceVariant,
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            stringResource(R.string.gallery_dl_saved_to),
                            style = MaterialTheme.typography.labelMedium,
                            color = palette.textSecondary,
                        )
                        Text(
                            directory,
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.textPrimary,
                        )
                    }
                }
            }

            Text(
                text = stringResource(R.string.gallery_dl_permission_note),
                style = MaterialTheme.typography.bodySmall,
                color = palette.textSecondary,
            )
        }
    }
}

@Composable
private fun EngineCard(
    state: GalleryDlViewModel.ViewState,
    palette: ToolPalette,
    onInstall: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
        color = palette.surface,
        border = BorderStroke(1.dp, palette.border.copy(alpha = 0.5f)),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.SystemUpdateAlt,
                    contentDescription = null,
                    tint = palette.primary,
                )
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.gallery_dl_engine),
                        style =
                            MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold
                            ),
                        color = palette.textPrimary,
                    )
                    Text(
                        text =
                            state.installedVersion?.let {
                                stringResource(R.string.gallery_dl_engine_installed, it)
                            } ?: stringResource(R.string.gallery_dl_engine_not_installed),
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.textSecondary,
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = onInstall,
                enabled = !state.isBusy,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (state.isInstalling) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                    )
                    Spacer(Modifier.width(10.dp))
                }
                Text(
                    if (state.isInstalling) {
                        stringResource(R.string.gallery_dl_installing)
                    } else {
                        stringResource(R.string.gallery_dl_install_update)
                    }
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.gallery_dl_engine_note),
                style = MaterialTheme.typography.bodySmall,
                color = palette.textSecondary,
            )
        }
    }
}

@Composable
private fun CompatibilityCard(
    state: GalleryDlViewModel.ViewState,
    palette: ToolPalette,
    onDiagnostics: () -> Unit,
    onClearCache: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
        color = palette.surface,
        border = BorderStroke(1.dp, palette.border.copy(alpha = 0.5f)),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = palette.primary)
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Compatibility Status",
                        style =
                            MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold
                            ),
                        color = palette.textPrimary,
                    )
                    Text(
                        text = "Codeberg engine, persistent config/cache and optional helpers",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.textSecondary,
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            StatusLine(
                label = "Engine",
                value = state.installedVersion ?: "Not installed",
                valueColor = if (state.isInstalled) palette.success else palette.textSecondary,
                palette = palette,
            )
            StatusLine(
                label = "Config",
                value = if (state.configValid) "Valid" else "Invalid",
                valueColor = if (state.configValid) palette.success else palette.error,
                palette = palette,
            )
            StatusLine(
                label = "Cookies",
                value =
                    if (state.cookiesImported) {
                        "${state.cookiesSize} bytes"
                    } else {
                        "Optional / not imported"
                    },
                valueColor =
                    if (state.cookiesImported) palette.success else palette.textSecondary,
                palette = palette,
            )
            StatusLine(
                label = "Cache",
                value = "${state.cacheSize} bytes",
                valueColor = palette.textPrimary,
                palette = palette,
            )

            if (state.runtimeReadyModules.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Ready: ${state.runtimeReadyModules.joinToString(", ")}",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.success,
                )
            }
            if (state.runtimeMissingOptionalModules.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text =
                        "Optional helpers not bundled: " +
                            state.runtimeMissingOptionalModules.joinToString(", "),
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textSecondary,
                )
            }

            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = onDiagnostics,
                enabled = state.isInstalled && !state.isBusy,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (state.isCheckingRuntime) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text("Run Compatibility Check")
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = onClearCache,
                enabled = !state.isBusy && state.cacheSize > 0L,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Outlined.Delete, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Clear Gallery DL Cache")
            }
        }
    }
}

@Composable
private fun StatusLine(
    label: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color,
    palette: ToolPalette,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodySmall,
            color = palette.textSecondary,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = valueColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ConfigCard(
    state: GalleryDlViewModel.ViewState,
    palette: ToolPalette,
    onConfigChanged: (String) -> Unit,
    onSave: () -> Unit,
    onReset: () -> Unit,
    onImport: () -> Unit,
    onExport: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
        color = palette.surface,
        border = BorderStroke(1.dp, palette.border.copy(alpha = 0.5f)),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Settings, contentDescription = null, tint = palette.primary)
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.gallery_dl_configuration),
                        style =
                            MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold
                            ),
                        color = palette.textPrimary,
                    )
                    Text(
                        text = stringResource(R.string.gallery_dl_config_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.textSecondary,
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = state.configText,
                onValueChange = onConfigChanged,
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isBusy,
                minLines = 7,
                maxLines = 14,
                isError = !state.configValid,
                supportingText = {
                    Text(
                        text =
                            if (state.configValid) {
                                stringResource(R.string.gallery_dl_config_valid)
                            } else {
                                stringResource(R.string.gallery_dl_config_invalid)
                            },
                        color = if (state.configValid) palette.success else palette.error,
                    )
                },
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = onSave,
                    enabled = state.configValid && !state.isBusy,
                    modifier = Modifier.weight(1f),
                ) {
                    if (state.isSavingConfig) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Icon(Icons.Outlined.Save, contentDescription = null)
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.gallery_dl_save_config))
                }
                OutlinedButton(
                    onClick = onReset,
                    enabled = !state.isBusy,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(R.string.gallery_dl_reset_config))
                }
            }

            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = onImport,
                enabled = !state.isBusy,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Import Config JSON")
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = onExport,
                enabled = state.configValid && !state.isBusy,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Export Config JSON")
            }
        }
    }
}

@Composable
private fun CookiesCard(
    state: GalleryDlViewModel.ViewState,
    palette: ToolPalette,
    onImport: () -> Unit,
    onClear: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
        color = palette.surface,
        border = BorderStroke(1.dp, palette.border.copy(alpha = 0.5f)),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Cookie, contentDescription = null, tint = palette.primary)
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.gallery_dl_cookies),
                        style =
                            MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold
                            ),
                        color = palette.textPrimary,
                    )
                    Text(
                        text = stringResource(R.string.gallery_dl_cookies_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.textSecondary,
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text =
                    if (state.cookiesImported) {
                        stringResource(
                            R.string.gallery_dl_cookies_imported_size,
                            state.cookiesSize,
                        )
                    } else {
                        stringResource(R.string.gallery_dl_cookies_not_imported)
                    },
                style = MaterialTheme.typography.bodySmall,
                color = if (state.cookiesImported) palette.success else palette.textSecondary,
            )
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = onImport,
                    enabled = !state.isBusy,
                    modifier = Modifier.weight(1f),
                ) {
                    if (state.isImportingCookies) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Icon(Icons.Outlined.Cookie, contentDescription = null)
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.gallery_dl_import_cookies))
                }
                OutlinedButton(
                    onClick = onClear,
                    enabled = state.cookiesImported && !state.isBusy,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Outlined.Delete, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.gallery_dl_clear_cookies))
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.gallery_dl_cookies_note),
                style = MaterialTheme.typography.bodySmall,
                color = palette.textSecondary,
            )
        }
    }
}

@Composable
private fun MessageCard(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: androidx.compose.ui.graphics.Color,
    palette: ToolPalette,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
        color = palette.surfaceVariant,
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, tint = tint)
            Spacer(Modifier.width(10.dp))
            Text(
                text,
                color = palette.textPrimary,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}
