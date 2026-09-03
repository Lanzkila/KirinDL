package com.junkfood.seal.ui.page.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.junkfood.seal.Downloader
import com.junkfood.seal.util.GalleryDlEngine
import com.junkfood.seal.util.PreferenceUtil.getInt
import com.junkfood.seal.util.PreferenceUtil.getBoolean
import com.junkfood.seal.util.PreferenceUtil.getLong
import com.junkfood.seal.util.PreferenceUtil.updateBoolean
import com.junkfood.seal.util.PreferenceUtil.getString
import com.junkfood.seal.util.UpdateUtil
import com.junkfood.seal.util.YT_DLP_NIGHTLY
import com.junkfood.seal.util.YT_DLP_UPDATE_CHANNEL
import com.junkfood.seal.util.YT_DLP_VERSION
import com.junkfood.seal.util.YT_DLP_AUTO_UPDATE
import com.junkfood.seal.util.YT_DLP_UPDATE_INTERVAL
import com.junkfood.seal.util.GALLERY_DL_AUTO_UPDATE
import com.junkfood.seal.util.GALLERY_DL_UPDATE_INTERVAL
import com.junkfood.seal.util.PreferenceStrings.getUpdateIntervalText
import com.junkfood.seal.ui.component.PreferenceSwitch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

