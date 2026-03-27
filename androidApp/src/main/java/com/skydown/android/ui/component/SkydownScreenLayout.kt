package com.skydown.android.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

object SkydownUiTokens {
    val screenHorizontalPadding = 16.dp
    val screenTopPadding = 6.dp
    val screenBottomPadding = 28.dp
    val cardPadding = 18.dp
    val heroPadding = 20.dp
    val cardCornerRadius = 24.dp
    val heroCornerRadius = 26.dp
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
    primaryAlpha: Float = 0.07f,
    secondaryAlpha: Float = 0.05f,
): Brush = Brush.verticalGradient(
    colors = listOf(
        MaterialTheme.colorScheme.background,
        primaryColor.copy(alpha = primaryAlpha),
        secondaryColor.copy(alpha = secondaryAlpha),
        MaterialTheme.colorScheme.background,
    ),
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
