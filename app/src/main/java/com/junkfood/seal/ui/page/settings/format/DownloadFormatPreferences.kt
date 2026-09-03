package com.junkfood.seal.ui.page.settings.format

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArtTrack
import androidx.compose.material.icons.outlined.ContentCut
import androidx.compose.material.icons.outlined.Crop
import androidx.compose.material.icons.outlined.HighQuality
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Sort
import androidx.compose.material.icons.outlined.SpatialAudioOff
import androidx.compose.material.icons.outlined.Subtitles
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.VideoFile
import androidx.compose.material.icons.outlined.VideoSettings
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
import com.junkfood.seal.ui.component.PreferenceSubtitle
import com.junkfood.seal.ui.component.PreferenceSwitch
import com.junkfood.seal.ui.component.PreferenceSwitchWithDivider
import com.junkfood.seal.ui.component.DialogSingleChoiceItemVariant
import com.junkfood.seal.util.AUDIO_CONVERSION_FORMAT
import com.junkfood.seal.util.AUDIO_FORMAT
import com.junkfood.seal.util.AUDIO_QUALITY
import com.junkfood.seal.util.AUDIO_CODEC
import com.junkfood.seal.util.AUDIO_COVER_MODE
import com.junkfood.seal.util.AUDIO_COVER_FORMAT
import com.junkfood.seal.util.AUDIO_CODEC_AUTO
import com.junkfood.seal.util.AUDIO_CODEC_AAC
import com.junkfood.seal.util.AUDIO_CODEC_OPUS
import com.junkfood.seal.util.AUDIO_CODEC_VORBIS
import com.junkfood.seal.util.AUDIO_CODEC_MP3
import com.junkfood.seal.util.AUDIO_CODEC_FLAC
import com.junkfood.seal.util.AUDIO_CODEC_ALAC
import com.junkfood.seal.util.AUDIO_COVER_LEGACY
import com.junkfood.seal.util.AUDIO_COVER_NONE
import com.junkfood.seal.util.AUDIO_COVER_EMBED
import com.junkfood.seal.util.AUDIO_COVER_SAVE
import com.junkfood.seal.util.AUDIO_COVER_BOTH
import com.junkfood.seal.util.AUDIO_COVER_FORMAT_AUTO
import com.junkfood.seal.util.AUDIO_COVER_FORMAT_JPG
import com.junkfood.seal.util.AUDIO_COVER_FORMAT_PNG
import com.junkfood.seal.util.AUDIO_COVER_FORMAT_WEBP
import com.junkfood.seal.util.AUDIO_CONVERT
import com.junkfood.seal.util.CROP_ARTWORK
import com.junkfood.seal.util.CUSTOM_COMMAND
import com.junkfood.seal.util.DownloadUtil
import com.junkfood.seal.util.DownloadUtil.toFormatSorter
import com.junkfood.seal.util.EMBED_METADATA
import com.junkfood.seal.util.EMBED_SUBTITLE
import com.junkfood.seal.util.EXTRACT_AUDIO
import com.junkfood.seal.util.FORMAT_SELECTION
import com.junkfood.seal.util.FORMAT_SORTING
import com.junkfood.seal.util.MERGE_MULTI_AUDIO_STREAM
import com.junkfood.seal.util.MERGE_OUTPUT_MKV
import com.junkfood.seal.util.PreferenceStrings
import com.junkfood.seal.util.PreferenceUtil
import com.junkfood.seal.util.PreferenceUtil.getBoolean
import com.junkfood.seal.util.PreferenceUtil.getInt
import com.junkfood.seal.util.PreferenceUtil.getString
import com.junkfood.seal.util.PreferenceUtil.updateBoolean
import com.junkfood.seal.util.PreferenceUtil.updateInt
import com.junkfood.seal.util.PreferenceUtil.updateString
import com.junkfood.seal.util.SORTING_FIELDS
import com.junkfood.seal.util.SUBTITLE
import com.junkfood.seal.util.VIDEO_CLIP
import com.junkfood.seal.util.VIDEO_FORMAT
import com.junkfood.seal.util.VIDEO_CODEC
import com.junkfood.seal.util.VIDEO_CONTAINER
import com.junkfood.seal.util.VIDEO_CODEC_AUTO
import com.junkfood.seal.util.VIDEO_CODEC_H264
import com.junkfood.seal.util.VIDEO_CODEC_VP9
import com.junkfood.seal.util.VIDEO_CODEC_AV1
import com.junkfood.seal.util.VIDEO_CODEC_HEVC
import com.junkfood.seal.util.VIDEO_CONTAINER_AUTO
import com.junkfood.seal.util.VIDEO_CONTAINER_MP4
import com.junkfood.seal.util.VIDEO_CONTAINER_WEBM
import com.junkfood.seal.util.VIDEO_CONTAINER_MKV
import com.junkfood.seal.util.VIDEO_QUALITY

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadFormatPreferences(onNavigateBack: () -> Unit, navigateToSubtitlePage: () -> Unit) {
    val scrollBehavior =
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
            rememberTopAppBarState(),
            canScroll = { true },
        )

    var audioSwitch by remember { mutableStateOf(EXTRACT_AUDIO.getBoolean()) }
    var isArtworkCroppingEnabled by remember { mutableStateOf(CROP_ARTWORK.getBoolean()) }
    val downloadSubtitle by SUBTITLE.booleanState
    val embedSubtitle by EMBED_SUBTITLE.booleanState
    var remuxToMkv by MERGE_OUTPUT_MKV.booleanState
    var embedMetadata by EMBED_METADATA.booleanState

    var showAudioFormatDialog by remember { mutableStateOf(false) }
    var showAudioQualityDialog by remember { mutableStateOf(false) }
    var showAudioConvertDialog by remember { mutableStateOf(false) }
    var showVideoQualityDialog by remember { mutableStateOf(false) }
    var showVideoFormatDialog by remember { mutableStateOf(false) }
    var showFormatSorterDialog by remember { mutableStateOf(false) }
    var showYtdlpFormatProfileDialog by remember { mutableStateOf(false) }
    var showVideoClipDialog by remember { mutableStateOf(false) }
    var showAudioCodecDialog by remember { mutableStateOf(false) }
    var showAudioCoverDialog by remember { mutableStateOf(false) }
    var showAudioCoverFormatDialog by remember { mutableStateOf(false) }
    var showVideoCodecDialog by remember { mutableStateOf(false) }
    var showVideoContainerDialog by remember { mutableStateOf(false) }

    var videoFormat by VIDEO_FORMAT.intState
    var videoQuality by VIDEO_QUALITY.intState
    var audioFormat by AUDIO_FORMAT.intState
    var audioQuality by AUDIO_QUALITY.intState
    var audioCodec by AUDIO_CODEC.intState
    var audioCoverMode by AUDIO_COVER_MODE.intState
    var audioCoverFormat by AUDIO_COVER_FORMAT.intState
    var videoCodec by VIDEO_CODEC.intState
    var videoContainer by VIDEO_CONTAINER.intState
    var convertFormat by AUDIO_CONVERSION_FORMAT.intState
    var sortingFields by
        remember(showFormatSorterDialog) { mutableStateOf(SORTING_FIELDS.getString()) }
    var convertAudio by AUDIO_CONVERT.booleanState
    var isFormatSortingEnabled by FORMAT_SORTING.booleanState
    var isVideoClipEnabled by VIDEO_CLIP.booleanState
    var isFormatSelectionEnabled by FORMAT_SELECTION.booleanState
    var mergeAudioStream by MERGE_MULTI_AUDIO_STREAM.booleanState
    var showMergeAudioDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text(modifier = Modifier, text = stringResource(id = R.string.format)) },
                navigationIcon = { BackButton { onNavigateBack() } },
                scrollBehavior = scrollBehavior,
            )
        },
        content = {
            val isCustomCommandEnabled by remember { mutableStateOf(CUSTOM_COMMAND.getBoolean()) }
            LazyColumn(contentPadding = it) {
                if (isCustomCommandEnabled)
                    item {
                        PreferenceInfo(
                            text = stringResource(id = R.string.custom_command_enabled_hint)
                        )
                    }
                item { PreferenceSubtitle(text = stringResource(id = R.string.audio)) }
                item {
                    PreferenceSwitch(
                        title = stringResource(id = R.string.extract_audio),
                        description = stringResource(id = R.string.extract_audio_summary),
                        icon = Icons.Outlined.MusicNote,
                        isChecked = audioSwitch,
                        enabled = !isCustomCommandEnabled,
                        onClick = {
                            audioSwitch = !audioSwitch
                            PreferenceUtil.updateValue(EXTRACT_AUDIO, audioSwitch)
                        },
                    )
                }
                item {
                    PreferenceItem(
                        title = stringResource(id = R.string.audio_format_preference),
                        description = PreferenceStrings.getAudioFormatDesc(audioFormat),
                        icon = Icons.Outlined.MusicNote,
                        enabled = audioSwitch && !isCustomCommandEnabled && !isFormatSortingEnabled,
                        onClick = { showAudioFormatDialog = true },
                    )
                }
                item {
                    PreferenceItem(
                        title = "Audio bitrate",
                        description = PreferenceStrings.getAudioQualityDesc(audioQuality),
                        icon = Icons.Outlined.HighQuality,
                        enabled = audioSwitch && !isCustomCommandEnabled && !isFormatSortingEnabled,
                        onClick = { showAudioQualityDialog = true },
                    )
                }
                item {
                    PreferenceItem(
                        title = "Audio codec",
                        description = PreferenceStrings.getAudioCodecDesc(audioCodec),
                        icon = Icons.Outlined.Tune,
                        enabled = audioSwitch && !isCustomCommandEnabled && !isFormatSortingEnabled,
                        onClick = { showAudioCodecDialog = true },
                    )
                }
                item {
                    PreferenceItem(
                        title = "Thumbnail / cover artwork",
                        description = PreferenceStrings.getAudioCoverModeDesc(audioCoverMode),
                        icon = Icons.Outlined.ArtTrack,
                        enabled = audioSwitch && !isCustomCommandEnabled,
                        onClick = { showAudioCoverDialog = true },
                    )
                }
                item {
                    PreferenceItem(
                        title = "Cover image format",
                        description = PreferenceStrings.getAudioCoverFormatDesc(audioCoverFormat),
                        icon = Icons.Outlined.ArtTrack,
                        enabled = audioSwitch && !isCustomCommandEnabled && audioCoverMode != AUDIO_COVER_NONE,
                        onClick = { showAudioCoverFormatDialog = true },
                    )
                }
                item {
                    PreferenceSwitchWithDivider(
                        title = stringResource(R.string.convert_audio_format),
                        description = PreferenceStrings.getAudioConvertDesc(convertFormat),
                        icon = Icons.Outlined.Sync,
                        enabled = audioSwitch && !isCustomCommandEnabled,
                        onClick = { showAudioConvertDialog = true },
                        isChecked = convertAudio,
                        onChecked = {
                            convertAudio = !convertAudio
                            AUDIO_CONVERT.updateBoolean(convertAudio)
                        },
                    )
                }
                item {
                    PreferenceSwitch(
                        title = stringResource(id = R.string.embed_metadata),
                        description = stringResource(id = R.string.embed_metadata_desc),
                        enabled = audioSwitch && !isCustomCommandEnabled,
                        isChecked = embedMetadata,
                        icon = Icons.Outlined.ArtTrack,
                        onClick = {
                            embedMetadata = !embedMetadata
                            EMBED_METADATA.updateBoolean(embedMetadata)
                        },
                    )
                }
                item {
                    PreferenceSwitch(
                        title = stringResource(R.string.crop_artwork),
                        description = stringResource(R.string.crop_artwork_desc),
                        icon = Icons.Outlined.Crop,
                        enabled = embedMetadata && audioSwitch && !isCustomCommandEnabled,
                        isChecked = isArtworkCroppingEnabled,
                    ) {
                        isArtworkCroppingEnabled = !isArtworkCroppingEnabled
                        PreferenceUtil.updateValue(CROP_ARTWORK, isArtworkCroppingEnabled)
                    }
                }
                item { PreferenceSubtitle(text = stringResource(id = R.string.video)) }
                item {
                    PreferenceItem(
                        title = stringResource(R.string.video_format_preference),
                        description = PreferenceStrings.getVideoFormatLabel(videoFormat),
                        icon = Icons.Outlined.VideoFile,
                        enabled = !audioSwitch && !isCustomCommandEnabled && !isFormatSortingEnabled,
                    ) {
                        showVideoFormatDialog = true
                    }
                }
                item {
                    PreferenceItem(
                        title = stringResource(id = R.string.video_quality),
                        description = PreferenceStrings.getVideoResolutionDesc(videoQuality),
                        icon = Icons.Outlined.HighQuality,
                        enabled = !audioSwitch && !isCustomCommandEnabled && !isFormatSortingEnabled,
                    ) {
                        showVideoQualityDialog = true
                    }
                }
                item {
                    PreferenceItem(
                        title = "Video codec",
                        description = PreferenceStrings.getVideoCodecDesc(videoCodec),
                        icon = Icons.Outlined.VideoSettings,
                        enabled = !audioSwitch && !isCustomCommandEnabled && !isFormatSortingEnabled,
                        onClick = { showVideoCodecDialog = true },
                    )
                }
                item {
                    PreferenceItem(
                        title = "Video output format",
                        description = PreferenceStrings.getVideoContainerDesc(videoContainer),
                        icon = Icons.Outlined.Movie,
                        enabled = !audioSwitch && !isCustomCommandEnabled && !isFormatSortingEnabled,
                        onClick = { showVideoContainerDialog = true },
                    )
                } /*                item {
                      var embedThumbnail by EMBED_THUMBNAIL.booleanState

                      PreferenceSwitch(
                          title = stringResource(id = R.string.embed_thumbnail),
                          description = stringResource(id = R.string.embed_thumbnail_desc),
                          icon = Icons.Outlined.Photo,
                          isChecked = embedThumbnail,
                          enabled = !isCustomCommandEnabled && !audioSwitch
                      ) {
                          embedThumbnail = !embedThumbnail
                          EMBED_THUMBNAIL.updateBoolean(embedThumbnail)
                      }
                  }*/

                item {
                    PreferenceSwitch(
                        title = stringResource(id = R.string.remux_container_mkv),
                        description = stringResource(id = R.string.remux_container_mkv_desc),
                        isChecked = (downloadSubtitle && embedSubtitle) || remuxToMkv,
                        icon = Icons.Outlined.Movie,
                        enabled =
                            !(downloadSubtitle && embedSubtitle) &&
                                !isCustomCommandEnabled &&
                                !audioSwitch &&
                                videoContainer == VIDEO_CONTAINER_AUTO,
                        onClick = {
                            remuxToMkv = !remuxToMkv
                            MERGE_OUTPUT_MKV.updateBoolean(remuxToMkv)
                        },
                    )
                }
                if (downloadSubtitle && embedSubtitle) {
                    item {
                        PreferenceInfo(text = stringResource(id = R.string.embed_subtitles_mkv_msg))
                    }
                }

                item { PreferenceSubtitle(text = stringResource(id = R.string.advanced_settings)) }
                item {
                    PreferenceItem(
                        title = stringResource(id = R.string.subtitle),
                        icon = Icons.Outlined.Subtitles,
                        enabled = !isCustomCommandEnabled,
                        description = stringResource(id = R.string.subtitle_desc),
                    ) {
                        navigateToSubtitlePage()
                    }
                }
                item {
                    PreferenceItem(
                        title = "yt-dlp format profile",
                        description = ytdlpFormatProfileLabel(isFormatSortingEnabled, sortingFields),
                        icon = Icons.Outlined.Tune,
                        enabled = !isCustomCommandEnabled,
                        onClick = { showYtdlpFormatProfileDialog = true },
                    )
                }
                item {
                    PreferenceSwitchWithDivider(
                        title = stringResource(id = R.string.format_sorting),
                        icon = Icons.Outlined.Sort,
                        description = stringResource(id = R.string.format_sorting_desc),
                        enabled = !isCustomCommandEnabled,
                        isChecked = isFormatSortingEnabled,
                        onChecked = {
                            isFormatSortingEnabled = !isFormatSortingEnabled
                            FORMAT_SORTING.updateBoolean(isFormatSortingEnabled)
                        },
                        onClick = { showFormatSorterDialog = true },
                    )
                }
                item {
                    PreferenceSwitch(
                        title = stringResource(id = R.string.format_selection),
                        icon = Icons.Outlined.VideoSettings,
                        enabled = !isCustomCommandEnabled,
                        description = stringResource(id = R.string.format_selection_desc),
                        isChecked = isFormatSelectionEnabled,
                    ) {
                        isFormatSelectionEnabled = !isFormatSelectionEnabled
                        PreferenceUtil.updateValue(FORMAT_SELECTION, isFormatSelectionEnabled)
                    }
                }
                item {
                    PreferenceSwitch(
                        title = stringResource(id = R.string.clip_video),
                        description = stringResource(id = R.string.clip_video_desc),
                        icon = Icons.Outlined.ContentCut,
                        isChecked = isVideoClipEnabled,
                        enabled = !isCustomCommandEnabled && isFormatSelectionEnabled,
                    ) {
                        if (!isVideoClipEnabled) showVideoClipDialog = true
                        else {
                            isVideoClipEnabled = false
                            VIDEO_CLIP.updateBoolean(false)
                        }
                    }
                }
                item {
                    PreferenceSwitch(
                        title = stringResource(id = R.string.merge_audiostream),
                        description = stringResource(id = R.string.merge_audiostream_desc),
                        isChecked = mergeAudioStream,
                        icon = Icons.Outlined.SpatialAudioOff,
                        onClick = {
                            if (mergeAudioStream) {
                                mergeAudioStream = false
                                MERGE_MULTI_AUDIO_STREAM.updateBoolean(false)
                            } else {
                                showMergeAudioDialog = true
                            }
                        },
                        enabled = !isCustomCommandEnabled && isFormatSelectionEnabled,
                    )
                }
            }
        },
    )
    if (showAudioFormatDialog) {
        AudioFormatDialog {
            audioFormat = AUDIO_FORMAT.getInt()
            showAudioFormatDialog = false
        }
    }
    if (showAudioQualityDialog) {
        AudioQualityDialog {
            audioQuality = AUDIO_QUALITY.getInt()
            showAudioQualityDialog = false
        }
    }
    if (showAudioCodecDialog) {
        SimpleFormatChoiceDialog(
            title = "Audio codec",
            selected = audioCodec,
            options = listOf(
                AUDIO_CODEC_AUTO to "Auto / source best",
                AUDIO_CODEC_AAC to "AAC",
                AUDIO_CODEC_OPUS to "Opus",
                AUDIO_CODEC_VORBIS to "Vorbis",
                AUDIO_CODEC_MP3 to "MP3",
                AUDIO_CODEC_FLAC to "FLAC",
                AUDIO_CODEC_ALAC to "ALAC",
            ),
            onDismiss = { showAudioCodecDialog = false },
            onSelect = { audioCodec = it; AUDIO_CODEC.updateInt(it); showAudioCodecDialog = false },
        )
    }
    if (showAudioCoverDialog) {
        SimpleFormatChoiceDialog(
            title = "Thumbnail / cover artwork",
            selected = audioCoverMode,
            options = listOf(
                AUDIO_COVER_LEGACY to "Automatic (current behavior)",
                AUDIO_COVER_NONE to "No cover",
                AUDIO_COVER_EMBED to "Embed cover",
                AUDIO_COVER_SAVE to "Save cover file",
                AUDIO_COVER_BOTH to "Embed + save cover",
            ),
            onDismiss = { showAudioCoverDialog = false },
            onSelect = { audioCoverMode = it; AUDIO_COVER_MODE.updateInt(it); showAudioCoverDialog = false },
        )
    }
    if (showAudioCoverFormatDialog) {
        SimpleFormatChoiceDialog(
            title = "Cover image format",
            selected = audioCoverFormat,
            options = listOf(
                AUDIO_COVER_FORMAT_AUTO to "Auto / source format",
                AUDIO_COVER_FORMAT_JPG to "JPG",
                AUDIO_COVER_FORMAT_PNG to "PNG",
                AUDIO_COVER_FORMAT_WEBP to "WebP",
            ),
            onDismiss = { showAudioCoverFormatDialog = false },
            onSelect = {
                audioCoverFormat = it
                AUDIO_COVER_FORMAT.updateInt(it)
                showAudioCoverFormatDialog = false
            },
        )
    }
    if (showAudioConvertDialog) {
        AudioConversionDialog(
            onDismissRequest = { showAudioConvertDialog = false },
            audioFormat = convertFormat,
            onConfirm = {
                convertFormat = it
                AUDIO_CONVERSION_FORMAT.updateInt(it)
            },
        )
    }
    if (showVideoQualityDialog) {
        VideoQualityDialog(
            videoQuality = videoQuality,
            onDismissRequest = { showVideoQualityDialog = false },
        ) {
            videoQuality = it
            VIDEO_QUALITY.updateInt(it)
        }
    }
    if (showVideoFormatDialog) {
        VideoFormatDialog(
            videoFormatPreference = videoFormat,
            onDismissRequest = { showVideoFormatDialog = false },
        ) {
            PreferenceUtil.encodeInt(VIDEO_FORMAT, it)
            videoFormat = it
        }
    }
    if (showVideoCodecDialog) {
        SimpleFormatChoiceDialog(
            title = "Video codec",
            selected = videoCodec,
            options = listOf(
                VIDEO_CODEC_AUTO to "Auto / profile default",
                VIDEO_CODEC_H264 to "H.264 / AVC",
                VIDEO_CODEC_VP9 to "VP9",
                VIDEO_CODEC_AV1 to "AV1",
                VIDEO_CODEC_HEVC to "H.265 / HEVC",
            ),
            onDismiss = { showVideoCodecDialog = false },
            onSelect = { videoCodec = it; VIDEO_CODEC.updateInt(it); showVideoCodecDialog = false },
        )
    }
    if (showVideoContainerDialog) {
        SimpleFormatChoiceDialog(
            title = "Video output format",
            selected = videoContainer,
            options = listOf(
                VIDEO_CONTAINER_AUTO to "Auto / source best",
                VIDEO_CONTAINER_MP4 to "MP4",
                VIDEO_CONTAINER_WEBM to "WebM",
                VIDEO_CONTAINER_MKV to "MKV",
            ),
            onDismiss = { showVideoContainerDialog = false },
            onSelect = { videoContainer = it; VIDEO_CONTAINER.updateInt(it); showVideoContainerDialog = false },
        )
    }

    if (showFormatSorterDialog) {
        FormatSortingDialog(
            fields = sortingFields,
            onImport = {
                sortingFields =
                    DownloadUtil.DownloadPreferences.createFromPreferences().toFormatSorter()
            },
            onDismissRequest = { showFormatSorterDialog = false },
            showSwitch = false,
            onConfirm = {
                sortingFields = it
                SORTING_FIELDS.updateString(sortingFields)
            },
        )
    }
    if (showYtdlpFormatProfileDialog) {
        YtdlpFormatProfileDialog(
            currentFields = sortingFields,
            sortingEnabled = isFormatSortingEnabled,
            onDismissRequest = { showYtdlpFormatProfileDialog = false },
            onSelect = { fields ->
                sortingFields = fields
                SORTING_FIELDS.updateString(fields)
                isFormatSortingEnabled = fields.isNotEmpty()
                FORMAT_SORTING.updateBoolean(isFormatSortingEnabled)
                showYtdlpFormatProfileDialog = false
            },
        )
    }
    if (showVideoClipDialog) {
        AlertDialog(
            onDismissRequest = { showVideoClipDialog = false },
            icon = { Icon(Icons.Outlined.ContentCut, null, tint = MaterialTheme.colorScheme.primary) },
            confirmButton = {
                ConfirmButton {
                    isVideoClipEnabled = true
                    VIDEO_CLIP.updateBoolean(true)
                    showVideoClipDialog = false
                }
            },
            dismissButton = { DismissButton { showVideoClipDialog = false } },
            text = { Text(stringResource(id = R.string.clip_video_dialog_msg)) },
            title = {
                Text(
                    stringResource(id = R.string.enable_experimental_feature),
                    textAlign = TextAlign.Center,
                )
            },
        )
    }
    if (showMergeAudioDialog) {
        AlertDialog(
            onDismissRequest = { showMergeAudioDialog = false },
            icon = { Icon(Icons.Outlined.SpatialAudioOff, null, tint = MaterialTheme.colorScheme.primary) },
            confirmButton = {
                ConfirmButton {
                    mergeAudioStream = true
                    MERGE_MULTI_AUDIO_STREAM.updateBoolean(true)
                    showMergeAudioDialog = false
                }
            },
            dismissButton = { DismissButton { showMergeAudioDialog = false } },
            text = { Text(stringResource(id = R.string.merge_audiostream_desc)) },
            title = {
                Text(
                    stringResource(id = R.string.enable_experimental_feature),
                    textAlign = TextAlign.Center,
                )
            },
        )
    }
}


