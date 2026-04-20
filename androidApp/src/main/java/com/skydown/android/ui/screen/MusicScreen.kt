package com.skydown.android.ui.screen

import android.content.Intent
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.skydown.android.data.mediaAttributionContext
import com.skydown.android.data.ArtistPageBrand
import com.skydown.android.data.ArtistPagesStore
import com.skydown.android.data.AppContainer
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.skydown.android.data.SpotifyAuthManager
import com.skydown.android.ui.component.AppTopBarSessionActions
import com.skydown.android.ui.component.BrandActionButton
import com.skydown.android.ui.component.BrandArtwork
import com.skydown.android.ui.component.BrandHeroCard
import com.skydown.android.ui.component.BrandHeroMetricCard
import com.skydown.android.ui.component.BrandSectionBanner
import com.skydown.android.ui.component.BrandStatusChip
import com.skydown.android.ui.component.BrandPill
import com.skydown.android.ui.component.SkydownCard
import com.skydown.android.ui.component.SkydownTopBarTitle
import com.skydown.android.ui.component.TrackRow
import com.skydown.android.ui.component.rememberUsesCompactVisualDensity
import com.skydown.android.ui.component.skydownContentPadding
import com.skydown.android.ui.component.skydownScreenBrush
import com.skydown.android.ui.component.skydownTopBarColors
import com.skydown.android.ui.component.openTrackInSpotify
import com.skydown.android.ui.model.MusicUiState
import com.skydown.android.ui.model.MusicInstagramDestination
import com.skydown.android.ui.theme.ArenaGold
import com.skydown.android.ui.theme.DexBlueDeep
import com.skydown.android.ui.theme.FieldMint
import com.skydown.android.ui.theme.InstagramOrange
import com.skydown.android.ui.theme.SpotifyGreen
import com.skydown.android.ui.viewmodel.MusicViewModel
import coil3.compose.AsyncImage
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MusicScreen(
    onBack: (() -> Unit)? = null,
    onOpenBeatHub: (() -> Unit)? = null,
    onOpenStudio: (() -> Unit)? = null,
    initialArtist: String? = null,
    initialTrackId: Int? = null,
    autoplaySelectedTrackPreview: Boolean = false,
    autoOpenSelectedTrackInSpotify: Boolean = false,
    onOpenCart: (() -> Unit)? = null,
    onOpenProfile: (() -> Unit)? = null,
    onOpenSettings: (() -> Unit)? = null,
    onOpenArtistPage: ((String) -> Unit)? = null,
    viewModel: MusicViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val artistPages by ArtistPagesStore.pages.collectAsState()

    val context = LocalContext.current
    val mediaContext = remember(context) { context.mediaAttributionContext() }
    val player = remember(mediaContext) {
        ExoPlayer.Builder(mediaContext).build().apply {
            playWhenReady = true
        }
    }
    var selectedTrackId by rememberSaveable { mutableStateOf<Int?>(null) }
    var hasHandledInitialSelection by rememberSaveable { mutableStateOf(false) }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val selectedTrack = uiState.tracks.firstOrNull { it.trackId == selectedTrackId } ?: uiState.tracks.firstOrNull()
    val hasShortcutHub = onOpenBeatHub != null || onOpenStudio != null
    val compactVisualDensity = rememberUsesCompactVisualDensity()
    val artistSectionIndex = if (hasShortcutHub) 3 else 2
    val spotifyStatusSectionIndex = if (hasShortcutHub) 5 else 4
    val tracksSectionIndex = if (uiState.tracks.isNotEmpty()) {
        if (hasShortcutHub) 7 else 6
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
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    SkydownTopBarTitle(
                        title = "Music",
                        subtitle = "Artists, Tracks, Spotify.",
                    )
                },
                actions = {
                    if (onOpenSettings != null) {
                        AppTopBarSessionActions(
                            onOpenCart = onOpenCart,
                            onOpenProfile = onOpenProfile,
                            onOpenSettings = onOpenSettings,
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
                scrollBehavior = scrollBehavior,
            )
        },
        floatingActionButton = {
            if (onOpenBeatHub != null || onOpenStudio != null) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.End,
                ) {
                    onOpenStudio?.let { openStudio ->
                        ExtendedFloatingActionButton(
                            onClick = openStudio,
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                            )
                            Text(
                                text = "Studio",
                                modifier = Modifier.padding(start = 8.dp),
                            )
                        }
                    }

                    onOpenBeatHub?.let { openBeatHub ->
                        ExtendedFloatingActionButton(
                            onClick = openBeatHub,
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        ) {
                            Icon(
                                imageVector = Icons.Default.GraphicEq,
                                contentDescription = null,
                            )
                            Text(
                                text = "Beat Hub",
                                modifier = Modifier.padding(start = 8.dp),
                            )
                        }
                    }
                }
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    skydownScreenBrush(
                        primaryColor = SpotifyGreen,
                        secondaryColor = ArenaGold,
                        primaryAlpha = 0.075f,
                        secondaryAlpha = 0.050f,
                    ),
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
                        start = 16.dp,
                        top = innerPadding.calculateTopPadding() + 10.dp,
                        end = 16.dp,
                        bottom = innerPadding.calculateBottomPadding() + 10.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    item {
                        MusicOverviewCard(
                            uiState = uiState,
                            compactVisualDensity = compactVisualDensity,
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
                        MusicSpotlightDeckCard(
                            uiState = uiState,
                            selectedTrack = selectedTrack,
                            artistPage = selectedArtistPage,
                            onOpenInstagram = {
                                uiState.selectedArtistSocialProfile?.let { socialProfile ->
                                    openExternalLink(context, socialProfile.instagramUrl)
                                }
                            },
                            onConnect = {
                                viewModel.clearSpotifyError()
                                openExternalIntent(
                                    context = context,
                                    intent = Intent(Intent.ACTION_VIEW, SpotifyAuthManager.buildAuthorizationUri()),
                                    missingMessage = "Spotify konnte nicht gestartet werden.",
                                )
                            },
                            onDisconnect = {
                                player.stop()
                                player.clearMediaItems()
                                viewModel.disconnectSpotify()
                            },
                            onOpenArtistPage = {
                                onOpenArtistPage?.invoke(uiState.selectedArtist)
                            },
                        )
                    }

                    if (onOpenBeatHub != null || onOpenStudio != null) {
                        item {
                            MusicShortcutHubCard(
                                onOpenStudio = onOpenStudio,
                                onOpenBeatHub = onOpenBeatHub,
                            )
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
                        MusicInstagramHubCard(
                            destinations = uiState.instagramHubDestinations,
                            onOpenLink = { url ->
                                openExternalLink(context, url)
                            },
                        )
                    }

                    item {
                        when {
                            uiState.isLoading -> {
                                MusicStatusCard(
                                    title = "Tracks laden",
                                    body = uiState.selectedArtist,
                                    loading = true,
                                )
                            }

                            !uiState.errorMessage.isNullOrBlank() -> {
                                MusicStatusCard(
                                    title = "Tracks fehlen",
                                    body = uiState.errorMessage.orEmpty(),
                                    actionLabel = "Erneut laden",
                                    onAction = {
                                        viewModel.clearSpotifyError()
                                        viewModel.selectArtist(uiState.selectedArtist)
                                    },
                                )
                            }

                            uiState.tracks.isEmpty() -> {
                                MusicStatusCard(
                                    title = "Keine Tracks",
                                    body = uiState.selectedArtist,
                                )
                            }
                        }
                    }

                    if (uiState.tracks.isNotEmpty()) {
                        item {
                            MusicPlayerCard(
                                track = selectedTrack,
                                isPlaying = selectedTrack?.trackId == uiState.currentlyPlayingId,
                                onPlayToggle = {
                                    selectedTrack?.let { viewModel.togglePreview(it) }
                                },
                                onOpenSpotify = {
                                    openTrackInSpotify(
                                        context = context,
                                        spotifyTrackId = selectedTrack?.spotifyTrackId,
                                        externalUrl = selectedTrack?.externalUrl,
                                    )
                                },
                            )
                        }

                        item {
                            BrandSectionBanner(
                                title = "Tracks",
                                subtitle = "${uiState.tracks.size} Tracks im aktuellen Artist-Set.",
                                accent = SpotifyGreen,
                                icon = Icons.Default.MusicNote,
                                tag = "LIST",
                            )
                        }

                        items(uiState.tracks, key = { it.trackId }) { track ->
                            TrackRow(
                                track = track,
                                isPlaying = uiState.currentlyPlayingId == track.trackId,
                                isSelected = selectedTrackId == track.trackId,
                                onSelectTrack = { selectedTrackId = track.trackId },
                                onPlayToggle = { viewModel.togglePreview(track) },
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
    SkydownCard(contentPadding = PaddingValues(18.dp)) {
        BrandSectionBanner(
            title = "Instagram",
            subtitle = "Artist-Links und direkte Wege in den Social Feed.",
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
                                        com.skydown.android.ui.theme.InstagramPurple,
                                        com.skydown.android.ui.theme.InstagramPink,
                                        com.skydown.android.ui.theme.InstagramOrange,
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
    onOpenStudio: (() -> Unit)?,
    onOpenBeatHub: (() -> Unit)?,
) {
    SkydownCard(contentPadding = PaddingValues(18.dp)) {
        BrandSectionBanner(
            title = "Quick Links",
            subtitle = "Direkt zu Studio und Beats.",
            accent = ArenaGold,
            icon = Icons.Default.AutoAwesome,
            tag = "LINKS",
        )

        Column(
            modifier = Modifier.padding(top = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            onOpenStudio?.let { openStudio ->
                BrandActionButton(
                    text = "Studio",
                    onClick = openStudio,
                    accent = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.fillMaxWidth(),
                    icon = Icons.Default.AutoAwesome,
                )
            }

            onOpenBeatHub?.let { openBeatHub ->
                BrandActionButton(
                    text = "Beat Hub",
                    onClick = openBeatHub,
                    accent = ArenaGold,
                    modifier = Modifier.fillMaxWidth(),
                    icon = Icons.Default.GraphicEq,
                    filled = false,
                )
            }
        }
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

    SkydownCard(contentPadding = PaddingValues(18.dp)) {
        val playerAccent = if (!track.spotifyTrackId.isNullOrBlank()) SpotifyGreen else MaterialTheme.colorScheme.primary

        BrandSectionBanner(
            title = "Now Playing",
            subtitle = "Preview und Spotify-Link fuer den aktuellen Track.",
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
                                hasPreview && hasDirectSpotifyTrack -> "Preview laeuft direkt hier und springt bei Bedarf weiter zu Spotify."
                                hasPreview -> "Der Track hat eine direkte Preview in der App."
                                hasDirectSpotifyTrack || hasSpotifyArtistLink || hasSpotifySearch ->
                                    "Keine lokale Preview, aber Spotify bleibt direkt erreichbar."
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
                                hasPreview && hasDirectSpotifyTrack -> "Preview laeuft direkt hier und springt bei Bedarf weiter zu Spotify."
                                hasPreview -> "Der Track hat eine direkte Preview in der App."
                                hasDirectSpotifyTrack || hasSpotifyArtistLink || hasSpotifySearch ->
                                    "Keine lokale Preview, aber Spotify bleibt direkt erreichbar."
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
                text = if (hasPreview) "Preview" else "No Preview",
                imageVector = if (hasPreview) Icons.Default.PlayArrow else Icons.Default.Refresh,
                isActive = hasPreview,
                onClick = if (hasPreview) onPlayToggle else null,
            )
            MusicBadge(
                text = if (hasDirectSpotifyTrack || hasSpotifyArtistLink || hasSpotifySearch) "Spotify" else "Nur App",
                imageVector = Icons.Default.MusicNote,
                isActive = hasDirectSpotifyTrack || hasSpotifyArtistLink || hasSpotifySearch,
                onClick = if (hasDirectSpotifyTrack || hasSpotifyArtistLink || hasSpotifySearch) onOpenSpotify else null,
            )
            MusicBadge(
                text = if (isPlaying) "Live" else "Ready",
                imageVector = if (isPlaying) Icons.Default.GraphicEq else Icons.Default.Sync,
                isActive = isPlaying,
                onClick = if (hasPreview) onPlayToggle else null,
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
                            text = if (isPlaying) "Stop" else "Preview",
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
                            text = if (isPlaying) "Stop" else "Preview",
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
    onOpenArtistHub: () -> Unit,
    onOpenTracks: () -> Unit,
    onOpenSpotifyStatus: () -> Unit,
) {
    val screenHeaderSettings by AppContainer.screenHeaderSettingsRepository.settings.collectAsStateWithLifecycle()
    val socialProfile = uiState.selectedArtistSocialProfile
    val trackLabel = musicTrackLabel(uiState)
    BrandHeroCard(
        eyebrow = screenHeaderSettings.musicHubEyebrow.ifBlank { "SKY OS" },
        title = screenHeaderSettings.musicHubTitle.ifBlank { "Music" },
        subtitle = screenHeaderSettings.musicHubSubtitle.ifBlank { "Artists, Releases und Spotify im SkyOs Sound-Flow." },
        detail = screenHeaderSettings.musicHubDetail.ifBlank {
            "${uiState.selectedArtist} im Fokus. $trackLabel und ${uiState.availableArtists.size} Artists live."
        },
        backgroundImageUrl = screenHeaderSettings.musicHubImageUrl.ifBlank { null },
        accent = SpotifyGreen,
        secondaryAccent = ArenaGold,
        marks = listOf(BrandArtwork.Zweizwei),
        compactVisualDensity = compactVisualDensity,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                    text = "${uiState.availableArtists.size} Artists",
                    tint = ArenaGold,
                    onClick = onOpenArtistHub,
                )
                BrandPill(
                    text = trackLabel,
                    tint = ArenaGold,
                    onClick = onOpenTracks,
                )
                BrandPill(
                    text = if (uiState.isSpotifyConnected) "Spotify live" else "Spotify standby",
                    tint = if (uiState.isSpotifyConnected) FieldMint else MaterialTheme.colorScheme.secondary,
                    onClick = onOpenSpotifyStatus,
                )
            }

            BoxWithConstraints(
                modifier = Modifier.fillMaxWidth(),
            ) {
                val stackMetrics = maxWidth < 420.dp

                if (stackMetrics) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        MusicHeroStatusCard(
                            label = "Artist",
                            value = socialProfile?.handle ?: uiState.selectedArtist,
                            icon = Icons.Default.MusicNote,
                            accent = SpotifyGreen,
                            isActive = socialProfile != null || uiState.selectedArtist.isNotBlank(),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        MusicHeroStatusCard(
                            label = "Tracks",
                            value = trackLabel,
                            icon = Icons.Default.GraphicEq,
                            accent = ArenaGold,
                            isActive = uiState.isLoading || uiState.tracks.isNotEmpty(),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        MusicHeroStatusCard(
                            label = "Status",
                            value = musicHeroStatusValue(uiState),
                            icon = Icons.Default.Sync,
                            accent = FieldMint,
                            isActive = uiState.currentPreviewUrl != null || uiState.isSpotifyConnected || uiState.isLoading,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        MusicHeroStatusCard(
                            label = "Artist",
                            value = socialProfile?.handle ?: uiState.selectedArtist,
                            icon = Icons.Default.MusicNote,
                            accent = SpotifyGreen,
                            isActive = socialProfile != null || uiState.selectedArtist.isNotBlank(),
                            modifier = Modifier.weight(1f),
                        )
                        MusicHeroStatusCard(
                            label = "Tracks",
                            value = trackLabel,
                            icon = Icons.Default.GraphicEq,
                            accent = ArenaGold,
                            isActive = uiState.isLoading || uiState.tracks.isNotEmpty(),
                            modifier = Modifier.weight(1f),
                        )
                        MusicHeroStatusCard(
                            label = "Status",
                            value = musicHeroStatusValue(uiState),
                            icon = Icons.Default.Sync,
                            accent = FieldMint,
                            isActive = uiState.currentPreviewUrl != null || uiState.isSpotifyConnected || uiState.isLoading,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MusicHeroStatusCard(
    label: String,
    value: String,
    icon: ImageVector,
    accent: Color,
    isActive: Boolean,
    modifier: Modifier = Modifier,
) {
    BrandHeroMetricCard(
        label = label,
        value = value,
        accent = accent,
        modifier = modifier,
        icon = icon,
        isActive = isActive,
    )
}

private fun musicHeroStatusValue(uiState: MusicUiState): String {
    return when {
        uiState.currentPreviewUrl != null -> "Preview live"
        !uiState.errorMessage.isNullOrBlank() -> "Check feed"
        uiState.isSpotifyConnected -> "Spotify live"
        uiState.isLoading -> "Loading"
        else -> "Ready"
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
    Box(modifier = modifier) {
        MusicBackdropHalo(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 12.dp, start = 14.dp),
            size = 170.dp,
            tint = MaterialTheme.colorScheme.surface.copy(alpha = 0.12f),
        )
        MusicBackdropHalo(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 26.dp, end = 8.dp),
            size = 220.dp,
            tint = SpotifyGreen.copy(alpha = 0.14f),
        )
        MusicBackdropHalo(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 0.dp, bottom = 64.dp),
            size = 210.dp,
            tint = ArenaGold.copy(alpha = 0.12f),
        )
        MusicBackdropHalo(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 10.dp, bottom = 110.dp),
            size = 240.dp,
            tint = FieldMint.copy(alpha = 0.10f),
        )
    }
}

@Composable
private fun MusicBackdropHalo(
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp,
    tint: Color,
) {
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center,
    ) {
        listOf(1f, 0.72f, 0.46f).forEachIndexed { index, scale ->
            Box(
                modifier = Modifier
                    .size(size * scale)
                    .border(
                        width = 1.dp,
                        color = tint.copy(alpha = 0.20f - (index * 0.04f)),
                        shape = CircleShape,
                    ),
            )
        }
        Box(
            modifier = Modifier
                .size(size * 0.16f)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            tint.copy(alpha = 0.28f),
                            tint.copy(alpha = 0.08f),
                            Color.Transparent,
                        ),
                    ),
                    shape = CircleShape,
                ),
        )
    }
}

@Composable
private fun MusicSpotlightDeckCard(
    uiState: MusicUiState,
    selectedTrack: com.skydown.shared.model.Track?,
    artistPage: com.skydown.android.data.ArtistPageUi,
    onOpenInstagram: () -> Unit,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onOpenArtistPage: () -> Unit,
) {
    val socialProfile = uiState.selectedArtistSocialProfile
    val trackLabel = musicTrackLabel(uiState)
    val heroImage = artistPage.heroImageURL ?: selectedTrack?.artworkUrl100
    val spotlightSubtitle = if (socialProfile != null) {
        "${uiState.selectedArtist} ist im Fokus. Socials, Artist-Page und Spotify liegen direkt in Reichweite."
    } else {
        "${uiState.selectedArtist} ist im Fokus. Die Stage bleibt offen fuer Artist-Page und Spotify."
    }
    SkydownCard(contentPadding = PaddingValues(18.dp)) {
        BrandSectionBanner(
            title = "Artist Hub",
            subtitle = spotlightSubtitle,
            accent = SpotifyGreen,
            icon = Icons.Default.AutoAwesome,
            tag = "SPOTLIGHT",
        )

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 14.dp),
        ) {
            val stackHero = maxWidth < 380.dp

            if (stackHero) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    MusicSpotlightArtwork(
                        imageUrl = heroImage,
                        accent = if (uiState.isSpotifyConnected) SpotifyGreen else ArenaGold,
                        frameSize = 110.dp,
                        modifier = Modifier.size(110.dp),
                    )

                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = uiState.selectedArtist,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Black,
                        )
                        Text(
                            text = artistPage.tagline ?: socialProfile?.handle ?: "SkyOs Artist Hub",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.76f),
                        )
                        Text(
                            text = artistPage.bio ?: selectedTrack?.trackName?.let { "Aktueller Fokus: $it" }
                                ?: "Die Stage bleibt bereit fuer Releases, Clips und Social Touchpoints.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f),
                            maxLines = 4,
                            overflow = TextOverflow.Ellipsis,
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
                        imageUrl = heroImage,
                        accent = if (uiState.isSpotifyConnected) SpotifyGreen else ArenaGold,
                        frameSize = 110.dp,
                        modifier = Modifier.size(110.dp),
                    )

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = uiState.selectedArtist,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Black,
                        )
                        Text(
                            text = artistPage.tagline ?: socialProfile?.handle ?: "SkyOs Artist Hub",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.76f),
                        )
                        Text(
                            text = artistPage.bio ?: selectedTrack?.trackName?.let { "Aktueller Fokus: $it" }
                                ?: "Die Stage bleibt bereit fuer Releases, Clips und Social Touchpoints.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f),
                            maxLines = 4,
                            overflow = TextOverflow.Ellipsis,
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
            BrandPill(text = uiState.selectedArtist, tint = SpotifyGreen)
            BrandPill(text = trackLabel, tint = ArenaGold)
            BrandPill(
                text = if (uiState.isSpotifyConnected) "Hub live" else "Hub standby",
                tint = if (uiState.isSpotifyConnected) FieldMint else MaterialTheme.colorScheme.secondary,
            )
        }

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 14.dp),
        ) {
            val stackMetrics = maxWidth < 420.dp

            if (stackMetrics) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    MusicDeckMetricCard(
                        label = "Lead Track",
                        value = selectedTrack?.trackName ?: "Kein Track",
                        accent = SpotifyGreen,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    MusicDeckMetricCard(
                        label = "Links",
                        value = buildString {
                            append(if (socialProfile != null) "IG" else "Feed")
                            append(" / ")
                            append(if (!artistPage.spotifyURL.isNullOrBlank()) "Spotify" else "Page")
                        },
                        accent = ArenaGold,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    MusicDeckMetricCard(
                        label = "Status",
                        value = musicHeroStatusValue(uiState),
                        accent = FieldMint,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    MusicDeckMetricCard(
                        label = "Lead Track",
                        value = selectedTrack?.trackName ?: "Kein Track",
                        accent = SpotifyGreen,
                        modifier = Modifier.weight(1f),
                    )
                    MusicDeckMetricCard(
                        label = "Links",
                        value = buildString {
                            append(if (socialProfile != null) "IG" else "Feed")
                            append(" / ")
                            append(if (!artistPage.spotifyURL.isNullOrBlank()) "Spotify" else "Page")
                        },
                        accent = ArenaGold,
                        modifier = Modifier.weight(1f),
                    )
                    MusicDeckMetricCard(
                        label = "Status",
                        value = musicHeroStatusValue(uiState),
                        accent = FieldMint,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        Column(
            modifier = Modifier.padding(top = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            socialProfile?.let {
                BrandActionButton(
                    text = it.handle,
                    onClick = onOpenInstagram,
                    accent = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxWidth(),
                    icon = Icons.Default.CameraAlt,
                    filled = false,
                )
            }

            BoxWithConstraints {
                val stackButtons = maxWidth < 360.dp

                if (stackButtons) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        BrandActionButton(
                            text = if (uiState.isSpotifyConnected) "Spotify trennen" else "Spotify verbinden",
                            onClick = if (uiState.isSpotifyConnected) onDisconnect else onConnect,
                            accent = SpotifyGreen,
                            modifier = Modifier.fillMaxWidth(),
                            icon = if (uiState.isSpotifyConnected) {
                                Icons.AutoMirrored.Filled.Logout
                            } else {
                                Icons.Default.MusicNote
                            },
                            filled = !uiState.isSpotifyConnected,
                        )

                        BrandActionButton(
                            text = "Artist Page",
                            onClick = onOpenArtistPage,
                            accent = ArenaGold,
                            modifier = Modifier.fillMaxWidth(),
                            icon = Icons.Default.AutoAwesome,
                            filled = false,
                        )
                    }
                } else {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        BrandActionButton(
                            text = if (uiState.isSpotifyConnected) "Spotify trennen" else "Spotify verbinden",
                            onClick = if (uiState.isSpotifyConnected) onDisconnect else onConnect,
                            accent = SpotifyGreen,
                            modifier = Modifier.weight(1f),
                            icon = if (uiState.isSpotifyConnected) {
                                Icons.AutoMirrored.Filled.Logout
                            } else {
                                Icons.Default.MusicNote
                            },
                            filled = !uiState.isSpotifyConnected,
                        )

                        BrandActionButton(
                            text = "Artist Page",
                            onClick = onOpenArtistPage,
                            accent = ArenaGold,
                            modifier = Modifier.weight(1f),
                            icon = Icons.Default.AutoAwesome,
                            filled = false,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun musicSurfaceBrush(
    primaryAccent: Color,
    secondaryAccent: Color = ArenaGold,
): Brush {
    val isDarkPalette = MaterialTheme.colorScheme.background.luminance() < 0.36f
    return Brush.linearGradient(
        colors = listOf(
            MaterialTheme.colorScheme.surface.copy(alpha = if (isDarkPalette) 0.98f else 0.995f),
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (isDarkPalette) 0.78f else 0.64f),
            primaryAccent.copy(alpha = if (isDarkPalette) 0.16f else 0.10f),
            secondaryAccent.copy(alpha = if (isDarkPalette) 0.10f else 0.06f),
        ),
    )
}

@Composable
private fun musicSurfaceBorderColor(accent: Color): Color {
    val isDarkPalette = MaterialTheme.colorScheme.background.luminance() < 0.36f
    return accent.copy(alpha = if (isDarkPalette) 0.22f else 0.14f)
}

@Composable
private fun MusicSpotlightArtwork(
    imageUrl: String?,
    accent: Color,
    modifier: Modifier = Modifier,
    frameSize: androidx.compose.ui.unit.Dp = 110.dp,
) {
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
                        secondaryAccent = ArenaGold,
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
    artistPagesByName: Map<String, com.skydown.android.data.ArtistPageUi>,
    onArtistSelected: (String) -> Unit,
    onOpenArtistPage: ((String) -> Unit)?,
) {
    val safeArtists = artists.ifEmpty { listOf(selectedArtist) }
    val initialPage = safeArtists.indexOf(selectedArtist).takeIf { it >= 0 } ?: 0
    val pagerState = rememberPagerState(initialPage = initialPage) { safeArtists.size }

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

    SkydownCard(contentPadding = PaddingValues(18.dp)) {
        BrandSectionBanner(
            title = "Artists",
            subtitle = "Alle Artists sind direkt anwaehlbar. Nutze die Schnellwahl oder swipe unten durch den Showcase.",
            accent = ArenaGold,
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
                            primaryAccent = ArenaGold,
                            secondaryAccent = SpotifyGreen,
                        ),
                    )
                    .border(
                        width = 1.dp,
                        color = musicSurfaceBorderColor(ArenaGold),
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
                                primaryAccent = ArenaGold,
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
                                .background(ArenaGold)
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                        ) {
                            Text(
                                text = "Live",
                                style = MaterialTheme.typography.labelMedium,
                                color = DexBlueDeep,
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
                        BrandPill(text = "Artist Hub", tint = ArenaGold)
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
                                accent = ArenaGold,
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
                                accent = ArenaGold,
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
                            onClick = onOpenArtistPage?.let { { it(artist) } },
                        )
                    } else {
                        SmallMusicBadge(
                            text = "Page",
                            isAccent = true,
                            onClick = onOpenArtistPage?.let { { it(artist) } },
                        )
                    }
                    if (!page.instagramURL.isNullOrBlank()) {
                        SmallMusicBadge(
                            text = "Instagram",
                            isAccent = false,
                            onClick = onOpenArtistPage?.let { { it(artist) } },
                        )
                    }
                    if (!page.youtubeURL.isNullOrBlank()) {
                        SmallMusicBadge(
                            text = "YouTube",
                            isAccent = false,
                            onClick = onOpenArtistPage?.let { { it(artist) } },
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
    artistPagesByName: Map<String, com.skydown.android.data.ArtistPageUi>,
    onArtistSelected: (String) -> Unit,
) {
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
                        accent = MaterialTheme.colorScheme.primary,
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
                        accent = ArenaGold,
                        modifier = Modifier.weight(1f),
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    ArtistMetricCard(
                        title = "Artists",
                        value = artists.size.toString(),
                        accent = MaterialTheme.colorScheme.primary,
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
                        accent = ArenaGold,
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
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(
                musicSurfaceBrush(
                    primaryAccent = accent,
                    secondaryAccent = MaterialTheme.colorScheme.primary,
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
    page: com.skydown.android.data.ArtistPageUi,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = if (isSelected) SpotifyGreen else ArenaGold
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
                    secondaryAccent = MaterialTheme.colorScheme.primary,
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
    page: com.skydown.android.data.ArtistPageUi,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onOpenArtistPage: (() -> Unit)?,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                musicSurfaceBrush(
                    primaryAccent = MaterialTheme.colorScheme.primary,
                    secondaryAccent = SpotifyGreen,
                ),
            )
            .border(
                width = 1.dp,
                color = musicSurfaceBorderColor(MaterialTheme.colorScheme.primary),
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
                BrandPill(text = "Live", tint = ArenaGold)
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
    onClick: (() -> Unit)? = null,
) {
    BrandStatusChip(
        text = text,
        accent = SpotifyGreen,
        isActive = isAccent,
        onClick = onClick,
    )
}

private fun artistPageReachCount(page: com.skydown.android.data.ArtistPageUi): Int {
    return listOf(page.spotifyURL, page.instagramURL, page.youtubeURL).count { !it.isNullOrBlank() }
}

private fun artistReachSummary(page: com.skydown.android.data.ArtistPageUi): String {
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
    SkydownCard(contentPadding = PaddingValues(18.dp)) {
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
    SkydownCard(contentPadding = PaddingValues(18.dp)) {
        BrandSectionBanner(
            title = title,
            accent = if (loading) FieldMint else SpotifyGreen,
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
                    text = "Ladevorgang laeuft ...",
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }

        if (actionLabel != null && onAction != null) {
            Spacer(modifier = Modifier.height(16.dp))
            BrandActionButton(
                text = actionLabel,
                onClick = onAction,
                accent = if (loading) FieldMint else SpotifyGreen,
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
    onClick: (() -> Unit)? = null,
) {
    BrandStatusChip(
        text = text,
        accent = SpotifyGreen,
        icon = imageVector,
        isActive = isActive,
        onClick = onClick,
    )
}
