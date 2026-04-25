package com.nash.skyos.ui.component

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import com.nash.skyos.ui.theme.skydownAccentHighlight
import com.nash.skyos.ui.theme.skydownCardBackground
import com.nash.skyos.ui.theme.skydownCinematicShadow
import com.nash.skyos.ui.theme.skydownIsDarkPalette
import com.nash.skyos.ui.theme.skydownLuminanceLift
import com.nash.skyos.ui.theme.skydownSecondaryBackground

@Composable
fun SkydownCard(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(SkydownUiTokens.cardPadding),
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .skydownPanelSurface(
                cornerRadius = SkydownUiTokens.cardCornerRadius,
                shadowRadius = 10.dp,
                shadowYOffset = 5.dp,
            )
            .padding(contentPadding),
        content = content,
    )
}

@Composable
fun Modifier.skydownPanelSurface(
    accent: Color? = null,
    cornerRadius: androidx.compose.ui.unit.Dp = SkydownUiTokens.cardCornerRadius,
    shadowRadius: androidx.compose.ui.unit.Dp = 14.dp,
    shadowYOffset: androidx.compose.ui.unit.Dp = 8.dp,
): Modifier = composed {
    val colorScheme = MaterialTheme.colorScheme
    val isDarkPalette = colorScheme.skydownIsDarkPalette()
    val resolvedAccent = accent ?: colorScheme.primary
    val shape = RoundedCornerShape(cornerRadius)

    shadow(
        elevation = 5.dp,
        shape = shape,
        ambientColor = resolvedAccent.copy(alpha = if (isDarkPalette) 0.020f else 0.016f),
        spotColor = resolvedAccent.copy(alpha = if (isDarkPalette) 0.020f else 0.016f),
    )
        .shadow(
            elevation = shadowRadius,
            shape = shape,
            ambientColor = colorScheme.skydownCinematicShadow().copy(alpha = if (isDarkPalette) 0.12f else 0.065f),
            spotColor = colorScheme.skydownCinematicShadow().copy(alpha = if (isDarkPalette) 0.12f else 0.065f),
        )
        .clip(shape)
        .drawWithContent {
            val baseBrush = Brush.linearGradient(
                colors = listOf(
                    colorScheme.skydownLuminanceLift().copy(alpha = if (isDarkPalette) 0.08f else 0.16f),
                    colorScheme.skydownCardBackground().copy(alpha = if (isDarkPalette) 0.965f else 0.990f),
                    colorScheme.skydownSecondaryBackground().copy(alpha = if (isDarkPalette) 0.25f else 0.38f),
                    colorScheme.skydownAccentHighlight().copy(alpha = if (isDarkPalette) 0.016f else 0.020f),
                    resolvedAccent.copy(alpha = if (isDarkPalette) 0.014f else 0.012f),
                ),
                start = Offset(size.width * 0.06f, 0f),
                end = Offset(size.width, size.height),
            )
            val upperBloom = Brush.radialGradient(
                colors = listOf(
                    colorScheme.skydownLuminanceLift().copy(alpha = if (isDarkPalette) 0.05f else 0.12f),
                    resolvedAccent.copy(alpha = if (isDarkPalette) 0.022f else 0.028f),
                    Color.Transparent,
                ),
                center = Offset(size.width * 0.16f, size.height * 0.12f),
                radius = size.width * 0.92f,
            )
            val lowerMist = Brush.radialGradient(
                colors = listOf(
                    colorScheme.skydownAccentHighlight().copy(alpha = if (isDarkPalette) 0.034f else 0.048f),
                    resolvedAccent.copy(alpha = if (isDarkPalette) 0.018f else 0.026f),
                    Color.Transparent,
                ),
                center = Offset(size.width * 0.88f, size.height * 0.86f),
                radius = size.width * 1.04f,
            )

            drawRect(baseBrush)
            drawRect(upperBloom, blendMode = BlendMode.Screen)
            drawRect(lowerMist)
            drawContent()
        }
        .border(
            width = 0.9.dp,
            brush = Brush.linearGradient(
                colors = listOf(
                    colorScheme.skydownLuminanceLift().copy(alpha = if (isDarkPalette) 0.10f else 0.18f),
                    colorScheme.skydownAccentHighlight().copy(alpha = if (isDarkPalette) 0.030f else 0.054f),
                    resolvedAccent.copy(alpha = if (isDarkPalette) 0.050f else 0.042f),
                    colorScheme.skydownCinematicShadow().copy(alpha = if (isDarkPalette) 0.014f else 0.020f),
                ),
            ),
            shape = shape,
        )
        .skydownSheen(
            accent = resolvedAccent,
            alpha = if (isDarkPalette) 0.010f else 0.009f,
        )
}

