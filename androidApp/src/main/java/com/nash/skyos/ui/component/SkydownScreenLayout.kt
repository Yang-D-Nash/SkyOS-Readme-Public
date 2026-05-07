package com.nash.skyos.ui.component

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
import androidx.compose.ui.draw.paint
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nash.skyos.R
import com.nash.skyos.ui.theme.SkydownBodyCaptionTextStyle
import com.nash.skyos.ui.theme.SkydownHeroEyebrowTextStyle
import com.nash.skyos.ui.theme.SkydownPanelTitleTextStyle
import com.nash.skyos.ui.theme.skydownAccentHighlight
import com.nash.skyos.ui.theme.skydownAtmosphereHorizon
import com.nash.skyos.ui.theme.skydownAtmosphereMid
import com.nash.skyos.ui.theme.skydownAtmosphereTop
import com.nash.skyos.ui.theme.skydownCardBackground
import com.nash.skyos.ui.theme.skydownCinematicShadow
import com.nash.skyos.ui.theme.skydownIsDarkPalette
import com.nash.skyos.ui.theme.skydownLuminanceLift
import com.nash.skyos.ui.theme.skydownPrimaryBackground
import com.nash.skyos.ui.theme.skydownSecondaryBackground
import com.nash.skyos.ui.theme.skydownSecondaryText
import com.nash.skyos.ui.theme.skydownText

object SkydownUiTokens {
    val screenHorizontalPadding = 20.dp
    val screenTopPadding = 18.dp
    val screenBottomPadding = 56.dp
    val cardPadding = 18.dp
    /// Größere Karten/Module (Music, Video, Profil) — iOS `SkydownLayout.panelPadding` Parität.
    val panelPadding = 22.dp
    val heroPadding = 20.dp
    val cardCornerRadius = 20.dp
    val heroCornerRadius = 28.dp
    val buttonCornerRadius = 14.dp
    val buttonCompactCornerRadius = 15.dp
    val buttonStandardCornerRadius = 16.dp
    val buttonCompactMinHeight = 40.dp
    val buttonStandardMinHeight = 46.dp
    val buttonCompactIconSize = 16.dp
    val buttonStandardIconSize = 18.dp
    val iconActionCompactSurfaceSize = 36.dp
    val iconActionSurfaceSize = 40.dp
    val iconActionContentSize = 18.dp
    val stateIconSurfaceSize = 42.dp
    val stateIconContentSize = 20.dp
    val stateIconProgressSize = 18.dp
    val toastAccentRailWidth = 3.dp
    val toastIconSurfaceSize = 40.dp
    val toastIconContentSize = 22.dp
    val toastHorizontalPadding = 12.dp
    val toastVerticalPadding = 11.dp
    val portalChipHorizontalPadding = 11.dp
    val portalChipVerticalPadding = 9.dp
    val portalChipIconSurfaceSize = 24.dp
    val portalChipIconContentSize = 13.dp
    val segmentHorizontalPadding = 12.dp
    val segmentVerticalPadding = 8.dp
    val segmentCenteredHorizontalPadding = 8.dp
    val segmentCenteredVerticalPadding = 10.dp
    val linkCompactHorizontalPadding = 10.dp
    val linkCompactVerticalPadding = 7.dp
    val linkCompactIconSize = 12.dp
    val linkCompactContentSpacing = 5.dp
    val linkStandardHorizontalPadding = 16.dp
    val linkStandardVerticalPadding = 14.dp
    val inlinePanelHorizontalPadding = 10.dp
    val inlinePanelVerticalPadding = 8.dp
    val inlinePanelIconSize = 14.dp
    val inlinePanelMinHeight = 34.dp
    val countBadgeHorizontalPadding = 6.dp
    val countBadgeVerticalPadding = 1.dp
    val editableImagePreviewHeight = 136.dp
    val editableVideoPreviewHeight = 148.dp
    val mediaOverlayHorizontalPadding = 20.dp
    val sheetDragHandleWidth = 52.dp
    val sheetDragHandleHeight = 4.dp
    val buttonIconLabelSpacing = 8.dp
    val buttonCompactHorizontalPadding = 14.dp
    val buttonStandardHorizontalPadding = 16.dp
    val buttonCompactVerticalPadding = 9.dp
    val buttonStandardVerticalPadding = 12.dp
    val screenSectionSpacing = 22.dp
    val compactScreenSectionSpacing = 16.dp
    /// Sehr kleine Ecken (z. B. kompakte Menü-Clips).
    val microCorner = 8.dp
    /// Kleine Kacheln und Tag-Container.
    val tightRadius = 12.dp
    /// Chips, kleine Clips, kompakte UI-Elemente.
    val compactRadius = 14.dp
    /// Dichte Raster / sekundäre Flächen.
    val denseRadius = 16.dp
    /// Chat-Bubbles, modale Eingaben — etwas weicher als Karte.
    val messageBubbleRadius = 18.dp
    /// Größere Paneele, Sheets (unterhalb Hero).
    val elevatedPanelRadius = 24.dp
    /// Prominente Sheet- und Dialog-Ecken.
    val sheetHeroRadius = 26.dp
    /// Große Medien-/Spotlight-Kacheln (zwischen Card und Hero).
    val spotlightRadius = 28.dp
    /// Katalog-Zeilen (Music/Video), leicht enger als Standard-Karte.
    val catalogCornerRadius = 17.dp
    /// Sanfte Mini-Pills (Badges, Micro-Chips).
    val pillSoftRadius = 10.dp
    /// Minimaler Radius (Fortschritt, Hairline-Flächen).
    val nanoCorner = 4.dp
    /// Volle Pille / Capsule (ersetzt magisches 999.dp an einer Stelle im Code).
    val fullCapsuleRadius = 999.dp
    /// Sheet-Ziehgriff (Material-nah, aber markenkonstant).
    val sheetDragHandleRadius = 99.dp

