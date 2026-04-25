package com.nash.skyos.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nash.skyos.R

@Composable
fun ConnectivityStatusBanner(
    visible: Boolean,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(14.dp)

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(
            initialOffsetY = { -it / 2 },
            animationSpec = tween(
                durationMillis = SkydownMotionTokens.statusEnterDurationMillis,
                easing = LinearOutSlowInEasing,
            ),
        ) + fadeIn(
            animationSpec = tween(
                durationMillis = SkydownMotionTokens.statusEnterDurationMillis,
                easing = LinearOutSlowInEasing,
            ),
        ) + scaleIn(
            initialScale = 0.985f,
            animationSpec = tween(
                durationMillis = SkydownMotionTokens.statusEnterDurationMillis,
                easing = LinearOutSlowInEasing,
            ),
        ),
        exit = slideOutVertically(
            targetOffsetY = { -it / 2 },
            animationSpec = tween(
                durationMillis = SkydownMotionTokens.statusExitDurationMillis,
                easing = FastOutSlowInEasing,
            ),
        ) + fadeOut(
            animationSpec = tween(
                durationMillis = SkydownMotionTokens.statusExitDurationMillis,
                easing = FastOutSlowInEasing,
            ),
        ) + scaleOut(
            targetScale = 0.985f,
            animationSpec = tween(
                durationMillis = SkydownMotionTokens.statusExitDurationMillis,
                easing = FastOutSlowInEasing,
            ),
        ),
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF5A340E),
                            Color(0xFF102A41),
                        ),
                    ),
                    shape = shape,
                )
                .border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.16f),
                    shape = shape,
                )
                .skydownLuminousSweep(
                    shape = shape,
                    accent = Color(0xFFFFC56A),
                    alpha = 0.16f,
                )
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Default.WifiOff,
                contentDescription = null,
                tint = Color.White,
            )
            Column(
                modifier = Modifier
                    .padding(start = 10.dp)
                    .weight(1f),
            ) {
                Text(
                    text = stringResource(R.string.offline_banner_title),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
                Text(
                    text = stringResource(R.string.offline_banner_message),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.88f),
                )
            }
        }
    }
}