@Composable
fun Modifier.skydownCapsuleSurface(
    accent: Color? = null,
    shape: Shape = RoundedCornerShape(999.dp),
): Modifier = composed {
    val colorScheme = MaterialTheme.colorScheme
    val isDarkPalette = colorScheme.skydownIsDarkPalette()
    val resolvedAccent = accent ?: colorScheme.primary

    shadow(
        elevation = 5.dp,
        shape = shape,
        ambientColor = Color.Black.copy(alpha = if (isDarkPalette) 0.13f else 0.075f),
        spotColor = Color.Black.copy(alpha = if (isDarkPalette) 0.13f else 0.075f),
    )
        .clip(shape)
        .drawWithContent {
            val fillGradient = Brush.linearGradient(
                colors = listOf(
                    colorScheme.skydownLuminanceLift().copy(alpha = if (isDarkPalette) 0.14f else 0.32f),
                    colorScheme.skydownCardBackground().copy(alpha = if (isDarkPalette) 0.72f else 0.88f),
                    resolvedAccent.copy(alpha = if (isDarkPalette) 0.038f else 0.034f),
                    colorScheme.skydownAccentHighlight().copy(alpha = if (isDarkPalette) 0.028f else 0.024f),
                ),
                start = Offset.Zero,
                end = Offset(size.width, size.height),
            )
            val upperGlow = Brush.radialGradient(
                colors = listOf(
                    colorScheme.skydownLuminanceLift().copy(alpha = if (isDarkPalette) 0.06f else 0.14f),
                    Color.Transparent,
                ),
                center = Offset(size.width * 0.10f, size.height * 0.12f),
                radius = size.width * 0.84f,
            )
            val lowerGlow = Brush.radialGradient(
                colors = listOf(
                    resolvedAccent.copy(alpha = if (isDarkPalette) 0.04f else 0.05f),
                    Color.Transparent,
                ),
                center = Offset(size.width * 0.92f, size.height * 0.86f),
                radius = size.width * 0.90f,
            )

            drawRect(fillGradient)
            drawRect(upperGlow, blendMode = BlendMode.Screen)
            drawRect(lowerGlow)
            drawContent()
        }
        .border(
            width = 0.9.dp,
            brush = Brush.linearGradient(
                colors = listOf(
                    colorScheme.skydownLuminanceLift().copy(alpha = if (isDarkPalette) 0.12f else 0.22f),
                    resolvedAccent.copy(alpha = 0.10f),
                    resolvedAccent.copy(alpha = 0.055f),
                ),
            ),
            shape = shape,
        )
}

@Stable
fun Modifier.skydownPressable(
    interactionSource: MutableInteractionSource,
    pressedScale: Float = 0.98f,
): Modifier = composed {
    val isPressed by interactionSource.collectIsPressedAsState()
    val view = LocalView.current
    var emittedPressHaptic by remember(interactionSource) { mutableStateOf(false) }
    val pressSpec = tween<Float>(
        durationMillis = SkydownMotionTokens.pressDurationMillis,
        easing = FastOutSlowInEasing,
    )
    val animatedScale by animateFloatAsState(
        targetValue = if (isPressed) pressedScale else 1f,
        animationSpec = pressSpec,
        label = "skydownPressScale",
    )
    val animatedAlpha by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = pressSpec,
        label = "skydownPressAlpha",
    )
    val animatedTranslationY by animateFloatAsState(
        targetValue = if (isPressed) 0.5f else 0f,
        animationSpec = pressSpec,
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
): Modifier =
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
                x = size.width * 0.18f,
                y = -size.height * 0.2f,
            ),
            size = Size(sheenWidth, size.height * 1.4f),
            blendMode = BlendMode.Screen,
        )
    }
