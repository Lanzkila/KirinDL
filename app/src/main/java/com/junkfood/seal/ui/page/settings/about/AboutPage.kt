package com.junkfood.seal.ui.page.settings.about

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.NewReleases
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.UpdateDisabled
import androidx.compose.material.icons.outlined.VolunteerActivism
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.UrlAnnotation
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.junkfood.seal.App
import com.junkfood.seal.App.Companion.packageInfo
import com.junkfood.seal.R
import com.junkfood.seal.ui.component.BackButton
import com.junkfood.seal.ui.component.ConfirmButton
import com.junkfood.seal.ui.page.UpdateDialog
import com.junkfood.seal.util.APP_UPDATE_CHECK_TIME
import com.junkfood.seal.util.AUTO_UPDATE
import com.junkfood.seal.util.PreferenceUtil
import com.junkfood.seal.util.PreferenceUtil.updateLong
import com.junkfood.seal.util.UpdateUtil
import com.junkfood.seal.util.makeToast
import kotlinx.coroutines.launch

private const val releaseURL = "https://github.com/Lanzkila/KirinDL/releases"
private const val repoUrl = "https://github.com/Lanzkila/KirinDL/blob/main/README.md"
const val weblate = "https://hosted.weblate.org/engage/seal/"
const val YtdlpRepository = "https://github.com/yt-dlp/yt-dlp"
private const val githubIssueUrl = "https://github.com/Lanzkila/KirinDL/issues"
private const val telegramChannelUrl = ""
private const val youtubeChannelUrl = ""
private const val websiteUrl = ""
private const val githubSponsor = "https://github.com/sponsors/JunkFood02"
private const val TAG = "AboutPage"


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutPage(
    onNavigateBack: () -> Unit,
    onNavigateToTools: () -> Unit,
) {
    val scrollBehavior =
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
            rememberTopAppBarState(),
            canScroll = { true },
        )
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var availableRelease by remember { mutableStateOf<UpdateUtil.Release?>(null) }
    var showUpdateDialog by remember { mutableStateOf(false) }
    var isLoadingStableNotes by remember { mutableStateOf(false) }
    var isLoadingPreReleaseNotes by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(id = R.string.about)) },
                navigationIcon = { BackButton { onNavigateBack() } },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                ReleaseNotesCard(
                    title = stringResource(id = R.string.more_tools),
                    description = stringResource(id = R.string.more_tools_desc),
                    icon = Icons.Outlined.Build,
                    loading = false,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onNavigateToTools,
                )
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    ReleaseNotesCard(
                        title = "Stable",
                        description =
                            if (isLoadingStableNotes) "Loading…"
                            else "Latest Stable release notes",
                        icon = Icons.Outlined.NewReleases,
                        loading = isLoadingStableNotes,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            if (!isLoadingStableNotes) {
                                isLoadingStableNotes = true
                                scope.launch {
                                    try {
                                        UpdateUtil.getLatestStableReleaseResult()
                                            .onSuccess { release ->
                                                availableRelease = release
                                                showUpdateDialog = true
                                            }
                                            .onFailure {
                                                context.makeToast("Could not load Stable release notes")
                                            }
                                    } finally {
                                        isLoadingStableNotes = false
                                    }
                                }
                            }
                        },
                    )
                    ReleaseNotesCard(
                        title = "Pre-release",
                        description =
                            if (isLoadingPreReleaseNotes) "Loading…"
                            else "Latest testing release notes",
                        icon = Icons.Outlined.AutoAwesome,
                        loading = isLoadingPreReleaseNotes,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            if (!isLoadingPreReleaseNotes) {
                                isLoadingPreReleaseNotes = true
                                scope.launch {
                                    try {
                                        UpdateUtil.getLatestPreReleaseResult()
                                            .onSuccess { release ->
                                                availableRelease = release
                                                showUpdateDialog = true
                                            }
                                            .onFailure {
                                                context.makeToast("No Pre-release notes are available")
                                            }
                                    } finally {
                                        isLoadingPreReleaseNotes = false
                                    }
                                }
                            }
                        },
                    )
                }
            }
        }
    }

    if (showUpdateDialog) {
        availableRelease?.let { release ->
            UpdateDialog(
                onDismissRequest = { showUpdateDialog = false },
                release = release,
                isUpdateAvailable = false,
            )
        }
    }
}

@Composable
private fun ReleaseNotesCard(
    title: String,
    description: String,
    icon: ImageVector,
    loading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        modifier = modifier.heightIn(min = 132.dp),
        enabled = !loading,
        shape = RoundedCornerShape(12.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
            ),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (loading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            } else {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalTextApi::class)
@Composable
@Preview
fun AutoUpdateUnavailableDialog(onDismissRequest: () -> Unit = {}) {
    val uriHandler = LocalUriHandler.current
    val hapticFeedback = LocalHapticFeedback.current
    val hyperLinkText = stringResource(id = R.string.switch_to_github_builds)
    val text = stringResource(id = R.string.auto_update_disabled_msg, "F-Droid", hyperLinkText)

    val annotatedString = buildAnnotatedString {
        append(text)
        val startIndex = text.indexOf(hyperLinkText)
        val endIndex = startIndex + hyperLinkText.length
        addUrlAnnotation(
            UrlAnnotation("https://github.com/Lanzkila/KirinDL/releases/latest"),
            start = startIndex,
            end = endIndex,
        )
        addStyle(
            SpanStyle(
                color = MaterialTheme.colorScheme.tertiary,
                textDecoration = TextDecoration.Underline,
            ),
            start = startIndex,
            end = endIndex,
        )
    }
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            ConfirmButton(stringResource(id = R.string.got_it)) { onDismissRequest() }
        },
        icon = {
            Icon(
                Icons.Outlined.UpdateDisabled,
                null,
                tint = MaterialTheme.colorScheme.primary,
            )
        },
        title = {
            Text(
                text = stringResource(id = R.string.feature_unavailable),
                textAlign = TextAlign.Center,
            )
        },
        text = {
            ClickableText(
                text = annotatedString,
                onClick = { index ->
                    annotatedString.getUrlAnnotations(index, index).firstOrNull()?.let {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        uriHandler.openUri(it.item.url)
                    }
                },
                style =
                    MaterialTheme.typography.bodyMedium.copy(
                        MaterialTheme.colorScheme.onSurfaceVariant
                    ),
            )
        },
    )
}

