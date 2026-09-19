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
import com.junkfood.seal.ui.common.LocalBodyColorPreset
import com.junkfood.seal.ui.common.LocalButtonColorPreset
import com.junkfood.seal.ui.common.LocalFixedColorRoles
import com.junkfood.seal.ui.common.LocalGradientDarkMode
import com.junkfood.seal.ui.common.LocalSealPlusFollowKirinTheme
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

    val baseColorScheme =
        dynamicColorScheme(!darkTheme).run {
            when {
                // Gradient Dark mode overrides all other themes.
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

    // Optional bridge for the old Seal/SealPlus Material colors. OFF preserves the original
    // theme exactly. Gradient Dark and High Contrast intentionally keep priority when enabled.
    val bridgeEnabled =
        LocalSealPlusFollowKirinTheme.current &&
            !(isGradientDarkEnabled && darkTheme) &&
            !(isHighContrastModeEnabled && darkTheme)
    val bodyOverride =
        if (bridgeEnabled) kirinBodyColor(LocalBodyColorPreset.current, darkTheme) else null
    val accentOverride =
        if (bridgeEnabled) kirinButtonColor(LocalButtonColorPreset.current, darkTheme) else null

    val colorScheme =
        if (!bridgeEnabled || (bodyOverride == null && accentOverride == null)) {
            baseColorScheme
        } else {
            var bridged = baseColorScheme

            bodyOverride?.let { body ->
                val surfaceBase = lerp(body, baseColorScheme.surface, if (darkTheme) 0.22f else 0.15f)
                val surfaceVariantColor = lerp(body, baseColorScheme.surfaceVariant, 0.38f)
                val surfaceLow = lerp(body, baseColorScheme.surfaceContainerLow, if (darkTheme) 0.28f else 0.20f)
                val surfaceMid = lerp(body, baseColorScheme.surfaceContainer, if (darkTheme) 0.34f else 0.26f)
                val surfaceHigh = lerp(body, baseColorScheme.surfaceContainerHigh, if (darkTheme) 0.40f else 0.32f)
                val surfaceHighest =
                    lerp(body, baseColorScheme.surfaceContainerHighest, if (darkTheme) 0.46f else 0.38f)

                bridged =
                    bridged.copy(
                        background = body,
                        onBackground = readableOnColor(body),
                        surface = surfaceBase,
                        onSurface = readableOnColor(surfaceBase),
                        surfaceVariant = surfaceVariantColor,
                        onSurfaceVariant = readableOnColor(surfaceVariantColor),
                        surfaceContainerLowest = body,
                        surfaceContainerLow = surfaceLow,
                        surfaceContainer = surfaceMid,
                        surfaceContainerHigh = surfaceHigh,
                        surfaceContainerHighest = surfaceHighest,
                    )
            }

            accentOverride?.let { accent ->
                val bodyForContainers = bodyOverride ?: bridged.background
                val primaryContainerColor =
                    lerp(accent, bodyForContainers, if (darkTheme) 0.70f else 0.82f)
                val secondaryColor = lerp(accent, baseColorScheme.secondary, 0.42f)
                val secondaryContainerColor = lerp(secondaryColor, bodyForContainers, 0.78f)
                val tertiaryColor = lerp(accent, baseColorScheme.tertiary, 0.58f)
                val tertiaryContainerColor = lerp(tertiaryColor, bodyForContainers, 0.78f)

                bridged =
                    bridged.copy(
                        primary = accent,
                        onPrimary = readableOnColor(accent),
                        primaryContainer = primaryContainerColor,
                        onPrimaryContainer = readableOnColor(primaryContainerColor),
                        secondary = secondaryColor,
                        onSecondary = readableOnColor(secondaryColor),
                        secondaryContainer = secondaryContainerColor,
                        onSecondaryContainer = readableOnColor(secondaryContainerColor),
                        tertiary = tertiaryColor,
                        onTertiary = readableOnColor(tertiaryColor),
                        tertiaryContainer = tertiaryContainerColor,
                        onTertiaryContainer = readableOnColor(tertiaryContainerColor),
                        outline = lerp(baseColorScheme.outline, accent, 0.24f),
                        outlineVariant = lerp(baseColorScheme.outlineVariant, accent, 0.18f),
                        // Error/warning semantics are deliberately not overridden.
                    )
            }

            bridged
        }

    val textStyle =
        LocalTextStyle.current.copy(
            lineBreak = LineBreak.Paragraph,
            textDirection = TextDirection.Content,
        )

    val tonalPalettes = LocalTonalPalettes.current
    val fixedColorRoles =
        if (bridgeEnabled && accentOverride != null) {
            // Fixed Material roles are used by a number of older Seal/SealPlus components.
            // Bridge them too so those accents do not stay on the old palette.
            FixedColorRoles.fromColorSchemes(colorScheme, colorScheme)
        } else {
            FixedColorRoles.fromTonalPalettes(tonalPalettes)
        }

    CompositionLocalProvider(
        LocalFixedColorRoles provides fixedColorRoles,
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
