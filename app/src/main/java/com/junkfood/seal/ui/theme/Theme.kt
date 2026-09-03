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
import com.junkfood.seal.ui.common.LocalBodyColorPreset
import com.junkfood.seal.ui.common.LocalButtonColorPreset
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
                else -> this
            }
        }

    val customBody = kirinBodyColor(LocalBodyColorPreset.current, darkTheme)
    val customButton = kirinButtonColor(LocalButtonColorPreset.current, darkTheme)
    val colorScheme =
        baseColorScheme.run {
            var result = this
            customBody?.let { body ->
                val onBody = readableOnColor(body)
                val subtle = lerp(body, if (darkTheme) Color.White else Color.Black, 0.06f)
                val elevated = lerp(body, if (darkTheme) Color.White else Color.Black, 0.11f)
                result =
                    result.copy(
                        background = body,
                        onBackground = onBody,
                        surface = body,
                        onSurface = onBody,
                        surfaceVariant = subtle,
                        onSurfaceVariant = onBody.copy(alpha = 0.78f),
                        surfaceContainerLowest = body,
                        surfaceContainerLow = subtle,
                        surfaceContainer = subtle,
                        surfaceContainerHigh = elevated,
                        surfaceContainerHighest = elevated,
                    )
            }
            customButton?.let { button ->
                val onButton = readableOnColor(button)
                val container = lerp(button, if (darkTheme) Color.Black else Color.White, 0.28f)
                result =
                    result.copy(
                        primary = button,
                        onPrimary = onButton,
                        primaryContainer = container,
                        onPrimaryContainer = readableOnColor(container),
                        secondary = button,
                        onSecondary = onButton,
                        tertiary = button,
                        onTertiary = onButton,
                    )
            }
            result
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
