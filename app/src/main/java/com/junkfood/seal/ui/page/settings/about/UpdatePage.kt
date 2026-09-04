package com.junkfood.seal.ui.page.settings.about

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.NewReleases
import androidx.compose.material.icons.outlined.Update
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.junkfood.seal.App
import com.junkfood.seal.R
import com.junkfood.seal.ui.common.intState
import com.junkfood.seal.ui.component.BackButton
import com.junkfood.seal.ui.component.PreferenceInfo
import com.junkfood.seal.ui.component.PreferenceItem
import com.junkfood.seal.ui.component.PreferenceSingleChoiceItem
import com.junkfood.seal.ui.component.PreferenceSubtitle
import com.junkfood.seal.ui.component.PreferenceSwitch
import com.junkfood.seal.ui.page.UpdateDialog
import com.junkfood.seal.util.APP_UPDATE_CHECK_TIME
import com.junkfood.seal.util.AUTO_UPDATE
import com.junkfood.seal.util.PRE_RELEASE
import com.junkfood.seal.util.PreferenceUtil
import com.junkfood.seal.util.PreferenceUtil.getLong
import com.junkfood.seal.util.PreferenceUtil.updateBoolean
import com.junkfood.seal.util.PreferenceUtil.updateInt
import com.junkfood.seal.util.STABLE
import com.junkfood.seal.util.UPDATE_CHANNEL
import com.junkfood.seal.util.UpdateUtil
import com.junkfood.seal.util.makeToast
import java.text.DateFormat
import java.util.Date
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdatePage(onNavigateBack: () -> Unit) {
    val scrollBehavior =
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
            rememberTopAppBarState(),
            canScroll = { true },
        )
    var automaticChecks by remember { mutableStateOf(PreferenceUtil.isAutoUpdateEnabled()) }
    var updateChannel by UPDATE_CHANNEL.intState
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var release by remember { mutableStateOf(UpdateUtil.Release()) }
    var currentRelease by remember { mutableStateOf(UpdateUtil.Release()) }
    var showUpdateDialog by remember { mutableStateOf(false) }
    var showCurrentReleaseDialog by remember { mutableStateOf(false) }
    var isChecking by remember { mutableStateOf(false) }
    var isLoadingCurrentNotes by remember { mutableStateOf(false) }
    var statusText by remember { mutableStateOf("Ready to check") }
    var lastChecked by remember { mutableStateOf(APP_UPDATE_CHECK_TIME.getLong()) }

    Scaffold(
        modifier = Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text(text = "App Update") },
                navigationIcon = { BackButton { onNavigateBack() } },
                scrollBehavior = scrollBehavior,
            )
        },
        content = { paddings ->
            LazyColumn(modifier = Modifier.padding(paddings)) {
                item {
                    PreferenceSubtitle(text = "KirinDL app")
                }

                item {
                    PreferenceInfo(
                        modifier = Modifier.padding(horizontal = 4.dp),
                        text =
                            "$statusText\nInstalled: v${App.packageInfo.versionName ?: "Unknown"}" +
                                formatLastChecked(lastChecked),
                    )
                }

                // Compact switch here intentionally: the previous highlighted container used
                // the large 20sp preference title and looked oversized on the App Update page.
                item {
                    PreferenceSwitch(
                        title = "Check for KirinDL updates automatically",
                        icon = null,
                        isChecked = automaticChecks,
                    ) {
                        automaticChecks = !automaticChecks
                        AUTO_UPDATE.updateBoolean(automaticChecks)
                    }
                }

                item {
                    var ignored by remember { mutableStateOf(false) }
                    PreferenceItem(
                        title = "What's New",
                        description =
                            if (isLoadingCurrentNotes) "Loading current release notes…"
                            else "Read the release notes for the installed KirinDL version",
                        icon = Icons.Outlined.NewReleases,
                        enabled = !isLoadingCurrentNotes,
                        onClick = {
                            if (!ignored && !isLoadingCurrentNotes) {
                                ignored = true
                                isLoadingCurrentNotes = true
                                scope.launch {
                                    UpdateUtil.getCurrentReleaseResult(context)
                                        .onSuccess {
                                            currentRelease = it
                                            showCurrentReleaseDialog = true
                                        }
                                        .onFailure {
                                            context.makeToast("Could not load current release notes")
                                        }
                                    isLoadingCurrentNotes = false
                                    ignored = false
                                }
                            }
                        },
                    )
                }

                item {
                    PreferenceSubtitle(
                        modifier = Modifier.padding(horizontal = 4.dp),
                        text = stringResource(id = R.string.update_channel),
                    )
                }

                item {
                    PreferenceSingleChoiceItem(
                        text = stringResource(id = R.string.stable_channel),
                        selected = updateChannel == STABLE,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                    ) {
                        updateChannel = STABLE
                        UPDATE_CHANNEL.updateInt(updateChannel)
                    }
                }

                item {
                    PreferenceSingleChoiceItem(
                        text = stringResource(id = R.string.pre_release_channel),
                        selected = updateChannel == PRE_RELEASE,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                    ) {
                        updateChannel = PRE_RELEASE
                        UPDATE_CHANNEL.updateInt(updateChannel)
                    }
                }

                item {
                    Row(
                        horizontalArrangement = Arrangement.End,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        ProgressIndicatorButton(
                            modifier =
                                Modifier.padding(horizontal = 24.dp)
                                    .padding(top = 6.dp)
                                    .padding(bottom = 12.dp),
                            text = if (isChecking) "Checking…" else stringResource(id = R.string.check_for_updates),
                            icon = Icons.Outlined.Update,
                            isLoading = isChecking,
                        ) {
                            if (!isChecking) {
                                isChecking = true
                                statusText = "Checking for updates…"
                                scope.launch {
                                    UpdateUtil.checkForUpdateResult(context)
                                        .onSuccess { available ->
                                            lastChecked = APP_UPDATE_CHECK_TIME.getLong()
                                            if (available != null) {
                                                release = available
                                                val version = available.tagName ?: available.name ?: "new version"
                                                statusText = "Update available • $version"
                                                showUpdateDialog = true
                                            } else {
                                                statusText = "Up to date"
                                            }
                                        }
                                        .onFailure { throwable ->
                                            lastChecked = APP_UPDATE_CHECK_TIME.getLong()
                                            statusText = "Check failed"
                                            throwable.printStackTrace()
                                            context.makeToast(R.string.app_update_failed)
                                        }
                                    isChecking = false
                                }
                            }
                        }
                    }
                    androidx.compose.material3.HorizontalDivider()
                }

                item {
                    PreferenceInfo(
                        modifier = Modifier.padding(horizontal = 4.dp),
                        text =
                            "KirinDL checks its official GitHub releases for new versions. " +
                                "When an update is available, the popup includes the release notes. " +
                                "The app opens the official release page in your browser and does not silently install APKs.",
                    )
                }

                item {
                    PreferenceItem(
                        title = "Release history",
                        description = "Use What's New for the installed version or open a detected update from this page",
                        icon = Icons.Outlined.History,
                        enabled = false,
                    )
                }
            }
        },
    )

    if (showUpdateDialog) {
        UpdateDialog(
            onDismissRequest = { showUpdateDialog = false },
            release = release,
            isUpdateAvailable = true,
        )
    }

    if (showCurrentReleaseDialog) {
        UpdateDialog(
            onDismissRequest = { showCurrentReleaseDialog = false },
            release = currentRelease,
            isUpdateAvailable = false,
        )
    }
}

private fun formatLastChecked(timestamp: Long): String {
    if (timestamp <= 0L) return "\nLast checked: Never"
    val formatted = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(timestamp))
    return "\nLast checked: $formatted"
}

@Composable
fun ProgressIndicatorButton(
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    FilledTonalButton(
        modifier = modifier,
        onClick = onClick,
        enabled = !isLoading,
        contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
    ) {
        if (isLoading) {
            Box(modifier = Modifier.size(18.dp)) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp).align(Alignment.Center),
                    strokeWidth = 3.dp,
                )
            }
        } else {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
        Text(text = text, modifier = Modifier.padding(start = 8.dp))
    }
}
