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
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingBag
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import java.util.Calendar
import com.skydown.android.data.AppContainer
import com.skydown.android.data.ExternalMediaProvider
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
import com.skydown.android.ui.component.BrandPreviewFrame
import com.skydown.android.ui.component.BrandSectionBanner
import com.skydown.android.ui.component.BrandPill
import com.skydown.android.ui.component.BrandStatusChip
import com.skydown.android.ui.component.ExternalVideoWebPlayer
import com.skydown.android.ui.component.OriginalVideoViewerDialog
import com.skydown.android.ui.component.SkydownHapticKind
import com.skydown.android.ui.component.SkydownCard
import com.skydown.android.ui.component.SkydownMotionTokens
import com.skydown.android.ui.component.SkydownTopBarTitle
import com.skydown.android.ui.component.SkydownUiTokens
import com.skydown.android.ui.component.performSkydownHaptic
import com.skydown.android.ui.component.rememberSkydownScreenSectionSpacing
import com.skydown.android.ui.component.skydownPressable
import com.skydown.android.ui.component.skydownContentPadding
import com.skydown.android.ui.component.skydownScreenBrush
import com.skydown.android.ui.component.skydownTopBarColors
import com.skydown.android.ui.model.FeaturedBeatHighlight
import com.skydown.android.ui.model.FeaturedVideoHighlight
import com.skydown.android.ui.model.HomeUiState
import com.skydown.android.ui.theme.InstagramOrange
import com.skydown.android.ui.theme.InstagramPink
import com.skydown.android.ui.theme.InstagramPurple
import com.skydown.android.ui.theme.SpotifyGreen
import com.skydown.android.ui.theme.skydownAccent
import com.skydown.android.ui.theme.skydownAccentHighlight
import com.skydown.android.ui.theme.skydownAccentMystic
import com.skydown.android.ui.theme.skydownCardBackground
import com.skydown.android.ui.theme.skydownCinematicShadow
import com.skydown.android.ui.theme.skydownIsDarkPalette
import com.skydown.android.ui.theme.skydownSecondaryBackground
import com.skydown.android.ui.theme.skydownSpotify
import com.skydown.android.ui.viewmodel.HomeViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
    var selectedVideoHubId by rememberSaveable { mutableStateOf<String?>(null) }

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

    if (activeDestination == homeDestinationVideoHub) {
        VideoHubScreen(
            onBack = {
                activeDestination = null
                selectedVideoHubId = null
            },
            initialSelectedVideoId = selectedVideoHubId,
            autoplayInitialSelection = true,
        )
        return
    }

    val context = LocalContext.current
    val mediaContext = remember(context) { context.mediaAttributionContext() }
    var inAppOriginalVideoUrl by rememberSaveable { mutableStateOf<String?>(null) }
    var inAppOriginalVideoTitle by rememberSaveable { mutableStateOf("Original") }
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
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

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
    val heroPriorityTarget = homeHeroPriorityTarget(
        hasTrackSignal = uiState.featuredTrack != null,
        hasBeatSignal = uiState.featuredBeat != null,
        hasVideoSignal = uiState.featuredVideo != null,
    )
    val homeGreetingTitle = homeGreetingTitle()
    val homeGreetingSubtitle = homeGreetingSubtitle(activeSignalCount, homeSignalTotal)
    val homeGreetingDetail = "$activeSignalCount / $homeSignalTotal live · Daily Ops bereit"
    val colorScheme = MaterialTheme.colorScheme
    val homeAccent = colorScheme.skydownAccent()
    val homeMysticAccent = colorScheme.skydownAccentMystic()
    val homeHighlightAccent = colorScheme.skydownAccentHighlight()
    val homeSpotifyAccent = colorScheme.skydownSpotify()
    val heroPillTint: (String) -> Color = { target ->
        val base = when (target) {
            "track" -> homeSpotifyAccent
            "beat" -> homeMysticAccent
            else -> homeHighlightAccent
        }
        if (heroPriorityTarget == target) base else base.copy(alpha = 0.66f)
    }
    val sectionSpacing = rememberSkydownScreenSectionSpacing()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    SkydownTopBarTitle(
                        "SkyOS",
                        accent = homeAccent,
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
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.68f),
                            shape = RoundedCornerShape(999.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
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
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    skydownScreenBrush(
                        primaryColor = homeAccent,
                        secondaryColor = homeSpotifyAccent,
                    ),
                ),
        ) {
            HomeMapBackdrop(
                modifier = Modifier.fillMaxSize(),
            )
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = listState,
                contentPadding = skydownContentPadding(innerPadding),
                verticalArrangement = Arrangement.spacedBy(sectionSpacing),
            ) {
                item {
                    HomeAnimatedItem(order = 0) {
                        BrandHeroCard(
                            eyebrow = screenHeaderSettings.homeEyebrow.ifBlank { "SKY OS" },
                            title = screenHeaderSettings.homeTitle.ifBlank { homeGreetingTitle },
                            subtitle = screenHeaderSettings.homeSubtitle.ifBlank { homeGreetingSubtitle },
                            detail = screenHeaderSettings.homeDetail.ifBlank { homeGreetingDetail },
                            backgroundImageUrl = screenHeaderSettings.homeImageUrl.ifBlank { null },
                            accent = homeAccent,
                            secondaryAccent = homeMysticAccent,
                            marks = listOf(BrandArtwork.Combined),
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                BrandPill(
                                    text = if (heroPriorityTarget == "track") {
                                        if (uiState.featuredTrack == null) "Next: Musik laden" else "Next: Musik"
                                    } else {
                                        if (uiState.featuredTrack == null) "Musik laedt" else "Musik live"
                                    },
                                    tint = heroPillTint("track"),
                                    onClick = {
                                        coroutineScope.launch {
                                            listState.animateScrollToItem(homeMediaClusterSectionIndex)
                                        }
                                    },
                                )
                                BrandPill(
                                    text = if (heroPriorityTarget == "beat") {
                                        if (uiState.featuredBeat == null) "Next: Beats laden" else "Next: Beats"
                                    } else {
                                        if (uiState.featuredBeat == null) "Beats laden" else "Beats live"
                                    },
                                    tint = heroPillTint("beat"),
                                    onClick = {
                                        coroutineScope.launch {
                                            listState.animateScrollToItem(homeMediaClusterSectionIndex)
                                        }
                                    },
                                )
                                BrandPill(
                                    text = if (heroPriorityTarget == "video") {
                                        if (uiState.featuredVideo == null) "Next: Visual laden" else "Next: Visual"
                                    } else {
                                        if (uiState.featuredVideo == null) "Video laedt" else "Video live"
                                    },
                                    tint = heroPillTint("video"),
                                    onClick = {
                                        coroutineScope.launch {
                                            listState.animateScrollToItem(homeMediaClusterSectionIndex)
                                        }
                                    },
                                )
                            }
                        }
                    }
                }

                item {
                    HomeAnimatedItem(order = 1) {
                        HomeDailyOpsStrip(
                            activeSignalCount = activeSignalCount,
                            totalSignalCount = homeSignalTotal,
                            hasTrackSignal = uiState.featuredTrack != null,
                            hasBeatSignal = uiState.featuredBeat != null,
                            hasVideoSignal = uiState.featuredVideo != null,
                            onRefresh = viewModel::refresh,
                            onOpenRelease = {
                                coroutineScope.launch { listState.animateScrollToItem(homeMediaClusterSectionIndex) }
                            },
                            onOpenBeat = {
                                coroutineScope.launch { listState.animateScrollToItem(homeMediaClusterSectionIndex) }
                            },
                            onOpenVideo = {
                                coroutineScope.launch { listState.animateScrollToItem(homeMediaClusterSectionIndex) }
                            },
                        )
                    }
                }

                item {
                    HomeAnimatedItem(order = 2) {
                        HomeCommandDockStrip(
                            priorityTarget = heroPriorityTarget,
                            onOpenWorkflow = onOpenWorkflow,
                            onOpenCart = onOpenCart,
                            onOpenSettings = onOpenSettings,
                        )
                    }
                }

                item {
                    HomeAnimatedItem(order = 3) {
                        HomeUtilityRow(
                            onOpenAi = { onOpenWorkflow?.invoke() ?: onOpenSettings() },
                            onOpenMusic = { coroutineScope.launch { listState.animateScrollToItem(homeMediaClusterSectionIndex) } },
                            onOpenCreate = { activeDestination = homeDestinationNicmaProducer },
                            onOpenOrders = onOpenCart,
                            onOpenSearch = { coroutineScope.launch { listState.animateScrollToItem(homeMediaClusterSectionIndex) } },
                            onOpenSettings = onOpenSettings,
                        )
                    }
                }

                item {
                    HomeAnimatedItem(order = 4) {
                        HomeStoryCard(
                            onOpenBeatHub = { activeDestination = homeDestinationBeatHub },
                            onOpenNicma = { activeDestination = homeDestinationNicmaProducer },
                        )
                    }
                }

                item {
                    HomeAnimatedItem(order = 5) {
                        HomeLiveSignalSurface(
                            hasTrackSignal = uiState.featuredTrack != null,
                            hasBeatSignal = uiState.featuredBeat != null,
                            hasVideoSignal = uiState.featuredVideo != null,
                            trackName = uiState.featuredTrack?.trackName,
                            beatName = uiState.featuredBeat?.title,
                            videoName = uiState.featuredVideo?.title,
                            priorityTarget = heroPriorityTarget,
                            aiUsageWarning = uiState.aiUsageWarning,
                            creatorLimitZone = uiState.creatorLimitZone,
                            agentRunning = uiState.agentRunning,
                            workflowWaiting = uiState.workflowWaiting,
                            newOrderHint = uiState.commerceSignal,
                            syncPaused = uiState.syncPaused,
                            recoverableError = uiState.recoverableError,
                            contentSignal = uiState.contentSignal,
                        )
                    }
                }

                item {
                    HomeAnimatedItem(order = 6) {
                        HomeMediaCluster(
                            uiState = uiState,
                            isTrackPlaying = currentAudioKey == uiState.featuredTrack?.let(::homeTrackAudioKey),
                            isBeatPlaying = currentAudioKey == uiState.featuredBeat?.let(::homeBeatAudioKey),
                            isVideoPlaying = currentVideoId == uiState.featuredVideo?.id,
                            player = videoPlayer,
                            onPlayTrackToggle = { track ->
                                val previewUrl = track.previewUrl
                                if (previewUrl.isNullOrBlank()) return@HomeMediaCluster
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
                            onPlayBeatToggle = { beat ->
                                if (!beat.isPlayable || beat.downloadUrl.isBlank()) return@HomeMediaCluster
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
                            onPlayVideoToggle = { video ->
                                if (video.downloadUrl.isBlank()) return@HomeMediaCluster
                                if (currentVideoId == video.id) {
                                    videoPlayer.pause()
                                    videoPlayer.seekTo(0)
                                    currentVideoId = null
                                } else {
                                    audioPlayer.stop()
                                    audioPlayer.clearMediaItems()
                                    currentAudioKey = null
                                    videoPlayer.setMediaItem(MediaItem.fromUri(video.downloadUrl))
                                    videoPlayer.prepare()
                                    videoPlayer.play()
                                    currentVideoId = video.id
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
                            onOpenVideoHub = { video ->
                                audioPlayer.stop()
                                audioPlayer.clearMediaItems()
                                currentAudioKey = null
                                videoPlayer.pause()
                                videoPlayer.seekTo(0)
                                currentVideoId = null
                                selectedVideoHubId = video.id
                                activeDestination = homeDestinationVideoHub
                            },
                            onOpenOriginal = { video ->
                                val originalUrl = video.openUrl.trim().ifBlank { video.inlineEmbedUrl.trim() }
                                if (originalUrl.isBlank()) return@HomeMediaCluster
                                videoPlayer.pause()
                                videoPlayer.seekTo(0)
                                currentVideoId = null
                                inAppOriginalVideoTitle = video.title.ifBlank { "Original" }
                                inAppOriginalVideoUrl = originalUrl
                            },
                        )
                    }
                }

                item {
                    HomeAnimatedItem(order = 8) {
                        Text(
                            text = "SkyOS Home fuehrt ruhig: Signal lesen, Fokus setzen, naechsten Schritt ausfuehren.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.66f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 6.dp),
                        )
                    }
                }
            }

            inAppOriginalVideoUrl?.let { url ->
                OriginalVideoViewerDialog(
                    url = url,
                    title = inAppOriginalVideoTitle,
                    onDismiss = { inAppOriginalVideoUrl = null },
                )
            }
        }
    }
}

