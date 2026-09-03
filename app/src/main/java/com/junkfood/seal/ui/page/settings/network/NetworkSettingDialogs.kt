package com.junkfood.seal.ui.page.settings.network

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.OfflineBolt
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.core.text.isDigitsOnly
import com.junkfood.seal.R
import com.junkfood.seal.ui.component.ConfirmButton
import com.junkfood.seal.ui.component.DismissButton
import com.junkfood.seal.ui.component.DialogSingleChoiceItem
import com.junkfood.seal.ui.component.DialogSingleChoiceItemVariant
import com.junkfood.seal.util.MAX_RATE
import com.junkfood.seal.util.PreferenceUtil
import com.junkfood.seal.util.PreferenceUtil.getString
import com.junkfood.seal.util.PreferenceUtil.updateString
import com.junkfood.seal.util.isNumberInRange

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun RateLimitDialog(onDismissRequest: () -> Unit) {
    var isError by remember { mutableStateOf(false) }
    var maxRate by remember { mutableStateOf(PreferenceUtil.getMaxDownloadRate()) }
    val focusManager = LocalFocusManager.current
    val softwareKeyboardController = LocalSoftwareKeyboardController.current
    AlertDialog(
        onDismissRequest = onDismissRequest,
        icon = { Icon(Icons.Outlined.Speed, null, tint = MaterialTheme.colorScheme.primary) },
        title = { Text(stringResource(R.string.rate_limit)) },
        text = {
            Column {
                Text(
                    stringResource(R.string.rate_limit_desc),
                    style = MaterialTheme.typography.bodyLarge,
                )
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 12.dp),
                    isError = isError,
                    supportingText = {
                        Text(
                            text = if (isError) stringResource(R.string.invalid_input) else "",
                            style = MaterialTheme.typography.bodySmall,
                            color =
                                if (isError) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    value = maxRate,
                    label = { Text(stringResource(R.string.max_rate)) },
                    onValueChange = {
                        if (it.isDigitsOnly()) maxRate = it
                        isError = false
                    },
                    trailingIcon = { Text("K") },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                )
            }
        },
        dismissButton = { DismissButton { onDismissRequest() } },
        confirmButton = {
            ConfirmButton {
                if (maxRate.isNumberInRange(1, 100_0000)) {
                    PreferenceUtil.encodeString(MAX_RATE, maxRate)
                    onDismissRequest()
                } else {
                    isError = true
                }
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConcurrentDownloadDialog(
    selected: Int,
    onDismissRequest: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    val standardOptions = listOf(1, 4, 8, 12, 16)
    val options = remember(selected) {
        (standardOptions + listOfNotNull(selected.takeIf { it > 0 && it !in standardOptions }))
            .distinct()
            .sorted()
    }
    var value by remember(selected) {
        mutableStateOf(selected.coerceAtLeast(1))
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        icon = { Icon(Icons.Outlined.OfflineBolt, null, tint = MaterialTheme.colorScheme.primary) },
        title = { Text(stringResource(R.string.concurrent_download)) },
        text = {
            Column {
                Text(
                    "yt-dlp native fragment parallelism for DASH/HLS streams. " +
                        "This can stay enabled together with Aria2, which handles direct HTTP/HTTPS downloads.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                options.forEach { option ->
                    val description =
                        when (option) {
                            1 -> "Single fragment • maximum compatibility"
                            4 -> "Balanced • lighter network usage"
                            8 -> "Recommended • good speed/stability balance"
                            12 -> "Fast • higher parallelism"
                            16 -> "Maximum standard preset • use on strong connections"
                            else -> "Legacy/custom value • preserved from your previous setting"
                        }
                    DialogSingleChoiceItemVariant(
                        title = "$option fragment${if (option == 1) "" else "s"}",
                        desc = description,
                        selected = value == option,
                        onClick = { value = option },
                    )
                }
            }
        },
        dismissButton = { DismissButton { onDismissRequest() } },
        confirmButton = { ConfirmButton { onConfirm(value) } },
    )
}





@Composable
fun Aria2ConnectionsDialog(
    selected: Int,
    onDismissRequest: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    val options = listOf(2, 4, 8, 16, 32)
    var value by remember(selected) {
        mutableStateOf(selected.takeIf { it in options } ?: 16)
    }
    AlertDialog(
        onDismissRequest = onDismissRequest,
        icon = { Icon(Icons.Outlined.OfflineBolt, null, tint = MaterialTheme.colorScheme.primary) },
        title = { Text(stringResource(R.string.aria2c_connections)) },
        text = {
            Column {
                Text(
                    "Connections used by Aria2 for direct HTTP/HTTPS downloads. " +
                        "Concurrent fragments can remain enabled for DASH/HLS at the same time.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                options.forEach { option ->
                    DialogSingleChoiceItem(
                        text = option.toString(),
                        selected = value == option,
                        onClick = { value = option },
                    )
                }
            }
        },
        dismissButton = { DismissButton { onDismissRequest() } },
        confirmButton = { ConfirmButton { onConfirm(value) } },
    )
}
