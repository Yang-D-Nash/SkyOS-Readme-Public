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
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun ToastHost(
    message: String?,
    modifier: Modifier = Modifier,
    type: ToastType = ToastType.Info,
) {
    val view = LocalView.current
    val shape = RoundedCornerShape(18.dp)

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
                    elevation = 10.dp,
                    shape = shape,
                    ambientColor = Color.Black.copy(alpha = 0.14f),
                    spotColor = Color.Black.copy(alpha = 0.16f),
                )
                .clip(shape)
                .animateContentSize(
                    animationSpec = tween(
                        durationMillis = SkydownMotionTokens.contentRevealEnterMillis,
                        easing = FastOutSlowInEasing,
                    ),
                )
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.98f))
                .border(
                    width = 1.dp,
                    color = type.accent.copy(alpha = 0.32f),
                    shape = shape,
                )
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(type.accent.copy(alpha = 0.14f)),
            ) {
                Icon(
                    imageVector = type.icon,
                    contentDescription = null,
                    tint = type.accent,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(21.dp),
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    text = type.title,
                    color = type.accent,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = message.orEmpty(),
                    color = MaterialTheme.colorScheme.onSurface,
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
    val title: String,
    val hapticKind: SkydownHapticKind,
) {
    Success(
        accent = Color(0xFF47C873),
        icon = Icons.Default.CheckCircle,
        title = "Erfolgreich",
        hapticKind = SkydownHapticKind.Success,
    ),
    Error(
        accent = Color(0xFFFF6B6B),
        icon = Icons.Default.Error,
        title = "Aktion fehlgeschlagen",
        hapticKind = SkydownHapticKind.Error,
    ),
    Warning(
        accent = Color(0xFFFFB347),
        icon = Icons.Default.Warning,
        title = "Kurz pruefen",
        hapticKind = SkydownHapticKind.Warning,
    ),
    Info(
        accent = Color(0xFF6BB7FF),
        icon = Icons.Default.Info,
        title = "Info",
        hapticKind = SkydownHapticKind.Info,
    ),
}
