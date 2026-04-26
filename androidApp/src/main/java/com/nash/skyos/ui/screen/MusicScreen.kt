package com.nash.skyos.ui.screen
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ArrowOutward
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nash.skyos.data.mediaAttributionContext
import com.nash.skyos.data.ArtistPageBrand
import com.nash.skyos.data.ArtistPagesStore
import com.nash.skyos.data.AppContainer
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.nash.skyos.ui.component.AppTopBarSessionActions
import com.nash.skyos.ui.component.BrandActionButton
import com.nash.skyos.ui.component.BrandArtwork
import com.nash.skyos.ui.component.BrandHeroCard
import com.nash.skyos.ui.component.BrandHeroMetricCard
import com.nash.skyos.ui.component.BrandSectionBanner
import com.nash.skyos.ui.component.BrandStatusChip
import com.nash.skyos.ui.component.BrandPill
import com.nash.skyos.ui.component.SkydownCard
import com.nash.skyos.ui.component.SkydownTopBarTitle
import com.nash.skyos.ui.component.SkydownUiTokens
import com.nash.skyos.ui.component.TrackRow
import com.nash.skyos.ui.component.TrackRowPresentation
import com.nash.skyos.ui.component.rememberSkydownScreenSectionSpacing
import com.nash.skyos.ui.component.rememberUsesCompactVisualDensity
import com.nash.skyos.ui.component.skydownAtmosphereBackground
import com.nash.skyos.ui.component.skydownTopBarColors
import com.nash.skyos.ui.component.openTrackInSpotify
import com.nash.skyos.ui.model.MusicUiState
import com.nash.skyos.ui.model.MusicInstagramDestination
import com.nash.skyos.ui.theme.InstagramOrange
import com.nash.skyos.ui.theme.SkydownCardTitleTextStyle
import com.nash.skyos.ui.theme.SkydownEditorialCaptionTextStyle
import com.nash.skyos.ui.theme.SpotifyGreen
import com.nash.skyos.ui.theme.skydownAccent
import com.nash.skyos.ui.theme.skydownAccentHighlight
import com.nash.skyos.ui.theme.skydownAccentMystic
import com.nash.skyos.ui.theme.skydownCardBackground
import com.nash.skyos.ui.theme.skydownIsDarkPalette
import com.nash.skyos.ui.theme.skydownSecondaryBackground
import com.nash.skyos.ui.theme.skydownSpotify
import com.nash.skyos.ui.viewmodel.MusicViewModel
import coil3.compose.AsyncImage
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MusicScreen(
    onBack: (() -> Unit)? = null,
    onOpenStudio: (() -> Unit)? = null,
    initialArtist: String? = null,
    initialTrackId: Int? = null,
    autoplaySelectedTrackPreview: Boolean = false,
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
    val artistPages by ArtistPagesStore.pages.collectAsState()

    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val mediaContext = remember(context) { context.mediaAttributionContext() }
    val player = remember(mediaContext) {
        ExoPlayer.Builder(mediaContext).build().apply {
            playWhenReady = true
        }
    }
    var selectedTrackId by rememberSaveable { mutableStateOf<Int?>(null) }
    var hasHandledInitialSelection by rememberSaveable { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val selectedTrack = uiState.tracks.firstOrNull { it.trackId == selectedTrackId } ?: uiState.tracks.firstOrNull()
    LaunchedEffect(uiState.selectedArtist) {
        onArtistContextChange?.invoke(uiState.selectedArtist)
    }
    val selectedTrackQueuePosition = remember(uiState.tracks, selectedTrackId) {
        uiState.tracks.indexOfFirst { it.trackId == selectedTrackId }
            .takeIf { it >= 0 }
            ?.plus(1)
    }
    val queueContextText = remember(uiState.tracks, selectedTrackQueuePosition) {
        when {
            uiState.tracks.isEmpty() -> "Queue wird vorbereitet"
            selectedTrackQueuePosition != null -> "Queue $selectedTrackQueuePosition/${uiState.tracks.size}"
            else -> "Queue ${uiState.tracks.size} Tracks"
        }
    }
    val compactVisualDensity = rememberUsesCompactVisualDensity()
    val sectionSpacing = rememberSkydownScreenSectionSpacing()
    val spotlightSectionIndex = 1
    val artistSectionIndex = 2
    val spotifyStatusSectionIndex = 3
    val tracksSectionIndex = if (uiState.tracks.isNotEmpty()) {
        4
    } else {
        spotifyStatusSectionIndex
    }
    val artistPagesByName = remember(artistPages) {
        artistPages
            .filter { it.brand == ArtistPageBrand.Zweizwei }
            .associateBy { it.artistName }
    }
    val selectedArtistPage = remember(artistPagesByName, uiState.selectedArtist) {
        artistPagesByName[uiState.selectedArtist]
            ?: ArtistPagesStore.pageFor(ArtistPageBrand.Zweizwei, uiState.selectedArtist)
    }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    viewModel.stopPreview()
                }
            }
        }
        player.addListener(listener)

        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }

    LaunchedEffect(uiState.currentPreviewUrl) {
        val previewUrl = uiState.currentPreviewUrl
        if (previewUrl.isNullOrBlank()) {
            player.stop()
            player.clearMediaItems()
        } else {
            player.setMediaItem(MediaItem.fromUri(previewUrl))
            player.prepare()
            player.play()
        }
    }

    LaunchedEffect(uiState.tracks) {
        if (selectedTrackId == null || uiState.tracks.none { it.trackId == selectedTrackId }) {
            selectedTrackId = uiState.tracks.firstOrNull()?.trackId
        }
    }

    LaunchedEffect(uiState.currentlyPlayingId) {
        uiState.currentlyPlayingId?.let { playingId ->
            selectedTrackId = playingId
        }
    }

    LaunchedEffect(initialArtist) {
        initialArtist
            ?.takeIf { it.isNotBlank() && it != uiState.selectedArtist }
            ?.let(viewModel::selectArtist)
    }

    LaunchedEffect(uiState.tracks, initialTrackId) {
        if (hasHandledInitialSelection || uiState.tracks.isEmpty()) {
            return@LaunchedEffect
        }

        val initialTrack = initialTrackId?.let { targetId ->
            uiState.tracks.firstOrNull { it.trackId == targetId }
        }
        val track = initialTrack ?: selectedTrack ?: uiState.tracks.firstOrNull() ?: return@LaunchedEffect

        selectedTrackId = track.trackId

        if (autoplaySelectedTrackPreview && !track.previewUrl.isNullOrBlank()) {
            viewModel.togglePreview(track)
        } else if (autoOpenSelectedTrackInSpotify &&
            (musicScreenResolvedSpotifyTrackId(track.spotifyTrackId, track.externalUrl) != null ||
                musicScreenResolvedSpotifyArtistId(track.spotifyArtistId, track.externalUrl) != null)
        ) {
            openTrackInSpotify(
                context = context,
                spotifyArtistId = track.spotifyArtistId,
                spotifyTrackId = track.spotifyTrackId,
                externalUrl = track.externalUrl,
            )
        }

        hasHandledInitialSelection = true
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    SkydownTopBarTitle(
                        title = "Music",
                        subtitle = "Artists, Tracks, Spotify.",
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
                                    onClick = {
                                        player.stop()
                                        player.clearMediaItems()
                                        viewModel.disconnectSpotify()
                                    },
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
                            onClick = {
                                player.stop()
                                player.clearMediaItems()
                                viewModel.disconnectSpotify()
                            },
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
            MusicStageBackdrop(
                modifier = Modifier.fillMaxSize(),
            )
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.TopCenter,
            ) {
                val contentMaxWidth = if (compactVisualDensity) 620.dp else 1080.dp
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = contentMaxWidth)
                        .testTag("music.screen.root"),
                    state = listState,
                    contentPadding = PaddingValues(
                        start = SkydownUiTokens.screenHorizontalPadding,
                        top = 0.dp,
                        end = SkydownUiTokens.screenHorizontalPadding,
                        bottom = innerPadding.calculateBottomPadding() +
                            WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() +
                            SkydownUiTokens.screenBottomPadding + 24.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(sectionSpacing),
                ) {
                    item {
                        MusicOverviewCard(
                            uiState = uiState,
                            compactVisualDensity = compactVisualDensity,
                            topContentPadding = innerPadding.calculateTopPadding(),
                            onOpenStudio = onOpenStudio,
                            onOpenArtistPage = onOpenArtistPage?.let { openArtistPage ->
                                { openArtistPage(uiState.selectedArtist) }
                            },
                            onOpenSpotlight = {
                                coroutineScope.launch {
                                    listState.animateScrollToItem(spotlightSectionIndex)
                                }
                            },
                            onOpenArtistHub = {
                                coroutineScope.launch {
                                    listState.animateScrollToItem(artistSectionIndex)
                                }
                            },
                            onOpenTracks = {
                                coroutineScope.launch {
                                    listState.animateScrollToItem(tracksSectionIndex)
                                }
                            },
                            onOpenSpotifyStatus = {
                                coroutineScope.launch {
                                    listState.animateScrollToItem(spotifyStatusSectionIndex)
                                }
                            },
                        )
                    }

                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 2.dp),
                            ) {
                                MusicSpotlightDeckCard(
                                    uiState = uiState,
                                    selectedTrack = selectedTrack,
                                    artistPage = selectedArtistPage,
                                    isPreviewPlaying = selectedTrack?.trackId == uiState.currentlyPlayingId,
                                    onPlayPreview = {
                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        selectedTrack?.let(viewModel::togglePreview)
                                    },
                                    onOpenArtistPage = onOpenArtistPage?.let { openArtistPage ->
                                        { openArtistPage(uiState.selectedArtist) }
                                    },
                                    onOpenSpotify = {
                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        openTrackInSpotify(
                                            context = context,
                                            spotifyArtistId = selectedTrack?.spotifyArtistId,
                                            spotifyTrackId = selectedTrack?.spotifyTrackId,
                                            externalUrl = selectedTrack?.externalUrl,
                                        )
                                    },
                                )
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                        }
                    }

                    item {
                        ArtistPagerCard(
                            artists = uiState.availableArtists,
                            selectedArtist = uiState.selectedArtist,
                            artistPagesByName = artistPagesByName,
                            onArtistSelected = viewModel::selectArtist,
                            onOpenArtistPage = onOpenArtistPage,
                        )
                    }

                    item {
                        when {
                            uiState.isLoading -> {
                                MusicStatusCard(
                                    title = "Tracks werden vorbereitet",
                                    body = "Wir richten den Fokus fuer ${uiState.selectedArtist} ein.",
                                    loading = true,
                                )
                            }

                            !uiState.errorMessage.isNullOrBlank() -> {
                                MusicStatusCard(
                                    title = "Tracks gerade nicht erreichbar",
                                    body = "Der Katalog antwortet gerade nicht ruhig genug. Ein neuer Versuch reicht meist sofort.",
                                    actionLabel = "Erneut versuchen",
                                    onAction = {
                                        viewModel.clearSpotifyError()
                                        viewModel.selectArtist(uiState.selectedArtist)
                                    },
                                )
                            }

                            uiState.tracks.isEmpty() -> {
                                MusicStatusCard(
                                    title = "Noch keine Tracks im Fokus",
                                    body = "Fuer ${uiState.selectedArtist} ist gerade noch nichts live.",
                                )
                            }
                        }
                    }

                    if (uiState.tracks.isNotEmpty()) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 2.dp, bottom = 4.dp)
                                    .testTag("music.tracks.header"),
                            ) {
                                Text(
                                    text = "Catalog",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    text = queueContextText,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.64f),
                                )
                            }
                        }

                        itemsIndexed(uiState.tracks, key = { _, t -> t.trackId }) { index, track ->
                            val stepPresentation = if (index == 0) {
                                TrackRowPresentation.Featured
                            } else {
                                TrackRowPresentation.Catalog
                            }
                            TrackRow(
                                track = track,
                                isPlaying = uiState.currentlyPlayingId == track.trackId,
                                isSelected = selectedTrackId == track.trackId,
                                onSelectTrack = { selectedTrackId = track.trackId },
                                onPlayToggle = {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    viewModel.togglePreview(track)
                                },
                                presentation = stepPresentation,
                            )
                        }
                    }

                }
            }
        }
    }
}

