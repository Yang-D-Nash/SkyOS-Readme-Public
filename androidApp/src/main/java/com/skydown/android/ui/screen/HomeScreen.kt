package com.skydown.android.ui.screen

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.skydown.android.data.AppContainer
import com.skydown.android.data.mediaAttributionContext
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.skydown.android.ui.component.openTrackInSpotify
import coil3.compose.AsyncImage
import com.skydown.android.ui.component.AppTopBarSessionActions
import com.skydown.android.ui.component.BrandActionButton
import com.skydown.android.ui.component.BrandArtwork
import com.skydown.android.ui.component.BrandHeroCard
import com.skydown.android.ui.component.BrandHeroMetricCard
import com.skydown.android.ui.component.BrandPreviewFrame
import com.skydown.android.ui.component.BrandSectionBanner
import com.skydown.android.ui.component.BrandStatusChip
import com.skydown.android.ui.component.BrandPill
import com.skydown.android.ui.component.ExternalVideoWebPlayer
import com.skydown.android.ui.component.SkydownCard
import com.skydown.android.ui.component.SkydownTopBarTitle
import com.skydown.android.ui.component.SkydownUiTokens
import com.skydown.android.ui.component.skydownPressable
import com.skydown.android.ui.component.skydownScreenBrush
import com.skydown.android.ui.component.skydownTopBarColors
import com.skydown.android.ui.model.FeaturedBeatHighlight
import com.skydown.android.ui.model.FeaturedVideoHighlight
import com.skydown.android.ui.model.HomeUiState
import com.skydown.android.ui.theme.ArenaGold
import com.skydown.android.ui.theme.ArenaRed
import com.skydown.android.ui.theme.DexBlue
import com.skydown.android.ui.theme.DexBlueDeep
import com.skydown.android.ui.theme.FieldMint
import com.skydown.android.ui.theme.InstagramOrange
import com.skydown.android.ui.theme.InstagramPink
import com.skydown.android.ui.theme.InstagramPurple
import com.skydown.android.ui.theme.SpotifyGreen
import com.skydown.android.ui.viewmodel.HomeViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenCart: () -> Unit = {},
    onOpenProfile: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onOpenWorkflow: (() -> Unit)? = null,
    viewModel: HomeViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val screenHeaderSettings by AppContainer.screenHeaderSettingsRepository.settings.collectAsStateWithLifecycle()
    var activeDestination by rememberSaveable { mutableStateOf<String?>(null) }

    if (activeDestination == homeDestinationNicmaProducer) {
        NicmaProducerScreen(
            onBack = { activeDestination = null },
        )
        return
    }

    if (activeDestination == homeDestinationBeatHub) {
        BeatHubScreen(
            onBack = { activeDestination = null },
        )
        return
    }

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val context = LocalContext.current
    val mediaContext = remember(context) { context.mediaAttributionContext() }
    val audioPlayer = remember(mediaContext) {
        ExoPlayer.Builder(mediaContext).build().apply {
            playWhenReady = true
        }
    }
    val videoPlayer = remember(mediaContext) {
        ExoPlayer.Builder(mediaContext).build().apply {
            playWhenReady = false
        }
    }
    var currentAudioKey by rememberSaveable { mutableStateOf<String?>(null) }
    var currentVideoId by rememberSaveable { mutableStateOf<String?>(null) }

    DisposableEffect(audioPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    audioPlayer.stop()
                    audioPlayer.clearMediaItems()
                    currentAudioKey = null
                }
            }
        }
        audioPlayer.addListener(listener)

        onDispose {
            audioPlayer.removeListener(listener)
            audioPlayer.release()
        }
    }

    DisposableEffect(videoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    videoPlayer.pause()
                    videoPlayer.seekTo(0)
                    currentVideoId = null
                }
            }
        }
        videoPlayer.addListener(listener)

        onDispose {
            videoPlayer.removeListener(listener)
            videoPlayer.release()
        }
    }

    LaunchedEffect(uiState.featuredVideo?.id, uiState.featuredVideo?.downloadUrl) {
        val featuredVideo = uiState.featuredVideo
        if (featuredVideo == null || featuredVideo.downloadUrl.isBlank()) {
            videoPlayer.stop()
            videoPlayer.clearMediaItems()
            currentVideoId = null
        } else {
            videoPlayer.setMediaItem(MediaItem.fromUri(featuredVideo.downloadUrl))
            videoPlayer.prepare()
            videoPlayer.pause()
            currentVideoId = null
        }
    }

    val activeSignalCount = homeTrackedSignalCount(uiState)

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    SkydownTopBarTitle(
                        "Sky²²",
                    )
                },
                actions = {
                    AppTopBarSessionActions(
                        onOpenCart = onOpenCart,
                        onOpenProfile = onOpenProfile,
                        onOpenSettings = onOpenSettings,
                    ) {
                        val interactionSource = remember { MutableInteractionSource() }
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.82f),
                            shape = RoundedCornerShape(999.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)),
                            tonalElevation = 4.dp,
                            modifier = Modifier.skydownPressable(interactionSource),
                        ) {
                            IconButton(
                                onClick = onOpenWorkflow ?: viewModel::refresh,
                                interactionSource = interactionSource,
                            ) {
                                Icon(
                                    imageVector = if (onOpenWorkflow != null) {
                                        Icons.Default.AutoAwesome
                                    } else {
                                        Icons.Default.Refresh
                                    },
                                    contentDescription = if (onOpenWorkflow != null) {
                                        "Automationen oeffnen"
                                    } else {
                                        "Hub aktualisieren"
                                    },
                                )
                            }
                        }
                    }
                },
                colors = skydownTopBarColors(),
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    skydownScreenBrush(
                        primaryColor = DexBlue,
                        secondaryColor = ArenaRed,
                        primaryAlpha = 0.18f,
                        secondaryAlpha = 0.12f,
                    ),
                ),
        ) {
            HomeMapBackdrop(
                modifier = Modifier.fillMaxSize(),
            )
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = SkydownUiTokens.screenHorizontalPadding,
                    top = innerPadding.calculateTopPadding() + 4.dp,
                    end = SkydownUiTokens.screenHorizontalPadding,
                    bottom = innerPadding.calculateBottomPadding() + SkydownUiTokens.screenBottomPadding,
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item {
                    HomeAnimatedItem(order = 0) {
                        BrandHeroCard(
                            eyebrow = screenHeaderSettings.homeEyebrow.ifBlank { "SKY²²" },
                            title = screenHeaderSettings.homeTitle.ifBlank { "Home" },
                            subtitle = screenHeaderSettings.homeSubtitle.ifBlank { "Music, Merch und Video in einem klaren Street-Flow." },
                            detail = screenHeaderSettings.homeDetail.ifBlank { "$activeSignalCount von $homeSignalTotal Bereichen sind gerade live." },
                            backgroundImageUrl = screenHeaderSettings.homeImageUrl.ifBlank { null },
                            accent = ArenaGold,
                            secondaryAccent = ArenaRed,
                            marks = listOf(BrandArtwork.Combined),
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                BrandPill(text = "Sky²² Core", tint = ArenaGold)
                                BrandPill(text = "$activeSignalCount/$homeSignalTotal Live", tint = FieldMint)
                                BrandPill(text = "Street ready", tint = ArenaRed)
                            }
                            HomeHeroStatusRow(
                                uiState = uiState,
                                modifier = Modifier.padding(top = 12.dp),
                            )
                        }
                    }
                }

                item {
                    HomeAnimatedItem(order = 1) {
                        HomeLatestReleaseCard(
                            uiState = uiState,
                            isPlaying = currentAudioKey == uiState.featuredTrack?.let(::homeTrackAudioKey),
                            onPlayToggle = { track ->
                                val previewUrl = track.previewUrl
                                if (previewUrl.isNullOrBlank()) return@HomeLatestReleaseCard
                                val audioKey = homeTrackAudioKey(track)
                                if (currentAudioKey == audioKey) {
                                    audioPlayer.stop()
                                    audioPlayer.clearMediaItems()
                                    currentAudioKey = null
                                } else {
                                    videoPlayer.pause()
                                    videoPlayer.seekTo(0)
                                    currentVideoId = null
                                    audioPlayer.setMediaItem(MediaItem.fromUri(previewUrl))
                                    audioPlayer.prepare()
                                    audioPlayer.play()
                                    currentAudioKey = audioKey
                                }
                            },
                            onOpenSpotify = { track ->
                                openTrackInSpotify(
                                    context = context,
                                    spotifyArtistId = track.spotifyArtistId,
                                    spotifyTrackId = track.spotifyTrackId,
                                    externalUrl = track.externalUrl,
                                )
                            },
                        )
                    }
                }

                item {
                    HomeAnimatedItem(order = 2) {
                        HomeLatestBeatCard(
                            uiState = uiState,
                            isPlaying = currentAudioKey == uiState.featuredBeat?.let(::homeBeatAudioKey),
                            onPlayToggle = { beat ->
                                if (!beat.isPlayable || beat.downloadUrl.isBlank()) return@HomeLatestBeatCard
                                val audioKey = homeBeatAudioKey(beat)
                                if (currentAudioKey == audioKey) {
                                    audioPlayer.stop()
                                    audioPlayer.clearMediaItems()
                                    currentAudioKey = null
                                } else {
                                    videoPlayer.pause()
                                    videoPlayer.seekTo(0)
                                    currentVideoId = null
                                    audioPlayer.setMediaItem(MediaItem.fromUri(beat.downloadUrl))
                                    audioPlayer.prepare()
                                    audioPlayer.play()
                                    currentAudioKey = audioKey
                                }
                            },
                        )
                    }
                }

                item {
                    HomeAnimatedItem(order = 3) {
                        HomeLatestVideoCard(
                            uiState = uiState,
                            player = videoPlayer,
                            isPlaying = currentVideoId == uiState.featuredVideo?.id,
                            onPlayToggle = { video ->
                                if (video.downloadUrl.isBlank()) return@HomeLatestVideoCard

                                if (currentVideoId == video.id) {
                                    videoPlayer.pause()
                                    videoPlayer.seekTo(0)
                                    currentVideoId = null
                                } else {
                                    audioPlayer.stop()
                                    audioPlayer.clearMediaItems()
                                    currentAudioKey = null
                                    videoPlayer.play()
                                    currentVideoId = video.id
                                }
                            },
                        )
                    }
                }

                item {
                    HomeAnimatedItem(order = 4) {
                        HomeStoryCard(
                            onOpenBeatHub = {
                                audioPlayer.stop()
                                audioPlayer.clearMediaItems()
                                currentAudioKey = null
                                videoPlayer.pause()
                                videoPlayer.seekTo(0)
                                currentVideoId = null
                                activeDestination = homeDestinationBeatHub
                            },
                            onOpenNicma = {
                                audioPlayer.stop()
                                audioPlayer.clearMediaItems()
                                currentAudioKey = null
                                videoPlayer.pause()
                                videoPlayer.seekTo(0)
                                currentVideoId = null
                                activeDestination = homeDestinationNicmaProducer
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeAnimatedItem(
    order: Int,
    content: @Composable () -> Unit,
) {
    var visible by remember(order) { mutableStateOf(false) }

    LaunchedEffect(order) {
        delay(order * 18L)
        visible = true
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + slideInVertically(initialOffsetY = { it / 6 }),
    ) {
        content()
    }
}

@Composable
private fun HomeHeroStatusRow(
    uiState: HomeUiState,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        HomeHeroStatusCard(
            label = "Music",
            value = uiState.featuredTrack?.trackName ?: "Neuer Release",
            icon = Icons.Default.MusicNote,
            accent = SpotifyGreen,
            isActive = uiState.featuredTrack != null,
            modifier = Modifier.weight(1f),
        )
        HomeHeroStatusCard(
            label = "Beats",
            value = uiState.featuredBeat?.title ?: "Neue Auswahl",
            icon = Icons.Default.GraphicEq,
            accent = ArenaGold,
            isActive = uiState.featuredBeat != null,
            modifier = Modifier.weight(1f),
        )
        HomeHeroStatusCard(
            label = "Video",
            value = uiState.featuredVideo?.title ?: "Neuer Clip",
            icon = Icons.Default.Movie,
            accent = ArenaRed,
            isActive = uiState.featuredVideo != null,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun HomeHeroStatusCard(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
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

@Composable
private fun HomeMapBackdrop(
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        HomeBackdropHalo(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 82.dp, y = (-38).dp),
            size = 220.dp,
            tint = ArenaGold.copy(alpha = 0.08f),
        )
        HomeBackdropHalo(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset(x = (-94).dp, y = 96.dp),
            size = 210.dp,
            tint = DexBlue.copy(alpha = 0.08f),
        )
        HomeBackdropHalo(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 92.dp, y = 156.dp),
            size = 250.dp,
            tint = ArenaRed.copy(alpha = 0.07f),
        )
        HomeBackdropHalo(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = (-88).dp, y = 146.dp),
            size = 180.dp,
            tint = FieldMint.copy(alpha = 0.06f),
        )
    }
}

@Composable
private fun HomeBackdropHalo(
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp,
    tint: Color,
) {
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .background(
                    color = tint.copy(alpha = 0.18f),
                    shape = CircleShape,
                ),
        )
        Box(
            modifier = Modifier
                .width(size * 0.56f)
                .height(1.dp)
                .background(tint.copy(alpha = 0.16f)),
        )
        Box(
            modifier = Modifier
                .width(1.dp)
                .height(size * 0.56f)
                .background(tint.copy(alpha = 0.16f)),
        )
        listOf(0.78f, 0.56f, 0.34f).forEachIndexed { index, scale ->
            Box(
                modifier = Modifier
                    .size(size * scale)
                    .border(
                        width = 1.dp,
                        color = tint.copy(alpha = 0.16f - (index * 0.03f)),
                        shape = CircleShape,
                    ),
            )
        }
        Box(
            modifier = Modifier
                .size(size * 0.10f)
                .background(
                    color = tint.copy(alpha = 0.18f),
                    shape = CircleShape,
                ),
        )
    }
}

@Composable
private fun HomeFieldGuideCard(
    uiState: HomeUiState,
) {
    val signals = listOf(
        HomeRadarSignal(
            title = "Musik",
            subtitle = uiState.featuredTrack?.trackName ?: "Naechster Release folgt",
            icon = Icons.Default.MusicNote,
            accent = SpotifyGreen,
            isActive = uiState.featuredTrack != null,
        ),
        HomeRadarSignal(
            title = "Beats",
            subtitle = uiState.featuredBeat?.title ?: "Neue Beats folgen",
            icon = Icons.Default.GraphicEq,
            accent = MaterialTheme.colorScheme.secondary,
            isActive = uiState.featuredBeat != null,
        ),
        HomeRadarSignal(
            title = "Videos",
            subtitle = uiState.featuredVideo?.title ?: "Neuer Clip folgt",
            icon = Icons.Default.Movie,
            accent = InstagramOrange,
            isActive = uiState.featuredVideo != null,
        ),
    )
    val activeSignals = signals.count { it.isActive }
    val statusLabel = when (activeSignals) {
        homeSignalTotal -> "Live"
        0 -> "Standby"
        else -> "Update"
    }

    SkydownCard(contentPadding = PaddingValues(SkydownUiTokens.cardPadding)) {
        HomeSectionBanner(
            title = "League Map",
            subtitle = "Aktive Zonen, Begegnungen und der schnellste Weg durch den Hub.",
            icon = Icons.Default.Radar,
            accent = ArenaGold,
            tag = "LIVE",
        )

        Text(
            text = when (activeSignals) {
                homeSignalTotal -> "Alle Bereiche sind live. Die Region spielt gerade komplett offen."
                0 -> "Gerade ist noch kein Bereich live. Der Hub bleibt im Standby und scannt weiter."
                else -> "$activeSignals von $homeSignalTotal Bereichen senden gerade ein starkes Signal."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.74f),
            modifier = Modifier.padding(top = 10.dp),
        )

        HomeDexStatusRow(
            activeSignals = activeSignals,
            scanLabel = statusLabel,
            modifier = Modifier.padding(top = 16.dp),
        )

        HomeRouteDeck(
            signals = signals,
            modifier = Modifier.padding(top = 16.dp),
        )

        HomeSignalMeter(
            activeSignals = activeSignals,
            totalSignals = signals.size,
            modifier = Modifier.padding(top = 12.dp),
        )

        HomeRadarSurface(
            signals = signals,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
                .height(196.dp),
        )

        Column(
            modifier = Modifier.padding(top = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            signals.forEach { signal ->
                HomeRadarSignalRow(signal = signal)
            }
        }

        Text(
            text = "Zieh nach unten, um die Karte neu zu scannen und alle Bereiche zu aktualisieren.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.64f),
            modifier = Modifier.padding(top = 14.dp),
        )
    }
}

@Composable
private fun HomeSectionBanner(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accent: Color,
    tag: String? = null,
) {
    BrandSectionBanner(
        title = title,
        subtitle = subtitle,
        accent = accent,
        icon = icon,
        tag = tag,
    )
}

@Composable
private fun HomeDexStatusRow(
    activeSignals: Int,
    scanLabel: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        HomeDexStatusCard(
            label = "Bereiche",
            value = "$activeSignals/$homeSignalTotal",
            accent = ArenaGold,
            modifier = Modifier.weight(1f),
        )
        HomeDexStatusCard(
            label = "Status",
            value = scanLabel,
            accent = ArenaRed,
            modifier = Modifier.weight(1f),
        )
        HomeDexStatusCard(
            label = "Refresh",
            value = "Swipe",
            accent = DexBlue,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun HomeDexStatusCard(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                        accent.copy(alpha = 0.12f),
                        DexBlue.copy(alpha = 0.06f),
                    ),
                ),
            )
            .border(
                width = 1.dp,
                color = accent.copy(alpha = 0.18f),
                shape = RoundedCornerShape(20.dp),
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun HomeRouteDeck(
    signals: List<HomeRadarSignal>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            signals.getOrNull(0)?.let { signal ->
                HomeRouteCard(
                    routeLabel = "Route 01",
                    signal = signal,
                    modifier = Modifier.weight(1f),
                )
            }
            signals.getOrNull(1)?.let { signal ->
                HomeRouteCard(
                    routeLabel = "Route 02",
                    signal = signal,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        signals.getOrNull(2)?.let { signal ->
            HomeRouteCard(
                routeLabel = "Route 03",
                signal = signal,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun HomeRouteCard(
    routeLabel: String,
    signal: HomeRadarSignal,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        DexBlueDeep.copy(alpha = 0.94f),
                        signal.accent.copy(alpha = if (signal.isActive) 0.24f else 0.12f),
                        ArenaGold.copy(alpha = if (signal.isActive) 0.14f else 0.08f),
                    ),
                ),
            )
            .border(
                width = 1.dp,
                color = signal.accent.copy(alpha = if (signal.isActive) 0.30f else 0.14f),
                shape = RoundedCornerShape(22.dp),
            )
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = routeLabel,
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.74f),
                fontWeight = FontWeight.SemiBold,
            )

            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(
                        if (signal.isActive) {
                            signal.accent
                        } else {
                            Color.White.copy(alpha = 0.24f)
                        },
                    ),
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                signal.accent.copy(alpha = if (signal.isActive) 0.92f else 0.20f),
                                signal.accent.copy(alpha = if (signal.isActive) 0.28f else 0.08f),
                                Color.Transparent,
                            ),
                        ),
                    )
                    .border(
                        width = 1.dp,
                        color = signal.accent.copy(alpha = if (signal.isActive) 0.46f else 0.14f),
                        shape = CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = signal.icon,
                    contentDescription = null,
                    tint = if (signal.isActive) Color.White else signal.accent,
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = signal.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = signal.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.72f),
                )
            }
        }

        HomeBadge(
            text = if (signal.isActive) "ENCOUNTER" else "SCAN",
            icon = if (signal.isActive) Icons.Default.CheckCircle else Icons.Default.Refresh,
            isActive = signal.isActive,
            accent = signal.accent,
        )
    }
}

