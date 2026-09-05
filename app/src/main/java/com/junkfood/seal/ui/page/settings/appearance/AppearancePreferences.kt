package com.junkfood.seal.ui.page.settings.appearance

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.pager.HorizontalPager
import com.junkfood.seal.R
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Colorize
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TextButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import com.google.android.material.color.DynamicColors
import com.junkfood.seal.download.Task
import com.junkfood.seal.ui.common.LocalDarkTheme
import com.junkfood.seal.ui.common.LocalDynamicColorSwitch
import com.junkfood.seal.ui.common.LocalPaletteStyleIndex
import com.junkfood.seal.ui.common.LocalSeedColor
import com.junkfood.seal.ui.common.Route
import com.junkfood.seal.ui.component.BackButton
import com.junkfood.seal.ui.component.PreferenceItem
import com.junkfood.seal.ui.component.PreferenceSubtitle
import com.junkfood.seal.ui.component.PreferenceSwitch
import com.junkfood.seal.ui.component.PreferenceSwitchWithDivider
import com.junkfood.seal.ui.page.downloadv2.ActionButton
import com.junkfood.seal.ui.page.downloadv2.CardStateIndicator
import com.junkfood.seal.ui.page.downloadv2.VideoCardV2
import com.junkfood.seal.util.DarkThemePreference.Companion.OFF
import com.junkfood.seal.util.DarkThemePreference.Companion.ON
import com.junkfood.seal.util.PreferenceUtil
import com.junkfood.seal.util.GalleryDlThemePreference
import com.junkfood.seal.util.GalleryDlThemeStyle
import com.junkfood.seal.util.STYLE_MONOCHROME
import com.junkfood.seal.util.STYLE_TONAL_SPOT
import com.junkfood.seal.util.paletteStyles
import com.junkfood.seal.util.toDisplayName
import com.junkfood.seal.ui.theme.KirinColorPresets
import com.junkfood.seal.ui.theme.kirinBodyColor
import com.junkfood.seal.ui.theme.kirinButtonColor
import com.junkfood.seal.ui.theme.readableOnColor
import com.kyant.monet.LocalTonalPalettes
import com.kyant.monet.PaletteStyle
import com.kyant.monet.TonalPalettes
import com.kyant.monet.TonalPalettes.Companion.toTonalPalettes
import com.kyant.monet.a1
import com.kyant.monet.a2
import com.kyant.monet.a3
import io.material.hct.Hct
import java.util.Locale
import kotlinx.coroutines.Job

private val ColorList =
    (0 until 12).map { it * 30.0 }.map { Color(Hct.from(it, 48.0, 42.0).toInt()) }

