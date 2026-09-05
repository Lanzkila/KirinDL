package com.junkfood.seal.ui.theme

import android.os.Build
import android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextDirection
import com.google.android.material.color.MaterialColors
import com.junkfood.seal.ui.common.LocalFixedColorRoles
import com.junkfood.seal.ui.common.LocalGradientDarkMode
import com.kyant.monet.LocalTonalPalettes
import com.kyant.monet.dynamicColorScheme

fun Color.applyOpacity(enabled: Boolean): Color {
    return if (enabled) this else this.copy(alpha = 0.62f)
}

@Composable
@ReadOnlyComposable
fun Color.harmonizeWith(other: Color) =
    Color(MaterialColors.harmonize(this.toArgb(), other.toArgb()))

@Composable
@ReadOnlyComposable
fun Color.harmonizeWithPrimary(): Color =
    this.harmonizeWith(other = MaterialTheme.colorScheme.primary)

@Composable
fun SealTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    isHighContrastModeEnabled: Boolean = false,
    isGradientDarkEnabled: Boolean = LocalGradientDarkMode.current,
    content: @Composable () -> Unit,
) {
    val view = LocalView.current

    LaunchedEffect(darkTheme) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (darkTheme) {
                view.windowInsetsController?.setSystemBarsAppearance(
                    0,
                    APPEARANCE_LIGHT_STATUS_BARS,
                )
            } else {
                view.windowInsetsController?.setSystemBarsAppearance(
                    APPEARANCE_LIGHT_STATUS_BARS,
                    APPEARANCE_LIGHT_STATUS_BARS,
                )
            }
        }
    }

    val colorScheme =
        dynamicColorScheme(!darkTheme).run {
            when {
                // Gradient Dark mode overrides all other themes
                isGradientDarkEnabled && darkTheme -> copy(
                    primary = GradientDarkColors.GradientPrimaryEnd,
                    onPrimary = GradientDarkColors.OnPrimary,
                    primaryContainer = GradientDarkColors.GradientPrimaryStart,
                    onPrimaryContainer = GradientDarkColors.OnPrimary,
                    secondary = GradientDarkColors.GradientSecondaryEnd,
                    onSecondary = GradientDarkColors.OnPrimary,
                    secondaryContainer = GradientDarkColors.GradientSecondaryStart,
                    onSecondaryContainer = GradientDarkColors.OnPrimary,
                    tertiary = GradientDarkColors.GradientAccentEnd,
                    onTertiary = GradientDarkColors.OnPrimary,
                    tertiaryContainer = GradientDarkColors.GradientAccentStart,
                    onTertiaryContainer = GradientDarkColors.OnPrimary,
                    background = GradientDarkColors.Background,
                    onBackground = GradientDarkColors.OnBackground,
                    surface = GradientDarkColors.Surface,
                    onSurface = GradientDarkColors.OnSurface,
                    surfaceVariant = GradientDarkColors.SurfaceVariant,
                    onSurfaceVariant = GradientDarkColors.OnSurface,
                    surfaceContainer = GradientDarkColors.SurfaceContainer,
                    surfaceContainerLow = GradientDarkColors.SurfaceContainerLow,
                    surfaceContainerHigh = GradientDarkColors.SurfaceContainerHigh,
                    surfaceContainerLowest = GradientDarkColors.Background,
                    surfaceContainerHighest = GradientDarkColors.SurfaceContainerHigh,
                    outline = GradientDarkColors.GlassWhiteBorder,
                    outlineVariant = GradientDarkColors.GlassSurface,
                )
                isHighContrastModeEnabled && darkTheme -> copy(
                    surface = Color.Black,
                    background = Color.Black,
                    surfaceContainerLowest = Color.Black,
                    surfaceContainerLow = surfaceContainerLowest,
                    surfaceContainer = surfaceContainerLow,
                    surfaceContainerHigh = surfaceContainerLow,
                    surfaceContainerHighest = surfaceContainer,
                )
                !darkTheme -> {
                    val base = this
                    val primaryTint = lerp(base.primary, Color.White, 0.78f)
                    copy(
                        background = lerp(base.background, primaryTint, 0.18f),
                        surface = lerp(base.surface, primaryTint, 0.12f),
                        surfaceContainerLowest = lerp(base.surfaceContainerLowest, primaryTint, 0.08f),
                        surfaceContainerLow = lerp(base.surfaceContainerLow, primaryTint, 0.10f),
                        surfaceContainer = lerp(base.surfaceContainer, primaryTint, 0.12f),
                        surfaceContainerHigh = lerp(base.surfaceContainerHigh, primaryTint, 0.16f),
                        surfaceContainerHighest = lerp(base.surfaceContainerHighest, primaryTint, 0.20f),
                        outlineVariant = lerp(base.outlineVariant, base.primary, 0.12f),
                    )
                }
                else -> this
            }
        }

    val textStyle =
        LocalTextStyle.current.copy(
            lineBreak = LineBreak.Paragraph,
            textDirection = TextDirection.Content,
        )

    val tonalPalettes = LocalTonalPalettes.current

    CompositionLocalProvider(
        LocalFixedColorRoles provides FixedColorRoles.fromTonalPalettes(tonalPalettes),
        LocalTextStyle provides textStyle,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = Shapes,
            content = content,
        )
    }
}
