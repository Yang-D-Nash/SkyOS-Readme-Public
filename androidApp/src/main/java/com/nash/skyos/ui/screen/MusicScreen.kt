package com.nash.skyos.ui.screen

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.ArrowOutward
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nash.skyos.data.AppContainer
import com.nash.skyos.ui.component.AppTopBarSessionActions
import com.nash.skyos.ui.component.BrandArtwork
import com.nash.skyos.ui.component.BrandHeroCard
import com.nash.skyos.ui.component.BrandPill
import com.nash.skyos.ui.component.SkydownTopBarTitle
import com.nash.skyos.ui.component.SkydownUiTokens
import com.nash.skyos.ui.component.rememberUsesCompactVisualDensity
import com.nash.skyos.ui.component.skydownAtmosphereBackground
import com.nash.skyos.ui.component.skydownPressable
import com.nash.skyos.ui.component.skydownTopBarColors
import com.nash.skyos.ui.model.MusicUiState
import com.nash.skyos.ui.theme.SkydownMusicArtistNameTextStyle
import com.nash.skyos.ui.theme.SpotifyGreen
import com.nash.skyos.ui.theme.skydownAccent
import com.nash.skyos.ui.theme.skydownAccentHighlight
import com.nash.skyos.ui.theme.skydownAccentMystic
import com.nash.skyos.ui.theme.skydownIsDarkPalette
import com.nash.skyos.ui.theme.skydownSecondaryBackground
import com.nash.skyos.ui.theme.skydownSpotify
import com.nash.skyos.ui.viewmodel.MusicViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Suppress("UNUSED_PARAMETER")
fun MusicScreen(
    onBack: (() -> Unit)? = null,
    onOpenStudio: (() -> Unit)? = null,
    initialArtist: String? = null,
    initialTrackId: Int? = null,
    autoOpenSelectedTrackInSpotify: Boolean = false,
    onOpenCart: (() -> Unit)? = null,
    onOpenProfile: (() -> Unit)? = null,
    onOpenSettings: (() -> Unit)? = null,
    onGuestSignIn: (() -> Unit)? = null,
    onOpenArtistPage: ((String) -> Unit)? = null,
    onArtistContextChange: ((String) -> Unit)? = null,
    viewModel: MusicViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val compactVisualDensity = rememberUsesCompactVisualDensity()
    val scroll = rememberScrollState()
    val haptics = LocalHapticFeedback.current
    var catalogMotionReady by remember { mutableStateOf(false) }
    var listStaggerArmed by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(48)
        catalogMotionReady = true
        delay(120)
        listStaggerArmed = true
    }

    LaunchedEffect(uiState.selectedArtist) {
        onArtistContextChange?.invoke(uiState.selectedArtist)
    }

    LaunchedEffect(initialArtist) {
        initialArtist
            ?.takeIf { it.isNotBlank() && it != uiState.selectedArtist }
            ?.let(viewModel::selectArtist)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    SkydownTopBarTitle(
                        title = "Music",
                        subtitle = null,
                        accent = SpotifyGreen,
                    )
                },
                actions = {
                    if (onOpenSettings != null) {
                        AppTopBarSessionActions(
                            onOpenCart = onOpenCart,
                            onOpenProfile = onOpenProfile,
                            onOpenSettings = onOpenSettings,
                            onGuestSignIn = onGuestSignIn,
                            dense = compactVisualDensity,
                        ) {
                            if (uiState.isSpotifyConnected) {
                                IconButton(
                                    onClick = { viewModel.disconnectSpotify() },
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Logout,
                                        contentDescription = "Spotify trennen",
                                    )
                                }
                            }
                        }
                    } else if (uiState.isSpotifyConnected) {
                        IconButton(
                            onClick = { viewModel.disconnectSpotify() },
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Logout,
                                contentDescription = "Spotify trennen",
                            )
                        }
                    }
                },
                navigationIcon = if (onBack != null) {
                    {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Zurueck",
                            )
                        }
                    }
                } else {
                    {}
                },
                colors = skydownTopBarColors(),
            )
        },
    ) { innerPadding ->
        val colorScheme = MaterialTheme.colorScheme
        Box(
            modifier = Modifier
                .fillMaxSize()
                .skydownAtmosphereBackground(
                    primaryColor = colorScheme.skydownAccent(),
                    secondaryColor = colorScheme.skydownSpotify(),
                    primaryAlpha = 0.022f,
                    secondaryAlpha = 0.016f,
                ),
        ) {
            MusicStageBackdrop(Modifier.fillMaxSize())
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.TopCenter,
            ) {
                val contentMaxWidth = if (compactVisualDensity) 620.dp else 1080.dp
                val heroNudge by animateFloatAsState(
                    targetValue = if (catalogMotionReady) 1f else 0f,
                    animationSpec = spring(dampingRatio = 0.82f, stiffness = 280f),
                    label = "heroEnter",
                )
                val heroLiftPx = with(LocalDensity.current) { 12.dp.toPx() }
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = contentMaxWidth)
                        .verticalScroll(scroll)
                        .testTag("music.catalog.root")
                        .padding(
                            start = SkydownUiTokens.screenHorizontalPadding,
                            end = SkydownUiTokens.screenHorizontalPadding,
                            top = innerPadding.calculateTopPadding() + 8.dp,
                            bottom = innerPadding.calculateBottomPadding() +
                                WindowInsets.navigationBars.asPaddingValues()
                                    .calculateBottomPadding() +
                                SkydownUiTokens.screenBottomPadding + 32.dp,
                        ),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    MusicCatalogHero(
                        uiState = uiState,
                        compactVisualDensity = compactVisualDensity,
                        topContentPadding = 0.dp,
                        onOpenArtistPage = onOpenArtistPage,
                        modifier = Modifier
                            .testTag("music.catalog.hero")
                            .alpha(0.82f + 0.18f * heroNudge)
                            .graphicsLayer {
                                translationY = (1f - heroNudge) * heroLiftPx
                            },
                    )
                    ArtistPagerCard(
                        artists = uiState.availableArtists,
                        selectedArtist = uiState.selectedArtist,
                        onOpenArtist = { name ->
                            haptics.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
                            viewModel.selectArtist(name)
                            onOpenArtistPage?.invoke(name)
                        },
                        listStaggerKey = listStaggerArmed,
                    )
                }
            }
        }
    }
}

