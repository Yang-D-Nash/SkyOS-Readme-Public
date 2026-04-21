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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.skydown.android.ui.theme.skydownAccentHighlight
import com.skydown.android.ui.theme.skydownCardBackground
import com.skydown.android.ui.theme.skydownCinematicShadow
import com.skydown.android.ui.theme.skydownIsDarkPalette
import com.skydown.android.ui.theme.skydownLuminanceLift
import com.skydown.android.ui.theme.skydownPrimaryBackground
import com.skydown.android.ui.theme.skydownSecondaryBackground

object SkydownUiTokens {
    val screenHorizontalPadding = 18.dp
    val screenTopPadding = 12.dp
    val screenBottomPadding = 16.dp
    val cardPadding = 16.dp
    val heroPadding = 18.dp
    val cardCornerRadius = 28.dp
    val heroCornerRadius = 34.dp
    val buttonCornerRadius = 22.dp
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
    primaryAlpha: Float = 0.032f,
    secondaryAlpha: Float = 0.024f,
): Brush {
    val colorScheme = MaterialTheme.colorScheme
    val background = colorScheme.skydownPrimaryBackground()
    val isDarkPalette = colorScheme.skydownIsDarkPalette()

    val luminanceLift = colorScheme.skydownLuminanceLift()
    val topSky = if (isDarkPalette) Color(0xFF0D1522) else Color(0xFFFAF7F3)
    val midSky = if (isDarkPalette) Color(0xFF121D2C) else Color(0xFFEEF0F3)
    val horizonGlow = if (isDarkPalette) Color(0xFF1F2D40) else Color(0xFFE1E6EC)
    val cinematicShadow = colorScheme.skydownCinematicShadow()
    val pearlWash = colorScheme.skydownCardBackground()
    val surfaceWash = colorScheme.skydownSecondaryBackground()
    val accentHighlight = colorScheme.skydownAccentHighlight()

    return Brush.verticalGradient(
        colors = listOf(
            luminanceLift.copy(alpha = if (isDarkPalette) 0.12f else 0.72f),
            topSky,
            pearlWash.copy(alpha = if (isDarkPalette) 0.10f else 0.22f),
            midSky,
            surfaceWash.copy(alpha = if (isDarkPalette) 0.16f else 0.30f),
            horizonGlow,
            accentHighlight.copy(alpha = if (isDarkPalette) 0.06f else 0.035f),
            primaryColor.copy(alpha = if (isDarkPalette) maxOf(primaryAlpha, 0.080f) else primaryAlpha),
            secondaryColor.copy(alpha = if (isDarkPalette) maxOf(secondaryAlpha, 0.050f) else secondaryAlpha),
            cinematicShadow.copy(alpha = if (isDarkPalette) 0.070f else 0.036f),
            cinematicShadow.copy(alpha = if (isDarkPalette) 0.08f else 0.042f),
            background,
        ),
    )
}

@Composable
fun skydownTopBarColors(): TopAppBarColors {
    val colorScheme = MaterialTheme.colorScheme
    val isDarkPalette = colorScheme.skydownIsDarkPalette()
    return TopAppBarDefaults.topAppBarColors(
        containerColor = Color.Transparent,
        scrolledContainerColor = colorScheme.skydownCardBackground().copy(alpha = if (isDarkPalette) 0.78f else 0.88f),
        navigationIconContentColor = colorScheme.onSurface,
        titleContentColor = colorScheme.onSurface,
        actionIconContentColor = colorScheme.onSurface,
    )
}

@Composable
fun SkydownTopBarTitle(
    title: String,
    subtitle: String? = null,
    accent: Color = MaterialTheme.colorScheme.primary,
) {
    val compactLayout = rememberIsCompactAppLayout()
    val showSystemMeta = title.equals("SkyOs", ignoreCase = true) && !compactLayout

    Column(verticalArrangement = Arrangement.spacedBy(if (showSystemMeta) 3.dp else 0.dp)) {
        if (showSystemMeta) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .background(accent.copy(alpha = 0.90f), CircleShape),
                )
                Text(
                    text = "SYSTEM",
                    style = MaterialTheme.typography.labelMedium,
                    color = accent.copy(alpha = 0.92f),
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        if (showSystemMeta && !subtitle.isNullOrBlank()) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
