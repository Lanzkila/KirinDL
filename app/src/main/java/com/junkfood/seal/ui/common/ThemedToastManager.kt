package com.junkfood.seal.ui.common

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.junkfood.seal.App
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.util.concurrent.atomic.AtomicInteger

/**
 * Centralized themed toast manager for KirinDownloader.
 *
 * When a [ThemedToastHost] composable is active in the composition tree,
 * toasts are rendered with the custom themed UI. When no host is active
 * (e.g. from a background service or BroadcastReceiver), falls back to
 * a standard Android Toast so the message is never silently lost.
 */
object ThemedToastManager {

    private val _toastFlow = MutableSharedFlow<String>(extraBufferCapacity = 5)
    val toastFlow = _toastFlow.asSharedFlow()

    /** Number of active [ThemedToastHost] composables currently collecting. */
    private val activeHostCount = AtomicInteger(0)

    fun registerHost() { activeHostCount.incrementAndGet() }
    fun unregisterHost() { activeHostCount.decrementAndGet() }

    fun showToast(message: String) {
        if (activeHostCount.get() > 0) {
            _toastFlow.tryEmit(message)
        } else {
            // Fallback: show native Toast on the main thread
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(App.context, message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun showToast(context: Context, @StringRes stringId: Int) {
        showToast(context.getString(stringId))
    }
}

private const val TOAST_DISPLAY_DURATION = 2500L
private const val TOAST_ANIMATION_DURATION = 300

/**
 * Composable host that displays themed custom toasts.
 * Place this at the root of your composition tree (inside your theme).
 */
@Composable
fun ThemedToastHost(modifier: Modifier = Modifier) {
    var currentMessage by remember { mutableStateOf<String?>(null) }
    var isVisible by remember { mutableStateOf(false) }

    // Register this host so ThemedToastManager knows a UI collector is active
    DisposableEffect(Unit) {
        ThemedToastManager.registerHost()
        onDispose { ThemedToastManager.unregisterHost() }
    }

    LaunchedEffect(Unit) {
        ThemedToastManager.toastFlow.collect { message ->
            // If a toast is already showing, hide it first
            if (isVisible) {
                isVisible = false
                delay(TOAST_ANIMATION_DURATION.toLong())
            }
            currentMessage = message
            isVisible = true
            delay(TOAST_DISPLAY_DURATION)
            isVisible = false
            delay(TOAST_ANIMATION_DURATION.toLong())
            currentMessage = null
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .systemBarsPadding(),
        contentAlignment = Alignment.BottomCenter
    ) {
        AnimatedVisibility(
            visible = isVisible,
            enter = slideInVertically(
                animationSpec = tween(TOAST_ANIMATION_DURATION, easing = EaseOutCubic)
            ) { it } + fadeIn(animationSpec = tween(TOAST_ANIMATION_DURATION)),
            exit = slideOutVertically(
                animationSpec = tween(TOAST_ANIMATION_DURATION, easing = EaseInCubic)
            ) { it } + fadeOut(animationSpec = tween(TOAST_ANIMATION_DURATION))
        ) {
            currentMessage?.let { message ->
                ThemedToastContent(message = message)
            }
        }
    }
}

@Composable
private fun ThemedToastContent(message: String) {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val tertiary = MaterialTheme.colorScheme.tertiary
    val surfaceContainer = MaterialTheme.colorScheme.surfaceContainerHigh
    val onSurface = MaterialTheme.colorScheme.onSurface

    val gradientBrush = Brush.horizontalGradient(
        colors = listOf(primary, secondary, tertiary)
    )

    val shape = RoundedCornerShape(20.dp)

    Box(
        modifier = Modifier
            .padding(horizontal = 24.dp, vertical = 32.dp)
            .shadow(
                elevation = 8.dp,
                shape = shape,
                ambientColor = primary.copy(alpha = 0.3f),
                spotColor = primary.copy(alpha = 0.3f)
            )
            // Gradient border layer
            .clip(shape)
            .background(gradientBrush)
            .padding(1.5.dp)
            // Inner background
            .clip(RoundedCornerShape(19.dp))
            .background(surfaceContainer)
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 14.dp),
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Medium
            ),
            color = onSurface
        )
    }
}