    /// Parität iOS `SkydownLayout.stackSpacingComfortable`.
    val stackSpacingComfortable get() = cardPadding
    /// Parität iOS `SkydownLayout.stackSpacingCompact`.
    val stackSpacingCompact get() = tightRadius
    /// Parität iOS `SkydownLayout.stackSpacingMicro`.
    val stackSpacingMicro get() = microCorner
    /// Parität iOS `SkydownLayout.stackSpacingPill`.
    val stackSpacingPill get() = pillSoftRadius
    /// Parität iOS `SkydownLayout.stackSpacingRelaxed`.
    val stackSpacingRelaxed get() = compactRadius
    /// Parität iOS `SkydownLayout.stackSpacingSection`.
    val stackSpacingSection get() = screenSectionSpacing

    /// Parität iOS `SkydownLayout.stackSpacingNone`.
    val stackSpacingNone get() = 0.dp
    /// Parität iOS `SkydownLayout.stackSpacingSingle`.
    val stackSpacingSingle = 1.dp
    /// Parität iOS `SkydownLayout.stackSpacingHairline`.
    val stackSpacingHairline = 2.dp
    /// Parität iOS `SkydownLayout.stackSpacingTick`.
    val stackSpacingTick = 3.dp
    /// Parität iOS `SkydownLayout.stackSpacingNano`.
    val stackSpacingNano get() = nanoCorner
    /// Parität iOS `SkydownLayout.stackSpacingSubtle`.
    val stackSpacingSubtle = 5.dp
    /// Parität iOS `SkydownLayout.stackSpacingDense`.
    val stackSpacingDense = 6.dp
    /// Parität iOS `SkydownLayout.stackSpacingChrome`.
    val stackSpacingChrome = 7.dp
    /// Parität iOS `SkydownLayout.stackSpacingSnug`.
    val stackSpacingSnug = 9.dp
    /// Parität iOS `SkydownLayout.stackSpacingToast`.
    val stackSpacingToast = 11.dp
    /// Parität iOS `SkydownLayout.stackSpacingLoft`.
    val stackSpacingLoft = 15.dp
    /// Parität iOS `SkydownLayout.stackSpacingHero`.
    val stackSpacingHero get() = heroPadding
    /// Parität iOS `SkydownLayout.stackSpacingDockRow` (Dock-/Multi-Icon-Zeile).
    val stackSpacingDockRow = 13.dp
    /// Parität iOS `SkydownLayout.layoutProminentInset` (Dock-Vertikal, betonte Schatten).
    val layoutProminentInset = 19.dp

    val elevationHairline = 1.dp
    val elevationRaised = 6.dp
    val elevationStateIcon = 3.dp
    val elevationPanel = 14.dp
    val elevationHero = 18.dp
}

fun skydownContentPadding(innerPadding: PaddingValues): PaddingValues = PaddingValues(
    start = SkydownUiTokens.screenHorizontalPadding,
    top = innerPadding.calculateTopPadding() + SkydownUiTokens.screenTopPadding,
    end = SkydownUiTokens.screenHorizontalPadding,
    bottom = innerPadding.calculateBottomPadding() + SkydownUiTokens.screenBottomPadding,
)

