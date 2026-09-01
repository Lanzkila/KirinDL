package com.junkfood.seal.ui.page

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.animateContentSize
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.junkfood.seal.R
import com.junkfood.seal.util.UpdateUtil
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

private const val KIRIN_RELEASES_URL =
    "https://github.com/Lanzkila/KirinDL/releases"

@Composable
fun UpdateDialog(onDismissRequest: () -> Unit, release: UpdateUtil.Release) {
    val context = LocalContext.current

    UpdateDialogImpl(
        onDismissRequest = onDismissRequest,
        title = release.name.toString(),
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
        releaseNote = release.body.toString(),
        downloadStatus = UpdateUtil.DownloadStatus.NotYet,
    )
}

@Composable
fun UpdateDialogImpl(
    onDismissRequest: () -> Unit,
    title: String,
    onConfirmUpdate: () -> Unit,
    releaseNote: String,
    downloadStatus: UpdateUtil.DownloadStatus,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(title) },
        icon = { Icon(Icons.Outlined.NewReleases, null) },
        confirmButton = {
            Button(
                onClick = {
                    if (downloadStatus !is UpdateUtil.DownloadStatus.Progress) {
                        onConfirmUpdate()
                    }
                }
            ) {
                Text(
                    when (downloadStatus) {
                        is UpdateUtil.DownloadStatus.Progress -> "${downloadStatus.percent} %"
                        else -> "Open release"
                    },
                    modifier = Modifier.animateContentSize(),
                )
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismissRequest) {
                Text(text = stringResource(id = R.string.dismiss))
            }
        },
        text = { Column(Modifier.verticalScroll(rememberScrollState())) { Text(releaseNote) } },
    )
}

@Preview
@Composable
private fun Preview() {
    var b by remember { mutableStateOf(false) }
    val flow: MutableStateFlow<UpdateUtil.DownloadStatus> = remember {
        MutableStateFlow(UpdateUtil.DownloadStatus.NotYet)
    }

    LaunchedEffect(b) {
        if (b) {
            repeat(100) { i ->
                flow.update { UpdateUtil.DownloadStatus.Progress(percent = i) }
                delay(50)
            }
        } else {
            flow.update { UpdateUtil.DownloadStatus.NotYet }
        }
    }

    val status by flow.collectAsStateWithLifecycle()

    UpdateDialogImpl(
        onDismissRequest = { b = false },
        title = "v3.0.4",
        onConfirmUpdate = { b = true },
        releaseNote = "A new KirinDL release is available.",
        downloadStatus = status,
    )
}
