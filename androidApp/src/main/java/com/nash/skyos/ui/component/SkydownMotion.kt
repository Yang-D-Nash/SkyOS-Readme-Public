package com.nash.skyos.ui.component

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.LinearOutSlowInEasing
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
 * Calm “premium” motion language aligned with iOS `SkydownMotion`: ease-out, ~150–280ms,
 * no bouncy springs for standard UI.
 */
object SkydownMotionTokens {
    const val primaryEnterDurationMillis = 320
    const val primaryExitDurationMillis = 240
    const val overlayEnterDurationMillis = 300
    const val overlayExitDurationMillis = 230
    const val statusEnterDurationMillis = 230
    const val statusExitDurationMillis = 180
    /// Subtle stagger for first-paint reveals (keep short; no “cascade” feel).
    const val staggerStepMillis = 22
    const val premiumAccentTransitionMillis = 310
    const val premiumLabelTransitionMillis = 260
    const val dockSelectionDurationMillis = 260
    const val pressDurationMillis = 200
    const val selectionCrossFadeMillis = 260
    const val contentRevealEnterMillis = 300
    const val contentRevealExitMillis = 190
    /// Nav fade-in lead (works with [LinearOutSlowInEasing]).
    const val navFadeLeadMillis = 28
}

/**
 * Signature settle curve: quick start, long controlled deceleration — not Material default,
 * not elastic.
 */
val SkydownStandardEasing = CubicBezierEasing(0.2f, 0.96f, 0.3f, 1f)

/** Crisp departures for dismissals and route exits. */
val SkydownExitEasing = CubicBezierEasing(0.4f, 0f, 1f, 1f)

/** Default spec for float/color/size motion. */
fun <T> skydownTween(
    durationMillis: Int,
    delayMillis: Int = 0,
): FiniteAnimationSpec<T> = tween(
    durationMillis = durationMillis,
    delayMillis = delayMillis,
    easing = SkydownStandardEasing,
)

/** Spec tuned for overlays and leaves (slightly faster than enter). */
fun <T> skydownExitTween(
    durationMillis: Int,
    delayMillis: Int = 0,
): FiniteAnimationSpec<T> = tween(
    durationMillis = durationMillis,
    delayMillis = delayMillis,
    easing = SkydownExitEasing,
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
