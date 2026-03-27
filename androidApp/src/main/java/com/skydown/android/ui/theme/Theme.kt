package com.skydown.android.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = SkyLight,
    primaryContainer = SkyLightContainer,
    onPrimaryContainer = TextLight,
    secondary = MysticLight,
    secondaryContainer = MysticLightContainer,
    onSecondaryContainer = TextLight,
    tertiary = AuroraLight,
    tertiaryContainer = AuroraLightContainer,
    onTertiary = TextLight,
    onTertiaryContainer = TextLight,
    background = BackgroundLight,
    surface = SurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    outline = OutlineLight,
    outlineVariant = OutlineLight.copy(alpha = 0.72f),
    onPrimary = TextLight,
    onBackground = TextLight,
    onSurface = TextLight,
    onSurfaceVariant = TextMutedLight,
)

private val DarkColors = darkColorScheme(
    primary = SkyDark,
    primaryContainer = SkyDarkContainer,
    onPrimaryContainer = TextDark,
    secondary = MysticDark,
    secondaryContainer = MysticDarkContainer,
    onSecondaryContainer = TextDark,
    tertiary = AuroraDark,
    tertiaryContainer = AuroraDarkContainer,
    onTertiaryContainer = TextDark,
    background = BackgroundDark,
    surface = SurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onTertiary = BackgroundDark,
    outline = OutlineDark,
    outlineVariant = OutlineDark.copy(alpha = 0.76f),
    onPrimary = TextDark,
    onBackground = TextDark,
    onSurface = TextDark,
    onSurfaceVariant = TextMutedDark,
)

@Composable
fun SkydownTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colors = when {
        dynamicColor && darkTheme -> DarkColors
        dynamicColor && !darkTheme -> LightColors
        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colors,
        typography = Typography,
        content = content,
    )
}
