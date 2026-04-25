package com.nash.skyos.ui.component

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer

/**
 * Calm “premium” motion language aligned with iOS `SkydownMotion`: ease-out, ~150–250ms,
 * no bouncy springs for standard UI.
 */
object SkydownMotionTokens {
    const val primaryEnterDurationMillis = 250
    const val primaryExitDurationMillis = 200
    const val overlayEnterDurationMillis = 240
    const val overlayExitDurationMillis = 200
    const val statusEnterDurationMillis = 220
    const val statusExitDurationMillis = 170
    /// Subtle stagger for first-paint reveals (keep short; no “cascade” feel).
    const val staggerStepMillis = 8
    const val premiumAccentTransitionMillis = 240
    const val premiumLabelTransitionMillis = 200
    const val dockSelectionDurationMillis = 200
    const val pressDurationMillis = 180
    const val selectionCrossFadeMillis = 200
    const val contentRevealEnterMillis = 220
}

/** Default ease-out spec for float/color motion (matches iOS ease-out curves). */
fun <T> skydownTween(
    durationMillis: Int,
    delayMillis: Int = 0,
): AnimationSpec<T> = tween(
    durationMillis = durationMillis,
    delayMillis = delayMillis,
    easing = FastOutSlowInEasing,
)

@Composable
fun Modifier.skydownLuminousSweep(
    shape: Shape,
    accent: Color = Color.White,
    alpha: Float = 0.18f,
): Modifier =
    graphicsLayer {
        clip = true
        this.shape = shape
    }.drawWithContent {
        drawContent()

        val sweepWidth = size.width * 0.28f
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(
                    Color.Transparent,
                    Color.White.copy(alpha = alpha * 0.92f),
                    accent.copy(alpha = alpha),
                    Color.Transparent,
                ),
                start = Offset.Zero,
                end = Offset(sweepWidth, size.height),
            ),
            topLeft = Offset(
                x = size.width * 0.22f,
                y = -size.height * 0.18f,
            ),
            size = Size(sweepWidth, size.height * 1.5f),
            blendMode = BlendMode.Screen,
        )
    }