@Composable
private fun HomeSignalMeter(
    activeSignals: Int,
    totalSignals: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        repeat(totalSignals) { index ->
            val isActive = index < activeSignals
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(10.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(
                        Brush.linearGradient(
                            colors = if (isActive) {
                                listOf(
                                    ArenaGold.copy(alpha = 0.92f),
                                    ArenaRed.copy(alpha = 0.86f),
                                )
                            } else {
                                listOf(
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.70f),
                                    DexBlue.copy(alpha = 0.12f),
                                )
                            },
                        ),
                    )
                    .border(
                        width = 1.dp,
                        color = if (isActive) {
                            ArenaGold.copy(alpha = 0.36f)
                        } else {
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
                        },
                        shape = RoundedCornerShape(999.dp),
                    ),
            )
        }
    }
}

@Composable
private fun HomeRadarSurface(
    signals: List<HomeRadarSignal>,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(28.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                        DexBlue.copy(alpha = 0.22f),
                        ArenaRed.copy(alpha = 0.12f),
                        ArenaGold.copy(alpha = 0.12f),
                        Color.Black.copy(alpha = 0.22f),
                    ),
                ),
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        ArenaGold.copy(alpha = 0.32f),
                        ArenaRed.copy(alpha = 0.20f),
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.16f),
                    ),
                ),
                shape = RoundedCornerShape(28.dp),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(196.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            ArenaGold.copy(alpha = 0.16f),
                            DexBlue.copy(alpha = 0.12f),
                            Color.Transparent,
                        ),
                    ),
                    shape = CircleShape,
                ),
        )
        listOf(188.dp, 134.dp, 80.dp).forEachIndexed { index, ringSize ->
            Box(
                modifier = Modifier
                    .size(ringSize)
                    .border(
                        width = 1.dp,
                        color = ArenaGold.copy(alpha = 0.24f - (index * 0.04f)),
                        shape = CircleShape,
                    ),
            )
        }
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            ArenaRed.copy(alpha = 0.92f),
                            ArenaGold.copy(alpha = 0.82f),
                        ),
                    ),
                )
                .border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.16f),
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(28.dp),
            )
        }

        signals.getOrNull(0)?.let { signal ->
            HomeRadarNode(
                signal = signal,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = 28.dp),
            )
        }
        signals.getOrNull(1)?.let { signal ->
            HomeRadarNode(
                signal = signal,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = 46.dp, y = 26.dp),
            )
        }
        signals.getOrNull(2)?.let { signal ->
            HomeRadarNode(
                signal = signal,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = (-40).dp, y = (-34).dp),
            )
        }
    }
}