private val DrawableList =
    listOf(R.drawable.sample, R.drawable.sample1, R.drawable.sample2, R.drawable.sample3)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearancePreferences(onNavigateBack: () -> Unit, onNavigateTo: (String) -> Unit) {
    val scrollBehavior =
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
            rememberTopAppBarState(),
            canScroll = { true },
        )

    val index by remember { mutableIntStateOf(DrawableList.indices.random()) }

    val image by remember(index) { mutableIntStateOf(DrawableList[index]) }

    val galleryTheme by GalleryDlThemePreference.style.collectAsState()
    val appSettings by PreferenceUtil.AppSettingsStateFlow.collectAsState()
    var showBodyColorDialog by remember { mutableStateOf(false) }
    var showButtonColorDialog by remember { mutableStateOf(false) }
    var favoriteColorPair by remember { mutableStateOf(PreferenceUtil.getFavoriteColorPair()) }
    val previewDarkTheme = LocalDarkTheme.current.isDarkTheme()

    Scaffold(
        modifier = Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(modifier = Modifier, text = stringResource(id = R.string.look_and_feel))
                },
                navigationIcon = { BackButton(onNavigateBack) },
                scrollBehavior = scrollBehavior,
            )
        },
        content = {
            Column(Modifier.verticalScroll(rememberScrollState()).padding(it)) {
                val downloadState = Task.DownloadState.Running(Job(), "", 0.8f)
                VideoCardV2(
                    modifier = Modifier.padding(18.dp).clearAndSetSemantics {},
                    title = stringResource(R.string.video_title_sample_text),
                    uploader = stringResource(R.string.video_creator_sample_text),
                    thumbnailModel = image,
                    stateIndicator = {
                        CardStateIndicator(modifier = Modifier, downloadState = downloadState)
                    },
                    actionButton = {
                        ActionButton(modifier = Modifier, downloadState = downloadState) {}
                    },
                ) {}
                val pageCount = ColorList.size + 1

                val pagerState =
                    rememberPagerState(
                        initialPage =
                            if (LocalPaletteStyleIndex.current == STYLE_MONOCHROME) pageCount - 1
                            else
                                ColorList.indexOf(Color(LocalSeedColor.current)).run {
                                    if (this == -1) 0 else this
                                }
                    ) {
                        pageCount
                    }

                HorizontalPager(
                    modifier = Modifier.fillMaxWidth().clearAndSetSemantics {},
                    state = pagerState,
                    contentPadding = PaddingValues(horizontal = 12.dp),
                ) { page ->
                    if (page < pageCount - 1) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            ColorButtons(ColorList[page])
                        }
                    } else {
                        // ColorButton for Monochrome theme
                        val isSelected =
                            LocalPaletteStyleIndex.current == STYLE_MONOCHROME &&
                                !LocalDynamicColorSwitch.current
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            ColorButtonImpl(
                                modifier = Modifier,
                                isSelected = { isSelected },
                                tonalPalettes =
                                    Color.Black.toTonalPalettes(PaletteStyle.Monochrome),
                                onClick = {
                                    PreferenceUtil.switchDynamicColor(enabled = false)
                                    PreferenceUtil.modifyThemeSeedColor(
                                        Color.Black.toArgb(),
                                        STYLE_MONOCHROME,
                                    )
                                },
                            )
                        }
                    }
                }

                Row(
                    modifier =
                        Modifier.clearAndSetSemantics {}
                            .align(Alignment.CenterHorizontally)
                            .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    repeat(pageCount) { index ->
                        Box(
                            modifier =
                                Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (pagerState.currentPage == index)
                                            MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.outlineVariant
                                    ),
                        )
                    }
                }
                PreferenceSubtitle(text = "KirinDL Colors")
                PreferenceItem(
                    title = "App body color",
                    description =
                        KirinColorPresets.getOrNull(appSettings.bodyColorPreset)?.title
                            ?: KirinColorPresets.first().title,
                    icon = Icons.Outlined.Palette,
                    onClick = { showBodyColorDialog = true },
                )
                PreferenceItem(
                    title = "Buttons & accent color",
                    description =
                        KirinColorPresets.getOrNull(appSettings.buttonColorPreset)?.title
                            ?: KirinColorPresets.first().title,
                    icon = Icons.Outlined.Colorize,
                    onClick = { showButtonColorDialog = true },
                )
                KirinColorPairPreview(
                    bodyIndex = appSettings.bodyColorPreset,
                    buttonIndex = appSettings.buttonColorPreset,
                    darkTheme = previewDarkTheme,
                )
                PreferenceItem(
                    title = "Save favorite color pair",
                    description = "Remember the current body + button combination",
                    icon = Icons.Outlined.FavoriteBorder,
                    onClick = {
                        PreferenceUtil.saveFavoriteColorPair()
                        favoriteColorPair =
                            appSettings.bodyColorPreset to appSettings.buttonColorPreset
                    },
                )
                PreferenceItem(
                    title = "Apply favorite color pair",
                    description =
                        "${KirinColorPresets.getOrNull(favoriteColorPair.first)?.title ?: "Follow theme"} + " +
                            (KirinColorPresets.getOrNull(favoriteColorPair.second)?.title ?: "Follow theme"),
                    icon = Icons.Outlined.Palette,
                    onClick = { PreferenceUtil.applyFavoriteColorPair() },
                )
                PreferenceItem(
                    title = "Reset KirinDL colors",
                    description = "Return body and buttons to the active Material theme",
                    icon = Icons.Outlined.RestartAlt,
                    onClick = { PreferenceUtil.resetKirinColorPair() },
                )

                if (DynamicColors.isDynamicColorAvailable()) {
                    PreferenceSwitch(
                        title = stringResource(id = R.string.dynamic_color),
                        description = stringResource(id = R.string.dynamic_color_desc),
                        icon = Icons.Outlined.Colorize,
                        isChecked = LocalDynamicColorSwitch.current,
                        onClick = { PreferenceUtil.switchDynamicColor() },
                    )
                }
                val isDarkTheme = LocalDarkTheme.current.isDarkTheme()
                PreferenceSwitchWithDivider(
                    title = stringResource(id = R.string.dark_theme),
                    icon = if (isDarkTheme) Icons.Outlined.DarkMode else Icons.Outlined.LightMode,
                    isChecked = isDarkTheme,
                    description = LocalDarkTheme.current.getDarkThemeDesc(),
                    onChecked = {
                        PreferenceUtil.modifyDarkThemePreference(if (isDarkTheme) OFF else ON)
                    },
                    onClick = { onNavigateTo(Route.DARK_THEME) },
                )
                if (isDarkTheme) {
                    PreferenceSwitch(
                        title = "Gradient Dark",
                        description = "Premium dark mode with vibrant gradients and glassmorphism effects",
                        icon = Icons.Outlined.DarkMode,
                        isChecked = com.junkfood.seal.ui.common.LocalGradientDarkMode.current,
                        onClick = { PreferenceUtil.switchGradientDarkMode() },
                    )
                }
                PreferenceItem(
                    title = stringResource(R.string.language),
                    icon = Icons.Outlined.Language,
                    description = Locale.getDefault().toDisplayName(),
                ) {
                    onNavigateTo(Route.LANGUAGES)
                }
                PreferenceSubtitle(text = "Gallery DL")
                GalleryDlThemeStyle.entries.forEach { style ->
                    PreferenceItem(
                        title = style.title,
                        description = style.description,
                        icon = if (galleryTheme == style) Icons.Outlined.Check else Icons.Outlined.Palette,
                        onClick = { GalleryDlThemePreference.setStyle(style) },
                    )
                }
            }
        },
    )

    if (showBodyColorDialog) {
        KirinColorPresetDialog(
            title = "App body color",
            selected = appSettings.bodyColorPreset,
            bodyMode = true,
            onDismiss = { showBodyColorDialog = false },
            onSelect = {
                PreferenceUtil.modifyBodyColorPreset(it)
                showBodyColorDialog = false
            },
        )
    }
    if (showButtonColorDialog) {
        KirinColorPresetDialog(
            title = "Buttons & accent color",
            selected = appSettings.buttonColorPreset,
            bodyMode = false,
            onDismiss = { showButtonColorDialog = false },
            onSelect = {
                PreferenceUtil.modifyButtonColorPreset(it)
                showButtonColorDialog = false
            },
        )
    }
}

