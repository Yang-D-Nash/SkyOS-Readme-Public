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
    val screenBottomPadding = 22.dp
    val cardPadding = 15.dp
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
    primaryAlpha: Float = 0.045f,
    secondaryAlpha: Float = 0.032f,
): Brush {
    val background = MaterialTheme.colorScheme.background
    val isDarkPalette = background.luminance() < 0.36f

    val topSky = if (isDarkPalette) Color(0xFF050B13) else Color(0xFFDCE4EC)
    val upperSky = if (isDarkPalette) Color(0xFF09121D) else Color(0xFFCED8E2)
    val horizonBase = if (isDarkPalette) Color(0xFF102131) else Color(0xFFBBC8D6)
    val groundDepth = if (isDarkPalette) Color(0xFF03070D) else Color(0xFF9EACBB)
    val primaryMist = lerp(primaryColor, horizonBase, if (isDarkPalette) 0.90f else 0.86f)
    val secondaryMist = lerp(secondaryColor, horizonBase, if (isDarkPalette) 0.92f else 0.88f)

    return Brush.verticalGradient(
        colors = listOf(
            topSky,
            upperSky,
            horizonBase,
            primaryMist.copy(alpha = primaryAlpha),
            secondaryMist.copy(alpha = secondaryAlpha),
            groundDepth.copy(alpha = if (isDarkPalette) 0.34f else 0.15f),
            Color.Black.copy(alpha = if (isDarkPalette) 0.36f else 0.12f),
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
