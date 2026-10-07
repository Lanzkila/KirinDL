package com.junkfood.seal.ui.page.settings.format

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ClosedCaption
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Subtitles
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.junkfood.seal.R
import com.junkfood.seal.ui.common.booleanState
import com.junkfood.seal.ui.common.intState
import com.junkfood.seal.ui.component.BackButton
import com.junkfood.seal.ui.component.ConfirmButton
import com.junkfood.seal.ui.component.DismissButton
import com.junkfood.seal.ui.component.PreferenceInfo
import com.junkfood.seal.ui.component.PreferenceItem
import com.junkfood.seal.ui.component.PreferenceSwitch
import com.junkfood.seal.ui.component.PreferenceSwitchWithContainer
import com.junkfood.seal.util.AUTO_SUBTITLE
import com.junkfood.seal.util.AUTO_TRANSLATED_SUBTITLES
import com.junkfood.seal.util.EMBED_SUBTITLE
import com.junkfood.seal.util.EXTRACT_AUDIO
import com.junkfood.seal.util.KEEP_SUBTITLE_FILES
import com.junkfood.seal.util.ORIGINAL_SUBTITLE
import com.junkfood.seal.util.PreferenceStrings
import com.junkfood.seal.util.PreferenceUtil.getString
import com.junkfood.seal.util.PreferenceUtil.updateBoolean
import com.junkfood.seal.util.SPONSORBLOCK
import com.junkfood.seal.util.SUBTITLE
import com.junkfood.seal.util.SUBTITLE_LANGUAGE
import com.junkfood.seal.util.SUBTITLE_LANGUAGE_ALL
import com.junkfood.seal.util.SUBTITLE_LANGUAGE_MODE
import com.junkfood.seal.util.SUBTITLE_LANGUAGE_ORIGINAL
import com.junkfood.seal.util.SUBTITLE_LANGUAGE_SELECTED

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubtitlePreference(onNavigateBack: () -> Unit) {
    val scrollBehavior =
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
            rememberTopAppBarState(),
            canScroll = { true },
        )
    var downloadSubtitle by SUBTITLE.booleanState
    val sponsorBlock by SPONSORBLOCK.booleanState
    //    var keepSubtitleFile by KEEP_SUBTITLE_FILES.booleanState
    var embedSubtitle by EMBED_SUBTITLE.booleanState
    var originalSubtitle by ORIGINAL_SUBTITLE.booleanState
    var autoSubtitle by AUTO_SUBTITLE.booleanState
    var subtitleLanguageMode by SUBTITLE_LANGUAGE_MODE.intState
    var autoTranslatedSubtitle by AUTO_TRANSLATED_SUBTITLES.booleanState

    var showLanguageDialog by remember { mutableStateOf(false) }
    var showConversionDialog by remember { mutableStateOf(false) }
    var showEmbedSubtitleDialog by remember { mutableStateOf(false) }
    var showAutoTranslateDialog by remember { mutableStateOf(false) }

    val subtitleFormatText by
        remember(showConversionDialog) {
            mutableStateOf(PreferenceStrings.getSubtitleConversionFormat())
        }

    val subtitleLang by
        remember(showLanguageDialog) { mutableStateOf(SUBTITLE_LANGUAGE.getString()) }
    val subtitleLanguageDescription =
        when (subtitleLanguageMode) {
            SUBTITLE_LANGUAGE_ORIGINAL -> "Original languages"
            SUBTITLE_LANGUAGE_ALL -> "All languages"
            else -> "Selected • $subtitleLang"
        }
    val sponsorBlockText = stringResource(id = R.string.subtitle_sponsorblock)
    val embedSubtitleText = stringResource(R.string.embed_subtitles_mkv_msg)

    val hint by
        remember(sponsorBlock, embedSubtitle) {
            derivedStateOf {
                StringBuilder()
                    .apply {
                        if (sponsorBlock) append(sponsorBlockText)
                        if (isNotEmpty()) append("\n\n")
                        if (embedSubtitle) append(embedSubtitleText)
                    }
                    .toString()
            }
        }

    val downloadAudio by EXTRACT_AUDIO.booleanState

    Scaffold(
        modifier = Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(modifier = Modifier, text = stringResource(id = R.string.subtitle))
                },
                navigationIcon = { BackButton { onNavigateBack() } },
                scrollBehavior = scrollBehavior,
            )
        },
        content = {
            LazyColumn(modifier = Modifier, contentPadding = it) {
                item {
                    PreferenceSwitchWithContainer(
                        title = stringResource(id = R.string.download_subtitles),
                        isChecked = downloadSubtitle,
                        onClick = {
                            downloadSubtitle = !downloadSubtitle
                            SUBTITLE.updateBoolean(downloadSubtitle)
                        },
                        icon = null,
                    )
                }
                item {
                    PreferenceSwitch(
                        title = "Original / manual subtitles",
                        description = "Subtitle tracks provided by the website or uploader",
                        icon = Icons.Outlined.Subtitles,
                        isChecked = originalSubtitle,
                        enabled = downloadSubtitle,
                        onClick = {
                            originalSubtitle = !originalSubtitle
                            ORIGINAL_SUBTITLE.updateBoolean(originalSubtitle)
                        },
                    )
                }

                item {
                    PreferenceSwitch(
                        title = "Auto-generated subtitles",
                        icon = Icons.Outlined.ClosedCaption,
                        description = "Captions generated automatically by the website",
                        isChecked = autoSubtitle,
                        enabled = downloadSubtitle,
                        onClick = {
                            autoSubtitle = !autoSubtitle
                            AUTO_SUBTITLE.updateBoolean(autoSubtitle)
                        },
                    )
                }

                item {
                    PreferenceItem(
                        title = "Subtitle language mode",
                        icon = Icons.Outlined.Language,
                        description = subtitleLanguageDescription,
                        enabled = downloadSubtitle && (originalSubtitle || autoSubtitle),
                        onClick = { showLanguageDialog = true },
                    )
                }

                item {
                    PreferenceItem(
                        title = stringResource(id = R.string.convert_subtitle),
                        description = subtitleFormatText,
                        icon = Icons.Outlined.Sync,
                        enabled = downloadSubtitle && (originalSubtitle || autoSubtitle),
                    ) {
                        showConversionDialog = true
                    }
                }

                item {
                    PreferenceSwitch(
                        title = stringResource(id = R.string.auto_translated_subtitles),
                        icon = Icons.Outlined.Translate,
                        isChecked = autoTranslatedSubtitle,
                        enabled = downloadSubtitle && autoSubtitle,
                    ) {
                        if (!autoTranslatedSubtitle) {
                            showAutoTranslateDialog = true
                        } else {
                            autoTranslatedSubtitle = false
                            AUTO_TRANSLATED_SUBTITLES.updateBoolean(false)
                        }
                    }
                }

                item {
                    androidx.compose.material3.HorizontalDivider()
                    PreferenceSwitch(
                        title = stringResource(id = R.string.embed_subtitles),
                        description = stringResource(id = R.string.embed_subtitles_desc),
                        isChecked = embedSubtitle,
                        enabled = downloadSubtitle && (originalSubtitle || autoSubtitle) && !downloadAudio,
                        onClick = {
                            if (embedSubtitle) {
                                embedSubtitle = false
                                EMBED_SUBTITLE.updateBoolean(false)
                            } else {
                                showEmbedSubtitleDialog = true
                            }
                        },
                        icon = Icons.Outlined.Subtitles,
                    )
                }

                item {
                    Column {
                        var keepSubtitles by KEEP_SUBTITLE_FILES.booleanState
                        PreferenceSwitch(
                            title = stringResource(id = R.string.keep_subtitle_files),
                            description = null,
                            isChecked = keepSubtitles,
                            enabled =
                                downloadSubtitle &&
                                    (originalSubtitle || autoSubtitle) &&
                                    !downloadAudio &&
                                    embedSubtitle,
                            onClick = {
                                keepSubtitles = !keepSubtitles
                                KEEP_SUBTITLE_FILES.updateBoolean(keepSubtitles)
                            },
                            icon = Icons.Outlined.Save,
                        )
                    }
                }

                item { if (hint.isNotEmpty()) PreferenceInfo(text = hint) }
            }
        },
    )
    if (showLanguageDialog) {
        SubtitleLanguageDialog(
            onDismissRequest = { showLanguageDialog = false },
            onModeChanged = { subtitleLanguageMode = it },
        )
    }
    if (showConversionDialog) SubtitleConversionDialog { showConversionDialog = false }
    if (showEmbedSubtitleDialog) {
        AlertDialog(
            onDismissRequest = { showEmbedSubtitleDialog = false },
            icon = { Icon(Icons.Outlined.Subtitles, null, tint = MaterialTheme.colorScheme.primary) },
            confirmButton = {
                ConfirmButton {
                    embedSubtitle = true
                    EMBED_SUBTITLE.updateBoolean(true)
                    showEmbedSubtitleDialog = false
                }
            },
            dismissButton = { DismissButton { showEmbedSubtitleDialog = false } },
            text = { Text(stringResource(id = R.string.embed_subtitles_mkv_msg)) },
            title = {
                Text(
                    stringResource(id = R.string.enable_experimental_feature),
                    textAlign = TextAlign.Center,
                )
            },
        )
    }
    if (showAutoTranslateDialog) {
        AlertDialog(
            onDismissRequest = { showAutoTranslateDialog = false },
            icon = { Icon(Icons.Outlined.Translate, null, tint = MaterialTheme.colorScheme.primary) },
            confirmButton = {
                ConfirmButton {
                    autoTranslatedSubtitle = true
                    AUTO_TRANSLATED_SUBTITLES.updateBoolean(true)
                    showAutoTranslateDialog = false
                }
            },
            dismissButton = { DismissButton { showAutoTranslateDialog = false } },
            text = { Text(stringResource(id = R.string.auto_translated_subtitles_msg)) },
            title = {
                Text(
                    stringResource(id = R.string.enable_experimental_feature),
                    textAlign = TextAlign.Center,
                )
            },
        )
    }
}
