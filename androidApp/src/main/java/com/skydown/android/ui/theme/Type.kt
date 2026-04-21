package com.skydown.android.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.skydown.android.R

private val SyneTitleFontFamily = FontFamily(
    Font(R.font.syne_regular, FontWeight.Normal),
    Font(R.font.syne_medium, FontWeight.Medium),
    Font(R.font.syne_semibold, FontWeight.SemiBold),
    Font(R.font.syne_bold, FontWeight.Bold),
    Font(R.font.syne_extrabold, FontWeight.ExtraBold),
    Font(R.font.syne_extrabold, FontWeight.Black),
)

private val AwergyDisplayFontFamily = FontFamily(
    Font(R.font.awergy_regular, FontWeight.Normal),
)

private val TightPlatformStyle = PlatformTextStyle(includeFontPadding = false)

val SkydownHeroEyebrowTextStyle = TextStyle(
    fontFamily = SyneTitleFontFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 12.5.sp,
    lineHeight = 15.sp,
    letterSpacing = 2.4.sp,
    platformStyle = TightPlatformStyle,
)

val SkydownHeroTitleTextStyle = TextStyle(
    fontFamily = AwergyDisplayFontFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 38.sp,
    lineHeight = 40.sp,
    letterSpacing = 0.sp,
    platformStyle = TightPlatformStyle,
)

val SkydownCompactHeroTitleTextStyle = TextStyle(
    fontFamily = AwergyDisplayFontFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 35.sp,
    lineHeight = 37.sp,
    letterSpacing = 0.sp,
    platformStyle = TightPlatformStyle,
)

val SkydownHeroSubtitleTextStyle = TextStyle(
    fontFamily = SyneTitleFontFamily,
    fontWeight = FontWeight.Medium,
    fontSize = 17.sp,
    lineHeight = 23.sp,
    letterSpacing = 0.sp,
    platformStyle = TightPlatformStyle,
)

val SkydownEditorialCaptionTextStyle = TextStyle(
    fontFamily = SyneTitleFontFamily,
    fontWeight = FontWeight.Medium,
    fontSize = 13.4.sp,
    lineHeight = 18.sp,
    letterSpacing = 0.18.sp,
    platformStyle = TightPlatformStyle,
)

val SkydownBodyCaptionTextStyle = TextStyle(
    fontFamily = SyneTitleFontFamily,
    fontWeight = FontWeight.Medium,
    fontSize = 13.4.sp,
    lineHeight = 18.sp,
    letterSpacing = 0.04.sp,
    platformStyle = TightPlatformStyle,
)

val SkydownCardTitleTextStyle = TextStyle(
    fontFamily = SyneTitleFontFamily,
    fontWeight = FontWeight.SemiBold,
    fontSize = 20.sp,
    lineHeight = 25.sp,
    letterSpacing = (-0.10).sp,
    platformStyle = TightPlatformStyle,
)

val SkydownSectionTitleTextStyle = TextStyle(
    fontFamily = SyneTitleFontFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 26.sp,
    lineHeight = 30.sp,
    letterSpacing = (-0.18).sp,
    platformStyle = TightPlatformStyle,
)

val SkydownPanelTitleTextStyle = TextStyle(
    fontFamily = SyneTitleFontFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 22.sp,
    lineHeight = 28.sp,
    letterSpacing = (-0.08).sp,
    platformStyle = TightPlatformStyle,
)

val Typography = Typography(
    headlineLarge = TextStyle(
        fontFamily = AwergyDisplayFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 40.sp,
        lineHeight = 42.sp,
        letterSpacing = 0.sp,
        platformStyle = TightPlatformStyle,
    ),
    headlineMedium = TextStyle(
        fontFamily = SyneTitleFontFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 28.sp,
        lineHeight = 31.sp,
        letterSpacing = (-0.24).sp,
        platformStyle = TightPlatformStyle,
    ),
    headlineSmall = TextStyle(
        fontFamily = SyneTitleFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 23.sp,
        lineHeight = 27.sp,
        letterSpacing = (-0.18).sp,
        platformStyle = TightPlatformStyle,
    ),
    titleLarge = TextStyle(
        fontFamily = SyneTitleFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.5.sp,
        lineHeight = 23.sp,
        letterSpacing = (-0.08).sp,
        platformStyle = TightPlatformStyle,
    ),
    titleMedium = TextStyle(
        fontFamily = SyneTitleFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 20.sp,
        letterSpacing = (-0.04).sp,
        platformStyle = TightPlatformStyle,
    ),
    titleSmall = TextStyle(
        fontFamily = SyneTitleFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 19.sp,
        platformStyle = TightPlatformStyle,
    ),
    bodyLarge = TextStyle(
        fontFamily = SyneTitleFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        platformStyle = TightPlatformStyle,
    ),
    bodyMedium = TextStyle(
        fontFamily = SyneTitleFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp,
        platformStyle = TightPlatformStyle,
    ),
    bodySmall = TextStyle(
        fontFamily = SyneTitleFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 19.sp,
        platformStyle = TightPlatformStyle,
    ),
    labelLarge = TextStyle(
        fontFamily = SyneTitleFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.2.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.12.sp,
        platformStyle = TightPlatformStyle,
    ),
    labelMedium = TextStyle(
        fontFamily = SyneTitleFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.5.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.18.sp,
        platformStyle = TightPlatformStyle,
    ),
    labelSmall = TextStyle(
        fontFamily = SyneTitleFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 10.5.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.32.sp,
        platformStyle = TightPlatformStyle,
    ),
)