@Composable
private fun MusicCatalogHero(
    uiState: MusicUiState,
    compactVisualDensity: Boolean,
    topContentPadding: Dp,
    onOpenArtistPage: ((String) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val screenHeader by AppContainer.screenHeaderSettingsRepository.settings.collectAsStateWithLifecycle()
    val colorScheme = MaterialTheme.colorScheme
    val rowScroll = rememberScrollState()
    Box(
        modifier = modifier,
    ) {
        BrandHeroCard(
            eyebrow = screenHeader.musicHubEyebrow.ifBlank { "SkyOS" },
            title = screenHeader.musicHubTitle.ifBlank { "Music" },
            subtitle = screenHeader.musicHubSubtitle.ifBlank { "Ein Hub · drei Wege." },
            detail = screenHeader.musicHubDetail.ifBlank { "Katalog, Releases, Studio." },
            backgroundImageUrl = screenHeader.musicHubImageUrl.ifBlank { null },
            accent = SpotifyGreen,
            secondaryAccent = colorScheme.skydownAccent(),
            marks = listOf(BrandArtwork.Zweizwei),
            compactVisualDensity = compactVisualDensity,
            edgeToEdge = true,
            topContentPadding = topContentPadding,
            onSurfaceClick = onOpenArtistPage?.let { open ->
                { open(uiState.selectedArtist) }
            },
        ) {
            Row(
                modifier = Modifier
                    .horizontalScroll(rowScroll),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                BrandPill(
                    text = uiState.selectedArtist,
                    tint = SpotifyGreen,
                    onClick = onOpenArtistPage?.let { o ->
                        { o(uiState.selectedArtist) }
                    },
                )
            }
        }
    }
}

@Composable
private fun MusicStageBackdrop(
    modifier: Modifier = Modifier,
) {
    val colorScheme = MaterialTheme.colorScheme
    val isDarkPalette = colorScheme.skydownIsDarkPalette()
    Box(
        modifier = modifier.drawWithContent {
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        colorScheme.skydownSpotify().copy(alpha = if (isDarkPalette) 0.14f else 0.06f),
                        Color.Transparent,
                    ),
                    center = Offset(size.width, 0f),
                    radius = 320.dp.toPx(),
                ),
            )
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        colorScheme.skydownAccentHighlight().copy(alpha = if (isDarkPalette) 0.10f else 0.05f),
                        Color.Transparent,
                    ),
                    center = Offset(0f, size.height),
                    radius = 300.dp.toPx(),
                ),
            )
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = if (isDarkPalette) 0.05f else 0.10f),
                        Color.Transparent,
                        Color.Black.copy(alpha = if (isDarkPalette) 0.05f else 0.02f),
                    ),
                    startY = 0f,
                    endY = size.height,
                ),
            )
            drawContent()
        },
    )
}