@Composable
private fun HomeUtilityRow(
    onOpenAi: () -> Unit,
    onOpenMusic: () -> Unit,
    onOpenCreate: () -> Unit,
    onOpenOrders: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    val view = LocalView.current
    data class UtilityItem(
        val label: String,
        val icon: ImageVector,
        val iconOnly: Boolean,
        val action: () -> Unit,
    )
    val items = listOf(
        UtilityItem("AI", Icons.Default.AutoAwesome, false, onOpenAi),
        UtilityItem("Music", Icons.Default.MusicNote, false, onOpenMusic),
        UtilityItem("Create", Icons.Default.CheckCircle, false, onOpenCreate),
        UtilityItem("Orders", Icons.Default.ShoppingBag, false, onOpenOrders),
        UtilityItem("Search", Icons.Default.Search, true, onOpenSearch),
        UtilityItem("Settings", Icons.Default.Settings, true, onOpenSettings),
    )
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colorScheme.surface.copy(alpha = 0.32f))
            .border(1.dp, colorScheme.primary.copy(alpha = 0.10f), RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(items) { item ->
            OutlinedButton(
                onClick = {
                    view.performSkydownHaptic(SkydownHapticKind.Selection)
                    item.action()
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = colorScheme.surface.copy(alpha = 0.22f),
                ),
            ) {
                if (item.iconOnly) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label,
                        modifier = Modifier.size(18.dp),
                    )
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = item.icon, contentDescription = null, modifier = Modifier.size(16.dp))
                        Text(item.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeLiveSignalSurface(
    hasTrackSignal: Boolean,
    hasBeatSignal: Boolean,
    hasVideoSignal: Boolean,
    trackName: String?,
    beatName: String?,
    videoName: String?,
    priorityTarget: String,
    aiUsageWarning: String? = null,
    creatorLimitZone: Boolean? = null,
    agentRunning: Boolean? = null,
    workflowWaiting: Boolean? = null,
    newOrderHint: String? = null,
    syncPaused: Boolean? = null,
    recoverableError: String? = null,
    contentSignal: String? = null,
) {
    val missingCount = listOf(hasTrackSignal, hasBeatSignal, hasVideoSignal).count { !it }
    val nowText = when {
        hasTrackSignal && !trackName.isNullOrBlank() -> "Now: Music live - $trackName"
        hasBeatSignal && !beatName.isNullOrBlank() -> "Now: Beat live - $beatName"
        hasVideoSignal && !videoName.isNullOrBlank() -> "Now: Visual live - $videoName"
        else -> "Now: Noch kein Kernsignal live."
    }
    val nextText = when (priorityTarget) {
        "track" -> "Next: Musik-Status finalisieren."
        "beat" -> "Next: Beat-Signal finalisieren."
        else -> "Next: Visual-Signal finalisieren."
    }
    val riskText = if (missingCount > 0) {
        "Risk: $missingCount Kernsignal(e) fehlen aktuell."
    } else {
        null
    }
    val federatedSignals = buildList {
        aiUsageWarning?.takeIf { it.isNotBlank() }?.let { add("AI: $it") }
        if (creatorLimitZone == true) add("AI: Creator limit zone reached.")
        if (agentRunning == true) add("AI: Agent currently running.")
        if (workflowWaiting == true) add("AI: Workflow waiting for next step.")
        newOrderHint?.takeIf { it.isNotBlank() }?.let { add("Commerce: $it") }
        if (syncPaused == true) add("System: Sync currently paused.")
        recoverableError?.takeIf { it.isNotBlank() }?.let { add("System: $it") }
        contentSignal?.takeIf { it.isNotBlank() }?.let { add("Content: $it") }
    }
    val colorScheme = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(colorScheme.surfaceVariant.copy(alpha = 0.30f))
            .border(1.dp, colorScheme.primary.copy(alpha = 0.10f), RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "Live Signals",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = colorScheme.onSurface.copy(alpha = 0.84f),
        )
        Text(nowText, style = MaterialTheme.typography.bodySmall, color = colorScheme.onSurface.copy(alpha = 0.78f))
        Text(nextText, style = MaterialTheme.typography.bodySmall, color = colorScheme.onSurface.copy(alpha = 0.74f))
        riskText?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = colorScheme.onSurface.copy(alpha = 0.70f))
        }
        federatedSignals.takeIf { it.isNotEmpty() }?.forEach { signal ->
            Text(
                text = signal,
                style = MaterialTheme.typography.bodySmall,
                color = colorScheme.onSurface.copy(alpha = 0.68f),
            )
        }
    }
}

