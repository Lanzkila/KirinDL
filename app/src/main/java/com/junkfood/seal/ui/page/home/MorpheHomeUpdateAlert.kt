package com.junkfood.seal.ui.page.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.NewReleases
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.junkfood.seal.ui.page.UpdateDialog
import com.junkfood.seal.util.UpdateUtil
import com.junkfood.seal.util.makeToast

/**
 * Morphe-style update card for the Home screen.
 *
 * This feature is intentionally kept separate and is not currently called by
 * NewHomePage. The core updater and About > App Update remain independent.
 *
 * To restore the card in NewHomePage's LazyColumn, add:
 * item { MorpheHomeUpdateAlert() }
 */
@Composable
fun MorpheHomeUpdateAlert() {
    val context = LocalContext.current
    val availableAppUpdate by UpdateUtil.availableAppUpdate.collectAsStateWithLifecycle()
    var hiddenUpdateTag by remember { mutableStateOf<String?>(null) }
    var showAppUpdateDialog by remember { mutableStateOf(false) }

    LaunchedEffect(availableAppUpdate?.tagName, availableAppUpdate?.name) {
        val currentTag = availableAppUpdate?.tagName ?: availableAppUpdate?.name
        if (currentTag != null && hiddenUpdateTag != null && hiddenUpdateTag != currentTag) {
            hiddenUpdateTag = null
        }
    }

    val update = availableAppUpdate
    val updateTag = update?.tagName ?: update?.name
    if (update != null && updateTag != hiddenUpdateTag) {
        MorpheHomeUpdateAlertCard(
            release = update,
            onOpen = { showAppUpdateDialog = true },
            onDismissForSession = { hiddenUpdateTag = updateTag },
        )
    }

    if (showAppUpdateDialog) {
        availableAppUpdate?.let { release ->
            UpdateDialog(
                onDismissRequest = { showAppUpdateDialog = false },
                release = release,
                isUpdateAvailable = true,
                onBackgroundUpdate =
                    if (UpdateUtil.hasBackgroundAppUpdate(release)) {
                        {
                            UpdateUtil.enqueueBackgroundAppUpdate(context, release)
                                .onSuccess {
                                    context.makeToast(
                                        "KirinDL update is downloading in Android Download Manager"
                                    )
                                }
                                .onFailure { error ->
                                    error.printStackTrace()
                                    context.makeToast("Could not start the app update download")
                                }
                        }
                    } else {
                        null
                    },
            )
        }
    }
}

@Composable
private fun MorpheHomeUpdateAlertCard(
    release: UpdateUtil.Release,
    onOpen: () -> Unit,
    onDismissForSession: () -> Unit,
) {
    val version = (release.tagName ?: release.name ?: "New version").removePrefix("v")
    Card(
        onClick = onOpen,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f),
            ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp),
        ) {
            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
            ) {
                Icon(
                    imageVector = Icons.Outlined.NewReleases,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(10.dp).size(22.dp),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "KirinDL update available",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text =
                        "v$version • " +
                            if (release.preRelease == true) "Pre-release • Tap for notes"
                            else "Stable • Tap for notes",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onDismissForSession) {
                Icon(
                    imageVector = Icons.Outlined.Clear,
                    contentDescription = "Hide until next app launch",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
