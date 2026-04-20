package com.skydown.android.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

    val topLift = if (isDarkPalette) Color(0xFF1E2D3A) else Color(0xFFF8FBFD)
    val topSky = if (isDarkPalette) Color(0xFF15202C) else Color(0xFFEEF3F8)
    val upperSky = if (isDarkPalette) Color(0xFF1D2B39) else Color(0xFFE2EAF2)
    val horizonBase = if (isDarkPalette) Color(0xFF314355) else Color(0xFFD4E0EA)
    val groundDepth = if (isDarkPalette) Color(0xFF182633) else Color(0xFFB4C3D2)
    val primaryMist = lerp(primaryColor, horizonBase, 0.84f)
    val secondaryMist = lerp(secondaryColor, horizonBase, 0.87f)
    val vignette = Color.Black.copy(alpha = if (isDarkPalette) 0.10f else 0.10f)

    return Brush.verticalGradient(
        colors = listOf(
            topLift,
            topSky,
            upperSky,
            horizonBase,
            Color.White.copy(alpha = if (isDarkPalette) 0.050f else 0.30f),
            primaryMist.copy(alpha = primaryAlpha + if (isDarkPalette) 0.014f else 0.010f),
            secondaryMist.copy(alpha = secondaryAlpha + if (isDarkPalette) 0.012f else 0.008f),
            groundDepth.copy(alpha = if (isDarkPalette) 0.16f else 0.18f),
            vignette,
            background,
        ),
    )
}

@Composable
fun skydownTopBarColors(): TopAppBarColors = TopAppBarDefaults.topAppBarColors(
    containerColor = Color.Transparent,
    scrolledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.62f),
    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
    titleContentColor = MaterialTheme.colorScheme.onSurface,
    actionIconContentColor = MaterialTheme.colorScheme.onSurface,
)

@Composable
fun SkydownTopBarTitle(
    title: String,
    subtitle: String? = null,
    accent: Color = MaterialTheme.colorScheme.primary,
) {
    val systemLabel = if (title.equals("SkyOs", ignoreCase = true)) "System" else "SkyOs"

    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(accent.copy(alpha = 0.92f), CircleShape),
            )
            Text(
                text = systemLabel.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = accent.copy(alpha = 0.96f),
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

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