@Composable
private fun HomeMediaCluster(
    uiState: HomeUiState,
    isTrackPlaying: Boolean,
    isBeatPlaying: Boolean,
    isVideoPlaying: Boolean,
    player: ExoPlayer,
    onPlayTrackToggle: (com.skydown.shared.model.Track) -> Unit,
    onPlayBeatToggle: (FeaturedBeatHighlight) -> Unit,
    onPlayVideoToggle: (FeaturedVideoHighlight) -> Unit,
    onOpenSpotify: (com.skydown.shared.model.Track) -> Unit,
    onOpenVideoHub: (FeaturedVideoHighlight) -> Unit,
    onOpenOriginal: (FeaturedVideoHighlight) -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        HomeLatestReleaseCard(
            uiState = uiState,
            isPlaying = isTrackPlaying,
            onPlayToggle = onPlayTrackToggle,
            onOpenSpotify = onOpenSpotify,
        )
        HomeLatestBeatCard(
            uiState = uiState,
            isPlaying = isBeatPlaying,
            onPlayToggle = onPlayBeatToggle,
        )
        HomeLatestVideoCard(
            uiState = uiState,
            isPlaying = isVideoPlaying,
            player = player,
            onPlayToggle = onPlayVideoToggle,
            onOpenVideoHub = onOpenVideoHub,
            onOpenOriginal = onOpenOriginal,
        )
    }
}

