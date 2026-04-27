package com.nash.skyos.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.annotation.StringRes
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nash.skyos.R

@Composable
fun ToastHost(
    message: String?,
    modifier: Modifier = Modifier,
    type: ToastType = ToastType.Info,
) {
    val view = LocalView.current
    val shape = RoundedCornerShape(20.dp)

    LaunchedEffect(message, type) {
        if (!message.isNullOrBlank()) {
            view.performSkydownHaptic(type.hapticKind)
        }
    }

    AnimatedVisibility(
        visible = !message.isNullOrBlank(),
        enter = slideInVertically(
            initialOffsetY = { it / 2 },
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
            initialScale = 0.97f,
            animationSpec = tween(
                durationMillis = SkydownMotionTokens.statusEnterDurationMillis,
                easing = LinearOutSlowInEasing,
            ),
        ),
        exit = slideOutVertically(
            targetOffsetY = { it / 2 },
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
            targetScale = 0.97f,
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
                .padding(16.dp)
                .shadow(
                    elevation = 14.dp,
                    shape = shape,
                    ambientColor = Color.Black.copy(alpha = 0.16f),
                    spotColor = Color.Black.copy(alpha = 0.20f),
                )
                .clip(shape)
                .animateContentSize(
                    animationSpec = tween(
                        durationMillis = SkydownMotionTokens.contentRevealEnterMillis,
                        easing = FastOutSlowInEasing,
                    ),
                )
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.99f))
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            type.accent.copy(alpha = 0.11f),
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.04f),
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.00f),
                        ),
                    ),
                )
                .border(
                    width = 1.dp,
                    color = type.accent.copy(alpha = 0.24f),
                    shape = shape,
                )
                .semantics {
                    liveRegion = LiveRegionMode.Polite
                }
                .padding(horizontal = 12.dp, vertical = 11.dp)
                .height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(11.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(999.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                type.accent.copy(alpha = 0.92f),
                                type.accent.copy(alpha = 0.18f),
                            ),
                        ),
                    ),
            )

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(type.accent.copy(alpha = 0.13f))
                    .border(
                        width = 1.dp,
                        color = type.accent.copy(alpha = 0.20f),
                        shape = RoundedCornerShape(14.dp),
                    ),
            ) {
                Icon(
                    imageVector = type.icon,
                    contentDescription = null,
                    tint = type.accent,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(22.dp),
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    text = stringResource(type.titleRes),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.82f),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = message.orEmpty(),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.92f),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

enum class ToastType(
    val accent: Color,
    val icon: ImageVector,
    @StringRes val titleRes: Int,
    val hapticKind: SkydownHapticKind,
) {
    Success(
        accent = Color(0xFF47C873),
        icon = Icons.Default.CheckCircle,
        titleRes = R.string.toast_title_success,
        hapticKind = SkydownHapticKind.Success,
    ),
    Error(
        accent = Color(0xFFFF6B6B),
        icon = Icons.Default.Error,
        titleRes = R.string.toast_title_error,
        hapticKind = SkydownHapticKind.Error,
    ),
    Warning(
        accent = Color(0xFFFFB347),
        icon = Icons.Default.Warning,
        titleRes = R.string.toast_title_warning,
        hapticKind = SkydownHapticKind.Warning,
    ),
    Info(
        accent = Color(0xFF6BB7FF),
        icon = Icons.Default.Info,
        titleRes = R.string.toast_title_info,
        hapticKind = SkydownHapticKind.Info,
    ),
}
