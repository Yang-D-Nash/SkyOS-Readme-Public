package com.skydown.android.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer

object SkydownMotionTokens {
    const val primaryEnterDurationMillis = 460
    const val primaryExitDurationMillis = 300
    const val overlayEnterDurationMillis = 380
    const val overlayExitDurationMillis = 260
    const val statusEnterDurationMillis = 320
    const val statusExitDurationMillis = 220
}

@Composable
fun Modifier.skydownLuminousSweep(
    shape: Shape,
    accent: Color = Color.White,
    alpha: Float = 0.18f,
): Modifier = composed {
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
}
