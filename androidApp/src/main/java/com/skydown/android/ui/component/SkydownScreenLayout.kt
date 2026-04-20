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
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

object SkydownUiTokens {
    val screenHorizontalPadding = 16.dp
    val screenTopPadding = 10.dp
    val screenBottomPadding = 14.dp
    val cardPadding = 14.dp
    val heroPadding = 18.dp
    val cardCornerRadius = 24.dp
    val heroCornerRadius = 28.dp
    val buttonCornerRadius = 18.dp
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
    primaryAlpha: Float = 0.040f,
    secondaryAlpha: Float = 0.028f,
): Brush {
    val background = MaterialTheme.colorScheme.background
    val isDarkPalette = background.luminance() < 0.36f

    val topSky = if (isDarkPalette) Color(0xFF070B10) else Color(0xFFE8EDF2)
    val upperSky = if (isDarkPalette) Color(0xFF0D141D) else Color(0xFFD9E1E9)
    val horizonBase = if (isDarkPalette) Color(0xFF1A2838) else Color(0xFFC7D2DE)
    val groundDepth = if (isDarkPalette) Color(0xFF08111A) else Color(0xFFA8B7C6)
    val primaryMist = lerp(primaryColor, horizonBase, if (isDarkPalette) 0.86f else 0.86f)
    val secondaryMist = lerp(secondaryColor, horizonBase, if (isDarkPalette) 0.88f else 0.88f)

    return Brush.verticalGradient(
        colors = listOf(
            topSky,
            upperSky,
            horizonBase,
            primaryMist.copy(alpha = primaryAlpha + if (isDarkPalette) 0.014f else 0.006f),
            secondaryMist.copy(alpha = secondaryAlpha + if (isDarkPalette) 0.010f else 0.004f),
            groundDepth.copy(alpha = if (isDarkPalette) 0.24f else 0.16f),
            Color.Black.copy(alpha = if (isDarkPalette) 0.18f else 0.11f),
            background,
        ),
    )
}

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
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Black,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        if (!subtitle.isNullOrBlank()) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.70f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