@Composable
private fun MusicInstagramHubCard(
    destinations: List<MusicInstagramDestination>,
    onOpenLink: (String) -> Unit,
) {
    SkydownCard {
        BrandSectionBanner(
            title = "Instagram",
            subtitle = "Social-Links fuer den aktuellen Artist-Fokus.",
            accent = InstagramOrange,
            icon = Icons.Default.CameraAlt,
            tag = "SOCIAL",
        )

        Column(
            modifier = Modifier.padding(top = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            destinations.forEach { destination ->
                OutlinedButton(
                    onClick = { onOpenLink(destination.instagramUrl) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        com.nash.skyos.ui.theme.InstagramPurple,
                                        com.nash.skyos.ui.theme.InstagramPink,
                                        com.nash.skyos.ui.theme.InstagramOrange,
                                    ),
                                ),
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = null,
                            tint = androidx.compose.ui.graphics.Color.White,
                        )
                    }
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text(
                            text = destination.title,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = destination.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.66f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MusicShortcutHubCard(
    onOpenStudio: () -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    val studioAccent = colorScheme.skydownAccentMystic()
    SkydownCard {
        BrandSectionBanner(
            title = "Quick Access",
            subtitle = "Optionaler Schnellzugriff fuer Studio.",
            accent = studioAccent,
            icon = Icons.Default.AutoAwesome,
            tag = "LINKS",
        )

        Box(
            modifier = Modifier.padding(top = 14.dp),
        ) {
            MusicQuickAccessTile(
                title = "Studio",
                subtitle = "Record, Mix, Master",
                accent = studioAccent,
                icon = Icons.Default.AutoAwesome,
                onClick = onOpenStudio,
            )
        }
    }
}

@Composable
private fun MusicQuickAccessTile(
    title: String,
    subtitle: String,
    accent: Color,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(
                musicSurfaceBrush(
                    primaryAccent = accent,
                    secondaryAccent = SpotifyGreen,
                ),
            )
            .border(
                width = 1.dp,
                color = musicSurfaceBorderColor(accent),
                shape = RoundedCornerShape(20.dp),
            )
            .clickable(onClick = onClick)
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(accent.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accent,
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
            )
        }

        Icon(
            imageVector = Icons.Default.ArrowOutward,
            contentDescription = null,
            tint = accent,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

@Composable
private fun MusicPlayerCard(
    track: com.skydown.shared.model.Track?,
    isPlaying: Boolean,
    onPlayToggle: () -> Unit,
    onOpenSpotify: () -> Unit,
) {
    if (track == null) return

    val hasDirectSpotifyTrack = musicScreenResolvedSpotifyTrackId(track.spotifyTrackId, track.externalUrl) != null
    val hasSpotifyArtistLink = musicScreenResolvedSpotifyArtistId(track.spotifyArtistId, track.externalUrl) != null && !hasDirectSpotifyTrack
    val hasSpotifySearch = !track.externalUrl.isNullOrBlank() && !hasDirectSpotifyTrack && !hasSpotifyArtistLink
    val hasPreview = !track.previewUrl.isNullOrBlank()

    SkydownCard {
        val playerAccent = if (!track.spotifyTrackId.isNullOrBlank()) SpotifyGreen else MaterialTheme.colorScheme.primary

        BrandSectionBanner(
            title = "Now Playing",
            subtitle = "Preview und Handover bleiben ruhig im gleichen Listening-Flow.",
            accent = playerAccent,
            icon = Icons.Default.MusicNote,
            tag = "LIVE",
        )

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 14.dp),
        ) {
            val stackHero = maxWidth < 360.dp

            if (stackHero) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    MusicSpotlightArtwork(
                        imageUrl = track.artworkUrl100,
                        accent = playerAccent,
                        frameSize = 88.dp,
                        modifier = Modifier.size(88.dp),
                    )

                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = track.trackName,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = track.artistName ?: "22",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                        )
                        track.collectionName?.takeIf { it.isNotBlank() }?.let { album ->
                            Text(
                                text = album,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.64f),
                            )
                        }
                        Text(
                            text = when {
                                hasPreview && hasDirectSpotifyTrack -> "Preview laeuft direkt hier. Bei Bedarf geht es nahtlos zu Spotify weiter."
                                hasPreview -> "Der Track hat eine direkte Preview in der App."
                                hasDirectSpotifyTrack || hasSpotifyArtistLink || hasSpotifySearch ->
                                    "Gerade keine lokale Preview, aber Spotify bleibt direkt erreichbar."
                                else -> "Dieser Track liegt als Eintrag im aktuellen Artist-Hub bereit."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.64f),
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    MusicSpotlightArtwork(
                        imageUrl = track.artworkUrl100,
                        accent = playerAccent,
                        frameSize = 88.dp,
                        modifier = Modifier.size(88.dp),
                    )

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = track.trackName,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = track.artistName ?: "22",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                        )
                        track.collectionName?.takeIf { it.isNotBlank() }?.let { album ->
                            Text(
                                text = album,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.64f),
                            )
                        }
                        Text(
                            text = when {
                                hasPreview && hasDirectSpotifyTrack -> "Preview laeuft direkt hier. Bei Bedarf geht es nahtlos zu Spotify weiter."
                                hasPreview -> "Der Track hat eine direkte Preview in der App."
                                hasDirectSpotifyTrack || hasSpotifyArtistLink || hasSpotifySearch ->
                                    "Gerade keine lokale Preview, aber Spotify bleibt direkt erreichbar."
                                else -> "Dieser Track liegt als Eintrag im aktuellen Artist-Hub bereit."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.64f),
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .padding(top = 14.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            MusicBadge(
                text = if (hasPreview) "Preview" else "Warten auf Preview",
                imageVector = if (hasPreview) Icons.Default.PlayArrow else Icons.Default.Refresh,
                isActive = hasPreview,
                onClick = { if (hasPreview) onPlayToggle() },
            )
            MusicBadge(
                text = if (hasDirectSpotifyTrack || hasSpotifyArtistLink || hasSpotifySearch) "Spotify" else "Nur App",
                imageVector = Icons.Default.MusicNote,
                isActive = hasDirectSpotifyTrack || hasSpotifyArtistLink || hasSpotifySearch,
                onClick = {
                    if (hasDirectSpotifyTrack || hasSpotifyArtistLink || hasSpotifySearch) {
                        onOpenSpotify()
                    }
                },
            )
            MusicBadge(
                text = if (isPlaying) "Live" else "Ready",
                imageVector = if (isPlaying) Icons.Default.GraphicEq else Icons.Default.Sync,
                isActive = isPlaying,
                onClick = { if (hasPreview) onPlayToggle() },
            )
        }

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
        ) {
            val stackButtons = maxWidth < 360.dp

            if (stackButtons) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (hasPreview) {
                        BrandActionButton(
                            text = if (isPlaying) "Pause" else "Anhoeren",
                            onClick = onPlayToggle,
                            accent = playerAccent,
                            modifier = Modifier.fillMaxWidth(),
                            icon = if (isPlaying) Icons.Default.GraphicEq else Icons.Default.PlayArrow,
                            filled = isPlaying,
                        )
                    }

                    if (hasDirectSpotifyTrack) {
                        BrandActionButton(
                            text = "Spotify",
                            onClick = onOpenSpotify,
                            accent = SpotifyGreen,
                            modifier = Modifier.fillMaxWidth(),
                            icon = Icons.Default.MusicNote,
                        )
                    } else if (hasSpotifyArtistLink || hasSpotifySearch) {
                        BrandActionButton(
                            text = "Spotify",
                            onClick = onOpenSpotify,
                            accent = SpotifyGreen,
                            modifier = Modifier.fillMaxWidth(),
                            icon = Icons.Default.MusicNote,
                            filled = false,
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (hasPreview) {
                        BrandActionButton(
                            text = if (isPlaying) "Pause" else "Anhoeren",
                            onClick = onPlayToggle,
                            accent = playerAccent,
                            modifier = Modifier.weight(1f),
                            icon = if (isPlaying) Icons.Default.GraphicEq else Icons.Default.PlayArrow,
                            filled = isPlaying,
                        )
                    }

                    if (hasDirectSpotifyTrack) {
                        BrandActionButton(
                            text = "Spotify",
                            onClick = onOpenSpotify,
                            accent = SpotifyGreen,
                            modifier = if (hasPreview) Modifier.weight(1f) else Modifier.fillMaxWidth(),
                            icon = Icons.Default.MusicNote,
                        )
                    } else if (hasSpotifyArtistLink || hasSpotifySearch) {
                        BrandActionButton(
                            text = "Spotify",
                            onClick = onOpenSpotify,
                            accent = SpotifyGreen,
                            modifier = if (hasPreview) Modifier.weight(1f) else Modifier.fillMaxWidth(),
                            icon = Icons.Default.MusicNote,
                            filled = false,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MusicOverviewCard(
    uiState: MusicUiState,
    compactVisualDensity: Boolean,
    topContentPadding: Dp = 0.dp,
    onOpenStudio: (() -> Unit)?,
    onOpenArtistPage: (() -> Unit)?,
    onOpenSpotlight: () -> Unit,
    onOpenArtistHub: () -> Unit,
    onOpenTracks: () -> Unit,
    onOpenSpotifyStatus: () -> Unit,
) {
    val screenHeaderSettings by AppContainer.screenHeaderSettingsRepository.settings.collectAsStateWithLifecycle()
    val socialProfile = uiState.selectedArtistSocialProfile
    val trackLabel = musicTrackLabel(uiState)
    val colorScheme = MaterialTheme.colorScheme
    val beatAccent = colorScheme.skydownAccent()
    val statusAccent = colorScheme.skydownAccentMystic()
    BrandHeroCard(
        eyebrow = screenHeaderSettings.musicHubEyebrow.ifBlank { "SkyOS" },
        title = screenHeaderSettings.musicHubTitle.ifBlank { "Music" },
        subtitle = screenHeaderSettings.musicHubSubtitle.ifBlank {
            "Ueberblick vorweg – Artist, Drop und Track-Status im Griff."
        },
        detail = screenHeaderSettings.musicHubDetail.ifBlank {
            "${uiState.selectedArtist} · Die folgenden Bereiche fassen Spotlight, Katalog und Status zusammen."
        },
        backgroundImageUrl = screenHeaderSettings.musicHubImageUrl.ifBlank { null },
        accent = SpotifyGreen,
        secondaryAccent = beatAccent,
        marks = listOf(BrandArtwork.Zweizwei),
        compactVisualDensity = compactVisualDensity,
        edgeToEdge = true,
        topContentPadding = topContentPadding,
        onSurfaceClick = onOpenStudio ?: onOpenTracks,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                BrandPill(
                    text = uiState.selectedArtist,
                    tint = SpotifyGreen,
                    onClick = onOpenArtistHub,
                )
                BrandPill(
                    text = trackLabel,
                    tint = beatAccent,
                    onClick = onOpenTracks,
                )
                onOpenArtistPage?.let { openArtistPage ->
                    BrandPill(
                        text = "Artist Page",
                        tint = beatAccent,
                        onClick = openArtistPage,
                    )
                }
                BrandPill(
                    text = if (uiState.isSpotifyConnected) "Spotify live" else "Preview ready",
                    tint = statusAccent,
                    onClick = onOpenSpotifyStatus,
                )
            }
            socialProfile?.handle?.takeIf { it.isNotBlank() }?.let { handle ->
                BrandPill(
                    text = handle,
                    tint = SpotifyGreen,
                    onClick = onOpenArtistHub,
                )
            }
        }
    }
}

private fun musicTrackLabel(uiState: MusicUiState): String {
    return when {
        uiState.isLoading -> "Katalog wird geladen"
        uiState.tracks.isEmpty() -> "Noch keine Tracks"
        uiState.tracks.size == 1 -> "1 Track"
        else -> "${uiState.tracks.size} Tracks"
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
private fun MusicSpotlightDeckCard(
    uiState: MusicUiState,
    selectedTrack: com.skydown.shared.model.Track?,
    artistPage: com.nash.skyos.data.ArtistPageUi,
    isPreviewPlaying: Boolean,
    onPlayPreview: () -> Unit,
    onOpenArtistPage: (() -> Unit)?,
    onOpenSpotify: () -> Unit,
) {
    val heroImage = artistPage.heroImageURL ?: selectedTrack?.artworkUrl100
    val colorScheme = MaterialTheme.colorScheme
    val accentFallback = colorScheme.skydownAccent()
    val hasPreview = !selectedTrack?.previewUrl.isNullOrBlank()
    val hasSpotifyTrack = selectedTrack != null &&
        musicScreenResolvedSpotifyTrackId(selectedTrack.spotifyTrackId, selectedTrack.externalUrl) != null
    val hasSpotifyArtistLink = selectedTrack != null &&
        musicScreenResolvedSpotifyArtistId(selectedTrack.spotifyArtistId, selectedTrack.externalUrl) != null &&
        !hasSpotifyTrack
    val hasSpotifySearch = selectedTrack?.externalUrl?.isNotBlank() == true &&
        !hasSpotifyTrack &&
        !hasSpotifyArtistLink
    val spotlightArt = 144.dp
    SkydownCard(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 18.dp),
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize()
        ) {
            val stackHero = maxWidth < 380.dp

            if (stackHero) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    MusicSpotlightArtwork(
                        imageUrl = heroImage,
                        accent = if (uiState.isSpotifyConnected) SpotifyGreen else accentFallback,
                        frameSize = spotlightArt,
                        modifier = Modifier.size(spotlightArt),
                    )

                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "Im Fokus",
                                style = MaterialTheme.typography.labelSmall,
                                color = SpotifyGreen,
                                fontWeight = FontWeight.Bold,
                            )
                            if (uiState.isSpotifyConnected) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(999.dp))
                                        .background(MaterialTheme.colorScheme.primary)
                                        .padding(horizontal = 10.dp, vertical = 5.dp),
                                ) {
                                    Text(
                                        text = "Live",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            }
                        }
                        Text(
                            text = selectedTrack?.trackName ?: uiState.selectedArtist,
                            style = SkydownCardTitleTextStyle,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = artistPage.tagline ?: selectedTrack?.artistName ?: uiState.selectedArtist,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.76f),
                        )
                        Text(
                            text = artistPage.bio
                                ?: "Der aktuelle Fokus verbindet Artist-Page, Preview und Spotify direkt in einem Hub.",
                            style = SkydownEditorialCaptionTextStyle,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.64f),
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    MusicSpotlightArtwork(
                        imageUrl = heroImage,
                        accent = if (uiState.isSpotifyConnected) SpotifyGreen else accentFallback,
                        frameSize = spotlightArt,
                        modifier = Modifier.size(spotlightArt),
                    )

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "Im Fokus",
                                style = MaterialTheme.typography.labelSmall,
                                color = SpotifyGreen,
                                fontWeight = FontWeight.Bold,
                            )
                            if (uiState.isSpotifyConnected) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(999.dp))
                                        .background(MaterialTheme.colorScheme.primary)
                                        .padding(horizontal = 10.dp, vertical = 5.dp),
                                ) {
                                    Text(
                                        text = "Live",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            }
                        }
                        Text(
                            text = selectedTrack?.trackName ?: uiState.selectedArtist,
                            style = SkydownCardTitleTextStyle,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = artistPage.tagline ?: selectedTrack?.artistName ?: uiState.selectedArtist,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.76f),
                        )
                        Text(
                            text = artistPage.bio
                                ?: "Der aktuelle Fokus verbindet Artist-Page, Preview und Spotify direkt in einem Hub.",
                            style = SkydownEditorialCaptionTextStyle,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.64f),
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }

        Text(
            text = if (!selectedTrack?.collectionName.isNullOrBlank()) {
                "Aus ${selectedTrack.collectionName.orEmpty()} · zuerst hoeren, dann in Spotify oeffnen."
            } else {
                "Direkt hoeren, weiter in Spotify, ohne den Ablauf zu brechen."
            },
            style = SkydownEditorialCaptionTextStyle,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
            modifier = Modifier.padding(top = 14.dp),
        )

        if (hasPreview || onOpenArtistPage != null) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp),
            ) {
                val stackButtons = maxWidth < 360.dp

                if (stackButtons) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (hasPreview) {
                            BrandActionButton(
                                text = if (isPreviewPlaying) "Preview pausieren" else "Preview anhoeren",
                                onClick = onPlayPreview,
                                accent = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.fillMaxWidth(),
                                icon = if (isPreviewPlaying) Icons.Default.GraphicEq else Icons.Default.PlayArrow,
                            )
                        }

                        onOpenArtistPage?.let { openArtistPage ->
                            BrandActionButton(
                                text = "Artist Page",
                                onClick = openArtistPage,
                                accent = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.fillMaxWidth(),
                                icon = Icons.Default.AutoAwesome,
                                filled = false,
                            )
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        if (hasPreview) {
                            BrandActionButton(
                                text = if (isPreviewPlaying) "Preview pausieren" else "Preview anhoeren",
                                onClick = onPlayPreview,
                                accent = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.weight(1f),
                                icon = if (isPreviewPlaying) Icons.Default.GraphicEq else Icons.Default.PlayArrow,
                            )
                        }

                        onOpenArtistPage?.let { openArtistPage ->
                            BrandActionButton(
                                text = "Artist Page",
                                onClick = openArtistPage,
                                accent = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.weight(1f),
                                icon = Icons.Default.AutoAwesome,
                                filled = false,
                            )
                        }
                    }
                }
            }
        }

        if (hasSpotifyTrack || hasSpotifyArtistLink || hasSpotifySearch) {
            BrandActionButton(
                text = when {
                    hasSpotifyTrack -> "Spotify Player"
                    hasSpotifyArtistLink -> "Spotify Artist"
                    else -> "Spotify Suche"
                },
                onClick = onOpenSpotify,
                accent = SpotifyGreen,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                icon = if (hasSpotifyTrack) Icons.Default.MusicNote else Icons.Default.ArrowOutward,
                filled = false,
            )
        }
    }
}

