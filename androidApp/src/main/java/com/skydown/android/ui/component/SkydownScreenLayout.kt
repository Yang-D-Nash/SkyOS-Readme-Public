package com.skydown.android.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

object SkydownUiTokens {
    val screenHorizontalPadding = 14.dp
    val screenTopPadding = 8.dp
    val screenBottomPadding = 24.dp
    val cardPadding = 16.dp
    val heroPadding = 18.dp
    val cardCornerRadius = 22.dp
    val heroCornerRadius = 24.dp
}

fun skydownContentPadding(innerPadding: PaddingValues): PaddingValues = PaddingValues(
    start = SkydownUiTokens.screenHorizontalPadding,
    top = innerPadding.calculateTopPadding() + SkydownUiTokens.screenTopPadding,
    end = SkydownUiTokens.screenHorizontalPadding,
    bottom = innerPadding.calculateBottomPadding() + SkydownUiTokens.screenBottomPadding,
)

@Composable
fun skydownScreenBrush(
    primaryColor: Color = MaterialTheme.colorScheme.primary,
    secondaryColor: Color = MaterialTheme.colorScheme.secondary,
    primaryAlpha: Float = 0.10f,
    secondaryAlpha: Float = 0.08f,
): Brush = Brush.verticalGradient(
    colors = listOf(
        MaterialTheme.colorScheme.background,
        primaryColor.copy(alpha = primaryAlpha),
        secondaryColor.copy(alpha = secondaryAlpha),
        MaterialTheme.colorScheme.tertiary.copy(alpha = secondaryAlpha * 0.8f),
        MaterialTheme.colorScheme.background,
    ),
)

@Composable
fun skydownTopBarColors(): TopAppBarColors = TopAppBarDefaults.topAppBarColors(
    containerColor = Color.Transparent,
    scrolledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.84f),
    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
    titleContentColor = MaterialTheme.colorScheme.onSurface,
    actionIconContentColor = MaterialTheme.colorScheme.onSurface,
)

@Composable
fun SkydownTopBarTitle(
    title: String,
    subtitle: String? = null,
) {
    if (subtitle.isNullOrBlank()) {
        Text(
            text = title,
            fontWeight = FontWeight.Bold,
        )
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