@Composable
private fun HomeDailyOpsStrip(
    activeSignalCount: Int,
    totalSignalCount: Int,
    hasTrackSignal: Boolean,
    hasBeatSignal: Boolean,
    hasVideoSignal: Boolean,
    onRefresh: () -> Unit,
    onOpenRelease: () -> Unit,
    onOpenBeat: () -> Unit,
    onOpenVideo: () -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    val view = LocalView.current
    val currentHour = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }
    val priorityTarget = when {
        !hasTrackSignal -> "music"
        !hasBeatSignal -> "beats"
        !hasVideoSignal -> "visuals"
        currentHour in 5..11 -> "music"
        currentHour in 12..17 -> "beats"
        else -> "visuals"
    }
    val priorityAccent = when (priorityTarget) {
        "music" -> colorScheme.skydownSpotify()
        "beats" -> colorScheme.secondary
        else -> colorScheme.skydownAccentHighlight()
    }
    val priorityTitle = when (priorityTarget) {
        "music" -> if (hasTrackSignal) "Jetzt wichtig: Musik" else "Jetzt wichtig: Musik herstellen"
        "beats" -> if (hasBeatSignal) "Jetzt wichtig: Beats" else "Jetzt wichtig: Beats herstellen"
        else -> if (hasVideoSignal) "Jetzt wichtig: Visuals" else "Jetzt wichtig: Visuals herstellen"
    }
    val priorityHint = when (priorityTarget) {
        "music" -> if (hasTrackSignal) "Morgens zuerst Musik-Status checken." else "Musik ist noch nicht live."
        "beats" -> if (hasBeatSignal) "Tagsueber zuerst Beats fokussieren." else "Beats-Signal fehlt noch."
        else -> if (hasVideoSignal) "Abends zuerst Visuals-Status pruefen." else "Visuals-Signal fehlt noch."
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colorScheme.skydownSecondaryBackground().copy(alpha = 0.72f))
            .border(
                width = 1.dp,
                color = priorityAccent.copy(alpha = 0.18f),
                shape = RoundedCornerShape(16.dp),
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = priorityAccent,
                modifier = Modifier.size(14.dp),
            )
            Text(
                text = "Priority Layer",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "$activeSignalCount/$totalSignalCount live",
                style = MaterialTheme.typography.labelSmall,
                color = priorityAccent,
                fontWeight = FontWeight.SemiBold,
            )
        }

        Text(
            text = "Ein Fokus. Ein klarer naechster Schritt.",
            style = MaterialTheme.typography.bodySmall,
            color = colorScheme.onSurface.copy(alpha = 0.74f),
        )

        BrandActionButton(
            text = priorityTitle,
            onClick = {
                view.performSkydownHaptic(SkydownHapticKind.Selection)
                when (priorityTarget) {
                    "music" -> onOpenRelease()
                    "beats" -> onOpenBeat()
                    else -> onOpenVideo()
                }
            },
            accent = priorityAccent,
            compact = true,
            filled = true,
            modifier = Modifier.fillMaxWidth(),
            icon = Icons.Default.AutoAwesome,
        )

        Text(
            text = priorityHint,
            style = MaterialTheme.typography.bodySmall,
            color = colorScheme.onSurface.copy(alpha = 0.70f),
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BrandStatusChip(
                text = if (hasTrackSignal) "Music live" else "Music missing",
                accent = colorScheme.skydownSpotify(),
                isActive = hasTrackSignal,
            )
            BrandStatusChip(
                text = if (hasBeatSignal) "Beats live" else "Beats missing",
                accent = colorScheme.secondary,
                isActive = hasBeatSignal,
            )
            BrandStatusChip(
                text = if (hasVideoSignal) "Visuals live" else "Visuals missing",
                accent = colorScheme.skydownAccentHighlight(),
                isActive = hasVideoSignal,
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BrandActionButton(
                text = "Musik",
                onClick = {
                    view.performSkydownHaptic(SkydownHapticKind.Selection)
                    onOpenRelease()
                },
                accent = colorScheme.skydownSpotify(),
                compact = true,
                filled = false,
            )
            BrandActionButton(
                text = "Beats",
                onClick = {
                    view.performSkydownHaptic(SkydownHapticKind.Selection)
                    onOpenBeat()
                },
                accent = colorScheme.secondary,
                compact = true,
                filled = false,
            )
            BrandActionButton(
                text = "Visuals",
                onClick = {
                    view.performSkydownHaptic(SkydownHapticKind.Selection)
                    onOpenVideo()
                },
                accent = colorScheme.skydownAccentHighlight(),
                compact = true,
                filled = false,
            )
            BrandActionButton(
                text = "Refresh",
                onClick = {
                    view.performSkydownHaptic(SkydownHapticKind.Success)
                    onRefresh()
                },
                accent = priorityAccent,
                icon = Icons.Default.Refresh,
                compact = true,
                filled = false,
            )
        }
    }
}

