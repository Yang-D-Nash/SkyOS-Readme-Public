package com.nash.skyos.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = SkyLight,
    primaryContainer = SkyLightContainer,
    onPrimary = TextDark,
    onPrimaryContainer = TextLight,
    secondary = MysticLight,
    secondaryContainer = MysticLightContainer,
    onSecondary = TextDark,
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
    onBackground = TextLight,
    onSurface = TextLight,
    onSurfaceVariant = TextMutedLight,
)

private val DarkColors = darkColorScheme(
    primary = SkyDark,
    primaryContainer = SkyDarkContainer,
    onPrimary = BackgroundDark,
    onPrimaryContainer = TextDark,
    secondary = MysticDark,
    secondaryContainer = MysticDarkContainer,
    onSecondary = BackgroundDark,
    onSecondaryContainer = TextDark,
    tertiary = AuroraDark,
    tertiaryContainer = AuroraDarkContainer,
    onTertiary = BackgroundDark,
    onTertiaryContainer = TextDark,
    background = BackgroundDark,
    surface = SurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    outline = OutlineDark,
    outlineVariant = OutlineDark.copy(alpha = 0.76f),
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
        shapes = SkydownShapes,
        content = content,
    )
}