@Composable
private fun HomeRadarNode(
    signal: HomeRadarSignal,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(if (signal.isActive) 54.dp else 48.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            signal.accent.copy(alpha = if (signal.isActive) 0.92f else 0.22f),
                            ArenaGold.copy(alpha = if (signal.isActive) 0.40f else 0.08f),
                            signal.accent.copy(alpha = if (signal.isActive) 0.34f else 0.10f),
                        ),
                    ),
                )
                .border(
                    width = 1.dp,
                    color = signal.accent.copy(alpha = if (signal.isActive) 0.52f else 0.18f),
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = signal.icon,
                contentDescription = null,
                tint = if (signal.isActive) Color.White else signal.accent,
                modifier = Modifier.size(22.dp),
            )
        }
        Text(
            text = signal.title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.88f),
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun HomeRadarSignalRow(
    signal: HomeRadarSignal,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                        DexBlue.copy(alpha = 0.06f),
                        signal.accent.copy(alpha = if (signal.isActive) 0.16f else 0.06f),
                    ),
                ),
            )
            .border(
                width = 1.dp,
                color = signal.accent.copy(alpha = if (signal.isActive) 0.20f else 0.10f),
                shape = RoundedCornerShape(18.dp),
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(signal.accent.copy(alpha = if (signal.isActive) 1f else 0.32f)),
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = signal.title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = signal.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
            )
        }
        HomeBadge(
            text = if (signal.isActive) "AKTIV" else "SUCHE",
            icon = if (signal.isActive) Icons.Default.CheckCircle else Icons.Default.Refresh,
            isActive = signal.isActive,
            accent = signal.accent,
        )
    }
}