@Composable
private fun SimpleFormatChoiceDialog(
    title: String,
    selected: Int,
    options: List<Pair<Int, String>>,
    onDismiss: () -> Unit,
    onSelect: (Int) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            androidx.compose.foundation.layout.Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                options.forEach { (value, label) ->
                    DialogSingleChoiceItemVariant(
                        title = label,
                        desc = "",
                        selected = selected == value,
                        onClick = { onSelect(value) },
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = { DismissButton { onDismiss() } },
    )
}

private data class YtdlpFormatProfile(
    val title: String,
    val description: String,
    val fields: String,
)

private val ytdlpFormatProfiles =
    listOf(
        YtdlpFormatProfile(
            title = "App default",
            description = "Use KirinDL video quality/format preferences.",
            fields = "",
        ),
        YtdlpFormatProfile(
            title = "Compatibility",
            description = "Prefer H.264 video + AAC audio and compatible containers.",
            fields = "vcodec:h264,acodec:aac,ext",
        ),
        YtdlpFormatProfile(
            title = "Best quality",
            description = "Prioritize resolution, FPS, codecs, channels and bitrate.",
            fields = "res,fps,vcodec,channels,acodec,br",
        ),
        YtdlpFormatProfile(
            title = "High FPS",
            description = "Prefer higher frame rate first, then resolution and codec quality.",
            fields = "fps,res,hdr:12,vcodec,channels,acodec,br",
        ),
        YtdlpFormatProfile(
            title = "HDR quality",
            description = "Prefer HDR/10-bit capable formats, then resolution and FPS.",
            fields = "hdr:12,res,fps,vcodec,channels,acodec,br",
        ),
        YtdlpFormatProfile(
            title = "SDR compatibility",
            description = "Prefer SDR + H.264 + AAC for broad device/player compatibility.",
            fields = "hdr:sdr,vcodec:h264,acodec:aac,ext,res,fps",
        ),
        YtdlpFormatProfile(
            title = "AV1 quality",
            description = "Prefer AV1, then resolution/FPS/bitrate.",
            fields = "vcodec:av01,res,fps,br",
        ),
        YtdlpFormatProfile(
            title = "VP9 quality",
            description = "Prefer VP9 Profile 2, then resolution/FPS/bitrate.",
            fields = "vcodec:vp9.2,res,fps,br",
        ),
        YtdlpFormatProfile(
            title = "Smaller files",
            description = "Prefer smaller size/bitrate/resolution where available.",
            fields = "+size,+br,+res",
        ),
        YtdlpFormatProfile(
            title = "Low bandwidth",
            description = "Prefer lower bitrate, resolution and FPS to reduce transfer size.",
            fields = "+br,+res,+fps",
        ),
    )

private fun ytdlpFormatProfileLabel(enabled: Boolean, fields: String): String {
    if (!enabled || fields.isBlank()) return "App default • quality/format preferences"
    return ytdlpFormatProfiles.firstOrNull { it.fields == fields }?.title
        ?: "Custom • -S $fields"
}

@Composable
private fun YtdlpFormatProfileDialog(
    currentFields: String,
    sortingEnabled: Boolean,
    onDismissRequest: () -> Unit,
    onSelect: (String) -> Unit,
) {
    val selectedFields = if (sortingEnabled) currentFields else ""
    AlertDialog(
        onDismissRequest = onDismissRequest,
        icon = { Icon(Icons.Outlined.Tune, null, tint = MaterialTheme.colorScheme.primary) },
        title = { Text("yt-dlp format profile") },
        text = {
            androidx.compose.foundation.layout.Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
            ) {
                Text(
                    "Quick format presets inspired by YTDLnis. The normal custom -S editor remains available below.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                ytdlpFormatProfiles.forEach { profile ->
                    DialogSingleChoiceItemVariant(
                        title = profile.title,
                        desc =
                            if (profile.fields.isBlank()) {
                                profile.description
                            } else {
                                "${profile.description}\n-S ${profile.fields}"
                            },
                        selected = selectedFields == profile.fields,
                        onClick = { onSelect(profile.fields) },
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = { DismissButton { onDismissRequest() } },
    )
}
