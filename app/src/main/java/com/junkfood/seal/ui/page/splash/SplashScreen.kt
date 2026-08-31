package com.junkfood.seal.ui.page.splash

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.junkfood.seal.R
import com.junkfood.seal.ui.common.LocalGradientDarkMode
import com.junkfood.seal.ui.theme.GradientDarkColors
import com.junkfood.seal.ui.theme.SealTheme
import kotlinx.coroutines.delay

/**
 * KirinDownloader splash screen.
 *
 * Visual identity:
 * - Kirin emblem supplied by the launcher/splash resource pack
 * - dark navy + cyan emphasis
 * - no SealPlus badge or product wording
 * - media + gallery identity instead of video-only wording
 */
@Composable
fun SplashScreen(onSplashFinished: () -> Unit) {
    val isGradientDark = LocalGradientDarkMode.current

    var logoVisible by remember { mutableStateOf(false) }
    var titleVisible by remember { mutableStateOf(false) }
    var subtitleVisible by remember { mutableStateOf(false) }

    val logoScale by
        animateFloatAsState(
            targetValue = if (logoVisible) 1f else 0.78f,
            animationSpec =
                spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow,
                ),
            label = "kirinLogoScale",
        )

    val logoAlpha by
        animateFloatAsState(
            targetValue = if (logoVisible) 1f else 0f,
            animationSpec = tween(650, easing = FastOutSlowInEasing),
            label = "kirinLogoAlpha",
        )

    val titleAlpha by
        animateFloatAsState(
            targetValue = if (titleVisible) 1f else 0f,
            animationSpec = tween(450, easing = FastOutSlowInEasing),
            label = "kirinTitleAlpha",
        )

    val subtitleAlpha by
        animateFloatAsState(
            targetValue = if (subtitleVisible) 1f else 0f,
            animationSpec = tween(450, easing = FastOutSlowInEasing),
            label = "kirinSubtitleAlpha",
        )

    val pulse = rememberInfiniteTransition(label = "kirinPulse")
    val glowAlpha by
        pulse.animateFloat(
            initialValue = 0.18f,
            targetValue = 0.42f,
            animationSpec =
                infiniteRepeatable(
                    animation = tween(1300, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse,
                ),
            label = "kirinGlow",
        )

    LaunchedEffect(Unit) {
        delay(120)
        logoVisible = true
        delay(320)
        titleVisible = true
        delay(220)
        subtitleVisible = true
        delay(1150)
        onSplashFinished()
    }

    val backgroundBrush =
        if (isGradientDark) {
            Brush.verticalGradient(
                listOf(
                    Color(0xFF06131E),
                    Color(0xFF071A2B),
                    Color(0xFF092435),
                )
            )
        } else {
            Brush.verticalGradient(
                listOf(
                    MaterialTheme.colorScheme.background,
                    MaterialTheme.colorScheme.surface,
                    MaterialTheme.colorScheme.surfaceVariant,
                )
            )
        }

    val accent =
        if (isGradientDark) {
            Color(0xFF43D4FF)
        } else {
            MaterialTheme.colorScheme.primary
        }

    Box(
        modifier = Modifier.fillMaxSize().background(backgroundBrush),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 28.dp),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(190.dp),
            ) {
                Box(
                    modifier =
                        Modifier.size(184.dp)
                            .scale(logoScale * 1.04f)
                            .alpha(glowAlpha * logoAlpha)
                            .background(
                                Brush.radialGradient(
                                    listOf(
                                        accent.copy(alpha = 0.55f),
                                        Color.Transparent,
                                    )
                                )
                            )
                )

                Image(
                    painter = painterResource(R.drawable.splash_logo),
                    contentDescription = "KirinDownloader",
                    modifier =
                        Modifier.size(158.dp)
                            .scale(logoScale)
                            .alpha(logoAlpha),
                )
            }

            Spacer(Modifier.height(22.dp))

            Text(
                text = stringResource(R.string.app_name),
                style =
                    MaterialTheme.typography.displaySmall.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.6.sp,
                    ),
                color =
                    if (isGradientDark) {
                        Color(0xFFF4FAFC)
                    } else {
                        MaterialTheme.colorScheme.onBackground
                    },
                modifier = Modifier.alpha(titleAlpha),
            )

            Spacer(Modifier.height(7.dp))

            Text(
                text = "MEDIA  •  GALLERY  •  BATCH",
                style =
                    MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.6.sp,
                    ),
                color = accent,
                modifier = Modifier.alpha(subtitleAlpha),
            )

            Spacer(Modifier.height(12.dp))

            Text(
                text = "Open-source downloader",
                style = MaterialTheme.typography.bodySmall,
                color =
                    if (isGradientDark) {
                        GradientDarkColors.OnSurface.copy(alpha = 0.62f)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                modifier = Modifier.alpha(subtitleAlpha),
            )
        }

        Text(
            text = "KirinDownloader • 2026",
            style = MaterialTheme.typography.labelSmall,
            color =
                if (isGradientDark) {
                    GradientDarkColors.OnSurface.copy(alpha = 0.38f)
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                },
            modifier =
                Modifier.align(Alignment.BottomCenter)
                    .padding(bottom = 38.dp)
                    .alpha(subtitleAlpha),
        )
    }
}

@Preview(name = "Kirin Light", showBackground = true)
@Composable
private fun SplashScreenLightPreview() {
    SealTheme(darkTheme = false) { SplashScreen(onSplashFinished = {}) }
}

@Preview(name = "Kirin Dark", showBackground = true)
@Composable
private fun SplashScreenDarkPreview() {
    SealTheme(darkTheme = true) { SplashScreen(onSplashFinished = {}) }
}