private data class HomeRadarSignal(
    val title: String,
    val subtitle: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val accent: Color,
    val isActive: Boolean,
)

@Composable
private fun HomeLatestReleaseCard(
    uiState: HomeUiState,
    isPlaying: Boolean,
    onPlayToggle: (com.skydown.shared.model.Track) -> Unit,
    onOpenSpotify: (com.skydown.shared.model.Track) -> Unit,
) {
    SkydownCard(contentPadding = PaddingValues(SkydownUiTokens.cardPadding)) {
        HomeSectionBanner(
            title = "Musik",
            subtitle = "Neuester Release direkt auf Home.",
            icon = Icons.Default.MusicNote,
            accent = SpotifyGreen,
            tag = "TRACK 02",
        )
        val track = uiState.featuredTrack

        if (track == null) {
            Text(
                text = uiState.homeTrackMessage ?: "Neuer Song erscheint hier.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                modifier = Modifier.padding(top = 8.dp),
            )
            return@SkydownCard
        }

        Row(
            modifier = Modifier.padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BrandPreviewFrame(
                accent = SpotifyGreen,
                modifier = Modifier
                    .size(86.dp),
            ) {
                AsyncImage(
                    model = track.artworkUrl100,
                    contentDescription = track.trackName,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = track.trackName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = track.artistName ?: "Zweizwei",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.74f),
                )
                Text(
                    text = buildString {
                        append(track.collectionName ?: "Spotify Release")
                        track.releaseDate?.takeIf { it.isNotBlank() }?.let {
                            append(" • ")
                            append(it)
                        }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.64f),
                )
            }
        }

        val hasPreview = !track.previewUrl.isNullOrBlank()
        val hasSpotifyTarget = homeHasSpotifyTarget(track)

        Row(
            modifier = Modifier.padding(top = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            HomeBadge(
                text = track.artistName ?: "Zweizwei",
                icon = Icons.Default.Person,
                isActive = true,
                accent = MaterialTheme.colorScheme.primary,
            )
            HomeBadge(
                text = if (hasPreview) "Vorschau" else "Extern",
                icon = if (hasPreview) Icons.Default.PlayArrow else Icons.Default.Info,
                isActive = hasPreview,
                accent = SpotifyGreen,
            )
            if (hasSpotifyTarget) {
                HomeBadge(
                    text = "Spotify",
                    icon = Icons.Default.MusicNote,
                    isActive = true,
                    accent = SpotifyGreen,
                )
            }
        }

        Text(
            text = when {
                hasPreview -> "Die Vorschau startet direkt hier."
                hasSpotifyTarget -> "Falls keine Vorschau da ist, springst du direkt zu Spotify."
                else -> "Der neueste Track ist hier hinterlegt."
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.64f),
            modifier = Modifier.padding(top = 14.dp),
        )

        Column(
            modifier = Modifier.padding(top = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (hasPreview) {
                HomeMediaActionButton(
                    label = if (isPlaying) "Stop" else "Play",
                    isActive = isPlaying,
                    accent = SpotifyGreen,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { onPlayToggle(track) },
                )
            }

            if (hasSpotifyTarget) {
                BrandActionButton(
                    text = homeSpotifyActionLabel(track),
                    onClick = { onOpenSpotify(track) },
                    accent = SpotifyGreen,
                    modifier = Modifier.fillMaxWidth(),
                    icon = Icons.Default.MusicNote,
                    filled = false,
                )
            }
        }
    }
}

@Composable
private fun HomeLatestBeatCard(
    uiState: HomeUiState,
    isPlaying: Boolean,
    onPlayToggle: (FeaturedBeatHighlight) -> Unit,
) {
    val context = LocalContext.current
    SkydownCard(contentPadding = PaddingValues(SkydownUiTokens.cardPadding)) {
        HomeSectionBanner(
            title = "Beats",
            subtitle = "Neuester Beat aus dem Hub.",
            icon = Icons.Default.GraphicEq,
            accent = MaterialTheme.colorScheme.secondary,
            tag = "BEAT 03",
        )
        val beat = uiState.featuredBeat

        if (beat == null) {
            Text(
                text = uiState.homeBeatMessage ?: "Neuer Beat erscheint hier.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                modifier = Modifier.padding(top = 8.dp),
            )
            return@SkydownCard
        }

        Row(
            modifier = Modifier.padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.Top,
        ) {
            BrandPreviewFrame(
                accent = MaterialTheme.colorScheme.secondary,
                modifier = Modifier
                    .size(86.dp),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(34.dp),
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = beat.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = beat.artistName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.74f),
                )
                if (beat.notes.isNotBlank()) {
                    Text(
                        text = beat.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.64f),
                    )
                }
            }
        }

        Row(
            modifier = Modifier.padding(top = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            HomeBadge(
                text = beat.artistName,
                icon = Icons.Default.Person,
                isActive = true,
                accent = MaterialTheme.colorScheme.secondary,
            )
            HomeBadge(
                text = if (beat.isPlayable) "Abspielbar" else "Extern",
                icon = if (beat.isPlayable) Icons.Default.PlayArrow else Icons.Default.Language,
                isActive = beat.isPlayable,
                accent = MaterialTheme.colorScheme.secondary,
            )
        }

        if (beat.isPlayable && beat.downloadUrl.isNotBlank()) {
            HomeMediaActionButton(
                label = if (isPlaying) "Stop" else "Play",
                isActive = isPlaying,
                accent = MaterialTheme.colorScheme.secondary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp),
                onClick = { onPlayToggle(beat) },
            )
        }

        if (beat.openUrl.isNotBlank()) {
            BrandActionButton(
                text = "Original oeffnen",
                onClick = { openExternalLink(context, beat.openUrl) },
                accent = MaterialTheme.colorScheme.secondary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                icon = Icons.Default.Language,
                filled = false,
            )
        }
    }
}

