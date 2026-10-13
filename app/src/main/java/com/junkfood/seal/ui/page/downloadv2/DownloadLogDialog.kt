package com.junkfood.seal.ui.page.downloadv2

import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.junkfood.seal.util.DownloadLogStore
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable
internal fun DownloadLogDialog(taskId: String, onDismiss: () -> Unit) {
    var log by remember(taskId) { mutableStateOf(DownloadLogStore.read(taskId)) }
    val clipboard = LocalClipboardManager.current
    LaunchedEffect(taskId) {
        while (isActive) {
            log = DownloadLogStore.read(taskId)
            delay(500)
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Download log · recent 250 lines") },
        text = {
            SelectionContainer {
                Text(log, modifier = Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()),
                    fontFamily = FontFamily.Monospace, fontSize = 12.sp)
            }
        },
        confirmButton = {
            TextButton(onClick = { clipboard.setText(AnnotatedString(log)) }) { Text("Copy log") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}
