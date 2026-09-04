package com.junkfood.seal.ui.page.settings.troubleshooting

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Cookie
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Spellcheck
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.junkfood.seal.R
import com.junkfood.seal.ui.common.Route
import com.junkfood.seal.ui.common.booleanState
import com.junkfood.seal.ui.component.PreferenceInfo
import com.junkfood.seal.ui.component.PreferenceItem
import com.junkfood.seal.ui.component.PreferenceSubtitle
import com.junkfood.seal.ui.component.PreferenceSwitch
import com.junkfood.seal.ui.page.settings.BasePreferencePage
import com.junkfood.seal.util.PreferenceUtil.getString
import com.junkfood.seal.util.PreferenceUtil.updateBoolean
import com.junkfood.seal.util.RESTRICT_FILENAMES
import com.junkfood.seal.util.YT_DLP_VERSION

@Composable
fun TroubleShootingPage(
    modifier: Modifier = Modifier,
    onNavigateTo: (String) -> Unit,
    onBack: () -> Unit,
) {
    val uriHandler = LocalUriHandler.current

    BasePreferencePage(
        modifier = modifier,
        title = stringResource(R.string.trouble_shooting),
        onBack = onBack,
    ) {
        LazyColumn(contentPadding = it) {
            item {
                androidx.compose.material3.OutlinedCard(modifier = Modifier.padding(16.dp)) {
                    PreferenceInfo(
                        modifier = Modifier,
                        text = stringResource(R.string.issue_tracker_hint),
                    )
                    val knownIssueUrlSeal = "https://github.com/Lanzkila/KirinDownloader-Seal/issues"
                    PreferenceItem(
                        title = "KirinDownloader Issue Tracker",
                        description = null,
                        icon = Icons.AutoMirrored.Outlined.OpenInNew,
                        onClick = { uriHandler.openUri(knownIssueUrlSeal) },
                    )

                    val knownIssueUrlYtdlp = "https://github.com/yt-dlp/yt-dlp/issues/3766"
                    PreferenceItem(
                        title = "yt-dlp Issue Tracker",
                        description = null,
                        icon = Icons.AutoMirrored.Outlined.OpenInNew,
                        onClick = { uriHandler.openUri(knownIssueUrlYtdlp) },
                    )

                    Spacer(Modifier.height(8.dp))
                }
            }

            item { PreferenceSubtitle(text = "Engine diagnostics") }
            item {
                PreferenceItem(
                    title = "yt-dlp status",
                    description =
                        "Installed: ${YT_DLP_VERSION.getString().ifBlank { "Unknown" }} • Updates are managed in Engine Updates",
                    icon = Icons.Outlined.Settings,
                    onClick = { onNavigateTo(Route.GALLERY_DL_SETTINGS) },
                )
            }
            item {
                PreferenceInfo(
                    text =
                        "Troubleshooting only shows engine status. Manual and automatic yt-dlp/gallery-dl updates now live in one place: Engine Updates."
                )
            }

            item { PreferenceSubtitle(text = stringResource(R.string.network)) }
            item {
                PreferenceItem(
                    title = stringResource(R.string.cookies),
                    description = stringResource(R.string.cookies_desc),
                    icon = Icons.Outlined.Cookie,
                    onClick = { onNavigateTo(Route.COOKIE_PROFILE) },
                )
            }
            item { PreferenceSubtitle(text = stringResource(R.string.download_directory)) }
            item {
                var restrictFilenames by RESTRICT_FILENAMES.booleanState
                PreferenceSwitch(
                    title = stringResource(id = R.string.restrict_filenames),
                    icon = Icons.Outlined.Spellcheck,
                    description = stringResource(id = R.string.restrict_filenames_desc),
                    isChecked = restrictFilenames,
                ) {
                    restrictFilenames = !restrictFilenames
                    RESTRICT_FILENAMES.updateBoolean(restrictFilenames)
                }
            }
        }
    }
}
