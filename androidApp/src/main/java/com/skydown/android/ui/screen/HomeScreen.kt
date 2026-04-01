package com.skydown.android.ui.screen

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.skydown.android.data.mediaAttributionContext
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.skydown.android.ui.component.openTrackInSpotify
import coil3.compose.AsyncImage
import com.skydown.android.ui.component.AppTopBarSessionActions
import com.skydown.android.ui.component.BrandArtwork
import com.skydown.android.ui.component.BrandHeroCard
import com.skydown.android.ui.component.BrandPill
import com.skydown.android.ui.component.SectionHeader
import com.skydown.android.ui.component.SkydownCard
import com.skydown.android.ui.component.SkydownTopBarTitle
import com.skydown.android.ui.component.SkydownUiTokens
import com.skydown.android.ui.component.skydownScreenBrush
import com.skydown.android.ui.component.skydownTopBarColors
import com.skydown.android.ui.model.FeaturedBeatHighlight
import com.skydown.android.ui.model.FeaturedVideoHighlight
import com.skydown.android.ui.model.HomeUiState
import com.skydown.android.ui.viewmodel.HomeViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenCart: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onOpenWorkflow: (() -> Unit)? = null,
    viewModel: HomeViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
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

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    SkydownTopBarTitle(
                        "Sky²²",
                        "Alles, was fuer dich gerade wichtig ist, auf einen Blick.",
                    )
                },
                actions = {
                    AppTopBarSessionActions(
                        onOpenCart = onOpenCart,
                        onOpenSettings = onOpenSettings,
                    ) {
                        IconButton(onClick = onOpenWorkflow ?: viewModel::refresh) {
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
                    skydownScreenBrush(),
                ),
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = SkydownUiTokens.screenHorizontalPadding,
                    top = innerPadding.calculateTopPadding() + SkydownUiTokens.screenTopPadding,
                    end = SkydownUiTokens.screenHorizontalPadding,
                    bottom = innerPadding.calculateBottomPadding() + SkydownUiTokens.screenBottomPadding,
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item {
                    HomeAnimatedItem(order = 0) {
                        HomeHeroCard()
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
private fun HomeHeroCard() {
    BrandHeroCard(
        eyebrow = "Sky²² Home",
        title = "Sky²²",
        subtitle = "Hier startet direkt das, was fuer dich gerade wichtig ist.",
        detail = "Musik, Videos, Merchandise und Tools bleiben von hier aus sofort in Reichweite.",
        accent = MaterialTheme.colorScheme.primary,
        secondaryAccent = MaterialTheme.colorScheme.secondary,
        marks = listOf(BrandArtwork.Combined),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BrandPill(text = "Home", tint = MaterialTheme.colorScheme.primary)
            BrandPill(text = "Shop", tint = MaterialTheme.colorScheme.tertiary)
            BrandPill(text = "Tools", tint = MaterialTheme.colorScheme.secondary)
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
        SectionHeader("Gerade neu")
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
            AsyncImage(
                model = track.artworkUrl100,
                contentDescription = track.trackName,
                modifier = Modifier
                    .size(82.dp)
                    .clip(MaterialTheme.shapes.large),
                contentScale = ContentScale.Crop,
            )

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

        Text(
            text = when {
                hasPreview -> "Preview bleibt direkt im Home. Mehr dazu findest du im Music-Tab."
                hasSpotifyTarget -> "Neuester Song direkt ueber Spotify erreichbar."
                else -> "Neuester Song."
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
                Button(
                    onClick = { onPlayToggle(track) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = null,
                    )
                    Text(
                        text = if (isPlaying) "Release stoppen" else "Release abspielen",
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }

            if (hasSpotifyTarget) {
                OutlinedButton(
                    onClick = { onOpenSpotify(track) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                    )
                    Text(
                        text = homeSpotifyActionLabel(track),
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
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
    SkydownCard(contentPadding = PaddingValues(SkydownUiTokens.cardPadding)) {
        SectionHeader("Beat im Fokus")
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
            Box(
                modifier = Modifier
                    .size(82.dp)
                    .clip(MaterialTheme.shapes.large)
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
            OutlinedButton(
                onClick = { onPlayToggle(beat) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp),
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = null,
                )
                Text(
                    text = if (isPlaying) "Beat stoppen" else "Beat abspielen",
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
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
    SkydownCard(contentPadding = PaddingValues(SkydownUiTokens.cardPadding)) {
        SectionHeader("Video im Fokus")
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
            Box(
                modifier = Modifier
                    .size(82.dp)
                    .clip(MaterialTheme.shapes.large)
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
            text = "Direkt aus der Video-Auswahl ins Home geholt.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.64f),
            modifier = Modifier.padding(top = 14.dp),
        )

        if (video.downloadUrl.isNotBlank()) {
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
        }

        if (video.downloadUrl.isNotBlank()) {
            Button(
                onClick = { onPlayToggle(video) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp),
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = null,
                )
                Text(
                    text = if (isPlaying) "Video stoppen" else "Video abspielen",
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun HomeStoryCard(
    onOpenBeatHub: () -> Unit,
    onOpenNicma: () -> Unit,
) {
    val context = LocalContext.current
    SkydownCard(contentPadding = PaddingValues(SkydownUiTokens.cardPadding)) {
        SectionHeader("Direkt weiter")

        Text(
            text = "Hier findest du Kontakt, Artists, Beats und Studio gesammelt an einem Ort.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.74f),
            modifier = Modifier.padding(top = 8.dp),
        )

        Column(
            modifier = Modifier.padding(top = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            HomeStoryLinkButton(
                title = "Kontakt & Support",
                subtitle = "Wenn du etwas starten oder kurz klaeren willst.",
                isPrimary = true,
                onClick = { openExternalLink(context, homePrimaryContactLink.url) },
            )

            HomeLaneSection(
                title = "Music",
                subtitle = "Hier kommst du direkt zu Artists, Beats und Studio.",
            ) {
                homeZweizweiSocialLinks.forEachIndexed { index, link ->
                    val isPrimary = index == 0
                    HomeStoryLinkButton(
                        title = link.title,
                        subtitle = if (isPrimary) {
                            "Label, Releases und Updates auf einen Blick."
                        } else {
                            "Direkt zum Profil von ${link.title}."
                        },
                        isPrimary = isPrimary,
                        onClick = { openExternalLink(context, link.url) },
                    )
                }

                HomeStoryLinkButton(
                    title = "Zu den Beats",
                    subtitle = "Wenn du direkt zu Beats und Previews springen willst.",
                    isPrimary = false,
                    onClick = onOpenBeatHub,
                )

                HomeStoryLinkButton(
                    title = "Zum Studio",
                    subtitle = "Fuer Recording, Mixing und Mastering.",
                    isPrimary = false,
                    onClick = onOpenNicma,
                )
            }

            HomeLaneSection(
                title = "Video",
                subtitle = "Hier findest du Clips, Reels und den schnellsten Weg zum Kontakt.",
            ) {
                HomeStoryLinkButton(
                    title = "Video auf Instagram",
                    subtitle = "Fuer aktuelle Visuals, Clips und Updates.",
                    isPrimary = false,
                    onClick = {
                        openExternalLink(context, "https://www.instagram.com/skydown_entertainment/")
                    },
                )

                HomeStoryLinkButton(
                    title = "Kontakt per E-Mail",
                    subtitle = "Fuer Anfragen rund um Videography und Produktion.",
                    isPrimary = false,
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

@Composable
private fun HomeStoryLinkButton(
    title: String,
    subtitle: String,
    isPrimary: Boolean,
    onClick: () -> Unit,
) {
    val content: @Composable () -> Unit = {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = if (isPrimary) {
                    MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.80f)
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f)
                },
            )
        }
    }

    if (isPrimary) {
        Button(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
        ) {
            content()
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
        ) {
            content()
        }
    }
}

@Composable
private fun HomeLaneSection(
    title: String,
    subtitle: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.72f))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
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
) {
    val backgroundColor = if (isActive) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
    } else {
        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.70f)
    }
    val contentColor = if (isActive) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSecondaryContainer
    }

    Row(
        modifier = Modifier
            .clip(MaterialTheme.shapes.extraLarge)
            .background(backgroundColor)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = text,
            color = contentColor,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

private fun homeTrackAudioKey(track: com.skydown.shared.model.Track): String = "track:${track.trackId}"

private fun homeBeatAudioKey(beat: FeaturedBeatHighlight): String = "beat:${beat.id}"

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