@Composable
internal fun EngineUpdateCenterSection(
    galleryBusy: Boolean,
    onOpenYtdlpSettings: () -> Unit,
    onGalleryUpdated: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var ytdlpInstalled by remember { mutableStateOf(YT_DLP_VERSION.getString()) }
    var ytdlpLatest by remember { mutableStateOf<String?>(null) }
    var galleryInstalled by remember { mutableStateOf(GalleryDlEngine.installedVersion(context)) }
    var gallerySource by remember { mutableStateOf(GalleryDlEngine.installedSource(context)) }
    var galleryLatestCommit by remember { mutableStateOf<String?>(null) }
    var busyAction by remember { mutableStateOf<String?>(null) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var lastChecked by remember { mutableStateOf<String?>(null) }
    var ytdlpAutoUpdate by remember { mutableStateOf(YT_DLP_AUTO_UPDATE.getBoolean()) }
    var galleryAutoUpdate by remember { mutableStateOf(GALLERY_DL_AUTO_UPDATE.getBoolean()) }

    val channelLabel =
        if (YT_DLP_UPDATE_CHANNEL.getInt() == YT_DLP_NIGHTLY) "Nightly" else "Stable"

    fun refreshInstalledVersions() {
        ytdlpInstalled = YT_DLP_VERSION.getString()
        galleryInstalled = GalleryDlEngine.installedVersion(context)
        gallerySource = GalleryDlEngine.installedSource(context)
    }

    fun stampCheckedTime() {
        lastChecked =
            SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(System.currentTimeMillis()))
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Outlined.CloudDownload,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.size(10.dp))
            Column {
                Text(
                    "Engine Update Center",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    "Automatic updates first, with manual controls kept for on-demand checks.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.58f),
        ) {
            Column(
                modifier = Modifier.padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    "Automatic updates",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                    "Recommended. Manual Check / Update buttons remain available below.",
                    modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 4.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f),
                )
                PreferenceSwitch(
                    title = "yt-dlp auto update",
                    description =
                        if (ytdlpAutoUpdate) {
                            "$channelLabel • ${getUpdateIntervalText(YT_DLP_UPDATE_INTERVAL.getLong())}"
                        } else "Disabled",
                    icon = Icons.Outlined.CloudDownload,
                    isChecked = ytdlpAutoUpdate,
                    onClick = {
                        ytdlpAutoUpdate = !ytdlpAutoUpdate
                        YT_DLP_AUTO_UPDATE.updateBoolean(ytdlpAutoUpdate)
                    },
                )
                PreferenceSwitch(
                    title = "gallery-dl auto update",
                    description =
                        if (galleryAutoUpdate) {
                            "${getUpdateIntervalText(GALLERY_DL_UPDATE_INTERVAL.getLong())} • installed engine only"
                        } else "Disabled",
                    icon = Icons.Outlined.CloudDownload,
                    isChecked = galleryAutoUpdate,
                    onClick = {
                        galleryAutoUpdate = !galleryAutoUpdate
                        GALLERY_DL_AUTO_UPDATE.updateBoolean(galleryAutoUpdate)
                    },
                )
                OutlinedButton(
                    onClick = onOpenYtdlpSettings,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp),
                ) {
                    Icon(Icons.Outlined.Settings, contentDescription = null)
                    Spacer(Modifier.size(6.dp))
                    Text("yt-dlp channel & cadence")
                }
            }
        }

        EngineStatusCard(
            title = "yt-dlp",
            installed = ytdlpInstalled.ifBlank { "Unknown" },
            source = "Channel: $channelLabel",
            latest = ytdlpLatest?.let { "Latest: $it" },
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            busyAction = "Checking yt-dlp…"
                            errorMessage = null
                            val result = UpdateUtil.checkLatestYtDlpVersion()
                            ytdlpLatest = result.getOrNull()
                            errorMessage =
                                result.exceptionOrNull()?.let {
                                    "yt-dlp check failed: ${it.message ?: it.javaClass.simpleName}"
                                }
                            stampCheckedTime()
                            busyAction = null
                        }
                    },
                    enabled = busyAction == null,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Outlined.Refresh, contentDescription = null)
                    Spacer(Modifier.size(6.dp))
                    Text("Check")
                }

                Button(
                    onClick = {
                        scope.launch {
                            if (Downloader.downloaderState.value !is Downloader.State.Idle) {
                                errorMessage =
                                    "Finish or pause active media downloads before updating yt-dlp."
                                return@launch
                            }

                            busyAction = "Updating yt-dlp…"
                            errorMessage = null
                            statusMessage = null
                            Downloader.updateState(Downloader.State.Updating)
                            try {
                                runCatching { UpdateUtil.updateYtDlp() }
                                    .onSuccess {
                                        refreshInstalledVersions()
                                        statusMessage = "yt-dlp update finished."
                                    }
                                    .onFailure {
                                        errorMessage =
                                            "yt-dlp update failed: " +
                                                (it.message ?: it.javaClass.simpleName)
                                    }
                            } finally {
                                Downloader.updateState(Downloader.State.Idle)
                                busyAction = null
                            }
                        }
                    },
                    enabled = busyAction == null,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Update")
                }
            }

            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = onOpenYtdlpSettings,
                enabled = busyAction == null,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Outlined.Settings, contentDescription = null)
                Spacer(Modifier.size(6.dp))
                Text("Channel & Auto Update")
            }
        }

        EngineStatusCard(
            title = "gallery-dl",
            installed = galleryInstalled ?: "Not installed",
            source =
                gallerySource
                    ?.removePrefix("codeberg:")
                    ?.take(12)
                    ?.let { "Installed source: Codeberg $it" }
                    ?: "Source: Codeberg master",
            latest = galleryLatestCommit?.let { "Latest master commit: ${it.take(12)}" },
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            busyAction = "Checking gallery-dl…"
                            errorMessage = null
                            val result = GalleryDlEngine.latestMasterCommit()
                            galleryLatestCommit = result.getOrNull()
                            errorMessage =
                                result.exceptionOrNull()?.let {
                                    "gallery-dl check failed: " +
                                        (it.message ?: it.javaClass.simpleName)
                                }
                            stampCheckedTime()
                            busyAction = null
                        }
                    },
                    enabled = busyAction == null && !galleryBusy,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Outlined.Refresh, contentDescription = null)
                    Spacer(Modifier.size(6.dp))
                    Text("Check")
                }

                Button(
                    onClick = {
                        scope.launch {
                            busyAction = "Updating gallery-dl…"
                            errorMessage = null
                            statusMessage = null
                            val result = GalleryDlEngine.installLatest(context)
                            result
                                .onSuccess {
                                    refreshInstalledVersions()
                                    onGalleryUpdated()
                                    statusMessage = "gallery-dl updated to ${it.version}."
                                }
                                .onFailure {
                                    errorMessage =
                                        "gallery-dl update failed: " +
                                            (it.message ?: it.javaClass.simpleName)
                                }
                            busyAction = null
                        }
                    },
                    enabled = busyAction == null && !galleryBusy,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Update")
                }
            }
        }

        Button(
            onClick = {
                scope.launch {
                    if (Downloader.downloaderState.value !is Downloader.State.Idle) {
                        errorMessage =
                            "Finish or pause active media downloads before running Update All."
                        return@launch
                    }

                    busyAction = "Updating yt-dlp…"
                    errorMessage = null
                    statusMessage = null

                    Downloader.updateState(Downloader.State.Updating)
                    val ytResult =
                        try {
                            runCatching { UpdateUtil.updateYtDlp() }
                        } finally {
                            Downloader.updateState(Downloader.State.Idle)
                        }

                    if (ytResult.isFailure) {
                        errorMessage =
                            "yt-dlp update failed: " +
                                (ytResult.exceptionOrNull()?.message
                                    ?: ytResult.exceptionOrNull()?.javaClass?.simpleName
                                    ?: "Unknown error")
                        busyAction = null
                        return@launch
                    }

                    refreshInstalledVersions()
                    busyAction = "Updating gallery-dl…"
                    val galleryResult = GalleryDlEngine.installLatest(context)

                    galleryResult
                        .onSuccess {
                            refreshInstalledVersions()
                            onGalleryUpdated()
                            statusMessage =
                                "Update All finished. gallery-dl: ${it.version}; yt-dlp: " +
                                    ytdlpInstalled.ifBlank { "updated" }
                        }
                        .onFailure {
                            errorMessage =
                                "yt-dlp updated, but gallery-dl failed: " +
                                    (it.message ?: it.javaClass.simpleName)
                        }

                    busyAction = null
                }
            },
            enabled = busyAction == null && !galleryBusy,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (busyAction != null) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                )
                Spacer(Modifier.size(8.dp))
                Text(busyAction!!)
            } else {
                Text("Update All")
            }
        }

        lastChecked?.let {
            Text(
                "Last checked this session: $it",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        statusMessage?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        errorMessage?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun EngineStatusCard(
    title: String,
    installed: String,
    source: String,
    latest: String?,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                "Installed: $installed",
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                source,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            latest?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Spacer(Modifier.height(8.dp))
            content()
        }
    }
}
