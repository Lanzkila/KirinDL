package com.junkfood.seal.ui.page.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Cookie
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SystemUpdateAlt
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.junkfood.seal.ui.component.BackButton
import com.junkfood.seal.ui.page.tools.GalleryDlViewModel
import com.junkfood.seal.util.GalleryDlThemePreference
import com.junkfood.seal.util.GalleryDlThemeStyle
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryDlSettingsPage(
    onNavigateBack: () -> Unit,
    viewModel: GalleryDlViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val selectedTheme by GalleryDlThemePreference.style.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

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
                title = { Text("Gallery DL") },
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
            SettingsSection(
                icon = Icons.Outlined.Palette,
                title = "Gallery appearance",
                description =
                    "Gallery DL follows KirinDownloader's active Light/Dark/Dynamic theme. " +
                        "Choose an optional accent variation below.",
            ) {
                GalleryDlThemeStyle.entries.forEach { style ->
                    GalleryThemeChoice(
                        style = style,
                        selected = selectedTheme == style,
                        onClick = { GalleryDlThemePreference.setStyle(style) },
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }

            SettingsSection(
                icon = Icons.Outlined.SystemUpdateAlt,
                title = "Engine",
                description =
                    state.installedVersion?.let { "Codeberg engine installed: $it" }
                        ?: "Install the optional Codeberg gallery-dl engine.",
            ) {
                OutlinedButton(
                    onClick = viewModel::installOrUpdateEngine,
                    enabled = !state.isBusy,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (state.isInstalling) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(if (state.isInstalling) "Installing…" else "Install / Update Engine")
                }
            }

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
}

@Composable
private fun GalleryThemeChoice(
    style: GalleryDlThemeStyle,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme

    val swatch =
        when (style) {
            GalleryDlThemeStyle.APP_DEFAULT -> scheme.primary
            GalleryDlThemeStyle.KIRIN_CYAN -> Color(0xFF18BFEA)
            GalleryDlThemeStyle.OCEAN -> Color(0xFF4B8DFF)
            GalleryDlThemeStyle.EMERALD -> Color(0xFF2DBF85)
            GalleryDlThemeStyle.VIOLET -> Color(0xFF8B7CFF)
        }

    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color =
            if (selected) {
                scheme.primaryContainer.copy(alpha = 0.55f)
            } else {
                scheme.surfaceVariant.copy(alpha = 0.45f)
            },
        border =
            BorderStroke(
                1.dp,
                if (selected) scheme.primary else scheme.outlineVariant,
            ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(24.dp),
                shape = RoundedCornerShape(99.dp),
                color = swatch,
            ) {}

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    style.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    style.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                )
            }

            if (selected) {
                Icon(
                    Icons.Outlined.Check,
                    contentDescription = "Selected",
                    tint = scheme.primary,
                )
            }
        }
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