@Composable
private fun HomeLatestVideoCard(
    uiState: HomeUiState,
    player: ExoPlayer,
    isPlaying: Boolean,
    onPlayToggle: (FeaturedVideoHighlight) -> Unit,
) {
    val context = LocalContext.current
    SkydownCard(contentPadding = PaddingValues(SkydownUiTokens.cardPadding)) {
        HomeSectionBanner(
            title = "Videos",
            subtitle = "Aktueller Clip direkt auf Home.",
            icon = Icons.Default.Movie,
            accent = InstagramOrange,
            tag = "VIDEO 04",
        )
        val video = uiState.featuredVideo

        if (video == null) {
            Text(
                text = uiState.homeVideoMessage ?: "Neues Video erscheint hier.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                modifier = Modifier.padding(top = 8.dp),
            )
            return@SkydownCard
        }

        Row(
            modifier = Modifier.padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.Top,
        ) {
            BrandPreviewFrame(
                accent = InstagramOrange,
                modifier = Modifier
                    .size(86.dp),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.secondaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.Movie,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(34.dp),
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = video.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = video.projectName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.74f),
                )
                if (video.notes.isNotBlank()) {
                    Text(
                        text = video.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.64f),
                    )
                }
            }
        }

        Row(
            modifier = Modifier.padding(top = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            HomeBadge(
                text = video.projectName,
                icon = Icons.Default.Movie,
                isActive = true,
                accent = InstagramOrange,
            )
            HomeBadge(
                text = if (video.supportsInlinePlayback) "Direkt" else "Extern",
                icon = if (video.supportsInlinePlayback) Icons.Default.PlayArrow else Icons.Default.Language,
                isActive = video.supportsInlinePlayback,
                accent = InstagramOrange,
            )
        }

        Text(
            text = if (video.supportsInlinePlayback) {
                "Der Clip liegt direkt im Hub bereit."
            } else {
                "Dieser Clip oeffnet ueber einen externen Link."
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.64f),
            modifier = Modifier.padding(top = 14.dp),
        )

        if (video.usesEmbeddedPreview) {
            ExternalVideoWebPlayer(
                url = video.inlineEmbedUrl,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .padding(top = 14.dp)
                    .clip(MaterialTheme.shapes.extraLarge),
            )
        } else if (video.downloadUrl.isNotBlank()) {
            AndroidView(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .padding(top = 14.dp)
                    .clip(MaterialTheme.shapes.extraLarge),
                factory = { playerContext ->
                    PlayerView(playerContext).apply {
                        useController = true
                        this.player = player
                    }
                },
                update = { view ->
                    view.player = player
                },
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .padding(top = 14.dp)
                    .clip(MaterialTheme.shapes.extraLarge)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.58f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Dieser Clip wird ueber einen externen Link geoeffnet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                )
            }
        }

        if (video.downloadUrl.isNotBlank()) {
            HomeMediaActionButton(
                label = if (isPlaying) "Stop" else "Play",
                isActive = isPlaying,
                accent = InstagramOrange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp),
                onClick = { onPlayToggle(video) },
            )
        }

        if (video.openUrl.isNotBlank()) {
            BrandActionButton(
                text = "Original oeffnen",
                onClick = { openExternalLink(context, video.openUrl) },
                accent = InstagramOrange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                icon = Icons.Default.Language,
                filled = false,
            )
        }
    }
}