@Composable
fun rememberSkydownScreenSectionSpacing() =
    if (rememberUsesCompactVisualDensity()) {
        SkydownUiTokens.compactScreenSectionSpacing
    } else {
        SkydownUiTokens.screenSectionSpacing
    }

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
    val topSky = colorScheme.skydownAtmosphereTop()
    val midSky = colorScheme.skydownAtmosphereMid()
    val horizonGlow = colorScheme.skydownAtmosphereHorizon()
    val cinematicShadow = colorScheme.skydownCinematicShadow()
    val pearlWash = colorScheme.skydownCardBackground()
    val surfaceWash = colorScheme.skydownSecondaryBackground()
    val accentHighlight = colorScheme.skydownAccentHighlight()

    return Brush.verticalGradient(
        colors = listOf(
            luminanceLift.copy(alpha = if (isDarkPalette) 0.08f else 0.52f),
            topSky,
            pearlWash.copy(alpha = if (isDarkPalette) 0.07f else 0.16f),
            midSky,
            surfaceWash.copy(alpha = if (isDarkPalette) 0.12f else 0.22f),
            horizonGlow,
            accentHighlight.copy(alpha = if (isDarkPalette) 0.04f else 0.024f),
            primaryColor.copy(alpha = if (isDarkPalette) maxOf(primaryAlpha, 0.050f) else primaryAlpha * 0.70f),
            secondaryColor.copy(alpha = if (isDarkPalette) maxOf(secondaryAlpha, 0.032f) else secondaryAlpha * 0.68f),
            cinematicShadow.copy(alpha = if (isDarkPalette) 0.050f else 0.026f),
            cinematicShadow.copy(alpha = if (isDarkPalette) 0.055f else 0.030f),
            background,
        ),
    )
}

@Composable
fun Modifier.skydownAtmosphereBackground(
    primaryColor: Color = MaterialTheme.colorScheme.primary,
    secondaryColor: Color = MaterialTheme.colorScheme.secondary,
    primaryAlpha: Float = 0.032f,
    secondaryAlpha: Float = 0.024f,
    /** `false` = solid app background + skyline only (e.g. launch landing); avoids stacking [skydownScreenBrush] on top of the photo. */
    includeScreenGradient: Boolean = true,
): Modifier {
    val isDarkPalette = MaterialTheme.colorScheme.skydownIsDarkPalette()
    val colorScheme = MaterialTheme.colorScheme
    val withBase = if (includeScreenGradient) {
        this.background(
            skydownScreenBrush(
                primaryColor = primaryColor,
                secondaryColor = secondaryColor,
                primaryAlpha = primaryAlpha,
                secondaryAlpha = secondaryAlpha,
            ),
        )
    } else {
        this.background(colorScheme.skydownPrimaryBackground())
    }
    return withBase.paint(
        painter = painterResource(id = R.drawable.skyline_atmosphere_background),
        contentScale = ContentScale.Crop,
        alpha = if (isDarkPalette) 0.16f else 0.08f,
    )
}

@Composable
fun skydownTopBarColors(): TopAppBarColors {
    val colorScheme = MaterialTheme.colorScheme
    val iconColor = colorScheme.onSurface.copy(alpha = 0.92f)
    return TopAppBarDefaults.topAppBarColors(
        containerColor = Color.Transparent,
        scrolledContainerColor = Color.Transparent,
        navigationIconContentColor = iconColor,
        titleContentColor = colorScheme.onSurface,
        actionIconContentColor = iconColor,
    )
}

@Composable
fun SkydownTopBarTitle(
    title: String,
    subtitle: String? = null,
    accent: Color = MaterialTheme.colorScheme.primary,
    metaLabel: String? = null,
) {
    val compactLayout = rememberIsCompactAppLayout()
    val colorScheme = MaterialTheme.colorScheme
    val resolvedMetaLabel = metaLabel?.takeIf { it.isNotBlank() && !compactLayout }
    val resolvedSubtitle = subtitle?.takeIf { it.isNotBlank() && !compactLayout }

    Column(
        verticalArrangement = Arrangement.spacedBy(
            if (resolvedMetaLabel != null || resolvedSubtitle != null) SkydownUiTokens.stackSpacingNano else SkydownUiTokens.stackSpacingNone,
        ),
    ) {
        if (resolvedMetaLabel != null) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingDense),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .background(accent.copy(alpha = 0.90f), CircleShape),
                )
                Text(
                    text = resolvedMetaLabel.uppercase(),
                    style = SkydownHeroEyebrowTextStyle,
                    color = accent.copy(alpha = 0.92f),
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Text(
            text = title,
            style = SkydownPanelTitleTextStyle,
            fontWeight = FontWeight.SemiBold,
            color = colorScheme.skydownText(),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        if (resolvedSubtitle != null) {
            Text(
                text = resolvedSubtitle,
                style = SkydownBodyCaptionTextStyle,
                color = colorScheme.skydownSecondaryText().copy(alpha = 0.82f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
