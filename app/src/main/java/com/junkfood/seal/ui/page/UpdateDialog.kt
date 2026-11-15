package com.junkfood.seal.ui.page

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.NewReleases
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.UrlAnnotation
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.junkfood.seal.App
import com.junkfood.seal.R
import com.junkfood.seal.util.UpdateUtil
import com.junkfood.seal.util.makeToast
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val KIRIN_RELEASES_URL = "https://github.com/Lanzkila/KirinDL/releases"
private val releaseLinkPattern =
    Regex("""\[([^\]]+)]\((https?://[^)]+)\)|(https?://\S+)""")
private val releaseVersionPattern =
    Regex(
        """v?(\d+\.\d+\.\d+(?:\.\d+)?(?:-(?:(?:alpha|beta|rc)\.\d+|devpatch\d+))?)""",
        RegexOption.IGNORE_CASE,
    )

@Composable
fun UpdateDialog(
    onDismissRequest: () -> Unit,
    release: UpdateUtil.Release,
    isUpdateAvailable: Boolean = true,
    onUpdateLaunched: () -> Unit = {},
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isDownloading by remember(release.tagName) { mutableStateOf(false) }
    var downloadProgress by remember(release.tagName) { mutableStateOf<Int?>(null) }
    var downloadJob by remember(release.tagName) { mutableStateOf<Job?>(null) }

    // Keep the complete version name here. Pre-release builds must not be shown as Stable.
    val currentVersion = UpdateUtil.installedVersionName(context).ifBlank { "Unknown" }
    val releaseVersion = release.versionLabel()
    val releaseTitle =
        release.name?.takeIf { it.isNotBlank() }
            ?: release.tagName?.takeIf { it.isNotBlank() }
            ?: if (isUpdateAvailable) "KirinDL $releaseVersion" else "KirinDL release"
    val channelLabel = if (release.preRelease == true) "Pre-release" else "Stable"
    val availableBuilds = release.availableApkVariants()
    val canDownloadInApp = isUpdateAvailable && UpdateUtil.hasInAppAppUpdate(release)

    fun cancelDownload() {
        // Let the cancelled coroutine finish cleanup before the dialog allows a retry.
        downloadJob?.cancel()
    }

    fun startDownload() {
        if (isDownloading) return
        isDownloading = true
        downloadProgress = null
        downloadJob =
            scope.launch {
                try {
                    val apkFile =
                        UpdateUtil.downloadReleaseApk(context, release) { progress ->
                            withContext(Dispatchers.Main.immediate) {
                                downloadProgress = progress
                            }
                        }
                    UpdateUtil.installDownloadedAppUpdate(context, apkFile).getOrThrow()
                    // Keep the release eligible for another prompt if the user cancels Android's
                    // installer. Closing the details screen here does not add it to the dismissed list.
                    onUpdateLaunched()
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: Exception) {
                    context.makeToast(error.message ?: "Could not download the KirinDL update")
                } finally {
                    isDownloading = false
                    downloadProgress = null
                    downloadJob = null
                }
            }
    }

    val dismissOrCancel: () -> Unit = {
        if (isDownloading) cancelDownload() else onDismissRequest()
    }

    UpdateDialogImpl(
        onDismissRequest = dismissOrCancel,
        dialogTitle = if (isUpdateAvailable) "New KirinDL Update" else "What's New",
        releaseTitle = releaseTitle,
        channelLabel = channelLabel,
        availableBuilds = availableBuilds,
        currentVersion = currentVersion,
        releaseVersion = releaseVersion,
        publishedDate = release.publishedAt?.take(10) ?: release.createdAt?.take(10),
        isUpdateAvailable = isUpdateAvailable,
        backgroundUpdateMode = canDownloadInApp,
        isDownloading = isDownloading,
        downloadProgress = downloadProgress,
        onCancelDownload = ::cancelDownload,
        onConfirmUpdate = {
            if (canDownloadInApp) {
                startDownload()
            } else {
                val target =
                    release.htmlUrl?.takeIf { it.startsWith("https://") } ?: KIRIN_RELEASES_URL
                runCatching {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse(target)).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                    )
                }.onFailure {
                    context.makeToast("Could not open the KirinDL release page")
                }
                onDismissRequest()
            }
        },
        releaseNote = release.body.orEmpty(),
    )
}

