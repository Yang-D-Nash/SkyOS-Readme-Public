package com.nash.skyos.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private val GoogleBlue = Color(0xFF4285F4)
private val GoogleRed = Color(0xFFEA4335)
private val GoogleYellow = Color(0xFFFBBC05)
private val GoogleGreen = Color(0xFF34A853)
private val GoogleBorder = Color(0xFFDADCE0)
private val GoogleText = Color(0xFF1F1F1F)

@Composable
fun GoogleAuthButton(
    text: String,
    isLoading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val buttonEnabled = enabled && !isLoading
    Surface(
        onClick = onClick,
        enabled = buttonEnabled,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = SkydownUiTokens.buttonStandardMinHeight)
            .skydownPressable(interactionSource = interactionSource, pressedScale = 0.982f),
        shape = RoundedCornerShape(SkydownUiTokens.buttonStandardCornerRadius),
        color = Color.White,
        border = BorderStroke(1.dp, GoogleBorder),
        shadowElevation = if (buttonEnabled) SkydownUiTokens.elevationStateIcon else 0.dp,
        interactionSource = interactionSource,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = SkydownUiTokens.buttonStandardHorizontalPadding,
                    vertical = SkydownUiTokens.buttonStandardVerticalPadding,
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingCompact),
        ) {
            if (isLoading) {
                SkydownPremiumCircularProgress(
                    modifier = Modifier.size(SkydownUiTokens.buttonStandardIconSize),
                    accent = GoogleBlue,
                    strokeWidth = 2.dp,
                )
            } else {
                GoogleMark(modifier = Modifier.size(SkydownUiTokens.buttonStandardIconSize))
            }

            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = GoogleText,
            )
        }
    }
}

@Composable
private fun GoogleMark(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val stroke = size.minDimension * 0.16f
        val arcSize = Size(size.width, size.height)
        val topLeft = Offset.Zero

        drawArc(
            color = GoogleRed,
            startAngle = -40f,
            sweepAngle = 76f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = stroke, cap = StrokeCap.Round),
        )
        drawArc(
            color = GoogleYellow,
            startAngle = 36f,
            sweepAngle = 92f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = stroke, cap = StrokeCap.Round),
        )
        drawArc(
            color = GoogleGreen,
            startAngle = 128f,
            sweepAngle = 96f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = stroke, cap = StrokeCap.Round),
        )
        drawArc(
            color = GoogleBlue,
            startAngle = 224f,
            sweepAngle = 132f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = stroke, cap = StrokeCap.Round),
        )

        val centerY = size.height / 2f
        val barHeight = stroke
        drawLine(
            color = Color.White,
            start = Offset(size.width * 0.54f, centerY),
            end = Offset(size.width * 0.9f, centerY),
            strokeWidth = barHeight * 1.55f,
            cap = StrokeCap.Butt,
        )
        drawLine(
            color = GoogleBlue,
            start = Offset(size.width * 0.54f, centerY),
            end = Offset(size.width * 0.86f, centerY),
            strokeWidth = barHeight,
            cap = StrokeCap.Round,
        )
    }
}
