package com.skydown.android.ui.component

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun ToastHost(
    message: String?,
    modifier: Modifier = Modifier,
    type: ToastType = ToastType.Info,
) {
    val view = LocalView.current
    val shape = RoundedCornerShape(22.dp)

    LaunchedEffect(message, type) {
        if (!message.isNullOrBlank()) {
            view.performSkydownHaptic(type.hapticKind)
        }
    }

    AnimatedVisibility(
        visible = !message.isNullOrBlank(),
        enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn() + scaleIn(initialScale = 0.96f),
        exit = slideOutVertically(targetOffsetY = { it / 2 }) + fadeOut() + scaleOut(targetScale = 0.96f),
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .shadow(
                    elevation = 18.dp,
                    shape = shape,
                    ambientColor = type.accent.copy(alpha = 0.22f),
                    spotColor = type.accent.copy(alpha = 0.28f),
                )
                .clip(shape)
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            type.base,
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                            type.accent.copy(alpha = 0.22f),
                            Color.Black.copy(alpha = 0.32f),
                        ),
                    ),
                )
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            type.accent.copy(alpha = 0.42f),
                            Color.White.copy(alpha = 0.10f),
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.18f),
                        ),
                    ),
                    shape = shape,
                )
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier.fillMaxWidth(),
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(92.dp)
                        .background(type.accent.copy(alpha = 0.16f), shape = RoundedCornerShape(999.dp))
                        .clip(RoundedCornerShape(999.dp)),
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(999.dp))
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(type.accent, type.accent.copy(alpha = 0.18f)),
                                ),
                            ),
                    )

                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(type.accent, type.base.copy(alpha = 0.92f)),
                                ),
                            ),
                    ) {
                        Icon(
                            imageVector = type.icon,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.align(Alignment.Center),
                        )
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = type.title,
                            color = Color.White.copy(alpha = 0.78f),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = message.orEmpty(),
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }

            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(type.accent.copy(alpha = 0.92f), Color.White.copy(alpha = 0.52f)),
                        ),
                    ),
            )
        }
    }
}

enum class ToastType(
    val accent: Color,
    val base: Color,
    val icon: ImageVector,
    val title: String,
    val hapticKind: SkydownHapticKind,
) {
    Success(
        accent = Color(0xFF47C873),
        base = Color(0xFF112E1F),
        icon = Icons.Default.CheckCircle,
        title = "Erfolgreich",
        hapticKind = SkydownHapticKind.Success,
    ),
    Error(
        accent = Color(0xFFFF6B6B),
        base = Color(0xFF381217),
        icon = Icons.Default.Error,
        title = "Aktion fehlgeschlagen",
        hapticKind = SkydownHapticKind.Error,
    ),
    Warning(
        accent = Color(0xFFFFB347),
        base = Color(0xFF38220A),
        icon = Icons.Default.Warning,
        title = "Kurz pruefen",
        hapticKind = SkydownHapticKind.Warning,
    ),
    Info(
        accent = Color(0xFF6BB7FF),
        base = Color(0xFF102A41),
        icon = Icons.Default.Info,
        title = "Info",
        hapticKind = SkydownHapticKind.Info,
    ),
}