@OptIn(ExperimentalTextApi::class)
@Composable
fun UpdateDialogImpl(
    onDismissRequest: () -> Unit,
    dialogTitle: String,
    releaseTitle: String,
    channelLabel: String,
    availableBuilds: String?,
    currentVersion: String,
    releaseVersion: String,
    publishedDate: String?,
    isUpdateAvailable: Boolean,
    backgroundUpdateMode: Boolean = false,
    isDownloading: Boolean = false,
    downloadProgress: Int? = null,
    onCancelDownload: (() -> Unit)? = null,
    onConfirmUpdate: () -> Unit,
    releaseNote: String,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Column {
                Text(
                    text = dialogTitle,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = releaseTitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        icon = {
            Icon(
                Icons.Outlined.NewReleases,
                null,
                tint = MaterialTheme.colorScheme.primary,
            )
        },
        confirmButton = {
            Button(onClick = onConfirmUpdate, enabled = !isDownloading) {
                Text(
                    when {
                        isDownloading && downloadProgress != null -> "Downloading ${downloadProgress}%"
                        isDownloading -> "Downloading…"
                        backgroundUpdateMode -> "Update"
                        isUpdateAvailable -> "Open update"
                        else -> "Open release"
                    }
                )
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = if (isDownloading) {
                    onCancelDownload ?: onDismissRequest
                } else {
                    onDismissRequest
                }
            ) {
                Text(
                    text = when {
                        isDownloading -> "Cancel"
                        isUpdateAvailable -> "Later"
                        else -> stringResource(id = R.string.dismiss)
                    }
                )
            }
        },
        text = {
            Column {
                if (isUpdateAvailable) {
                    Text(
                        text = "Installed v$currentVersion  →  Available v$releaseVersion",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                } else {
                    Text(
                        text = "Installed v$currentVersion",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                val releaseMeta =
                    buildList {
                        add(channelLabel)
                        publishedDate?.let { add("Published $it") }
                    }.joinToString("  •  ")

                Text(
                    text = releaseMeta,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                if (isDownloading) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = downloadProgress?.let { "Downloading APK · $it%" }
                            ?: "Downloading APK…",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    if (downloadProgress == null) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    } else {
                        LinearProgressIndicator(
                            progress = { (downloadProgress.coerceIn(0, 100) / 100f) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }

                availableBuilds?.let {
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "APK builds: $it",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                if (backgroundUpdateMode && !isDownloading) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text =
                            "Tap Update to download the APK directly in KirinDL. " +
                                "When it finishes, Android's installer opens so you can confirm the update.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Release notes",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(7.dp))

                // Only the release-note area scrolls. Header, version comparison and buttons
                // stay visible even when GitHub contains a long changelog.
                Column(
                    modifier =
                        Modifier.fillMaxWidth()
                            .heightIn(max = 360.dp)
                            .verticalScroll(rememberScrollState()),
                ) {
                    ReleaseNotesContent(
                        releaseNote.ifBlank {
                            "No release notes were published for this KirinDL version."
                        }
                    )
                }
            }
        },
    )
}

@OptIn(ExperimentalTextApi::class)
@Composable
private fun ReleaseNotesContent(markdown: String) {
    val uriHandler = LocalUriHandler.current
    var inCodeBlock = false

    markdown.lines().forEach { sourceLine ->
        val line = sourceLine.trimEnd()
        val trimmed = line.trim()
        val leadingSpaces = line.takeWhile(Char::isWhitespace).length
        val listIndent = (leadingSpaces / 2).coerceIn(0, 3)

        // The dialog header already shows the release/version. GitHub notes often repeat the same
        // "# KirinDL vX.Y.Z" at the top and again near the footer, which made the popup look noisy.
        val duplicateVersionHeading =
            trimmed.matches(
                Regex(
                    """#{1,3}\s+KirinDL\s+v?\d+\.\d+\.\d+(?:\.\d+)?(?:-[A-Za-z0-9.]+)?""",
                    RegexOption.IGNORE_CASE,
                )
            )

        when {
            duplicateVersionHeading -> Unit

            trimmed.startsWith("~~~") || trimmed.startsWith("```") -> {
                inCodeBlock = !inCodeBlock
                Spacer(modifier = Modifier.height(3.dp))
            }

            trimmed.isBlank() -> Spacer(modifier = Modifier.height(6.dp))

            trimmed == "---" -> {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            }

            inCodeBlock -> {
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                ) {
                    Text(
                        text = line,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        style =
                            MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace
                            ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            trimmed.startsWith(">") -> {
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                ) {
                    ReleaseNoteLine(
                        text = trimmed.removePrefix(">").trim(),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        onOpenUrl = uriHandler::openUri,
                    )
                }
            }

            trimmed.startsWith("### ") -> {
                Spacer(modifier = Modifier.height(4.dp))
                ReleaseNoteLine(
                    text = trimmed.removePrefix("### "),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    onOpenUrl = uriHandler::openUri,
                )
                Spacer(modifier = Modifier.height(2.dp))
            }

            trimmed.startsWith("## ") -> {
                Spacer(modifier = Modifier.height(7.dp))
                ReleaseNoteLine(
                    text = trimmed.removePrefix("## "),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    onOpenUrl = uriHandler::openUri,
                )
                Spacer(modifier = Modifier.height(3.dp))
            }

            trimmed.startsWith("# ") -> {
                Spacer(modifier = Modifier.height(7.dp))
                ReleaseNoteLine(
                    text = trimmed.removePrefix("# "),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    onOpenUrl = uriHandler::openUri,
                )
                Spacer(modifier = Modifier.height(3.dp))
            }

            trimmed.startsWith("- ") || trimmed.startsWith("* ") -> {
                Row(
                    modifier =
                        Modifier.fillMaxWidth()
                            .padding(start = (listIndent * 12).dp, bottom = 3.dp)
                ) {
                    Text(
                        text = "•",
                        modifier = Modifier.padding(end = 8.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    ReleaseNoteLine(
                        text = trimmed.drop(2),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium,
                        onOpenUrl = uriHandler::openUri,
                    )
                }
            }

            trimmed.matches(Regex("""\d+\.\s+.*""")) -> {
                val marker = trimmed.substringBefore(" ") + " "
                val body = trimmed.substringAfter(" ", "")
                Row(
                    modifier =
                        Modifier.fillMaxWidth()
                            .padding(start = (listIndent * 12).dp, bottom = 3.dp)
                ) {
                    Text(
                        text = marker,
                        modifier = Modifier.padding(end = 5.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    ReleaseNoteLine(
                        text = body,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium,
                        onOpenUrl = uriHandler::openUri,
                    )
                }
            }

            else -> {
                ReleaseNoteLine(
                    text = trimmed,
                    modifier = Modifier.padding(bottom = 4.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    onOpenUrl = uriHandler::openUri,
                )
            }
        }
    }
}

@OptIn(ExperimentalTextApi::class)
@Composable
private fun ReleaseNoteLine(
    text: String,
    modifier: Modifier = Modifier,
    style: androidx.compose.ui.text.TextStyle,
    fontWeight: FontWeight? = null,
    onOpenUrl: (String) -> Unit,
) {
    val annotated = buildReleaseAnnotatedString(text)
    ClickableText(
        modifier = modifier,
        text = annotated,
        style =
            style.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = fontWeight,
            ),
        onClick = { index ->
            annotated.getUrlAnnotations(index, index).firstOrNull()?.let { annotation ->
                onOpenUrl(annotation.item.url)
            }
        },
    )
}

@OptIn(ExperimentalTextApi::class)
@Composable
private fun buildReleaseAnnotatedString(source: String): AnnotatedString {
    val linkColor = MaterialTheme.colorScheme.primary
    return buildAnnotatedString {
        var cursor = 0
        releaseLinkPattern.findAll(source).forEach { match ->
            if (match.range.first > cursor) {
                append(cleanInlineMarkdown(source.substring(cursor, match.range.first)))
            }

            val markdownLabel = match.groups[1]?.value
            val markdownUrl = match.groups[2]?.value
            val bareUrl = match.groups[3]?.value
            val label = markdownLabel ?: bareUrl.orEmpty()
            val url = markdownUrl ?: bareUrl.orEmpty()
            val start = length
            append(cleanInlineMarkdown(label))
            val end = length
            addUrlAnnotation(UrlAnnotation(url), start, end)
            addStyle(
                SpanStyle(
                    color = linkColor,
                    textDecoration = TextDecoration.Underline,
                ),
                start,
                end,
            )
            cursor = match.range.last + 1
        }
        if (cursor < source.length) {
            append(cleanInlineMarkdown(source.substring(cursor)))
        }
    }
}

private fun cleanInlineMarkdown(text: String): String =
    text.replace("**", "").replace("__", "").replace("`", "")

private fun UpdateUtil.Release.versionLabel(): String {
    val source = tagName ?: name.orEmpty()
    val parsed = releaseVersionPattern.find(source)?.groupValues?.get(1)
    return parsed?.takeIf { it.isNotBlank() }
        ?: source.removePrefix("v").takeIf { it.isNotBlank() }
        ?: "Unknown"
}

private fun UpdateUtil.Release.availableApkVariants(): String? {
    val names =
        assets.mapNotNull { it.name }
            .filter { it.endsWith(".apk", ignoreCase = true) }

    if (names.isEmpty()) return null

    return buildList {
        if (names.any { it.contains("arm64", ignoreCase = true) }) {
            add("ARM64")
        }
        if (names.any { it.contains("universal", ignoreCase = true) }) {
            add("Universal")
        }
    }.takeIf { it.isNotEmpty() }?.joinToString(" • ")
}

@Preview
@Composable
private fun Preview() {
    UpdateDialogImpl(
        onDismissRequest = {},
        dialogTitle = "New KirinDL Update",
        releaseTitle = "KirinDL v3.1.6",
        channelLabel = "Stable",
        availableBuilds = "ARM64 • Universal",
        currentVersion = "3.1.5",
        releaseVersion = "3.1.6",
        publishedDate = "2026-09-20",
        isUpdateAvailable = true,
        backgroundUpdateMode = false,
        onConfirmUpdate = {},
        releaseNote =
            "## Highlights\n" +
                "- Better updater popup\n" +
                "- Cleaner About update card\n" +
                "- ARM64 and Universal builds",
    )
}