@Composable
private fun HomeMediaActionButton(
    label: String,
    isActive: Boolean,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    BrandActionButton(
        text = label,
        onClick = onClick,
        accent = accent,
        modifier = modifier,
        icon = if (isActive) Icons.Default.Pause else Icons.Default.PlayArrow,
        filled = isActive,
    )
}

@Composable
private fun HomeStoryCard(
    onOpenBeatHub: () -> Unit,
    onOpenNicma: () -> Unit,
) {
    val context = LocalContext.current
    SkydownCard(contentPadding = PaddingValues(SkydownUiTokens.cardPadding)) {
        HomeSectionBanner(
            title = "Quick Links",
            subtitle = "Die wichtigsten Wege zu Beats, Studio und Kontakt.",
            icon = Icons.Default.AutoAwesome,
            accent = ArenaRed,
            tag = "LINKS",
        )

        Text(
            text = "Die wichtigsten Wege sind hier gebuendelt und sofort erreichbar.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.74f),
            modifier = Modifier.padding(top = 8.dp),
        )

        Column(
            modifier = Modifier.padding(top = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            HomeStoryLinkButton(
                title = "Yang D. Nash",
                icon = Icons.Default.Person,
                brand = HomeStoryBrand.League,
                subtitle = homePrimaryContactLink.subtitle,
                onClick = { openExternalLink(context, homePrimaryContactLink.url) },
            )

            HomeLaneSection(
                title = "Music Links",
                subtitle = "Releases, Beats und Studio.",
                accent = ArenaGold,
            ) {
                homeZweizweiSocialLinks.forEach { link ->
                    HomeStoryLinkButton(
                        title = link.title,
                        icon = Icons.Default.MusicNote,
                        brand = HomeStoryBrand.Instagram,
                        subtitle = link.subtitle,
                        onClick = { openExternalLink(context, link.url) },
                    )
                }

                HomeStoryLinkButton(
                    title = "Beats",
                    icon = Icons.Default.GraphicEq,
                    onClick = onOpenBeatHub,
                )

                HomeStoryLinkButton(
                    title = "Studio",
                    icon = Icons.Default.AutoAwesome,
                    onClick = onOpenNicma,
                )
            }

            HomeLaneSection(
                title = "Video Links",
                subtitle = "Clips & Mail.",
                accent = InstagramOrange,
            ) {
                HomeStoryLinkButton(
                    title = "Instagram",
                    icon = Icons.Default.Movie,
                    brand = HomeStoryBrand.Instagram,
                    subtitle = "@skydown_entertainment",
                    onClick = {
                        openExternalLink(context, "https://www.instagram.com/skydown_entertainment/")
                    },
                )

                HomeStoryLinkButton(
                    title = "E-Mail",
                    icon = Icons.Default.Email,
                    subtitle = "skydownent@gmail.com",
                    onClick = {
                        openEmailDraft(
                            context = context,
                            recipients = listOf("skydownent@gmail.com"),
                            subject = "Video Anfrage",
                            body = "",
                        )
                    },
                )
            }
        }
    }
}

private enum class HomeStoryBrand {
    Neutral,
    League,
    Instagram,
}

@Composable
private fun HomeStoryLinkButton(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    brand: HomeStoryBrand = HomeStoryBrand.Neutral,
    subtitle: String? = null,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val brandGradient = when (brand) {
        HomeStoryBrand.Neutral -> Brush.linearGradient(
            colors = listOf(
                MaterialTheme.colorScheme.surface.copy(alpha = 0.99f),
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.76f),
                MaterialTheme.colorScheme.primary.copy(alpha = 0.04f),
            ),
        )
        HomeStoryBrand.League -> Brush.linearGradient(
            colors = listOf(
                DexBlueDeep.copy(alpha = 0.96f),
                ArenaRed.copy(alpha = 0.72f),
                ArenaGold.copy(alpha = 0.54f),
            ),
        )
        HomeStoryBrand.Instagram -> Brush.linearGradient(
            colors = listOf(
                MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
                InstagramPurple.copy(alpha = 0.16f),
                InstagramPink.copy(alpha = 0.13f),
                InstagramOrange.copy(alpha = 0.10f),
            ),
        )
    }
    val iconTint = when (brand) {
        HomeStoryBrand.Neutral -> MaterialTheme.colorScheme.primary
        HomeStoryBrand.League -> Color.White
        HomeStoryBrand.Instagram -> Color.White
    }
    val iconBackground = when (brand) {
        HomeStoryBrand.Neutral -> Brush.linearGradient(
            colors = listOf(
                MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
            ),
        )
        HomeStoryBrand.League -> Brush.linearGradient(
            colors = listOf(
                ArenaRed,
                ArenaGold,
            ),
        )
        HomeStoryBrand.Instagram -> Brush.linearGradient(
            colors = listOf(
                InstagramPurple,
                InstagramPink,
                InstagramOrange,
            ),
        )
    }
    val borderColor = when (brand) {
        HomeStoryBrand.Neutral -> MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
        HomeStoryBrand.League -> ArenaGold.copy(alpha = 0.32f)
        HomeStoryBrand.Instagram -> InstagramPink.copy(alpha = 0.22f)
    }
    val titleColor = when (brand) {
        HomeStoryBrand.Neutral -> MaterialTheme.colorScheme.onSurface
        HomeStoryBrand.League -> Color.White
        HomeStoryBrand.Instagram -> MaterialTheme.colorScheme.onSurface
    }
    val subtitleColor = when (brand) {
        HomeStoryBrand.Neutral -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f)
        HomeStoryBrand.League -> Color.White.copy(alpha = 0.76f)
        HomeStoryBrand.Instagram -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f)
    }
    val content: @Composable () -> Unit = {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(iconBackground)
                    .padding(10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            )
            {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = titleColor,
                    fontWeight = FontWeight.SemiBold,
                )
                subtitle?.let { detail ->
                    Text(
                        text = detail,
                        style = MaterialTheme.typography.bodySmall,
                        color = subtitleColor,
                    )
                }
            }
        }
    }

    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .skydownPressable(interactionSource),
        interactionSource = interactionSource,
        shape = RoundedCornerShape(16.dp),
        color = androidx.compose.ui.graphics.Color.Transparent,
        tonalElevation = 4.dp,
        shadowElevation = 5.dp,
        border = BorderStroke(
            1.dp,
            borderColor,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(brandGradient)
                .padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            content()
        }
    }
}

