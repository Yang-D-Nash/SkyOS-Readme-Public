package com.nash.skyos.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

fun ColorScheme.skydownIsDarkPalette(): Boolean = background.luminance() < 0.36f

fun ColorScheme.skydownAccent(): Color = if (skydownIsDarkPalette()) SkyDark else SkyLight

fun ColorScheme.skydownAccentMystic(): Color = if (skydownIsDarkPalette()) MysticDark else MysticLight

fun ColorScheme.skydownAccentHighlight(): Color = if (skydownIsDarkPalette()) AuroraDark else AuroraLight

fun ColorScheme.skydownPrimaryBackground(): Color = if (skydownIsDarkPalette()) BackgroundDark else BackgroundLight

fun ColorScheme.skydownSecondaryBackground(): Color = if (skydownIsDarkPalette()) SurfaceVariantDark else SurfaceVariantLight

fun ColorScheme.skydownCardBackground(): Color = if (skydownIsDarkPalette()) SurfaceDark.copy(alpha = 0.97f) else SurfaceLight

fun ColorScheme.skydownText(): Color = if (skydownIsDarkPalette()) TextDark else TextLight

fun ColorScheme.skydownSecondaryText(): Color = if (skydownIsDarkPalette()) TextMutedDark else TextMutedLight

fun ColorScheme.skydownLuminanceLift(): Color = if (skydownIsDarkPalette()) PlatinumDark else PlatinumLight

fun ColorScheme.skydownCinematicShadow(): Color = if (skydownIsDarkPalette()) Obsidian else Graphite

fun ColorScheme.skydownAtmosphereTop(): Color = if (skydownIsDarkPalette()) Color(0xFF09111E) else Porcelain

fun ColorScheme.skydownAtmosphereMid(): Color = if (skydownIsDarkPalette()) Color(0xFF101A28) else Color(0xFFF0F1F2)

fun ColorScheme.skydownAtmosphereHorizon(): Color = if (skydownIsDarkPalette()) Color(0xFF1B293B) else Color(0xFFE4E8ED)

fun ColorScheme.skydownPremiumAccent(): Color = if (skydownIsDarkPalette()) AuroraDark else Champagne

fun ColorScheme.skydownSuccess(): Color = if (skydownIsDarkPalette()) SuccessDark else SuccessLight

fun ColorScheme.skydownSuccessContainer(): Color = if (skydownIsDarkPalette()) SuccessDarkContainer else SuccessLightContainer

fun ColorScheme.skydownError(): Color = if (skydownIsDarkPalette()) ErrorDark else ErrorLight

fun ColorScheme.skydownErrorContainer(): Color = if (skydownIsDarkPalette()) ErrorDarkContainer else ErrorLightContainer

fun ColorScheme.skydownSpotify(): Color = SpotifyGreen

fun ColorScheme.skydownSpotifySurface(): Color = skydownSpotify().copy(alpha = if (skydownIsDarkPalette()) 0.18f else 0.12f)

fun ColorScheme.skydownYoutube(): Color = if (skydownIsDarkPalette()) Color(0xFFFF5C74) else YouTubeRed

fun ColorScheme.skydownYoutubeDeep(): Color = if (skydownIsDarkPalette()) Color(0xFFD62B54) else YouTubeDeepRed

fun ColorScheme.skydownYoutubeSurface(): Color = skydownYoutube().copy(alpha = if (skydownIsDarkPalette()) 0.18f else 0.12f)

fun ColorScheme.skydownInstagramStart(): Color = if (skydownIsDarkPalette()) Color(0xFFB05CFF) else InstagramPurple

fun ColorScheme.skydownInstagramEnd(): Color = if (skydownIsDarkPalette()) Color(0xFFFF9A6F) else Color(0xFFFD1D1D)

fun ColorScheme.skydownDeepInk(): Color = if (skydownIsDarkPalette()) BackgroundDark else TextLight
