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
fun UpdateDialog(onDismissRequest: () -> Unit, release: UpdateUtil.Release) {
    val context = LocalContext.current

    UpdateDialogImpl(
        onDismissRequest = onDismissRequest,
        title = release.name ?: release.tagName ?: "KirinDL update",
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
        releaseNote = release.body.orEmpty(),
    )
}

@Composable
fun UpdateDialogImpl(
    onDismissRequest: () -> Unit,
    title: String,
    onConfirmUpdate: () -> Unit,
    releaseNote: String,
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
            OutlinedButton(onClick = onDismissRequest) {
                Text(text = stringResource(id = R.string.dismiss))
            }
        },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
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
        title = "KirinDL v3.1.0",
        onConfirmUpdate = {},
        releaseNote = "A new KirinDL release is available.",
    )
}