@Composable
private fun HomeLaneSection(
    title: String,
    subtitle: String,
    accent: Color = MaterialTheme.colorScheme.primary,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.86f),
                        accent.copy(alpha = 0.08f),
                        DexBlue.copy(alpha = 0.05f),
                    ),
                ),
            )
            .border(
                width = 1.dp,
                color = accent.copy(alpha = 0.14f),
                shape = RoundedCornerShape(18.dp),
            )
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = accent,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
        )
        content()
    }
}

@Composable
private fun HomeBadge(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isActive: Boolean,
    accent: Color,
) {
    BrandStatusChip(
        text = text,
        accent = accent,
        icon = icon,
        isActive = isActive,
    )
}

private fun homeTrackAudioKey(track: com.skydown.shared.model.Track): String = "track:${track.trackId}"

private fun homeBeatAudioKey(beat: FeaturedBeatHighlight): String = "beat:${beat.id}"

private fun homeTrackedSignalCount(uiState: HomeUiState): Int = listOf(
    uiState.featuredTrack,
    uiState.featuredBeat,
    uiState.featuredVideo,
).count { it != null }

private fun homeHasSpotifyTarget(track: com.skydown.shared.model.Track): Boolean {
    return homeResolvedSpotifyTrackId(track) != null ||
        homeResolvedSpotifyArtistId(track) != null ||
        !track.externalUrl.isNullOrBlank()
}