@Composable
private fun musicSurfaceBrush(
    primaryAccent: Color,
    secondaryAccent: Color? = null,
): Brush {
    val colorScheme = MaterialTheme.colorScheme
    val isDarkPalette = colorScheme.skydownIsDarkPalette()
    val resolvedSecondaryAccent = secondaryAccent ?: colorScheme.skydownAccentHighlight()
    return Brush.linearGradient(
        colors = listOf(
            colorScheme.skydownCardBackground().copy(alpha = if (isDarkPalette) 0.98f else 0.995f),
            colorScheme.skydownSecondaryBackground().copy(alpha = if (isDarkPalette) 0.78f else 0.64f),
            primaryAccent.copy(alpha = if (isDarkPalette) 0.16f else 0.10f),
            resolvedSecondaryAccent.copy(alpha = if (isDarkPalette) 0.10f else 0.06f),
        ),
    )
}

@Composable
private fun musicSurfaceBorderColor(accent: Color): Color {
    val isDarkPalette = MaterialTheme.colorScheme.skydownIsDarkPalette()
    return accent.copy(alpha = if (isDarkPalette) 0.22f else 0.14f)
}

@Composable
private fun MusicSpotlightArtwork(
    imageUrl: String?,
    accent: Color,
    modifier: Modifier = Modifier,
    frameSize: androidx.compose.ui.unit.Dp = 110.dp,
) {
    val colorScheme = MaterialTheme.colorScheme
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        listOf(1f, 0.80f, 0.58f).forEachIndexed { index, scale ->
            Box(
                modifier = Modifier
                    .size(frameSize * scale)
                    .border(
                        width = 1.dp,
                        color = accent.copy(alpha = 0.28f - (index * 0.05f)),
                        shape = CircleShape,
                    ),
            )
        }

        Box(
            modifier = Modifier
                .size(frameSize * 0.78f)
                .clip(RoundedCornerShape(26.dp))
                .background(
                    musicSurfaceBrush(
                        primaryAccent = accent,
                        secondaryAccent = colorScheme.skydownAccentHighlight(),
                    ),
                )
                .border(
                    width = 1.dp,
                    color = musicSurfaceBorderColor(accent),
                    shape = RoundedCornerShape(26.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (!imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(25.dp)),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(34.dp),
                )
            }
        }
    }
}

