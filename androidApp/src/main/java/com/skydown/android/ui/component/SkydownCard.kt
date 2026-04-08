package com.skydown.android.ui.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.composed
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp

@Composable
fun SkydownCard(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(SkydownUiTokens.cardPadding),
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(SkydownUiTokens.cardCornerRadius)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp,
                shape = shape,
            )
            .clip(shape)
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.07f),
                        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.04f),
                    ),
                ),
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.10f),
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.12f),
                    ),
                ),
                shape = shape,
            )
            .padding(contentPadding),
        content = content,
    )
}

@Stable
fun Modifier.skydownPressable(
    interactionSource: MutableInteractionSource,
    pressedScale: Float = 0.985f,
): Modifier = composed {
    val isPressed by interactionSource.collectIsPressedAsState()
    val view = LocalView.current
    var emittedPressHaptic by remember(interactionSource) { mutableStateOf(false) }
    val animatedScale by animateFloatAsState(
        targetValue = if (isPressed) pressedScale else 1f,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 560f),
        label = "skydownPressScale",
    )
    val animatedAlpha by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = 0.82f, stiffness = 620f),
        label = "skydownPressAlpha",
    )
    val animatedTranslationY by animateFloatAsState(
        targetValue = if (isPressed) 1.4f else 0f,
        animationSpec = spring(dampingRatio = 0.82f, stiffness = 620f),
        label = "skydownPressTranslationY",
    )

    LaunchedEffect(isPressed) {
        if (isPressed && !emittedPressHaptic) {
            emittedPressHaptic = true
            view.performSkydownHaptic(SkydownHapticKind.Press)
        } else if (!isPressed) {
            emittedPressHaptic = false
        }
    }

    graphicsLayer {
        scaleX = animatedScale
        scaleY = animatedScale
        alpha = animatedAlpha
        translationY = animatedTranslationY
    }
}

@Stable
fun Modifier.skydownSheen(
    accent: Color = Color.White,
    alpha: Float = 0.16f,
): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "skydownSheen")
    val travel by transition.animateFloat(
        initialValue = -0.45f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3900, easing = LinearEasing),
        ),
        label = "skydownSheenTravel",
    )

    drawWithContent {
        drawContent()

        val sheenWidth = size.width * 0.32f
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(
                    Color.Transparent,
                    Color.White.copy(alpha = alpha),
                    accent.copy(alpha = alpha * 0.72f),
                    Color.Transparent,
                ),
                start = Offset.Zero,
                end = Offset(sheenWidth, size.height),
            ),
            topLeft = Offset(
                x = size.width * travel - sheenWidth,
                y = -size.height * 0.2f,
            ),
            size = Size(sheenWidth, size.height * 1.4f),
            blendMode = BlendMode.Screen,
        )
    }
}
