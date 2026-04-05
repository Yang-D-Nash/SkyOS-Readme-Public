package com.skydown.android.ui.screen

import android.content.Intent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
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
import com.skydown.android.ui.component.skydownContentPadding
import com.skydown.android.ui.component.skydownScreenBrush
import com.skydown.android.ui.component.skydownTopBarColors
import com.skydown.android.ui.component.openTrackInSpotify
import com.skydown.android.ui.model.MusicUiState
import com.skydown.android.ui.model.MusicInstagramDestination
import com.skydown.android.ui.theme.ArenaGold
import com.skydown.android.ui.theme.FieldMint
import com.skydown.android.ui.theme.InstagramOrange
import com.skydown.android.ui.theme.SpotifyGreen
import com.skydown.android.ui.viewmodel.MusicViewModel

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
    val selectedTrack = uiState.tracks.firstOrNull { it.trackId == selectedTrackId } ?: uiState.tracks.firstOrNull()
    val artistPagesByName = remember(artistPages) {
        artistPages
            .filter { it.brand == ArtistPageBrand.Zweizwei }
            .associateBy { it.artistName }
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
                        primaryAlpha = 0.08f,
                        secondaryAlpha = 0.06f,
                    ),
                ),
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = skydownContentPadding(innerPadding),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item {
                    MusicOverviewCard(
                        uiState = uiState,
                    )
                }

                item {
                    MusicConnectionCard(
                        uiState = uiState,
                        onOpenInstagram = {
                            uiState.selectedArtistSocialProfile?.let { socialProfile ->
                                openExternalLink(context, socialProfile.instagramUrl)
                            }
                        },
                        onConnect = {
                            viewModel.clearSpotifyError()
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, SpotifyAuthManager.buildAuthorizationUri()),
                            )
                        },
                        onDisconnect = {
                            player.stop()
                            player.clearMediaItems()
                            viewModel.disconnectSpotify()
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

    SkydownCard(contentPadding = PaddingValues(18.dp)) {
        val playerAccent = if (!track.spotifyTrackId.isNullOrBlank()) SpotifyGreen else MaterialTheme.colorScheme.primary

        BrandSectionBanner(
            title = "Now Playing",
            subtitle = "Preview und Spotify-Link fuer den aktuellen Track.",
            accent = playerAccent,
            icon = Icons.Default.MusicNote,
            tag = "LIVE",
        )

        Row(
            modifier = Modifier.padding(top = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.Top,
        ) {
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
                    text = track.artistName ?: "Zweizwei",
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
            }

            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(
                        playerAccent.copy(alpha = 0.16f),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.GraphicEq else Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = playerAccent,
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (!track.previewUrl.isNullOrBlank()) {
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
                    modifier = if (!track.previewUrl.isNullOrBlank()) Modifier.weight(1f) else Modifier.fillMaxWidth(),
                    icon = Icons.Default.MusicNote,
                )
            } else if (hasSpotifyArtistLink || hasSpotifySearch) {
                BrandActionButton(
                    text = "Spotify",
                    onClick = onOpenSpotify,
                    accent = SpotifyGreen,
                    modifier = if (!track.previewUrl.isNullOrBlank()) Modifier.weight(1f) else Modifier.fillMaxWidth(),
                    icon = Icons.Default.MusicNote,
                    filled = false,
                )
            }
        }
    }
}