private fun homeSpotifyActionLabel(track: com.skydown.shared.model.Track): String {
    return when {
        homeResolvedSpotifyTrackId(track) != null -> "Song auf Spotify"
        homeResolvedSpotifyArtistId(track) != null -> "Artist auf Spotify"
        else -> "Auf Spotify ansehen"
    }
}

private fun homeResolvedSpotifyTrackId(track: com.skydown.shared.model.Track): String? {
    if (!track.spotifyTrackId.isNullOrBlank()) return track.spotifyTrackId
    val externalUrl = track.externalUrl ?: return null
    val parsed = Uri.parse(externalUrl)
    val pathSegments = parsed.pathSegments
    val trackIndex = pathSegments.indexOf("track")
    if (trackIndex == -1 || trackIndex + 1 >= pathSegments.size) return null
    return pathSegments[trackIndex + 1]
}

private fun homeResolvedSpotifyArtistId(track: com.skydown.shared.model.Track): String? {
    if (!track.spotifyArtistId.isNullOrBlank()) return track.spotifyArtistId
    val externalUrl = track.externalUrl ?: return null
    val parsed = Uri.parse(externalUrl)
    val pathSegments = parsed.pathSegments
    val artistIndex = pathSegments.indexOf("artist")
    if (artistIndex == -1 || artistIndex + 1 >= pathSegments.size) return null
    return pathSegments[artistIndex + 1]
}

private data class HomeSocialLink(
    val title: String,
    val subtitle: String,
    val url: String,
)

private val homePrimaryContactLink = HomeSocialLink(
    title = "Yang D. Nash • Ansprechpartner",
    subtitle = "@y.d.nash",
    url = "https://www.instagram.com/y.d.nash/",
)

private val homeZweizweiSocialLinks = listOf(
    HomeSocialLink(
        title = "Zweizwei Music",
        subtitle = "@zweizwei_music",
        url = "https://www.instagram.com/zweizwei_music/",
    ),
    HomeSocialLink(
        title = "JANNO",
        subtitle = "@janno_official_",
        url = "https://www.instagram.com/janno_official_/",
    ),
    HomeSocialLink(
        title = "ThaDude",
        subtitle = "@thadude_offizielle",
        url = "https://www.instagram.com/thadude_offizielle/",
    ),
    HomeSocialLink(
        title = "MAVE",
        subtitle = "@mave__official",
        url = "https://www.instagram.com/mave__official/",
    ),
    HomeSocialLink(
        title = "TANGAJOE007",
        subtitle = "@tangajoe007",
        url = "https://www.instagram.com/tangajoe007/",
    ),
)

private const val homeDestinationBeatHub = "home_beat_hub"
private const val homeDestinationNicmaProducer = "home_nicma_producer"
private const val homeSignalTotal = 3
