package com.junkfood.seal.ui.page

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.NewReleases
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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

private const val KIRIN_RELEASES_URL = "https://github.com/Lanzkila/KirinDL/releases"
private val releaseLinkPattern = Regex("""\[([^\]]+)]\((https?://[^)]+)\)|(https?://\S+)""")
private val releaseVersionPattern = Regex("""v?(\d+\.\d+\.\d+(?:-(?:alpha|beta|rc)\.\d+)?)""")

@Composable
fun UpdateDialog(
    onDismissRequest: () -> Unit,
    release: UpdateUtil.Release,
    isUpdateAvailable: Boolean = true,
    backgroundUpdateBusy: Boolean = false,
    onBackgroundUpdate: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val currentVersion = App.packageInfo.versionName?.substringBefore("-") ?: "Unknown"
    val releaseVersion = release.versionLabel()

    UpdateDialogImpl(
        onDismissRequest = onDismissRequest,
        title = release.name ?: release.tagName ?: if (isUpdateAvailable) "KirinDL update" else "What's new",
        currentVersion = currentVersion,
        releaseVersion = releaseVersion,
        publishedDate = release.publishedAt?.take(10) ?: release.createdAt?.take(10),
        isUpdateAvailable = isUpdateAvailable,
        backgroundUpdateMode = isUpdateAvailable && onBackgroundUpdate != null,
        backgroundUpdateBusy = backgroundUpdateBusy,
        onConfirmUpdate = {
            if (isUpdateAvailable && onBackgroundUpdate != null) {
                onBackgroundUpdate()
            } else {
                val target =
                    release.htmlUrl?.takeIf { it.startsWith("https://") } ?: KIRIN_RELEASES_URL
                runCatching {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse(target)).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                    )
                }
            }
            onDismissRequest()
        },
        releaseNote = release.body.orEmpty(),
    )
}

@OptIn(ExperimentalTextApi::class)
@Composable
fun UpdateDialogImpl(
    onDismissRequest: () -> Unit,
    title: String,
    currentVersion: String,
    releaseVersion: String,
    publishedDate: String?,
    isUpdateAvailable: Boolean,
    backgroundUpdateMode: Boolean = false,
    backgroundUpdateBusy: Boolean = false,
    onConfirmUpdate: () -> Unit,
    releaseNote: String,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(title) },
        icon = { Icon(Icons.Outlined.NewReleases, null, tint = MaterialTheme.colorScheme.primary) },
        confirmButton = {
            Button(
                onClick = onConfirmUpdate,
                enabled = !backgroundUpdateBusy,
            ) {
                Text(
                    when {
                        backgroundUpdateBusy -> "Starting…"
                        backgroundUpdateMode -> "Update"
                        isUpdateAvailable -> "View update"
                        else -> "Open release"
                    }
                )
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismissRequest) {
                Text(text = if (isUpdateAvailable) "Later" else stringResource(id = R.string.dismiss))
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
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

                publishedDate?.let {
                    Text(
                        text = "Published $it",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                if (backgroundUpdateMode) {
                    Text(
                        text =
                            "Tap Update to download the APK in the background. " +
                                "Android keeps the download progress in your notification shade.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                HorizontalDivider()
                Text(
                    text = "Release notes",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                ReleaseNotesContent(
                    releaseNote.ifBlank { "A new KirinDL release is available." }
                )
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

        when {
            trimmed.startsWith("```") -> {
                inCodeBlock = !inCodeBlock
            }

            trimmed.isBlank() -> Spacer(modifier = Modifier.height(2.dp))

            trimmed == "---" -> HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            inCodeBlock -> {
                Text(
                    text = trimmed,
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            trimmed.startsWith("### ") -> ReleaseNoteLine(
                text = trimmed.removePrefix("### "),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                onOpenUrl = uriHandler::openUri,
            )

            trimmed.startsWith("## ") -> ReleaseNoteLine(
                text = trimmed.removePrefix("## "),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                onOpenUrl = uriHandler::openUri,
            )

            trimmed.startsWith("# ") -> ReleaseNoteLine(
                text = trimmed.removePrefix("# "),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                onOpenUrl = uriHandler::openUri,
            )

            trimmed.startsWith("- ") || trimmed.startsWith("* ") -> {
                Row(modifier = Modifier.fillMaxWidth()) {
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

            else -> ReleaseNoteLine(
                text = trimmed,
                style = MaterialTheme.typography.bodyMedium,
                onOpenUrl = uriHandler::openUri,
            )
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
        style = style.copy(
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
                SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline),
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
    return releaseVersionPattern.find(source)?.groupValues?.get(1) ?: source.removePrefix("v")
}

@Preview
@Composable
private fun Preview() {
    UpdateDialogImpl(
        onDismissRequest = {},
        title = "KirinDL v3.1.2",
        currentVersion = "3.1.1",
        releaseVersion = "3.1.2",
        publishedDate = "2026-09-03",
        isUpdateAvailable = true,
        backgroundUpdateMode = true,
        onConfirmUpdate = {},
        releaseNote = "## Highlights\n- Better updater\n- Cleaner settings",
    )
}