@Composable
private fun HomeCommandDockStrip(
    priorityTarget: String,
    onOpenWorkflow: (() -> Unit)?,
    onOpenCart: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    val view = LocalView.current
    val priorityAccent = when (priorityTarget) {
        "track" -> colorScheme.skydownSpotify()
        "beat" -> colorScheme.skydownAccentMystic()
        else -> colorScheme.skydownAccentHighlight()
    }
    val actionAccent: (String) -> Color = { target ->
        val base = when (target) {
            "track" -> colorScheme.skydownSpotify()
            "beat" -> colorScheme.skydownAccentMystic()
            else -> colorScheme.skydownAccentHighlight()
        }
        if (target == priorityTarget) base else base.copy(alpha = 0.66f)
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(colorScheme.surface.copy(alpha = 0.34f))
            .border(
                width = 1.dp,
                color = priorityAccent.copy(alpha = 0.12f),
                shape = RoundedCornerShape(14.dp),
            )
            .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = priorityAccent,
                modifier = Modifier.size(14.dp),
            )
            Text(
                text = "Action Layer",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.weight(1f))
        }
        Text(
            text = "Schnelle Systemaktionen ohne Kontextwechsel.",
            style = MaterialTheme.typography.bodySmall,
            color = colorScheme.onSurface.copy(alpha = 0.74f),
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BrandActionButton(
                text = "KI-Agent",
                onClick = {
                    view.performSkydownHaptic(SkydownHapticKind.Selection)
                    onOpenWorkflow?.invoke()
                },
                accent = actionAccent("beat"),
                compact = true,
                filled = false,
                enabled = onOpenWorkflow != null,
            )
            BrandActionButton(
                text = "Warenkorb",
                onClick = {
                    view.performSkydownHaptic(SkydownHapticKind.Selection)
                    onOpenCart()
                },
                accent = actionAccent("track"),
                compact = true,
                filled = false,
            )
            BrandActionButton(
                text = "Settings",
                onClick = {
                    view.performSkydownHaptic(SkydownHapticKind.Selection)
                    onOpenSettings()
                },
                accent = actionAccent("video"),
                compact = true,
                filled = false,
            )
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
        delay(order.coerceAtMost(4) * SkydownMotionTokens.staggerStepMillis.toLong())
        visible = true
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + slideInVertically(initialOffsetY = { it / 12 }),
    ) {
        content()
    }
}