@Composable
private fun MusicDeckMetricCard(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(
                musicSurfaceBrush(
                    primaryAccent = accent,
                    secondaryAccent = SpotifyGreen,
                ),
            )
            .border(
                width = 1.dp,
                color = musicSurfaceBorderColor(accent),
                shape = RoundedCornerShape(18.dp),
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = accent.copy(alpha = 0.82f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ArtistPagerCard(
    artists: List<String>,
    selectedArtist: String,
    artistPagesByName: Map<String, com.nash.skyos.data.ArtistPageUi>,
    onArtistSelected: (String) -> Unit,
    onOpenArtistPage: ((String) -> Unit)?,
) {
    val safeArtists = artists.ifEmpty { listOf(selectedArtist) }
    val initialPage = safeArtists.indexOf(selectedArtist).takeIf { it >= 0 } ?: 0
    val pagerState = rememberPagerState(initialPage = initialPage) { safeArtists.size }
    val colorScheme = MaterialTheme.colorScheme
    val artistAccent = colorScheme.skydownAccentHighlight()

    LaunchedEffect(selectedArtist, safeArtists) {
        val targetPage = safeArtists.indexOf(selectedArtist).takeIf { it >= 0 } ?: 0
        if (targetPage != pagerState.currentPage) {
            pagerState.animateScrollToPage(targetPage)
        }
    }

    LaunchedEffect(pagerState.settledPage, safeArtists) {
        safeArtists.getOrNull(pagerState.settledPage)?.let { artist ->
            if (artist != selectedArtist) {
                onArtistSelected(artist)
            }
        }
    }

    SkydownCard {
        BrandSectionBanner(
            title = "Artists",
            subtitle = "Alle Artists sind direkt anwaehlbar. Nutze die Schnellwahl oder swipe unten durch den Showcase.",
            accent = artistAccent,
            icon = Icons.Default.AutoAwesome,
            tag = "ARTISTS",
        )
        ArtistMapSection(
            artists = safeArtists,
            selectedArtist = selectedArtist,
            artistPagesByName = artistPagesByName,
            onArtistSelected = onArtistSelected,
        )
        Text(
            text = "Schnellwahl",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 12.dp),
        )
        Column(
            modifier = Modifier.padding(top = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            safeArtists.forEach { artist ->
                val page = artistPagesByName[artist] ?: ArtistPagesStore.pageFor(ArtistPageBrand.Zweizwei, artist)
                ArtistOverviewRow(
                    artist = artist,
                    page = page,
                    isSelected = artist == selectedArtist,
                    onSelect = { onArtistSelected(artist) },
                    onOpenArtistPage = onOpenArtistPage?.let { { it(artist) } },
                )
            }
        }
        Text(
            text = "Swipe Showcase",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 16.dp),
        )
        Text(
            text = "Fuer einen schnellen visuellen Durchlauf mit grosser Vorschau.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f),
            modifier = Modifier.padding(top = 4.dp),
        )
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            pageSpacing = 12.dp,
        ) { pageIndex ->
            val artist = safeArtists[pageIndex]
            val page = artistPagesByName[artist] ?: ArtistPagesStore.pageFor(ArtistPageBrand.Zweizwei, artist)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        musicSurfaceBrush(
                            primaryAccent = artistAccent,
                            secondaryAccent = SpotifyGreen,
                        ),
                    )
                    .border(
                        width = 1.dp,
                        color = musicSurfaceBorderColor(artistAccent),
                        shape = RoundedCornerShape(24.dp),
                    )
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(168.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            musicSurfaceBrush(
                                primaryAccent = artistAccent,
                                secondaryAccent = SpotifyGreen,
                            ),
                        ),
                ) {
                    if (!page.heroImageURL.isNullOrBlank()) {
                        AsyncImage(
                            model = page.heroImageURL,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.18f)),
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Black.copy(alpha = 0.08f),
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.36f),
                                    ),
                                ),
                            ),
                    )

                    Column(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = artist,
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                        )
                        Text(
                            text = page.tagline ?: "22 Artist",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.78f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }

                    if (page.hasCustomPresentation) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(14.dp)
                                .clip(CircleShape)
                                .background(artistAccent)
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                        ) {
                            Text(
                                text = "Live",
                                style = MaterialTheme.typography.labelMedium,
                                color = colorScheme.onTertiary,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(14.dp)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        BrandPill(text = "Artist Hub", tint = artistAccent)
                        BrandPill(
                            text = if (!page.spotifyURL.isNullOrBlank()) "Spotify" else "Direct",
                            tint = SpotifyGreen,
                        )
                    }
                }

                Text(
                    text = page.bio ?: "$artist ist in Sky OS angelegt. Story, Visuals und Links koennen hier live geschaltet werden.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )

                BoxWithConstraints {
                    val stackMetrics = maxWidth < 360.dp

                    if (stackMetrics) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            MusicDeckMetricCard(
                                label = "Tag",
                                value = page.tagline ?: "Artist ready",
                                accent = artistAccent,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            MusicDeckMetricCard(
                                label = "Reach",
                                value = buildString {
                                    append(if (!page.instagramURL.isNullOrBlank()) "IG" else "Feed")
                                    append(" / ")
                                    append(if (!page.youtubeURL.isNullOrBlank()) "YT" else "Page")
                                },
                                accent = SpotifyGreen,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    } else {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            MusicDeckMetricCard(
                                label = "Tag",
                                value = page.tagline ?: "Artist ready",
                                accent = artistAccent,
                                modifier = Modifier.weight(1f),
                            )
                            MusicDeckMetricCard(
                                label = "Reach",
                                value = buildString {
                                    append(if (!page.instagramURL.isNullOrBlank()) "IG" else "Feed")
                                    append(" / ")
                                    append(if (!page.youtubeURL.isNullOrBlank()) "YT" else "Page")
                                },
                                accent = SpotifyGreen,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (!page.spotifyURL.isNullOrBlank()) {
                        SmallMusicBadge(
                            text = "Spotify",
                            isAccent = true,
                            onClick = { onOpenArtistPage?.invoke(artist) },
                        )
                    } else {
                        SmallMusicBadge(
                            text = "Page",
                            isAccent = true,
                            onClick = { onOpenArtistPage?.invoke(artist) },
                        )
                    }
                    if (!page.instagramURL.isNullOrBlank()) {
                        SmallMusicBadge(
                            text = "Instagram",
                            isAccent = false,
                            onClick = { onOpenArtistPage?.invoke(artist) },
                        )
                    }
                    if (!page.youtubeURL.isNullOrBlank()) {
                        SmallMusicBadge(
                            text = "YouTube",
                            isAccent = false,
                            onClick = { onOpenArtistPage?.invoke(artist) },
                        )
                    }
                }

                BrandActionButton(
                    text = "$artist entdecken",
                    onClick = { onOpenArtistPage?.invoke(artist) },
                    accent = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxWidth(),
                    icon = Icons.Default.AutoAwesome,
                    enabled = onOpenArtistPage != null,
                )
            }
        }

        if (safeArtists.size > 1) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.Center,
            ) {
                safeArtists.forEachIndexed { index, _ ->
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 3.dp)
                            .size(if (pagerState.currentPage == index) 10.dp else 8.dp)
                            .clip(CircleShape)
                            .background(
                                if (pagerState.currentPage == index) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.18f)
                                },
                            ),
                    )
                }
            }
        }
    }
}

