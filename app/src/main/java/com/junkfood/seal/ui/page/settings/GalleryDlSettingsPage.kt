package com.junkfood.seal.ui.page.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cookie
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.junkfood.seal.ui.component.BackButton
import com.junkfood.seal.ui.page.settings.general.YtdlpUpdateChannelDialog
import com.junkfood.seal.ui.page.tools.GalleryDlViewModel
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryDlSettingsPage(
    onNavigateBack: () -> Unit,
    viewModel: GalleryDlViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    var showYtdlpSettings by remember { mutableStateOf(false) }

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

    LaunchedEffect(Unit) { viewModel.refreshFromDisk() }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            LargeTopAppBar(
                title = { Text("Engine Update Center") },
                navigationIcon = { BackButton(onNavigateBack) },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { padding ->
        Column(
            modifier =
                Modifier.fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            EngineUpdateCenterSection(
                galleryBusy = state.isBusy,
                onOpenYtdlpSettings = { showYtdlpSettings = true },
                onGalleryUpdated = viewModel::refreshFromDisk,
            )

            SettingsSection(
                icon = Icons.Outlined.Settings,
                title = "Compatibility",
                description = "Check Python helpers, cache and the active Codeberg runtime.",
            ) {
                Text(
                    "Cache: ${state.cacheSize} bytes",
                    style = MaterialTheme.typography.bodySmall,
                )
                if (state.runtimeReadyModules.isNotEmpty()) {
                    Text(
                        "Ready: ${state.runtimeReadyModules.joinToString(", ")}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                if (state.runtimeMissingOptionalModules.isNotEmpty()) {
                    Text(
                        "Optional helpers not bundled: " +
                            state.runtimeMissingOptionalModules.joinToString(", "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = viewModel::runDiagnostics,
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
                    onClick = viewModel::clearCache,
                    enabled = !state.isBusy && state.cacheSize > 0L,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Outlined.Delete, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Clear Gallery DL Cache")
                }
            }

            SettingsSection(
                icon = Icons.Outlined.Cookie,
                title = "Cookies",
                description = "Optional Netscape cookies.txt for your existing site sessions.",
            ) {
                Text(
                    if (state.cookiesImported) {
                        "cookies.txt ready (${state.cookiesSize} bytes)"
                    } else {
                        "No Gallery DL cookies imported"
                    },
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(
                        onClick = {
                            cookiesLauncher.launch(
                                arrayOf("text/plain", "application/octet-stream")
                            )
                        },
                        enabled = !state.isBusy,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Import")
                    }
                    OutlinedButton(
                        onClick = viewModel::clearCookies,
                        enabled = state.cookiesImported && !state.isBusy,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Clear")
                    }
                }
            }

            SettingsSection(
                icon = Icons.Outlined.Save,
                title = "Expert Config",
                description =
                    "Raw gallery-dl JSON. Normal downloads do not require editing this file.",
            ) {
                OutlinedTextField(
                    value = state.configText,
                    onValueChange = viewModel::updateConfigText,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isBusy,
                    minLines = 8,
                    maxLines = 16,
                    isError = !state.configValid,
                    supportingText = {
                        Text(if (state.configValid) "Valid JSON" else "Invalid JSON")
                    },
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(
                        onClick = viewModel::saveConfig,
                        enabled = state.configValid && !state.isBusy,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Save")
                    }
                    OutlinedButton(
                        onClick = viewModel::resetConfig,
                        enabled = !state.isBusy,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Reset")
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = {
                        configImportLauncher.launch(
                            arrayOf(
                                "application/json",
                                "text/plain",
                                "application/octet-stream",
                            )
                        )
                    },
                    enabled = !state.isBusy,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Import Config JSON")
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { configExportLauncher.launch("gallery-dl-config.json") },
                    enabled = state.configValid && !state.isBusy,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Export Config JSON")
                }
            }

            state.statusMessage?.let {
                Text(
                    it,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            state.errorMessage?.let {
                Text(
                    it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (showYtdlpSettings) {
        YtdlpUpdateChannelDialog(
            onDismissRequest = {
                showYtdlpSettings = false
            }
        )
    }
}

@Composable
private fun SettingsSection(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        content()
    }
}