@Composable
private fun HomeMapBackdrop(
    modifier: Modifier = Modifier,
) {
    val colorScheme = MaterialTheme.colorScheme
    val isDarkPalette = colorScheme.skydownIsDarkPalette()
    Box(modifier = modifier) {
        HomeBackdropHalo(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(x = 168.dp, y = (-138).dp),
            haloSize = 280.dp,
            tint = colorScheme.skydownSpotify(),
            opacity = if (isDarkPalette) 0.09f else 0.07f,
        )
        HomeBackdropHalo(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(x = (-172).dp, y = 210.dp),
            haloSize = 240.dp,
            tint = colorScheme.skydownAccentMystic(),
            opacity = if (isDarkPalette) 0.10f else 0.08f,
        )
        HomeBackdropHalo(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(x = 154.dp, y = 498.dp),
            haloSize = 320.dp,
            tint = colorScheme.skydownAccentHighlight(),
            opacity = if (isDarkPalette) 0.08f else 0.06f,
        )
    }
}

@Composable
private fun HomeBackdropHalo(
    modifier: Modifier = Modifier,
    haloSize: androidx.compose.ui.unit.Dp,
    tint: Color,
    opacity: Float,
) {
    Box(
        modifier = modifier.size(haloSize),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(haloSize)
                .clip(CircleShape)
                .drawWithCache {
                    val haloBrush = Brush.radialGradient(
                        colors = listOf(
                            tint.copy(alpha = opacity),
                            tint.copy(alpha = opacity * 0.38f),
                            Color.Transparent,
                        ),
                        radius = minOf(size.width, size.height) * 0.5f,
                    )
                    onDrawBehind {
                        drawCircle(brush = haloBrush)
                    }
                },
        )
        listOf(1f, 0.78f, 0.56f).forEachIndexed { index, scale ->
            Box(
                modifier = Modifier
                    .size(haloSize * scale)
                    .border(
                        width = 1.dp,
                        color = tint.copy(alpha = opacity - (index * 0.02f)),
                        shape = CircleShape,
                    ),
            )
        }
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.62f))
            .border(
                width = 1.dp,
                color = accent.copy(alpha = 0.16f),
                shape = RoundedCornerShape(14.dp),
            )
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(14.dp),
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        tag?.takeIf { it.isNotBlank() }?.let { label ->
            BrandStatusChip(
                text = label,
                accent = accent,
                isActive = true,
            )
        }
    }
}

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
                    text = track.artistName ?: "22",
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

        if (beat.isPlayable && beat.downloadUrl.isNotBlank()) {
            HomeMediaActionButton(
                label = if (isPlaying) "Stoppen" else "Abspielen",
                isActive = isPlaying,
                accent = MaterialTheme.colorScheme.secondary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp),
                onClick = { onPlayToggle(beat) },
            )
        } else if (beat.openUrl.isNotBlank()) {
            BrandActionButton(
                text = beat.provider.originalVideoActionLabel,
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
    isPlaying: Boolean,
    player: ExoPlayer,
    onPlayToggle: (FeaturedVideoHighlight) -> Unit,
    onOpenVideoHub: (FeaturedVideoHighlight) -> Unit,
    onOpenOriginal: (FeaturedVideoHighlight) -> Unit,
) {
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

        Text(
            text = homeVideoModeDescription(video),
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
                        useController = false
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
                    text = video.originalDestinationDescription,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                )
            }
        }

        if (video.downloadUrl.isNotBlank()) {
            HomeMediaActionButton(
                label = if (isPlaying) "Stoppen" else "Abspielen",
                isActive = isPlaying,
                accent = InstagramOrange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp),
                onClick = { onPlayToggle(video) },
            )
        } else if (video.openUrl.isNotBlank() || video.inlineEmbedUrl.isNotBlank()) {
            HomeMediaActionButton(
                label = if (video.supportsInlinePlayback) "Video direkt oeffnen" else video.originalActionLabel,
                isActive = false,
                accent = InstagramOrange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp),
                onClick = { onOpenOriginal(video) },
            )
        } else if (video.supportsInlinePlayback) {
            HomeMediaActionButton(
                label = "Im Video ansehen",
                isActive = false,
                accent = InstagramOrange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp),
                onClick = { onOpenVideoHub(video) },
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
    val colorScheme = MaterialTheme.colorScheme
    SkydownCard(contentPadding = PaddingValues(SkydownUiTokens.cardPadding)) {
        HomeSectionBanner(
            title = "Community",
            subtitle = "Brand- und Artist-Profile klar gebuendelt auf Home.",
            icon = Icons.Default.AutoAwesome,
            accent = colorScheme.skydownAccent(),
            tag = "LINKS",
        )

        Text(
            text = "Social Reach first: direkte Wege zu Brand und Artists ohne Songs-Kontext.",
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
                brand = HomeStoryBrand.Core,
                subtitle = homePrimaryContactLink.subtitle,
                onClick = { openExternalLink(context, homePrimaryContactLink.url) },
            )

            HomeLaneSection(
                title = "Social Profiles",
                subtitle = "Brand und Artists.",
                accent = colorScheme.skydownSpotify(),
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
                title = "Support & Media",
                subtitle = "Instagram plus Kontakt.",
                accent = colorScheme.skydownAccentHighlight(),
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
    Core,
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
    val colorScheme = MaterialTheme.colorScheme
    val brandGradient = when (brand) {
        HomeStoryBrand.Neutral -> Brush.linearGradient(
            colors = listOf(
                colorScheme.skydownCardBackground().copy(alpha = 0.99f),
                colorScheme.skydownSecondaryBackground().copy(alpha = 0.76f),
                colorScheme.skydownAccent().copy(alpha = 0.04f),
            ),
        )
        HomeStoryBrand.Core -> Brush.linearGradient(
            colors = listOf(
                colorScheme.skydownCinematicShadow().copy(alpha = 0.96f),
                colorScheme.skydownAccentMystic().copy(alpha = 0.72f),
                colorScheme.skydownAccentHighlight().copy(alpha = 0.54f),
            ),
        )
        HomeStoryBrand.Instagram -> Brush.linearGradient(
            colors = listOf(
                colorScheme.skydownCardBackground().copy(alpha = 0.98f),
                InstagramPurple.copy(alpha = 0.16f),
                InstagramPink.copy(alpha = 0.13f),
                InstagramOrange.copy(alpha = 0.10f),
            ),
        )
    }
    val iconTint = when (brand) {
        HomeStoryBrand.Neutral -> colorScheme.skydownAccent()
        HomeStoryBrand.Core -> Color.White
        HomeStoryBrand.Instagram -> Color.White
    }
    val iconBackground = when (brand) {
        HomeStoryBrand.Neutral -> Brush.linearGradient(
            colors = listOf(
                colorScheme.skydownAccent().copy(alpha = 0.10f),
                colorScheme.skydownAccent().copy(alpha = 0.10f),
            ),
        )
        HomeStoryBrand.Core -> Brush.linearGradient(
            colors = listOf(
                colorScheme.skydownAccentMystic(),
                colorScheme.skydownAccentHighlight(),
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
        HomeStoryBrand.Neutral -> colorScheme.skydownAccent().copy(alpha = 0.08f)
        HomeStoryBrand.Core -> colorScheme.skydownAccentHighlight().copy(alpha = 0.32f)
        HomeStoryBrand.Instagram -> InstagramPink.copy(alpha = 0.22f)
    }
    val titleColor = when (brand) {
        HomeStoryBrand.Neutral -> MaterialTheme.colorScheme.onSurface
        HomeStoryBrand.Core -> Color.White
        HomeStoryBrand.Instagram -> MaterialTheme.colorScheme.onSurface
    }
    val subtitleColor = when (brand) {
        HomeStoryBrand.Neutral -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f)
        HomeStoryBrand.Core -> Color.White.copy(alpha = 0.76f)
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
    val colorScheme = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        colorScheme.skydownCardBackground().copy(alpha = 0.86f),
                        accent.copy(alpha = 0.08f),
                        colorScheme.skydownSecondaryBackground().copy(alpha = 0.34f),
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

private fun homeVideoModeDescription(video: FeaturedVideoHighlight): String {
    return when {
        video.usesEmbeddedPreview -> "Vorschau hier, ein Tap oeffnet den Clip direkt mit sichtbarem Rueckweg."
        video.supportsInlinePlayback -> "Ein Tap oeffnet den Clip direkt in der In-App-Ansicht."
        else -> video.originalDestinationDescription
    }
}

private fun homeTrackAudioKey(track: com.skydown.shared.model.Track): String = "track:${track.trackId}"

private fun homeBeatAudioKey(beat: FeaturedBeatHighlight): String = "beat:${beat.id}"

private fun homeTrackedSignalCount(uiState: HomeUiState): Int = listOf(
    uiState.featuredTrack,
    uiState.featuredBeat,
    uiState.featuredVideo,
).count { it != null }

private fun homeGreetingTitle(): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when (hour) {
        in 5..11 -> "Guten Morgen, Creator."
        in 12..17 -> "Guten Tag, Creator."
        else -> "Guten Abend, Creator."
    }
}

private fun homeGreetingSubtitle(activeSignalCount: Int, totalSignalCount: Int): String {
    val signalLine = if (activeSignalCount == totalSignalCount) {
        "Alle Kernsignale sind live."
    } else {
        "Nicht alle Kernsignale sind live."
    }
    return "Weitblick oben. Fokus unten. $signalLine"
}

private fun homeHeroPriorityTarget(
    hasTrackSignal: Boolean,
    hasBeatSignal: Boolean,
    hasVideoSignal: Boolean,
): String {
    if (!hasTrackSignal) return "track"
    if (!hasBeatSignal) return "beat"
    if (!hasVideoSignal) return "video"
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when (hour) {
        in 5..11 -> "track"
        in 12..17 -> "beat"
        else -> "video"
    }
}

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
        title = "22 Music",
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
private const val homeDestinationVideoHub = "home_video_hub"
private const val homeMediaClusterSectionIndex = 5
private const val homeSignalTotal = 3