@Composable
private fun ArtistMapSection(
    artists: List<String>,
    selectedArtist: String,
    artistPagesByName: Map<String, com.nash.skyos.data.ArtistPageUi>,
    onArtistSelected: (String) -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    val artistAccent = colorScheme.skydownAccent()
    val connectedAccent = colorScheme.skydownAccentHighlight()
    val liveCount = artists.count { artist ->
        (artistPagesByName[artist] ?: ArtistPagesStore.pageFor(ArtistPageBrand.Zweizwei, artist)).hasCustomPresentation
    }
    val connectedCount = artists.count { artist ->
        artistPageReachCount(artistPagesByName[artist] ?: ArtistPagesStore.pageFor(ArtistPageBrand.Zweizwei, artist)) > 1
    }

    Column(
        modifier = Modifier.padding(top = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "Artist Map",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = "Alle Artists auf einen Blick mit Fokus, Reach und Status. Ein Tap setzt den Artist sofort in den Fokus.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f),
        )
        BoxWithConstraints {
            val wideMetrics = maxWidth >= 420.dp
            if (wideMetrics) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ArtistMetricCard(
                        title = "Artists",
                        value = artists.size.toString(),
                        accent = artistAccent,
                        modifier = Modifier.weight(1f),
                    )
                    ArtistMetricCard(
                        title = "Live Pages",
                        value = liveCount.toString(),
                        accent = SpotifyGreen,
                        modifier = Modifier.weight(1f),
                    )
                    ArtistMetricCard(
                        title = "Connected",
                        value = connectedCount.toString(),
                        accent = connectedAccent,
                        modifier = Modifier.weight(1f),
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    ArtistMetricCard(
                        title = "Artists",
                        value = artists.size.toString(),
                        accent = artistAccent,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    ArtistMetricCard(
                        title = "Live Pages",
                        value = liveCount.toString(),
                        accent = SpotifyGreen,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    ArtistMetricCard(
                        title = "Connected",
                        value = connectedCount.toString(),
                        accent = connectedAccent,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
        BoxWithConstraints {
            val columns = if (maxWidth >= 520.dp) 3 else 2
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                artists.chunked(columns).forEach { rowArtists ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        rowArtists.forEach { artist ->
                            val page = artistPagesByName[artist] ?: ArtistPagesStore.pageFor(ArtistPageBrand.Zweizwei, artist)
                            ArtistSignalTile(
                                artist = artist,
                                page = page,
                                isSelected = artist == selectedArtist,
                                onClick = { onArtistSelected(artist) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        repeat(columns - rowArtists.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ArtistMetricCard(
    title: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    val colorScheme = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(
                musicSurfaceBrush(
                    primaryAccent = accent,
                    secondaryAccent = colorScheme.skydownAccent(),
                ),
            )
            .border(
                width = 1.dp,
                color = musicSurfaceBorderColor(accent),
                shape = RoundedCornerShape(18.dp),
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = accent.copy(alpha = 0.82f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Black,
        )
    }
}

@Composable
private fun ArtistSignalTile(
    artist: String,
    page: com.nash.skyos.data.ArtistPageUi,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colorScheme = MaterialTheme.colorScheme
    val accent = if (isSelected) SpotifyGreen else colorScheme.skydownAccentHighlight()
    val statusLabel = when {
        isSelected -> "Im Fokus"
        page.hasCustomPresentation -> "Live"
        else -> "Page"
    }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(
                musicSurfaceBrush(
                    primaryAccent = accent,
                    secondaryAccent = colorScheme.skydownAccent(),
                ),
            )
            .border(
                width = 1.dp,
                color = musicSurfaceBorderColor(accent),
                shape = RoundedCornerShape(20.dp),
            )
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = artist,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = page.tagline ?: "22 Artist",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            BrandStatusChip(
                text = statusLabel,
                accent = accent,
                isActive = true,
            )
        }

        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            BrandPill(
                text = artistReachSummary(page),
                tint = MaterialTheme.colorScheme.primary,
            )
            BrandPill(
                text = if (page.hasCustomPresentation) "Custom Page" else "Direkter Einstieg",
                tint = accent,
            )
        }

        Text(
            text = if (isSelected) "Aktuell im Fokus" else "Tippen zum Fokussieren",
            style = MaterialTheme.typography.labelMedium,
            color = accent.copy(alpha = 0.92f),
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun ArtistOverviewRow(
    artist: String,
    page: com.nash.skyos.data.ArtistPageUi,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onOpenArtistPage: (() -> Unit)?,
) {
    val colorScheme = MaterialTheme.colorScheme
    val accent = colorScheme.skydownAccent()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                musicSurfaceBrush(
                    primaryAccent = accent,
                    secondaryAccent = SpotifyGreen,
                ),
            )
            .border(
                width = 1.dp,
                color = musicSurfaceBorderColor(accent),
                shape = RoundedCornerShape(20.dp),
            )
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = artist,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = page.tagline ?: "22 Artist",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.70f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            if (isSelected) {
                BrandPill(text = "Im Fokus", tint = SpotifyGreen)
            } else if (page.hasCustomPresentation) {
                BrandPill(text = "Live", tint = colorScheme.skydownAccentHighlight())
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Button(
                onClick = onSelect,
                modifier = Modifier.weight(1f),
            ) {
                Text(if (isSelected) "Im Fokus" else "Artist waehlen")
            }

            OutlinedButton(
                onClick = { onOpenArtistPage?.invoke() },
                modifier = Modifier.weight(1f),
                enabled = onOpenArtistPage != null,
            ) {
                Text("Page oeffnen")
            }
        }
    }
}

@Composable
private fun SmallMusicBadge(
    text: String,
    isAccent: Boolean,
    onClick: () -> Unit = {},
) {
    BrandStatusChip(
        text = text,
        accent = SpotifyGreen,
        isActive = isAccent,
        onClick = onClick,
    )
}

private fun artistPageReachCount(page: com.nash.skyos.data.ArtistPageUi): Int {
    return listOf(page.spotifyURL, page.instagramURL, page.youtubeURL).count { !it.isNullOrBlank() }
}

private fun artistReachSummary(page: com.nash.skyos.data.ArtistPageUi): String {
    val channels = buildList {
        if (!page.spotifyURL.isNullOrBlank()) add("Spotify")
        if (!page.instagramURL.isNullOrBlank()) add("Instagram")
        if (!page.youtubeURL.isNullOrBlank()) add("YouTube")
    }
    return if (channels.isEmpty()) "Noch keine Links" else channels.take(2).joinToString(" • ")
}

private fun musicScreenResolvedSpotifyTrackId(
    spotifyTrackId: String?,
    externalUrl: String?,
): String? {
    if (!spotifyTrackId.isNullOrBlank()) return spotifyTrackId
    if (externalUrl.isNullOrBlank()) return null
    val marker = "/track/"
    val start = externalUrl.indexOf(marker)
    if (start == -1) return null
    return externalUrl
        .substring(start + marker.length)
        .substringBefore("?")
        .substringBefore("/")
        .takeIf { it.isNotBlank() }
}

private fun musicScreenResolvedSpotifyArtistId(
    spotifyArtistId: String?,
    externalUrl: String?,
): String? {
    if (!spotifyArtistId.isNullOrBlank()) return spotifyArtistId
    if (externalUrl.isNullOrBlank()) return null
    val marker = "/artist/"
    val start = externalUrl.indexOf(marker)
    if (start == -1) return null
    return externalUrl
        .substring(start + marker.length)
        .substringBefore("?")
        .substringBefore("/")
        .takeIf { it.isNotBlank() }
}

@Composable
private fun SpotifyConnectCard(
    onConnect: () -> Unit,
) {
    SkydownCard {
        BrandSectionBanner(
            title = "Spotify",
            subtitle = "Previews laufen direkt in der App. Mit Spotify kannst du kompatible Tracks zusaetzlich im In-App-Player oeffnen.",
            accent = SpotifyGreen,
            icon = Icons.Default.MusicNote,
            tag = "SPOTIFY",
        )
        BrandActionButton(
            text = "Spotify optional verbinden",
            onClick = onConnect,
            accent = SpotifyGreen,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            icon = Icons.Default.MusicNote,
        )
    }
}

@Composable
private fun MusicStatusCard(
    title: String,
    body: String,
    loading: Boolean = false,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    val statusAccent = if (loading) MaterialTheme.colorScheme.skydownAccentMystic() else SpotifyGreen
    SkydownCard {
        BrandSectionBanner(
            title = title,
            accent = statusAccent,
            icon = if (loading) Icons.Default.Sync else Icons.Default.MusicNote,
            tag = "STATUS",
        )
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
            modifier = Modifier.padding(top = 8.dp),
        )

        if (loading) {
            Row(
                modifier = Modifier.padding(top = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    strokeWidth = 2.5.dp,
                )
                Text(
                    text = "Wird ruhig geladen ...",
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }

        if (actionLabel != null && onAction != null) {
            Spacer(modifier = Modifier.height(16.dp))
            BrandActionButton(
                text = actionLabel,
                onClick = onAction,
                accent = statusAccent,
                modifier = Modifier.fillMaxWidth(),
                icon = if (loading) Icons.Default.Sync else Icons.Default.Refresh,
                filled = false,
            )
        }
    }
}


@Composable
private fun MusicBadge(
    text: String,
    imageVector: ImageVector,
    isActive: Boolean,
    onClick: () -> Unit = {},
) {
    BrandStatusChip(
        text = text,
        accent = SpotifyGreen,
        icon = imageVector,
        isActive = isActive,
        onClick = onClick,
    )
}