@Composable
private fun MusicOverviewCard(
    uiState: MusicUiState,
) {
    val screenHeaderSettings by AppContainer.screenHeaderSettingsRepository.settings.collectAsStateWithLifecycle()
    val socialProfile = uiState.selectedArtistSocialProfile
    val trackLabel = when {
        uiState.isLoading -> "Katalog wird geladen"
        uiState.tracks.isEmpty() -> "Noch keine Tracks"
        uiState.tracks.size == 1 -> "1 Track"
        else -> "${uiState.tracks.size} Tracks"
    }
    BrandHeroCard(
        eyebrow = screenHeaderSettings.musicHubEyebrow.ifBlank { "SKY²²" },
        title = screenHeaderSettings.musicHubTitle.ifBlank { "Music" },
        subtitle = screenHeaderSettings.musicHubSubtitle.ifBlank { "Artists, Releases und Spotify im Sky²² Sound-Flow." },
        detail = screenHeaderSettings.musicHubDetail.ifBlank {
            "${uiState.selectedArtist} im Fokus. $trackLabel und ${uiState.availableArtists.size} Artists live."
        },
        backgroundImageUrl = screenHeaderSettings.musicHubImageUrl.ifBlank { null },
        backgroundImageSizeFraction = 0.58f,
        backgroundImageAlignment = Alignment.CenterEnd,
        backgroundImageMaxWidth = 188.dp,
        backgroundImageMaxHeight = 92.dp,
        accent = SpotifyGreen,
        secondaryAccent = ArenaGold,
        marks = listOf(BrandArtwork.Zweizwei),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BrandPill(text = uiState.selectedArtist, tint = SpotifyGreen)
                BrandPill(text = "${uiState.availableArtists.size} Artists", tint = ArenaGold)
                BrandPill(
                    text = if (uiState.isSpotifyConnected) "Spotify live" else "Spotify standby",
                    tint = if (uiState.isSpotifyConnected) FieldMint else MaterialTheme.colorScheme.secondary,
                )
            }

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

@Composable
private fun MusicConnectionCard(
    uiState: MusicUiState,
    onOpenInstagram: () -> Unit,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
) {
    val socialProfile = uiState.selectedArtistSocialProfile
    val connectionSubtitle = if (socialProfile != null) {
        "${uiState.selectedArtist} auf Instagram und Spotify direkt steuern."
    } else {
        "Spotify Verbindung direkt unter dem Header verwalten."
    }
    SkydownCard(contentPadding = PaddingValues(14.dp)) {
        BrandSectionBanner(
            title = "Connections",
            subtitle = connectionSubtitle,
            accent = FieldMint,
            icon = Icons.Default.Sync,
            tag = "SYNC",
        )

        socialProfile?.let {
            BrandActionButton(
                text = it.handle,
                onClick = onOpenInstagram,
                accent = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                icon = Icons.Default.CameraAlt,
                filled = false,
            )
        }

        if (uiState.isSpotifyConnected) {
            BrandActionButton(
                text = "Spotify trennen",
                onClick = onDisconnect,
                accent = SpotifyGreen,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                icon = Icons.AutoMirrored.Filled.Logout,
                filled = false,
            )
        } else {
            BrandActionButton(
                text = "Spotify",
                onClick = onConnect,
                accent = SpotifyGreen,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                icon = Icons.Default.MusicNote,
            )
        }
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
            subtitle = "Swipe durch alle Artist-Pages und oeffne direkt das Profil, das dich interessiert.",
            accent = ArenaGold,
            icon = Icons.Default.AutoAwesome,
            tag = "ARTISTS",
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
                        Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.62f),
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
                            ),
                        ),
                    )
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = artist,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                        )
                        Text(
                            text = page.tagline ?: "ZweiZwei Artist",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                        )
                    }

                    if (page.hasCustomPresentation) {
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                        ) {
                            Text(
                                text = "Live",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }

                Text(
                    text = page.bio ?: "$artist hat eine eigene Page mit Songs, Story und den wichtigsten Links.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f),
                    maxLines = 3,
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!page.spotifyURL.isNullOrBlank()) {
                        SmallMusicBadge(text = "Spotify", isAccent = true)
                    } else {
                        SmallMusicBadge(text = "Page", isAccent = true)
                    }
                    if (!page.instagramURL.isNullOrBlank()) {
                        SmallMusicBadge(text = "Instagram", isAccent = false)
                    }
                    if (!page.youtubeURL.isNullOrBlank()) {
                        SmallMusicBadge(text = "YouTube", isAccent = false)
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
private fun SmallMusicBadge(
    text: String,
    isAccent: Boolean,
) {
    BrandStatusChip(
        text = text,
        accent = SpotifyGreen,
        isActive = isAccent,
    )
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
) {
    BrandStatusChip(
        text = text,
        accent = SpotifyGreen,
        icon = imageVector,
        isActive = isActive,
    )
}
