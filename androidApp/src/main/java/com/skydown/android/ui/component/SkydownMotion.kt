package com.skydown.android.ui.component

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

object SkydownMotionTokens {
    const val primaryEnterDurationMillis = 340
    const val primaryExitDurationMillis = 240
    const val overlayEnterDurationMillis = 300
    const val overlayExitDurationMillis = 220
    const val statusEnterDurationMillis = 260
    const val statusExitDurationMillis = 180
    const val staggerStepMillis = 12
    const val premiumAccentTransitionMillis = 380
    const val premiumLabelTransitionMillis = 260
    const val premiumPressDampingRatio = 0.86f
    const val premiumPressStiffness = 700f
    const val premiumPressAlphaDampingRatio = 0.90f
    const val premiumPressAlphaStiffness = 720f
    const val premiumPressLiftDampingRatio = 0.92f
    const val premiumPressLiftStiffness = 740f
    const val premiumDockIconDampingRatio = 0.88f
    const val premiumDockIconStiffness = 560f
    const val premiumDockLiftDampingRatio = 0.90f
    const val premiumDockLiftStiffness = 610f
}

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
