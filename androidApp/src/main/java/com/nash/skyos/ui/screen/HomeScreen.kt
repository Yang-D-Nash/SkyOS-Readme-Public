package com.nash.skyos.ui.screen

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import java.util.Calendar
import com.nash.skyos.R
import com.nash.skyos.data.AppContainer
import com.nash.skyos.data.ExternalMediaProvider
import com.nash.skyos.data.mediaAttributionContext
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.nash.skyos.ui.component.openTrackInSpotify
import coil3.compose.AsyncImage
import com.nash.skyos.ui.component.AppTopBarSessionActions
import com.nash.skyos.ui.component.BrandActionButton
import com.nash.skyos.ui.component.BrandArtwork
import com.nash.skyos.ui.component.BrandHeroCard
import com.nash.skyos.ui.component.BrandPreviewFrame
import com.nash.skyos.ui.component.BrandSectionBanner
import com.nash.skyos.ui.component.BrandPill
import com.nash.skyos.ui.component.BrandStatusChip
import com.nash.skyos.ui.component.ExternalVideoWebPlayer
import com.nash.skyos.ui.component.OriginalVideoViewerDialog
import com.nash.skyos.ui.component.SkydownHapticKind
import com.nash.skyos.ui.component.SkydownCard
import com.nash.skyos.ui.component.SkydownMotionTokens
import com.nash.skyos.ui.component.SkydownTopBarTitle
import com.nash.skyos.ui.component.SkydownUiTokens
import com.nash.skyos.ui.component.performSkydownHaptic
import com.nash.skyos.ui.component.rememberSkydownScreenSectionSpacing
import com.nash.skyos.ui.component.skydownPressable
import com.nash.skyos.ui.component.skydownContentPadding
import com.nash.skyos.ui.component.skydownAtmosphereBackground
import com.nash.skyos.ui.component.skydownTopBarColors
import com.nash.skyos.ui.model.FeaturedVideoHighlight
import com.nash.skyos.ui.model.HomeUiState
import com.nash.skyos.ui.theme.InstagramOrange
import com.nash.skyos.ui.theme.SpotifyGreen
import com.nash.skyos.ui.theme.skydownAccent
import com.nash.skyos.ui.theme.skydownAccentHighlight
import com.nash.skyos.ui.theme.skydownAccentMystic
import com.nash.skyos.ui.theme.skydownCardBackground
import com.nash.skyos.ui.theme.skydownIsDarkPalette
import com.nash.skyos.ui.theme.skydownSecondaryBackground
import com.nash.skyos.ui.theme.skydownSpotify
import com.nash.skyos.ui.viewmodel.HomeViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenCart: () -> Unit = {},
    onOpenProfile: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onGuestSignIn: (() -> Unit)? = null,
    onOpenWorkflow: (() -> Unit)? = null,
) {
    val app = LocalContext.current.applicationContext as Application
    val viewModel: HomeViewModel = viewModel(
        factory = remember(app) { HomeViewModel.provideFactory(app) },
    )
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
        hasVideoSignal = uiState.featuredVideo != null,
    )
    val homeGreetingTitle = stringResource(R.string.home_hero_title)
    val homeGreetingSubtitle = stringResource(R.string.home_hero_live_count, activeSignalCount, homeSignalTotal)
    val homeGreetingDetail = stringResource(R.string.home_hero_detail)
    val colorScheme = MaterialTheme.colorScheme
    val homeAccent = colorScheme.skydownAccent()
    val homeMysticAccent = colorScheme.skydownAccentMystic()
    val homeHighlightAccent = colorScheme.skydownAccentHighlight()
    val homeSpotifyAccent = colorScheme.skydownSpotify()
    val heroPillTint: (String) -> Color = { target ->
        val base = when (target) {
            "track" -> homeSpotifyAccent
            else -> homeHighlightAccent
        }
        if (heroPriorityTarget == target) base else base.copy(alpha = 0.33f)
    }
    val heroPillOrder: List<String> = when (heroPriorityTarget) {
        "track" -> listOf("track", "video")
        else -> listOf("video", "track")
    }
    val sectionSpacing = rememberSkydownScreenSectionSpacing()
    val topBarActionDescription = stringResource(
        if (onOpenWorkflow != null) {
            R.string.home_topbar_workflow_a11y
        } else {
            R.string.home_topbar_refresh_a11y
        },
    )

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
                        onGuestSignIn = onGuestSignIn,
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
                                    contentDescription = topBarActionDescription,
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
                .skydownAtmosphereBackground(
                    primaryColor = homeAccent,
                    secondaryColor = homeSpotifyAccent,
                    primaryAlpha = 0.016f,
                    secondaryAlpha = 0.012f,
                ),
        ) {
            HomeMapBackdrop(
                modifier = Modifier.fillMaxSize(),
            )
            BoxWithConstraints(
                modifier = Modifier.fillMaxSize(),
            ) {
                val contentMaxWidth = if (maxWidth > 1040.dp) 1040.dp else Dp.Unspecified
                val contentWidthFraction = if (contentMaxWidth != Dp.Unspecified && maxWidth > 0.dp) {
                    contentMaxWidth.value / maxWidth.value
                } else {
                    1f
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(contentWidthFraction)
                        .align(Alignment.TopCenter)
                        .testTag("home.root"),
                    state = listState,
                    contentPadding = skydownContentPadding(innerPadding),
                    verticalArrangement = Arrangement.spacedBy(sectionSpacing),
                ) {
                item {
                    val homeAtmosphereBg = colorScheme.background
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .drawBehind {
                                val h = size.height
                                if (h <= 0f) return@drawBehind
                                val wash = Brush.verticalGradient(
                                    colorStops = arrayOf(
                                        0f to homeAccent.copy(alpha = 0.024f),
                                        0.45f to homeMysticAccent.copy(alpha = 0.014f),
                                        0.8f to homeSpotifyAccent.copy(alpha = 0.010f),
                                        1f to homeAtmosphereBg,
                                    ),
                                    startY = 0f,
                                    endY = h,
                                )
                                drawRect(wash)
                            },
                    ) {
                        Spacer(Modifier.height(6.dp))
                        HomeAnimatedItem(order = 0) {
                            Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                BrandHeroCard(
                                    eyebrow = screenHeaderSettings.homeEyebrow.ifBlank { "SKY OS" },
                                    title = screenHeaderSettings.homeTitle.ifBlank { homeGreetingTitle },
                                    subtitle = screenHeaderSettings.homeSubtitle.ifBlank { homeGreetingSubtitle },
                                    detail = screenHeaderSettings.homeDetail.ifBlank { homeGreetingDetail },
                                    backgroundImageUrl = screenHeaderSettings.homeImageUrl.ifBlank { null },
                                    accent = homeAccent,
                                    secondaryAccent = homeMysticAccent,
                                    marks = listOf(BrandArtwork.SkyOS, BrandArtwork.Skydown),
                                    immersive = true,
                                    edgeToEdge = true,
                                    onSurfaceClick = onOpenProfile,
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 8.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        heroPillOrder.forEachIndexed { index, slot ->
                                            val weight = if (index == 0) 1.3f else 1f
                                            when (slot) {
                                                "track" -> Box(
                                                    modifier = Modifier
                                                        .weight(weight)
                                                        .then(
                                                            if (index == 0) Modifier.padding(end = 3.dp) else Modifier,
                                                        ),
                                                ) {
                                                    BrandPill(
                                                        text = stringResource(R.string.home_utility_music),
                                                        tint = heroPillTint("track"),
                                                        onClick = {
                                                            coroutineScope.launch {
                                                                listState.animateScrollToItem(homeMediaClusterSectionIndex)
                                                            }
                                                        },
                                                    )
                                                }
                                                else -> Box(
                                                    modifier = Modifier
                                                        .weight(weight)
                                                        .then(
                                                            if (index == 0) Modifier.padding(end = 3.dp) else Modifier,
                                                        ),
                                                ) {
                                                    BrandPill(
                                                        text = stringResource(R.string.home_utility_videos),
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
                                }
                            }
                        }
                        HomeAnimatedItem(order = 1) {
                            HomeUtilityRow(
                                onOpenMusic = { coroutineScope.launch { listState.animateScrollToItem(homeMediaClusterSectionIndex) } },
                                onOpenVideos = { coroutineScope.launch { listState.animateScrollToItem(homeMediaClusterSectionIndex) } },
                                onOpenMerch = onOpenCart,
                                onOpenSettings = onOpenSettings,
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                    }
                }

                item {
                    HomeAnimatedItem(order = 2) {
                        val homeMediaColorScheme = MaterialTheme.colorScheme
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 2.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(
                                text = stringResource(R.string.home_section_current),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium,
                                color = homeMediaColorScheme.onSurface.copy(alpha = 0.50f),
                            )
                        HomeMediaCluster(
                            uiState = uiState,
                            isTrackPlaying = currentAudioKey == uiState.featuredTrack?.let(::homeTrackAudioKey),
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
                }

                item {
                    HomeAnimatedItem(order = 3) {
                        Text(
                            text = stringResource(R.string.home_hero_tagline),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.66f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 6.dp),
                        )
                    }
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
    onOpenMusic: () -> Unit,
    onOpenVideos: () -> Unit,
    onOpenMerch: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    val view = LocalView.current
    data class UtilityItem(
        val id: String,
        val label: String,
        val icon: ImageVector,
        val action: () -> Unit,
    )
    val items = listOf(
        UtilityItem("music", stringResource(R.string.home_utility_music), Icons.Default.MusicNote, onOpenMusic),
        UtilityItem("videos", stringResource(R.string.home_utility_videos), Icons.Default.Movie, onOpenVideos),
        UtilityItem("merch", stringResource(R.string.home_utility_merch), Icons.Default.ShoppingBag, onOpenMerch),
        UtilityItem("settings", stringResource(R.string.home_utility_settings), Icons.Default.Settings, onOpenSettings),
    )
    val utilityTint: (String) -> Color = { id ->
        when (id) {
            "music" -> colorScheme.skydownAccent().copy(alpha = 0.94f)
            "videos" -> colorScheme.skydownAccentHighlight().copy(alpha = 0.94f)
            "merch" -> colorScheme.skydownAccentMystic().copy(alpha = 0.94f)
            else -> colorScheme.onSurface.copy(alpha = 0.76f)
        }
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.home_explore_title),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = colorScheme.onSurface.copy(alpha = 0.50f),
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(items) { item ->
                val tint = utilityTint(item.id)
                Surface(
                    onClick = {
                        view.performSkydownHaptic(SkydownHapticKind.Selection)
                        item.action()
                    },
                    shape = RoundedCornerShape(12.dp),
                    color = colorScheme.skydownSecondaryBackground().copy(alpha = 0.72f),
                    border = BorderStroke(1.dp, tint.copy(alpha = 0.22f)),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 9.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(tint.copy(alpha = 0.14f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = null,
                                modifier = Modifier.size(13.dp),
                                tint = tint,
                            )
                        }
                        Text(
                            item.label,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = tint,
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeLiveSignalSurface(
    hasTrackSignal: Boolean,
    hasVideoSignal: Boolean,
    trackName: String?,
    videoName: String?,
    aiUsageWarning: String? = null,
    creatorLimitZone: Boolean? = null,
    agentRunning: Boolean? = null,
    workflowWaiting: Boolean? = null,
    newOrderHint: String? = null,
    syncPaused: Boolean? = null,
    recoverableError: String? = null,
    contentSignal: String? = null,
) {
    val missingCount = listOf(hasTrackSignal, hasVideoSignal).count { !it }
    val nowText = when {
        hasTrackSignal && !trackName.isNullOrBlank() -> stringResource(R.string.home_live_now_music, trackName)
        hasVideoSignal && !videoName.isNullOrBlank() -> stringResource(R.string.home_live_now_visual, videoName)
        else -> stringResource(R.string.home_live_now_empty)
    }
    val nextText = when {
        !hasTrackSignal -> stringResource(R.string.home_live_next_track)
        !hasVideoSignal -> stringResource(R.string.home_live_next_video)
        else -> stringResource(R.string.home_live_next_focus)
    }
    val riskText = if (missingCount > 0) {
        stringResource(R.string.home_live_risk, missingCount)
    } else {
        null
    }
    val contentFederatedLine = contentSignal
        ?.takeIf { it.isNotBlank() }
        ?.let { stringResource(R.string.home_federated_content, it) }
    val federatedSignals = buildList {
        aiUsageWarning?.takeIf { it.isNotBlank() }?.let { add("AI: $it") }
        if (creatorLimitZone == true) add("AI: Creator limit zone reached.")
        if (agentRunning == true) add("AI: Agent currently running.")
        if (workflowWaiting == true) add("AI: Workflow waiting for next step.")
        newOrderHint?.takeIf { it.isNotBlank() }?.let { add("Commerce: $it") }
        if (syncPaused == true) add("System: Sync currently paused.")
        recoverableError?.takeIf { it.isNotBlank() }?.let { add("System: $it") }
        contentFederatedLine?.let { add(it) }
    }
    val colorScheme = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = stringResource(R.string.home_status_signals),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = colorScheme.onSurface.copy(alpha = 0.50f),
        )
        Text(nowText, style = MaterialTheme.typography.bodySmall, color = colorScheme.onSurface.copy(alpha = 0.66f))
        Text(nextText, style = MaterialTheme.typography.bodySmall, color = colorScheme.onSurface.copy(alpha = 0.58f))
        riskText?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = colorScheme.onSurface.copy(alpha = 0.55f))
        }
        federatedSignals.takeIf { it.isNotEmpty() }?.forEach { signal ->
            Text(
                text = signal,
                style = MaterialTheme.typography.bodySmall,
                color = colorScheme.onSurface.copy(alpha = 0.52f),
            )
        }
    }
}

@Composable
private fun HomeMediaCluster(
    uiState: HomeUiState,
    isTrackPlaying: Boolean,
    isVideoPlaying: Boolean,
    player: ExoPlayer,
    onPlayTrackToggle: (com.skydown.shared.model.Track) -> Unit,
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
    modifier: Modifier = Modifier,
    activeSignalCount: Int,
    totalSignalCount: Int,
    hasTrackSignal: Boolean,
    hasVideoSignal: Boolean,
    onRefresh: () -> Unit,
    onOpenRelease: () -> Unit,
    onOpenVideo: () -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    val view = LocalView.current
    val currentHour = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }
    val priorityTarget = when {
        !hasTrackSignal -> "music"
        !hasVideoSignal -> "visuals"
        currentHour in 5..11 -> "music"
        else -> "visuals"
    }
    val priorityAccent = when (priorityTarget) {
        "music" -> colorScheme.skydownSpotify()
        else -> colorScheme.skydownAccentHighlight()
    }
    val priorityTitle = when (priorityTarget) {
        "music" -> if (hasTrackSignal) {
            stringResource(R.string.home_dailyops_priority_music_ready)
        } else {
            stringResource(R.string.home_dailyops_priority_music_build)
        }
        else -> if (hasVideoSignal) {
            stringResource(R.string.home_dailyops_priority_visuals_ready)
        } else {
            stringResource(R.string.home_dailyops_priority_visuals_build)
        }
    }
    val priorityHint = when (priorityTarget) {
        "music" -> if (hasTrackSignal) {
            stringResource(R.string.home_dailyops_hint_music_morning)
        } else {
            stringResource(R.string.home_dailyops_hint_music_missing)
        }
        else -> if (hasVideoSignal) {
            stringResource(R.string.home_dailyops_hint_visuals_evening)
        } else {
            stringResource(R.string.home_dailyops_hint_visuals_missing)
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = priorityAccent.copy(alpha = 0.45f),
                modifier = Modifier.size(12.dp),
            )
            Text(
                text = stringResource(R.string.home_dailyops_current_focus),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Normal,
                color = colorScheme.onSurface.copy(alpha = 0.58f),
            )
            Spacer(modifier = Modifier.weight(1f))
            IconButton(
                onClick = {
                    view.performSkydownHaptic(SkydownHapticKind.Success)
                    onRefresh()
                },
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = stringResource(R.string.home_dailyops_refresh_a11y),
                    tint = priorityAccent.copy(alpha = 0.5f),
                )
            }
            Text(
                text = stringResource(R.string.home_dailyops_live_count, activeSignalCount, totalSignalCount),
                style = MaterialTheme.typography.labelSmall,
                color = priorityAccent.copy(alpha = 0.7f),
                fontWeight = FontWeight.Medium,
            )
        }

        Text(
            text = stringResource(R.string.home_dailyops_hint),
            style = MaterialTheme.typography.bodySmall,
            color = colorScheme.onSurface.copy(alpha = 0.55f),
        )

        BrandActionButton(
            text = priorityTitle,
            onClick = {
                view.performSkydownHaptic(SkydownHapticKind.Selection)
                when (priorityTarget) {
                    "music" -> onOpenRelease()
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
            color = colorScheme.onSurface.copy(alpha = 0.52f),
        )
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
        else -> colorScheme.skydownAccentHighlight()
    }
    val actionAccent: (String) -> Color = { target ->
        when (target) {
            "agent" -> colorScheme.skydownAccentMystic().copy(alpha = if (onOpenWorkflow != null) 1f else 0.4f)
            "track" -> {
                val base = colorScheme.skydownSpotify()
                if (priorityTarget == "track") base else base.copy(alpha = 0.56f)
            }
            else -> {
                val base = colorScheme.skydownAccentHighlight()
                if (priorityTarget == "video") base else base.copy(alpha = 0.56f)
            }
        }
    }
    Column(
        modifier = Modifier
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = priorityAccent.copy(alpha = 0.5f),
                modifier = Modifier.size(11.dp),
            )
            Text(
                text = stringResource(R.string.home_shortcuts_title),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                color = colorScheme.onSurface.copy(alpha = 0.6f),
            )
            Spacer(modifier = Modifier.weight(1f))
        }
        Text(
            text = stringResource(R.string.home_shortcuts_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = colorScheme.onSurface.copy(alpha = 0.52f),
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BrandActionButton(
                text = stringResource(R.string.home_command_ai_agent),
                onClick = {
                    view.performSkydownHaptic(SkydownHapticKind.Selection)
                    onOpenWorkflow?.invoke()
                },
                accent = actionAccent("agent"),
                compact = true,
                filled = onOpenWorkflow != null,
                enabled = onOpenWorkflow != null,
            )
            BrandActionButton(
                text = stringResource(R.string.home_command_cart),
                onClick = {
                    view.performSkydownHaptic(SkydownHapticKind.Selection)
                    onOpenCart()
                },
                accent = actionAccent("track"),
                compact = true,
                filled = false,
            )
            BrandActionButton(
                text = stringResource(R.string.home_command_settings),
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

    val fadeSpec = tween<Float>(
        durationMillis = SkydownMotionTokens.contentRevealEnterMillis,
        easing = FastOutSlowInEasing,
    )
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = fadeSpec),
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
            opacity = if (isDarkPalette) 0.055f else 0.045f,
        )
        HomeBackdropHalo(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(x = (-172).dp, y = 210.dp),
            haloSize = 240.dp,
            tint = colorScheme.skydownAccentMystic(),
            opacity = if (isDarkPalette) 0.06f else 0.05f,
        )
        HomeBackdropHalo(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(x = 154.dp, y = 498.dp),
            haloSize = 320.dp,
            tint = colorScheme.skydownAccentHighlight(),
            opacity = if (isDarkPalette) 0.05f else 0.04f,
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
            title = stringResource(R.string.home_media_music_title),
            subtitle = stringResource(R.string.home_media_music_subtitle),
            icon = Icons.Default.MusicNote,
            accent = SpotifyGreen,
        )
        val track = uiState.featuredTrack

        if (track == null) {
            Text(
                text = uiState.homeTrackMessage ?: stringResource(R.string.home_track_placeholder),
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
                hasPreview -> stringResource(R.string.home_track_preview_here)
                hasSpotifyTarget -> stringResource(R.string.home_track_open_spotify)
                else -> stringResource(R.string.home_track_ready)
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
                    label = if (isPlaying) {
                        stringResource(R.string.home_media_stop)
                    } else {
                        stringResource(R.string.home_media_play)
                    },
                    isActive = isPlaying,
                    accent = SpotifyGreen,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { onPlayToggle(track) },
                )
            }

            if (hasSpotifyTarget) {
                BrandActionButton(
                    text = homeSpotifyActionLabel(
                        track = track,
                        songLabel = stringResource(R.string.home_spotify_song),
                        artistLabel = stringResource(R.string.home_spotify_artist),
                        fallbackLabel = stringResource(R.string.home_spotify_open),
                    ),
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
            title = stringResource(R.string.home_media_video_title),
            subtitle = stringResource(R.string.home_media_video_subtitle),
            icon = Icons.Default.Movie,
            accent = InstagramOrange,
        )
        val video = uiState.featuredVideo

        if (video == null) {
            Text(
                text = uiState.homeVideoMessage ?: stringResource(R.string.home_video_placeholder),
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
            text = when {
                video.usesEmbeddedPreview -> stringResource(R.string.home_video_preview_here)
                video.supportsInlinePlayback -> stringResource(R.string.home_video_in_app)
                else -> stringResource(R.string.home_video_open_original)
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
                    text = if (video.openUrl.isBlank()) {
                        stringResource(R.string.home_video_no_link)
                    } else {
                        stringResource(R.string.home_video_open_original)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                )
            }
        }

        if (video.downloadUrl.isNotBlank()) {
            HomeMediaActionButton(
                label = if (isPlaying) {
                    stringResource(R.string.home_media_stop)
                } else {
                    stringResource(R.string.home_media_play)
                },
                isActive = isPlaying,
                accent = InstagramOrange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp),
                onClick = { onPlayToggle(video) },
            )
        } else if (video.openUrl.isNotBlank() || video.inlineEmbedUrl.isNotBlank()) {
            HomeMediaActionButton(
                label = stringResource(R.string.home_video_open),
                isActive = false,
                accent = InstagramOrange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp),
                onClick = { onOpenOriginal(video) },
            )
        } else if (video.supportsInlinePlayback) {
            HomeMediaActionButton(
                label = stringResource(R.string.home_video_open_hub),
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

private fun homeTrackAudioKey(track: com.skydown.shared.model.Track): String = "track:${track.trackId}"

private fun homeTrackedSignalCount(uiState: HomeUiState): Int = listOf(
    uiState.featuredTrack,
    uiState.featuredVideo,
).count { it != null }

private fun homeHeroPriorityTarget(
    hasTrackSignal: Boolean,
    hasVideoSignal: Boolean,
): String {
    if (!hasTrackSignal) return "track"
    if (!hasVideoSignal) return "video"
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return if (hour < 12) "track" else "video"
}

private fun homeHasSpotifyTarget(track: com.skydown.shared.model.Track): Boolean {
    return homeResolvedSpotifyTrackId(track) != null ||
        homeResolvedSpotifyArtistId(track) != null ||
        !track.externalUrl.isNullOrBlank()
}

private fun homeSpotifyActionLabel(
    track: com.skydown.shared.model.Track,
    songLabel: String,
    artistLabel: String,
    fallbackLabel: String,
): String {
    return when {
        homeResolvedSpotifyTrackId(track) != null -> songLabel
        homeResolvedSpotifyArtistId(track) != null -> artistLabel
        else -> fallbackLabel
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

private const val homeDestinationNicmaProducer = "home_nicma_producer"
private const val homeDestinationVideoHub = "home_video_hub"
/** LazyColumn index of [HomeMediaCluster] (0-based: hero, utility, media, footer). */
private const val homeMediaClusterSectionIndex = 1
private const val homeSignalTotal = 2
