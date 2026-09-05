package com.junkfood.seal.ui.page

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.NewReleases
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.junkfood.seal.R
import com.junkfood.seal.util.UpdateUtil

private const val KIRIN_RELEASES_URL =
    "https://github.com/Lanzkila/KirinDL/releases"

@Composable
fun UpdateDialog(
    onDismissRequest: () -> Unit,
    release: UpdateUtil.Release,
    backgroundUpdateBusy: Boolean = false,
    onBackgroundUpdate: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val versionName = runCatching {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            context.packageManager
                .getPackageInfo(context.packageName, android.content.pm.PackageManager.PackageInfoFlags.of(0))
                .versionName
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }
    }.getOrDefault("Unknown")
    val availableLabel = release.tagName ?: release.name ?: "Unknown"
    val channelLabel = if (release.preRelease == true) "Pre-release" else "Stable"

    UpdateDialogImpl(
        onDismissRequest = onDismissRequest,
        title = if (release.preRelease == true) "New prerelease available" else "New update available",
        onConfirmUpdate = {
            val target = release.htmlUrl?.takeIf { it.startsWith("https://") } ?: KIRIN_RELEASES_URL
            runCatching {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW, Uri.parse(target)).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                )
            }
            onDismissRequest()
        },
        onBackgroundUpdate = onBackgroundUpdate,
        backgroundUpdateBusy = backgroundUpdateBusy,
        releaseNote = release.body.orEmpty(),
        summary = "Installed $versionName → Available $availableLabel",
        channelLabel = channelLabel,
    )
}

@Composable
fun UpdateDialogImpl(
    onDismissRequest: () -> Unit,
    title: String,
    onConfirmUpdate: () -> Unit,
    releaseNote: String,
    summary: String,
    channelLabel: String,
    backgroundUpdateBusy: Boolean = false,
    onBackgroundUpdate: (() -> Unit)? = null,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(title) },
        icon = { Icon(Icons.Outlined.NewReleases, null) },
        confirmButton = {
            Button(onClick = onConfirmUpdate) {
                Text("Open release")
            }
        },
        dismissButton = {
            Column {
                if (onBackgroundUpdate != null) {
                    OutlinedButton(onClick = onBackgroundUpdate, enabled = !backgroundUpdateBusy) {
                        Text(if (backgroundUpdateBusy) "Starting…" else "Background update")
                    }
                }
                OutlinedButton(onClick = onDismissRequest) {
                    Text(text = stringResource(id = R.string.dismiss))
                }
            }
        },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(summary)
                Text(channelLabel)
                Text(releaseNote.ifBlank { "A new KirinDL release is available." })
            }
        },
    )
}

@Preview
@Composable
private fun Preview() {
    UpdateDialogImpl(
        onDismissRequest = {},
        title = "New update available",
        onConfirmUpdate = {},
        releaseNote = "A new KirinDL release is available.",
        summary = "Installed v3.1.2 → Available v3.1.3",
        channelLabel = "Stable",
    )
}