@Composable
private fun ArtistPagerCard(
    artists: List<String>,
    selectedArtist: String,
    onOpenArtist: (String) -> Unit,
    listStaggerKey: Boolean,
) {
    val safeArtists = artists.ifEmpty { listOf(selectedArtist) }
    ArtistButtonHub(
        artists = safeArtists,
        selectedArtist = selectedArtist,
        onOpenArtist = onOpenArtist,
        listStaggerKey = listStaggerKey,
    )
}

@Composable
private fun ArtistButtonHub(
    artists: List<String>,
    selectedArtist: String,
    onOpenArtist: (String) -> Unit,
    listStaggerKey: Boolean,
) {
    val colorScheme = MaterialTheme.colorScheme
    val labelLiftPx = with(LocalDensity.current) { 6.dp.toPx() }
    val headerAlpha by animateFloatAsState(
        targetValue = if (listStaggerKey) 1f else 0.35f,
        animationSpec = tween(280),
        label = "dirEinstiegHeader",
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Direkter Einstieg",
            style = MaterialTheme.typography.labelMedium,
            color = colorScheme.onSurface.copy(alpha = 0.72f),
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .alpha(headerAlpha)
                .graphicsLayer { translationY = (1f - headerAlpha) * labelLiftPx },
        )
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            artists.forEachIndexed { index, artist ->
                key(artist) {
                    StaggeredArtistEinstiegRow(
                        index = index,
                        artist = artist,
                        listStaggerKey = listStaggerKey,
                        selected = artist == selectedArtist,
                        onOpen = { onOpenArtist(artist) },
                    )
                }
            }
        }
    }
}

@Composable
private fun StaggeredArtistEinstiegRow(
    index: Int,
    artist: String,
    listStaggerKey: Boolean,
    selected: Boolean,
    onOpen: () -> Unit,
) {
    var rowShown by remember(artist) { mutableStateOf(false) }
    LaunchedEffect(listStaggerKey) {
        if (listStaggerKey) {
            delay(32L * index)
            rowShown = true
        } else {
            rowShown = false
        }
    }
    val appear by animateFloatAsState(
        targetValue = if (rowShown) 1f else 0f,
        animationSpec = spring(
            dampingRatio = 0.86f,
            stiffness = 420f,
        ),
        label = "artistRow$index",
    )
    val colorScheme = MaterialTheme.colorScheme
    val liftPx = with(LocalDensity.current) { 10.dp.toPx() }
    val interaction = remember(artist) { MutableInteractionSource() }
    val accent = musicCatalogEntryAccent(artist, colorScheme)
    val isDark = colorScheme.skydownIsDarkPalette()
    val background = if (selected) {
        accent.copy(alpha = if (isDark) 0.22f else 0.14f)
    } else {
        colorScheme.skydownSecondaryBackground()
    }
    val borderAlpha = if (selected) 0.55f else 0.35f
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("music.artist.open_page.$artist")
            .alpha(0.18f + 0.82f * appear)
            .graphicsLayer {
                translationY = (1f - appear) * liftPx
                scaleX = 0.96f + 0.04f * appear
            }
            .clip(RoundedCornerShape(14.dp))
            .skydownPressable(interaction, pressedScale = 0.99f)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onOpen,
            ),
        shape = RoundedCornerShape(14.dp),
        color = background,
        border = BorderStroke(1.dp, accent.copy(alpha = borderAlpha)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 46.dp)
                .padding(horizontal = 12.dp, vertical = 11.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Default.ArrowOutward,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = if (selected) accent else colorScheme.onSurface,
            )
            Text(
                text = artist,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = colorScheme.onSurface,
                style = SkydownMusicArtistNameTextStyle,
            )
        }
    }
}

private fun musicCatalogEntryAccent(artist: String, colorScheme: ColorScheme): Color = when (artist) {
    "JANNO" -> colorScheme.skydownAccent()
    "Yang D. Nash" -> colorScheme.skydownAccentHighlight()
    "MAVE" -> colorScheme.skydownAccentMystic()
    "ThaDude" -> colorScheme.skydownAccent()
    "TANGAJOE007" -> colorScheme.skydownSpotify()
    else -> colorScheme.skydownSpotify()
}