@Composable
private fun KirinColorPairPreview(
    bodyIndex: Int,
    buttonIndex: Int,
    darkTheme: Boolean,
) {
    val body = kirinBodyColor(bodyIndex, darkTheme) ?: MaterialTheme.colorScheme.background
    val button = kirinButtonColor(buttonIndex, darkTheme) ?: MaterialTheme.colorScheme.primary
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        color = body,
        tonalElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(
                    "Live color preview",
                    color = readableOnColor(body),
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    "Body + independent accent",
                    color = readableOnColor(body).copy(alpha = 0.72f),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Surface(shape = RoundedCornerShape(12.dp), color = button) {
                Text(
                    "Button",
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    color = readableOnColor(button),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

@Composable
private fun KirinColorPresetDialog(
    title: String,
    selected: Int,
    bodyMode: Boolean,
    onDismiss: () -> Unit,
    onSelect: (Int) -> Unit,
) {
    val darkTheme = LocalDarkTheme.current.isDarkTheme()
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title)
                Text(
                    text =
                        if (bodyMode) "Choose the main app background color"
                        else "Choose the color used by buttons and accents",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                KirinColorPresets.indices.chunked(2).forEach { indices ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        indices.forEach { index ->
                            KirinColorPresetTile(
                                index = index,
                                selected = selected == index,
                                bodyMode = bodyMode,
                                darkTheme = darkTheme,
                                onSelect = onSelect,
                            )
                        }
                        if (indices.size == 1) {
                            Box(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}

@Composable
private fun RowScope.KirinColorPresetTile(
    index: Int,
    selected: Boolean,
    bodyMode: Boolean,
    darkTheme: Boolean,
    onSelect: (Int) -> Unit,
) {
    val preset = KirinColorPresets[index]
    val previewColor =
        if (index == 0) {
            if (bodyMode) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.primary
        } else if (bodyMode) {
            if (darkTheme) preset.bodyDark else preset.bodyLight
        } else {
            if (darkTheme) preset.buttonDark else preset.buttonLight
        }

    Surface(
        modifier = Modifier.weight(1f),
        shape = RoundedCornerShape(18.dp),
        color =
            if (selected) MaterialTheme.colorScheme.secondaryContainer
            else MaterialTheme.colorScheme.surfaceContainerHigh,
        onClick = { onSelect(index) },
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier =
                    Modifier.fillMaxWidth()
                        .sizeIn(minHeight = 46.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(previewColor),
            ) {
                if (selected) {
                    Surface(
                        modifier = Modifier.align(Alignment.TopEnd).padding(5.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Check,
                            contentDescription = null,
                            modifier = Modifier.padding(4.dp).size(16.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
            Text(
                text = preset.title,
                style = MaterialTheme.typography.labelLarge,
                color =
                    if (selected) MaterialTheme.colorScheme.onSecondaryContainer
                    else MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
fun RowScope.ColorButtons(color: Color) {
    paletteStyles.subList(STYLE_TONAL_SPOT, STYLE_MONOCHROME).forEachIndexed { index, style ->
        ColorButton(color = color, index = index, tonalStyle = style)
    }
}

@Composable
fun RowScope.ColorButton(
    modifier: Modifier = Modifier,
    color: Color = Color.Green,
    index: Int = 0,
    tonalStyle: PaletteStyle = PaletteStyle.TonalSpot,
) {
    val tonalPalettes by remember { mutableStateOf(color.toTonalPalettes(tonalStyle)) }
    val isSelect =
        !LocalDynamicColorSwitch.current &&
            LocalSeedColor.current == color.toArgb() &&
            LocalPaletteStyleIndex.current == index
    ColorButtonImpl(modifier = modifier, tonalPalettes = tonalPalettes, isSelected = { isSelect }) {
        PreferenceUtil.switchDynamicColor(enabled = false)
        PreferenceUtil.modifyThemeSeedColor(color.toArgb(), index)
    }
}

@Composable
fun RowScope.ColorButtonImpl(
    modifier: Modifier = Modifier,
    isSelected: () -> Boolean = { false },
    tonalPalettes: TonalPalettes,
    cardColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    onClick: () -> Unit = {},
) {

    val containerSize by animateDpAsState(targetValue = if (isSelected.invoke()) 28.dp else 0.dp)
    val iconSize by animateDpAsState(targetValue = if (isSelected.invoke()) 16.dp else 0.dp)

    Surface(
        modifier =
            modifier
                .padding(4.dp)
                .sizeIn(maxHeight = 80.dp, maxWidth = 80.dp, minHeight = 64.dp, minWidth = 64.dp)
                .weight(1f, false)
                .aspectRatio(1f),
        shape = RoundedCornerShape(16.dp),
        color = cardColor,
        onClick = onClick,
    ) {
        CompositionLocalProvider(LocalTonalPalettes provides tonalPalettes) {
            val color1 = 80.a1
            val color2 = 90.a2
            val color3 = 60.a3
            Box(Modifier.fillMaxSize()) {
                Box(
                    modifier =
                        modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .drawBehind { drawCircle(color1) }
                            .align(Alignment.Center)
                ) {
                    Surface(
                        color = color2,
                        modifier = Modifier.align(Alignment.BottomStart).size(24.dp),
                    ) {}
                    Surface(
                        color = color3,
                        modifier = Modifier.align(Alignment.BottomEnd).size(24.dp),
                    ) {}
                    Box(
                        modifier =
                            Modifier.align(Alignment.Center)
                                .clip(CircleShape)
                                .size(containerSize)
                                .drawBehind { drawCircle(containerColor) }
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Check,
                            contentDescription = null,
                            modifier = Modifier.size(iconSize).align(Alignment.Center),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
            }
        }
    }
}
